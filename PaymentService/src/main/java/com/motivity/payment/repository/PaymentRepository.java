package com.motivity.payment.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.motivity.payment.entity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {
	
	Optional<Payment> findByBankName(String bankName);

}
