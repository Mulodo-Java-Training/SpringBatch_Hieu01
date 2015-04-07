package spring.batch.multithreaded;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class JobRunner {
	private static final String CONFIG = "/META-INF/spring/config/job-context.xml";
	private static ApplicationContext applicationContext;

	public static void main(String[] args) throws Exception {
		applicationContext = new ClassPathXmlApplicationContext(CONFIG);

		try{
			Job job = applicationContext.getBean(Job.class);
			JobLauncher jobLauncher = applicationContext.getBean(JobLauncher.class);
	
//			jobLauncher.run(job, new JobParameters());
			JobExecution execution = jobLauncher.run(job, new JobParameters());
			
			System.out.println("Exit status : "+ execution.getStatus());
			System.out.println("Exit status : "+ execution.getAllFailureExceptions());
		}catch(Exception ex){
			ex.printStackTrace();
		}
		
		System.out.println("Done");
	}
}
