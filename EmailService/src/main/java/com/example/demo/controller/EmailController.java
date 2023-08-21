package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.EmailDetails;
import com.example.demo.service.EmailService;

@RestController
public class EmailController {

	@Autowired
	private EmailService emailService;
	
	@PostMapping("/sendMail")
    public void sendMail(@RequestBody EmailDetails details)
    {
		try {
			System.out.println("I am here");
       emailService.sendSimpleMail(details);
       System.out.println("I am going");
       }catch (Exception e) {
		System.out.println(e.getMessage());
	}
    }
 
	
}