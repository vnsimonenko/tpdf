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
    
    public List<TextArea> locate(int x1, int y1, int x2, int y2, SelectedStartegy strategy) {
        switch (strategy) {
            case CONTINUE:
                return locateContinue(x1, y1, x2, y2);
            case EXACTLY:
                return locateExactly(x1, y1, x2, y2);
        }
        throw new IllegalArgumentException();
    }
    
    @Override
    public Collection<? extends TextArea> getTextAreas() {
        return textGrid.values().stream().map(textPointTextAreaMap -> textPointTextAreaMap.values()).flatMap(new Function<Collection<? extends TextArea>, Stream<? extends TextArea>>() {
            @Override
            public Stream<? extends TextArea> apply(Collection<? extends TextArea> textAreas) {
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
    
    private List<TextArea> locateExactly(int x1, int y1, int x2, int y2) {
        Rectangle selectedRectangle = new Rectangle(x1, y1, x2 - x1, y2 - y1 == 0 ? 1 : y2 - y1);
        List<TextArea> areas = new ArrayList<>();
        for (Map<TextPoint, TextArea> ent : textGrid.values()) {
            for (TextArea area : ent.values()) {
                Rectangle textRectangle = new Rectangle(area.getXmin(), area.getYmin(),
                                                               area.getWeight(),
                                                               area.getHeight());
                Rectangle rectangle = selectedRectangle.intersection(textRectangle);
                if (!rectangle.isEmpty()) {
                    areas.add(area);
                }
            }
        }
        return areas;
    }
    
    private List<TextArea> locateContinue(int x1, int y1, int x2, int y2) {
        Rectangle selectedRectangle = new Rectangle(x1, y1, x2 - x1, y2 - y1 == 0 ? 1 : y2 - y1);
        Rectangle extendedRectangle = new Rectangle(x1, y1, Integer.MAX_VALUE, y2 - y1 == 0 ? 1 : y2 - y1);
        int maxY1 = 0, maxY2 = y2;
        List<TextArea> areas = new ArrayList<>();
        for (Map<TextPoint, TextArea> ent : textGrid.values()) {
            for (TextArea area : ent.values()) {
                Rectangle textRectangle = new Rectangle(area.getXmin(), area.getYmin(),
                                                               area.getWeight(),
                                                               area.getHeight());
                Rectangle rectangle = extendedRectangle.intersection(textRectangle);
                if (!rectangle.isEmpty()) {
                    areas.add(area);
                    maxY1 = Math.max(area.getYmin(), maxY1);
                }
            }
        }
        List<TextArea> excludedAreas = new ArrayList<>();
        Rectangle excludedRectangle = new Rectangle(x2 + 1, maxY1, Integer.MAX_VALUE, maxY2 - maxY1);
        for (Map<TextPoint, TextArea> ent : textGrid.values()) {
            for (TextArea area : ent.values()) {
                Rectangle textRectangle = new Rectangle(area.getXmin(), area.getYmin(),
                                                               area.getWeight(),
                                                               area.getHeight());
                Rectangle exlRectangle = excludedRectangle.intersection(textRectangle);
                Rectangle selRectangle = selectedRectangle.intersection(textRectangle);
                if (!exlRectangle.isEmpty() && selRectangle.isEmpty()) {
                    excludedAreas.add(area);
                }
            }
        }
        areas.removeAll(excludedAreas);
        return areas;
    }
}
