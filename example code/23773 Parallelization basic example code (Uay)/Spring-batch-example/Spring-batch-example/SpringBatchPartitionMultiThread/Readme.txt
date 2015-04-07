- Ở ví dụ này, partition multi thread steps sẽ xử lý step như sau: đọc dữ liệu từ bảng user gồm các cột id, username, password, age, sau đó vào item processor xử lý, rồi write vào xún file csv và step này được chia ra làm 10 slave dạng thread để chạy.
- Tạo file contenxt.xml: chứa nội dung sau: chủ yếu cấu hình jobRepository và jobLauncher.

	<beans xmlns="http://www.springframework.org/schema/beans"
		xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		xsi:schemaLocation="
			http://www.springframework.org/schema/beans 
			http://www.springframework.org/schema/beans/spring-beans-3.2.xsd">

		<!-- stored job-meta in memory -->
		<!--  
		<bean id="jobRepository"
			class="org.springframework.batch.core.repository.support.MapJobRepositoryFactoryBean">
			<property name="transactionManager" ref="transactionManager" />
		</bean>
	 	 -->
	 	 
	 	 <!-- stored job-meta in database -->
		<bean id="jobRepository"
			class="org.springframework.batch.core.repository.support.JobRepositoryFactoryBean">
			<property name="dataSource" ref="dataSource" />
			<property name="transactionManager" ref="transactionManager" />
			<property name="databaseType" value="mysql" />
		</bean>
		
		<bean id="transactionManager"
			class="org.springframework.batch.support.transaction.ResourcelessTransactionManager" />
		 
		<bean id="jobLauncher"
			class="org.springframework.batch.core.launch.support.SimpleJobLauncher">
			<property name="jobRepository" ref="jobRepository" />
		</bean>

	</beans>

- Tạo file database.xml: file này chưa bean dataSource, và initialize-database để khởi tạo các bảng cần thiết cho jobRepository.

	<beans xmlns="http://www.springframework.org/schema/beans"
		xmlns:jdbc="http://www.springframework.org/schema/jdbc" 
		xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		xsi:schemaLocation="http://www.springframework.org/schema/beans 
			http://www.springframework.org/schema/beans/spring-beans-3.2.xsd
			http://www.springframework.org/schema/jdbc 
			http://www.springframework.org/schema/jdbc/spring-jdbc-3.2.xsd">

	    <!-- connect to database -->
		<bean id="dataSource"
			class="org.springframework.jdbc.datasource.DriverManagerDataSource">
			<property name="driverClassName" value="com.mysql.jdbc.Driver" />
			<property name="url" value="jdbc:mysql://192.168.56.5:3306/testBatch" />
			<property name="username" value="root" />
			<property name="password" value="root" />
		</bean>

		<bean id="transactionManager"
			class="org.springframework.batch.support.transaction.ResourcelessTransactionManager" />
		
		<!-- create job-meta tables automatically -->
		<jdbc:initialize-database data-source="dataSource">
			<jdbc:script location="org/springframework/batch/core/schema-drop-mysql.sql" />
			<jdbc:script location="org/springframework/batch/core/schema-mysql.sql" />
		</jdbc:initialize-database>
		
	</beans>
- Tạo file job-partitioner: 

	Trong file này chỉ quan tâm đến vị trí: 
	    Cấu hình step: cấu hình partition cho step này và gắn 10 slave tương đương 10 thread được cấu hình ở grid-size và đưa taskExecutor vào.
		 <step id="masterStep">
			<partition step="slave" partitioner="rangePartitioner">
				<handler grid-size="10" task-executor="taskExecutor" />
			</partition>
	    </step>
	    <bean id="taskExecutor" class="org.springframework.core.task.SimpleAsyncTaskExecutor" />


	<?xml version="1.0" encoding="UTF-8"?>
	<beans xmlns="http://www.springframework.org/schema/beans"
		xmlns:batch="http://www.springframework.org/schema/batch"
		xmlns:util="http://www.springframework.org/schema/util"
		xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		xsi:schemaLocation="http://www.springframework.org/schema/batch 
		http://www.springframework.org/schema/batch/spring-batch-2.2.xsd
		http://www.springframework.org/schema/beans 
		http://www.springframework.org/schema/beans/spring-beans-3.2.xsd
		http://www.springframework.org/schema/util 
		http://www.springframework.org/schema/util/spring-util-3.2.xsd
		">
	 
	  <!-- spring batch core settings -->
	  <import resource="../config/context.xml" />
	 
	  <!-- database settings -->
	  <import resource="../config/database.xml" />
	 
	  <!-- partitioner job -->
	  <job id="partitionJob" xmlns="http://www.springframework.org/schema/batch">
	 
	 	<step id="cleanup" next="masterStep">
				<batch:tasklet ref="cleanupTasklet"/>
		</step>
	    <!-- master step, 10 threads (grid-size)  -->
	    <step id="masterStep">
			<partition step="slave" partitioner="rangePartitioner">
				<handler grid-size="10" task-executor="taskExecutor" />
			</partition>
	    </step>
	 
	  </job>
	 
	  <!-- each thread will run this job, with different stepExecutionContext values. -->
	  <step id="slave" xmlns="http://www.springframework.org/schema/batch">
		<tasklet>
			<chunk reader="pagingItemReader" writer="flatFileItemWriter"
				processor="itemProcessor" commit-interval="1" />
		</tasklet>
	  </step>
	  
	  	<bean id="cleanupTasklet" class="com.test.spring.batch.CleanupTasklet">
			<constructor-arg value="csv/outputs/"/>
		</bean>
	 
	  	<bean id="rangePartitioner" class="com.test.spring.batch.partition.RangePartitioner">
			<constructor-arg index="0" ref="dataSource"/>
			<constructor-arg index="1" value="users"/>
		</bean>
	 
		<bean id="taskExecutor" class="org.springframework.core.task.SimpleAsyncTaskExecutor" />
		
		<!-- inject stepExecutionContext -->
	 	<bean id="itemProcessor" class="com.test.spring.batch.processor.UserProcessor" scope="step">
			<property name="threadName" value="#{stepExecutionContext[name]}" />
		</bean>
	 
		 <bean id="pagingItemReader"
		class="org.springframework.batch.item.database.JdbcCursorItemReader"
		scope="step">
		<property name="dataSource" ref="dataSource" />
			<property name="sql"
				value="select id, user_login, user_pass, age from users limit #{stepExecutionContext[fromRow]}, #{stepExecutionContext[range]} ">	
			</property>
			
			<property name="rowMapper">
				<bean class="com.test.spring.batch.UserRowMapper" />
			</property>
		</bean>
	 
		<!-- csv file writer -->
		<bean id="flatFileItemWriter" class="org.springframework.batch.item.file.FlatFileItemWriter"
				scope="step" >
			<property name="resource"
				value="file:csv/outputs/users.processed#{stepExecutionContext[fromRow]+1}-#{stepExecutionContext[toRow]}.csv" />
			<property name="appendAllowed" value="false" />
			<property name="lineAggregator">
		  		<bean
					class="org.springframework.batch.item.file.transform.DelimitedLineAggregator">
					<property name="delimiter" value="," />
					<property name="fieldExtractor">
					  	<bean
							class="org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor">
							<property name="names" value="id, username, password, age" />
					  	</bean>
					</property>
			  	</bean>
			</property>
		</bean>
	 
	</beans>