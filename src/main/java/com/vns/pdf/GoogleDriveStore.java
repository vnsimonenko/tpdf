package com.vns.pdf;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.GeneratedIds;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//https://console.developers.google.com/apis/credentials
//https://developers.google.com/drive/v3/web/search-parameters
//https://google.github.io/google-api-java-client/releases/1.21.0/javadoc/index.html
//https://developers.google.com/resources/api-libraries/documentation/drive/v3/java/latest/
//https://developers.google.com/drive/v2/reference/files/list
//https://developers.google.com/drive/v3/reference/files/list
public class GoogleDriveStore {
    /**
     * Application name.
     */
    private final String APPLICATION_NAME = "Google Drive API";
    
    /**
     * Directory to store user credentials for this application.
     */
    private final java.io.File DATA_STORE_DIR = new java.io.File(System.getProperty("user.home"),
                                                                        ".credentials/drive-java-quickstart");
    /**
     * Global instance of the JSON factory.
     */
    private final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    /**
     * Global instance of the scopes required by this quickstart.
     * <p>
     * If modifying these scopes, delete your previously saved credentials
     * at ~/.credentials/drive-java-quickstart
     */
    private final List<String> SCOPES = Arrays.asList(DriveScopes.DRIVE_FILE, DriveScopes.DRIVE_METADATA);
    private final Logger LOGGER = LoggerFactory.getLogger(GoogleDriveStore.class);
    /**
     * Global instance of the {@link FileDataStoreFactory}.
     */
    private FileDataStoreFactory DATA_STORE_FACTORY;
    /**
     * Global instance of the HTTP transport.
     */
    private HttpTransport HTTP_TRANSPORT;
    private URL clientSecretUrl;
    private Drive service;
    
