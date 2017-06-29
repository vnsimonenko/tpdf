package com.vns.pdf.impl;

import com.vns.pdf.OxfordReceiver;
import com.vns.pdf.Phonetic;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import java.util.regex.Matcher;
import org.apache.commons.io.IOUtils;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class OxfordReceiverImpl implements OxfordReceiver {
    private static final Logger logger = LoggerFactory.getLogger(OxfordReceiverImpl.class);
    private final static String USERAGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.152 Safari/537.36";
    private static final String REQUEST = "http://www.oxfordlearnersdictionaries.com/search/english/direct/?q=%1$s";
    //private static final String REQUEST = "http://www.oxfordlearnersdictionaries.com/definition/english/%1$s_1?q=%1$s";
    //private static final String REQUEST = "http://www.oxfordlearnersdictionaries.com/definition/english/%1$s";

    private FileHolder fileHolder;
    
    public OxfordReceiverImpl(FileHolder fileHolder) {
        this.fileHolder = fileHolder;
    }
    
    public InputStream load(String enWord, Phonetic phonetic, boolean exactly) {
        if (!enWord.matches("[a-zA-Z']+")) {
            return null;
        }
        try {
            capture(enWord, exactly);
            File f = fileHolder.getOxfordFile(enWord, phonetic);
            return f != null ? new FileInputStream(f) : null;
        } catch (IOException | URISyntaxException | InstantiationException | IllegalAccessException ex) {
            logger.error("The fail is in the method OxfordReceiverImpl.load.", ex);
            if (ex instanceof HttpStatusException) {
                HttpStatusException het = (HttpStatusException) ex;
                if (het.getStatusCode() == 404) {
                    return null;
                }
            }
            throw new RuntimeException("OxfordReceiver:load: " + enWord, ex);
        }
    }
    
    public String getTranscription(String enWord, Phonetic phonetic) {
        try {
            return fileHolder.getTranscription(enWord, phonetic);
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
        }
        return "";
    }
    
    public File getFile(String enWord, Phonetic phonetic) {
        try {
            return fileHolder.getOxfordFile(enWord, phonetic);
        } catch (UnsupportedEncodingException ex) {
            logger.error(ex.getMessage(), ex);
        }
        return null;
    }
    
    

    private void capture(String enWord, boolean exactly) throws IOException, URISyntaxException, IllegalAccessException, 
                                                            InstantiationException {
        String request = String.format(REQUEST, enWord);
        org.jsoup.Connection conn = Jsoup.connect(request).timeout(30000);
        Document doc = conn.get();
        //int i = conn.response().statusCode();
        Elements elements = doc.select(
                "div[class=\"pron-link\"] a[href^=\"http://www.oxfordlearnersdictionaries.com/pronunciation/english/\"]");
        if (elements.size() == 0) {
            return;
        }
        String phonRequest = elements.get(0).attr("href");
        Document phdoc = Jsoup.connect(phonRequest).timeout(3000).get();
        for (String[] phonView : new String[][]{{"NAmE", "us", Phonetic.AM.name()},
                {"BrE", "uk", Phonetic.BR.name()}}) {
            elements = phdoc.select("div[class=\"pron_row clear_fix\"]:has(span:contains(" + phonView[0]
                    + ")) div[class=\"pron_row__wrap1\"]:has(span:contains(" + phonView[0] + "))");
            for (Element el : elements) {
                Elements phElements = el
                        .select("span:contains(" + phonView[0] + ") + span[class=\"pron_phonetic\"]:has(pnc.wrap)");
                Elements sndElements = el
                        .select("div[class=\"sound audio_play_button pron-" + phonView[1] + " icon-audio\"]");
                if (sndElements.size() == 0) {
                    continue;
                }
                String href = sndElements.get(0).attr("data-src-mp3");
                String word = Phonetic.AM.name().equals(phonView[2]) 
                            ? sndElements.get(0).attr("title").replace(": American pronunciation", "")
                            : sndElements.get(0).attr("title").replace(": British pronunciation", "");
                //String word = getWordFromHref(href, Phonetic.AM.name().equals(phonView[2]));
                if (exactly && !word.toLowerCase().contains(enWord.toLowerCase())) {
                    logger.error("fail the word not contains enWord: " + word + "," + enWord + ", url: " + request);
                    return;
                }
                String transcription = "";
                try {
                    transcription = phElements.get(0).text().substring(1, phElements.get(0).text().length() - 1);
                } catch (IndexOutOfBoundsException ex) {
                    logger.error("fail in capture: " + enWord + ", url: " + request);
                }
                Phonetic phonetic = Phonetic.valueOf(phonView[2]);
                try (InputStream in = loadFile(href)) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    IOUtils.copy(in, out);
                    fileHolder.saveOxfordFile(new ByteArrayInputStream(out.toByteArray()), phonetic, word, transcription);
                } catch (IOException ex) {
                    logger.error(ex.getMessage() + "; URL: ".concat(request), ex);
                    throw ex;
                }
            }
        }
    }

    private String getWordFromHref(String href, boolean isAm) {
        String word = href.substring(href.lastIndexOf('/') + 1, href.length() - 4);
        java.util.regex.Pattern pattern = java.util.regex.Pattern
                .compile(String.format("^([^0-9]*)[_0-9]*%s[_0-9]*", isAm ? "_us" : "_gb"));
        Matcher matcher = pattern.matcher(word);
        if (matcher.find()) {
            word = matcher.group(1);
            while (word.endsWith("_")) {
                word = word.substring(0, word.length() - 1);
            }
        }
        return word.replace("_", "'");
    }

    private InputStream loadFile(String request) throws IOException {
        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {
            logger.error(ex.getMessage());
        }
        URL url = new URL(request);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(15000);
        conn.setDoOutput(false);
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", USERAGENT);
        conn.setUseCaches(true);
        return conn.getInputStream();
    }
    
    interface ValueFactory<T> {
        T create();
    }

    static <T, K1> T getMultiplicityValueFromMap(Map<K1, T> map, K1 key, ValueFactory<T> factory) throws IllegalAccessException, InstantiationException {
        T val = map.get(key);
        if (val == null) {
            val = factory.create();
            map.put(key, val);
            return val;
        }
        return val;
    }
}