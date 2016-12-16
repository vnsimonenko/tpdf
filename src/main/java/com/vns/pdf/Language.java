package com.vns.pdf;

public enum Language {
    RU("ru"), EN("en"), UA("uk");
    
    public final String GOOGLE;
    
    Language(String google) {
        this.GOOGLE = google;
    }
}
