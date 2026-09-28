package com.stays.hotel;

import java.util.UUID;

public record RoomType(
        UUID id,
        UUID hotelId,
        String name,
        String details,
        int maxGuests,
        int totalInventory) {
}
