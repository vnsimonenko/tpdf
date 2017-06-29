package com.vns.pdf.impl;

import com.vns.pdf.Phonetic;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import org.apache.commons.lang3.StringUtils;

class FileHolder {
    private String dir;

    public FileHolder(String dir) {
        this.dir = dir;
    }

    public File getOxfordFile(String engWord, Phonetic phonetic) throws UnsupportedEncodingException {
        File dir = Paths.get(this.dir, "oxford", phonetic.name().toLowerCase()).toFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String fileName = engWord + ".mp3";
        File f = Paths.get(dir.getPath(), fileName).toFile();
        return f.exists() ? f : null;
    }

    public File getIvonaFile(String engWord, Phonetic phonetic) throws UnsupportedEncodingException {
        File dir = Paths.get(this.dir, "ivona", phonetic.name().toLowerCase()).toFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String fileName1 = engWord + ".mp3";
        String fileName2 = engWord + ".wav";
        File f1 = Paths.get(dir.getPath(), fileName1).toFile();
        File f2 = Paths.get(dir.getPath(), fileName2).toFile();
        return f1.exists() ? f1 : f2.exists() ? f2 : null;
    }

    public String getTranscription(String engWord, Phonetic phonetic) throws IOException {
        File dir = Paths.get(this.dir, "oxford", phonetic.name().toLowerCase()).toFile();
        String fileName = engWord + ".trn";
        Path path = Paths.get(dir.getPath(), fileName);
        return Files.exists(path, LinkOption.NOFOLLOW_LINKS)
            ? new String(Files.readAllBytes(Paths.get(dir.getPath(), fileName))) : "";
    }

    public File saveOxfordFile(InputStream in, Phonetic phonetic, String enWord, String transcription) throws 
            IOException {
        File dir = Paths.get(this.dir, "oxford", phonetic.name().toLowerCase()).toFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String fileName = enWord + ".mp3";
        Path path = Paths.get(dir.getAbsolutePath(), fileName);
        Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);

        path = Paths.get(dir.getAbsolutePath(), enWord + ".trn");
        if (!StringUtils.isBlank(transcription)) {
            Files.copy(new ByteArrayInputStream(transcription.getBytes()), path, StandardCopyOption.REPLACE_EXISTING);
        }
        File f = Paths.get(dir.getPath(), fileName).toFile();
        return f.exists() ? f : null;
    }

    public File saveIvonaFile(InputStream in, Phonetic phonetic, String enWord, String ext) throws IOException {
        File dir = Paths.get(this.dir, "ivona", phonetic.name().toLowerCase()).toFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String fileName = FileHolder.normalFileName(enWord + "." + ext);
        Path path = Paths.get(dir.getAbsolutePath(), fileName);
        Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
        
        File f = path.toFile();
        return f.exists() ? f : null;
    }

    public static String normalFileName(String fileName) {
        return fileName.replaceAll("[:/]", "_");
    }

    public static String normalFilePath(String filePath) {         
        String fn = filePath.trim()                      
                     .replaceAll("\\?", ".")
                     .replaceAll("\n", " ")
                     .replaceAll(":", ".")
                     .replaceAll("[\\\\]|[/]", "|");
        return fn.length() > 128 ? fn.substring(0, 128) : fn;
    }
}
