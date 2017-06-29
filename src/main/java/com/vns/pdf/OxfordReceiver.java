package com.vns.pdf;


import java.io.InputStream;

public interface OxfordReceiver {     
    InputStream load(String enWord, Phonetic phonetic, boolean exactly);
    String getTranscription(String enWord, Phonetic phonetic);
}
