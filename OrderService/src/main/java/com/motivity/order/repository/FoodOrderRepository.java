package com.motivity.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.motivity.order.entity.FoodOrder;

public interface FoodOrderRepository extends JpaRepository<FoodOrder, Integer> {

}
