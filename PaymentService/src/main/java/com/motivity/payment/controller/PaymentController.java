package com.motivity.payment.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.RestController;

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
@RestController
public class PaymentController {

	@Autowired
	private PaymentRepository paymentRepository;

	@Autowired
	private KafkaTemplate<String, StockEvent> template;

	@Autowired
	private KafkaTemplate<String, PaymentEvent> kafkaTemplate;

	@KafkaListener(topics = "new-stock", groupId = "${spring.kafka.consumer.group-id}")
	public void makePayment(String event) throws JsonMappingException, JsonProcessingException {
		log.info("-------------------------------------------------");
		log.info("Your order is now in the payment checking phase with event {} ", event);
		StockEvent stockEvent = new ObjectMapper().readValue(event, StockEvent.class);
		Payment payment;
		CustomerOrder customerOrder = stockEvent.getOrder();
		try {
			log.info("We are checking your bank {} is present in our database or not for orderId {}", customerOrder.getBank(),customerOrder.getOrderId());
			Optional<Payment> paymentBank = paymentRepository.findByBankName(customerOrder.getBank());
			if (paymentBank.isPresent()) {
				log.info(
						"We are having your bank in our database and now we are checking the amount availability in your {} bank for orderId {}",
						customerOrder.getBank(),customerOrder.getOrderId());
				payment = paymentBank.get();
				if (customerOrder.getAmount() <= payment.getAmount()) {
					log.info("You have enough balance to make a payment and {} is being deducted from {} bank for orderId {}",
							customerOrder.getAmount(), customerOrder.getBank(),customerOrder.getOrderId());
					payment.setAmount(payment.getAmount() - customerOrder.getAmount());
					paymentRepository.save(payment);
					log.info("We have deducted Rs.{} from {} bank for orderId {}", customerOrder.getAmount(), customerOrder.getBank(),customerOrder.getOrderId());
					log.info("------------------------------------------------");
					log.info("Now Payment event is being created to publish the details into 'new-payments' topic for orderId {}",customerOrder.getOrderId());
					PaymentEvent paymentEvent = new PaymentEvent();
					paymentEvent.setOrder(customerOrder);
					paymentEvent.setType("Payment_successfull");
					kafkaTemplate.send("new-payments", paymentEvent);
					log.info("Stock event is sent to 'new-stock' topic with event is {} for orderId {}", paymentEvent,customerOrder.getOrderId());
					log.info("------------------------------------------------");
				} else {
					throw new RuntimeException("Amount is insufficient in payment bank");
				}
			} else {
				throw new RuntimeException("Payment bank is not present");
			}
		} catch (Exception exception) {
			log.info("--------------------------------------");
			log.error("Exception is occured due to {}", exception.getMessage());
			stockEvent.setType("Payment_Failed");
			template.send("reverse-stock", stockEvent);
			log.error("payment failure happened and we are sending event to the 'reverse-stock'"
					+ " topic to fail the order and event is {} for orderId {}", stockEvent,customerOrder.getOrderId());
			log.info("------------------------------------------------");
		}

	}

}
