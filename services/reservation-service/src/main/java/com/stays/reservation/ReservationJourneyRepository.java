package com.stays.reservation;

import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ReservationJourneyRepository {
    private final JdbcTemplate jdbc;

    public ReservationJourneyRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void track(JourneyEventRequest event) {
        switch (event.eventType()) {
            case STARTED -> start(event);
            case SCREEN_VIEWED -> updateScreen(event);
            case ABANDONED -> abandon(event);
        }
    }

    public void complete(UUID journeyId) {
        if (journeyId == null) {
            return;
        }
        jdbc.update("""
                INSERT INTO reservations.reservation_journeys
                    (id, current_screen, status, started_at, last_activity_at, ended_at)
                VALUES (?, 'CHECKOUT', 'COMPLETED', now(), now(), now())
                ON CONFLICT (id) DO UPDATE
                SET current_screen = 'CHECKOUT', status = 'COMPLETED',
                    last_activity_at = now(), ended_at = now()
                """, journeyId);
    }

    private void start(JourneyEventRequest event) {
        jdbc.update("""
                INSERT INTO reservations.reservation_journeys (id, current_screen, status)
                VALUES (?, ?, 'STARTED')
                ON CONFLICT (id) DO NOTHING
                """, event.journeyId(), event.screen().name());
    }

    private void updateScreen(JourneyEventRequest event) {
        jdbc.update("""
                INSERT INTO reservations.reservation_journeys AS journey (id, current_screen, status)
                VALUES (?, ?, 'STARTED')
                ON CONFLICT (id) DO UPDATE
                SET current_screen = EXCLUDED.current_screen, last_activity_at = now()
                WHERE journey.status = 'STARTED'
                """, event.journeyId(), event.screen().name());
    }

    private void abandon(JourneyEventRequest event) {
        jdbc.update("""
                INSERT INTO reservations.reservation_journeys AS journey
                    (id, current_screen, status, ended_at)
                VALUES (?, ?, 'ABANDONED', now())
                ON CONFLICT (id) DO UPDATE
                SET current_screen = EXCLUDED.current_screen, status = 'ABANDONED',
                    last_activity_at = now(), ended_at = now()
                WHERE journey.status = 'STARTED'
                """, event.journeyId(), event.screen().name());
    }
}
