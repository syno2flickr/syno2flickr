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
import org.jickr.Permission;
import org.jickr.Photo;
import org.jickr.PhotoUpload;
import org.jickr.User;

/**
 * Main class 
 * @author jbrek
 *
 */
public class Syno2Flickr {

	/**
	 * Main method
	 * @param args
	 */
	public static void main(String[] args) {
		
		// Initialization
		String syncFolder=null; // folder to upload
		org.jickr.Permission perm = Permission.WRITE; // access level we want
		org.jickr.User user = null; // User Flickr authenticated 
		
		// Get property values
		try {
			
			// Test first arg (must be the path of the properties file)
			if (args != null && args.length>0) 
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
			user = Auth.getDefaultAuthUser(); // Check to see if we've already authenticated
		} catch (FlickrException e1) { user = null; }
		
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
				BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
				in.readLine();
				
				// Alright, now that we're cleared with Flickr, let's try to authenticate
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
					"Error while Flickr authentication\n"
							+ e1.getMessage());
			System.exit(0);
		} catch (IOException e1) {
			Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).severe(
					"Error while Flickr authentication\n"
							+ e1.getMessage());
			System.exit(0);
		}

		// Set authentication context for user (credentials)
		Auth.resetAuthContext();
		Auth.setAuthContext(user);
		
		// Check what to upload 
		File folder = new File(syncFolder);
		File[] listOfFiles = folder.listFiles(new FileFilter() {
			
			@Override
			public boolean accept(File pathname) {
				return !pathname.isDirectory();
			}
		});
		
		// Synchronize (upload photos/video)
		for(File f : listOfFiles){
			// Generate metadata for the upload
			PhotoUpload uploader = new PhotoUpload.Builder(f).hidden(true)
															 .build();
			
			// Upload the content
			try {
				Photo.uploadNewPhoto(uploader);
				
			} catch (FlickrException e) {
				Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).severe(
						"An error occured while uploading file "+f.getPath()+"\n"
								+ e.getMessage());
				System.out.println("An error occured while uploading file "+f.getPath());
			}
		}
		
		// Summary and notify 
	}
}
