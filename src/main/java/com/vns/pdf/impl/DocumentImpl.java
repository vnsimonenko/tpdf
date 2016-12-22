package com.vns.pdf.impl;

import com.vns.pdf.Document;
import com.vns.pdf.TextArea;
import com.vns.pdf.TextLocation;
import com.vns.pdf.Translator;
import com.vns.pdf.Utils;
import com.vns.pdf.domain.Annotation;
import com.vns.pdf.domain.Page;
import com.vns.pdf.domain.Word;
import java.awt.Image;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DocumentImpl implements Document {
    private Map<Integer, TextLocation> textLocationByPage;
    private Map<Integer, TextLocation> annotationLocationByPage;
    private Map<Integer, Set<TextArea>> textAreas;
    private Map<Integer, Set<TextArea>> annotationAreas;
    private PdfDocument pdfDocument;
    
    public DocumentImpl(String pdfFileName) throws IllegalAccessException, InstantiationException, IOException {
        pdfDocument = PdfDocument.createDocument(pdfFileName);
        textLocationByPage = new HashMap<>();
        annotationLocationByPage = new HashMap<>();
        textAreas = new HashMap<>();
        annotationAreas = new HashMap<>();
        int pageNumber = 0;
        for (Page page : pdfDocument.getDoc().getPages()) {
            TextLocation textLocation = new TextLocationImpl();
            for (Word w : page.getWords()) {
                TextArea area = textLocation.register(w.getText(), w.getxMin(), w.getyMin(), w.getxMax() - w.getxMin(), w.getyMax() - w.getyMin());
                Utils.getMultiplicityValueFromMap(textAreas, pageNumber, HashSet::new).add(area);
            }
            textLocationByPage.put(pageNumber, textLocation);
            TextLocation annotationLocation = new TextLocationImpl() {
                
            };
            for (Annotation w : page.getAnnotations()) {
                TextArea area = annotationLocation.register(null, w.getX(), w.getY(), w.getWidth(), w.getHeight());
                area.setTag(w);
                Utils.getMultiplicityValueFromMap(annotationAreas, pageNumber, HashSet::new).add(area);
            }
            annotationLocationByPage.put(pageNumber, annotationLocation);
            pageNumber++;
        }
    }
    
    public Image renderImage(int pageIndex, float scale) throws IOException {
        return pdfDocument.renderImage(pageIndex, scale);
    }
    
    @Override
    public TextLocation getTextLocation(int page) {
        return textLocationByPage.get(page);
    }
    
    @Override
    public TextLocation getAnnotationLocation(int page) {
        return annotationLocationByPage.get(page);
    }
    
    @Override
    public int getPageAmount() {
        return pdfDocument.getDoc().getPages().size();
    }
    
    @Override
    public float getWidth(int pageNumber) {
        return pdfDocument.getDoc().getPages().get(pageNumber).getWidth();
    }
    
    @Override
    public float getHeight(int pageNumber) {
        return pdfDocument.getDoc().getPages().get(pageNumber).getHeight();
    }
    
    @Override
    public Translator getTranslator() {
        return TranslatorImpl.INSTANCE;
    }
    
    @Override
    public void close() throws IOException {
        pdfDocument.close();
    }
}
