package com.motivity.stock.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.RestController;

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
@RestController
public class FoodStockController {

	@Autowired
	private StockRepository repository;

	@Autowired
	private KafkaTemplate<String, FoodOrderEvent> kafkaTemplate2;

	@Autowired
	private KafkaTemplate<String, StockEvent> kafkaTemplate;

	@KafkaListener(topics = "new-orders", groupId = "${spring.kafka.consumer.group-id}")
	public void checkStockAndUpdate(String event) throws JsonMappingException, JsonProcessingException {
		log.info("--------------------------------------------------");
		log.info("Your order is now in the stock checking phase with event {} ",event);
		FoodOrderEvent foodEvent = new ObjectMapper().readValue(event, FoodOrderEvent.class);
		CustomerOrder customerOrder = foodEvent.getOrder();
		log.info("Your order item {} is being checked for the availability in the stock for orderId {}",customerOrder.getItem(),customerOrder.getOrderId());
		Optional<FoodStock> stock = repository.findByitem(customerOrder.getItem());
		FoodStock foodStock = new FoodStock();
		try {
			if (stock.isPresent()) {
				log.info("Your order item {} is available in the stock for orderId {}",customerOrder.getItem(),customerOrder.getOrderId());
				foodStock = stock.get();
				log.info("Your order item {} is being checked for the quantity in the stock for orderId {}",customerOrder.getItem(),customerOrder.getOrderId());
				if (customerOrder.getQuantity() <= foodStock.getQuantity()) {
					log.info("Your order item {} quantity is available in the stock for orderId {}",customerOrder.getItem(),customerOrder.getOrderId());
					int price = foodStock.getAmount() * customerOrder.getQuantity();
					customerOrder.setAmount(price);
					foodStock.setQuantity(foodStock.getQuantity() - customerOrder.getQuantity());
					repository.save(foodStock);
					log.info("Your order item quantity {}  is deducted from our stock for orderId {}",customerOrder.getQuantity(),customerOrder.getOrderId());
					log.info("------------------------------------------------");
					log.info("Now stock event is being created to publish the details into 'new-stock' topic");
					StockEvent stockEvent = new StockEvent();
					stockEvent.setOrder(customerOrder);
					stockEvent.setType("Stock_Updated");
					kafkaTemplate.send("new-stock", stockEvent);
					log.info("Stock event is sent to 'new-stock' topic with event is {} for orderId {}",stockEvent,customerOrder.getOrderId());
					log.info("------------------------------------------------");
				} else {
					throw new RuntimeException("Quantity is not available");
				}
			}else {
				throw new RuntimeException("Item is not available");
			}
		} catch (Exception e) {
			log.info("---------------------------------------------");
			log.error("Exception is occured due to {}",e.getMessage());
			FoodOrderEvent orderEvent = new FoodOrderEvent();
			orderEvent.setOrder(customerOrder);
			orderEvent.setType("Stock_Failed");
			kafkaTemplate2.send("failOrders", orderEvent);
			log.error("stock failure happened and we are sending event to the 'failOrders'"
					+ " topic to fail the order and event is {} for orderId {}",orderEvent,customerOrder.getOrderId());
			log.info("------------------------------------------------");
		}

	}
}
