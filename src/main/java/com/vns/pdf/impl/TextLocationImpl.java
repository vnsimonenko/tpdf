package com.vns.pdf.impl;

import com.vns.pdf.TextArea;
import com.vns.pdf.TextLocation;
import com.vns.pdf.TextPoint;
import com.vns.pdf.Utils;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TextLocationImpl implements TextLocation {
    
    private int stepWidth = 10;
    private int stepHeigt = 10;
    private Map<TextPoint, Map<TextPoint, TextArea>> textGrid;
    
    public TextLocationImpl() {
        init();
    }
    
    public TextArea locate(int x, int y) {
        int x1 = x;
        int y1 = y;
        TextPoint gridLeftTop = new TextPoint(x1 - x1 % stepWidth, y1 - y1 % stepHeigt);
        Map<TextPoint, TextArea> textScreenRects = textGrid.get(gridLeftTop);
        if (textScreenRects != null) {
            for (TextArea rect : textScreenRects.values()) {
                if (rect.isEnter(x1, y1)) {
                    return rect;
                }
            }
        }
        return null;
    }
    
    public List<TextArea> locate(int x1, int y1, int x2, int y2) {
        Rectangle selectedRectangle = new Rectangle(x1, y1, x2 - x1, y2 - y1 == 0 ? 1 : y2 - y1);
        List<TextArea> areas = new ArrayList<>();
        for (Map<TextPoint, TextArea> ent : textGrid.values()) {
            for (TextArea area : ent.values()) {
                Rectangle textRectangle = new Rectangle(area.getXmin(), area.getYmin(),
                                                               area.getXmax() - area.getXmin(),
                                                               area.getYmax() - area.getYmin());
                Rectangle rectangle = selectedRectangle.intersection(textRectangle);
                if (!rectangle.isEmpty()) {
                    areas.add(area);
                }
            }
        }
        return areas;
    }
    
    @Override
    public Collection<TextArea> getTextAreas() {
        return textGrid.values().stream().map(textPointTextAreaMap -> textPointTextAreaMap.values()).flatMap(new Function<Collection<TextArea>, Stream<TextArea>>() {
            @Override
            public Stream<TextArea> apply(Collection<TextArea> textAreas) {
                return textAreas.stream();
            }
        }).collect(Collectors.toList());
    }
    
    public TextArea register(String text, float x, float y, float width, float height) throws InstantiationException, IllegalAccessException {
        return register(text, Math.round(x), Math.round(y), Math.round(width), Math.round(height));
    }
    
    public TextArea register(String text, int x, int y, int width, int height) throws InstantiationException, IllegalAccessException {
        Rectangle textRectangle = new Rectangle(x, y, width, height);
        TextPoint textLeftTop = new TextPoint(x, y);
        TextArea textRect = new TextArea(text, textLeftTop, new TextPoint(x + width, y + height));
        for (int x1 = x - x % stepWidth; x1 < x + width; x1 += stepWidth) {
            for (int y1 = y - y % stepHeigt; y1 < y + height; y1 += stepHeigt) {
                Rectangle gridRectangle = new Rectangle(x1, y1, stepWidth, stepHeigt);
                Rectangle rectangle = gridRectangle.intersection(textRectangle);
                if (rectangle.isEmpty()) {
                    break;
                }
                Utils.getMultiplicityValueFromMap(textGrid, new TextPoint(x1, y1), HashMap::new).put(textLeftTop, textRect);
            }
        }
        return textRect;
    }
    
    private void init() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        long capacity1 = Math.round(screenSize.getHeight() / stepHeigt);
        long capacity2 = Math.round(screenSize.getWidth() / stepWidth);
        textGrid = new HashMap<>((int) (capacity1 * capacity2));
    }
}
