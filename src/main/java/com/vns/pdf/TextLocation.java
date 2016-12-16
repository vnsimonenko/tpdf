package com.vns.pdf;

import java.util.Collection;
import java.util.List;

public interface TextLocation {
    TextArea locate(int x, int y);
    
    List<TextArea> locate(int x1, int y1, int x2, int y2);
    
    Collection<TextArea> getTextAreas();
    
    TextArea register(String text, int x, int y, int width, int height) throws InstantiationException, IllegalAccessException;
    
    TextArea register(String text, float x, float y, float width, float height) throws InstantiationException, IllegalAccessException;
}
