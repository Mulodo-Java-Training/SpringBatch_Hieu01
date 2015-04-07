- Ở ví dụ này, multi thread steps sẽ xử lý step như sau: đọc dữ liệu từ bảng user gồm các cột id, file_name sau đó vào item processor resize bức ảnh có tên tương ứng vs file_name, rồi write vào file mới vào thư mục mới, step này được chia thành nhiều thread để chạy.
- Tạo file job-contenxt.xml: chứa nội dung sau: chủ yếu cấu hình jobRepository.

	<?xml version="1.0" encoding="UTF-8"?>

	<beans xmlns="http://www.springframework.org/schema/beans"
		   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		   xmlns:jdbc="http://www.springframework.org/schema/jdbc"
		   xmlns:p="http://www.springframework.org/schema/p"
		   xsi:schemaLocation="http://www.springframework.org/schema/beans
			 http://www.springframework.org/schema/beans/spring-beans.xsd
			 http://www.springframework.org/schema/jdbc
			 http://www.springframework.org/schema/jdbc/spring-jdbc.xsd">


		
		<jdbc:initialize-database data-source="dataSource">
			<jdbc:script location="org/springframework/batch/core/schema-drop-mysql.sql" />
			<jdbc:script location="org/springframework/batch/core/schema-mysql.sql" />
			<!-- <jdbc:script location="${data.script}"/> -->
		</jdbc:initialize-database>

		<bean id="dataSource" class="org.springframework.jdbc.datasource.DriverManagerDataSource"
			  p:driverClassName="${batch.jdbc.driver}" p:url="${batch.jdbc.url}"
			  p:username="${batch.jdbc.user}" p:password="${batch.jdbc.password}"/>

		<bean id="transactionManager" class="org.springframework.jdbc.datasource.DataSourceTransactionManager"
			  p:dataSource-ref="dataSource"/>
	</beans>


- Tạo file application.properties: chưa các properties sẽ được dùng trong các file xml.
	
	batch.jdbc.driver=com.mysql.jdbc.Driver
	batch.jdbc.url=jdbc:mysql://192.168.56.5:3306/testBatchMutilThread
	batch.jdbc.user=root
	batch.jdbc.password=
	batch.schema.script=org/springframework/batch/core/schema-drop-mysql.sql
	batch.drop.script=org/springframework/batch/core/schema-mysql.sql
	data.script=classpath*:/META-INF/spring/data/data-setup.sql
	processed.images.dir=#{systemProperties['user.home']}#{systemProperties['file.separator']}image_submissions#{systemProperties['file.separator']}processed#{systemProperties['file.separator']}
	unprocessed.images.dir=#{systemProperties['user.home']}#{systemProperties['file.separator']}image_submissions#{systemProperties['file.separator']}unprocessed#{systemProperties['file.separator']}



- Tạo file database.xml: file này chưa bean dataSource, và initialize-database để khởi tạo các bảng cần thiết cho jobRepository.

 	<?xml version="1.0" encoding="UTF-8"?>

	<beans xmlns="http://www.springframework.org/schema/beans"
		   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		   xmlns:context="http://www.springframework.org/schema/context"
		   xsi:schemaLocation="http://www.springframework.org/schema/beans
			 http://www.springframework.org/schema/beans/spring-beans.xsd
			 http://www.springframework.org/schema/context
			 http://www.springframework.org/schema/context/spring-context.xsd">

		<context:property-placeholder location="classpath:/META-INF/spring/config/application.properties"/>

		<import resource="datasource-beans.xml"/>
		<import resource="job.xml"/>
		
		<!-- stored job-meta in database -->
		<bean id="jobRepository"
			class="org.springframework.batch.core.repository.support.JobRepositoryFactoryBean">
			<property name="dataSource" ref="dataSource" />
			<property name="transactionManager" ref="transactionManager" />
			<property name="databaseType" value="mysql" />
		</bean>
		
		<bean id="transactionManager"
			class="org.springframework.batch.support.transaction.ResourcelessTransactionManager" />
	</beans>


