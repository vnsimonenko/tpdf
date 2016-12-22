package com.vns.pdf.domain;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "annotation")
public class Annotation {
    @XmlAttribute
    private float x;
    @XmlAttribute
    private float y;
    @XmlAttribute
    private float width;
    @XmlAttribute
    private float height;
    
    @XmlAttribute
    private float destX;
    @XmlAttribute
    private float destY;
    @XmlAttribute
    private int destPage;
    @XmlAttribute
    private float destZoom;
    
    public Annotation() {
    }
    
    public Annotation(float x, float y, float width, float height, float destX, float destY, int destPage, float
                                                                                                                   destZoom) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.destX = destX;
        this.destY = destY;
        this.destPage = destPage;
        this.destZoom = destZoom;
    }
    
    public float getX() {
        return x;
    }
    
    public float getY() {
        return y;
    }
    
    public float getWidth() {
        return width;
    }
    
    public float getHeight() {
        return height;
    }
    
    public float getDestX() {
        return destX;
    }
    
    public float getDestY() {
        return destY;
    }
    
    public int getDestPage() {
        return destPage;
    }
    
    public float getDestZoom() {
        return destZoom;
    }
}
