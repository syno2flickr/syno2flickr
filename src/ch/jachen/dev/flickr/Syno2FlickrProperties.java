package ch.jachen.dev.flickr;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Properties;

/**
 * Get Flickr properties (sharedKey, api key, ...) Singleton not thread-safe.
 * 
 * @author jbrek
 * 
 */
public class Syno2FlickrProperties {

	private String propertyFile = "syno2flickr.properties"; /// Properties file name
	
	private String apiKey; /// API Key provided by Flickr.com
	private String sharedSecret; /// Shared secret for this app
	private String syncFolder; /// Folder synchronized with your Flickr account
	private String archiveFolder; /// Path to archive folder
	private String errorFolder; /// Path to error folder
	private String defaultSetId; /// Default Set id for new photos
	private Integer defaultPrivacy=4; /// Default privacy for new photos
	private Integer defaultLicense; /// Default license for new photos
	private Integer folderSyncMaxDepth=3; // Maximum deep search in syncFolder
	private Boolean createSet=false; // Create set with parent folder name
	private Boolean createCollection=false; // Create collection with parent folder name of set
	private Boolean showUploadProgress=false; // Show a progress bar in console to see upload progress
	private Boolean showUploadProgressNoCR=false; // Show a progress bar in console to see upload progress without carriage return 
	private Boolean showFoundFilesTree=false; // Display all objects found
	private Boolean showFoundFilesTreeReduced=false; // Display reduced objects found (depending on createSet and createCollection)
	private Boolean debug=false; // Debug mode
	private boolean gotProperties = false; // Flag to know if we already read property file
	
	private Syno2FlickrProperties() {}
 
	/** Holder */
	private static class Syno2FlickrPropertiesHolder {		
		private final static Syno2FlickrProperties instance = new Syno2FlickrProperties();
	}
	
	public static Syno2FlickrProperties getInstance() {
		return Syno2FlickrPropertiesHolder.instance;
	}
	
	/**
	 * Read properties from file
	 * 
	 * @throws Syno2FlickrException
	 *          For any error.
	 */
	private void readProperties() throws Syno2FlickrException {

		Properties prop = new Properties();
		try {
			prop.load(new FileInputStream(propertyFile));
			System.out.println("Properties file '" + propertyFile
							+ "' loaded successfully.");
		} catch (IOException e) {
			System.out.println("Properties file '" + propertyFile + "' not found. Exit.");
			throw new Syno2FlickrException(
					"Cannot read Syno2Flickr properties file. "
							+ "The properties file name must be '"
							+ propertyFile
							+ "' and be placed in the same folder as the program.");
		}
		
		// Read each property
		try{
			apiKey = prop.getProperty("apiKey").trim();
		}catch(Exception e){
			throw new Syno2FlickrException("Error: apiKey is required. Please verify the "+propertyFile+" config file.");
		}
		try{
			sharedSecret = prop.getProperty("sharedSecret").trim();
		}catch(Exception e){
			throw new Syno2FlickrException("Error: sharedSecret is required. Please verify the "+propertyFile+" config file.");
		}
		try{
			syncFolder = prop.getProperty("syncFolder").trim();
		}catch(Exception e){
			throw new Syno2FlickrException("Error: syncFolder is required. Please verify the "+propertyFile+" config file.");
		}
		try{
			archiveFolder = prop.getProperty("archiveFolder").trim();
		} catch(Exception e){}
		try{
			errorFolder = prop.getProperty("errorFolder").trim();
		} catch(Exception e){}
		try{
			defaultSetId = prop.getProperty("defaultSetId").trim();
		} catch(Exception e){}		
		try{
			defaultPrivacy = Integer.parseInt(prop.getProperty("defaultPrivacy").trim());
		}catch(Exception e){}
		try{
			defaultLicense = Integer.parseInt(prop.getProperty("defaultLicense").trim());
		}catch(Exception e){}
		try {
			folderSyncMaxDepth = Integer.parseInt(prop.getProperty("syncFolderMaxDepth").trim());
		} catch (Exception e) {}
		try {
			createSet = Boolean.parseBoolean(prop.getProperty("createSet").trim());
		} catch (Exception e) {}
		try {
			createCollection = Boolean.parseBoolean(prop.getProperty("createCollection").trim());
		} catch (Exception e) {}
		try {
			showUploadProgress = Boolean.parseBoolean(prop.getProperty("showUploadProgress").trim());
		} catch (Exception e) {}
		try {
			showUploadProgressNoCR = Boolean.parseBoolean(prop.getProperty("showUploadProgressNoCR").trim());
		} catch (Exception e) {}
		try {
			showFoundFilesTree = Boolean.parseBoolean(prop.getProperty("showFoundFilesTree").trim());
		} catch (Exception e) {}
		try {
			showFoundFilesTreeReduced = Boolean.parseBoolean(prop.getProperty("showFoundFilesTreeReduced").trim());
		} catch (Exception e) {}
		try {
			debug = Boolean.parseBoolean(prop.getProperty("debug").trim());
		} catch (Exception e) {}		
		
		// Set flag OK
		gotProperties = true;
	}

	
	public String getApiKey() throws Syno2FlickrException {
		if (!gotProperties)
			readProperties();
		return apiKey;
	}
	
	
	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getSharedSecret() throws Syno2FlickrException {
		if (!gotProperties)
			readProperties();
		return sharedSecret;
	}

	public void setSharedSecret(String sharedSecret) {
		this.sharedSecret = sharedSecret;
	}

	public String getSyncFolder() throws Syno2FlickrException {
		if (!gotProperties)
			readProperties();
		return syncFolder;
	}
	
	public void setSyncFolder(String syncFolder) {
		this.syncFolder = syncFolder;
	}

