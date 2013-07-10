/**
 * 
 */
package fileutils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author betsy
 *
 */
public class FileCopier {

	/**
	 * 
	 */
	public FileCopier() {
		
	}
		public void copyFile(String origFile, String newFile){
		InputStream inStream = null;
		OutputStream outStream = null;
	 
	    	try{
	 
	    	    File afile =new File(origFile);
	    	    File bfile =new File(newFile);
	 
	    	    inStream = new FileInputStream(afile);
	    	    outStream = new FileOutputStream(bfile);
	 
	    	    byte[] buffer = new byte[1024];
	 
	    	    int length;
	    	    //copy the file content in bytes 
	    	    while ((length = inStream.read(buffer)) > 0){
	 
	    	    	outStream.write(buffer, 0, length);
	 
	    	    }
	 
	    	    inStream.close();
	    	    outStream.close();
	 
	    	    //System.out.println("File is copied successful!");
	 
	    	}catch(IOException e){
	    		e.printStackTrace();
	    	}
		}
	

}
