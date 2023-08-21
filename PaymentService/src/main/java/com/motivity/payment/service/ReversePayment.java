package com.motivity.payment.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.example.demo.dto.CustomerOrder;
import com.example.demo.dto.PaymentEvent;
import com.example.demo.dto.StockEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.motivity.payment.entity.Payment;
import com.motivity.payment.repository.PaymentRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ReversePayment {
	
	@Autowired
	private PaymentRepository paymentRepository;
	
	@Autowired
	private KafkaTemplate<String, StockEvent> kafkaTemplate;
	
	@KafkaListener(topics = "reverse-payment",groupId = "{spring.kafka.consumer.group-id}")
	public void reversePayment(String event) throws JsonMappingException, JsonProcessingException {
		log.info("------------------------------------------------------");
		log.info("Payment rollbacking is started and event received is {}",event);
		PaymentEvent paymentEvent = new ObjectMapper().readValue(event, PaymentEvent.class);
		CustomerOrder customerOrder = paymentEvent.getOrder();
		Optional<Payment> paymentBank = paymentRepository.findByBankName(customerOrder.getBank());
		log.info("Payment table is updating as before and we are adding back our amount for orderId {}",customerOrder.getOrderId());
		paymentBank.ifPresent(t -> {
			t.setAmount(t.getAmount()+customerOrder.getAmount());
			paymentRepository.save(t);
		});
		log.info("Payment table is successfully updated");
		log.info("------------------------------------------------");
		log.info("Now we are passing the event to 'reverse-stock' topic to fail the order for orderId {}",customerOrder.getOrderId());
		StockEvent stockEvent = new StockEvent();
		stockEvent.setOrder(customerOrder);
		stockEvent.setType(paymentEvent.getType());
		kafkaTemplate.send("reverse-stock", stockEvent);
		log.info("Successfully passed the event to 'reverse-stock' topic to fail the order and event is {} for orderId {}",stockEvent,customerOrder.getOrderId());
	}

}
