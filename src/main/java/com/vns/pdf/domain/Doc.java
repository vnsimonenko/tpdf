package com.vns.pdf.domain;

import java.util.Collections;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "doc")
public class Doc {
    @XmlElement(name = "page")
    private List<Page> pages;
    @XmlElement(name = "bookmarks")
    private List<Annotation> bookmarks;
    
    public Doc() {
    }
    
    public Doc(List<Page> pages, List<Annotation> bookmarks) {
        this.pages = pages;
        this.bookmarks = bookmarks;
    }
    
    
    public List<Page> getPages() {
        return pages;
    }
    
    public List<Annotation> getBookmarks() {
        return bookmarks == null ? Collections.emptyList() : bookmarks;
    }
}
