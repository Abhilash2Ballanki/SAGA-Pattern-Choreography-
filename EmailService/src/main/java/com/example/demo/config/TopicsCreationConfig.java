package com.example.demo.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class TopicsCreationConfig {

	
private final String TOPIC_NAME = "new-orders";
	
	@Bean
	public NewTopic createTopic() {
		return TopicBuilder
				.name(TOPIC_NAME)
				.build();
	}
	@Bean
	public NewTopic createPaymentTopic() {
		return TopicBuilder
				.name("new-payments")
				.build();
	}
	@Bean
	public NewTopic createStockTopic() {
		return TopicBuilder
				.name("new-stock")
				.build();
	}
	
	@Bean
	public NewTopic reverseOrderTopic()
	{
		return TopicBuilder
				.name("failOrders")
				.build();
	}
	
	@Bean
	public NewTopic reverseStockTopic()
	{
		return TopicBuilder
				.name("reverse-stock")
				.build();
	}
	
	@Bean
	public NewTopic reversePaymentTopic()
	{
		return TopicBuilder
				.name("reverse-payment")
				.build();
	}
}
