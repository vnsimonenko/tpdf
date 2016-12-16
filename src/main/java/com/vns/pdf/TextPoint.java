package com.vns.pdf;

public class TextPoint {
    public final int x;
    public final int y;
    
    public TextPoint(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TextPoint)) return false;
        
        TextPoint that = (TextPoint) o;
        
        if (x != that.x) return false;
        return y == that.y;
    }
    
    @Override
    public int hashCode() {
        int result = x;
        result = 31 * result + y;
        return result;
    }
}
