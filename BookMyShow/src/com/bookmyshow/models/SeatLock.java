package com.bookmyshow.models;

import java.time.LocalDateTime;
import java.util.List;

public class SeatLock {
    private final String        lockId;
    private final String        showId;
    private final List<String>  seatIds;
    private final String        userId;
    private final LocalDateTime expiresAt;

    public SeatLock(String lockId, String showId, List<String> seatIds,
                    String userId, int ttlMinutes) {
        this.lockId    = lockId;
        this.showId    = showId;
        this.seatIds   = List.copyOf(seatIds);
        this.userId    = userId;
        this.expiresAt = LocalDateTime.now().plusMinutes(ttlMinutes);
    }

    public boolean isExpired() { return LocalDateTime.now().isAfter(expiresAt); }

    public String       getLockId()    { return lockId; }
    public String       getShowId()    { return showId; }
    public List<String> getSeatIds()   { return seatIds; }
    public String       getUserId()    { return userId; }
    public LocalDateTime getExpiresAt(){ return expiresAt; }
}
