package com.vns.pdf;


import com.vns.pdf.gmodel.Dics;

public interface GoogleReceiver {
    Dics translate(String text, Language from, Language to) throws Exception;
}
