package com.itec4020.websearch;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

@SuppressWarnings("restriction")
public class WebServerHandler implements HttpHandler {
	
	// Directory to serve static files from
	final String ROOT_DIRECTORY = "./public";
	
	// Used to call ElasticSearch functions
	ElasticSearchHandler elastic;
	
	// Content-Type (MIME) map
	private static final Map<String,String> MIME_MAP = new HashMap<String, String>();
    static {
        MIME_MAP.put("appcache", "text/cache-manifest");
        MIME_MAP.put("css", "text/css");
        MIME_MAP.put("gif", "image/gif");
        MIME_MAP.put("html", "text/html");
        MIME_MAP.put("js", "application/javascript");
        MIME_MAP.put("json", "application/json");
        MIME_MAP.put("jpg", "image/jpeg");
        MIME_MAP.put("jpeg", "image/jpeg");
        MIME_MAP.put("mp4", "video/mp4");
        MIME_MAP.put("pdf", "application/pdf");
        MIME_MAP.put("png", "image/png");
        MIME_MAP.put("svg", "image/svg+xml");
        MIME_MAP.put("xlsm", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        MIME_MAP.put("xml", "application/xml");
        MIME_MAP.put("zip", "application/zip");
        MIME_MAP.put("md", "text/plain");
        MIME_MAP.put("txt", "text/plain");
        MIME_MAP.put("php", "text/plain");
    }
    
    public WebServerHandler(ElasticSearchHandler esh) {
    	elastic = esh;
    }
    
    /**
     * Handles the HTTP requests
     */
	public void handle(HttpExchange h) throws IOException {
		String reqPath = h.getRequestURI().getPath();
        String method = h.getRequestMethod();
        
        // Provide landing page
        if(reqPath.equals("/")) {
        	reqPath = "/index.html";
        }
        
		// Perform search if requested
		if(reqPath.equalsIgnoreCase("/search") && method.equals("POST")) {
			// Set the req path as the search page
			reqPath = "/index.html";
			
			// Gather the request parameters
			String query = h.getRequestURI().getQuery();
			String[] params = query.split("&");
			
			String title = "";
			String content = "";
			// Set the values based on the request parameters
			for(int i = 0; i < params.length; i++) {
				String[] newParam = params[i].split("=");
				if(newParam[0].equals("title")) {
					title = newParam[1];
				} else if (newParam[0].equals("content")) {
					content = newParam[1];
				}
			}
			
			// Perform the search
	        String result = elastic.search(title, content);
	        
	        // Send the Search-Results as part of a custom header
	        h.getResponseHeaders().set("Search-Data", result);
		}
		
		// Find the file in the root directory
		File f = new File(ROOT_DIRECTORY, reqPath);
		
        try {
        	// Get the canonical path to this directory to check if a 
        	// Path Traversal attack is happening.  If the canonical
        	// path does not match the ROOT_DIRECTORY path then we know
        	// and attack is being attempted.
            File canonFile = f.getCanonicalFile();
	        String canonPath = canonFile.getPath();
	        if (! canonPath.startsWith(new File(ROOT_DIRECTORY).getCanonicalPath())) {
	        	// If so exit handler
	        	sendError(h, 404);
	            return;
	        }
	        
	        // Set the Content-Type for the response based on extension
			String ext = getExtension(canonPath).toLowerCase();
	        String mime = MIME_MAP.getOrDefault(ext, "application/octet-stream");
	        h.getResponseHeaders().set("Content-Type", mime);
	        // Output the file and close the streams
            FileInputStream fis = new FileInputStream(canonFile);
            h.sendResponseHeaders(200, canonFile.length());
            OutputStream os = h.getResponseBody();
            copyStream(fis, os);
            os.close();
            fis.close();
            h.close();
        } catch (Exception e) {
        	sendError(h, 404);
            return;
        }
	}
	
	/**
	 * Render an error to the user if the path isn't valid.
	 * 
	 * @param h
	 * @param errorCode
	 * @throws IOException
	 */
	private void sendError(HttpExchange h, int errorCode) throws IOException {
        String msg = "Sorry!  We could not render the page based on your request.  Please try again.";
        byte[] msgBytes = msg.getBytes("UTF-8");
        h.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        h.sendResponseHeaders(errorCode, msgBytes.length);
        OutputStream os = h.getResponseBody();
        os.write(msgBytes);
        os.close();
    }
	
	/**
	 * Returns the extension string to be used to get the content type provided in the
	 * response.
	 * 
	 * @param path The path to the file being checked against
	 * @return The extension for the provided file path
	 */
	private static String getExtension(String path) {
        int slashIndex = path.lastIndexOf('/');
        String basename = (slashIndex < 0) ? path : path.substring(slashIndex + 1);

        int dotIndex = basename.lastIndexOf('.');
        if (dotIndex >= 0) {
            return basename.substring(dotIndex + 1);
        } else {
            return "";
        }
    }
	
	/**
	 * Writes data from an established InputStream into the given OutputStream.  Used for
	 * reading in files to be served to the user.
	 * 
	 * @param is The input stream to read from
	 * @param os The output stream to write data into
	 * @throws IOException
	 */
	private void copyStream(InputStream is, OutputStream os) throws IOException {
        byte[] buf = new byte[4096];
        int n;
        while ((n = is.read(buf)) >= 0) {
            os.write(buf, 0, n);
        }
    }
	
}
