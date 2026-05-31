package com.bookmyshow.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Screen {
    private final String id;
    private final String name;
    private final Seat[][] seatLayout;
    private final List<Show> shows = new ArrayList<>();

    public Screen(String id, String name, Seat[][] seatLayout) {
        this.id = id; this.name = name; this.seatLayout = seatLayout;
    }

    public synchronized void addShow(Show show) {
        shows.add(show);
    }

    public synchronized List<Show> getShows() {
        return Collections.unmodifiableList(new ArrayList<>(shows));
    }

    public String   getId()         { return id; }
    public String   getName()       { return name; }
    public Seat[][] getSeatLayout() { return seatLayout; }
}
