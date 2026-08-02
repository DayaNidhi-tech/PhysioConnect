package com.physioconnect.repository;

import com.physioconnect.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByAppointmentId(Long appointmentId);
    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);
}