	public String getArchiveFolder() throws Syno2FlickrException {
		if (!gotProperties)
			readProperties();
		return archiveFolder;
	}
	
	public void setArchiveFolder(String archiveFolder) {
		this.archiveFolder = archiveFolder;
	}
	
	public String getErrorFolder() throws Syno2FlickrException {
		if (!gotProperties)
			readProperties();
		return errorFolder;
	}
	
	public void setErrorFolder(String errorFolder) {
		this.errorFolder = errorFolder;
	}

	public String getDefaultSetId() throws Syno2FlickrException {
		if (!gotProperties)
			readProperties();
		return defaultSetId;
	}
	
	public void setDefaultSetId(String defaultSetId) {
		this.defaultSetId = defaultSetId;
	}

	public Integer getDefaultPrivacy() throws Syno2FlickrException {
		if (!gotProperties)
			readProperties();
		return defaultPrivacy;
	}
	
	public void setDefaultPrivacy(Integer defaultPrivacy) {
		this.defaultPrivacy = defaultPrivacy;
	}

	
	public Integer getDefaultLicense() throws Syno2FlickrException {
		if (!gotProperties)
			readProperties();
		return defaultLicense;
	}
	
	public void setDefaultLicense(Integer defaultLicense) {
		this.defaultLicense = defaultLicense;
	}
	
	public Integer getFolderSyncMaxDepth() {
		if (!gotProperties)
			try {
				readProperties();
			} catch (Syno2FlickrException e) {
				return 3;
			}
		return folderSyncMaxDepth;
	}	
	
	public void setFolderSyncMaxDepth(Integer folderSyncMaxDepth) {
		this.folderSyncMaxDepth = folderSyncMaxDepth;
	}
	
	public boolean isCreateSet() {
		if (!gotProperties)
			try {
				readProperties();
			} catch (Syno2FlickrException e) {
				return false;
			}
		return Boolean.TRUE.equals(createSet);
	}
	
	public void setCreateSet(Boolean createSet) {
		this.createSet = createSet;
	}
	
	public boolean isCreateCollection() {
		if (!gotProperties)
			try {
				readProperties();
			} catch (Syno2FlickrException e) {
				return false;
			}
		return Boolean.TRUE.equals(createCollection);
	}
	
	public void setCreateCollection(Boolean createCollection) {
		this.createCollection = createCollection;
	}
	
	public boolean isShowUploadProgress() {
		if (!gotProperties)
			try {
				readProperties();
			} catch (Syno2FlickrException e) {
				return false;
			}
		return Boolean.TRUE.equals(showUploadProgress);
	}
	
	public void setShowUploadProgress(Boolean showUploadProgress) {
		this.showUploadProgress = showUploadProgress;
	}
	
	public boolean isShowUploadProgressNoCR() {
		if (!gotProperties)
			try {
				readProperties();
			} catch (Syno2FlickrException e) {
				return false;
			}
		return Boolean.TRUE.equals(showUploadProgressNoCR);
	}
	
	public boolean isShowFoundFilesTree() {
		if (!gotProperties)
			try {
				readProperties();
			} catch (Syno2FlickrException e) {
				return false;
			}
		return Boolean.TRUE.equals(showFoundFilesTree);
	}
	
	public void setShowFoundFilesTree(Boolean showFoundFilesTree) {
		this.showFoundFilesTree = showFoundFilesTree;
	}
	
	public boolean isShowFoundFilesTreeReduced() {
		if (!gotProperties)
			try {
				readProperties();
			} catch (Syno2FlickrException e) {
				return false;
			}
		return Boolean.TRUE.equals(showFoundFilesTreeReduced);
	}
	
	public void setShowUploadProgressNoCR(Boolean showUploadProgressNoCR) {
		this.showUploadProgressNoCR = showUploadProgressNoCR;
	}

	public void setShowFoundFilesTreeReduced(Boolean showFoundFilesTreeReduced) {
		this.showFoundFilesTreeReduced = showFoundFilesTreeReduced;
	}

	public boolean isDebug() {
		if (!gotProperties)
			try {
				readProperties();
			} catch (Syno2FlickrException e) {
				return false;
			}
		return Boolean.TRUE.equals(debug);
	}

	public String getPropertyFile() {
		return propertyFile;
	}

	public void setPropertyFile(String propertyFile) {
		this.propertyFile = propertyFile;
	}
	
	public void overrideProperties(String[] args) throws Syno2FlickrException{
		readProperties();
		for(String arg : args){
			if(arg.contains("=")){
				String[] entry = arg.split("=", 2);
				String key = entry[0];
				String valueStr = entry[1];
				
				Object value = null;
				Boolean valueBool = null;
				Integer valueInt = null;
				if(valueStr.trim().toLowerCase().matches("true|false"))
					valueBool = Boolean.parseBoolean(valueStr);
				try {
					valueInt = Integer.parseInt(valueStr);
				} catch (Exception e) {}
				
				if(valueBool!=null)
					value = valueBool;
				else if ( valueInt!=null)
					value = valueInt;
				else
					value = valueStr;
				
				try {
					callMethod(new Object[]{ value }, "set"+key.substring(0, 1).toUpperCase()+key.substring(1, key.length()));
					System.out.println("Property '"+key+"' was overriden with value: "+value);
				} catch (Exception e) {
					System.out.println("Warning: argument '"+key+"' was ignored.");
				}
			}
		}
	}
	
	private Object callMethod(Object[] args, String name) throws Exception {
	  Class<?>[] paramTypes = null;
	  if(args != null){
	    paramTypes = new Class[args.length];
	    for(int i=0;i<args.length;++i) {
	      paramTypes[i] = args[i].getClass();
	    }
	  }
	  
	  Method m = this.getClass().getMethod(name,paramTypes);
	  return m.invoke(this,args);
	}
}
