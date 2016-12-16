package com.vns.pdf.gmodel;

import java.io.Serializable;
import java.math.BigDecimal;

public class Entry implements Serializable {
    @com.google.api.client.util.Key("word")
    String word;
    @com.google.api.client.util.Key("score")
    BigDecimal score;
    
    public String getWord() {
        return word;
    }
    
    public void setWord(String word) {
        this.word = word;
    }
    
    public BigDecimal getScore() {
        return score == null ? BigDecimal.ZERO : score;
    }
    
    public void setScore(BigDecimal score) {
        this.score = score;
    }
}
