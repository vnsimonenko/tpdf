package com.vns.pdf;

import com.google.api.client.util.Value;

public enum Language {
    @Value
    RU("ru"),
    @Value
    EN("en"),
    @Value
    UA("uk");
    
    public final String GOOGLE;
    
    Language(String google) {
        this.GOOGLE = google;
    }
}
