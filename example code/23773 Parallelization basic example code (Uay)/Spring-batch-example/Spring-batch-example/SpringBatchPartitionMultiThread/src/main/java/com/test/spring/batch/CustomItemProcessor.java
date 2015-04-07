package com.test.spring.batch;

import org.springframework.batch.item.ItemProcessor;

import com.test.spring.batch.model.Report;

public class CustomItemProcessor implements ItemProcessor<Report, Report> {

	@Override
	public Report process(Report item) throws Exception {
		
		System.out.println("Processing..." + item);
		return item;
	}

}