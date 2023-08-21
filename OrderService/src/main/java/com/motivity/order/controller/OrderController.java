package com.motivity.order.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.CustomerOrder;
import com.example.demo.dto.FoodOrderEvent;
import com.motivity.order.entity.FoodOrder;
import com.motivity.order.repository.FoodOrderRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/order")
public class OrderController {

	@Autowired
	private FoodOrderRepository orderRepository;

	@Autowired
	private KafkaTemplate<String, FoodOrderEvent> kafkaTemplate;

	@PostMapping("/bookOrder")
	public ResponseEntity<String> foodOrder(@RequestBody CustomerOrder customerOrder) {
		log.info("Food order is being started for the order {} ", customerOrder);
		log.info("------------------------------------------------------");
		FoodOrder foodOrder = new FoodOrder();
		foodOrder.setItem(customerOrder.getItem());
		foodOrder.setQuantity(customerOrder.getQuantity());
		foodOrder.setStatus("Created");

		try {
			foodOrder = orderRepository.save(foodOrder);
			log.info("food order is created and saved in the table and status is {} ", foodOrder.getStatus());
			log.info("------------------------------------------------------");
			customerOrder.setOrderId(foodOrder.getOrderId());
			FoodOrderEvent event = new FoodOrderEvent();
			event.setOrder(customerOrder);
			event.setType("Order_Created");
			log.info("Food event is now created");
			kafkaTemplate.send("new-orders", event);
			log.info("Food event is sent to the broker and the message is {} ", event);
			log.info("-------------------------------------------------------");
			return new ResponseEntity<String>("Order is being placed ! wait for the confirmation through mail",
					HttpStatus.OK);
		} catch (Exception exception) {
			log.error("--------------------------------------------------");
			log.error("Exception occured ! We are failing your order");
			foodOrder.setStatus("Failed");
			orderRepository.save(foodOrder);
			log.error("Your order is failed ! please try after some time");
			log.error("--------------------------------------------------");
		}
		return new ResponseEntity<String>("Unsuccessfull order", HttpStatus.INTERNAL_SERVER_ERROR);
	}

}
