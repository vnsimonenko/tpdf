package com.vns.pdf.domain;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

@XmlRootElement(name = "word")
public class Word {
    @XmlAttribute
    private float xMin;
    @XmlAttribute
    private float yMin;
    @XmlAttribute
    private float xMax;
    @XmlAttribute
    private float yMax;
    @XmlValue
    private String text;
    
    public Word() {
    }
    
    public Word(float xMin, float yMin, float xMax, float yMax, String text) {
        this.xMin = xMin;
        this.yMin = yMin;
        this.xMax = xMax;
        this.yMax = yMax;
        this.text = text;
    }
    
    public float getxMin() {
        return xMin;
    }
    
    public float getyMin() {
        return yMin;
    }
    
    public float getxMax() {
        return xMax;
    }
    
    public float getyMax() {
        return yMax;
    }
    
    public String getText() {
        return text;
    }
    
    public void changeText(String s) {
        text = s;
    }
    
    public void changexMax(float x) {
        xMax = x;
    }
    
}
