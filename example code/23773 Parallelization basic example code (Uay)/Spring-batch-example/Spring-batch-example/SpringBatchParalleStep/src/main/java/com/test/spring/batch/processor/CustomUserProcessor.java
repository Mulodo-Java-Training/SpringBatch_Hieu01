package com.test.spring.batch.processor;

import org.springframework.batch.item.ItemProcessor;

import com.test.spring.batch.model.User;

public class CustomUserProcessor implements ItemProcessor<User, User> {
	
	private String process;
	
	public String getProcess() {
		return process;
	}

	public void setProcess(String process) {
		this.process = process;
	}

	@Override
	public User process(User user) throws Exception {
		
		System.out.println(process +  " Process:  " + user.getUsername());
		return user;
	}

}