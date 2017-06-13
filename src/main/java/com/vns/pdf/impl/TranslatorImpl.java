package com.vns.pdf.impl;

import com.vns.pdf.ApplicationProperties;
import com.vns.pdf.DataManager;
import com.vns.pdf.Language;
import com.vns.pdf.Translator;
import com.vns.pdf.gmodel.Dics;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
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
    private final DataManager dataManager;
    private final AtomicReference<Language> srcLang;
    private final AtomicReference<Language> trgLang;
    
    //http://www.ehcache.org/documentation/3.1/cache-event-listeners.html
    private BlockingQueue<TranslatorEvent> translatorEvents = new LinkedBlockingQueue<>();
    
    private long lastTimeInMillis = System.currentTimeMillis();
    private final Object translationSync = new Object();
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
                            if (tevn.isActive()) {
                                tevn.setTranslation(dics);
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            tevn.setTranslation(new Dics());
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
    
    private void delayIfNecessary() {
        synchronized (translationSync) {
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
    }
    
    static class TranslatedCacheEventListener implements CacheEventListener<String, String> {
        
        @Override
        public void onEvent(CacheEvent<String, String> cacheEvent) {
            
        }
    }
}
