package com.stays.hotel;

import java.net.URI;
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
public class HotelController {
    private final HotelRepository hotels;

    public HotelController(HotelRepository hotels) {
        this.hotels = hotels;
    }

    @GetMapping("/hotels")
    public List<Hotel> findHotels(@RequestParam(name = "destination", required = false) String destination) {
        return hotels.findHotels(destination);
    }

    @GetMapping("/hotels/{id}")
    public Hotel findHotel(@PathVariable("id") UUID id) {
        return hotels.findHotel(id);
    }

    @PostMapping("/admin/hotels")
    public ResponseEntity<Hotel> createHotel(@Valid @RequestBody HotelDraft draft) {
        Hotel created = hotels.createHotel(draft);
        return ResponseEntity.created(URI.create("/api/hotels/" + created.id())).body(created);
    }

    @PutMapping("/admin/hotels/{id}")
    public Hotel updateHotel(@PathVariable("id") UUID id, @Valid @RequestBody HotelDraft draft) {
        return hotels.updateHotel(id, draft);
    }

    @DeleteMapping("/admin/hotels/{id}")
    public ResponseEntity<Void> removeHotel(@PathVariable("id") UUID id) {
        hotels.removeHotel(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/admin/hotels/{hotelId}/room-types")
    public ResponseEntity<RoomType> createRoomType(
            @PathVariable("hotelId") UUID hotelId,
            @Valid @RequestBody RoomTypeDraft draft) {
        RoomType created = hotels.createRoomType(hotelId, draft);
        return ResponseEntity.created(URI.create("/api/hotels/" + hotelId)).body(created);
    }

    @PutMapping("/admin/room-types/{id}")
    public RoomType updateRoomType(@PathVariable("id") UUID id, @Valid @RequestBody RoomTypeDraft draft) {
        return hotels.updateRoomType(id, draft);
    }

    @DeleteMapping("/admin/room-types/{id}")
    public ResponseEntity<Void> removeRoomType(@PathVariable("id") UUID id) {
        hotels.removeRoomType(id);
        return ResponseEntity.noContent().build();
    }
}
