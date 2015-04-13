package com.test.spring.batch.mail.service;

import javax.ejb.Stateless;
import javax.mail.Message;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Name;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
 
@Stateless
@Name("SendMail")
public class SendMailService {
 
    private static final Log log = LogFactory.getLog(SendMailService.class);
    private JavaMailSender mailSender;
    private String senderAddress;
    private String recipient;
 
    // set the fields
    public void setFields(JavaMailSender mailSender, String senderAddress, String recipient) {
 
        this.mailSender = mailSender;
        this.senderAddress = senderAddress;
        this.recipient = recipient;
    }
 
    public void sendMail() {
        log.debug("send Email started");
        
        System.out.println("mailSender=" +mailSender);
        System.out.println("senderAddress=" +senderAddress);
        System.out.println("recipient=" +recipient);
        
        
 
        MimeMessagePreparator preparator = new MimeMessagePreparator() {
            public void prepare(MimeMessage mimeMessage) throws Exception {
                mimeMessage.setRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
                mimeMessage.setFrom(new InternetAddress(senderAddress));
                mimeMessage.setSubject("Newer Report");
                // MimeMessagesHelper is needed for the attachment. The Boolean value in
                // constructor is for multipart/data = true
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
                helper.setText("Text in Email Body");
            }
        };
        try {
            this.mailSender.send(preparator);
            log.debug("send Email completed");
        } catch (MailException ex) {
        	ex.printStackTrace();
            log.debug("send Email failed", ex);
        }
    }
 
    
 }