- Tạo file job.xml: file này bao gồm cấu hình job, step, itemReader, itemWriter, chú yểu tập trung vào phần <bean id="taskExecutor" class="org.springframework.core.task.SimpleAsyncTaskExecutor"> nó sẽ tự động tách step thành nhiều step khác nhau để xử lý. Chỉ cần tập trung vài 2 đoạn:
		
		<batch:step id="processImages">
			<batch:tasklet task-executor="taskExecutor">
				<batch:chunk reader="itemReader" processor="itemProcessor" writer="itemWriter" commit-interval="100"/>
			</batch:tasklet>
		</batch:step>
		và 
		<bean id="taskExecutor" class="org.springframework.core.task.SimpleAsyncTaskExecutor">
			<property name="concurrencyLimit" value="8"/>
		</bean>
		2 cấu hình chính cho multi-thread. Còn lại các cấu hình itemReader, itemWriter và itemProcessor cấu hình như bình thường.


		File cấu hình đầy đủ gồm :



			<beans xmlns="http://www.springframework.org/schema/beans"
				   xmlns:batch="http://www.springframework.org/schema/batch"
				   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
				   xsi:schemaLocation="http://www.springframework.org/schema/beans
					 http://www.springframework.org/schema/beans/spring-beans.xsd
					 http://www.springframework.org/schema/batch
					 http://www.springframework.org/schema/batch/spring-batch.xsd">

				<batch:job id="imageProcessingJob">
					<batch:listeners>
						<batch:listener ref="jobListener"/>
					</batch:listeners>
					<batch:step id="cleanup" next="processImages">
						<batch:tasklet ref="cleanupTasklet"/>
					</batch:step>
					<batch:step id="processImages">
						<batch:tasklet task-executor="taskExecutor">
							<batch:chunk reader="itemReader" processor="itemProcessor" writer="itemWriter" commit-interval="100"/>
						</batch:tasklet>
					</batch:step>
				</batch:job>

				<bean id="cleanupTasklet" class="spring.batch.multithreaded.tasklet.CleanupTasklet">
					<constructor-arg value="${processed.images.dir}"/>
				</bean>

				<bean id="taskExecutor" class="org.springframework.core.task.SimpleAsyncTaskExecutor">
					<property name="concurrencyLimit" value="8"/>
				</bean>

				<bean id="itemReader" class="org.springframework.batch.item.database.JdbcPagingItemReader">
					<property name="dataSource" ref="dataSource"/>
					<property name="queryProvider">
						<bean class="org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean">
							<property name="dataSource" ref="dataSource"/>
							<property name="selectClause" value="SELECT id, file_name"/>
							<property name="fromClause" value="from image_submissions"/>
							<property name="sortKey" value="id"/>
						</bean>
					</property>
					<property name="pageSize" value="100"/>
					<property name="rowMapper">
						<bean class="org.springframework.jdbc.core.BeanPropertyRowMapper">
							<constructor-arg value="spring.batch.multithreaded.domain.ImageSubmission"/>
						</bean>
					</property>
				</bean>

				<bean id="itemProcessor" class="spring.batch.multithreaded.processor.ImageProcessor">
					<constructor-arg index="0" value="${unprocessed.images.dir}"/>
					<constructor-arg index="1" value="200"/>
					<constructor-arg index="2" value="150"/>
				</bean>

				<bean id="itemWriter" class="spring.batch.multithreaded.writer.ImageWriter">
					<constructor-arg index="0" value="${processed.images.dir}"/>
					<constructor-arg index="1" value="jpg"/>
				</bean>

				<bean id="jobListener" class="spring.batch.multithreaded.listener.JobDurationListener"/>

				<bean id="jobLauncher" class="org.springframework.batch.core.launch.support.SimpleJobLauncher">
					<property name="jobRepository" ref="jobRepository"/>
				</bean>
			</beans>







	
