package com.vns.pdf.gmodel;

import java.io.Serializable;

public class Trans implements Serializable {
    @com.google.api.client.util.Key("trans")
    private String trans;
    @com.google.api.client.util.Key("orig")
    private String orig;
    
    public String getTrans() {
        return trans;
    }
    
    public String getOrig() {
        return orig;
    }
}
