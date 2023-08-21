package com.motivity.delivery.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.motivity.delivery.entity.DeliveryTable;

public interface DeliveryRepository extends JpaRepository<DeliveryTable, Integer> {

}
