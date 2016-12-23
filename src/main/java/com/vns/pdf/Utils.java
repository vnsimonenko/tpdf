package com.vns.pdf;

import java.net.URLConnection;
import java.util.List;
import java.util.Map;

public class Utils {
    public static <T, K1> T getMultiplicityValueFromMap(Map<K1, T> map, K1 key, ValueFactory<T> factory) throws IllegalAccessException, InstantiationException {
        T val = map.get(key);
        if (val == null) {
            val = factory.create();
            map.put(key, val);
            return val;
        }
        return val;
    }
    
    /**
     * Получаем куки из сокета
     *
     * @param conn
     * @return String куки
     */
    public static String extractCookie(URLConnection conn) {
        StringBuilder cookieSb = new StringBuilder();
        conn.getHeaderFields().entrySet().stream()
                .filter(ent -> ent.getKey() != null && ent.getKey().equals("Set-Cookie")).forEach(ent -> {
            for (String s : ent.getValue()) {
                cookieSb.append(s);
                cookieSb.append(";");
            }
        });
        return cookieSb.toString();
    }
    
    public static TextArea extractText(float x1, float y1, float x2, float y2, List<TextArea> areas) {
        String s = "";
        float w = 0;
        int minX = Integer.MAX_VALUE;
        int maxX = 0;
        for (TextArea ta : areas) {
            s += ta.getText();
            w += ta.getWeight();
            minX = Math.min(ta.getXmin(), minX);
            maxX = Math.max(ta.getXmax(), maxX);
        }
        int l1 = minX > x1 ? 0 : (int) ((x1 - minX) / w * s.length());
        int l2 = maxX < x2 ? 0 : (int) ((maxX - x2) / w * s.length());
        
        s = s.substring(l1, s.length() - l2);
        
        return new TextArea(s, new TextPoint((int) x1, (int) y1), new TextPoint((int) x2, (int) y2));
    }
    
    public interface ValueFactory<T> {
        T create();
    }
}
