2. Slave: 

  - Tạo file application.properties:
  	
  	batch.jdbc.driver=com.mysql.jdbc.Driver
	batch.jdbc.url=jdbc:mysql://172.16.1.37:3306/testBatchMutilThread
	batch.jdbc.user=root
	batch.jdbc.password=root
	broker.url=tcp://172.16.1.19:61616
	processed.images.dir=#{systemProperties['user.home']}#{systemProperties['file.separator']}image_submissions#{systemProperties['file.separator']}processed#{systemProperties['file.separator']}
	unprocessed.images.dir=#{systemProperties['user.home']}#{systemProperties['file.separator']}image_submissions#{systemProperties['file.separator']}unprocessed#{systemProperties['file.separator']}

  - Tạo file datasource-bean.xml: cấu hình datasource dùng để làm việc với database mysql, và khởi tạo database để spring batch làm việc.

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

		<bean id="dataSource" class="org.apache.commons.dbcp.BasicDataSource"
			  p:driverClassName="${batch.jdbc.driver}" p:url="${batch.jdbc.url}"
			  p:username="${batch.jdbc.user}" p:password="${batch.jdbc.password}"/>

		<bean id="transactionManager" class="org.springframework.jdbc.datasource.DataSourceTransactionManager"
			  p:dataSource-ref="dataSource"/>
	</beans>
  
  - Tạo file jms để lắng nghe master: để hiểu thêm cấu hình ở đây đọc thêm ở phần parallel configuration trong spring batch research documment.
	
	<?xml version="1.0" encoding="UTF-8"?>

	<beans xmlns="http://www.springframework.org/schema/beans"
		   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		   xmlns:int-jms="http://www.springframework.org/schema/integration/jms"
		   xmlns:p="http://www.springframework.org/schema/p"
		   xmlns:int="http://www.springframework.org/schema/integration"
		   xsi:schemaLocation="http://www.springframework.org/schema/beans
			 http://www.springframework.org/schema/beans/spring-beans.xsd
			 http://www.springframework.org/schema/integration/jms
			 http://www.springframework.org/schema/integration/jms/spring-integration-jms.xsd
			 http://www.springframework.org/schema/integration
			 http://www.springframework.org/schema/integration/spring-integration.xsd">

		<bean id="connectionFactory" class="org.apache.activemq.ActiveMQConnectionFactory"
			  p:brokerURL="${broker.url}"/>

		<int:channel id="requestChannel"/>

		<int-jms:message-driven-channel-adapter connection-factory="connectionFactory"
												destination-name="requestsQueue"
												channel="requestChannel"/>

		<int:channel id="replyChannel"/>

		<int-jms:outbound-channel-adapter connection-factory="connectionFactory"
										  destination-name="replyQueue"
										  channel="replyChannel"/>

		<int:service-activator input-channel="requestChannel"
							   output-channel="replyChannel"
							   ref="stepExecutionRequestHandler"/>
	</beans>

  
  - Tạo file job.xml: dùng để cấu hình step gồm các chức năng cơ bản như itemReader, itemProcessor, itemWriter, commit-interval, để hiểu thêm cấu hình ở đây đọc thêm ở phần parallel configuration trong spring batch research documment.

	<?xml version="1.0" encoding="UTF-8"?>

	<beans xmlns="http://www.springframework.org/schema/beans"
		   xmlns:batch="http://www.springframework.org/schema/batch"
		   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		   xsi:schemaLocation="http://www.springframework.org/schema/beans
			 http://www.springframework.org/schema/beans/spring-beans.xsd
			 http://www.springframework.org/schema/batch
			 http://www.springframework.org/schema/batch/spring-batch.xsd">

		<batch:step id="processImages">
			<batch:tasklet>
				<batch:chunk reader="itemReader" processor="itemProcessor" writer="itemWriter" commit-interval="50"/>
			</batch:tasklet>
		</batch:step>

		<batch:job-repository/>

		<bean id="itemReader" class="org.springframework.batch.item.database.JdbcPagingItemReader" scope="step">
			<property name="dataSource" ref="dataSource"/>
			<property name="queryProvider">
				<bean class="org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean">
					<property name="dataSource" ref="dataSource"/>
					<property name="selectClause" value="SELECT id, file_name"/>
					<property name="fromClause" value="from image_submissions"/>
					<property name="whereClause">
						<value>
							<![CDATA[
								where id >= :minValue and id <= :maxValue
							]]>
						</value>
					</property>
					<property name="sortKey" value="id"/>
				</bean>
			</property>
			<property name="pageSize" value="500"/>
			<property name="rowMapper">
				<bean class="org.springframework.jdbc.core.BeanPropertyRowMapper">
					<constructor-arg value="remote.partition.slave.domain.ImageSubmission"/>
				</bean>
			</property>
			<property name="parameterValues">
				<map>
					<entry key="minValue" value="#{stepExecutionContext[minValue]}"/>
					<entry key="maxValue" value="#{stepExecutionContext[maxValue]}"/>
				</map>
			</property>
		</bean>

		<bean id="itemProcessor" class="remote.partition.slave.processor.ImageProcessor">
			<constructor-arg index="0" value="${unprocessed.images.dir}"/>
			<constructor-arg index="1" value="200"/>
			<constructor-arg index="2" value="150"/>
		</bean>

		<bean id="itemWriter" class="remote.partition.slave.writer.ImageWriter" scope="step">
			<constructor-arg index="0" value="${processed.images.dir}"/>
			<constructor-arg index="1" value="jpg"/>
		</bean>

		<bean id="jobLauncher" class="org.springframework.batch.core.launch.support.SimpleJobLauncher">
			<property name="jobRepository" ref="jobRepository"/>
		</bean>

		<bean id="stepExecutionRequestHandler"
			  class="org.springframework.batch.integration.partition.StepExecutionRequestHandler">
			<property name="jobExplorer" ref="jobExplorer"/>
			<property name="stepLocator" ref="stepLocator"/>
		</bean>

		<bean id="jobExplorer" class="org.springframework.batch.core.explore.support.JobExplorerFactoryBean">
			<property name="dataSource" ref="dataSource"/>
		</bean>

		<bean id="stepLocator" class="org.springframework.batch.integration.partition.BeanFactoryStepLocator"/>
	</beans>

  - Tạo file job-context.xml: file run job sẽ lấy file này rồi get job và joblaucher để chạy:

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
	<import resource="jms.xml"/>
	<import resource="job.xml"/>
	</beans>



  







