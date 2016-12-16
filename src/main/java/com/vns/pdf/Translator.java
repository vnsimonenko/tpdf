package com.vns.pdf;

import com.vns.pdf.gmodel.Dics;

public interface Translator {
    void doTranslation(TranslatorEvent event);
    
    Dics translate(TranslatorEvent event);
    
    void setSrc(Language lng);
    
    void setTrg(Language lng);
    
    interface TranslatorEvent {
        boolean isActive();
        
        String getText();
        
        void setTranslation(Dics dics);
    }
}
