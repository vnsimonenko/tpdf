package com.vns.pdf;

import com.google.api.client.util.Value;

public enum Phonetic {
    @Value
    NONE("none"),
    @Value
    AM("am"),
    @Value
    BR("br"),
    @Value
    RU("ru");
    
    public final String VALUE;
    
    Phonetic(String value) {
        this.VALUE = value;
    }
    
    @Override
    public String toString() {
        return VALUE;
    }
}
