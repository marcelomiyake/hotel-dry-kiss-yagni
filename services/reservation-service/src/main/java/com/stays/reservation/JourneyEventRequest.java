package com.stays.reservation;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record JourneyEventRequest(
        @NotNull UUID journeyId,
        @NotNull JourneyEventType eventType,
        @NotNull JourneyScreen screen) {
}
