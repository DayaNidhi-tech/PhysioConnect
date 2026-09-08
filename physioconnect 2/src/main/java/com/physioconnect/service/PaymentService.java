package com.physioconnect.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.physioconnect.dto.PaymentOrderResponse;
import com.physioconnect.entity.Appointment;
import com.physioconnect.entity.Payment;
import com.physioconnect.entity.enums.AppointmentStatus;
import com.physioconnect.entity.enums.PaymentStatus;
import com.physioconnect.exception.BadRequestException;
import com.physioconnect.exception.ResourceNotFoundException;
import com.physioconnect.repository.AppointmentRepository;
import com.physioconnect.repository.PaymentRepository;
import com.physioconnect.security.UserPrincipal;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
public class PaymentService {

    private final AppointmentRepository appointmentRepository;
    private final PaymentRepository paymentRepository;
    private final String razorpayKeyId;
    private final String razorpayKeySecret;

    public PaymentService(
            AppointmentRepository appointmentRepository,
            PaymentRepository paymentRepository,
            @Value("${app.razorpay.key-id:}") String razorpayKeyId,
            @Value("${app.razorpay.key-secret:}") String razorpayKeySecret
    ) {
        this.appointmentRepository = appointmentRepository;
        this.paymentRepository = paymentRepository;
        this.razorpayKeyId = razorpayKeyId;
        this.razorpayKeySecret = razorpayKeySecret;
    }

    @Transactional
    public PaymentOrderResponse createOrder(UserPrincipal principal, Long appointmentId) {
        Appointment appointment = appointmentRepository.findByIdForUpdate(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        ensurePatientOwnsAppointment(principal, appointment);

        if (appointment.getStatus() != AppointmentStatus.PENDING_PAYMENT) {
            throw new BadRequestException("Payment can only be initiated for a pending-payment appointment");
        }

        LocalDateTime now = LocalDateTime.now();
        if (appointment.getSlot().getHeldUntil() == null
                || !appointment.getSlot().getHeldUntil().isAfter(now)) {
            throw new BadRequestException("The payment hold for this appointment has expired. Please book the slot again");
        }

        if (appointment.getAmount() == null || appointment.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Appointment amount must be greater than zero");
        }

        Payment existingPayment = paymentRepository.findByAppointmentId(appointmentId).orElse(null);
        if (existingPayment != null) {
            if (existingPayment.getStatus() == PaymentStatus.SUCCESS) {
                throw new BadRequestException("Payment for this appointment is already completed");
            }

            if (existingPayment.getRazorpayOrderId() != null
                    && existingPayment.getStatus() == PaymentStatus.PENDING) {
                return PaymentOrderResponse.from(existingPayment, razorpayKeyId);
            }
        }

        if (razorpayKeyId.isBlank() || razorpayKeySecret.isBlank()) {
            throw new BadRequestException("Razorpay payment configuration is not available");
        }

        long amountInPaise;
        try {
            amountInPaise = appointment.getAmount()
                    .setScale(2, RoundingMode.UNNECESSARY)
                    .movePointRight(2)
                    .longValueExact();
        } catch (ArithmeticException ex) {
            throw new BadRequestException("Appointment amount must be a valid INR amount");
        }

        try {
            RazorpayClient razorpayClient = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", appointment.getReferenceNo());
            orderRequest.put("partial_payment", false);

            Order order = razorpayClient.orders.create(orderRequest);
            String orderId = order.get("id");

            if (orderId == null || orderId.isBlank()) {
                throw new BadRequestException("Razorpay did not return an order ID");
            }

            Payment payment = existingPayment != null
                    ? existingPayment
                    : Payment.builder().appointment(appointment).build();

            payment.setRazorpayOrderId(orderId);
            payment.setAmount(appointment.getAmount());
            payment.setCurrency("INR");
            payment.setStatus(PaymentStatus.PENDING);
            payment = paymentRepository.save(payment);

            return PaymentOrderResponse.from(payment, razorpayKeyId);
        } catch (RazorpayException ex) {
            throw new BadRequestException("Unable to create Razorpay order: " + ex.getMessage());
        }
    }

    private void ensurePatientOwnsAppointment(UserPrincipal principal, Appointment appointment) {
        if (!"ROLE_PATIENT".equals(principal.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .findFirst()
                .orElse(null))
                || !appointment.getPatient().getUser().getId().equals(principal.getId())) {
            throw new BadRequestException("You are not authorized to pay for this appointment");
        }
    }
}
