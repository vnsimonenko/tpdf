package com.vns.pdf.impl;

import com.vns.pdf.TextArea;
import com.vns.pdf.TextLocation;
import com.vns.pdf.TextPoint;
import com.vns.pdf.Utils;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TextLocationImpl implements TextLocation {
    
    private final static Logger LOGGER = LoggerFactory.getLogger(TextLocationImpl.class);
    
    private int stepWidth = 10;
    private int stepHeigt = 10;
    private Map<TextPoint, Map<TextPoint, TextArea>> textGrid;
    
    public TextLocationImpl() {
        init();
    }
    
    private static TextArea cutText(float x1, float y1, float x2, float y2, List<TextArea> areas) {
        String s = "";
        for (TextArea ta : areas) {
            int l1 = ta.getXmin() > x1 ? 0 : (int) ((x1 - ta.getXmin()) / ta.getWeight() * ta.getText().length());
            int l2 = ta.getXmax() < x2 ? 0 : (int) ((ta.getXmax() - x2) / ta.getWeight() * ta.getText().length());
            s += ta.getText().substring(l1, ta.getText().length() - l2) + " ";
        }
        return new TextArea(s, new TextPoint((int) x1, (int) y1), new TextPoint((int) x2, (int) y2));
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
    
    public List<TextArea> locate(int x1, int y1, int x2, int y2, TextPoint firstXY, SelectedStartegy strategy) {
        switch (strategy) {
            case FRAMEOUT:
                return locateFrameOut(x1, y1, x2, y2, firstXY);
            case FRAMEIN:
                return locateFrameIn(x1, y1, x2, y2);
            case CUT:
                return locateCut(x1, y1, x2, y2);
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
    
    private List<TextArea> locateCut(int x1, int y1, int x2, int y2) {
        List<TextArea> areas = locateFrameIn(x1, y1, x2, y2);
        System.out.println(areas.size());
        return Arrays.asList(cutText(x1, y1, x2, y2, areas));
    }
    
    private List<TextArea> locateFrameIn(int x1, int y1, int x2, int y2) {
        Rectangle selectedRectangle = new Rectangle(x1, y1, x2 - x1, y2 - y1 == 0 ? 1 : y2 - y1);
        Set<TextArea> areas = new LinkedHashSet<>();
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
        return new ArrayList<>(areas);
    }
    
    private List<TextArea> locateFrameOut(int x1, int y1, int x2, int y2, TextPoint firstXY) {
        Rectangle selectedRectangle = new Rectangle(x1, y1, x2 - x1, y2 - y1 == 0 ? 1 : y2 - y1);
        Rectangle extendedRectangle = new Rectangle(0, y1, Integer.MAX_VALUE, y2 - y1 == 0 ? 1 : y2 - y1);
        int maxY = 0;
        int minY = y2;
        Set<TextArea> areas = new LinkedHashSet<>();
        for (Map<TextPoint, TextArea> ent : textGrid.values()) {
            for (TextArea area : ent.values()) {
                Rectangle textRectangle = new Rectangle(area.getXmin(), area.getYmin(),
                                                               area.getWeight(),
                                                               area.getHeight());
                Rectangle rectangle = extendedRectangle.intersection(textRectangle);
                if (!rectangle.isEmpty()) {
                    areas.add(area);
                    maxY = Math.max(area.getYmin(), maxY);
                    minY = Math.min(area.getYmax(), minY);
                }
            }
        }
        
        List<TextArea> excludedAreas = new LinkedList<>();
        Rectangle excludedRectangle1 = new Rectangle(0, y1, x1 - 1, minY - y1);
        int x12 = x1 < firstXY.x ? firstXY.x : x1;
        Rectangle excludedRectangle12 = new Rectangle(0, y1, x12 - 1, minY - y1);
        Rectangle excludedRectangle2 = new Rectangle(x2 + 1, maxY, Integer.MAX_VALUE, y2 - maxY);
        int x22 = x2 < firstXY.x ? x2 : x1;
        Rectangle excludedRectangle22 = new Rectangle(x22 + 1, maxY, Integer.MAX_VALUE, y2 - maxY);
        for (TextArea area : areas) {
            Rectangle textRectangle = new Rectangle(area.getXmin(), area.getYmin(),
                                                           area.getWeight(),
                                                           area.getHeight());
            Rectangle exlRectangle1 = excludedRectangle1.intersection(textRectangle);
            Rectangle exlRectangle2 = excludedRectangle2.intersection(textRectangle);
            Rectangle exlRectangle12 = excludedRectangle12.intersection(textRectangle);
            Rectangle exlRectangle22 = excludedRectangle22.intersection(textRectangle);
            Rectangle selRectangle = selectedRectangle.intersection(textRectangle);
            if (!exlRectangle1.isEmpty() && selRectangle.isEmpty()) {
                excludedAreas.add(area);
            }
            if (!exlRectangle2.isEmpty() && selRectangle.isEmpty()) {
                excludedAreas.add(area);
            }
            if (area.getYmax() <= (y2 - 5) && !exlRectangle12.isEmpty()) {
                excludedAreas.add(area);
            }
            if (area.getYmin() > minY && firstXY.x > x1 && !exlRectangle22.isEmpty()) {
                excludedAreas.add(area);
            }
        }
        areas.removeAll(excludedAreas);
        return new ArrayList<>(areas);
    }
}
