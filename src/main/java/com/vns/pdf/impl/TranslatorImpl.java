package com.vns.pdf.impl;

import com.google.common.io.Files;
import com.vns.pdf.ApplicationProperties;
import com.vns.pdf.AudioHelper;
import com.vns.pdf.DataManager;
import com.vns.pdf.Language;
import com.vns.pdf.Phonetic;
import com.vns.pdf.Translator;
import com.vns.pdf.gmodel.Dics;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.Status;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheEventListenerConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.event.CacheEvent;
import org.ehcache.event.CacheEventListener;
import org.ehcache.event.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TranslatorImpl implements Translator {
    public static final Translator INSTANCE = new TranslatorImpl();
    private static final Logger LOGGER = LoggerFactory.getLogger(TranslatorImpl.class);
    private final CacheManager cacheManager;
    private final Cache<String, Dics> cache;
    private final GoogleReceiverImpl googleReceiver = new GoogleReceiverImpl();
    private final OxfordReceiverImpl oxfordReceiver = new OxfordReceiverImpl(
            new FileHolder(ApplicationProperties.KEY.SndDictDir.asString(Files.createTempDir().getAbsolutePath())));
    private final DataManager dataManager;
    private final AtomicReference<Language> srcLang;
    private final AtomicReference<Language> trgLang;
    private final AtomicReference<Phonetic> phonetic;
    
    //http://www.ehcache.org/documentation/3.1/cache-event-listeners.html
    private BlockingQueue<TranslatorEvent> translatorEvents = new LinkedBlockingQueue<>();
    
    private long lastTimeInMillis = System.currentTimeMillis();
    private final long DELAY_MILLIS = 500;
    
    private TranslatorImpl() {
        CacheEventListenerConfigurationBuilder cacheEventListenerConfiguration = CacheEventListenerConfigurationBuilder
                                                                                         .newEventListenerConfiguration(
                                                                                                 new TranslatedCacheEventListener(),
                                                                                                 EventType.CREATED,
                                                                                                 EventType.UPDATED,
                                                                                                 EventType.EVICTED,
                                                                                                 EventType.EXPIRED,
                                                                                                 EventType.REMOVED)
                                                                                         .unordered().asynchronous();
        cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
                               .withCache("translator",
                                       CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, Dics.class,
                                               ResourcePoolsBuilder.newResourcePoolsBuilder().heap(2, EntryUnit.ENTRIES))
                                               .add(cacheEventListenerConfiguration)
                               ).build(true);
        cache = cacheManager.getCache("translator", String.class, Dics.class);
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (cacheManager.getStatus() == Status.AVAILABLE) {
                cacheManager.close();
            }
        }));
        
        try {
            srcLang = new AtomicReference<>();
            srcLang.set(Language.valueOf(ApplicationProperties.KEY.SrcLang.asString("en").toUpperCase()));
            trgLang = new AtomicReference<>();
            trgLang.set(Language.valueOf(ApplicationProperties.KEY.TrgLang.asString("ru").toUpperCase()));
            phonetic = new AtomicReference<>();
            phonetic.set(Phonetic.valueOf(ApplicationProperties.KEY.Phonetic.asStringIfEmpty("none").toUpperCase()));
            
            dataManager = DataManager.createDataManager();
        } catch (IOException ex) {
            LOGGER.error(ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
        
        Thread thread = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    TranslatorEvent tevn = translatorEvents.poll(1000, TimeUnit.MILLISECONDS);
                    if (tevn != null && tevn.isActive()) {
                        try {
                            Dics dics = translate(tevn);
                            String transc = "";
                            if (Phonetic.NONE != phonetic.get()) {
                                transc = oxfordReceiver.getTranscription(normalize(tevn.getText()), 
                                        phonetic.get());
                                if (StringUtils.isBlank(transc)) {
                                    final Phonetic phn = phonetic.get();
                                    final String text = tevn.getText();
                                    CompletableFuture.supplyAsync(() -> transc(text)).thenAccept(s -> {
                                        if (tevn.isActive() && text.equals(tevn.getText()) && phn == phonetic.get()) {
                                            if (!StringUtils.isBlank(s)) doTranslation(tevn);
                                        }
                                    });
                                }
                            }
                            if (tevn.isActive()) {
                                tevn.setTranslation(dics, transc);
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            tevn.setTranslation(new Dics(), "");
                        }
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        thread.setDaemon(true);
        thread.start();
    }
    
    public synchronized Dics translate(TranslatorEvent event) {
        if (StringUtils.isBlank(event.getText())) {
            return new Dics();
        }
        
        String srcNormal = normalize(event.getText());
        String ext = ("" + srcLang + trgLang).toLowerCase();
        Dics dics = cache.get(srcNormal + "." + ext);
        if (dics == null) {
            try {
                String dataStoreName = isSingleWord(srcNormal) ? "dict." + ext : "text." + ext;
                String rawDict = dataManager.read(dataStoreName, srcNormal);
                if (rawDict == null) {
                    delayIfNecessary();
                    dics = googleReceiver.translate(srcNormal, srcLang.get(), trgLang.get());
                    dataManager.save(dataStoreName, srcNormal, dics.getRawText());
                } else {
                    dics = googleReceiver.toDics(rawDict.getBytes());
                }
                dics.setSourceText(srcNormal);
                cache.put(srcNormal + "." + ext, dics);
            } catch (Exception ex) {
                LOGGER.error(ex.getMessage(), ex);
            }
        }
        return dics;
    }
    
    public synchronized String transc(String text) {
        if (StringUtils.isBlank(text) || Phonetic.NONE == phonetic.get()) {
            return "";
        }
        String srcNormal = normalize(text);
        if (!isSingleWord(srcNormal)) {
            return "";
        }
        String transc = oxfordReceiver.getTranscription(srcNormal, phonetic.get());
        if (StringUtils.isBlank(transc)) {
            oxfordReceiver.load(srcNormal, phonetic.get(), false);
            transc = oxfordReceiver.getTranscription(srcNormal, phonetic.get());
        }
        return transc;
    }
    
    public void play(String text) {
        File f = oxfordReceiver.getFile(text, phonetic.get());
        if (f != null) {
            AudioHelper.play(f.getAbsolutePath());
        }
    }
    
    public void doTranslation(TranslatorEvent event) {
        translatorEvents.offer(event);
    }
    
    @Override
    public void setSrc(Language lng) {
        srcLang.set(lng);
    }
    
    @Override
    public void setTrg(Language lng) {
        trgLang.set(lng);
    }
    
    @Override
    public void setPhonetic(Phonetic phonetic) {
        this.phonetic.set(phonetic);
    }
    
    private String normalize(String text) {
        String text2 = StringUtils.defaultString(text, "").replaceAll("\n", " ").trim();
        if (StringUtils.isBlank(text2)) {
            return StringUtils.EMPTY;
        }
        
        int left = 0;
        int right = text2.length() - 1;
        while (left < text2.length()) {
            if (Character.isLetter(text2.charAt(left))) {
                break;
            } else {
                left++;
            }
        }
        while (right >= 0 && right >= left) {
            if (Character.isLetter(text2.charAt(right))) {
                break;
            } else {
                right--;
            }
        }
        if (right == text2.length()) {
            return StringUtils.EMPTY;
        }
        try {
            return text2.substring(left, right + 1).replaceAll("[ ]+", " ").toLowerCase();
        } catch (StringIndexOutOfBoundsException ex) {
            return StringUtils.EMPTY;
        }
    }
    
    private boolean isSingleWord(String text) {
        String s = StringUtils.defaultString(text.trim(), "").trim();
        return !s.matches(".*[ ]+.*");
    }
    
    private synchronized void delayIfNecessary() {
        long delay = System.currentTimeMillis() - lastTimeInMillis;
        if (delay < DELAY_MILLIS) {
            LOGGER.info("Delay in translation is less than the limit " + delay + ", limit is " + DELAY_MILLIS);
            try {
                Thread.sleep(DELAY_MILLIS - delay);
                LOGGER.info("Delay in translation is " + (DELAY_MILLIS - delay));
            } catch (InterruptedException ex) {
                LOGGER.error(ex.getMessage());
                Thread.currentThread().interrupt();
            }
        }
        lastTimeInMillis = System.currentTimeMillis();
    }
    
    static class TranslatedCacheEventListener implements CacheEventListener<String, String> {
        
        @Override
        public void onEvent(CacheEvent<String, String> cacheEvent) {
            
        }
    }
}