    public GoogleDriveStore(URL clientSecretUrl) {
        this.clientSecretUrl = clientSecretUrl;
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
        } catch (Throwable t) {
            LOGGER.error(t.getMessage(), t);
            //TODO RuntimeException
            throw new RuntimeException(t);
        }
    }
    
    private File getFile(String id) throws IOException {
        Drive.Files.Get get = getDriveService().files().get(id);
        return get.execute();
    }
    
    //A comma-separated list of spaces to query within the corpus. 
    //Supported values are 'drive', 'appDataFolder' and 'photos'. (string)
    private List<File> getFiles(String fileName, Map<String, String> props, String... folderIds) throws IOException {
        StringBuilder sbQ = new StringBuilder();
        for (String id : folderIds) {
            if (sbQ.length() > 0) {
                sbQ.append(" and ");
            }
            sbQ.append("'");
            sbQ.append(id);
            sbQ.append("' in parents");
        }
        if (!StringUtils.isBlank(fileName)) {
            if (sbQ.length() > 0) {
                sbQ.append(" and ");
            }
            sbQ.append("(name = '");
            sbQ.append(fileName);
            sbQ.append("')");
        }
        if (!props.isEmpty()) {
            for (Map.Entry<String, String> ent : props.entrySet()) {
                if (sbQ.length() > 0) {
                    sbQ.append(" and ");
                }
                sbQ.append("(properties has { key='");
                sbQ.append(ent.getKey());
                sbQ.append("' and value='");
                sbQ.append(ent.getValue());
                sbQ.append("'})");
            }
        }
        FileList result = getDriveService().files().list()
                                  .setPageSize(100)
                                  .setFields("nextPageToken, files(id, name, parents, modifiedTime, properties, " +
                                                     "appProperties)")
                                  .setQ(sbQ.toString())
                                  .setSpaces("drive")
                                  .execute();
        
        List<File> files = result.getFiles();
        if (files == null || files.size() == 0) {
            return Collections.emptyList();
        } else {
            return files;
        }
    }
    
    private File createFolder(String folderName, String id, Map<String, String> props, String parentId) throws IOException {
        File fileMetadata = new File();
        fileMetadata.setName(folderName);
        fileMetadata.setMimeType("application/vnd.google-apps.folder");
        fileMetadata.setId(id);
        if (!StringUtils.isBlank(parentId)) {
            fileMetadata.setParents(Arrays.asList(parentId));
        }
        setProperty(fileMetadata, props);
        
        File file = getDriveService().files().create(fileMetadata)
                            .setFields("id, name, parents, modifiedTime, properties, appProperties")
                            .set("id", id)
                            .execute();
        return file;
        
    }
    
    private File createFile(String name, Map<String, String> props, String parentId, String mimeType, java.io.File content)
            throws IOException {
        File fileMetadata = new File();
        fileMetadata.setName(name);
        fileMetadata.setMimeType(mimeType);
        fileMetadata.setParents(Arrays.asList(parentId));
        setProperty(fileMetadata, props);
        
        
        FileContent mediaContent = new FileContent(mimeType, content);
        File file = getDriveService().files().create(fileMetadata, mediaContent)
                            .setFields("id, name, parents, modifiedTime, properties, appProperties").execute();
        return file;
    }
    
    private File updateFile(File targetFile, Map<String, String> props, java.io.File content) throws IOException {
        File fileMetadata = new File();
        fileMetadata.setName(targetFile.getName());
        fileMetadata.setMimeType(targetFile.getMimeType());
        fileMetadata.setParents(targetFile.getParents());
        setProperty(fileMetadata, props);
        
        FileContent mediaContent = new FileContent(targetFile.getMimeType(), content);
        File file = getDriveService().files().update(targetFile.getId(), fileMetadata, mediaContent)
                            .setFields("id, name, parents, modifiedTime, properties, appProperties").execute();
        return file;
    }
    
    private InputStream downloadFile(File targetFile) throws IOException {
        Drive.Files.Get get = getDriveService().files().get(targetFile.getId());
        return get == null ? null : get.executeMediaAsInputStream();
    }
    
    private void deleteFile(File targetFile) throws IOException {
        getDriveService().files().delete(targetFile.getId()).execute();
    }
    
    public List<String> generateIds() throws IOException {
        int numOfIds = 20;
        GeneratedIds allIds = null;
        allIds = getDriveService().files().generateIds().setSpace("drive").setCount(numOfIds).execute();
        return allIds.getIds();
    }
    
    /**
     * Creates an authorized Credential object.
     *
     * @return an authorized Credential object.
     * @throws IOException
     */
    Credential authorize() throws IOException {
        // Load client secrets.
        InputStream in = clientSecretUrl.openStream();
        try {
            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
            
            // Build flow and trigger user authorization request.
            GoogleAuthorizationCodeFlow flow =
                    new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                            .setDataStoreFactory(DATA_STORE_FACTORY)
                            .setAccessType("offline")
                            .build();
            Credential credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver())
                                            .authorize("edit");
            LOGGER.info("Credentials saved to " + DATA_STORE_DIR.getAbsolutePath());
            return credential;
        } finally {
            in.close();
        }
    }
    
    /**
     * Build and return an authorized Drive client service.
     *
     * @return an authorized Drive client service
     * @throws IOException
     */
    private Drive getDriveService() throws IOException {
        if (service == null) {
            Credential credential = authorize();
            return new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential).setApplicationName(APPLICATION_NAME).build();
        } else {
            return service;
        }
    }
    
    private void setProperty(File file, Map<String, String> props) {
        if (props.isEmpty()) {
            return;
        }
        if (file.getProperties() == null) {
            file.setProperties(new HashMap<>());
        }
        if (file.getAppProperties() == null) {
            file.setAppProperties(new HashMap<>());
        }
        file.getProperties().putAll(props);
        file.getAppProperties().putAll(props);
    }
    
    public GetFilesExecutor getFilesExecutor() {
        class C extends ParameterBuilder<C> implements GetFilesExecutor {
        }
        return new C();
    }
    
    public CreateFolderExecutor createFolderExecutor() {
        class C extends ParameterBuilder<C> implements CreateFolderExecutor {
        }
        return new C();
    }
    
    public CreateFileExecutor createFileExecutor() {
        class C extends ParameterBuilder<C> implements CreateFileExecutor {
        }
        return new C();
    }
    
    public UpdateFileBuilder updateFileExecutor() {
        class C extends ParameterBuilder<C> implements UpdateFileBuilder {
        }
        return new C();
    }
    
    public DeleteFileExecutor deleteFileExecutor() {
        class C extends ParameterBuilder<C> implements DeleteFileExecutor {
        }
        return new C();
    }
    
    public DownloadFileBuilder downloadFileExecutor() {
        class C extends ParameterBuilder<C> implements DownloadFileBuilder {
        }
        return new C();
    }
    
    interface Executor<T> {
        T execute() throws Exception;
    }
    
    interface GetFilesExecutor extends Executor<List<File>> {
        GetFilesExecutor setName(String name);
        
        GetFilesExecutor putProperty(String key, String value);
        
        GetFilesExecutor addParentId(String parentId);
        
        default List<File> execute() throws IOException {
            ParameterBuilder b = (ParameterBuilder) this;
            return b.getDrive().getFiles(b.name, b.properties, (String[]) b.parentIds.toArray(new String[0]));
        }
    }
    
    interface CreateFolderExecutor extends Executor<File> {
        CreateFolderExecutor setId(String id);
        
        CreateFolderExecutor setName(String name);
        
        CreateFolderExecutor putProperty(String key, String value);
        
        CreateFolderExecutor addParentId(String parentId);
        
        default File execute() throws IOException {
            ParameterBuilder b = (ParameterBuilder) this;
            return b.getDrive().createFolder(b.name, b.id, b.properties, b.getFirstParentId());
        }
    }
    
    interface CreateFileExecutor extends Executor<File> {
        CreateFileExecutor setName(String name);
        
        CreateFileExecutor putProperty(String key, String value);
        
        CreateFileExecutor addParentId(String parentId);
        
        CreateFileExecutor setMimeType(String mimeType);
        
        CreateFileExecutor setContent(java.io.File content);
        
        default File execute() throws IOException {
            ParameterBuilder b = (ParameterBuilder) this;
            return b.getDrive().createFile(b.name, b.properties, b.getFirstParentId(), b.mimeType, b.content);
        }
    }
    
    interface DeleteFileExecutor extends Executor<Void> {
        DeleteFileExecutor setId(String id);
        
        default Void execute() throws IOException {
            ParameterBuilder b = (ParameterBuilder) this;
            File targetFile = b.getDrive().getFile(b.id);
            b.getDrive().deleteFile(targetFile);
            return null;
        }
    }
    
    interface DownloadFileBuilder {
        DownloadFileBuilder setId(String id);
        
        default InputStream execute() throws IOException {
            ParameterBuilder b = (ParameterBuilder) this;
            File targetFile = b.getDrive().getFile(b.id);
            return b.getDrive().downloadFile(targetFile);
        }
    }
    
    interface UpdateFileBuilder extends CreateFileExecutor {
        UpdateFileBuilder setId(String id);
        
        UpdateFileBuilder putProperty(String key, String value);
        
        UpdateFileBuilder setContent(java.io.File content);
        
        default File execute() throws IOException {
            ParameterBuilder b = (ParameterBuilder) this;
            File targetFile = b.getDrive().getFile(b.id);
            return b.getDrive().updateFile(targetFile, b.properties, b.content);
        }
    }
    
    private abstract class ParameterBuilder<T extends ParameterBuilder> {
        private String id;
        private String name;
        private Set<String> parentIds = Collections.emptySet();
        private Map<String, String> properties = Collections.emptyMap();
        private String mimeType = "text/plain";
        private java.io.File content;
        
        public T setId(String id) {
            this.id = id;
            return (T) this;
            
        }
        
        public T setName(String name) {
            this.name = name;
            return (T) this;
        }
        
        public T addParentId(String parentId) {
            if (parentIds == Collections.<String>emptySet()) {
                parentIds = new HashSet<>();
            }
            parentIds.add(parentId);
            return (T) this;
        }
        
        public T putProperty(String key, String value) {
            if (properties == Collections.<String, String>emptyMap()) {
                properties = new HashMap<>();
            }
            properties.put(key, value);
            return (T) this;
        }
        
        public T setMimeType(String mimeType) {
            this.mimeType = mimeType;
            return (T) this;
        }
        
        public T setContent(java.io.File content) {
            this.content = content;
            return (T) this;
        }
        
        private GoogleDriveStore getDrive() {
            return GoogleDriveStore.this;
        }
        
        private String getFirstParentId() {
            ParameterBuilder b = (ParameterBuilder) this;
            Iterator<String> it = b.parentIds.iterator();
            return it.hasNext() ? it.next() : null;
        }
    }
}
