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
import com.vns.pdf.domain.Doc;
import com.vns.pdf.domain.Page;
import com.vns.pdf.domain.Word;
import java.awt.Image;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
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
import org.apache.pdfbox.pdmodel.common.PDRectangle;
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
            
            this.doc = new Doc(new ArrayList<>());
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                PDRectangle box = document.getPage(i).getMediaBox();
                this.doc.getPages().add(new Page(new ArrayList<>(), (int) box.getWidth(), (int) box.getHeight()));
            }
            
            Writer dummy = new OutputStreamWriter(new ByteArrayOutputStream());
            pdfTextStripper.writeText(document, dummy);
            createTextAreaFile();
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
    
    class CustomPDFTextStripper extends PDFTextStripper {
        private String patternCompile;
        
        public CustomPDFTextStripper() throws IOException {
            patternCompile = ApplicationProperties.KEY.PatternCompile.asString("([^ \\s\n\t.,:]+)");
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
