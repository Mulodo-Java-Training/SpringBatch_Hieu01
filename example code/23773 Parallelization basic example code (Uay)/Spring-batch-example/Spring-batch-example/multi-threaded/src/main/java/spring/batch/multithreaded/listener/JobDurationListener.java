package spring.batch.multithreaded.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.util.StopWatch;

import java.util.concurrent.TimeUnit;

public class JobDurationListener implements JobExecutionListener {
	private static final Log LOG = LogFactory.getLog(JobDurationListener.class);

	private StopWatch stopWatch;

	public void beforeJob(JobExecution jobExecution) {
		stopWatch = new StopWatch();
		stopWatch.start("Processing image submissions");
	}

	public void afterJob(JobExecution jobExecution) {
		stopWatch.stop();

		long duration = stopWatch.getLastTaskTimeMillis();

		LOG.info(String.format("Job took: %d minutes, %d seconds.",
				TimeUnit.MILLISECONDS.toMinutes(duration),
				TimeUnit.MILLISECONDS.toSeconds(duration) -
						TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration))
		));
	}
}
