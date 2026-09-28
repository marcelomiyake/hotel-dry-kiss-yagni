package com.stays.rate;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RateController {
    private final RateRepository rates;

    public RateController(RateRepository rates) {
        this.rates = rates;
    }

    @GetMapping("/rates/quote")
    public RateQuote quote(
            @RequestParam(name = "roomTypeId") UUID roomTypeId,
            @RequestParam(name = "checkIn") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam(name = "checkOut") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut) {
        return rates.quote(roomTypeId, checkIn, checkOut);
    }

    @PutMapping("/admin/rates")
    public NightlyRate setRate(@Valid @RequestBody RateDraft draft) {
        return rates.setRate(draft);
    }

    @PostMapping("/admin/rates/schedule")
    public ResponseEntity<Void> createSchedule(@Valid @RequestBody RateSchedule schedule) {
        rates.createSchedule(schedule);
        return ResponseEntity.noContent().build();
    }
}
