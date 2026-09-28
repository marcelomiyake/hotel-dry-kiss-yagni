package com.stays.rate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.stays.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RateRepository {
    private final JdbcTemplate jdbc;

    public RateRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public RateQuote quote(UUID roomTypeId, LocalDate checkIn, LocalDate checkOut) {
        long nights = checkIn == null || checkOut == null ? 0 : checkOut.toEpochDay() - checkIn.toEpochDay();
        if (nights < 1 || nights > 365) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_dates", "Choose a stay between 1 and 365 nights.");
        }
        List<NightlyRate> rates = jdbc.query(
                "SELECT rate_date, amount FROM nightly_rates.rates "
                        + "WHERE room_type_id = ? AND rate_date >= ? AND rate_date < ? ORDER BY rate_date",
                (result, row) -> new NightlyRate(result.getDate("rate_date").toLocalDate(), result.getBigDecimal("amount")),
                roomTypeId,
                checkIn,
                checkOut);
        if (rates.size() != nights) {
            throw new ApiException(HttpStatus.NOT_FOUND, "rate_not_found", "No rate is available for every night.");
        }
        BigDecimal total = rates.stream().map(NightlyRate::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new RateQuote(roomTypeId, rates, total);
    }

    public NightlyRate setRate(RateDraft draft) {
        jdbc.update(
                "INSERT INTO nightly_rates.rates (room_type_id, rate_date, amount) VALUES (?, ?, ?) "
                        + "ON CONFLICT (room_type_id, rate_date) DO UPDATE SET amount = excluded.amount",
                draft.roomTypeId(), draft.date(), draft.amount());
        return new NightlyRate(draft.date(), draft.amount());
    }

    public void createSchedule(RateSchedule schedule) {
        jdbc.update(
                "INSERT INTO nightly_rates.rates (room_type_id, rate_date, amount) "
                        + "SELECT ?, rate_date::date, round(? * CASE WHEN extract(isodow FROM rate_date) IN (5, 6) "
                        + "THEN 1.15 ELSE 1 END, 2) "
                        + "FROM generate_series(current_date, current_date + 730, interval '1 day') rate_date "
                        + "ON CONFLICT (room_type_id, rate_date) DO UPDATE SET amount = excluded.amount",
                schedule.roomTypeId(), schedule.baseRate());
    }
}
