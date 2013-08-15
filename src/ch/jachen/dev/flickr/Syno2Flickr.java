package ch.jachen.dev.flickr;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.jickr.Auth;
import org.jickr.Flickr;
import org.jickr.FlickrException;
import org.jickr.FlickrRuntimeException;
import org.jickr.License;
import org.jickr.License.LicenseType;
import org.jickr.MimeType;
import org.jickr.Permission;
import org.jickr.Photo;
import org.jickr.PhotoCollection;
import org.jickr.PhotoSet;
import org.jickr.PhotoUpload;
import org.jickr.Privacy;
import org.jickr.RequestEvent;
import org.jickr.RequestListener;
import org.jickr.User;
import org.jickr.UserLimitations;

import ch.jachen.dev.util.DateUtils;
import ch.jachen.dev.util.Node;
import ch.jachen.dev.util.Tree;


/**
 * Main class of Syno2Flickr application
 * 
 * Java command line tool called "Syno2Flickr" allowing to upload your photos and videos to your flickr account.
 * The process can be run in interactive or background mode.
 * 
 * @author jbrek 
 */

public class Syno2Flickr {

	/**
	 * Progress bar for console output
	 * @param progressPercentage progress (0.0. to 1.0)
	 */
	private static Stack<Thread> progressList = new Stack<Thread>();
	private static void stopAllThreads(){		
		while(!progressList.empty()){
			ProgressUpload p = (ProgressUpload) progressList.pop();
			p.stopRun();
		}
	}	
	
	/**
	 * Interrupt all threads
	 * Interrupt animation (used in error/exception case)
	 */
	private static void interruptAllThreads(){
		
		while(!progressList.empty()){
			ProgressUpload p = (ProgressUpload) progressList.pop();
			p.interruptRun();
		}
	}
	
	/**
	 * Authentication to flickr
	 * Authenticates to flickr and register this app to the flickr account if needed
	 * 
	 * @param user
	 * @param perm
	 */
	private static User authenticationToFlickr(User user, Permission perm){
		
		// Establish Flickr auth. or ask permission to
		try {
			user = Auth.getDefaultAuthUser(); // Check to see if we've already
												// authenticated
		} catch (FlickrException e1) {
			user = null;
		}

		try {

			// if need to authenticated
			if (user == null || !Auth.isAuthenticated(user, perm)) {

				// Give the URL to enter in the browser
				System.out
						.println("\nPlease enter the following URL into a browser, "
								+ "follow the instructions, and then come back and press ENTER");
				String authURL = Auth.getAuthURL(perm);
				System.out.println(authURL);

				// Wait for them to come back
				BufferedReader in = new BufferedReader(new InputStreamReader(
						System.in));
				in.readLine();

				// Alright, now that we're cleared with Flickr, let's try to
				// authenticate
				System.out.println("Trying to get " + perm + " access.");

				try {
					user = Auth.authenticate();
				} catch (FlickrException ex) {
					System.out.println("Failure to authenticate.");
					System.exit(1);
				}
				if (Auth.isAuthenticated(user, perm)) {
					System.out.println("We're authenticated with "
									+ Auth.getPermLevel(user) + " access.");
					Auth.setDefaultAuthUser(user);
				} else {
					// Shouldn't ever get here - we throw an exception above if
					// we
					// can't authenticate.
					System.out.println("Oddly unauthenticated");
					System.exit(2);
				}

			} // if (user == null || !Auth.isAuthenticated(user, perm)) {
		
		} catch (FlickrException e1) {
			System.out.println("Error while Flickr authentication\n" + e1.getMessage());
			System.exit(0);
		} catch (IOException e1) {
			System.out.println("Error while Flickr authentication\n" + e1.getMessage());
			System.exit(0);
		}

		// Set authentication context for user (credentials)
		Auth.resetAuthContext();
		Auth.setAuthContext(user);
		try {
			user = User.findByNSID(user.getNSID());
		} catch (FlickrException e) {
			System.out.println("Error: can't retrieve user NSID: "+user.getNSID());
		}
		
		// Show infos
		System.out.println("\n\nHello "+user.getRealName());
		System.out.println("\tYou have " +user.getPhotoCount()+" photos");
		
		return user;
	
	}
	
