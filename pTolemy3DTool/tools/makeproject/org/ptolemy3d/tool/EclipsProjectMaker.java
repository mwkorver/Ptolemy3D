/* $LICENSE$ */
package org.ptolemy3d.tool;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class EclipsProjectMaker
{
	public static void main(String[] args)
	{
		//Command lineanalyse
		boolean restore = false;
		if(args.length > 0) {
			String arg1 = args[0];
			if(arg1.equals("-restore")) {
				restore = true;
			}
		}
		
		EclipsProjectMaker p = new EclipsProjectMaker("../../");
		if(restore) {
			p.restoreProject();
		}
		else {
			p.makeProject();
		}
	}
	
	private final static String CONFIG_FILE = "/.classpath";
	private final static String TMP_EXT = ".tmp";
	private final static String BACKUP_EXT = ".save";
	
	private final static String[] PROJECTS = {"src/Ptolemy3DCore", "src/Ptolemy3DExample", "src/Ptolemy3DPlugins", "src/Ptolemy3DViewer"};
	
	private final static String OLD_PATH = "D:/Mes documents/Mes Programmes/Ptolemy3D/Ptolemy3D";
	
	private final String root;
	private EclipsProjectMaker(String basePath)
	{
		this.root = basePath;
	}
	
	/**
	 * Fix and backup original configuration
	 */
	public void makeProject()
	{
		for(String s : PROJECTS) {
			try {
				makeConfig(root + s + CONFIG_FILE);
			}
			catch(IOException e) {
				e.printStackTrace();
			}
		}
	}
	private void makeConfig(String path) throws IOException
	{
		BufferedReader br = new BufferedReader(new FileReader(new File(path)));
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(path+TMP_EXT)));
		
		String line;
		while((line = br.readLine()) != null) {
			line = makeConfigLine(line);
			bw.write(line);
			bw.newLine();
		}
		
		br.close();
		bw.close();
		
		if(!new File(path+BACKUP_EXT).exists()) {
			new File(path).renameTo(new File(path+BACKUP_EXT));
		}
		new File(path).delete();
		new File(path+TMP_EXT).renameTo(new File(path));
	}
	private String makeConfigLine(String line)
	{
		line = line.replace(OLD_PATH, new File(root).getAbsolutePath().replace("\\", "/"));
		return line;
	}
	
	/**
	 * Restore original configuration
	 */
	public void restoreProject()
	{
		for(String s : PROJECTS) {
			try {
				restoreConfig(root + s + CONFIG_FILE);
			}
			catch(IOException e) {
				e.printStackTrace();
			}
		}
	}
	private void restoreConfig(String path) throws IOException
	{
		BufferedReader br = new BufferedReader(new FileReader(new File(path+BACKUP_EXT)));
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(path)));
		
		String line;
		while((line = br.readLine()) != null) {
			bw.write(line);
			bw.newLine();
		}
		
		br.close();
		bw.close();
	}
}
