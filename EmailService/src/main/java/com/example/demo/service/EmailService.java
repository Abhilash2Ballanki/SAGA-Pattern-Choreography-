package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.example.demo.dto.EmailDetails;

import lombok.extern.slf4j.Slf4j;
@Slf4j
@Service
public class EmailService {

	@Autowired
	public JavaMailSender javaMailSender;
	

	public void sendSimpleMail(EmailDetails details) {

		try {
			log.info("Now I am came here with details {} ",details.toString());
			SimpleMailMessage mailMessage = new SimpleMailMessage();

			mailMessage.setTo(details.getTo());
			mailMessage.setText(details.getMsgBody());
			mailMessage.setSubject(details.getSubject());

			javaMailSender.send(mailMessage);
			log.info("message successfully sent");
		}

		catch (Exception e) {
			System.out.println("exception occured "+e.getMessage());
		}
	}

}
