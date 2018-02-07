/**
 * @author Adrian Olszak
 * @version 1.0
 *
 * */
package com.olszak.JarForCC_maven_plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.TreeMap;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

import java.util.Enumeration;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * 
 * Plugin for Apache Maven's tool used to create the distributive 
 * version of  JSF composite components
 * 
 * Optional Parameters:
 * @param pathOfJar Set the path in which it is to be created .jar file.
 * Default value is: "C:\\".
 * @param pathOfClasses Set the path where the java files are located.
 * Default value is: "src/main/java".
 * @param pathOfResources Set the path in which the resources directory is located.
 * Default value is: "src/main/webapp/resources".
 * @param jarName Set the name of .jar file which you want to create.
 * Default value is: "jarCompositeComponent".
 * @param taglibName Set the namespace which you want to use in taglib file.
 * Default value is: "defaulttaglib.com".
 * @param pathOfOwnFacesConfig Set the path of your own faces-config file,
 * or don't chagne to create empty faces-config file.
 * Default value is: "1".
 * @param pathOfLib Set the path in which the library directory is located.
 * Default value is: "src/main/webapp/lib/".
 * 
 */
@Mojo(name = "JarOfCC")
public class CreateJarOfCompositeComponent  extends AbstractMojo {

    @Parameter(property = "pathOfJar",defaultValue = "C:\\")
    private String pathOfJar;
    @Parameter(property = "pathOfClasses",defaultValue = "src/main/java")
    private String pathOfClasses;
    @Parameter(property = "pathOfResources",defaultValue = "src/main/webapp/resources")
    private String pathOfResources;
    @Parameter(property = "jarName",defaultValue = "jarCompositeComponent")
    private String jarName;
    @Parameter(property = "taglibName",defaultValue = "defaulttaglib.com")
    private String taglibName;
    @Parameter(property = "pathOfOwnFacesConfig",defaultValue = "1")
    private String pathOfOwnFacesConfig;
    @Parameter(property = "pathOfLib",defaultValue = "src/main/webapp/lib/")
    private String pathOfLib;
    private String path = pathOfJar;
    private String temporaryDirectoryName;
	private Map<String, String> components = new TreeMap();
	private final String facesConfigName = "faces-config.xml";
	private final String [] taglibXmlHeaders = {"http://xmlns.jcp.org/xml/ns/javaee", "facelet-taglib",
	"version","2.2",	
	"xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance",
	"xsi:schemaLocation", "http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-facelettaglibrary_2_2.xsd"
	};
	/**
	 * This method is used to create appropriate structure
	 * of .jar file with required files that it creates.
	 * This method is executed after run plugin with this 
	 * goal: JarForCC-maven-plugin:JarOfCC
	 * @return Nothing.
	 * @exception MojoExecutionException on execution error. 
	 */
    public void execute() throws MojoExecutionException {
    	try {
    		run();
    	} catch (IOException e) {
    		e.printStackTrace();
    	}        
    }
    
