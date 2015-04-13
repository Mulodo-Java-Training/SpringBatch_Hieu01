package com.test.spring.batch.processor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.mail.javamail.JavaMailSender;

import com.test.spring.batch.mail.service.SendMailService;
 
public class SendMailTasklet implements Tasklet {
    private static final Log log = LogFactory.getLog(SendMailTasklet.class);
    private SendMailService sendMailService;
    private JavaMailSender mailSender;
    private String senderAddress;
    private String recipient;
 
    public void setMailSender(JavaMailSender mailSender) {
       this.mailSender = mailSender;
    }
    public void setSenderAddress(String senderAddress) {
        this.senderAddress = senderAddress;
    }
 
    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }
 
 
    public void setSendMailService(SendMailService sendMailService) {
        this.sendMailService = sendMailService;
    }
 
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.debug("execute(StepContribution contribution, ChunkContext chunkContext) begin");
        sendMailService.setFields(mailSender, senderAddress, recipient);
        sendMailService.sendMail();
        log.debug("execute(StepContribution contribution, ChunkContext chunkContext) end");
        return RepeatStatus.FINISHED;
    }
}