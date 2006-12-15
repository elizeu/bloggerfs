package ca.ubc.ece.bloggerfs;

import com.google.gdata.data.Entry;
import com.google.gdata.data.PlainTextConstruct;

import fuse.FuseFtype;

/**
 * 
 * This class represents an entry in the Blogger Filesystem.
 * 
 * @author elizeu
 *
 */
public class BloggerEntry {
	
	/** An entry in the Blogger */
	Entry entry;
	
	String abs_path_name;
	String name; 
	
	/** Determines the entry type. Currently, 
	 *  only FILE and DIRECTORY (which is the root entry) 
	 *  make sense 
	 **/
	private int entryType;
	
	/**
	 * Default constructor. The entry type is inferred by the name. 
	 * 
	 * @param _name the name that will represent the file in the BloggerFS.  
	 * 
	 */
	public BloggerEntry(String _name){
		
		this.name = _name;

		int index = this.name.indexOf('\n');
		
		if ( index != -1 ) {
			this.name = this.name.substring(0,index);
		}		
		
		if ( this.name.equals("/") ) {
			this.abs_path_name = this.name;
			this.entryType = FuseFtype.TYPE_DIR;
		} else {
			this.abs_path_name = "/"+this.name;
			this.entryType = FuseFtype.TYPE_FILE;
		}
		
		this.entry = new Entry();		 
		this.entry.setTitle( new PlainTextConstruct(""));
	}
	
	/**
	 * 
	 * @param _name
	 * @param type
	 */
	public BloggerEntry(String _name, int type){
		
		this(_name);
		
		this.entryType = type;		
	}	
	
	public BloggerEntry(Entry e){
		
		this(e.getTitle().getPlainText());
		
		this.entry = e;
	}

	public String getName() {
		return this.name;
	}
	
	/**
	 * Get the absolute file entry name 
	 * 
	 * @return entry absolute name in the form "/filename"
	 */
	public String getAbsoluteName(){
		return this.abs_path_name;
	}

	/**
	 * Get the size of this entry 
	 * 
	 * @return the size in bytes 
	 */
	public long getSize() {

		if ( isRoot() ) {
			return 1;
		} else {
			return this.getName().length();
		}
	}

	/**
	 * The creation time of the entry. That is the publication time of the Blog entry.
	 * 
	 * @return a long representation of the publication time
	 */
	public long getTime() {
		
		if ( isRoot() ) {
			return 1L;
		} else {
			return this.entry.getPublished().getValue();
		}
	}

	public String getId() {
		
		return entry.getId();
	}

	public Entry getEntry() {
		return entry;
	}

	public boolean isDirectory() {		
		return (entryType == FuseFtype.TYPE_DIR);
	}
	
	public boolean isRoot() {
		return this.abs_path_name.equals("/");
	}
}
