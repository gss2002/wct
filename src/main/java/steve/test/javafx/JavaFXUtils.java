package steve.test.javafx;

import java.io.File;

public class JavaFXUtils {
	
	
	public static void main(String[] args) {
		try {
			registerJavaFXInClasspath();
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	
	public static void registerJavaFXInClasspath() throws Exception {
		String javahome = System.getProperty("java.home");
		System.out.println(javahome);
		File javafxJar = new File(javahome+File.separator+"lib"+File.separator+"jfxrt.jar");
		loadLibrary(javafxJar);
	}

	public static synchronized void loadLibrary(File jar) throws Exception
	{        
	    try {                    
	        /*We are using reflection here to circumvent encapsulation; addURL is not public*/
	        java.net.URLClassLoader loader = (java.net.URLClassLoader)ClassLoader.getSystemClassLoader();                        
	        java.net.URL url = jar.toURI().toURL();
	        /*Disallow if already loaded*/
	        for (java.net.URL it : java.util.Arrays.asList(loader.getURLs())){
	            if (it.equals(url)){
	                throw new Exception("library " + jar.toString() + " is already loaded");
	            }                
	        }                 
	        java.lang.reflect.Method method = java.net.URLClassLoader.class.getDeclaredMethod(
	            "addURL", 
	            new Class[]{java.net.URL.class}
	        );
	        method.setAccessible(true); /*promote the method to public access*/
	        method.invoke(loader, new Object[]{url});
	    } catch (Exception e){
	        throw new Exception(e.getMessage());
	    }        
	}
}