    private String getPathOfJar() {
        return pathOfJar;
    }
    /**
	 * This method is used to check if META-INF file exists.
	 * @return {@code true} if META-INF file exists {@code false} otherwise
	 */
    public boolean checkMetaInf(){
		File folder = new File(path +"META-INF");
		File[] listOfFiles = folder.listFiles();

		    for (int i = 0; i < listOfFiles.length; i++) {
		      if (listOfFiles[i].isFile() && listOfFiles[i].getName().equals("MANIFEST.MF")) {
		    	  return true;
		      } 
		    }
		    return false;
	}
    /**
     * This is function to extract additional libraries if 
     * they exist
     * @return Nothing.
     * @exception IOException On input error.
     * @see IOException
     */
	public void unzipAll()throws IOException{
		java.nio.file.Path lib = Paths.get(System.getProperty("user.dir") + File.separator + pathOfLib); 
		if (Files.exists(lib)) {
			File folder = new File(System.getProperty("user.dir") + File.separator + pathOfLib);
			
			File[] listOfFiles = folder.listFiles();
	
			for (int i = 0; i < listOfFiles.length; i++) {
				String[] parts = listOfFiles[i].getName().split(Pattern.quote("."));
				if (listOfFiles[i].isFile()) {
					if(parts[parts.length -1].equals("jar"))unzipJar(path, folder.getAbsolutePath()+ "/" + listOfFiles[i].getName());
				} 
			}
		}
	}
    /**
     * This is function to call the other functions needed 
     * to create the .jar file
     * @return Nothing.
     * @exception IOException On input error.
     * @see IOException
     */
    public void run() throws IOException
    {
    createFiles();
	unzipAll();
	createFacesConfig();
	checkMetaInf();
	copyCompositeComponents();
	copyClasses();
	createTagLib("http://"+taglibName+"/taglib");
      Manifest manifest = new Manifest();
      manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
	  JarOutputStream target = new JarOutputStream(new FileOutputStream(getPathOfJar() + jarName + ".jar"));
      if(!checkMetaInf()){
    	  target = new JarOutputStream(new FileOutputStream(getPathOfJar() + jarName + ".jar"),manifest);
      }
      File folder = new File(path);
      File[] listOfFiles = folder.listFiles();

      for (int i = 0; i < listOfFiles.length; i++) {
    	  if(!listOfFiles[i].getName().equals(jarName + ".jar")){
    	  File plik = new File(path + listOfFiles[i].getName());
          add(plik, target,path);    
    	  }
      } 
      target.close();
      FileUtils.deleteDirectory(this.path);	    	    
    }
    /**
     * This is a function that adds files to be packed 
     * into a .jar file
     * @param source This is the parameter with the file 
     * path of source file
     * @param target This is the parameter with .jar file 
     * in which the source files will be placed
     * @param removme This is the parameter with path of 
     * text to delete
     * @return Nothing.
     * @exception IOException On input error.
     * @see IOException
     */ 
    public static void add(File source, JarOutputStream target, String removeme) throws IOException
    {
    	BufferedInputStream in = null;
        try
        {
        	File parentDir = new File(removeme);
            File source2 = new File(source.getCanonicalPath().substring(
                    parentDir.getCanonicalPath().length() + 1,
                    source.getCanonicalPath().length()));
            if (source.isDirectory())
            {
                String name = source2.getPath().replace("\\", "/");
                if (!name.isEmpty())
                {
                    if (!name.endsWith("/"))name += "/";
                    JarEntry entry = new JarEntry(name);
                    entry.setTime(source.lastModified());
                    target.putNextEntry(entry);
                    target.closeEntry();
                }
                for (File nestedFile : source.listFiles())
                {
                    add(nestedFile, target, removeme);
                }
                return;
            }

            JarEntry entry = new JarEntry(source2.getPath().replace("\\", "/"));
            entry.setTime(source.lastModified());
            target.putNextEntry(entry);
            in = new BufferedInputStream(new FileInputStream(source));

            byte[] buffer = new byte[2048];
            while (true)
            {
                int count = in.read(buffer);
                if (count == -1)
                    break;
                target.write(buffer, 0, count);
            }
            target.closeEntry();
        }
        finally
        {
            if (in != null)
                in.close();
        }
    }
    /**
     * This is the function to copy folder from 
     * one place to second
     * @param src File or directory to copy
     * @param dest File or directory where the copied 
     * files will be placed
     * @param a Number with information what kind of files 
     * are copied
     * @return Nothing.
     * @exception IOException On input error.
     * @see IOException
     */
    private void copyFolder(File src, File dest,int a )
        	throws IOException{
        	if(src.isDirectory()){

        		if(!dest.exists()){
        		   dest.mkdir();
        		}

        		String files[] = src.list();
        		for (String file : files) {
        			if(!file.equals("META-INF")){
        		   File srcFile = new File(src, file);
        		   File destFile = new File(dest, file);
        		   copyFolder(srcFile,destFile,a);
        			}
        		}

        	}else{
        		InputStream in = new FileInputStream(src);
        	        OutputStream out = new FileOutputStream(dest);
        	        byte[] buffer = new byte[1024];

        	        int length;
        	        while ((length = in.read(buffer)) > 0){
        	    	   out.write(buffer, 0, length);
        	        }
        	        
        	        in.close();
        	        out.close();
        	        if(a==0)makeMap(dest);
        	}
        }
	/**
	   * This is method to copy resources directory.
	   * @return 0.
	   * @exception IOException On input error.
	   * @see IOException
	   */    
	private int copyCompositeComponents(){
		File source = new File(System.getProperty("user.dir") + File.separator + pathOfResources);
		if(!source.exists()) return 0;
		File dest = new File(path + "META-INF" + File.separator + "resources");
		dest.getParentFile().mkdirs(); 
		try{
        	copyFolder(source,dest,0);
           }catch(IOException e){
        	e.printStackTrace();
                System.exit(0);
           }
		return 0;
	}
	
