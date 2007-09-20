package ca.ubc.ece.netsys.bloggerfs;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.gdata.client.GoogleService;
import com.google.gdata.client.Query;
import com.google.gdata.data.Entry;
import com.google.gdata.data.Feed;
import com.google.gdata.util.AuthenticationException;
import com.google.gdata.util.ServiceException;

import fuse.FuseException;
import fuse.FuseFtype;
import fuse.FuseMount;
import fuse.FuseStatfs;
import fuse.compat.Filesystem1;
import fuse.compat.FuseDirEnt;
import fuse.compat.FuseStat;

/**
 * This class implements a user-level Filesystem based on FUSE and FUSE-J. The idea is to encapsulate invocations 
 * to GData API to interact to Blogger.com via a file interface. 
 * 
 * @author elizeu
 *
 */
public class BloggerFileSystem implements Filesystem1 {

	/* Attributes */
	private FuseStatfs statfs;

	/** A blogger url */
	private URL feedUrl;

	/** An object which represents a query to get all posts in your Blog */
	private Query queryPosts;

	/** An abstraction to a Google Service - in our case Blogger */
	private GoogleService myService;

	/** */
	private Feed myFeed;

	/** The representation of all files in BloggerFS */
	private Map<String,BloggerEntry> fileMap;

	/** The root of BloggerFS */
	private BloggerEntry rootEntry;
	
