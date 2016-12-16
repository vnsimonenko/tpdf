package com.vns.pdf.domain;

import java.util.Collections;
import java.util.List;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "page")
public class Page {
    @XmlElement(name = "word")
    private List<Word> words;
    
    @XmlAttribute
    private float width;
    
    @XmlAttribute
    private float height;
    
    public Page() {
    }
    
    public Page(List<Word> words, float width, float height) {
        this.words = words;
        this.width = width;
        this.height = height;
    }
    
    public List<Word> getWords() {
        return words == null ? Collections.emptyList() : words;
    }
    
    public float getWidth() {
        return width;
    }
    
    public float getHeight() {
        return height;
    }
}
