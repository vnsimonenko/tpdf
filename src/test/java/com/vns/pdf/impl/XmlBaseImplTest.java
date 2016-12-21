package com.vns.pdf.impl;

import com.vns.pdf.DataStore;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import org.junit.Test;

public class XmlBaseImplTest {
    @Test
    public void loadFile() throws IOException {
        URL u = getClass().getClassLoader().getResource("dictionary.data");        
        DataStore base = new DataStore();
        base.load(Paths.get(u.getFile()).toFile());
    }
    @Test
    public void saveFile() throws IOException {
        URL u1 = getClass().getClassLoader().getResource("dictionary.data");        
        URL u2 = getClass().getClassLoader().getResource("dictionary2.data");
        Files.copy(u2.openStream(), Paths.get(u1.getFile()), StandardCopyOption.REPLACE_EXISTING);
        try {
            DataStore base = new DataStore();
            base.load(Paths.get(u1.getFile()).toFile());
            base.save("text", "{\"sentences\":[{\"trans\":\"книга\",\"orig\":\"book5\",\"backend\":2}],\"dict\":[{\"pos\":\"noun\",\"terms\":[\"книга\",\"книжка\",\"журнал\",\"книжечка\",\"том\",\"текст\",\"часть\",\"литературное произведение\",\"сценарий\",\"библия\",\"телефонная книга\",\"букмекерская книга записи\",\"шесть взяток\",\"конторская книга\",\"сборник отчетов\",\"запись заключаемых пари\",\"либретто\"],\"entry\":[{\"word\":\"книга\",\"reverse_translation\":[\"book\",\"volume\"],\"score\":1},{\"word\":\"книжка\",\"reverse_translation\":[\"book\",\"psalterium\"],\"score\":0.029268308},{\"word\":\"журнал\",\"reverse_translation\":[\"magazine\",\"log\",\"journal\",\"periodical\",\"book\",\"register\"],\"score\":0.0012660227},{\"word\":\"книжечка\",\"reverse_translation\":[\"book\",\"booklet\"],\"score\":0.00022698537},{\"word\":\"том\",\"reverse_translation\":[\"volume\",\"part\",\"tome\",\"book\"],\"score\":9.6111653e-05},{\"word\":\"текст\",\"reverse_translation\":[\"text\",\"word\",\"version\",\"document\",\"book\"],\"score\":7.4851829e-05},{\"word\":\"часть\",\"reverse_translation\":[\"part\",\"portion\",\"piece\",\"section\",\"proportion\",\"book\"],\"score\":1.7231871e-05},{\"word\":\"литературное произведение\",\"reverse_translation\":[\"book\",\"writing\",\"composition\",\"thing\"],\"score\":9.9730414e-06},{\"word\":\"сценарий\",\"reverse_translation\":[\"scenario\",\"script\",\"screenplay\",\"continuity\",\"book\",\"photoplay\"],\"score\":6.4390792e-06},{\"word\":\"библия\",\"reverse_translation\":[\"Bible\",\"book\",\"scripture\",\"Holy Writ\"],\"score\":5.5076362e-06},{\"word\":\"телефонная книга\",\"reverse_translation\":[\"phone book\",\"telephone directory\",\"book\"],\"score\":3.6688768e-06},{\"word\":\"букмекерская книга записи\",\"reverse_translation\":[\"book\"],\"score\":3.6688768e-06},{\"word\":\"шесть взяток\",\"reverse_translation\":[\"book\"],\"score\":3.6688768e-06},{\"word\":\"конторская книга\",\"reverse_translation\":[\"account book\",\"book\"],\"score\":1.8162257e-06},{\"word\":\"сборник отчетов\",\"reverse_translation\":[\"books\",\"book\"],\"score\":1.8162257e-06},{\"word\":\"запись заключаемых пари\",\"reverse_translation\":[\"book\"],\"score\":1.8162257e-06},{\"word\":\"либретто\",\"reverse_translation\":[\"libretto\",\"wordbook\",\"book\"],\"score\":1.8162257e-06}],\"base_form\":\"book\",\"pos_enum\":1},{\"pos\":\"adjective\",\"terms\":[\"книжный\"],\"entry\":[{\"word\":\"книжный\",\"reverse_translation\":[\"book\",\"bookish\",\"literary\"],\"score\":0.016418032}],\"base_form\":\"book\",\"pos_enum\":3},{\"pos\":\"verb\",\"terms\":[\"заказывать\",\"зарегистрировать\",\"заносить в книгу\",\"ангажировать\",\"вносить в книгу\",\"регистрировать\",\"брать\",\"брать билет\",\"принимать заказы на билеты\",\"выдавать билет\",\"заручиться согласием\",\"приглашать\"],\"entry\":[{\"word\":\"заказывать\",\"reverse_translation\":[\"order\",\"book\",\"bespeak\",\"charter\"],\"score\":0.00018817748},{\"word\":\"зарегистрировать\",\"reverse_translation\":[\"book\"],\"score\":9.080557e-06},{\"word\":\"заносить в книгу\",\"reverse_translation\":[\"book\"],\"score\":3.6688768e-06},{\"word\":\"ангажировать\",\"reverse_translation\":[\"book\"],\"score\":2.7264778e-06},{\"word\":\"вносить в книгу\",\"reverse_translation\":[\"book\"],\"score\":1.8162257e-06},{\"word\":\"регистрировать\",\"reverse_translation\":[\"register\",\"record\",\"log\",\"enroll\",\"check in\",\"book\"],\"score\":1.8162257e-06},{\"word\":\"брать\",\"reverse_translation\":[\"take\",\"get\",\"accept\",\"take out\",\"take in\",\"book\"],\"score\":1.8162257e-06},{\"word\":\"брать билет\",\"reverse_translation\":[\"book\"],\"score\":1.8162257e-06},{\"word\":\"принимать заказы на билеты\",\"reverse_translation\":[\"book\"],\"score\":1.8162257e-06},{\"word\":\"выдавать билет\",\"reverse_translation\":[\"book\"],\"score\":1.8162257e-06},{\"word\":\"заручиться согласием\",\"reverse_translation\":[\"book\"],\"score\":1.8162257e-06},{\"word\":\"приглашать\",\"reverse_translation\":[\"invite\",\"ask\",\"call in\",\"retain\",\"ask out\",\"book\"],\"score\":9.1325563e-07}],\"base_form\":\"book\",\"pos_enum\":2}],\"src\":\"en\"}");
            System.out.println(base.read("test"));
        } finally {
            //Files.copy(u2.openStream(), Paths.get(u1.getFile()), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
