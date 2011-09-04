package ch.jachen.dev.flickr;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Stack;
import java.util.logging.Logger;

import org.jickr.Auth;
import org.jickr.Flickr;
import org.jickr.FlickrException;
import org.jickr.MimeType;
import org.jickr.Permission;
import org.jickr.Photo;
import org.jickr.PhotoSet;
import org.jickr.PhotoUpload;
import org.jickr.Privacy;
import org.jickr.RequestEvent;
import org.jickr.RequestListener;
import org.jickr.User;
import org.jickr.UserLimitations;

/**
 * Class to animate progress uploading in Console output
 * @author jbrek
 *
 */
class ProgressUpload extends Thread{

	private final double progress;
	private final double total;
	private boolean stop=false;
	private static long calls = 0;
								
	public ProgressUpload(double progress, double total) {
		this.progress = progress;
		this.total = total;
	}
	
	private final static int width = 50; // progress bar width in chars
	private final static char[] animationChars = new char[] { '-', '\\', '|', '/' };
	
	/**
	 * Show progress of upload in Console
	 * @param progress progress in bytes
	 * @param total	total in bytes
	 * @param finished is the progress finished
	 */
	public static void showProgress(double progress, double total, boolean finished){
		
		double progressPercentage = progress / total;
		
		synchronized (System.out) {
			System.out.print("\rProcessing: |");
			int i = 0;
			int max = 0;
			for (; i <= (max = (int) (progressPercentage * width)); i++) {
				System.out.print(((i < max || finished) ? "="
						: animationChars[(int)(calls++ % 4)]));
			}
			for (; i <= width; i++) {
				System.out.print(" ");
			}
			System.out.print("| "
					+ String.format("%3d", (finished ? 100 : progressPercentage==1?99:(int) (progressPercentage * 100)))
					+ "% (" + String.format("%.1fMB/%.1fMB", progress/(1024*1024), total/(1024*1024)) + ")");
		}	
	}
	
	public void stopRun(){
		stop = true;
	}
	
	
	@Override
	public void run() {
										
		// Update progress									
		while(!stop){
			
			showProgress(progress, total, false);
			
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) { }
		}
		
		if (progress==total)
			showProgress(total, total, true);
	}
	
}

