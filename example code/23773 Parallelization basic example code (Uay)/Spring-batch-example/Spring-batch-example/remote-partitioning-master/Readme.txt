1.  Master: 

  - Tạo file application.properties:

  	batch.jdbc.driver=com.mysql.jdbc.Driver
	batch.jdbc.url=jdbc:mysql://172.16.1.37:3306/testBatchMutilThread
	batch.jdbc.user=root
	batch.jdbc.password=root
	batch.schema.script=org/springframework/batch/core/schema-drop-mysql.sql
	batch.drop.script=org/springframework/batch/core/schema-mysql.sql
	data.script=classpath*:/META-INF/spring/data/data-setup.sql
	broker.url=tcp://172.16.1.19:61616
	processed.images.dir=#{systemProperties['user.home']}#{systemProperties['file.separator']}image_submissions#{systemProperties['file.separator']}processed#{systemProperties['file.separator']}
  - Tạo file datasource-bean.xml:

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

  - Tạo file jms.xml: cấu hình jms có thể đọc thêm trong phần parallel configuration trong spring batch research doc

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

		<int:channel id="requestsChannel"/>

		<int-jms:outbound-channel-adapter connection-factory="connectionFactory"
										  channel="requestsChannel"
										  destination-name="requestsQueue"/>

		<int:channel id="replyChannel"/>

		<int-jms:message-driven-channel-adapter connection-factory="connectionFactory"
												channel="replyChannel"
												destination-name="replyQueue"/>

		<int:channel id="aggregatedReplyChannel">
			<int:queue/>
		</int:channel>

		<int:aggregator ref="partitionHandler"
						input-channel="replyChannel"
						output-channel="aggregatedReplyChannel"
						send-timeout="3600000"/>
	</beans>

  - Tạo file job: cấu hình các job và step được chay partition, cấu hình job có thể đọc thêm trong phần parallel configuration trong spring batch research doc

 	<?xml version="1.0" encoding="UTF-8"?>

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
			<batch:step id="cleanup" next="processImages.master">
				<batch:tasklet ref="cleanupTasklet"/>
			</batch:step>
			<batch:step id="processImages.master">
				<batch:partition  partitioner="partitioner" handler="partitionHandler"/>
			</batch:step>
		</batch:job>

		<batch:step id="processImages">
			<batch:tasklet>
				<bean class="remote.partition.master.tasklet.NoopTasklet"/>
			</batch:tasklet>
		</batch:step>

		<batch:job-repository/>

		<bean id="cleanupTasklet" class="remote.partition.master.tasklet.CleanupTasklet">
			<constructor-arg value="${processed.images.dir}"/>
		</bean>

		<bean id="jobListener" class="remote.partition.master.listener.JobDurationListener"/>

		<bean id="jobLauncher" class="org.springframework.batch.core.launch.support.SimpleJobLauncher">
			<property name="jobRepository" ref="jobRepository"/>
		</bean>

		<bean id="partitioner" class="remote.partition.master.partition.ColumnRangePartitioner">
			<constructor-arg index="0" ref="dataSource"/>
			<constructor-arg index="1" value="image_submissions"/>
			<constructor-arg index="2" value="id"/>
		</bean>

		<bean id="partitionHandler" class="org.springframework.batch.integration.partition.MessageChannelPartitionHandler">
			<property name="stepName" value="processImages"/>
			<property name="gridSize" value="5"/>
			<property name="replyChannel" ref="aggregatedReplyChannel"/>
			<property name="messagingOperations">
				<bean class="org.springframework.integration.core.MessagingTemplate">
					<property name="defaultChannel" ref="requestsChannel"/>
				</bean>
			</property>
		</bean>
	</beans>

  - Tạo file job-context.xml, class run sẽ gọi file này để chạy: dùng để import 3 file datasource, jms, job

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

