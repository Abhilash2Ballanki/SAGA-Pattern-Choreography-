package com.motivity.stock.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.example.demo.dto.CustomerOrder;
import com.example.demo.dto.FoodOrderEvent;
import com.example.demo.dto.StockEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.motivity.stock.model.FoodStock;
import com.motivity.stock.repository.StockRepository;

import lombok.extern.slf4j.Slf4j;
@Slf4j
@Service
public class ReverseStock {

	@Autowired
	private KafkaTemplate<String, FoodOrderEvent> kafkaTemplate;

	@Autowired
	private StockRepository repository;

	@KafkaListener(topics = "reverse-stock", groupId = "${spring.kafka.consumer.group-id}")
	public void reverseStock(String event) throws JsonMappingException, JsonProcessingException {
		log.info("------------------------------------------------------");
		log.info("Stock rollbacking is started and event received is {}",event);
		StockEvent stockEvent = new ObjectMapper().readValue(event, StockEvent.class);
		CustomerOrder customerOrder = stockEvent.getOrder();
		try {
			log.info("Stock table is updating as before and we are adding back our item {}'s quantity of {} into our inventory for orderId {}"
					,customerOrder.getItem(),customerOrder.getQuantity(),customerOrder.getOrderId());
			Optional<FoodStock> foodStock = repository.findByitem(customerOrder.getItem());
			foodStock.ifPresent(t -> {
				t.setQuantity(t.getQuantity() + customerOrder.getQuantity());
				repository.save(t);
			});
			log.info("stock table is successfully updated for orderId {}",customerOrder.getOrderId());
			log.info("------------------------------------------------");
			log.info("Now we are passing the event to 'failOrders' topic to fail the order for orderId {}",customerOrder.getOrderId());
			FoodOrderEvent foodEvent = new FoodOrderEvent();
			foodEvent.setOrder(customerOrder);
			foodEvent.setType(stockEvent.getType());
			kafkaTemplate.send("failOrders", foodEvent);
			log.info("Successfully passed the event to 'failOrders' topic to fail the order and event is {} for orderId {}",foodEvent,customerOrder.getOrderId());
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

}
