package com.vns.pdf.domain;

import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "doc")
public class Doc {
    @XmlElement(name = "page")
    private List<Page> pages;
    
    public Doc() {
    }
    
    public Doc(List<Page> pages) {
        this.pages = pages;
    }
    
    
    public List<Page> getPages() {
        return pages;
    }
}
