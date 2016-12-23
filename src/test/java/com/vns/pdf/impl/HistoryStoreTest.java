package com.vns.pdf.impl;

import com.vns.pdf.ApplicationProperties;
import com.vns.pdf.Language;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import org.apache.commons.configuration.ConfigurationException;
import org.junit.BeforeClass;
import org.junit.Test;
import static java.lang.System.out;

public class HistoryStoreTest {
    @BeforeClass
    public static void up() throws IOException, ConfigurationException {
        String workingDir = "../google-context-translator/";
        String srcLang = "en", trgLang = "ru";
        ApplicationProperties.prepareWorkingDir(workingDir, srcLang, trgLang);
    }
    
    @Test
    public void test() throws Exception {
        HistoryStore store = new HistoryStore();
        store.save("test1", 1, BigDecimal.valueOf(100), 1000, Language.EN, Language.RU, 5);
        Thread.sleep(1000);
        store.save("test2", 2, BigDecimal.valueOf(200), 1000, Language.EN, Language.RU, 5);
        
        List<HistoryStore.History> hs = store.read();
        for (HistoryStore.History h : hs) {
            out.println(h);
        }
    }
}