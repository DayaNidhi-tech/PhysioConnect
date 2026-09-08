package com.physioconnect.dto;

import com.physioconnect.entity.Payment;

import java.math.BigDecimal;

public record PaymentOrderResponse(
        Long paymentId,
        Long appointmentId,
        String appointmentReferenceNo,
        String razorpayOrderId,
        String razorpayKeyId,
        BigDecimal amount,
        String currency
) {
    public static PaymentOrderResponse from(Payment payment, String razorpayKeyId) {
        return new PaymentOrderResponse(
                payment.getId(),
                payment.getAppointment().getId(),
                payment.getAppointment().getReferenceNo(),
                payment.getRazorpayOrderId(),
                razorpayKeyId,
                payment.getAmount(),
                payment.getCurrency()
        );
    }
}
