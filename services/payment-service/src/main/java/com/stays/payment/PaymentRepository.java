package com.stays.payment;

import java.util.List;
import java.util.UUID;

import com.stays.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PaymentRepository {
    private final JdbcTemplate jdbc;

    public PaymentRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Payment charge(PaymentRequest request) {
        jdbc.update(
                "INSERT INTO payments.transactions (reservation_id, amount, guest_email, status) "
                        + "VALUES (?, ?, ?, 'PAID') ON CONFLICT (reservation_id) DO NOTHING",
                request.reservationId(), request.amount(), request.guestEmail().trim().toLowerCase());
        Payment payment = findByReservation(request.reservationId());
        if (payment.amount().compareTo(request.amount()) != 0) {
            throw new ApiException(HttpStatus.CONFLICT, "payment_conflict", "The reservation already has a different payment amount.");
        }
        return payment;
    }

    public Payment refund(UUID reservationId) {
        jdbc.update(
                "UPDATE payments.transactions SET status = 'REFUNDED' "
                        + "WHERE reservation_id = ? AND status = 'PAID'",
                reservationId);
        Payment payment = findByReservation(reservationId);
        if (!"REFUNDED".equals(payment.status())) {
            throw new ApiException(HttpStatus.CONFLICT, "payment_not_refundable", "The payment cannot be refunded.");
        }
        return payment;
    }

    private Payment findByReservation(UUID reservationId) {
        List<Payment> payments = jdbc.query(
                "SELECT id, reservation_id, amount, status, created_at FROM payments.transactions WHERE reservation_id = ?",
                (result, row) -> new Payment(
                        result.getObject("id", UUID.class),
                        result.getObject("reservation_id", UUID.class),
                        result.getBigDecimal("amount"),
                        result.getString("status"),
                        result.getTimestamp("created_at").toInstant()),
                reservationId);
        if (payments.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "payment_not_found", "Payment not found.");
        }
        return payments.getFirst();
    }
}
