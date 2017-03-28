/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vns.pdf.impl;

import com.sun.xml.internal.bind.marshaller.CharacterEscapeHandler;
import com.vns.pdf.ApplicationProperties;
import com.vns.pdf.domain.Annotation;
import com.vns.pdf.domain.Doc;
import com.vns.pdf.domain.Page;
import com.vns.pdf.domain.Word;
import java.awt.Image;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.action.PDAction;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDNamedDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineNode;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT;

class PdfDocument {
    private final static Logger LOGGER = LoggerFactory.getLogger(PdfDocument.class);
    private Path workingDir;
    private Path pdfTempDir;
    private String pdfFileName;
    private Path textAreaFilePath;
    private Doc doc;
    private PDFTextStripper pdfTextStripper;
    private PDDocument document;
    private PDFRenderer pdfRenderer;
    private List<Annotation> bookmarks;
    
    private PdfDocument(String pdfFileName) throws IOException {
        this.pdfFileName = pdfFileName;
        setWorkingDir();
        Path filePath = Paths.get(pdfFileName);
        PosixFileAttributes attrs = Files.getFileAttributeView(filePath, PosixFileAttributeView.class).readAttributes();
        String textAreaFileName = filePath.getFileName().toString() +
                                          "_" + filePath.toAbsolutePath().hashCode() +
                                          "_" + attrs.size() +
                                          "_" + attrs.lastModifiedTime().toString().replace(":", "_") +
                                          ".xml";
        textAreaFilePath = Paths.get(workingDir.toAbsolutePath().toString(), textAreaFileName);
        pdfTextStripper = new CustomPDFTextStripper();
        document = PDDocument.load(new File(pdfFileName));
        pdfRenderer = new PDFRenderer(document);
        
        if (Files.notExists(textAreaFilePath, LinkOption.NOFOLLOW_LINKS)) {
            pdfTextStripper.setSortByPosition(false);
            pdfTextStripper.setStartPage(0);
            pdfTextStripper.setEndPage(document.getNumberOfPages());
            
            this.doc = new Doc(new ArrayList<>(), new ArrayList<>());
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                PDPage pdPage = document.getPage(i);
                PDRectangle box = pdPage.getMediaBox();
                this.doc.getPages().add(new Page(new ArrayList<>(), new ArrayList<>(), (int) box.getWidth(), (int) box.getHeight()));
            }
            
            Writer dummy = new OutputStreamWriter(new ByteArrayOutputStream());
            pdfTextStripper.writeText(document, dummy);
            parseBookmarksAnnotation();
            createTextAreaFile();
            //document.save(pdfFileName + ".pdf");
        } else {
            loadTextAreaFile();
        }
    }
    
    static PdfDocument createDocument(String pdfFileName) throws IOException {
        return new PdfDocument(pdfFileName);
    }
    
    private void setWorkingDir() throws IOException {
        ApplicationProperties.KEY.PdfDir.asString();
        workingDir = Paths.get(ApplicationProperties.KEY.PdfDir.asString());
        Files.createDirectories(workingDir, new FileAttribute[0]);
        pdfTempDir = Paths.get(ApplicationProperties.KEY.Workspace.asString(), "temp");
        if (!Files.exists(pdfTempDir, LinkOption.NOFOLLOW_LINKS)) {
            Files.createDirectories(pdfTempDir);
        }
        ImageIO.setCacheDirectory(pdfTempDir.toFile());
        ImageIO.setUseCache(true);
    }
    
    void close() throws IOException {
        try {
            document.close();
        } catch (Exception ex) {
        }
        Files.deleteIfExists(pdfTempDir);
    }
    
    Doc getDoc() {
        return doc;
    }
    
    Image renderImage(int pageIndex, float scale) throws IOException {
        return pdfRenderer.renderImage(pageIndex, scale);
    }
    
    private void createTextAreaFile() {
        String pageXmlFileName = textAreaFilePath.toAbsolutePath().toString();
        try {
            JAXBContext context = JAXBContext.newInstance(Doc.class);
            Marshaller ms = context.createMarshaller();
            ms.setProperty(JAXB_FORMATTED_OUTPUT, true);
            ms.setProperty(CharacterEscapeHandler.class.getName(), (CharacterEscapeHandler) (ch, start, length, isAttVal, out) -> {
                String escape = StringEscapeUtils.escapeXml11(new String(ch));
                if (!escape.contains("&#")) 
                    out.write(escape.toCharArray(), 0, escape.toCharArray().length);
            });
            ms.marshal(doc, new File(pageXmlFileName));
        } catch (JAXBException ex) {
            LOGGER.error(ex.getMessage(), ex);
        }
    }
    
    private void loadTextAreaFile() {
        try {
            JAXBContext context = JAXBContext.newInstance(Doc.class);
            Unmarshaller un = context.createUnmarshaller();
            doc = (Doc) un.unmarshal(textAreaFilePath.toFile());
        } catch (JAXBException ex) {
            LOGGER.error(ex.getMessage(), ex);
        }
    }
    
    private void parseBookmarksAnnotation() throws IOException {
        PDDocumentOutline outline = document.getDocumentCatalog().getDocumentOutline();
        if (outline != null) {
            fillBookmark(outline, "");
        }
    }
    
    public void fillBookmark(PDOutlineNode bookmark, String indentation) throws IOException {
        PDOutlineItem current = bookmark.getFirstChild();
        while (current != null) {
            ActionData actionData = parsePDAction(current.getAction());
            if (actionData == null) {
                actionData = parsePDDestination(current.getDestination());
            }
            Annotation annotation;
            if (actionData != null) {
                annotation = new Annotation(-1, -1, -1, -1,
                                                   actionData.destX, actionData.destY,
                                                   actionData.destPage,
                                                   actionData.destZoom, indentation + current.getTitle());
            } else {
                annotation = new Annotation(indentation + current.getTitle());
            }
            this.doc.getBookmarks().add(annotation);
            fillBookmark(current, indentation + "    ");
            current = current.getNextSibling();
        }
    }
    
    private List<Annotation> parseAnnotation(PDPage pdPage) throws IOException {
        List<Annotation> annotations = new ArrayList<>();
        for (PDAnnotation annt : pdPage.getAnnotations()) {
            if (annt instanceof PDAnnotationLink) {
                PDAnnotationLink link = (PDAnnotationLink) annt;
                PDRectangle rect = link.getRectangle();
                float x = rect.getLowerLeftX();
                float y = rect.getUpperRightY();
                float width = rect.getWidth();
                float height = rect.getHeight();
                int rotation = pdPage.getRotation();
                if (rotation == 0) {
                    PDRectangle pageSize = pdPage.getMediaBox();
                    y = pageSize.getHeight() - y;
                } else if (rotation == 90) {
                    //do nothing
                }
                
                ActionData actionData = parsePDAction(link.getAction());
                if (actionData == null) {
                    actionData = parsePDDestination(link.getDestination());
                }
                if (actionData != null) {
                    Annotation a = new Annotation(x, y, width, height,
                                                         actionData.destX, actionData.destY,
                                                         actionData.destPage,
                                                         actionData.destZoom);
                    annotations.add(a);
                }
            }
        }
        return annotations;
    }
    
    private ActionData parsePDAction(PDAction action) throws IOException {
        PDDestination dest = null;
        if (action instanceof PDActionGoTo
                    && ((PDActionGoTo) action).getDestination() instanceof PDPageDestination) {
            dest = ((PDActionGoTo) action).getDestination();
        }
        if (action instanceof PDActionGoTo
                    && ((PDActionGoTo) action).getDestination() instanceof PDNamedDestination) {
            PDNamedDestination nameDest = (PDNamedDestination) ((PDActionGoTo) action).getDestination();
            dest = document.getDocumentCatalog().findNamedDestinationPage(nameDest);
        }
//        if (action instanceof PDActionURI) 
//            try {
//                final PDActionURI linkUri = (PDActionURI) action;
//                URI u = new URI(linkUri.getURI());
//                if ("file".equalsIgnoreCase(u.getScheme())) {
//                    Path path1 = Paths.get(u.getPath());
//                    String s = path1.toAbsolutePath().toString();
//                    boolean b = s.endsWith(".html");
//                    if (b) {
//                        s = s.substring(0, s.length() - 5) + ".pdf";
//                        Path path2 = Paths.get(pdfFileName).getParent();
//                        path1 = Paths.get(s);
//                        Path pathRelative = path2.relativize(path1);
//                        System.out.println(pathRelative);
//                        System.out.println(linkUri.getURI());
//                        ((PDActionURI) action).setURI(pathRelative.toString());
//                    }
//                }
//            } catch (URISyntaxException ex) {
//                ex.printStackTrace();
//            }
        return parsePDDestination(dest);
    }
    
    private ActionData parsePDDestination(PDDestination dest) throws IOException {
        if (dest != null) {
            if (dest instanceof PDNamedDestination) {
                return parsePDDestination(document.getDocumentCatalog().findNamedDestinationPage((PDNamedDestination) dest));
            }
            if (dest instanceof PDPageDestination) {
                float destZoom = -1;
                int destX = -1;
                int destY = -1;
                if (dest instanceof PDPageXYZDestination) {
                    PDPageXYZDestination destXYZ = (PDPageXYZDestination) dest;
                    destZoom = destXYZ.getZoom();
                    destX = destXYZ.getLeft();
                    destY = destXYZ.getTop();
                }
                int destPage = ((PDPageDestination) dest).retrievePageNumber();
                return destPage < 0 ? null : new ActionData(destX, destY, destPage, destZoom);
            }
        }
        return null;
    }
    
    class ActionData {
        final float destX;
        final float destY;
        final int destPage;
        final float destZoom;
        
        public ActionData(float destX, float destY, int destPage, float destZoom) {
            this.destX = destX;
            this.destY = destY;
            this.destPage = destPage;
            this.destZoom = destZoom;
        }
    }
    
    class CustomPDFTextStripper extends PDFTextStripper {
        private String patternCompile;
        
        public CustomPDFTextStripper() throws IOException {
            patternCompile = ApplicationProperties.KEY.PatternCompile.asString("([^ \\s\n\t.,:]+)");
        }
        
        protected void endPage(PDPage pdPage) throws IOException {
            Page page = doc.getPages().get(this.getCurrentPageNo() - 1);
            List<Annotation> annotations = parseAnnotation(pdPage);
            page.getAnnotations().addAll(annotations);
        }
        
        @Override
        protected void writeString(String string, List<TextPosition> textPositions) throws IOException {
            List<TextPosition> ts = new ArrayList<>(string.length());
            for (TextPosition t : textPositions) {
                String s = t.getUnicode();
                for (char ch : s.toCharArray()) {
                    ts.add(t);
                }
            }
            
            Page page = doc.getPages().get(this.getCurrentPageNo() - 1);
            Pattern p = Pattern.compile(patternCompile);
            Matcher m = p.matcher(string);
            Word prevWord = null;
            while (m.find()) {
                String s = m.group(1);
                try {
                    TextPosition tp1 = ts.get(m.start());
                    TextPosition tp2 = (ts.size() <= m.end() - 1) ? ts.get(ts.size() - 1) : ts.get(m.end() - 1);
                    float minY = Float.MAX_VALUE, maxY = 0.0f;
                    for (TextPosition tp : ts) {
                        minY = Math.min(tp.getY() - tp.getHeight(), minY);
                        maxY = Math.max(tp.getY(), maxY);
                    }
                    
                    Word word = new Word(tp1.getX(),
                                                minY,
                                                tp2.getX() + tp2.getWidth(),
                                                maxY,
                                                s
                    );
                    
                    if (prevWord != null) {
                        if (prevWord.getxMax() >= word.getxMin()
                                    && (prevWord.getyMax() > word.getyMin() && prevWord.getyMin() <= word.getyMin()
                                                || prevWord.getyMax() > word.getyMax() && prevWord.getyMin() >= word.getyMin())
                                    && m.start() > 0 && string.charAt(m.start() - 1) == ' ') {
                            prevWord.changeText(prevWord.getText() + word.getText());
                            prevWord.changexMax(word.getxMax());
                            prevWord = word;
                            continue;
                        }
                    }
                    prevWord = word;
                    page.getWords().add(word);
                } catch (IndexOutOfBoundsException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}
