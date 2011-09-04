package ch.jachen.dev.flickr;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Logger;

import org.jickr.Auth;
import org.jickr.Flickr;
import org.jickr.FlickrException;
import org.jickr.MimeType;
import org.jickr.Permission;
import org.jickr.Photo;
import org.jickr.PhotoUpload;
import org.jickr.RequestEvent;
import org.jickr.RequestListener;
import org.jickr.UserLimitations;

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
	static long calls = 0;
	static void updateProgress(double progressPercentage) {
		final int width = 50; // progress bar width in chars
		char[] animationChars = new char[] { '-', '\\', '|', '/' };

		System.out.print("\rProcessing: |");
		int i = 0;
		int max = 0;
		for (; i <= (max = (int) (progressPercentage * width)); i++) {
			System.out.print(((i < max || progressPercentage == 1.0) ? "="
					: animationChars[(int)(calls++ % 4)]));
		}
		for (; i <= width; i++) {
			System.out.print(" ");
		}
		System.out.print("| "
				+ String.format("%3d", ((int) (progressPercentage * 100)))
				+ "%" + (progressPercentage==1.0?"\n":""));
	}

	/**
	 * Main method
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		// Initialization
		String syncFolder = null; // folder to upload
		org.jickr.Permission perm = Permission.WRITE; // access level we want
		org.jickr.User user = null; // User Flickr authenticated

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

		} catch (Syno2FlickrException e) {

			// Message d'erreur
			System.out.println(e.getMessage());

			System.exit(0);
		}

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
					Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).severe(
							"Failure to authenticate.");
					System.exit(1);
				}
				if (Auth.isAuthenticated(user, perm)) {
					Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info(
							"We're authenticated with "
									+ Auth.getPermLevel(user) + " access.");
					Auth.setDefaultAuthUser(user);
				} else {
					// Shouldn't ever get here - we throw an exception above if
					// we
					// can't authenticate.
					Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).severe(
							"Oddly unauthenticated");
					System.exit(2);
				}

			} // if (user == null || !Auth.isAuthenticated(user, perm)) {
			else {

				// If already authenticated
				Logger.getLogger(Logger.GLOBAL_LOGGER_NAME)
						.info(
								"Already authenticated to at least " + perm
										+ " level.");
				Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info(
						"We're actually at the " + Auth.getPermLevel(user)
								+ " level.");
			}
		} catch (FlickrException e1) {
			Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).severe(
					"Error while Flickr authentication\n" + e1.getMessage());
			System.exit(0);
		} catch (IOException e1) {
			Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).severe(
					"Error while Flickr authentication\n" + e1.getMessage());
			System.exit(0);
		}

		// Set authentication context for user (credentials)
		Auth.resetAuthContext();
		Auth.setAuthContext(user);

		// Get user upload limitations
		UserLimitations userLimits = new UserLimitations();
		long bandwithMaxBytes = 0, bandwidthRemainingBytes = 0, bandwidthUsedBytes = 0 ;
		long filesizeMaxBytes = 0 ;
		long videosizeMaxBytes=0, videosUploaded=0;
		String videosRemaining = null;
		boolean pro = false, bandwidthIlimited=false;
		try {
			bandwithMaxBytes = userLimits.getBandwidthMaxBytes();
			bandwidthRemainingBytes = userLimits.getBandwidthRemainingBytes();
			bandwidthUsedBytes = userLimits.getBandwidthUsedBytes();
			filesizeMaxBytes = userLimits.getFilesizeMaxBytes();
			videosizeMaxBytes = userLimits.getVideosizeMaxBytes();
			videosRemaining = userLimits.getVideosRemaining();
			videosUploaded = userLimits.getVideosUploaded();
			bandwidthIlimited = userLimits.isBandwidthUnlimited();
			pro = userLimits.isPro();
		} catch (FlickrException e1) {
			System.out.println(e1.getMessage());
			System.exit(0);
		}
		
		// Show limit / usage
		int denomMega = 1024*1024;
		System.out.println("Usage and limitations status");
		System.out.println("  Pro account: "+ (pro?"yes":"no"));
		System.out.println("  Bandwith:");
		System.out.println("\tUnlimited: "+(bandwidthIlimited?"yes":"no"+
						   "\tMax: "+bandwithMaxBytes/denomMega+"MB" +
						   "\tRemaining: "+bandwidthRemainingBytes/denomMega+"MB"+
						   "\tUsed: "+bandwidthUsedBytes/denomMega+"MB"));
		System.out.println("  Image:");
		System.out.println("\tMax size: "+ filesizeMaxBytes/denomMega+"MB");
		System.out.println("  Video:");
		System.out.println("\tMax size: "+videosizeMaxBytes/denomMega+"MB"+
						   "\tRemaing: "+videosRemaining+
						   "\tUploaded: "+videosUploaded+"\n");
		
		// Check what to upload
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

		// Synchronize (upload photos/video)
		File archiveFolder = new File(Syno2FlickrProperties.getArchiveFolder());
		int noFile = 0;
		for (File f : listOfFiles) {
			
			// init
			calls = 0;
			noFile++;
			
			// Info
			System.out.println("\nUpload of "+f.getName()+ " ("+noFile+"/"+listOfFiles.length+")");
			
			// Check limitations/file type
			String mime=null;
			try {
				mime = MimeType.getMimeType("file://"+f.getPath());
			} catch (IOException e1) {}
			
			if (mime!=null && mime.contains("image")) {
				if (f.length() > filesizeMaxBytes){
					System.out.println("Error: image " + f.getName()+ " exceeds the maximum size accepted (max. "+filesizeMaxBytes/denomMega+"MB). Skipped.");
					continue;
				}
				if(!bandwidthIlimited && (bandwidthRemainingBytes-f.length()) < 0){
					System.out.println("Error: user " + user.getUserName() + " has reached his monthly bandwidth limit. Upload cancelled.");
					break;
				}
					
			} else {
				if (f.length() > videosizeMaxBytes){
					System.out.println("Error: " + f.getName()+ " exceeds the maximum size accepted (max. "+videosizeMaxBytes/denomMega+"MB). Skipped.");
					continue;
				}
			}
			
			// Generate metadata for the upload
			PhotoUpload uploader = new PhotoUpload.Builder(f).hidden(true)
					.build();

			try {
				
				// Upload the content
				Photo.uploadNewPhoto(uploader, new RequestListener() {

					@Override
					public void progressRequest(RequestEvent event) {
						double progress = (new Double(event.getProgress()) / event.getTotalProgress());
						updateProgress(progress);
					}
				}); System.out.println("");
				
				// Move uploaded file to archive
				if (archiveFolder.exists()){
					f.renameTo(new File( archiveFolder.getPath()+File.separator+f.getName()));
				}
				
				if (!bandwidthIlimited) bandwidthRemainingBytes-=f.length();

			} catch (FlickrException e) {
				
				if (e.getCode()==6) break;
				
				Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).severe(
						"An error occured while uploading file " + f.getPath()
								+ "\n" + e.getMessage());
				System.out.println("An error occured while uploading file "
						+ f.getPath());
			}
		}

		// Summary and notify

		System.out.println("Terminate.");
	}
}
