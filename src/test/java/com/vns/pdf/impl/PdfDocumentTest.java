package com.vns.pdf.impl;

import com.vns.pdf.ApplicationProperties;
import com.vns.pdf.domain.Doc;
import java.io.IOException;
import org.apache.commons.configuration.ConfigurationException;
import org.junit.BeforeClass;
import org.junit.Test;
import static java.lang.System.out;

public class PdfDocumentTest {
    
    @BeforeClass
    public static void up() throws IOException, ConfigurationException {
        String workingDir = "../google-context-translator/";
        String srcLang = "en", trgLang = "ru";
        ApplicationProperties.prepareWorkingDir(workingDir, srcLang, trgLang);
    }
    
    @Test
    public void testParse() throws IOException {
        PdfDocument document = PdfDocument.createDocument("../JAAS Reference Guide.pdf");
        Doc doc = document.getDoc();
        
        out.println(doc);
    }
    
}