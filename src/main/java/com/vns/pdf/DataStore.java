package com.vns.pdf;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataStore {
    private final static Logger LOGGER = LoggerFactory.getLogger(DataStore.class);
    private Map<String, FileMetaInfo> indexes = new HashMap<>();
    private File dbFile;
    
    public DataStore(File dbFile) throws IOException {
        load(dbFile);
    }
    
    public boolean isEmpty() {
        return indexes.isEmpty();
    }
    
    public void reload() throws IOException {
        load(dbFile);
    }
    
    private void load(File dbFile) throws IOException {
        this.dbFile = dbFile;
        indexes = new HashMap<>();
        fillIndex();
    }
    
    private void fillIndex() throws IOException {
        indexes.clear();
        RandomAccessFile file = new RandomAccessFile(dbFile, "rw");
        try {
            long pos = 0;
            String key;
            while ((key = file.readLine()) != null) {
                long posStart = pos;
                int size = Integer.parseInt(file.readLine()) + 1;
                long posData = file.getFilePointer();
                if (file.skipBytes(size) != size) {
                    //TODO RuntimeException
                    throw new RuntimeException();
                }
                pos = file.getFilePointer();
                indexes.put(key, new FileMetaInfo(posStart, posData, pos));
            }
        } catch (EOFException ex) {
            LOGGER.error(ex.getMessage(), ex);
        } finally {
            file.close();
        }
    }
    
    public void save(String key, String value) throws IOException {
        if (indexes.containsKey(key)) {
            long from = indexes.get(key).endPos;
            Long to = indexes.get(key).startPos;
            truncate(from, to);
            fillIndex();
        }
        RandomAccessFile f = new RandomAccessFile(dbFile, "rw");
        try {
            f.seek(f.length());
            long startPos = f.getFilePointer();
            f.write((key + '\n').getBytes("utf-8"));
            byte[] bs = value.getBytes("utf-8");
            f.write(("" + bs.length + "\n").getBytes("utf-8"));
            long dataPos = f.getFilePointer();
            f.write(bs);
            f.write('\n');
            long endPos = f.getFilePointer();
            indexes.put(key, new FileMetaInfo(startPos, dataPos, endPos));
        } finally {
            f.close();
        }
    }
    
    public void merge(InputStream in) throws IOException {
        Path p = Files.createTempFile("tpdf1", "_" + System.currentTimeMillis(), new FileAttribute[0]);
        try {
            Files.copy(in, p, StandardCopyOption.REPLACE_EXISTING);
            DataStore ds = new DataStore(p.toFile());
            for (String key : ds.indexes.keySet()) {
                if (!indexes.containsKey(key)) {
                    save(key, ds.read(key));
                }
            }
        } finally {
            Files.deleteIfExists(p);
        }
    }
    
    private void truncate(long from, long to) throws IOException {
        RandomAccessFile f2 = new RandomAccessFile(dbFile, "rw");
        try {
            RandomAccessFile f1 = new RandomAccessFile(dbFile, "r");
            try {
                long fileLength = f1.length();
                f1.seek(from);
                f2.seek(to);
                byte[] bs = new byte[1024];
                int readCount;
                long offset = from;
                while ((readCount = f1.read(bs)) != -1 && offset != fileLength) {
                    System.out.println(f1.getFilePointer());
                    f2.write(bs, 0, readCount);
                    offset += readCount;
                }
            } finally {
                f1.close();
            }
            f2.setLength(f2.getFilePointer());
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
        } finally {
            f2.close();
        }
    }
    
    public String read(String key) throws IOException {
        FileMetaInfo meta = indexes.get(key);
        if (meta != null) {
            RandomAccessFile f = new RandomAccessFile(dbFile, "r");
            try {
                byte[] bs = new byte[(int) (meta.endPos - meta.dataPos)];
                f.seek(meta.dataPos);
                f.readFully(bs);
                return new String(bs, "utf-8");
            } finally {
                f.close();
            }
        }
        return null;
    }
    
    public File getDbFile() {
        return dbFile;
    }
    
    private static class FileMetaInfo {
        final long startPos;
        final long dataPos;
        final long endPos;
        
        public FileMetaInfo(long startPos, long dataPos, long endPos) {
            this.startPos = startPos;
            this.dataPos = dataPos;
            this.endPos = endPos;
        }
    }
}