	//TODO: define it better
	/** The block size in BloggerFS */
	private static final int blockSize = 1;
		
	
	/**
	 * The default constructor 
	 * 
	 * @param username a username to access the blog
	 * @param password a password used to authenticate the user at the blog
	 */
	public BloggerFileSystem(String username, String password, String url){
		
		/* Create a tree structure to represent the file system */
		rootEntry = new BloggerEntry("/");
						
		fileMap = new HashMap<String,BloggerEntry>();
	    fileMap.put(rootEntry.getAbsoluteName(), rootEntry);

	    /* just the url of a blog */
		feedUrl = null;
		try {
			//feedUrl = new URL("http://mundau.blogspot.com/feeds/posts/default");
			feedUrl = new URL(url+"/feeds/posts/default");
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		/* Query for a list of posts */
		queryPosts = new Query(feedUrl);
		
		/* service representation */
		myService = new GoogleService("blogger", "exampleCo-exampleApp-1");

		//Set up authentication (optional for beta Blogger, required for current Blogger):
		try { 
			myService.setUserCredentials(username, password);
		} catch (AuthenticationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		}

		//Send the request and receive the response:
		myFeed = null;

		try {
			myFeed = myService.query(queryPosts, Feed.class);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		int files = myFeed.getTotalResults();
		int dirs = 1;
		int blocks = 0;		
		
		for ( Entry e : myFeed.getEntries()) {
			
			BloggerEntry b = new BloggerEntry(e);
					
			//System.out.println("|-" + b.getName());
			
			fileMap.put(b.getAbsoluteName(), b);
						
			blocks = blocks + ( 1 / blockSize );
		}
				
		statfs = new FuseStatfs();
		statfs.blocks = blocks;
		statfs.blockSize = blockSize;
		statfs.blocksFree = 0;
		statfs.files = files + dirs;
		statfs.filesFree = 0;
		statfs.namelen = 2048;
				
		//Print the title of the returned feed:
		System.out.println("BloggerFS Initialized (files = "+ files + ")");
		
	}
	
	public FuseStat getattr(String arg0) throws FuseException {

		BloggerEntry b = fileMap.get(arg0);		
				
		FuseStat stat = new FuseStat();
		
		stat.mode = b.isDirectory() ? FuseFtype.TYPE_DIR | 0755 : FuseFtype.TYPE_FILE | 0644;
		stat.nlink = 1;
		stat.uid = 1000;
		stat.gid = 1000;
		stat.size = b.getSize();
		stat.inode = 0;
		stat.atime = stat.mtime = stat.ctime = (int) (b.getTime() / 1000L);
		stat.blocks = (int) ((stat.size));// + 511L) / 512L);
		
		return stat;		
	}

	/**
	 * Return an array with entries to the content the directory passed as a parameter
	 */
	public FuseDirEnt[] getdir(String arg0) throws FuseException {

		FuseDirEnt[] dirEntries;
		
		BloggerEntry b = fileMap.get(arg0);
	      
	    //System.out.println("Number of entries:\t" + fileMap.size() );
	      
	    if ( b.getAbsoluteName().equals("/") ) {
	    	  
	    	dirEntries= new FuseDirEnt[fileMap.size()-1];
	      
	      	int i = 0;
	      
	      	Set<String> keys = fileMap.keySet();
	      
	      	for (String key : keys)  {
	      		
	      		BloggerEntry entry = fileMap.get(key);
	      		
	      		if ( ! entry.equals(b) ) {
	      		
	      			FuseDirEnt dirEntry = new FuseDirEnt();

	      			dirEntries[i] = dirEntry;
	      			dirEntry.name = entry.getName(); 	        	 	        	 
	      			dirEntry.mode = FuseFtype.TYPE_FILE;
	        	 
	      			i++;
	      		}
	      	}
	      	
		} else {
			dirEntries = new FuseDirEnt[1];
			dirEntries[0] = new FuseDirEnt();
			dirEntries[0].name = b.getAbsoluteName();
			dirEntries[0].mode = FuseFtype.TYPE_FILE;
		}
	
	    return dirEntries;
	}

	public void open(String arg0, int arg1) throws FuseException {
		// TODO Auto-generated method stub
		
	}

	public void read(String arg0, ByteBuffer arg1, long arg2) throws FuseException {
		// TODO Auto-generated method stub
		
	}

	public String readlink(String arg0) throws FuseException {
		// TODO Auto-generated method stub
		return null;
	}

	public void release(String arg0, int arg1) throws FuseException {
		// TODO Auto-generated method stub
		
	}

	public FuseStatfs statfs() throws FuseException {
		return this.statfs;
	}

	public void write(String arg0, ByteBuffer arg1, long arg2) throws FuseException {
		// TODO Auto-generated method stub
		
	}

	public void chmod(String arg0, int arg1) throws FuseException {
	      throw new FuseException("Read Only").initErrno(FuseException.EACCES);		
	}

	public void chown(String arg0, int arg1, int arg2) throws FuseException {
	      throw new FuseException("Read Only").initErrno(FuseException.EACCES);		
	}

	public void link(String arg0, String arg1) throws FuseException {
	      throw new FuseException("Read Only").initErrno(FuseException.EACCES);		
	}

	public void mkdir(String arg0, int arg1) throws FuseException {
	      throw new FuseException("Read Only").initErrno(FuseException.EACCES);		
	}

	public void mknod(String arg0, int arg1, int arg2) throws FuseException {
	      throw new FuseException("Read Only").initErrno(FuseException.EACCES);
	}

	public void rename(String arg0, String arg1) throws FuseException {
	      throw new FuseException("Read Only").initErrno(FuseException.EACCES);		
	}

	public void rmdir(String arg0) throws FuseException {
	      throw new FuseException("Read Only").initErrno(FuseException.EACCES);
	}

	public void symlink(String arg0, String arg1) throws FuseException {
	      throw new FuseException("Read Only").initErrno(FuseException.EACCES);
	}

	public void truncate(String arg0, long arg1) throws FuseException {
	      throw new FuseException("Read Only").initErrno(FuseException.EACCES);		
	}

	public void unlink(String arg0) throws FuseException {
	      throw new FuseException("Read Only").initErrno(FuseException.EACCES);		
	}

	public void utime(String arg0, int arg1, int arg2) throws FuseException {
		//Not implemented yet!
	}
	

	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args){
		
		if (args.length < 3) {
			System.out.println("[Error]: Must specify a mounting point, a Blog URL, username & password.");
			System.out.println();			
			System.out.println("[Usage]: bloggermnt <username> <url> <mounting point>");
		    System.exit(-1);
		}

		BufferedReader in = new BufferedReader( new InputStreamReader(System.in));
				
		String password = "";
		
		try {
			System.out.print("Password: ");
			password = in.readLine();
		} catch (IOException e1) {
			e1.printStackTrace();
			System.exit(-1);
		}
		
		String[] fs_args = new String[3];
		fs_args[0] = "-f";
		fs_args[1] = "-s";
		fs_args[2] = args[2];

		/** A BloggerFS instance */
		Filesystem1 bloggerfs = new BloggerFileSystem(args[0],password,args[1]);

		try {
			FuseMount.mount(fs_args, bloggerfs);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

}
