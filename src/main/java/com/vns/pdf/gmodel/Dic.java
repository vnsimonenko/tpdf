package com.vns.pdf.gmodel;

import java.io.Serializable;
import java.util.List;

public class Dic implements Serializable {
    @com.google.api.client.util.Key("pos")
    private String pos;
    @com.google.api.client.util.Key("entry")
    private List<Entry> entries;
    
    public String getPos() {
        return pos;
    }
    
    public void setPos(String pos) {
        this.pos = pos;
    }
    
    public List<Entry> getEntries() {
        return entries;
    }
    
    public void setEntries(List<Entry> entries) {
        this.entries = entries;
    }
}
