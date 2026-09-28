package com.stays.hotel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import com.stays.common.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(classes = HotelApplication.class)
@Testcontainers
class HotelRepositoryIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("hotel")
            .withUsername("hotel_app")
            .withPassword("hotel_test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private HotelRepository hotels;

    @Test
    void searchesSeededHotelsAndTheirActiveRoomTypes() {
        assertThat(hotels.findHotels(null)).hasSize(2);
        assertThat(hotels.findHotels("lisBON")).allSatisfy(hotel -> assertThat(hotel.roomTypes()).isNotEmpty());
        assertThat(hotels.findHotels("nowhere")).isEmpty();
        assertThat(hotels.findHotel(java.util.UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .roomTypes()).hasSize(2);
    }

    @Test
    void managesHotelsAndRoomTypesAndReturnsNotFoundForInactiveRecords() {
        Hotel created = hotels.createHotel(hotelDraft("Casa Test"));
        UUID hotelId = created.id();
        assertThat(created.city()).isEqualTo("Lisbon");

        Hotel updated = hotels.updateHotel(hotelId, hotelDraft("Casa Updated"));
        assertThat(updated.name()).isEqualTo("Casa Updated");

        RoomType room = hotels.createRoomType(hotelId, new RoomTypeDraft("Suite", "Two beds", 4, 3));
        UUID roomId = room.id();
        assertThat(room.totalInventory()).isEqualTo(3);
        assertThat(hotels.findHotel(hotelId).roomTypes()).contains(room);

        RoomType updatedRoom = hotels.updateRoomType(roomId, new RoomTypeDraft("Family suite", "Two beds", 4, 4));
        assertThat(updatedRoom.name()).isEqualTo("Family suite");
        hotels.removeRoomType(roomId);
        assertThat(hotels.findHotel(hotelId).roomTypes()).isEmpty();
        RoomTypeDraft hiddenDraft = new RoomTypeDraft("Hidden", "Details", 2, 1);
        assertThatThrownBy(() -> hotels.updateRoomType(roomId, hiddenDraft))
                .isInstanceOf(ApiException.class);

        hotels.removeHotel(hotelId);
        assertThat(hotels.findHotels("Casa Updated")).isEmpty();
        assertThatThrownBy(() -> hotels.findHotel(hotelId)).isInstanceOf(ApiException.class);
        RoomTypeDraft replacementDraft = new RoomTypeDraft("Suite", "Details", 2, 1);
        assertThatThrownBy(() -> hotels.createRoomType(hotelId, replacementDraft))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> hotels.removeRoomType(roomId)).isInstanceOf(ApiException.class);
    }

    private HotelDraft hotelDraft(String name) {
        return new HotelDraft(name, "Lisbon", "Alcântara", "Rua Test 1", "Portugal", "A test hotel.",
                "/images/test.webp", "A test hotel", 8.5);
    }
}
