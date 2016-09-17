package com.zxcl.war_revision_maven_plugin;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 
 * @author Chang Wen
 */
public class IOUtil {

	public static String readText(File file, String charset)
	        throws IOException {
		FileInputStream fin = new FileInputStream(file);
		try {
			byte[] buf = new byte[4096];
			int c = -1;
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			while ((c = fin.read(buf)) != -1) {
				bout.write(buf, 0, c);
			}
			return bout.toString(charset);
		} finally {
			close(fin);
		}

	}

	public static void writeText(File destFile, String text, String charset)
	        throws IOException {
		FileOutputStream fout = new FileOutputStream(destFile);
		try {
			fout.write(text.getBytes(charset));
		} finally {
			close(fout);
		}
	}
	
	public static void copyDirectoryStructure(File srcDirectory, File destDirectory) throws IOException{
		if(!srcDirectory.exists()) {
			return;
		}
		if(!srcDirectory.isDirectory()){
			throw new IOException("src directory " + srcDirectory + " is not a directory."); 
		}
		if(destDirectory.exists() && !destDirectory.isDirectory()) {
			throw new IOException("dest directory " + destDirectory + " is not a directory."); 
		}
		if(!destDirectory.exists()) {
			if(!destDirectory.mkdirs()) {
				throw new IOException("can not create directory : " + destDirectory);
			}
		}
		File[] files = srcDirectory.listFiles();
		if(files == null)
			return;
		for(int i = 0; i < files.length; i++){
			File file = files[i];
			File dest = new File(destDirectory, file.getName());
			if(file.isDirectory()) {
				if(!dest.exists()) {
					if(!dest.mkdirs()){
						throw new IOException("can not create directory : " + dest);
					}
				}
				copyDirectoryStructure(file, dest);
			} else {
				
				//check source != dest
				if(dest.getCanonicalFile().equals(file.getCanonicalFile())){
					continue;
				}
				if(dest.exists() && !dest.canExecute()){
					throw new IOException("can not open file " + dest + " for writing.");
				}
				
				InputStream in = null;
				OutputStream out = null;
				try{
					in = new FileInputStream(file);
					out = new FileOutputStream(dest);
					copy(in,out);
				} finally {
					close(in);
					close(out);
				}
			}
		}
	}
	
	public static void copy(InputStream in , OutputStream out) throws IOException{
		byte[] buf = new byte[1024];
		int c = -1;
		while( (c = in.read(buf)) != -1) {
			out.write(buf,0,c);
		}
		out.flush();
	}

	public static void deleteDirectory(File directory)
	        throws IOException {
		if (!directory.exists()) {
			return;
		}
		
		if(!directory.isDirectory()){
			return;
		}
		
		final File[] files = directory.listFiles();

        if ( files == null ) {
            return;
        }
        
        for ( int i = 0; i < files.length; i++ ) {
            File file = files[i];
            if(file.isDirectory()) {
            	deleteDirectory(file);
            } else {
            	/*
                 * NOTE: Always try to delete the file even if it appears to be non-existent. This will ensure that a
                 * symlink whose target does not exist is deleted, too.
                 */
                boolean filePresent = file.getCanonicalFile().exists();
                boolean deleted = file.delete();
                
                if(!deleted) {
                	
                	//NOTE: On windows, sometimes we need to tell the OS this file is no longer in use.
                	//we try to make a GC to achieve that.
                	System.gc();
                	file = file.getCanonicalFile();
                	try {
						Thread.sleep(10);
						deleted = file.delete();
					} catch (InterruptedException e) {
						deleted = file.delete();
					}
                }
                //if the symbol link or the file exists, but failed to delete 
                if ( !deleted && filePresent )
                {
                    final String message = "File " + file + " unable to be deleted.";
                    throw new IOException( message );
                }
            }
        }

		if (!directory.delete()) {
			final String message = "Directory " + directory
			        + " unable to be deleted.";
			throw new IOException(message);
		}
	}
	
	public static void close(Closeable c) {
		try {
			c.close();
		} catch (IOException e) {
		}
	}
}
