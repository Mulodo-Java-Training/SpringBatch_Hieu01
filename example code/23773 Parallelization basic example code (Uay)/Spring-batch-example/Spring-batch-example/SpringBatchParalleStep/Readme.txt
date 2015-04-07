- Ở ví dụ này, parallel steps sẽ xử lý step như sau: đọc dữ liệu từ bảng user gồm các cột id, username, password, age, sau đó vào item processor xử lý, rồi write vào xún file csv và bảng này được chia ra làm 3 step để chạy parallel.
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

- Tạo file job-parallel.xml: file này bao gồm cấu hình job, step, itemReader, itemWriter và incude 2 file: context.xml và database.xml, chú yểu tập trung vào phần <split id="split1" task-executor="taskExecutor"> nó chứa nhiều flow và mỗi flow chưa nhiều step dùng để chạy parallel.

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
	<job id="parallelJob" xmlns="http://www.springframework.org/schema/batch">
		
		<batch:listeners>
			<batch:listener ref="jobListener"/>
		</batch:listeners>
			
	 	<step id="cleanup" next="split1">
			<batch:tasklet ref="cleanupTasklet"/>
		</step>
		
		<split id="split1" task-executor="taskExecutor">
	        <flow>
	            <step id="setDataStep1" next="step1">
					<batch:tasklet ref="stepData1"/>
				</step>
				<step id="step1">
					<batch:tasklet>
						<batch:chunk reader="itemReader1"  processor="itemProcessor1" 
						writer="itemWriter1" commit-interval="100"/>
					</batch:tasklet>
			    </step>
	        </flow>
	        <flow>
		        <step id="setDataStep2" next="step2">
					<batch:tasklet ref="stepData2"/>
				</step>
		 	    <step id="step2">
					<batch:tasklet>
						<batch:chunk reader="itemReader2" processor="itemProcessor2"
						 writer="itemWriter2" commit-interval="100"/>
					</batch:tasklet>
			    </step>
			    	           
	        </flow>
	        <flow>
	        	<step id="setDataStep3" next="step3">
					<batch:tasklet ref="stepData3"/>
				</step>
	        	<step id="step3">
					<batch:tasklet>
						<batch:chunk reader="itemReader3" processor="itemProcessor3" 
						writer="itemWriter3" commit-interval="100"/>
					</batch:tasklet>
			    </step>
	        </flow>
	    </split>
		</job>
		
		<bean id="stepData1" class="com.test.spring.batch.tasklet.ReadingJobExecutionContextTasklet" scope="step">
		<constructor-arg index="0" ref="dataSource"/>
		<constructor-arg index="1" value="users"/>
		<constructor-arg index="2" value="1"/>
	</bean>

	<bean id="stepData2" class="com.test.spring.batch.tasklet.ReadingJobExecutionContextTasklet" scope="step">
		<constructor-arg index="0" ref="dataSource"/>
		<constructor-arg index="1" value="users"/>
		<constructor-arg index="2" value="2"/>
	</bean>

	<bean id="stepData3" class="com.test.spring.batch.tasklet.ReadingJobExecutionContextTasklet" scope="step">
		<constructor-arg index="0" ref="dataSource"/>
		<constructor-arg index="1" value="users"/>
		<constructor-arg index="2" value="3"/>
	</bean>

	<bean id="jobListener" class="com.test.spring.batch.listener.JobDurationListener"/>

		<bean id="cleanupTasklet" class="com.test.spring.batch.tasklet.CleanupTasklet">
		<constructor-arg value="csv/outputs/"/>
	</bean>

	<bean id="taskExecutor" class="org.springframework.core.task.SimpleAsyncTaskExecutor" />

	<!-- inject stepExecutionContext -->
	<bean id="itemProcessor1" class="com.test.spring.batch.processor.CustomUserProcessor" scope="step">
		<property name="process" value="#{jobExecutionContext[stepParallel1]}"></property>
	</bean>
	<bean id="itemProcessor2" class="com.test.spring.batch.processor.CustomUserProcessor" scope="step">
		<property name="process" value="#{jobExecutionContext[stepParallel2]}"></property>
	</bean>
	<bean id="itemProcessor3" class="com.test.spring.batch.processor.CustomUserProcessor" scope="step">
		<property name="process" value="#{jobExecutionContext[stepParallel3]}"></property>
	</bean>

		<bean id="itemReader1"
		class="org.springframework.batch.item.database.JdbcCursorItemReader"
		scope="step">
		<property name="dataSource" ref="dataSource" />
		<property name="sql"
			value="select id, user_login, user_pass, age from users limit #{jobExecutionContext[fromRow1]}, #{jobExecutionContext[range]} ">	
		</property>
		
		<property name="rowMapper">
			<bean class="com.test.spring.batch.model.UserRowMapper" />
		</property>
	</bean>

	<bean id="itemReader2"
		class="org.springframework.batch.item.database.JdbcCursorItemReader"
		scope="step">
		<property name="dataSource" ref="dataSource" />
		<property name="sql"
			value="select id, user_login, user_pass, age from users limit #{jobExecutionContext[fromRow2]}, #{jobExecutionContext[range]} ">	
		</property>
		
		<property name="rowMapper">
			<bean class="com.test.spring.batch.model.UserRowMapper" />
		</property>
	</bean>

	<bean id="itemReader3"
		class="org.springframework.batch.item.database.JdbcCursorItemReader"
		scope="step">
		<property name="dataSource" ref="dataSource" />
		<property name="sql"
			value="select id, user_login, user_pass, age from users limit #{jobExecutionContext[fromRow3]}, #{jobExecutionContext[range]} ">	
		</property>
		
		<property name="rowMapper">
			<bean class="com.test.spring.batch.model.UserRowMapper" />
		</property>
	</bean>

	 <!-- csv file writer -->
	 <bean id="itemWriter1" class="org.springframework.batch.item.file.FlatFileItemWriter"
			scope="step" >
		 <property name="resource"
			value="file:csv/outputs/users.processed#{jobExecutionContext[fromRow1]+1}-#{jobExecutionContext[toRow1]}.csv" />
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
	 
	 <!-- csv file writer -->
	 <bean id="itemWriter2" class="org.springframework.batch.item.file.FlatFileItemWriter"
			scope="step" >
		 <property name="resource"
			value="file:csv/outputs/users.processed#{jobExecutionContext[fromRow2]+1}-#{jobExecutionContext[toRow2]}.csv" />
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
	 
	 <!-- csv file writer -->
	 <bean id="itemWriter3" class="org.springframework.batch.item.file.FlatFileItemWriter"
			scope="step" >
		 <property name="resource"
			value="file:csv/outputs/users.processed#{jobExecutionContext[fromRow3]+1}-#{jobExecutionContext[toRow3]}.csv" />
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

