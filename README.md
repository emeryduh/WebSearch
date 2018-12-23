# WebSearch

### Installation for Windows
1  Install the [JDK](https://www.oracle.com/technetwork/java/javase/downloads/index.html)
	
	- Go to https://www.oracle.com/technetwork/java/javase/downloads/index.html and download the JDK (not the JRE)
	** It is important that you download version 8u191/8u192, ElasticSearch will not work with 11.0.1
	- Add the /Java/jdk/bin directory to your environment PATH
		- This can be found typically in the C:/Program Files/ directory
	- If it doesn't already exist, create the JAVA_HOME user variable and set it's value to the /Java/jdk directory
	
2  Install [Apache Maven](https://maven.apache.org/download.cgi)
	
	- Go to https://maven.apache.org/download.cgi and download the binary ZIP archive
	- Extract the archive to any folder, it is recommended to extract it into the C:/Program Files/ directory
	- Add the /apahce-maven/bin directory to your environment PATH variables
	
3  Install [Eclipse](https://www.eclipse.org/downloads/)
	
	- Go to https://www.eclipse.org/downloads/
	- Run the installer
	
4  Install [ElasticSearch](https://www.elastic.co/downloads/elasticsearch)
	
	- Go to https://www.elastic.co/downloads/elasticsearch
	- Download and extract the archive to any folder, again it is recommended to extract it into C:/Program Files/
	- Add the /elasticsearch/bin directory to your environment PATH
	
5  Download/Clone this project to your computer

### Compiling and Running the Project
1.  Open Eclipse, choose any workspace
2.  Import the Maven Project by selecting: File > Import... > Type `Existing Maven` > Select the only option and hit Next > Click "Browse..." and navigate to the cloned project > Click Finish
3.  Right-click the project under the Package Explorer for Eclipse > Run As > maven build... > Type `package shade:shade` into the Goals field > Click Run.  
If your Maven, JDK, and Eclipse installations are successful this will compile the project successfully
4.  Open a terminal window (cmd prompt) and type `elasticsearch`.  This will load up the elasticsearch server to be queried.
5.  Open another terminal window and change the directory to the root project folder
6.  Type into the new terminal window `java -cp target/web-search-1.0-SNAPSHOT.jar com.itec4020.websearch.App`

The jar will now decompress, extract documents, index them, and start up a web server when it's done.  You can view the website for the search engine by going to http://localhost:8080/


