package com.vns.pdf;

import com.google.api.services.drive.model.File;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class GoogleDriveTest {
    private final static String IDENTITY = "identity";
    private final static String VERSION = "version";
    private GoogleDriveStore googleDrive;
    private String dictionaryId = "0B0vbT3TEn4Crc19YamVjN2piNEU";
    private String textId = "0B0vbT3TEn4CrZUNrdDZQLTZsZ0E";
    private String soundId = "0B0vbT3TEn4CrX3lzNW9EWnVidVU";
    
    @Before
    public void up() throws IOException {
        URL u = GoogleDriveStore.class.getClassLoader().getResource("client_secret.json");
        googleDrive = new GoogleDriveStore(u);
    }
    
    @Test
    public void authorize() {
    }
    
    @Test
    public void generateIds() throws IOException {
        List<String> ss = googleDrive.generateIds();
        for (String s : ss) {
            System.out.println(s);
        }
        
        System.out.println();
        
        System.out.println("private String dictionaryId = \"" + ss.get(0) + "\";");
        System.out.println("private String textId = \"" + ss.get(1) + "\";");
        System.out.println("private String soundId = \"" + ss.get(2) + "\";");
    }
    
    @Test
    public void prepareDictionary() throws IOException {
        File root = googleDrive.createFolderExecutor()
                            .setName("dictionary").setId(dictionaryId)
                            .putProperty("", "dictionary").execute();
        File dict = googleDrive.createFolderExecutor()
                            .setName("text").setId(textId)
                            .putProperty(IDENTITY, "dictionary/text")
                            .addParentId(root.getId()).execute();
        File sound = googleDrive.createFolderExecutor().setName("sound").setId(soundId)
                             .putProperty(IDENTITY, "dictionary/sound")
                             .addParentId(root.getId()).execute();
        System.out.printf("%s (%s) (%s)\n", root.getName(), root.getId(), root.getProperties());
        System.out.printf("%s (%s) (%s)\n", dict.getName(), dict.getId(), dict.getProperties());
        System.out.printf("%s (%s) (%s)\n", sound.getName(), sound.getId(), sound.getProperties());
    }
    
    @Test
    public void testList() throws IOException {
        List<File> files = googleDrive.getFilesExecutor()
                                   .putProperty(IDENTITY, "dictionary/text/dict3.enru")
                                   .addParentId(textId).execute();
        System.out.println("Files:");
        for (File file : files) {
            System.out.printf("%s (%s) (%s)\n", file.getName(), file.getId(), file.getProperties());
        }
    }
    
    @Test
    public void testDeleteFile() throws IOException {
        List<File> target = googleDrive.getFilesExecutor().setName("dict.enru").addParentId(textId).execute();
        googleDrive.deleteFileExecutor().setId(target.get(0).getId()).execute();
        List<File> files = googleDrive.getFilesExecutor().addParentId(textId).execute();
        System.out.println("Files:");
        for (File file : files) {
            System.out.printf("%s (%s)\n", file.getName(), file.getId());
        }
    }
    
    @Test
    public void testCreateFile() throws IOException {
        java.io.File content = new java.io.File(getClass().getClassLoader().getResource("google1.txt").getFile());
        String name = "dict3.enru";
        File f = googleDrive.createFileExecutor()
                         .setName(name)
                         .putProperty(IDENTITY, "dictionary/text/" + name)
                         .addParentId(textId).setContent(content).execute();
        System.out.printf("%s (%s) (%s)\n", f.getName(), f.getId(), f.getAppProperties());
    }
    
    @Test
    public void testUpdateFile() throws IOException {
        java.io.File content = new java.io.File(getClass().getClassLoader().getResource("google2.txt").getFile());
        String name = "dict3.enru";
        int version = 2;
        List<File> target = googleDrive.getFilesExecutor().setName(name)
                                    .putProperty(IDENTITY, "dictionary/text/" + name)
                                    .addParentId(textId).execute();
        File f = googleDrive.updateFileExecutor().setId(target.get(0).getId())
                         .putProperty(IDENTITY, "dictionary/text/" + name)
                         .putProperty(VERSION, "" + version)
                         .setContent(content).execute();
        System.out.printf("%s (%s) (%s)\n", f.getName(), f.getId(), f.getProperties());
    }
    
    @Test
    public void testDownload() throws IOException {
        String name = "dict.enru";
        String identity = "dictionary/text/" + name;
        List<File> target = googleDrive.getFilesExecutor().setName(name)
                                    .putProperty(IDENTITY, identity)
                                    .addParentId(textId).execute();
        InputStream in = googleDrive.downloadFileExecutor().setId(target.get(0).getId()).execute();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int size;
            byte[] bs = new byte[1024];
            while ((size = in.read(bs)) != -1) {
                out.write(bs, 0, size);
            }
            System.out.println(new String(out.toByteArray(), "utf-8"));
        } finally {
            in.close();
        }
    }
}
