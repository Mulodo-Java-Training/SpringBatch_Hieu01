package com.test.spring.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class App {
	private ApplicationContext context;

	public static void main(String[] args) {
		
		App obj = new App();
		obj.run();

	}
	
	private void run(){
		String[] stringConfig = { "spring/batch/jobs/job-partitioner.xml" };
		
		context = new ClassPathXmlApplicationContext(stringConfig);
		
		JobLauncher jobLauncher = (JobLauncher) context.getBean("jobLauncher");
		Job job =  (Job) context.getBean("retrySample");
		
		try{
			
			JobExecution execution = jobLauncher.run(job, new JobParameters());
			System.out.println("Exit status : "+ execution.getStatus());
			System.out.println("Exit status : "+ execution.getAllFailureExceptions());
			
		}catch(Exception ex){
			ex.printStackTrace();
		}
		System.out.println("Done");
	}
}
