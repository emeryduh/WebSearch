package com.itec4020.websearch;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.zip.GZIPInputStream;

import org.apache.http.HttpHost;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseListener;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class ElasticSearchHandler {
	
	// REST Client to handle requests to ElasticSearch server
	RestClient rest;

	// Location for ElasticSearch index
	final String ELASTIC_PATH = "/files/documents";
	
	public ElasticSearchHandler() {
		open();
	}
	
	/**
	 * Open the REST client connection when booting up
	 */
	public void open() {
		// Initialize the REST client to communicate with the ElasticSearch server
		rest = RestClient.builder(new HttpHost("localhost", 9200, "http"), new HttpHost("localhost", 9201, "http"))
				.build();
		
		if(rest != null) {
			System.out.println("REST Client has been started.");
		}
	}

	/**
	 * Close the REST client connection when no longer needed
	 */
	public void close() {
		try {
			rest.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Extract document data from compressed files
	 */
	public void decompress() {
		decompressDirectory("WT01");
		decompressDirectory("WT02");
		decompressDirectory("WT03");
	}

	/**
	 * Index documents into ElasticSearch
	 */
	public void index() {
		indexDocuments("WT01", 0);
		indexDocuments("WT02", 1);
		indexDocuments("WT03", 2);
	}

	/**
	 * Searches given the information
	 */
	public String search(String title, String content) {
		try {
			Request request = new Request("GET", ELASTIC_PATH + "/_search");

			
			XContentBuilder builder = jsonBuilder()
					.startObject()
					.array("_source", "title", "docno")
					.startObject("query")
					.startObject("bool")
					.startArray("should")
					.startObject()
					.startObject("match")
					.startObject("title")
					.field("query", title)
					.field("boost", 100)
					.endObject()
					.endObject()
					.endObject()
					.startObject()
					.startObject("match")
					.field("content", content)
					.endObject() // end match
					.endObject() // close open bracket
					.endArray() // end should
					.endObject() // end bool
					.endObject() // end query
					.endObject(); // close open bracket
				
			String json = Strings.toString(builder);
			System.out.println("Request:  " + json);
			request.setJsonEntity(json);
			//request.addParameter("pretty", "true");

			builder.close();
			
			// Get the response from the server for the search
			Response resp = rest.performRequest(request);
			
			//System.out.println(EntityUtils.toString(resp.getEntity()));
			
			// Return the JSON string that is in the Response
			return EntityUtils.toString(resp.getEntity());
			//return "test2";
		} catch (Exception e) {
			System.out.println(e);
		}
		
		return null;
	}

	/**
	 * Decompress all .GZ files in the given folder. Will assume that the folder
	 * name provided resides in %PROJECT_PATH%/data/{folder}. The output will be
	 * stored in %PROJECT_PATH%/extracted/{folder}. If the extracted folder does not
	 * exist it will be created.
	 * 
	 * @param folder The folder name to pull data from and to extract data to.
	 */
	public void decompressDirectory(String folder) {
		// Establish paths to the given folders
		String filePath = System.getProperty("user.dir") + "\\data\\" + folder;
		String outputPath = System.getProperty("user.dir") + "\\extracted\\" + folder;

		// Create the output directories if they don't exist
		new File(outputPath).mkdirs();

		// Gather all .GZ files in the directory
		File[] files = getFilesByExt(filePath, ".GZ");

		// Buffer used to read in data from file
		byte[] buffer = new byte[1024];

		try {
			for (int i = 0; i < files.length; i++) {
				// Setup input streams to extract data from the file
				FileInputStream fis = new FileInputStream(files[i]);
				GZIPInputStream gis = new GZIPInputStream(fis);

				// The file name for the current File
				String fileName = files[i].getName();
				String outputName = fileName.substring(0, fileName.indexOf(".GZ")) + ".txt";

				// Setup output stream to push data into
				FileOutputStream fos = new FileOutputStream(outputPath + "\\" + outputName);

				// Stores data to be written
				int bytes_read;

				// Write data until the EOF has been reached
				while ((bytes_read = gis.read(buffer)) > 0) {
					fos.write(buffer, 0, bytes_read);
				}

				// Close streams when finished
				gis.close();
				fos.close();
			}

			System.out.println("Decompression complete for path:  " + filePath);
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	/**
	 * Returns a File array of all files that match a given extension filter.
	 * 
	 * @param directory The directory to search in
	 * @param extension The extension to search by
	 * @return All files that match the extension filter
	 */
	public File[] getFilesByExt(String directory, String extension) {
		// Make the search case insensitive
		final String ext = extension.toLowerCase();
		File fileDir = new File(directory);

		// Returns a list of all files that match the extension
		return fileDir.listFiles(new FilenameFilter() {
			// Specify the acceptance filter
			public boolean accept(File fileDir, String fileName) {
				return fileName.toLowerCase().endsWith(ext);
			}
		});
	}

	/**
	 * TODO: Remove indexBump and replace with a more dynamic way to generate
	 * indicies
	 * 
	 * Index gathered document data
	 * 
	 * @param folder The folder name for the data where the folder name is placed
	 *               like so: %PROJECT_DIR%/extracted/{folder}
	 */
	public void indexDocuments(String folder, int indexBump) {
		int numOfDocuments = 0;
		System.out.println("Indexing folder: " + folder);

		// Setup the file path
		String filePath = System.getProperty("user.dir") + "\\extracted\\" + folder;

		// Gather all files with the .txt extension in the file path
		File[] files = getFilesByExt(filePath, ".txt");

		// CountDownLoatch for asynchronously requests to be used to identify when the
		// next step can start
		// TODO: Will be used later when other tasks are completed
		final CountDownLatch latch = new CountDownLatch(files.length);

		for (int i = 0; i < files.length; i++) {
			System.out.println("Indexing file " + i);

			// Gather all relevant JSONs for this file's documents
			ArrayList<String> jsons = getJSONsFromPath(files[i].getPath());
			int size = jsons.size();

			for (int j = 0; j < size; j++) {
				numOfDocuments++;
				// Assign each page an index assuming i are files, and j are documents
				int documentIndex = (i * 10000) + j + (indexBump * 100000);

				// Build relevant requests
				Request request = new Request("PUT", ELASTIC_PATH + "/" + documentIndex);
				request.setJsonEntity(jsons.get(j));

				rest.performRequestAsync(request, new ResponseListener() {
					public void onSuccess(Response response) {
						// System.out.println(response);
						latch.countDown();
					}

					public void onFailure(Exception exception) {
						System.out.println(exception);
						latch.countDown();
					}
				});
			}
		}

		System.out.println("Number of Documents indexed: " + numOfDocuments);
	}

	/**
	 * Breaks apart an entire document that follows the format of the decompressed
	 * files into separate JSON strings. The strings will be used into
	 * ElasticSearch.
	 * 
	 * @param path The complete path with file name to be read in.
	 * @return An array of JSON strings to be used for indexing.
	 */
	public ArrayList<String> getJSONsFromPath(String path) {
		// The String array to be passed back once complete
		ArrayList<String> jsons = new ArrayList<String>();

		try {
			// The entire document that is currently being parsed.
			Document entireDoc = Jsoup.parse(new File(path), null);

			Element doc;
			int i = 0;

			// Break each Document (<DOC>) down into it's own index
			// Shift out the top-most DOC element to be processed
			while ((doc = entireDoc.select("doc").remove(i)) != null) {
				jsons.add(getJSONFromData(doc));
				i++;
			}
		} catch (NullPointerException errNull) {
			errNull.printStackTrace();
		} catch (IndexOutOfBoundsException errIndex) {
			// This occurs when there are no more DOC tags to select, in which case return
			// the JSONs
			return jsons;
		} catch (IOException errIO) {
			errIO.printStackTrace();
		}

		return jsons;
	}

	/**
	 * Extracts data from the given Element into the JSON format. It is expected
	 * that the Element provided was generated from Jsoup pulling out a single DOC
	 * tag. (i.e. the root element must have no siblings and must be a DOC tag)
	 * 
	 * @param e The DOC tag element with all information inside
	 * @return The JSON string for the data provided
	 */
	public String getJSONFromData(Element e) {
		try {
			String title = "", docno = "", olddocno = "", keywords = "", content = "";
			Element ele;

			// Only update the Strings if the elements exist
			if ((ele = e.select("title").first()) != null) {
				title = ele.text();
			}

			if ((ele = e.select("docno").first()) != null) {
				docno = ele.text();
			}

			if ((ele = e.select("docoldno").first()) != null) {
				olddocno = ele.text();
			}

			if ((ele = e.select("meta").first()) != null) {
				if (ele.hasAttr("name") && ele.attr("name") == "keywords") {
					if (ele.hasAttr("content")) {
						keywords = ele.attr("content");
						System.out.println("FOUND KEYWORDS: " + keywords);
					}
				}
			}

			// Gather only the body of the document as text content
			if ((ele = e.select("html").first()) != null) {
				content = ele.text();
			} else if ((ele = e.select("body").first()) != null) {
				content = ele.text();
			} else if ((ele = e.select("DOCHDR").first()) != null) {
				// If the document is malformed and is missing opening html and body tags
				// then instead strip out everything above and including the <DOCHDR>.
				// Then set the content as all remaining text for this <DOC>.
				int i = ele.elementSiblingIndex();

				// Recursively remove all siblings above the DOCHDR
				removePreviousSiblings(ele, i);
				content = e.text();
			} else {
				// In the unlikely scenario that the document is malformed and doesn't have an
				// html, body, or DOCHDR tag, then instead set the content as the entire <DOC>
				content = e.text();
			}

			// Build the json using the extracted data
			XContentBuilder builder = jsonBuilder().startObject().field("title", title).field("docno", docno)
					.field("olddocno", olddocno).field("keywords", keywords).field("content", content).endObject();
			
			return Strings.toString(builder);
		} catch (Exception exc) {
			exc.printStackTrace();
		}

		return null;
	}

	/**
	 * Recursively removes the previous sibling until there are no more jumps
	 * 
	 * @param e The bottom-most element to start removing
	 */
	public void removePreviousSiblings(Element e, int jumpsLeft) {
		if (jumpsLeft > 0) {
			removePreviousSiblings(e.previousElementSibling(), jumpsLeft - 1);
		}

		e.remove();
	}
}
