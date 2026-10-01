package com.stays.reservation;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ReservationController {
    private final ReservationService reservations;
    private final ReservationTransactions transactions;
    private final InventoryRepository inventory;
    private final ReservationJourneyRepository journeys;

    public ReservationController(
            ReservationService reservations,
            ReservationTransactions transactions,
            InventoryRepository inventory,
            ReservationJourneyRepository journeys) {
        this.reservations = reservations;
        this.transactions = transactions;
        this.inventory = inventory;
        this.journeys = journeys;
    }

    @GetMapping("/reservations")
    public List<Reservation> history(@RequestParam(name = "email") String email) {
        return transactions.findByEmail(email);
    }

    @GetMapping("/reservations/{id}")
    public Reservation find(@PathVariable("id") UUID id) {
        return transactions.find(id);
    }

    @PostMapping("/reservations")
    public ResponseEntity<Reservation> book(@Valid @RequestBody ReservationRequest request) {
        return ResponseEntity.ok(reservations.book(request));
    }

    @PostMapping("/reservation-journeys/events")
    public ResponseEntity<Void> trackJourney(@Valid @RequestBody JourneyEventRequest event) {
        journeys.track(event);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/reservations/{id}")
    public Reservation cancel(@PathVariable("id") UUID id) {
        return reservations.cancel(id);
    }

    @PutMapping("/admin/inventory")
    public ResponseEntity<Void> changeInventory(@Valid @RequestBody InventoryDraft draft) {
        inventory.changeTotal(draft);
        return ResponseEntity.noContent().build();
    }
}
