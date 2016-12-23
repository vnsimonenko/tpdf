package com.vns.pdf.impl;

import com.vns.pdf.TextArea;
import com.vns.pdf.TextLocation;
import com.vns.pdf.Translator;
import com.vns.pdf.Viewer;
import com.vns.pdf.domain.Annotation;
import com.vns.pdf.gmodel.Dic;
import com.vns.pdf.gmodel.Dics;
import com.vns.pdf.gmodel.Entry;
import com.vns.pdf.gmodel.Trans;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import org.apache.commons.lang3.StringUtils;

public class ImageViewer extends JPanel implements Viewer, Translator.TranslatorEvent {
    
    private final static int PERIOD = 100;
    private static Timer timerOfEvents;
    private static ConcurrentSkipListSet<TimerHandler> timerOfEventsQueue;
    
    static {
        
        timerOfEventsQueue = new ConcurrentSkipListSet<>();
        timerOfEvents = new Timer();
        timerOfEvents.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                for (TimerHandler th : timerOfEventsQueue) {
                    if (th.isVisibleViewer()) {
                        th.run();
                    }
                }
                Iterator<TimerHandler> it = timerOfEventsQueue.iterator();
                while (it.hasNext()) {
                    TimerHandler th = it.next();
                    if (!th.isVisibleViewer()) {
                        it.remove();
                    }
                }
            }
        }, 5000, PERIOD);
        
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                timerOfEvents.cancel();
            }
        }));
    }
    
    private float imageWidth;
    private float imageHeight;
    private DocumentViewer documentViewer;
    private JPopupMenu popupMenu;
    private TextArea currentTextScreenArea;
    private Set<TextArea> selectedTextScreenAreas;
    private TimerHandler timerHandler;
    private int pageNumber = -1;
    private int cursorX1;
    private int cursorY1;
    private int cursorY2;
    private int cursorX2;
    private int cursorX;
    private int cursorY;
    private boolean isSelectedAreas = false;
    private float imageScale = 2.0f;
    private boolean isActiveTranslation;
    private AtomicReference<String> englishText = new AtomicReference<>("");
    
    public ImageViewer(int pageNumber, DocumentViewer document) {
        this.pageNumber = pageNumber;
        this.documentViewer = document;
        
        changeImageScale(document.getImageScale());
        
        addMouseListener(new MouseClickedAdapter());
        addMouseMotionListener(new MouseMovedAdapter());
        
        selectedTextScreenAreas = new HashSet<>();
        popupMenu = new JPopupMenu();
        popupMenu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            }
            
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                isActiveTranslation = false;
            }
            
            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });
        timerHandler = new TimerHandler();
    }
    
    public void changeImageScale(float scale) {
        this.imageScale = scale;
        imageWidth = documentViewer.getDocument().getWidth(pageNumber);
        imageHeight = documentViewer.getDocument().getHeight(pageNumber);
        
        Dimension size = getImageSize();
        setPreferredSize(size);
        setMinimumSize(size);
        setMaximumSize(size);
        setSize(size);
    }
    
    @Override
    public Rectangle getWindowSize() {
        return getBounds();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        if (!documentViewer.isVisibleInWindow(this)) {
            g.drawRect(0, 0, (int) (imageWidth * imageScale), (int) (imageHeight * imageScale));
            return;
        }
        
        documentViewer.setIndicatorPage(pageNumber);
        
        try {
            Image image = documentViewer.getDocument().renderImage(pageNumber, imageScale);
            g.drawImage(image, 0, 0, this);
            
            timerOfEventsQueue.add(timerHandler);
            
            g.drawRect(0, 0, (int) (imageWidth * imageScale), (int) (imageHeight * imageScale));
            
            if (getCursor().getType() == Cursor.MOVE_CURSOR) {
                return;
            }
            
            g.setColor(Color.lightGray);
            for (TextArea area : documentViewer.getDocument().getAnnotationLocation(pageNumber).getTextAreas()) {
                drawLine(area.getXmin(), area.getYmax(), area.getXmax(), area.getYmax(), g);
            }

//            for (TextArea area : documentViewer.getDocument().getTextLocation(pageNumber).getTextAreas()) {
//                drawRect(area.getXmin(), area.getYmin(), area.getXmax(), area.getYmax(), g);
//            }
            
            g.setColor(Color.darkGray);
            if (selectedTextScreenAreas.size() > 0) {
                g.setXORMode(Color.black);
                for (TextArea area : selectedTextScreenAreas) {
                    int x1 = (int) (area.getXmin() * imageScale);
                    int x2 = (int) (area.getXmax() * imageScale);
                    int y1 = (int) (area.getYmin() * imageScale);
                    int y2 = (int) (area.getYmax() * imageScale);
                    g.fillRect(x1, y1, x2 - x1, y2 - y1);
                }
            } else if (currentTextScreenArea != null) {
                int x1 = (int) (currentTextScreenArea.getXmin() * imageScale);
                int x2 = (int) (currentTextScreenArea.getXmax() * imageScale);
                int y1 = (int) (currentTextScreenArea.getYmin() * imageScale);
                int y2 = (int) (currentTextScreenArea.getYmax() * imageScale);
                g.setXORMode(Color.black);
                g.fillRect(x1, y1, x2 - x1, y2 - y1);
            }
            if (isSelectedAreas) {
                g.setColor(Color.gray);
                g.drawRect(cursorX1, cursorY1, cursorX - cursorX1, cursorY - cursorY1);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            g.setColor(Color.red);
            int w = SwingUtilities.computeStringWidth(getFontMetrics(getFont()), ex.getMessage());
            g.drawString(ex.getMessage(),
                    (getVisibleRect().width - w) / 2,
                    getVisibleRect().height / 2);
        }
    }

//    void drawRect(double xMin, double yMin, double xMax, double yMax, Graphics g) {
//        xMin *= imageScale;
//        yMin *= imageScale;
//        xMax *= imageScale;
//        yMax *= imageScale;
//        g.drawRect((int) xMin, (int) yMin, (int) (xMax - xMin), (int) (yMax - yMin));
//    }
    
    private void drawLine(double xMin, double yMin, double xMax, double yMax, Graphics g) {
        xMin *= imageScale;
        yMin *= imageScale;
        xMax *= imageScale;
        yMax *= imageScale;
        g.drawLine((int) xMin, (int) yMin, (int) xMax, (int) yMax);
    }
    
    private void showPopupMenu(Collection<TextArea> areas) {
        String s = "";
        for (TextArea area : sortByPos(areas)) {
            if (!s.isEmpty()) s += " ";
            s += area.getText();
        }
        englishText.set(s);
        isActiveTranslation = true;
        if (!documentViewer.isLock()) {
            documentViewer.getDocument().getTranslator().doTranslation(ImageViewer.this);
        }
    }
    
    private List<TextArea> sortByPos(Collection<TextArea> areas) {
        List<TextArea> sortList = new ArrayList<>(areas);
        sortList.sort((t1, t2) -> {
            if (t1.getYmin() < t2.getYmin() && t1.getYmax() < t2.getYmax()) {
                return -1;
            }
            if (t2.getYmin() < t1.getYmin() && t2.getYmax() < t1.getYmax()) {
                return 1;
            }
            if (t1.getXmin() < t2.getXmin() && t1.getXmax() < t2.getXmax()) {
                return -1;
            }
            if (t2.getXmin() < t1.getXmin() && t2.getXmax() < t1.getXmax()) {
                return 1;
            }
            return 0;
        });
        return sortList;
    }
    
    private void calculateSize(JTextArea textArea, String text) {
        FontMetrics fontMetrics = textArea.getFontMetrics(textArea.getFont());
        Dimension sz = Toolkit.getDefaultToolkit().getScreenSize();
        double screenWidth = sz.getWidth() / 3 * 2;
        if (screenWidth < fontMetrics.stringWidth(text)) {
            int w = (int) Math.sqrt(fontMetrics.stringWidth(text) * fontMetrics.getHeight());
            if (w * 5 / 2 < screenWidth) screenWidth = w * 5 / 2;
        }
        
        StringBuilder sb = new StringBuilder();
        String[] ss = text.split("[\n]");
        int row = 0;
        int size[];
        int maxWidth = 0;
        for (String s : ss) {
            if (sb.length() != 0) {
                sb.append("\n");
            }
            size = calculateSizeBySpace(sb, fontMetrics, screenWidth, s.split("[ ]"));
            row += size[0];
            maxWidth = Math.max(maxWidth, size[1]);
        }
        int h = row * fontMetrics.getHeight();
        textArea.setText(sb.toString());
        textArea.setPreferredSize(new Dimension(maxWidth, row * fontMetrics.getHeight()));
    }
    
    private int[] calculateSizeBySpace(StringBuilder sb, FontMetrics fontMetrics, double screenWidth, String[] ss) {
        int lineWidth = 0;
        int maxWidth = 0;
        int row = 1;
        for (String s : ss) {
            int w = fontMetrics.stringWidth(s.trim() + " ");
            if (lineWidth + w < screenWidth) {
                lineWidth += w;
                sb.append(s.trim() + " ");
            } else {
                sb.append((sb.toString().endsWith("\n") ? "" : "\n") + s.trim() + " ");
                lineWidth = w;
                row++;
            }
            if (maxWidth < lineWidth) maxWidth = lineWidth;
        }
        return new int[]{row, maxWidth};
    }
    
    public Dimension getImageSize() {
        return new Dimension((int) (imageWidth * imageScale), (int) (imageHeight * imageScale));
    }
    
    private void changeCursor(int type) {
        setCursor(Cursor.getPredefinedCursor(type));
    }
    
    @Override
    public boolean isActive() {
        return documentViewer.isVisibleInWindow(this) && isActiveTranslation;
    }
    
    @Override
    public String getText() {
        String s = StringUtils.defaultIfBlank(englishText.get(), "");
        return s.substring(0, s.length() < documentViewer.getTranslatedLength() ? s.length() : documentViewer.getTranslatedLength());
    }
    
    @Override
    public void setTranslation(Dics dics) {
        StringBuilder sb = new StringBuilder();
        for (Dic dic : dics.getDics()) {
            if (sb.length() > 0) sb.append("\n");
            sb.append("---");
            sb.append(dic.getPos());
            sb.append("---");
            int row = 0;
            for (Entry ent : dic.getEntries()) {
                sb.append("\n");
                sb.append(ent.getWord());
                row++;
                if (row == documentViewer.getTranslatedRows()) {
                    break;
                }
            }
        }
        if (dics.getDics().isEmpty()) {
            for (Trans tn : dics.getTrans()) {
                if (sb.length() > 0) sb.append("\n");
                sb.append(tn.getTrans());
            }
        }
        
        JTextArea text = new JTextArea();
        text.setLineWrap(false);
        text.setEditable(false);
        calculateSize(text, sb.toString());
        popupMenu.removeAll();
        popupMenu.add(text);
        popupMenu.setVisible(false);
        popupMenu.show(this, cursorX, cursorY + 20);
    }
    
    public class MouseMovedAdapter extends MouseAdapter {
        @Override
        public void mouseMoved(MouseEvent e) {
            cursorX = e.getX();
            cursorY = e.getY();
            TextArea area = documentViewer.getDocument().getTextLocation(pageNumber).locate((int) (e.getX() / imageScale), (int) (e.getY() / imageScale));
            TextArea anntArea = documentViewer.getDocument().getAnnotationLocation(pageNumber).locate((int) (e.getX() / imageScale), (int) (e.getY() / imageScale));
            if (area == null) {
                if (anntArea != null) changeCursor(Cursor.getDefaultCursor().getType());
                if (Math.abs(cursorX2 - cursorX) > 10 || Math.abs(cursorY2 - cursorY) > 10)
                    selectedTextScreenAreas.clear();
                changeCursor(Cursor.DEFAULT_CURSOR);
            } else {
                changeCursor(Cursor.HAND_CURSOR);
                selectedTextScreenAreas.clear();
            }
            if (anntArea != null) {
                changeCursor(Cursor.HAND_CURSOR);
            }
            if (currentTextScreenArea == null || !currentTextScreenArea.equals(area)) {
                currentTextScreenArea = area;
                repaint();
            }
        }
        
        @Override
        public void mouseDragged(MouseEvent e) {
            if (getCursor().getType() == Cursor.MOVE_CURSOR) {
                Point point = new Point(e.getLocationOnScreen().x, e.getLocationOnScreen().y);
                documentViewer.scroll(point.x - cursorX, point.y - cursorY);
                cursorX = point.x;
                cursorY = point.y;
                return;
            }
            
            cursorX = e.getX();
            cursorY = e.getY();
            
            int x1 = cursorX1 <= cursorX ? cursorX1 : cursorX;
            int x2 = cursorX1 <= cursorX ? cursorX : cursorX1;
            int y1 = cursorY1 <= cursorY ? cursorY1 : cursorY;
            int y2 = cursorY1 <= cursorY ? cursorY : cursorY1;
            
            List<TextArea> areas = documentViewer.getDocument().getTextLocation(pageNumber).locate(
                    (int) (x1 / imageScale), (int) (y1 / imageScale),
                    (int) (x2 / imageScale), (int) (y2 / imageScale),
                    e.isControlDown()
                            ? TextLocation.SelectedStartegy.EXACTLY
                            : TextLocation.SelectedStartegy.CONTINUE);
            TextArea area = documentViewer.getDocument().getTextLocation(pageNumber).locate((int) (cursorX / imageScale), (int) (cursorY / imageScale));
            selectedTextScreenAreas.clear();
            selectedTextScreenAreas.addAll(areas);
            if (area != null) selectedTextScreenAreas.add(area);
            currentTextScreenArea = area;
            repaint();
        }
    }
    
    public class MouseClickedAdapter extends MouseAdapter {
        
        @Override
        public void mousePressed(MouseEvent e) {
            isSelectedAreas = true;
            cursorX = e.getX();
            cursorY = e.getY();
            cursorX1 = cursorX;
            cursorY1 = cursorY;
            
            TextArea annotation = documentViewer.getDocument().getAnnotationLocation(pageNumber)
                                          .locate((int) (cursorX / imageScale), (int) (cursorY / imageScale));
            if (annotation != null) {
                currentTextScreenArea = null;
                selectedTextScreenAreas.clear();
                Annotation a = (Annotation) annotation.getTag();
                changeCursor(Cursor.getDefaultCursor().getType());
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        documentViewer.addNavigateHistory(true);
                        documentViewer.showPage(a.getDestPage());
                        float y = imageHeight * imageScale - a.getDestY() * imageScale;
                        documentViewer.scroll(a.getDestX() * imageScale, y);
                        Viewer w = documentViewer.getViewer(a.getDestPage());
                        ((JComponent) w).setCursor(Cursor.getDefaultCursor());
                    }
                });
                return;
            }
            
            TextArea area = documentViewer.getDocument().getTextLocation(pageNumber).locate((int) (cursorX / imageScale), (int) (cursorY / imageScale));
            currentTextScreenArea = area;
            selectedTextScreenAreas.clear();
            if (area != null) selectedTextScreenAreas.add(area);
            if (area == null) {
                if (e.isPopupTrigger()) {
                    setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                    cursorX = e.getLocationOnScreen().x;
                    cursorY = e.getLocationOnScreen().y;
                    return;
                } else {
                    changeCursor(Cursor.CROSSHAIR_CURSOR);
                }
            } else {
                changeCursor(Cursor.getDefaultCursor().getType());
            }
            repaint();
        }
        
        @Override
        public void mouseReleased(MouseEvent e) {
            isSelectedAreas = false;
            cursorX = e.getX();
            cursorY = e.getY();
            cursorX2 = cursorX;
            cursorY2 = cursorY;
            setCursor(Cursor.getDefaultCursor());
            repaint();
            showPopupMenu(selectedTextScreenAreas);
        }
    }
    
    class TimerHandler implements Comparable {
        TextArea prevTextScreenArea;
        long waiting;
        boolean isFirst;
        
        private void reset() {
            prevTextScreenArea = null;
            isFirst = true;
            popupMenu.removeAll();
            popupMenu.setVisible(false);
            waiting = 0;
        }
        
        public void run() {
            if (getCursor().getType() == Cursor.MOVE_CURSOR) {
                return;
            }
            
            if (isSelectedAreas) {
                reset();
                return;
            }
            if (selectedTextScreenAreas.size() > 0) {
                return;
            }
            
            int waiting2 = ImageViewer.this.documentViewer.getTranslatedDelay() / PERIOD;
            if (prevTextScreenArea != null && prevTextScreenArea.equals(currentTextScreenArea)) {
                if (waiting >= waiting2) {
                    if (isFirst) {
                        showPopupMenu(Arrays.asList(currentTextScreenArea));
                        isFirst = false;
                    }
                } else {
                    popupMenu.setVisible(false);
                    isFirst = true;
                    waiting++;
                }
            } else {
                reset();
                prevTextScreenArea = currentTextScreenArea;
            }
        }
        
        public boolean isVisibleViewer() {
            return documentViewer.isVisibleInWindow(ImageViewer.this);
        }
        
        int page() {
            return ImageViewer.this.pageNumber;
        }
        
        @Override
        public int compareTo(Object o) {
            int pageNumber1 = ((TimerHandler) o).page();
            int pageNumber2 = ImageViewer.this.pageNumber;
            return pageNumber1 == pageNumber2 ? 0 : pageNumber1 < pageNumber2 ? -1 : 1;
        }
    }
}