	/**
	 * Welcome message
	 */
	public static void printWelcome(){
		System.out.print("\n");
		System.out.println("**********************************************************************");
		System.out.println("* Syno2Flickr v0.2.0 - 08/2013                                       *");
		System.out.println("*    https://github.com/syno2flickr/syno2flickr                      *");
		System.out.println("**********************************************************************");		
	}
	
	
	/**
	 * Main method
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		// Initialization
		String syncFolder = null; // folder to upload
		String archiveFolderString = null; // archive folder
		String errorFolderString = null; // error folder
		String defaultSetId = null; // id of default set for new photos
		Privacy defaultPrivacy = null; // default privacy
		LicenseType defaultLicense = null; // default licence
		org.jickr.Permission perm = Permission.WRITE; // access level we want
		org.jickr.User user = null; // User Flickr authenticated

		// Welcome
		printWelcome();
		
		// Get property values
		try {

			// Test first arg (must be the path of the properties file)
			if (args != null && args.length > 0)
				if (new File(args[0]).exists())
					Syno2FlickrProperties.getInstance().setPropertyFile(args[0]);
			
			// Override properties
			Syno2FlickrProperties.getInstance().overrideProperties(args);

			// Set key/secret
			Flickr.setApiKey(Syno2FlickrProperties.getInstance().getApiKey());
			Flickr.setSharedSecret(Syno2FlickrProperties.getInstance().getSharedSecret());

			// Get folder to sync
			syncFolder = Syno2FlickrProperties.getInstance().getSyncFolder();
			
			// Archive folder
			archiveFolderString = Syno2FlickrProperties.getInstance().getArchiveFolder();
			
			// Error folder
			errorFolderString = Syno2FlickrProperties.getInstance().getErrorFolder();
			
			// Default set id
			defaultSetId = Syno2FlickrProperties.getInstance().getDefaultSetId();
			
			// Default privacy
			try {
				defaultPrivacy = Privacy.valueOf(Syno2FlickrProperties.getInstance().getDefaultPrivacy());
			} catch (Exception e){
				// Error while getting the default privacy
				System.out.println("Error while getting default privacy, value must be between 0 and 4. Default privacy will be set to private");
				defaultPrivacy = Privacy.PRIVATE;
			}
			
			// Default license
			try {
				defaultLicense = LicenseType.getEnumFromValue(Syno2FlickrProperties.getInstance().getDefaultLicense());
			} catch (Exception e){}
			
		} catch (Syno2FlickrException e) {
			// Error message
			System.out.println(e.getMessage());
			System.exit(0);
		} 

		// Auth
		user = authenticationToFlickr(user, perm);
		Date start = new Date();
		
		// Check default set
		PhotoSet defaultSet=null;
		try {
			if(defaultSetId!=null)
				defaultSet = PhotoSet.findByID(defaultSetId);
		} catch (FlickrException e3) {
			System.out.println("Error while getting default set (defaulSetId="+defaultSetId+").\n"+e3.getMessage());
		}
		
		// Show params
		System.out.println("\n\nParameters:");
		System.out.println("\tSync folder: "+syncFolder);
		System.out.println("\tArchive folder: "+ (archiveFolderString==null ? "undefined" : archiveFolderString));
		System.out.println("\tError folder: "+ (errorFolderString==null ? "undefined" : errorFolderString));
		if(defaultSet!=null)
			System.out.println("\tDefault Set: "+defaultSet.getTitle());
		else
			System.out.println("\tDefault Set: none");
		System.out.println("\tDefault privacy: "+defaultPrivacy);
		if(defaultLicense!=null)
			System.out.println("\tDefault License: "+defaultLicense.getTitle());
		
		// Get user upload limitations
		UserLimitations userLimits = new UserLimitations();
		
		// Show limit / usage
		try {
			System.out.println("\n\n"+userLimits.showUsageAndLimitations());
		} catch (FlickrException e2) {
			System.out.println("Error: impossible to show usage and restrictions for user "+user.getUserName()+"\n"+e2.getMessage());
		}		
				
		// Sync folder
		File folder = new File(syncFolder);
		
		// Check upload folder
		if (!folder.exists()){
			System.out.println("Error: sync folder does not exist ("+folder.getPath()+"). Please verify the "+Syno2FlickrProperties.getInstance().getPropertyFile()+" config file.");
			System.exit(0);
		}
		
		// Synchronize (upload photos/video)
		final long denomMega=1024L*1024L;
		File archiveFolder = null;
		try{
			archiveFolder = new File(archiveFolderString);
		} catch (Exception e) {}
		File errorFolder = null;
		try{
			errorFolder = new File(errorFolderString);
		} catch (Exception e) {}
		
		// Build sync folder tree
		Set<File> excludes = new HashSet<File>();
		if(archiveFolder!=null && archiveFolder.exists())
			excludes.add(archiveFolder);
		if(errorFolder!=null && errorFolder.exists())
			excludes.add(errorFolder);
		Tree syncFolderTree = new Tree(folder, Syno2FlickrProperties.getInstance().getFolderSyncMaxDepth(), excludes);
		
		if(syncFolderTree.isEmpty())
			System.out.println("No file to upload.");
		else {
			// Get Set / Collection info
			boolean createCollection = Syno2FlickrProperties.getInstance().isCreateCollection();
			boolean createSet = createCollection || Syno2FlickrProperties.getInstance().isCreateSet();
			List<PhotoSet> userSets = null;
			List<PhotoCollection> userCollections = null;
			if(createCollection || createSet)
				try {
					System.out.print("\nRetrieving existing sets and collections...");
					userSets = user.getPhotoSets();
					userCollections = user.getCollections(false);
					System.out.println(" OK.\n");
				} catch (FlickrException e) {
					System.out.println("Error while retrieving your collections and sets. Photos will not be organized.");
				}
			
			// Show system directory tree and files structure of syncFolder
			if(Syno2FlickrProperties.getInstance().isShowFoundFilesTree()){
				System.out.println("\nSystem directory tree of found files in "+folder.getPath()+":");
				syncFolderTree.printTree();
			}
			
			// Collections, sets and photos found of syncFolder
			if(Syno2FlickrProperties.getInstance().isShowFoundFilesTreeReduced()){
				int level=0;
				if(createCollection){
					System.out.println("\nCollections, sets and photos found:");
					level = Syno2FlickrProperties.getInstance().getFolderSyncMaxDepth();
				}
				else if(createSet){
					System.out.println("\nSets and photos found:");
					level = 1;
				}
				else
					System.out.println("\nPhotos found:");
				syncFolderTree.printTree(level);
			}
							
			// Show nb files founds
			final int nbFilesFound = syncFolderTree.countFiles();
			System.out.println("\nStart uploading: " + nbFilesFound + " file"+(nbFilesFound>1?"s":"")+" found to upload");
			
			// Check archive folder folder
			if (archiveFolder==null || !archiveFolder.exists()){
				System.out.println("Warning: archive folder does not exist ("+(archiveFolder==null ? "property not found" : archiveFolder.getPath())+"). Successfully sent files will not be moved.");
			}
			
			// Check error folder folder
			if (errorFolder==null || !errorFolder.exists()){
				System.out.println("Warning: error folder does not exist ("+(errorFolder==null ? "property not found" : errorFolder.getPath())+"). Unsuccessfully sent files will not be moved.");
			}
			
			int noFile = 0;
			try {
				
				// Get bandwith remaining
				long bandwidthRemaingBytes = userLimits.getBandwidthRemainingBytes();
				
				// Get all files to upload
				for (Node node : syncFolderTree.getRoot().getFiles()) {
					// Get file
					File f = node.getFile();
					
					// init
					noFile++;
					
					// Check limitations/file type
					if(!userLimits.isBandwidthUnlimited() && (bandwidthRemaingBytes-f.length()) < 0){
						System.out.println("Error: user " + user.getUserName() + 
										   " has reached his monthly bandwidth limit ("+
										   userLimits.getBandwidthMaxBytes()/denomMega+
										   "MB). Upload cancelled.");
						break;
					}
					String mime=null;
					try {
						mime = MimeType.getMimeType("file://"+f.getPath());
					} catch (IOException e1) {}
					
					if (mime!=null && mime.contains("image")) {
						if (f.length() > userLimits.getFilesizeMaxBytes()){
							System.out.println("Error: image \"" + f.getName()+ "\" exceeds the maximum accepted size (max. "+
													userLimits.getFilesizeMaxBytes()/denomMega+"MB). Skipped.");
							continue;
						}
							
					} else {
						if (f.length() > userLimits.getVideosizeMaxBytes()){
							System.out.println("Error: \"" + f.getName()+ 
											   "\" exceeds the maximum accepted size (max. "+
											   userLimits.getVideosizeMaxBytes()/denomMega+"MB). Skipped.");
							continue;
						}
					}
					
					// Info
					final boolean showNoCr = Syno2FlickrProperties.getInstance().isShowUploadProgressNoCR() && 
						  						!Syno2FlickrProperties.getInstance().isShowUploadProgress();
					System.out.println("\nSending "+f.getName()+ " ("+(showNoCr ? "size: "+String.format("%.1fMB", f.length()/(1024d*1024d))+", no: " : "")+noFile+"/"+nbFilesFound+"):");
					
					// Generate metadata for the upload
					PhotoUpload uploader = new PhotoUpload.Builder(f)
																.familyFlag(defaultPrivacy.equals(Privacy.FAMILY) || defaultPrivacy.equals(Privacy.FRIENDSANDFAMILY))
																.friendFlag(defaultPrivacy.equals(Privacy.FRIENDS) || defaultPrivacy.equals(Privacy.FRIENDSANDFAMILY))
																.publicFlag(defaultPrivacy.equals(Privacy.PUBLIC))
																.build();
		
					try {
						
						// Upload the content
						final int maxWidth=75;
						boolean showProgress = Syno2FlickrProperties.getInstance().isShowUploadProgress() || Syno2FlickrProperties.getInstance().isShowUploadProgressNoCR();
						if(showNoCr){
							// No carriage return mode
							System.out.print("< 0% ");
							for(int i=0; i < maxWidth - "0% ".length() - " 100%".length(); i++)
								System.out.print("-");
							System.out.println(" 100% >");
							System.out.print("< ");	
						}
						String uploadedPhotoId = Photo.uploadNewPhoto(uploader, showProgress ? new RequestListener() {	
							private int progressNoCr=0;
							
							@Override
							public void progressRequest(RequestEvent event) {
								if(showNoCr){
									// Show light animation (without carriage return)
									if (event.getProgress()<event.getTotalProgress()){
										int ratio = (int) ((double)event.getProgress() / event.getTotalProgress() * (double)maxWidth);
										int steps = ratio - progressNoCr;
										for(int i=0; i < steps; i++){
											System.out.print("=");
											System.out.flush();
										}
										progressNoCr += steps;
									} else {
										int steps = maxWidth - progressNoCr;
										for(int i=0; i < steps; i++){
											System.out.print("=");
											System.out.flush();
										}
										System.out.println(" >");
									}
								} else {
									// Show animate progess bar
									stopAllThreads();
									ProgressUpload r = new ProgressUpload(event.getProgress(), 
																		  event.getTotalProgress());
									progressList.push(r);
									r.start();	
								}
							}
						} : null); 
						// Stop all threads
						stopAllThreads();
		
						// Set default license if applicable
						if(defaultLicense!=null){
							try {
								License.updateLicense(uploadedPhotoId, defaultLicense);
								System.out.println("License \""+defaultLicense.getTitle()+"\" set for photo \""+node.getFile().getName()+"\" (id: "+uploadedPhotoId+")");
							} catch(FlickrException e){
								System.out.println("Error while updating licence \""+defaultLicense.getTitle()+"\" for photo (id: "+uploadedPhotoId+")\n"+e.getMessage());
							}
						}
						
						// Put in default set
						if (defaultSet!=null && node.isStandaloneFile()){
							try {
								defaultSet.add(uploadedPhotoId);
								System.out.println("Photo "+node.getFile().getName()+" (id: "+uploadedPhotoId+") was added to set \""+defaultSet.getTitle()+"\" (id: "+defaultSet.getID()+").");
							} catch(FlickrException e){
								System.out.println("Error while adding photo (id: "+uploadedPhotoId+") to set \""+defaultSet.getTitle()+"\" (id: "+defaultSet.getID()+").\n"+e.getMessage());
							}
						}
						
						// Set / Collection
						if(createSet && !node.isStandaloneFile() && userCollections!=null){
							PhotoSet set = null;
							String setName = node.getParent().getFile().getName();
							
							// Check set existance
							for(PhotoSet ps : userSets){
								if(ps.getTitle().equals(setName)){
									set = ps;
									break;
								}
							}
							if(set==null){
								try{
									set = PhotoSet.findByID(PhotoSet.newPhotoSet(setName, "", uploadedPhotoId));
									userSets = user.getPhotoSets();				
									System.out.println("Set \""+setName+"\" (id: "+set.getID()+") was created. Photo "+node.getFile().getName()+" (id: "+uploadedPhotoId+") represents this set.");
								} catch (FlickrException e){
									System.out.println("Error while create set \""+setName+"\" for photo (id: "+uploadedPhotoId+")\n"+e.getMessage());
								}
							} else {			
								try{
									// Link Photo to set
									set.add(uploadedPhotoId);
									System.out.println("Photo "+node.getFile().getName()+" (id: "+uploadedPhotoId+") was added to set \""+set.getTitle()+"\" (id: "+set.getID()+").");
								} catch (FlickrException e){
									System.out.println("Error while adding photo (id: "+uploadedPhotoId+") to set \""+setName+"\".\n"+e.getMessage());
								}
							}
							
							// Collections parent
							if(createCollection){
								PhotoCollection collection = null;
								PhotoCollection parentCollection = null;
								Node currentCollection = node.getParent().getParent();
								if(currentCollection!=null && !currentCollection.isRoot()){
									List<Node> nodes = currentCollection.getParents();
									nodes.add(0, currentCollection);
									Collections.reverse(nodes);
									for(Node n : nodes){
										if(!n.isRoot()){
											String collectionName = n.getFile().getName();
											if(collectionName==null || collectionName.length()==0)
												throw new FlickrRuntimeException("Fatal error: folder name is null or empty");
											// Check collection existance
											for(PhotoCollection pc : userCollections){
												PhotoCollection result = pc.findCollectionByName(collectionName);
												if(result!=null){
													collection = result;
													break;
												}
											}
											if(collection==null){
												String parentId = null;
												if(parentCollection!=null)
													parentId = parentCollection.getId(); 
												collection = PhotoCollection.findByID(PhotoCollection.newPhotoCollection(collectionName, "", parentId, null));
												userCollections = user.getCollections(false);
												System.out.println("Collection \""+collection.getTitle()+"\" (id: "+collection.getId()+") was created"+
																	(parentCollection!=null?" with parent collection \""+parentCollection.getTitle()+
																			"\" (id: "+parentCollection.getId()+")":"")+".");
											} 
											
											// Link to Set
											if(collection!=null && n.equals(currentCollection)){
												try {
													boolean created = collection.addSet(set);
													if(created)
														System.out.println("Set \""+set.getTitle()+"\" (id: "+set.getID()+") was added to collection \""+collection.getTitle()+"\" (id: "+collection.getId()+").");
												} catch (FlickrException e){
													System.out.println("Error while adding set \""+set.getTitle()+"\" (id: "+set.getID()+") to collection \""+collection.getTitle()+"\" (id: "+collection.getId()+").\n"+e.getMessage());
												}
											}
											parentCollection = collection;
											collection = null;
										}
									}
								}
							}						
						}					
						
						// Move uploaded file to archive
						if (archiveFolder!=null && archiveFolder.exists()){
							String targetDir = archiveFolder.getPath() + f.getPath().replace(syncFolder, "").replace(f.getName(), "");
							File archivedFile = new File(targetDir);
							archivedFile.mkdirs();
							f.renameTo(new File(archivedFile.getPath()+File.separator+f.getName()));
						}
						
						// Update bandwidth remaining
						if (!userLimits.isBandwidthUnlimited()) 
							bandwidthRemaingBytes-=f.length();
		
					} catch (FlickrException e) {					
						interruptAllThreads();					
						System.out.println("\nERROR: An error occured while uploading file "
								+ f.getPath()+":\n"+e.getMessage());
						
						if (e.getCode()==6 || e.getCode()==-999) break;
						// Move file to error
						if (errorFolder!=null && errorFolder.exists()){						
							String targetDir = errorFolder.getPath() + f.getPath().replace(syncFolder, "").replace(f.getName(), "");
							File errorFile = new File(targetDir);
							errorFile.mkdirs();
							f.renameTo(new File(errorFile.getPath()+File.separator+f.getName()));
						}
					}
				}
				
				// Summary
				Date end = new Date();
				try { Thread.sleep(500); } catch (InterruptedException e) {}
				System.out.println("\n\nSend completed in "+DateUtils.getFormatedTimeElapsed(start, end));
				
			} catch (FlickrException e){
				System.out.println("\nA grave error occurs:\n"+e.getMessage());
	
			}
		}

		// Bye
		try { Thread.sleep(500); } catch (InterruptedException e) {}
		System.out.println("\nBye.");
	}

}

/**
 * Class to animate progress uploading in Console output
 * @author jbrek
 *
 */
class ProgressUpload extends Thread{