	/**
	   * This is method to copy compiled and source java files.
	   * @return 0.
	   * @exception IOException On input error.
	   * @see IOException
	   */
	private int copyClasses(){
		File source = new File(System.getProperty("user.dir") + File.separator + pathOfClasses);
		File source2 = new File(System.getProperty("user.dir") + File.separator + "target/classes");
		if(!source.exists()) return 0;
		File dest = new File(path);
		try{
        	copyFolder(source,dest,1);
        	copyFolder(source2,dest,1);
           }catch(IOException e){
        	e.printStackTrace();
                System.exit(0);
           }
		return 0;
	}
	/**
	   * This is method to create taglib.xml file.
	   * @param tagLib This parameter contains the 
	   * namespace that will be used in taglib file.
	   * @return Nothing.
	   * @exception IOException On input error.
	   * @see IOException
	   */
	private void createTagLib(String tagLib){
		 try {
				TransformerFactory transformerFactory = TransformerFactory.newInstance();
				Transformer transformer = transformerFactory.newTransformer();
				DOMSource source = null;
				DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

				Document doc = docBuilder.newDocument();
				Element rootElement = doc.createElementNS(taglibXmlHeaders[0],taglibXmlHeaders[1]);
				rootElement.setAttribute(taglibXmlHeaders[2],taglibXmlHeaders[3]);
				rootElement.setAttribute(taglibXmlHeaders[4],taglibXmlHeaders[5]);
				rootElement.setAttribute(taglibXmlHeaders[6],taglibXmlHeaders[7]);
				doc.appendChild(rootElement);

				Element namespace = doc.createElement("namespace");
				rootElement.appendChild(namespace);
				namespace.appendChild(doc.createTextNode(tagLib));
				for (Map.Entry<String, String> entry : components.entrySet()){
				Element tag = doc.createElement("tag");
				rootElement.appendChild(tag);

				Element tagName = doc.createElement("tag-name");
				tagName.appendChild(doc.createTextNode(entry.getKey()));
				tag.appendChild(tagName);

				Element component = doc.createElement("component");
				tag.appendChild(component);

				Element resourceId = doc.createElement("resource-id");
				resourceId.appendChild(doc.createTextNode(entry.getValue() + entry.getKey() + ".xhtml"));
				component.appendChild(resourceId);

				source = new DOMSource(doc);}

				StreamResult result = new StreamResult(new File( path + "META-INF"+ File.separator + "resources"+ File.separator + taglibName+".taglib.xml"));

				transformer.transform(source, result);
				
			  } catch (ParserConfigurationException pce) {
				pce.printStackTrace();
			  } catch (TransformerException tfe) {
				tfe.printStackTrace();
			  }
	}
	/**
	   * This is the method to create faces-config file or copy 
	   * from given path
	   * @param args Unused.
	   * @return Nothing.
	   * @exception IOException On input error.
	   * @see IOException
	   */
	private void createFacesConfig() throws IOException {
		if(pathOfOwnFacesConfig.equals("1")){			
			InputStream in = getClass().getResourceAsStream("/" + facesConfigName); 
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));
			String textLine = bufferedReader.readLine();
			PrintWriter writer = null;
			try {
				writer = new PrintWriter(path +  "META-INF" + File.separator + facesConfigName, "UTF-8");
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			do 	{
					writer.println(textLine);		  
					textLine = bufferedReader.readLine();
				} while(textLine != null);
			bufferedReader.close();			
			writer.close();
		}else{
			File source = new File(pathOfOwnFacesConfig);
			String[] parts = pathOfOwnFacesConfig.split(Pattern.quote("\\"));
			File dest = new File(path +  "META-INF" + File.separator + parts[parts.length - 1]);
		    try {
		        Files.copy(source.toPath(), dest.toPath(),StandardCopyOption.REPLACE_EXISTING );
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	/**
	   * This is method to create temporary directory in 
	   * system temporary directory
	   * @param args Unused.
	   * @return Nothing.
	   * @exception IOException On input error.
	   * @see IOException
	   */
	private void createFiles() {
		temporaryDirectoryName = "olszak" + String.valueOf(System.currentTimeMillis());
		this.path = System.getProperty("java.io.tmpdir") + temporaryDirectoryName + File.separator; 
		//this.path = getPathOfJar() + temporaryDirectoryName + File.separator;	
		File f = new File(path+  "META-INF" + File.separator + facesConfigName);
		new File(getPathOfJar()).mkdirs();
		f.getParentFile().mkdirs(); 
		try {
			f.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	/**
	   * This is the method to create map structure with 
	   * path and name of composite components.
	   * @param dest Path of file.
	   * @return Nothing.
	   */
	private void makeMap(File dest){
		String[] path = dest.toString().split(Pattern.quote("\\"));
		int foundResource = 0;
		String key = "";
		for ( int i = 0; i<path.length; i++){
			if(foundResource == 1 && i<path.length - 1)key = key + path[i] + File.separator;
			if(path[i].equalsIgnoreCase("resources")){
				foundResource = 1;
			}
		}
		String[] fileName = path[path.length - 1].split(Pattern.quote("."));
		if(fileName.length>1){
		if(fileName[1].equals("xhtml"))components.put(fileName[0],key);
		}
	}
	/**
	   * This is the function to unzip single jar file.
	   * @param destinationDir This is path where
	   * files will be placed.
	   * @param jarPath This is path of jar file.
	   * @return Nothing.
	   * @exception IOException If file input doesn't exist.
	   * @see IOException
	   */
	private static void unzipJar(String destinationDir, String jarPath) throws IOException {
		File file = new File(jarPath);
		JarFile jar = new JarFile(file);
		for (Enumeration<JarEntry> enums = jar.entries(); enums.hasMoreElements();) {
			JarEntry entry = (JarEntry) enums.nextElement(); 
			String fileName = destinationDir + File.separator + entry.getName();
			File f = new File(fileName); 
			if (fileName.endsWith("/")) {
				f.mkdirs();
			}
		}
		for (Enumeration<JarEntry> enums = jar.entries(); enums.hasMoreElements();) {
			JarEntry entry = (JarEntry) enums.nextElement();
			String fileName = destinationDir + File.separator + entry.getName();
			File f = new File(fileName);
			if (!fileName.endsWith("/")) {
				InputStream is = jar.getInputStream(entry);
				FileOutputStream fos = new FileOutputStream(f);
				while (is.available() > 0) {
					fos.write(is.read());
				}
				fos.close();
				is.close();
			}
		}
	}

}