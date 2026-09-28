package com.stays.reservation;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record Reservation(
        UUID id,
        UUID hotelId,
        UUID roomTypeId,
        String hotelName,
        String city,
        String district,
        String imagePath,
        String imageAlt,
        String roomTypeName,
        LocalDate checkIn,
        LocalDate checkOut,
        int rooms,
        int guests,
        String guestName,
        String guestEmail,
        BigDecimal total,
        String status,
        UUID paymentId,
        Instant createdAt) {
}
