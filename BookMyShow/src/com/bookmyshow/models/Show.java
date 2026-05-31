package com.bookmyshow.models;

import com.bookmyshow.enums.SeatStatus;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Show {
    private final String        id;
    private final Movie         movie;
    private final Screen        screen;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final Map<String, SeatStatus> seatStatusMap = new ConcurrentHashMap<>();

    public Show(String id, Movie movie, Screen screen, LocalDateTime startTime) {
        this.id        = id;
        this.movie     = movie;
        this.screen    = screen;
        this.startTime = startTime;
        this.endTime   = startTime.plusMinutes(movie.getDurationMins());

        // Initialize all seats as AVAILABLE
        for (Seat[] row : screen.getSeatLayout())
            for (Seat seat : row)
                seatStatusMap.put(seat.getId(), SeatStatus.AVAILABLE);
    }

    public SeatStatus getSeatStatus(String seatId) {
        return seatStatusMap.getOrDefault(seatId, SeatStatus.AVAILABLE);
    }

    public void updateSeatStatus(String seatId, SeatStatus status) {
        seatStatusMap.put(seatId, status);
    }

    public String        getId()            { return id; }
    public Movie         getMovie()         { return movie; }
    public Screen        getScreen()        { return screen; }
    public LocalDateTime getStartTime()     { return startTime; }
    public LocalDateTime getEndTime()       { return endTime; }
    public Map<String, SeatStatus> getSeatStatusMap() { return seatStatusMap; }
}
