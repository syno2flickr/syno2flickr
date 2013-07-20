README FILE


SYNO2FLICKR
	Version: 	0.1.2
	Release date: 	20/07/2012
	Web site:	https://github.com/syno2flickr/
	Author:		Jachen Brechbuehl (dev@jachen.ch) 


DESCRIPTION

	Java command line tool called "Syno2Flickr" allowing to upload 
	your photos and videos to your flickr account. The process can
	be run in interactive or background mode.


LICENCE

	Syno2flickr is free software: you can redistribute it and/or modify
    	it under the terms of the GNU General Public License as published by
    	the Free Software Foundation, either version 3 of the License, or
    	(at your option) any later version.

    	This program is distributed in the hope that it will be useful,
    	but WITHOUT ANY WARRANTY; without even the implied warranty of
   	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    	GNU General Public License for more details.

    	You should have received a copy of the GNU General Public License
    	along with this program.  If not, see <http://www.gnu.org/licenses/>.1


CHANGELOG
	0.1.2
		- Fix HTTP error 411

FEATURES

	- Upload photo/video to a flickr account
	- Set default privacy
	- Set default album
	- Show account info (usage & limitations)
	- Show a progress bar



INSTALLATION

	1. You must have a Java Virtual Machine installed (1.5 and above).

	2. Download the lastest version

	3. In the zip, 5 files:			
	     - syno2flickr.jar 	 	       -> the tool bin
	     - syno2flickr_windows.properties  -> configuration sample file for Windows
	     - syno2flickr_unix.properties     -> configuration sample file for Unix
	     - readme.txt		       -> README file
	     - Licence-gpl3.0.txt	       -> GPL 3.0 Licence

	4. You have to get an application key and a secret key. 
	   (http://www.flickr.com/services/apps/create/apply/)

	5. Edit the syno2flickr.properties config file and add 
	   the application key (apiKey param) and the secret key 
	   (sharedSecret param).
	
	6. Set the folder to sync with Flickr (syncFolder param)

	7. Set the folder to archive photos/videos successfully uploaded 
	   (archiveFolder param).
	
	8. Eventually change the another params if needed


RUN syno2Flickr
	
	Note: the first run must be in interactive mode because you need to 
	      register this app with your flickr account.

	Run in interactive mode:
		#> java -jar syno2flickr.jar syno2flickr.properties

	Run in background mode for unix (need nohup installed):
		#> nohup java -jar syno2flickr.jar syno2flickr.properties >syno2flickr.log 2>&1 &


COPYRIGHT
	Copyright 2011-2012 Jachen Brechbuehl (dev@jachen.ch)

