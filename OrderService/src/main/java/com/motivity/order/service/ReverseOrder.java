package com.motivity.order.service;

import java.util.Collections;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.demo.dto.CustomerOrder;
import com.example.demo.dto.EmailDetails;
import com.example.demo.dto.FoodOrderEvent;
import com.example.demo.service.EmailService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.motivity.order.entity.FoodOrder;
import com.motivity.order.repository.FoodOrderRepository;

import lombok.extern.slf4j.Slf4j;
@Slf4j
@Service
public class ReverseOrder {
	
	private RestTemplate restTemplate;
	
	@Autowired
	public ReverseOrder(RestTemplateBuilder restTemplate) {
		this.restTemplate = restTemplate.build();
	}

	@Autowired
	private FoodOrderRepository repository;
	
	@KafkaListener(topics = "failOrders",groupId = "${spring.kafka.consumer.group-id}")
	public void reverseOrder(String event) throws JsonMappingException, JsonProcessingException {
		log.info("Order cancellation started, event received is {} ",event);
		log.info("------------------------------------------------------");
		try {
		FoodOrderEvent foodEvent = new ObjectMapper().readValue(event, FoodOrderEvent.class);
		CustomerOrder order = foodEvent.getOrder();
		Optional<FoodOrder> foodOrder = repository.findById(order.getOrderId());
		foodOrder.ifPresent(o -> {
			o.setStatus("Failed");
			repository.save(o);
		});
		log.info("Order cancellation success and the order table status is updated as failed for orderId {}",order.getOrderId());
		log.info("------------------------------------------------------");
		log.info("Order message is being sent to email {} for orderId {}",order.getEmail(),order.getOrderId());
		log.info("------------------------------------------------------");
		String subject = "Order Failed";
		String body = "Your order "+order.getItem()+" of quantity "+order.getQuantity()+" of order id "+order.getOrderId()+" is failed"+ " due to "+foodEvent.getType();
		String toEmail = order.getEmail();
		EmailDetails details = new EmailDetails(toEmail, body, subject);
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
		HttpEntity<EmailDetails> entity = new HttpEntity<EmailDetails>(details);
		restTemplate.exchange("http://email-service:8080/sendMail", HttpMethod.POST, entity, void.class);
		log.info("Order message is successfully sent to email {} for orderId {}",order.getEmail(),order.getOrderId());
		log.info("------------------------------------------------------");
		}catch (Exception e) {
			System.out.println(e.getMessage());
		}
		
	}

}
