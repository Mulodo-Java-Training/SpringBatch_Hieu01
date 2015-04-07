package com.test.spring.batch.tasklet;

import javax.sql.DataSource;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;

public class ReadingJobExecutionContextTasklet implements Tasklet, StepExecutionListener {
	
	private int numPage;
	private final String table;
	private final JdbcOperations jdbcTemplate;

	public ReadingJobExecutionContextTasklet(DataSource datasource, String table, int numPage) {
		this.table = table;
		this.jdbcTemplate = new JdbcTemplate(datasource);
		this.numPage = numPage;
	}

	@Override
	public void beforeStep(StepExecution stepExecution) {
		ExecutionContext executionContext = stepExecution.getJobExecution().getExecutionContext();
		executionContext.put("pageNum", numPage);
		
		int total = jdbcTemplate.queryForObject("SELECT COUNT(*) from " + table, Integer.class);
		

		int range = total / 3;
		
		numPage = numPage - 1;

		int fromRow = range*numPage;

		executionContext.putInt("range", range);
		
		if((numPage+1) == 1){
			executionContext.putString("stepParallel1", ("step parallel "+ (numPage+1)));
			executionContext.putInt("fromRow1", fromRow);
			executionContext.putInt("toRow1", fromRow+range);
		}else if ((numPage+1) == 2){
			executionContext.putString("stepParallel2", ("step parallel "+ (numPage+1)));
			executionContext.putInt("fromRow2", fromRow);
			executionContext.putInt("toRow2", fromRow+range);
		}else if ((numPage+1) == 3){
			executionContext.putString("stepParallel3", ("step parallel "+ (numPage+1)));
			executionContext.putInt("fromRow3", fromRow);
			executionContext.putInt("toRow3", fromRow+range);
		}

	}

	@Override
	public ExitStatus afterStep(StepExecution stepExecution) {

//		ExecutionContext executionContext = stepExecution.getJobExecution().getExecutionContext();
//		executionContext.put("pageNum", numPage);
//		
//		int total = jdbcTemplate.queryForObject("SELECT COUNT(*) from " + table, Integer.class);
//		
//
//		int range = total / 3;
//
//		int fromRow = range*numPage;
//
//		executionContext.putInt("fromRow", fromRow);
//		executionContext.putInt("toRow", fromRow+range);
//		executionContext.putInt("range", range);
//		
//		return ExitStatus.COMPLETED;
		return null;
	}

	@Override
	public RepeatStatus execute(StepContribution contribution,
			ChunkContext chunkContext) throws Exception {
		ExecutionContext executionContext = chunkContext.getStepContext().getStepExecution().getExecutionContext();
		executionContext.put("pageNum", numPage);
		
		int total = jdbcTemplate.queryForObject("SELECT COUNT(*) from " + table, Integer.class);
		

		int range = total / 3;

		int fromRow = range*numPage;

		executionContext.putInt("fromRow", fromRow);
		executionContext.putInt("toRow", fromRow+range);
		executionContext.putInt("range", range);
		return RepeatStatus.FINISHED;
	}

}
