package com.vns.pdf.impl;

import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.gson.GsonFactory;
import com.vns.pdf.ApplicationProperties;
import com.vns.pdf.Language;
import com.vns.pdf.Phonetic;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * История прочитанных файлов.
 */
public class HistoryStore {
    private final static Logger LOGGER = LoggerFactory.getLogger(HistoryStore.class);
    private JsonObjectParser parser;
    private JsonFactory jsonFactory;
    private Path historyDir;
    
    public HistoryStore() throws IOException {
        JsonObjectParser.Builder builder = new JsonObjectParser.Builder(new GsonFactory());
        parser = builder.build();
        jsonFactory = parser.getJsonFactory();
        historyDir = Paths.get(ApplicationProperties.KEY.HistoryDir.asString());
        if (!Files.exists(historyDir, LinkOption.NOFOLLOW_LINKS)) {
            Files.createDirectories(historyDir, new FileAttribute[0]);
        }
    }
    
    /**
     * Сохраняем название файла, страницу, масштаб
     *
     * @param filePath, page, scale
     */
    public void save(String filePath, int page, BigDecimal scale, Integer delay, Language from, Language to,
                     Integer rows, int leftSplit, int bottomSplit, 
                     boolean clipboard, Phonetic phonetic) throws IOException {
        if (StringUtils.isBlank(filePath)) {
            return;
        }
        String fileName = Paths.get(filePath + ".json").getFileName().toString();
        History history = new History(filePath, page, scale, delay, from, to, rows, leftSplit, bottomSplit, 
                                             clipboard, phonetic);
        byte[] historyBytes = jsonFactory.toPrettyString(history).getBytes("utf-8");
        Path target = Paths.get(historyDir.toAbsolutePath().toString(), fileName);
        Files.copy(new ByteArrayInputStream(historyBytes), target, StandardCopyOption.REPLACE_EXISTING);
    }
    
    /**
     * Считываем json файлы из history папки.
     *
     * @return список аттрибутов прочитанных файлов: String filePath, int page, BigDecimal scale
     * @throws IOException
     */
    public List<History> read() throws IOException {
        List<History> his = Files.list(Paths.get(ApplicationProperties.KEY.HistoryDir.asString())).filter(
                path -> path.getFileName().toString().endsWith(".json"))
                                    .map(path -> {
                                        try {
                                            byte[] bs = Files.readAllBytes(path);
                                            History h = parser.parseAndClose(new ByteArrayInputStream(bs), Charset.forName("utf-8"), History.class);
                                            FileTime ft = Files.getLastModifiedTime(path, LinkOption.NOFOLLOW_LINKS);
                                            h.version = ft.toMillis();
                                            return h;
                                        } catch (IOException e) {
                                            LOGGER.error(e.getMessage(), e);
                                        }
                                        return null;
                                    }).filter(history -> history != null).sorted(Comparator.comparingLong(
                        h -> -1 * h.version)).collect(Collectors.toList());
        return his;
    }
    
    public History last() throws IOException {
        List<History> his = read();
        return his.size() > 0 ? his.get(0) : null;
    }
    
    public static class History implements Serializable {
        @com.google.api.client.util.Key("filepath")
        private String filePath;
        @com.google.api.client.util.Key("page")
        private Integer page;
        @com.google.api.client.util.Key("scale")
        private BigDecimal scale;
        @com.google.api.client.util.Key("delay")
        private Integer delay;
        @com.google.api.client.util.Key("from")
        private Language from;
        @com.google.api.client.util.Key("to")
        private Language to;
        @com.google.api.client.util.Key("rows")
        private Integer rows;
        @com.google.api.client.util.Key("leftsplitsize")
        private Integer leftSplitSize;
        @com.google.api.client.util.Key("bottomsplitsize")
        private Integer bottomSplitSize;
        @com.google.api.client.util.Key("clipboard")
        private Boolean clipboard;        
        @com.google.api.client.util.Key("phonetic")
        private Phonetic phonetic;
        
        private long version;
        
        public History() {
        }
        
        public History(String fileName, Integer page, BigDecimal scale, Integer delay, Language from, Language to,
                       Integer rows, Integer leftSplitSize, Integer bottomSplitSize, 
                       Boolean clipboard, Phonetic phonetic) {
            this.filePath = fileName;
            this.page = page;
            this.scale = scale;
            this.delay = delay;
            this.from = from;
            this.to = to;
            this.rows = rows;
            this.leftSplitSize = leftSplitSize;
            this.bottomSplitSize = bottomSplitSize;
            this.clipboard = clipboard;
            this.phonetic = phonetic;
        }
        
        public String getFilePath() {
            return filePath;
        }
        
        public Integer getPage() {
            return page;
        }
        
        public BigDecimal getScale() {
            return scale;
        }
        
        public Integer getDelay() {
            return delay;
        }
        
        public Language getFrom() {
            return from;
        }
        
        public Language getTo() {
            return to;
        }
        
        public Integer getRows() {
            return rows;
        }
        
        public long getVersion() {
            return version;
        }
        
        public Integer getBottomSplitSize() {
            return bottomSplitSize;
        }
        
        public void setBottomSplitSize(Integer bottomSplitSize) {
            this.bottomSplitSize = bottomSplitSize;
        }
        
        public Integer getLeftSplitSize() {
            return leftSplitSize;
        }
        
        public void setLeftSplitSize(Integer leftSplitSize) {
            this.leftSplitSize = leftSplitSize;
        }
    
        public Boolean getClipboard() {
            return clipboard;
        }
    
        public void setClipboard(Boolean clipboard) {
            this.clipboard = clipboard;
        }
    
        public Phonetic getPhonetic() {
            return phonetic;
        }
    
        public void setPhonetic(Phonetic phonetic) {
            this.phonetic = phonetic;
        }
    
        @Override
        public String toString() {
            return "History{" +
                           "filePath='" + filePath + '\'' +
                           ", page=" + page +
                           ", scale=" + scale +
                           ", delay=" + delay +
                           ", from=" + from +
                           ", to=" + to +
                           ", rows=" + rows +
                           ", leftSplitSize=" + leftSplitSize +
                           ", bottomSplitSize=" + bottomSplitSize +
                           ", clipboard=" + clipboard +
                           ", phonetic=" + phonetic +
                           ", version=" + version +
                           '}';
        }
    }
}
