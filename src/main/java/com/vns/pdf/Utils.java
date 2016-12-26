package com.vns.pdf;

import java.net.URLConnection;
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
    
    public interface ValueFactory<T> {
        T create();
    }
}
