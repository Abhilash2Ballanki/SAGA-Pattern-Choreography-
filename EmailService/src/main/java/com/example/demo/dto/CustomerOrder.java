package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerOrder {
	
	private String item;
	private int quantity;
	private int amount;
	private int orderId;
	private String address;
	private String email;
	private String bank;

}