/**
 * Main class
 * 
 * @author jbrek
 * 
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
	 * Authentication to flickr
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
						.println("Please enter the following URL into a browser, "
								+ "follow the instructions, and then come back");
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
		System.out.println("**********************************************************************");
		System.out.println("* Syno2Flickr v0.1 09/2011                                           *");
		System.out.println("*                                                                    *");
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
		String defaultSetId = null; // id of default set for new photos
		Privacy defaultPrivacy = null; // default privacy
		org.jickr.Permission perm = Permission.WRITE; // access level we want
		org.jickr.User user = null; // User Flickr authenticated

		// Welcome
		printWelcome();
		
		// Get property values
		try {

			// Test first arg (must be the path of the properties file)
			if (args != null && args.length > 0)
				if (new File(args[0]).exists())
					Syno2FlickrProperties.setPropertyFile(args[0]);

			// Set key/secret
			Flickr.setApiKey(Syno2FlickrProperties.getApiKey());
			Flickr.setSharedSecret(Syno2FlickrProperties.getSharedSecret());

			// Get folder to sync
			syncFolder = Syno2FlickrProperties.getFolderToSync();
			
			// Archive folder
			archiveFolderString = Syno2FlickrProperties.getArchiveFolder();
			
			// Default set id
			defaultSetId = Syno2FlickrProperties.getDefaultSetId();
			
			// Default privacy
			String privacyString = Syno2FlickrProperties.getDefaultPrivacy();
			defaultPrivacy = Privacy.valueOf(Integer.parseInt(privacyString));

		} catch (Syno2FlickrException e) {

			// Error message
			System.out.println(e.getMessage());

			System.exit(0);
		} catch (NumberFormatException e){
			// Error while getting the default privacy
			System.out.println("Error while getting default privacy, value must be between 0 and 4. Default privacy will be set to private");
			defaultPrivacy = Privacy.PRIVATE;
		}

		// Auth
		user = authenticationToFlickr(user, perm);
		
		// Check default set
		PhotoSet defaultSet=null;
		try {
			defaultSet = PhotoSet.findByID(defaultSetId);
		} catch (FlickrException e3) {
			System.out.println("Error while getting informations of the default set (id: "+
									defaultSetId+").\n"+e3.getMessage());
		}
		
		// Show params
		System.out.println("\n\nParameters:");
		System.out.println("\tSync folder: "+syncFolder);
		System.out.println("\tArchive folder: "+archiveFolderString);
		if(defaultSet!=null)
			System.out.println("\tDefault Set: "+defaultSet.getTitle());
		System.out.println("\tDefault privacy: "+defaultPrivacy);
		
		// Get user upload limitations
		UserLimitations userLimits = new UserLimitations();
		
		// Show limit / usage
		try {
			System.out.println("\n\n"+userLimits.showUsageAndLimitations());
		} catch (FlickrException e2) {
			System.out.println("Error: impossible to show usage and limitations for user "+user.getUserName()+"\n"+e2.getMessage());
		}		
		
		// Get list of all files to upload
		File folder = new File(syncFolder);
		File[] listOfFiles = folder.listFiles(new FileFilter() {

			@Override
			public boolean accept(File pathname) {
				
				try {
					return !pathname.isDirectory() && !pathname.isHidden();
				} catch (Exception e) {
					return false;
				}
			}
		});
		
		// Show nb files founds
		System.out.println("\nStart uploading: "+listOfFiles.length+ " files found to upload");

		// Synchronize (upload photos/video)
		long denomMega=1024L*1024L;
		File archiveFolder = new File(archiveFolderString);
		int noFile = 0;
		try {
			
			// Get bandwith remaining
			long bandwidthRemaingBytes = userLimits.getBandwidthRemainingBytes();
			
			for (File f : listOfFiles) {
				
				// init
				//ProgressUpload.calls = 0;
				noFile++;
				
				// Info
				System.out.println("\nSending "+f.getName()+ " ("+noFile+"/"+listOfFiles.length+")");
				
				// Check limitations/file type
				String mime=null;
				try {
					mime = MimeType.getMimeType("file://"+f.getPath());
				} catch (IOException e1) {}
				
				if (mime!=null && mime.contains("image")) {
					if (f.length() > userLimits.getFilesizeMaxBytes()){
						System.out.println("Error: image " + f.getName()+ " exceeds the maximum accepted size (max. "+
												userLimits.getFilesizeMaxBytes()/denomMega+"MB). Skipped.");
						continue;
					}
					if(!userLimits.isBandwidthUnlimited() && (bandwidthRemaingBytes-f.length()) < 0){
						System.out.println("Error: user " + user.getUserName() + 
										   " has reached his monthly bandwidth limit ("+
										   userLimits.getBandwidthMaxBytes()/denomMega+
										   "MB). Upload cancelled.");
						break;
					}
						
				} else {
					if (f.length() > userLimits.getVideosizeMaxBytes()){
						System.out.println("Error: " + f.getName()+ 
										   " exceeds the maximum accepted size (max. "+
										   userLimits.getVideosizeMaxBytes()/denomMega+"MB). Skipped.");
						continue;
					}
				}
				
				// Generate metadata for the upload
				PhotoUpload uploader = new PhotoUpload.Builder(f)
															.familyFlag(defaultPrivacy.equals(Privacy.FAMILY) || defaultPrivacy.equals(Privacy.FRIENDSANDFAMILY))
															.friendFlag(defaultPrivacy.equals(Privacy.FRIENDS) || defaultPrivacy.equals(Privacy.FRIENDSANDFAMILY))
															.publicFlag(defaultPrivacy.equals(Privacy.PUBLIC))
															.build();
	
				try {
					
					// Upload the content
					String id;
					id = Photo.uploadNewPhoto(uploader, new RequestListener() {
						
						
						@Override
						public void progressRequest(RequestEvent event) {
							
							stopAllThreads();
							ProgressUpload r = new ProgressUpload(event.getProgress(), event.getTotalProgress());
							progressList.push(r);
							r.start();
							
						}
					}); 
					// Stop all threads
					stopAllThreads();
	
					
					// Put in default set
					if (defaultSet!=null){
						try {
							defaultSet.add(id);
						} catch(FlickrException e){
							System.out.println("Error while adding photo (id: "+id+") to set (id: "+defaultSet.getID()+").\n"+e.getMessage());
						}
					}
					
					// Move uploaded file to archive
					if (archiveFolder.exists()){
						f.renameTo(new File( archiveFolder.getPath()+File.separator+f.getName()));
					}
					
					if (!userLimits.isBandwidthUnlimited()) bandwidthRemaingBytes-=f.length();
	
				} catch (FlickrException e) {
					
					if (e.getCode()==6) break;
					
					System.out.println("An error occured while uploading file "
							+ f.getPath()+"\n"+e.getMessage());
				}
			}
		} catch (FlickrException e){
			System.out.println("A grave error occurs:\n"+e.getMessage());
		}

		// Summary and notify

		System.out.println("\n\nSend completed.\nBye.");
	}

}
