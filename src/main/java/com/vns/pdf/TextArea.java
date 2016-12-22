package com.vns.pdf;

import org.apache.commons.lang3.StringUtils;

public class TextArea {
    private TextPoint leftTop;
    private TextPoint rightBottom;
    private String text;
    private Object tag;
    
    public TextArea(String text, TextPoint leftTop, TextPoint rightBottom) {
        this.leftTop = leftTop;
        this.rightBottom = rightBottom;
        this.text = StringUtils.defaultString(text, "").trim();
    }
    
    public boolean isEnter(int x, int y) {
        int w = rightBottom.x - leftTop.x;
        int h = rightBottom.y - leftTop.y;
        return leftTop.x <= x && x < leftTop.x + w && leftTop.y <= y && y < leftTop.y + h;
    }
    
    public int getXmin() {
        return leftTop.x;
    }
    
    public int getXmax() {
        return rightBottom.x;
    }
    
    public int getYmin() {
        return leftTop.y;
    }
    
    public int getYmax() {
        return rightBottom.y;
    }
    
    public int getWeight() {
        return getXmax() - getXmin();
    }
    
    public int getHeight() {
        return getYmax() - getYmin();
    }
    
    public String getText() {
        return text;
    }
    
    public Object getTag() {
        return tag;
    }
    
    public void setTag(Object tag) {
        this.tag = tag;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TextArea)) return false;
        
        TextArea that = (TextArea) o;
        
        if (leftTop != null ? !leftTop.equals(that.leftTop) : that.leftTop != null) return false;
        return rightBottom != null ? rightBottom.equals(that.rightBottom) : that.rightBottom == null;
    }
    
    @Override
    public int hashCode() {
        int result = leftTop != null ? leftTop.hashCode() : 0;
        result = 31 * result + (rightBottom != null ? rightBottom.hashCode() : 0);
        return result;
    }
}
