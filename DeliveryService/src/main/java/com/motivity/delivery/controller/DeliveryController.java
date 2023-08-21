package com.motivity.delivery.controller;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.example.demo.dto.CustomerOrder;
import com.example.demo.dto.EmailDetails;
import com.example.demo.dto.PaymentEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.motivity.delivery.entity.DeliveryTable;
import com.motivity.delivery.repository.DeliveryRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class DeliveryController {

	private RestTemplate restTemplate;

	@Autowired
	public DeliveryController(RestTemplateBuilder restTemplate) {
		super();
		this.restTemplate = restTemplate.build();
	}

	@Autowired
	private KafkaTemplate<String, PaymentEvent> kafkaTemplate;

	@Autowired
	private DeliveryRepository deliveryRepository;

	@KafkaListener(topics = "new-payments", groupId = "${spring.kafka.consumer.group-id}")
	public void deliverOrder(String event) throws JsonMappingException, JsonProcessingException {
		log.info("-------------------------------------------------");
		log.info("Your order is now in the Delivery checking phase with event {} ", event);
		PaymentEvent paymentEvent = new ObjectMapper().readValue(event, PaymentEvent.class);
		CustomerOrder order = paymentEvent.getOrder();
		DeliveryTable deliveryTable = new DeliveryTable();
		try {
			log.info("We are checking your address is present or not for orderId {}",order.getOrderId());
			if (order.getAddress() != null) {
				log.info("We got your address and the order is being placed for your address which is {} for orderId {}",order.getAddress(),order.getOrderId());
				deliveryTable.setAddress(order.getAddress());
				deliveryTable.setOrderId(order.getOrderId());
				deliveryTable.setStatus("Order will be delivered");
				deliveryRepository.save(deliveryTable);
				log.info("The order is  placed for your address which is {}",order.getAddress());
				log.info("-------------------------------------------------");
				log.info("Now we are sending a mail to your email that your order is being successfull and mail is {} for orderId {}",order.getEmail(),order.getOrderId());
				String subject = "Order Placed successfully";
				String body = "Your order " + order.getItem() + " of quantity " + order.getQuantity()
						+ " is placed and the total bill is Rs." + order.getAmount() + " and order id is "
						+ order.getOrderId();
				String toEmail = order.getEmail();
				EmailDetails details = new EmailDetails(toEmail, body, subject);
				HttpHeaders headers = new HttpHeaders();
				headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
				HttpEntity<EmailDetails> entity = new HttpEntity<EmailDetails>(details);
				restTemplate.exchange("http://email-service:8080/sendMail", HttpMethod.POST, entity, void.class);
				log.info("We have sent a mail to {} for orderId {}",order.getEmail(),order.getOrderId());
			} else {
				throw new RuntimeException("Address is not found");
			}
		} catch (Exception e) {
			log.info("------------------------------------------------");
			log.error("Exception is occured due to {}", e.getMessage());
			deliveryTable.setOrderId(order.getOrderId());
			deliveryTable.setStatus("Delivery Failed");
			deliveryRepository.save(deliveryTable);
			PaymentEvent paymentEvent2 = new PaymentEvent();
			paymentEvent2.setOrder(order);
			paymentEvent2.setType("Delivery_Failed");
			kafkaTemplate.send("reverse-payment", paymentEvent2);
			log.error("Delivery failure happened and we are sending event to the 'reverse-payment'"
					+ " topic to fail the order and event is {} for orderId {}", paymentEvent2,order.getOrderId());
			log.info("------------------------------------------------");
		}
	}

}
