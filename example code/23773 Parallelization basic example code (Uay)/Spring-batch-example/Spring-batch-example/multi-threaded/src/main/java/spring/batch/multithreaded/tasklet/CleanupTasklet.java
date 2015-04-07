package spring.batch.multithreaded.tasklet;

import org.apache.commons.io.FileUtils;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import java.io.File;

public class CleanupTasklet implements Tasklet {
	private final String processedImagePath;

	public CleanupTasklet(final String processedImagePath) {
		this.processedImagePath = processedImagePath;
	}

	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
		FileUtils.cleanDirectory(new File(processedImagePath));

		return RepeatStatus.FINISHED;
	}
}
