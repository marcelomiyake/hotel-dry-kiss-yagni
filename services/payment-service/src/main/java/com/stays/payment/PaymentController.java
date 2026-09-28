package com.stays.payment;

import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentRepository payments;

    public PaymentController(PaymentRepository payments) {
        this.payments = payments;
    }

    @PostMapping
    public Payment charge(@Valid @RequestBody PaymentRequest request) {
        return payments.charge(request);
    }

    @PutMapping("/{reservationId}/refund")
    public Payment refund(@PathVariable("reservationId") UUID reservationId) {
        return payments.refund(reservationId);
    }
}
