package com.vns.pdf;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataManager {
    private final static Logger logger = LoggerFactory.getLogger(DataManager.class);
    private final static String IDENTITY = "identity";
    private final static String VERSION = "version";
    private final String googleDirectoryId;
    private final Map<String, DataStore> dictStore;
    private GoogleDriveStore gds;
    private Path dictDir;
    
    private DataManager(Path dictDir, String googleDirectoryId, String s) throws IOException {
        this.dictDir = dictDir;
        this.googleDirectoryId = googleDirectoryId;
        
        if (!Files.exists(this.dictDir)) {
            Files.createDirectories(dictDir, new FileAttribute[0]);
        }
        dictStore = new HashMap<>();
        
        if (!StringUtils.isBlank(s) && !StringUtils.isBlank(googleDirectoryId)) {
            try {
                gds = new GoogleDriveStore(Paths.get(s).toUri().toURL());
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    try {
                        synchWithGoogle();
                    } catch (IOException ex) {
                        logger.error(ex.getMessage(), ex);
                    }
                }
                ));
            } catch (Exception ex) {
                logger.error("create GoogleDriveStore", ex);
            }
        }
    }
    
    public static DataManager createDataManager() throws IOException {
        Path dictDir = Paths.get(ApplicationProperties.KEY.DictDir.asString());
        String googleDirectoryId = ApplicationProperties.KEY.GoogleDirectoryId.asString();
        final String s = ApplicationProperties.KEY.GoogleClientSecretUrl.asString();
        return new DataManager(dictDir, googleDirectoryId, s);
    }
    
    public String read(String dataStoreName, String key) throws IOException {
        DataStore ds = getDataStore(dataStoreName);
        return ds.read(key);
    }
    
    public void save(String dataStoreName, String key, String value) throws IOException {
        DataStore ds = getDataStore(dataStoreName);
        ds.save(key, value);
    }
    
    private DataStore getDataStore(String dataStoreName) throws IOException {
        DataStore ds = dictStore.get(dataStoreName);
        if (ds == null) {
            ds = new DataStore();
            File file = Paths.get(dictDir.toAbsolutePath().toString(), dataStoreName).toFile();
            List<com.google.api.services.drive.model.File> gfiles =
                    gds.getFilesExecutor().setName(file.getName()).addParentId(googleDirectoryId).execute();
            synchWithGoogleFile(gfiles.size() > 0 ? gfiles.get(0) : null, file);
            ds.load(file);
            dictStore.put(dataStoreName, ds);
        }
        return ds;
    }
    
    private void synchWithGoogle() throws IOException {
        for (DataStore ds : dictStore.values()) {
            File file = ds.getDbFile();
            List<com.google.api.services.drive.model.File> gfiles =
                    gds.getFilesExecutor().setName(file.getName()).addParentId(googleDirectoryId).execute();
            synchWithGoogleFile(gfiles.size() > 0 ? gfiles.get(0) : null, file);
        }
    }
    
    private void synchWithGoogleFile(com.google.api.services.drive.model.File glFile, File file) throws IOException {
        long fileVersion = file.lastModified();
        if (glFile == null && file.length() > 0) {
            gds.createFileExecutor().setName(file.getName())
                    .addParentId(googleDirectoryId)
                    .putProperty(IDENTITY, file.getName())
                    .putProperty(VERSION, "" + file.lastModified())
                    .setContent(file)
                    .execute();
        } else if (glFile != null) {
            long glVersion = Long.parseLong(glFile.getProperties().get(VERSION));
            if (glVersion < fileVersion) {
                gds.updateFileExecutor().setId(glFile.getId())
                        .putProperty(IDENTITY, glFile.getProperties().get(IDENTITY))
                        .putProperty(VERSION, "" + fileVersion)
                        .setContent(file).execute();
            } else if (glVersion > fileVersion || file.length() == 0) {
                try (InputStream in = gds.downloadFileExecutor().setId(glFile.getId()).execute()) {
                    Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }
}
