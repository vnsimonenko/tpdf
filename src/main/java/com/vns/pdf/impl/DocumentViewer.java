package com.vns.pdf.impl;

import com.vns.pdf.ApplicationProperties;
import com.vns.pdf.Document;
import com.vns.pdf.Language;
import com.vns.pdf.TextArea;
import com.vns.pdf.Viewer;
import com.vns.pdf.domain.Annotation;
import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.text.DefaultEditorKit;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.debugger.ui.ExtensionFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.awt.image.ToolkitImage;
import sun.awt.image.URLImageSource;
import static java.awt.AWTEvent.KEY_EVENT_MASK;
import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static java.awt.event.KeyEvent.KEY_PRESSED;
import static java.awt.event.KeyEvent.KEY_RELEASED;
import static java.awt.event.KeyEvent.VK_ESCAPE;
import static java.awt.event.KeyEvent.VK_WINDOWS;
import static java.awt.event.MouseWheelEvent.WHEEL_UNIT_SCROLL;

public class DocumentViewer extends JPanel {
    private static final String PREVIOUS = "previous";
    private static final String NEXT = "next";
    private static final String HIS_PREVIOUS = "his_previous";
    private static final String HIS_NEXT = "his_next";
    private static Logger logger;
    private final HistoryStore historyStore = new HistoryStore();
    private Map<String, JButton> buttonByCommandName = new HashMap<>();
    private JScrollPane imageScrollPane;
    private Map<Integer, Viewer> pageViewer;
    private Document document;
    private BlockingQueue<Viewer> imageBlockingQueue;
    private JFrame frame;
    private int currentPage;
    private float imageScale;
    private JComboBox<String> pagesCombobox;
    private JComboBox<Integer> translatedDelay;
    private JComboBox<Integer> translatedRows;
    private JComboBox<Integer> translatedLength;
    private JComboBox<Language> srcLanguage;
    private JComboBox<Language> trgLanguage;
    private JButton openBookButton;
    private JFileChooser fileChooser;
    private JToolBar toolBar;
    private JCheckBox clipboardCheckBox;
    private Color color = new Color(150, 150, 150);
    private Color origColor;
    private volatile boolean lock;
    private String pdfFilePath;
    private JComboBox<Integer> scale;
    private LinkedBlockingDeque historyPrevDeque = new LinkedBlockingDeque();
    private LinkedBlockingDeque historyNextDeque = new LinkedBlockingDeque();
    private JPanel contentPane;
    private JPanel workingPane;
    private JTextArea messageArea;
    private JSplitPane viewSplitPane;
    private JSplitPane workingSplitPane;
    private Lock mouseWheelMovedLock = new ReentrantLock();
    private static volatile String lastMessage;
    
    private DocumentViewer(JFrame jFrame) throws IllegalAccessException, IOException, InstantiationException {
        super(new BorderLayout());
        this.imageBlockingQueue = new LinkedBlockingQueue<>();
        this.frame = jFrame;
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                Language srcLng = srcLanguage.getItemAt(srcLanguage.getSelectedIndex());
                Language trgLng = srcLanguage.getItemAt(trgLanguage.getSelectedIndex());
                Integer rows = translatedRows.getItemAt(translatedRows.getSelectedIndex());
                int left = workingSplitPane == null ? -1 : workingSplitPane.getDividerLocation();
                int bottom = viewSplitPane == null ? -1 : viewSplitPane.getDividerLocation();
                boolean isCopyingClipbord = clipboardCheckBox.isSelected();
                historyStore.save(pdfFilePath, currentPage, BigDecimal.valueOf(imageScale),
                        getTranslatedDelay(), srcLng, trgLng, rows, left, bottom, isCopyingClipbord);
                if (document != null) document.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
    }
    
