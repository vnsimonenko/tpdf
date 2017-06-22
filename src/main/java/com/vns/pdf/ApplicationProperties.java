package com.vns.pdf;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang3.StringUtils;

public interface ApplicationProperties {
    Map<KEY, Object> REGISTRATOR = new HashMap<>();
    
    static <T> T defVal(T... val) {
        return val.length == 0 ? null : val[0];
    }
    
    static void prepareWorkingDir(String workingDir, String srcLang, String trgLang) throws IOException, ConfigurationException {
        Path workingDirPath;
        if (StringUtils.isBlank(workingDir)) {
            String userHomeDir = System.getProperty("user.home");
            boolean isWin = System.getProperty("os.name").startsWith("win");
            String dir = "google-context-translator";
            workingDirPath = isWin ? Paths.get(userHomeDir, dir) : Paths.get(userHomeDir, "." + dir);
        } else {
            workingDirPath = Paths.get(workingDir);
        }
        if (!Files.exists(workingDirPath, LinkOption.NOFOLLOW_LINKS)) {
            Files.createDirectories(workingDirPath, new FileAttribute[0]);
        }
        System.setProperty("workspace", workingDirPath.toAbsolutePath().toString());
        Path applicationPropertiesPath = Paths.get(workingDirPath.toAbsolutePath().toString(), "application.properties");
        if (!Files.exists(applicationPropertiesPath, LinkOption.NOFOLLOW_LINKS)) {
            InputStream in = ApplicationProperties.class.getClassLoader().getResourceAsStream("application.properties");
            Files.copy(in, applicationPropertiesPath);
        }
        PropertiesConfiguration.setDefaultListDelimiter(';');
        PropertiesConfiguration configuration = new PropertiesConfiguration(applicationPropertiesPath.toFile());
        configuration.setProperty("workspace", workingDirPath.toAbsolutePath().toString());
        if (!StringUtils.isBlank(srcLang)) {
            configuration.setProperty("SrcLang", srcLang);
        }
        if (!StringUtils.isBlank(trgLang)) {
            configuration.setProperty("TrgLang", trgLang);
        }
        REGISTRATOR.put(KEY.Configuration, configuration);
    }
    
    default String name() {
        return null;
    }
    
    default Configuration getConfiguration() {
        return (Configuration) REGISTRATOR.get(KEY.Configuration);
    }
    
    default String asString(String... def) {
        return getConfiguration().getString(name(), defVal(def));
    }
    
    default Integer asInt(Integer... def) {
        return getConfiguration().getInt(name(), defVal(def) == null ? 0 : defVal(def));
    }
    
    default Double asDouble(Double... def) {
        return getConfiguration().getDouble(name(), defVal(def));
    }
    
    default Float asFloat(Float... def) {
        return getConfiguration().getFloat(name(), defVal(def));
    }
    
    default Boolean asBoolean(Boolean... def) {
        return getConfiguration().getBoolean(name(), defVal(def));
    }
    
    default List<Object> asList(Object... def) {
        return getConfiguration().getList(name(), Arrays.asList(def));
    }
    
    default List<Integer> asIntegerList(Object... def) {
        return getConfiguration().getList(name(), Arrays.asList(def)).stream().map(new Function<Object, Integer>() {
            @Override
            public Integer apply(Object obj) {
                return obj instanceof Integer ? (Integer) obj : Integer.valueOf(obj.toString());
            }
        }).collect(Collectors.toList());
    }
    
    enum KEY implements ApplicationProperties {
        Configuration,
        DictDir,
        PdfDir,
        Workspace,
        TranslatedLength,
        TranslatedLengthIndex,
        TranslatedRows,
        TranslatedRowsIndex,
        TranslatedDelay,
        TranslatedDelayIndex,
        SrcLang,
        TrgLang,
        GoogleClientSecretUrl,
        GoogleDirectoryId,
        HistoryDir,
        PatternCompile,
        Clipboard
    }
}
