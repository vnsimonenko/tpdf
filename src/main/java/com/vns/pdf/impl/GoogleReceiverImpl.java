package com.vns.pdf.impl;

import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.gson.GsonFactory;
import com.vns.pdf.GoogleReceiver;
import com.vns.pdf.Language;
import com.vns.pdf.Utils;
import com.vns.pdf.gmodel.Dics;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//https://translate.googleapis.com/translate_a/single?dt=t&dt=bd&dt=t&dt=bd&client=gtx&q=text&sl=en&tl=ru&dj=1&source=bubble
class GoogleReceiverImpl implements GoogleReceiver {
    private static final Logger glLogger = LoggerFactory.getLogger("gllogger");
    private static final Logger logger = LoggerFactory.getLogger(GoogleReceiverImpl.class);
    private static String request = "https://translate.googleapis.com/translate_a/single?dt=t&dt=bd&dt=t&dt=bd&client=gtx&q=%s&sl=%s&tl=%s&dj=1&source=bubble";
    private String cookie;
    
    static SSLContext createTrustAllSSLContext() throws Exception {
        TrustManager[] byPassTrustManagers = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                    
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {
                    }
                    
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {
                    }
                }
        };
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, byPassTrustManagers, new SecureRandom());
        return sslContext;
    }
    
    @Override
    public Dics translate(String text, Language from, Language to) throws Exception {
        URL url = createURL(text, from.GOOGLE.toLowerCase(), to.GOOGLE.toLowerCase());
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setSSLSocketFactory(createTrustAllSSLContext().getSocketFactory());
        connection.setRequestProperty("method", "GET");
        connection.setRequestProperty("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.87 Safari/537.36");
        connection.setRequestProperty("Accept", "text/html;charset=UTF-8");
        connection.setRequestProperty("Accept-Charset", "UTF-8");
        connection.setDoInput(true);
        connection.setDoOutput(true);
        InputStream in = connection.getInputStream();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] bytes = new byte[1024];
        int size;
        while ((size = in.read(bytes)) != -1) {
            bos.write(Arrays.copyOf(bytes, size));
        }
        if (!StringUtils.isBlank(cookie)) {
            connection.setRequestProperty("Cookie", cookie);
        } else {
            cookie = Utils.extractCookie(connection);
        }
        
        Dics dics = toDics(bos.toByteArray());
        glLogger.info(dics.getRawText());
        logger.info("translate(text:" + text + ", from:" + from + ",to:" + to + ")");
        return dics;
    }
    
    public Dics toDics(byte[] dicBytes) throws IOException {
        JsonObjectParser.Builder builder = new JsonObjectParser.Builder(new GsonFactory());
        JsonObjectParser parser = builder.build();
        
        String rawText = new String(dicBytes, "utf-8");
        StringReader reader = new StringReader(rawText);
        Dics dics = parser.parseAndClose(reader, Dics.class);
        dics.setRawText(rawText);
        return dics;
    }
    
    private URL createURL(String text, String from, String to) throws MalformedURLException, UnsupportedEncodingException {
        return new URL(String.format(request, URLEncoder.encode(text, "utf-8"), from, to));
    }
}
