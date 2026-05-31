package com.bookmyshow.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Theater {
    private final String id;
    private final String name;
    private final String city;
    private final List<Screen> screens = new ArrayList<>();

    public Theater(String id, String name, String city) {
        this.id = id; this.name = name; this.city = city;
    }

    public synchronized void addScreen(Screen screen) {
        screens.add(screen);
    }

    public synchronized List<Screen> getScreens() {
        return Collections.unmodifiableList(new ArrayList<>(screens));
    }

    public String getId()   { return id; }
    public String getName() { return name; }
    public String getCity() { return city; }
}
