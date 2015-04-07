package com.test.spring.batch.partition;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;

public class RangePartitioner implements Partitioner {
	
	private final String table;
	private final JdbcOperations jdbcTemplate;

	public RangePartitioner(DataSource datasource, String table) {
		this.table = table;
		this.jdbcTemplate = new JdbcTemplate(datasource);
	}

	@Override
	public Map<String, ExecutionContext> partition(int gridSize) {
		
		Map<String, ExecutionContext> result = new HashMap<String, ExecutionContext>();
		
		int fromRow = 0;
		
		System.out.println("grid size = "+ gridSize);
		
		
		int total = jdbcTemplate.queryForObject("SELECT COUNT(*) from " + table, Integer.class);
		

		int range = total / gridSize;
		int toRow = range;

		
		for(int i = 1; i <= gridSize; i++){
			ExecutionContext value = new ExecutionContext();
			
			System.out.println("\n Starting : thread "+ i);
			System.out.println("fromRow: "+ (fromRow+1));
			System.out.println("toRow :"+toRow);
			
			value.putInt("fromRow", fromRow);
			value.putInt("toRow", toRow);
			value.putInt("range", range);
			
			value.putString("name", "Thread: " + i);
			result.put("partition" + i, value);			
			
			fromRow = i*range;
			toRow = fromRow + range;
		}
		
		return result;
	}

}