    static void createAndShowGUI(String pdfFileName) throws IllegalAccessException, IOException, InstantiationException {
        JFrame frame = new JFrame(pdfFileName);
        Image img = new ToolkitImage(new URLImageSource(DocumentViewer.class.getClassLoader().getResource("images/worlwide.png")));
        frame.setIconImage(img);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        DocumentViewer documentViewer = new DocumentViewer(frame);
        documentViewer.createToolBar();
        
        frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
        frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        frame.setVisible(true);
        try {
            frame.add(documentViewer);
            documentViewer.createViewArea();
            documentViewer.open(pdfFileName);
            frame.pack();
        } finally {
            frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
        
        Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
            private boolean lock;
            
            @Override
            public void eventDispatched(AWTEvent event) {
                KeyEvent keyEvent = (KeyEvent) event;
                if (keyEvent.getID() == KEY_RELEASED && keyEvent.getKeyCode() == VK_ESCAPE
                            && keyEvent.getModifiersEx() == 128) {
                    lock = !lock;
                    documentViewer.lock(lock);
                    documentViewer.imageScrollPane.repaint();
                }
                if (keyEvent.getID() == KEY_PRESSED 
                            && (keyEvent.getModifiersEx() & CTRL_DOWN_MASK) == CTRL_DOWN_MASK
                            && (keyEvent.getKeyCode() & VK_WINDOWS) == VK_WINDOWS
                            && !StringUtils.isBlank(lastMessage)) {
                    copyToClipboard(lastMessage);
                }
            }
        }, KEY_EVENT_MASK);
    }
    
    public static void main(String... args) throws ConfigurationException, IOException, ParseException {
        CommandLine line = new PosixParser().parse(new Options()
                                                           .addOption(OptionAdapter.PDFFILENAME.createOption())
                                                           .addOption(OptionAdapter.WORKING.createOption())
                                                           .addOption(OptionAdapter.SRC.createOption())
                                                           .addOption(OptionAdapter.TRG.createOption()), args);
        
        String pdfFileName = OptionAdapter.PDFFILENAME.getOptionValue(line);
        
        String workingDir = OptionAdapter.WORKING.getOptionValue(line);
        String srcLang = OptionAdapter.SRC.getOptionValue(line);
        String trgLang = OptionAdapter.TRG.getOptionValue(line);
        
        ApplicationProperties.prepareWorkingDir(workingDir, srcLang, trgLang);
        logger = LoggerFactory.getLogger(DocumentViewer.class);
        //String pdfFileName = "/home/vns/workspace/projects/tpdf/docs/itext7buildingblocks-sample.pdf";
        //String pdfFileName = "/home/vns/workspace/projects/trnpdf/docs/iText in Action 2nd Edition.pdf";
        //String pdfFileName = "/home/vns/Desktop/tools/books/scl/programming-in-scala_3nd.pdf";
        logger.info(pdfFileName);
        SwingUtilities.invokeLater(() -> {
            UIManager.put("swing.boldMetal", Boolean.FALSE);
            try {
                createAndShowGUI(pdfFileName);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });
    }
    
    void sendMessage(String msg) {
        if (!StringUtils.isBlank(msg)) {
            messageArea.insert("\n=====\n\n", 0);
            messageArea.insert(msg, 0);
            lastMessage = msg;
            if (clipboardCheckBox.isSelected()) {
                copyToClipboard(lastMessage);  
            }
        }
    }
    
    private void createViewArea() {
        viewSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        viewSplitPane.setOneTouchExpandable(true);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                viewSplitPane.setDividerLocation(1.0);
                DocumentViewer.this.removeComponentListener(this);
            }
        });
        workingSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        workingSplitPane.setOneTouchExpandable(true);
        contentPane = new JPanel();
        contentPane.setLayout(new BorderLayout());
        workingPane = new JPanel();
        workingPane.setLayout(new BorderLayout());
        messageArea = new JTextArea();
        messageArea.setLineWrap(true);
        JPopupMenu messageAreaMenu = new JPopupMenu();
        Action copyAction = new AbstractAction(DefaultEditorKit.copyAction) {
            public void actionPerformed(ActionEvent e) {
                messageArea.copy();
            }
        };
        JMenuItem copyMenuItem = new JMenuItem(copyAction);
        messageAreaMenu.add(copyMenuItem);
        messageArea.setComponentPopupMenu(messageAreaMenu);
                
        JScrollPane messagePane = new JScrollPane(messageArea);
        
        viewSplitPane.setTopComponent(workingPane);
        viewSplitPane.setBottomComponent(messagePane);
        
        workingSplitPane.setTopComponent(contentPane);
        workingSplitPane.setBottomComponent(viewSplitPane);
        add(workingSplitPane, BorderLayout.CENTER);
        workingSplitPane.setDividerLocation(-1);
    }
    
    public boolean isLock() {
        return lock;
    }
    
    private void lock(boolean b) {
        if (origColor == null) {
            origColor = toolBar.getBackground();
        }
        lock = b;
        if (lock) {
            toolBar.setBackground(color);
        } else {
            toolBar.setBackground(origColor);
        }
        imageScrollPane.repaint();
    }
    
    public Document getDocument() {
        return document;
    }
    
    public boolean isVisibleInWindow(Viewer viewer) {
        Rectangle rect = imageScrollPane.getViewport().getViewRect();
        if (JComponent.class.isAssignableFrom(viewer.getClass())) {
            JComponent c = (JComponent) viewer;
            if (c.getBounds().intersects(rect)) {
                return true;
            }
        }
        return false;
    }
    
    public void scroll(int directX, int directY) {
        if (directX != 0) {
            int value = imageScrollPane.getHorizontalScrollBar().getValue();
            imageScrollPane.getHorizontalScrollBar().setValue(value + (directX > 0 ? 1 : -1) * imageScrollPane.getHorizontalScrollBar().getBlockIncrement());
        }
        if (directY != 0) {
            int value = imageScrollPane.getVerticalScrollBar().getValue();
            imageScrollPane.getVerticalScrollBar().setValue(value + (directY > 0 ? 1 : -1) * imageScrollPane.getVerticalScrollBar().getBlockIncrement());
        }
    }
    
    public void scroll(float x, float y) {
        if (x > 0) {
            int value = imageScrollPane.getHorizontalScrollBar().getValue();
            imageScrollPane.getHorizontalScrollBar().setValue((int) (value + x));
        }
        if (y > 0) {
            int value = imageScrollPane.getVerticalScrollBar().getValue();
            imageScrollPane.getVerticalScrollBar().setValue((int) (value + y));
        }
    }
    
    public Viewer getViewer(int page) {
        return pageViewer.get(page);
    }
    
    private void addScalePage(JToolBar toolBar) {
        scale = new JComboBox<>(new Integer[]{25, 50, 75, 100, 125, 150, 175, 200});
        scale.setSelectedIndex(5);
        int sc = scale.getItemAt(scale.getSelectedIndex());
        imageScale = sc / 100.f;
        
        JTextField scaleField = (JTextField) scale.getEditor().getEditorComponent();
        scaleField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                int page = Integer.parseInt(scaleField.getText().replaceAll("[^0-9]", ""));
                if (page > 300) {
                    page = 300;
                }
                scaleField.setText("" + page);
            }
        });
        scale.addActionListener(e -> {
            if (e.getActionCommand() == "comboBoxChanged") {
                if (scale.getSelectedIndex() != -1) {
                    setImageScale(scale.getItemAt(scale.getSelectedIndex()) / 100.0f);
                } else {
                    JTextField scaleField1 = (JTextField) scale.getEditor().getEditorComponent();
                    setImageScale(Integer.parseInt(scaleField1.getText()) / 100.0f);
                }
            }
        });
        
        toolBar.add(new JLabel("scale: "));
        scale.setEditable(true);
        scale.setMaximumSize(new Dimension(100, 100));
        toolBar.add(scale);
        
        toolBar.addSeparator();
    }
    
    private void addNavPages(JToolBar toolBar) {
        pagesCombobox = new JComboBox<>();
        JTextField pageField = (JTextField) pagesCombobox.getEditor().getEditorComponent();
        pageField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                String page = pageField.getText().replaceAll("[^0-9]", "");
                int pageNumber;
                try {
                    pageNumber = Integer.parseInt(page);
                } catch (NumberFormatException ex) {
                    int index = pagesCombobox.getSelectedIndex() == -1 ? 0 : pagesCombobox.getSelectedIndex();
                    pageNumber = Integer.parseInt(pagesCombobox.getItemAt(index));
                }
                if (pageNumber == 0) {
                    pageField.setText("1");
                } else if (pageNumber > document.getPageAmount()) {
                    pageField.setText("" + document.getPageAmount());
                } else {
                    pageField.setText(page);
                }
            }
        });
        pagesCombobox.addActionListener(e -> {
            if (e.getActionCommand() == "comboBoxChanged") {
                if (pagesCombobox.getSelectedIndex() != -1) {
                    showPage(pagesCombobox.getSelectedIndex());
                }
            }
        });
        
        toolBar.add(new JLabel("page: "));
        pagesCombobox.setEditable(true);
        pagesCombobox.setMaximumSize(new Dimension(100, 100));
        toolBar.add(pagesCombobox);
        
        toolBar.addSeparator();
        
        addNavButtons(toolBar);
    }
    
    private void addNavHisPages(JToolBar toolBar) {
        toolBar.addSeparator();
        
        ActionListener listener = e -> {
            if (e.getActionCommand().equals(HIS_PREVIOUS)) {
                navigateHistory(true);
                pagesCombobox.setEditable(true);
            } else if (e.getActionCommand().equals(HIS_NEXT)) {
                navigateHistory(false);
            }
        };
        
        JButton button = makeNavigationButton("Back24", HIS_PREVIOUS,
                "Previous history",
                "Previous history", listener, null);
        button.setEnabled(false);
        toolBar.add(button);
        button = makeNavigationButton("Forward24", HIS_NEXT,
                "Next history",
                "Next history", listener, null);
        button.setEnabled(false);
        toolBar.add(button);
        
        Toolkit kit = Toolkit.getDefaultToolkit();
        kit.addAWTEventListener(event -> {
            if (event instanceof KeyEvent) {
                KeyEvent e = (KeyEvent) event;
                if (e.getID() == KEY_PRESSED && (e.getKeyCode() == KeyEvent.VK_Z || e.getKeyCode() == KeyEvent.VK_X)) {
                    pagesCombobox.setEditable(false);
                    try {
                        navigateHistory(e.getKeyCode() == KeyEvent.VK_Z);
                    } finally {
                        pagesCombobox.setEditable(true);
                    }
                }
            }
        }, AWTEvent.KEY_EVENT_MASK);
    }
    
    private void addNavButtons(JToolBar toolBar) {
        JButton button = null;
        ActionListener listener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand().equals("previous")) {
                    if (currentPage > 0) {
                        if (pagesCombobox != null) {
                            pagesCombobox.setSelectedIndex(currentPage - 1);
                        }
                    }
                } else if (e.getActionCommand().equals("next")) {
                    if (currentPage < document.getPageAmount() - 1) {
                        pagesCombobox.setSelectedIndex(currentPage + 1);
                    }
                }
            }
        };
        
        button = makeNavigationButton("Back24", PREVIOUS,
                "Previous page",
                "Previous page", listener, null);
        toolBar.add(button);
        
        button = makeNavigationButton("Forward24", NEXT,
                "Next page",
                "Next", listener, null);
        toolBar.add(button);
    }
    
    private JButton makeNavigationButton(String imageName,
                                         String actionCommand,
                                         String toolTipText,
                                         String altText,
                                         ActionListener actionListener,
                                         KeyListener keyListener) {
        String imgLocation = "images/" + imageName + ".gif";
        URL imageURL = getClass().getClassLoader().getResource(imgLocation);
        
        JButton button = new JButton();
        buttonByCommandName.put(actionCommand, button);
        button.setActionCommand(actionCommand);
        button.setToolTipText(toolTipText);
        if (actionListener != null) button.addActionListener(actionListener);
        if (keyListener != null) button.addKeyListener(keyListener);
        
        if (imageURL != null) {                      //image found
            button.setIcon(new ImageIcon(imageURL, altText));
        } else {                                     //no image found
            button.setText(altText);
            System.err.println("Resource not found: " + imgLocation);
        }
        
        return button;
    }
    
    private void addTranslatedDelay(JToolBar toolBar) {
        List<Integer> delays = ApplicationProperties.KEY.TranslatedDelay.asIntegerList(500, 750, 1000, 1250, 1500, 1750, 2000);
        translatedDelay = new JComboBox<>(delays.toArray(new Integer[0]));
        translatedDelay.setEditable(false);
        translatedDelay.setSelectedIndex(ApplicationProperties.KEY.TranslatedDelayIndex.asInt(2));
        translatedDelay.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand() == "comboBoxChanged") {
                    translatedDelay.getItemAt(translatedDelay.getSelectedIndex());
                }
            }
        });
        translatedDelay.setMaximumSize(new Dimension(100, 100));
        toolBar.add(new JLabel("delay: "));
        toolBar.add(translatedDelay);
        toolBar.addSeparator();
        
        translatedDelay.setFocusable(false);
    }
    
    private void addTranslatedRows(JToolBar toolBar) {
        List<Integer> rows = ApplicationProperties.KEY.TranslatedRows.asIntegerList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        translatedRows = new JComboBox<>(rows.toArray(new Integer[0]));
        translatedRows.setEditable(false);
        translatedRows.setSelectedIndex(ApplicationProperties.KEY.TranslatedRowsIndex.asInt(2));
        translatedRows.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand() == "comboBoxChanged") {
                    translatedRows.getItemAt(translatedRows.getSelectedIndex());
                }
            }
        });
        translatedRows.setMaximumSize(new Dimension(100, 100));
        toolBar.add(new JLabel("rows: "));
        toolBar.add(translatedRows);
        toolBar.addSeparator();
        
        translatedRows.setFocusable(false);
    }
    
    private void addTranslatedLength(JToolBar toolBar) {
        List<Integer> lengths = ApplicationProperties.KEY.TranslatedLength.asIntegerList(100, 200, 300, 400, 500);
        translatedLength = new JComboBox<Integer>(lengths.toArray(new Integer[0]));
        translatedLength.setEditable(false);
        translatedLength.setSelectedIndex(ApplicationProperties.KEY.TranslatedLengthIndex.asInt(1));
        translatedLength.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand() == "comboBoxChanged") {
                    translatedLength.getItemAt(translatedLength.getSelectedIndex());
                }
            }
        });
        translatedLength.setMaximumSize(new Dimension(100, 100));
        toolBar.add(new JLabel("length: "));
        toolBar.add(translatedLength);
        toolBar.addSeparator();
        
        translatedLength.setFocusable(false);
    }
    
    private void addLanguage(JToolBar toolBar) {
        srcLanguage = new JComboBox<>(Language.values());
        Language lng = Language.valueOf(ApplicationProperties.KEY.SrcLang.asString().toUpperCase());
        srcLanguage.setSelectedIndex(lng.ordinal());
        srcLanguage.setEditable(false);
        srcLanguage.addActionListener(e -> {
            if (e.getActionCommand() == "comboBoxChanged") {
                Language lng1 = srcLanguage.getItemAt(srcLanguage.getSelectedIndex());
                getDocument().getTranslator().setSrc(lng1);
            }
        });
        srcLanguage.setMaximumSize(new Dimension(100, 100));
        toolBar.add(new JLabel("from: "));
        toolBar.add(srcLanguage);
        toolBar.addSeparator();
        srcLanguage.setFocusable(false);
        
        trgLanguage = new JComboBox<>(Language.values());
        lng = Language.valueOf(ApplicationProperties.KEY.TrgLang.asString().toUpperCase());
        trgLanguage.setSelectedIndex(lng.ordinal());
        trgLanguage.setEditable(false);
        trgLanguage.addActionListener(e -> {
            if (e.getActionCommand() == "comboBoxChanged") {
                Language lng12 = trgLanguage.getItemAt(trgLanguage.getSelectedIndex());
                getDocument().getTranslator().setTrg(lng12);
            }
        });
        trgLanguage.setMaximumSize(new Dimension(100, 100));
        toolBar.add(new JLabel("to: "));
        toolBar.add(trgLanguage);
        toolBar.addSeparator();
        trgLanguage.setFocusable(false);
    }
    
    private void addClipboard(JToolBar toolBar) {
        clipboardCheckBox = new JCheckBox();
        boolean isCopyingClipboard = ApplicationProperties.KEY.Clipboard.asBoolean();
        clipboardCheckBox.setSelected(isCopyingClipboard);
        toolBar.add(new JLabel("clipb: "));
        toolBar.add(clipboardCheckBox);
        toolBar.addSeparator();
        clipboardCheckBox.setFocusable(false);
    }
    
    private void addOpenFile(JToolBar toolBar) {
        String imgLocation = "images/book.png";
        URL imageURL = getClass().getClassLoader().getResource(imgLocation);
        
        openBookButton = new JButton();
        openBookButton.setActionCommand("openbook");
        openBookButton.setToolTipText("Open book");
        openBookButton.addActionListener(e -> {
            if (fileChooser == null) {
                fileChooser = new JFileChooser();
                fileChooser.addChoosableFileFilter(new ExtensionFileFilter(new String[]{"pdf"}, ""));
                fileChooser.setAcceptAllFileFilterUsed(false);
            }
            
            int returnVal = fileChooser.showDialog(DocumentViewer.this,
                    "Open file");
            
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                fileChooser.setSelectedFile(null);
                DocumentViewer.this.frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            DocumentViewer.this.open(file.getAbsolutePath());
                        } catch (Exception ex) {
                            logger.error(ex.getMessage(), ex);
                        } finally {
                            DocumentViewer.this.frame.pack();
                            DocumentViewer.this.frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                        }
                    }
                });
            }
        });
        
        if (imageURL != null) {
            openBookButton.setIcon(new ImageIcon(imageURL, "open"));
        }
        openBookButton.setText("open");
        toolBar.add(openBookButton);
        toolBar.addSeparator();
    }
    
    public void addNavigateHistory(boolean isPrev) {
        Object[] hisState;
        hisState = new Object[]{
                currentPage,
                imageScale,
                imageScrollPane.getHorizontalScrollBar().getValue(),
                imageScrollPane.getVerticalScrollBar().getValue()
        };
        if (isPrev) historyPrevDeque.offerFirst(hisState);
        if (!isPrev) historyNextDeque.offerFirst(hisState);
        buttonByCommandName.get(HIS_PREVIOUS).setEnabled(!historyPrevDeque.isEmpty());
        buttonByCommandName.get(HIS_NEXT).setEnabled(!historyNextDeque.isEmpty());
    }
    
    public void navigateHistory(boolean isPrev) {
        Object[] hisState = null;
        if (isPrev && historyPrevDeque.size() > 0) {
            hisState = (Object[]) historyPrevDeque.pollFirst();
            addNavigateHistory(false);
        } else if (!isPrev && historyNextDeque.size() > 0) {
            hisState = (Object[]) historyNextDeque.pollLast();
            addNavigateHistory(true);
        }
        buttonByCommandName.get(HIS_PREVIOUS).setEnabled(!historyPrevDeque.isEmpty());
        buttonByCommandName.get(HIS_NEXT).setEnabled(!historyNextDeque.isEmpty());
        if (hisState != null) {
            int page = (int) hisState[0];
            float scale = (float) hisState[1];
            int horiz = (int) hisState[2];
            int vert = (int) hisState[3];
            imageScrollPane.setVisible(false);
            try {
                if (scale != imageScale) {
                    currentPage = page;
                    setImageScale(scale);
                } else {
                    showPage(page);
                }
                imageScrollPane.getHorizontalScrollBar().setValue(horiz);
                imageScrollPane.getVerticalScrollBar().setValue(vert);
            } finally {
                imageScrollPane.setVisible(true);
            }
        }
    }
    
    public void showPage(int page) {
        currentPage = page;
        Viewer viewer = pageViewer.get(page);
        Rectangle point = viewer.getWindowSize();
        imageScrollPane.getViewport().setViewPosition(new Point(0, point.y));
        if (buttonByCommandName.containsKey(PREVIOUS))
            buttonByCommandName.get(PREVIOUS).setEnabled(currentPage > 0);
        if (buttonByCommandName.containsKey(NEXT))
            buttonByCommandName.get(NEXT).setEnabled(currentPage < document.getPageAmount() - 1);
        buttonByCommandName.get(HIS_PREVIOUS).setEnabled(!historyPrevDeque.isEmpty());
        buttonByCommandName.get(HIS_NEXT).setEnabled(!historyNextDeque.isEmpty());
        if (frame != null) {
            frame.pack();
        }
    }
    
    public float getImageScale() {
        return imageScale;
    }
    
    public void setImageScale(float scale) {
        imageScale = scale;
        for (Viewer v : pageViewer.values()) {
            v.changeImageScale(imageScale);
        }
        if (frame != null) {
            frame.pack();
        }
        showPage(currentPage);
    }
    
    public int getTranslatedDelay() {
        return translatedDelay.getItemAt(translatedDelay.getSelectedIndex());
    }
    
    public int getTranslatedRows() {
        return translatedRows.getItemAt(translatedRows.getSelectedIndex());
    }
    
    public int getTranslatedLength() {
        return translatedLength.getItemAt(translatedLength.getSelectedIndex());
    }
    
    public void setIndicatorPage(int page) {
        currentPage = page;
        ActionListener[] ls = pagesCombobox.getActionListeners();
        if (ls.length > 0)
            pagesCombobox.removeActionListener(ls[0]);
        pagesCombobox.setSelectedIndex(page);
        if (ls.length > 0)
            pagesCombobox.addActionListener(ls[0]);
        if (buttonByCommandName.containsKey(PREVIOUS))
            buttonByCommandName.get(PREVIOUS).setEnabled(currentPage > 0);
        if (buttonByCommandName.containsKey(NEXT))
            buttonByCommandName.get(NEXT).setEnabled(currentPage < document.getPageAmount() - 1);
    }
    
    private void createToolBar() throws IllegalAccessException, IOException, InstantiationException {
        toolBar = new JToolBar();
        add(toolBar, BorderLayout.PAGE_START);
        
        addOpenFile(toolBar);
        addClipboard(toolBar);
        addLanguage(toolBar);
        addTranslatedLength(toolBar);
        addTranslatedRows(toolBar);
        addTranslatedDelay(toolBar);
        addScalePage(toolBar);
        addNavPages(toolBar);
        addNavHisPages(toolBar);
        
        for (Component c : toolBar.getComponents()) {
            c.setMaximumSize(new Dimension(c.getPreferredSize().width, 20));
        }
    }
    
    private void open(final String pdfFileName) throws IllegalAccessException, IOException, InstantiationException {
        if (!StringUtils.isBlank(pdfFilePath)) {
            Language srcLng = srcLanguage.getItemAt(srcLanguage.getSelectedIndex());
            Language trgLng = srcLanguage.getItemAt(trgLanguage.getSelectedIndex());
            Integer rows = translatedRows.getItemAt(translatedRows.getSelectedIndex());
            int left = workingSplitPane == null ? -1 : workingSplitPane.getDividerLocation();
            int bottom = viewSplitPane == null ? -1 : viewSplitPane.getDividerLocation();
            boolean isCopyingClipbord = clipboardCheckBox.isSelected();
            historyStore.save(pdfFilePath, currentPage, BigDecimal.valueOf(imageScale),
                    getTranslatedDelay(), srcLng, trgLng, rows, left, bottom, isCopyingClipbord);
        }
        
        pdfFilePath = pdfFileName;
        currentPage = 0;
        
        pagesCombobox.setSelectedIndex(-1);
        pagesCombobox.removeAllItems();
        if (imageScrollPane != null) {
            imageScrollPane.setVisible(false);
            remove(imageScrollPane);
        }
        
        for (Component c : toolBar.getComponents()) {
            c.setEnabled(false);
        }
        
        HistoryStore.History selectedHistory = null;
        if (StringUtils.isBlank(pdfFilePath)) {
            Map<String, HistoryStore.History> his = new LinkedHashMap<>();
            for (HistoryStore.History h : historyStore.read()) {
                his.put(h.getFilePath(), h);
            }
            Iterator<HistoryStore.History> it = his.values().iterator();
            if (it.hasNext()) {
                String selected = (String) JOptionPane.showInputDialog(
                        frame,
                        "Pdf file name:",
                        "History",
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        his.keySet().toArray(),
                        it.next().getFilePath());
                selectedHistory = his.get(selected);
            }
        } else {
            Map<String, HistoryStore.History> his = new HashMap<>();
            for (HistoryStore.History h : historyStore.read()) {
                his.put(h.getFilePath(), h);
            }
            selectedHistory = his.get(pdfFileName);
        }
        if (selectedHistory != null) {
            pdfFilePath = selectedHistory.getFilePath();
            currentPage = selectedHistory.getPage();
            imageScale = selectedHistory.getScale().floatValue();
            scale.getEditor().setItem(imageScale * 100);
            DefaultComboBoxModel<Integer> model = (DefaultComboBoxModel<Integer>) translatedDelay.getModel();
            Integer index = model.getIndexOf(selectedHistory.getDelay());
            if (index != -1) translatedDelay.setSelectedIndex(index);
            DefaultComboBoxModel<Integer> rowsModel = (DefaultComboBoxModel<Integer>) translatedRows.getModel();
            index = rowsModel.getIndexOf(selectedHistory.getRows());
            if (index != -1) translatedRows.setSelectedIndex(index);
            clipboardCheckBox.setSelected(selectedHistory.getClipboard());
        }
        
        contentPane.removeAll();
        if (StringUtils.isBlank(pdfFilePath)) {
            openBookButton.setEnabled(true);
            frame.setTitle("");
            SwingUtilities.invokeLater(() -> openBookButton.doClick());
            return;
        }
        frame.setTitle(pdfFilePath);
        
        JPanel viewPanel = new JPanel();
        viewPanel.setLayout(new BoxLayout(viewPanel, BoxLayout.Y_AXIS));
        
        if (imageScrollPane != null) remove(imageScrollPane);
        imageScrollPane = new JScrollPane(viewPanel);
        imageScrollPane.getVerticalScrollBar().setUnitIncrement(100);
        workingPane.removeAll();
        workingPane.add(imageScrollPane, BorderLayout.CENTER);
        int w = Toolkit.getDefaultToolkit().getScreenSize().width;
        int h = Toolkit.getDefaultToolkit().getScreenSize().height;
        setPreferredSize(new Dimension(w, h));
        
        try {
            document = new DocumentImpl(pdfFilePath);
            pageViewer = new HashMap<>();
            for (int i = 0; i < document.getPageAmount(); i++) {
                ImageViewer viewer = new ImageViewer(i, this);
                viewPanel.add(viewer);
                pageViewer.put(i, viewer);
            }
            ActionListener[] al = pagesCombobox.getActionListeners();
            pagesCombobox.removeActionListener(al[0]);
            for (int page = 1; page <= document.getPageAmount(); page++) {
                pagesCombobox.addItem("" + page);
            }
            pagesCombobox.addActionListener(al[0]);
            pagesCombobox.setSelectedIndex(currentPage);
            for (Component c : toolBar.getComponents()) {
                c.setEnabled(true);
            }
            setImageScale(imageScale);
            
            imageScrollPane.setWheelScrollingEnabled(false);
            imageScrollPane.addMouseWheelListener(new MouseWheelMovedAdapter(imageScrollPane));
            
            if (selectedHistory != null) {
                DefaultComboBoxModel<Language> lngModel = (DefaultComboBoxModel<Language>) srcLanguage.getModel();
                int index = lngModel.getIndexOf(selectedHistory.getFrom());
                if (index != -1) srcLanguage.setSelectedIndex(index);
                lngModel = (DefaultComboBoxModel<Language>) trgLanguage.getModel();
                index = lngModel.getIndexOf(selectedHistory.getTo());
                if (index != -1) trgLanguage.setSelectedIndex(index);
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            openBookButton.setEnabled(true);
            frame.setTitle("");
            contentPane.removeAll();
            SwingUtilities.invokeLater(() -> openBookButton.doClick());
        }
        
        try {
            fillContent();
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
        
        if (selectedHistory != null) {
            final int bs = selectedHistory.getBottomSplitSize();
            final int ls = selectedHistory.getLeftSplitSize();
            viewSplitPane.setDividerLocation(bs);
            workingSplitPane.setDividerLocation(ls);
        }
    }
    
    private void fillContent() {
        contentPane.removeAll();
        contentPane.setLayout(new BorderLayout());
        JPanel viewPanel = new JPanel();
        viewPanel.setLayout(new BoxLayout(viewPanel, BoxLayout.Y_AXIS));
        
        JScrollPane imageScrollPane = new JScrollPane(viewPanel);
        imageScrollPane.getVerticalScrollBar().setUnitIncrement(100);
        contentPane.add(imageScrollPane, BorderLayout.CENTER);
        for (TextArea ta : document.getBookmarks()) {
            JLabel f = new JLabel();
            f.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            final Annotation a = (Annotation) ta.getTag();
            String page = (a == null ? "" : (a.getDestPage() + "      ").substring(0, 5)).replaceAll(" ", "&ensp;");
            f.setText("<html><font color=gray>" + page + "</font><font color=black>" + ta.getText().replaceAll(" ", "&ensp;") +
                              "</font></html>");
            f.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    final Annotation a = (Annotation) ta.getTag();
                    if (a != null) {
                        SwingUtilities.invokeLater(() -> {
                            showPage(a.getDestPage());
                            float imageHeight = getDocument().getHeight(currentPage);
                            float y = imageHeight * imageScale - a.getDestY() * imageScale;
                            scroll(a.getDestX() * imageScale, y);
                            Viewer w = getViewer(a.getDestPage());
                            ((JComponent) w).setCursor(Cursor.getDefaultCursor());
                        });
                    }
                }
            });
            viewPanel.add(f);
        }
    }
    
    private static void copyToClipboard(String message) {
        StringSelection selection = new StringSelection(message);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, selection);
    }
    
    private enum OptionAdapter {
        PDFFILENAME("pdf"),
        WORKING("working"),
        SRC("src"),
        TRG("trg");
        
        private String name;
        private String defValue;
        
        OptionAdapter(String name) {
            this.name = name;
        }
        
        OptionAdapter(String name, String defValue) {
            this(name);
            this.defValue = defValue;
        }
        
        public String getOptionValue(CommandLine commandLine) {
            return commandLine.getOptionValue(name, defValue);
        }
        
        public Boolean getBooleanOptionValue(CommandLine commandLine) {
            String s = getOptionValue(commandLine);
            return StringUtils.isBlank(s) ? null : Boolean.valueOf(s.trim());
        }
        
        public Long getLongOptionValue(CommandLine commandLine) {
            String s = getOptionValue(commandLine);
            return StringUtils.isBlank(s) ? null : Long.valueOf(s.trim());
        }
        
        public String getOptionName() {
            return name;
        }
        
        @SuppressWarnings("static-access")
        public Option createOption() {
            return OptionBuilder.withLongOpt(name)
                           .hasArg()
                           .withValueSeparator('=')
                           .create();
        }
    }
    
    public class MouseWheelMovedAdapter extends MouseAdapter {
        
        private final Timer scrollingTimer;
        private volatile int units;
        private volatile int direct;
        private volatile int offset;
        private Thread thread;
        
        public MouseWheelMovedAdapter(final JScrollPane imageScrollPane) {
            thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    long inactivityTime = System.currentTimeMillis();
                    try {
                        while (!Thread.interrupted()) {
                            while (direct != 0) {
                                setOffset(units);
                                int y = imageScrollPane.getVerticalScrollBar().getValue();
                                imageScrollPane.getVerticalScrollBar().setValue(y + direct * offset);
                                inactivityTime = System.currentTimeMillis();
                                direct = 0;
                            }
                            if (System.currentTimeMillis() - inactivityTime > 3000) {
                                Thread.sleep(100);
                            }
                        }
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
            thread.start();
            scrollingTimer = new javax.swing.Timer(500, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    units = 0;
                }
            });
            scrollingTimer.start();
        }
        
        private void setOffset(int units) {
            if (units < 2) {
                offset = 20;
            } else if (units < 50) {
                offset = 50;
            } else if (units < 100) {
                offset = 100;
            } else {
                offset = 200;
            }
        }
        
        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            if (e.getScrollType() == WHEEL_UNIT_SCROLL) {
                direct = e.getUnitsToScroll() == 0 ? 0 : e.getUnitsToScroll() > 0 ? 1 : -1;
                units++;
            }
        }
    }
}