	private final double progress;
	private final double total;
	private boolean stop=false;
	private boolean interrupt=false;
	private static long calls = 0;
	
	public ProgressUpload(double progress, double total) {
		this.progress = progress;
		this.total = total;
	}
	
	private final static int width = 40; // progress bar width in chars
	private final static char[] animationProgressChars = new char[] { '-', '\\', '|', '/' };
	
	/**
	 * Show progress of upload in Console
	 * @param progress progress in bytes
	 * @param total	total in bytes
	 * @param finished is the progress finished
	 */
	public static void showProgress(double progress, double total, boolean finished){		
		double progressPercentage = progress / total;
		System.out.print("\rProcessing: |");
		int i = 0;
		int max = 0;
		for (; i <= (max = (int) (progressPercentage * width)); i++) {
			System.out.print(((i < max || finished) ? "="
					: animationProgressChars[(int)(calls++ % 4)]));
		}
		for (; i <= width; i++) {
			System.out.print(" ");
		}
		System.out.print("| "
				+ String.format("%3d", (finished ? 100 : progressPercentage==1?99:(int) (progressPercentage * 100)))
				+ "% (" + String.format("%.1fMB/%.1fMB", progress/(1024*1024), total/(1024*1024)) + ")");	
	}
	
	/**
	 * Stop thread
	 * Ends animation progress and show total progress
	 */
	public void stopRun(){
		stop = true;
	}
	
	/**
	 * Interrupt thread 
	 * Ends animation progress wihtout output
	 */
	public void interruptRun(){
		interrupt = true;
	}
	
	@Override
	public void run() {
		synchronized (System.out) {			
			// Update progress		
			while(!stop && !interrupt){			
				// Show animation
				showProgress(progress, total, false);
				try{
					Thread.sleep(200);
				} catch (Exception e) {}
			}
			
			// If process finished but not interrupted
			if (!interrupt && progress==total){
				showProgress(total, total, true);
				System.out.print("\n");
			}
		}
	}	
}
