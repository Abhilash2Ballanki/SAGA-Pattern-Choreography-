package com.motivity.stock.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.motivity.stock.model.FoodStock;

public interface StockRepository extends JpaRepository<FoodStock, Integer> {
	
	Optional<FoodStock> findByitem(String item);

}
