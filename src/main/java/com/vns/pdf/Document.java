package com.vns.pdf;

import java.awt.Image;
import java.io.IOException;

public interface Document {
    TextLocation getTextLocation(int page);
    
    TextLocation getAnnotationLocation(int page);
    
    int getPageAmount();
    
    Image renderImage(int pageIndex, float scale) throws IOException;
    
    float getWidth(int page);
    
    float getHeight(int page);
    
    Translator getTranslator();
    
    void close() throws IOException;
}
