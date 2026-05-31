package com.bookmyshow.models;

import com.bookmyshow.enums.Genre;

public class Movie {
    private final String id;
    private final String title;
    private final int    durationMins;
    private final String language;
    private final Genre  genre;

    public Movie(String id, String title, int durationMins, String language, Genre genre) {
        this.id = id; this.title = title; this.durationMins = durationMins;
        this.language = language; this.genre = genre;
    }

    public String getId()          { return id; }
    public String getTitle()       { return title; }
    public int    getDurationMins(){ return durationMins; }
    public String getLanguage()    { return language; }
    public Genre  getGenre()       { return genre; }
}
