package com.vns.pdf;

import java.awt.Rectangle;

public interface Viewer {
    Rectangle getWindowSize();
    
    void changeImageScale(float scale);
}
