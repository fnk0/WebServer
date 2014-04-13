package main;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class Page {

	/**
	 * Constants!!!
	 */
	public static final String HEADER_ERROR = "HTTP/1.1 404 Not Found\r\n\r\n";
	public static final String HEADER_SUCCESS = "HTTP/1.1 200 OK\r\n";
	public static final String HEADER_HTML = "Content-type: text/html\r\n\r\n";
	public static final String HEADER_JPG = "Content-type: image/jpeg\r\n\r\n";
	public static final String HEADER_PNG = "Content-type: image/png\r\n\r\n";
	public static final String HEADER_PDF = "Content-type: application/pdf\r\n\r\n";
	public static final String HEADER_ZIP = "Content-type: application/zip\r\n\r\n";
	public static final String HEADER_CSS = "Content-type: text/css\r\n\r\n";
	public static final String JAVASCRIPT_HEADER = "Content-type: application/javascript\r\n\r\n";
	public static final Charset ISO_ENCODE = Charset.forName("ISO-8859-1");
	public static final Charset UTF_ENCODE = StandardCharsets.UTF_8;
	public static final String SERVER_FOLDER = "ServerFolder/";
	public static final String INDEX_PAGE = "index.html";
	public static final String _404_PAGE = "page404.html";
	public static final String PAGE_DENIED = "access_denied.html";
	public static final String FOLDER_PAGE = "folder.html";
	public static final int TXT_FILE = 0;
	public static final int HTML_FILE = 1;
	public static final int JPEG_FILE = 2;
	public static final int PDF_FILE = 3;
	public static final int ZIP_FILE = 4;
	public static final int DIRECTORY = 5;
	public static final int PNG_FILE = 6;
	public static final int OTHER_FILE = 7;
	public static final int CSS_FILE = 8;
	public static final int JAVASCRIPT_FILE = 9;
	public static final int JAVA_FILE = 10;
	public static final int DIR_BACK = 11;
	public static final int PHP_FILE = 12;
	public static final int AWESOME_FILE = 13;

	// Private Instance Variables.
	private boolean accesDenied = false;
	private boolean retrieveFiles = false;
	private boolean empty = false;
	private boolean isIndexPage = false;
	private boolean skipReader = false;
	private File pageFile;
	private Socket socket;

	public Page(Socket socket) {
		this.socket = socket;
	}

	public Page(File pageFile, Socket socket) {
		this.pageFile = pageFile;
		this.socket = socket;

	}

	public void setPageFile(File pageFile) {
		this.pageFile = pageFile;
	}

	public File getPageFile() {
		return pageFile;
	}

	/**
	 * Retrieves the page and prints it to the client.
	 * 
	 * @param headerString
	 * @throws FileNotFoundException
	 */
	public void retrievePage(String headerString) throws FileNotFoundException {

		DataOutputStream writer = null;
		File fileToRetrieve = null;
		BufferedReader reader = null;
		String output = "";
		String readLineString;

		try {
			writer = new DataOutputStream(socket.getOutputStream());
		} catch (IOException e) {
			System.out.println("OutputStream not found!!");
		}

		String fileName = getFileName(headerString);

		try {
			if (shouldReturnIndex(fileName)) {
				fileToRetrieve = getIndexPage();
				isIndexPage = true;
				retrieveFiles = true;
			} else {
				if (isAccessible(fileName)) {
					fileToRetrieve = getUserPage(fileName);
				} else {
					// System.out.println("Access Denied!!");
					accesDenied = true;
				}
			}
			if (!accesDenied) {
				if (fileToRetrieve.getCanonicalFile().isDirectory()) {
					retrieveFiles = true;
				} else {
					reader = new BufferedReader(new FileReader(fileToRetrieve));
				}
			} else {
				fileToRetrieve = getAccessDenied();
				reader = new BufferedReader(new FileReader(fileToRetrieve));
			}
		} catch (FileNotFoundException e) {
			if (fileToRetrieve.length() == 0) {
				fileToRetrieve = get404page();
				reader = new BufferedReader(new FileReader(fileToRetrieve));
			}
		} catch (IOException e) {
		}
		try {
			if (retrieveFiles == true && writer != null) {
				output = output + getFilesInDirectory(fileToRetrieve.getPath());
				// System.out.println(output);
				skipReader = true;
				retrieveFiles = false;
			}
			if (reader != null && writer != null) {
				while ((readLineString = reader.readLine()) != null) {
					if (retrieveFiles == true) {
						output = output
								+ getFilesInDirectory(fileToRetrieve.getPath());
						retrieveFiles = false;
						break;
					}
					output = output + readLineString;
				}
			}
			if (fileToRetrieve != null) {
				int fileType = getFileType(getFileExtension(fileToRetrieve));
				// System.out.println(getHeader(fileType));
				writer.writeBytes(Page.HEADER_SUCCESS);
				writer.writeBytes(getHeader(fileType));

				if (getHeader(fileType) != Page.HEADER_HTML) {
					sendFileToServer(writer, fileToRetrieve);
				} else {
					writer.writeBytes(output);
				}
			}
			writer.flush();
			writer.close();
			skipReader = false;
		} catch (IOException e) {
			System.out.println("No Output Stream");
		} catch (Exception e) {
			System.out.println("Other Exception");
			e.printStackTrace();
		}
	}

	/**
	 * Checks if the user have permission to access the directory.
	 * 
	 * @param pathName
	 * @return
	 */
	public boolean isAccessible(String pathName) {
		File fileToCheck = new File(pathName);
		// System.out.println("Path Name: " + pathName);
		// System.out.println("File Path: " + fileToCheck.getPath());

		if (fileToCheck.getPath().contains(Page.SERVER_FOLDER)) {
			return true;
		}
		return false;
	}

	/**
	 * Gets the name of the file from the header.
	 * 
	 * @param headerString
	 *            the first line of the Header String
	 * @return fileName String with the fileName
	 */
	public String getFileName(String headerString) {
		String toReturn = headerString.replaceAll("GET ", "").replaceAll(
				" HTTP/1.1", "");

		if (toReturn.startsWith("/")) {
			toReturn = toReturn.substring(1);
		}

		// System.out.println(toReturn);
		return toReturn;
	}

	@SuppressWarnings("resource")
	public void sendFileToServer(DataOutputStream writer, File output)
			throws IOException {
		try {
			InputStream input = new FileInputStream(output);
			byte[] byteArray = new byte[(int) output.length()];
			int readData;
			while ((readData = input.read(byteArray)) != -1) {
				try {
					writer.write(byteArray, 0, readData);
				} catch (IOException e) {

				}
			}
		} catch (FileNotFoundException e) {

		}
	}

	/**
	 * Method to check if the fileName should return index in the cases of being
	 * in the server folder or empty.
	 * 
	 * @param fileName
	 * @return
	 */
	public boolean shouldReturnIndex(String fileName) {
		// Check if the address is empty!
		// System.out.println("Filename is...: " + fileName);
		if (fileName.isEmpty() || fileName.equals("/")) {
			empty = true;
			return true;
		}
		if (fileName.equals(Page.SERVER_FOLDER)
				|| fileName.equals(Page.SERVER_FOLDER.replaceAll("/", ""))) {
			return true;
		}
		return false;
	}

	/**
	 * Gets the directory of a file based on it's name
	 * 
	 * @param fileName
	 * @return
	 */
	public String getWorkingDirectory(String fileName) {
		File file = new File(fileName);
		return file.getPath().replaceAll(fileName, "");
	}

	/**
	 * Get the files of a directory and prints it out to the client formated in
	 * HTML.
	 * 
	 * @param path
	 * @return
	 */
	public String getFilesInDirectory(String path) {

		// Page content
		File myDirectory = null;
		HtmlPage myPage = new HtmlPage();
		if (path.equals(getIndexPage().getPath())) {
			myDirectory = new File(Page.SERVER_FOLDER);
		} else {
			myDirectory = new File(path);
		}

		// System.out.println(myDirectory.getPath());
		String output = "<h2> Current Directory: /" + myDirectory.getPath()
				+ "</h2>\n";
		String fileAddress = "";
		output += "<div id=\"fileList\">";
		output += "<ul id=\"files\">";
		if(!isIndexPage) {
			output += "<li><img src=\"" + getThumbnailFileImage(Page.DIR_BACK)
					+ "\"height=\"30\" width=\"30\">&nbsp;&nbsp;&nbsp;&nbsp;"
					+ "<a href=\"" + "/" + myDirectory.getParent() + "/"
					+ "\">Parent Directory</a></li>\n";
		} else {
			output += "<li><img src=\"" + getThumbnailFileImage(Page.AWESOME_FILE)
					+ "\"height=\"30\" width=\"30\">&nbsp;&nbsp;&nbsp;&nbsp;"
					+ "You have reached the end of the internet!! </li>\n";
		}

		for (File file : myDirectory.listFiles()) {
			if (empty) {
				fileAddress = Page.SERVER_FOLDER + file.getName();
			} else if (!isIndexPage) {
				// System.out.println("File Path:" + file.getPath());
				fileAddress = "/" + file.getPath();

			} else {
				fileAddress = file.getName();
			}
			int fileType = getFileType(getFileExtension(file));
			output += "<li><img src=\"" + getThumbnailFileImage(fileType)
					+ "\"height=\"30\" width=\"30\">&nbsp;&nbsp;&nbsp;&nbsp;"
					+ "<a href=\"" + fileAddress + "\">" + file.getName()
					+ "</a></li>\n";
		}
		empty = false;
		isIndexPage = false;
		output += "</ul>";
		output += "</div>";

		myPage.setContent(output);

		return myPage.getHtmlPage();
	}

	public String getFileExtension(File file) {

		if (file != null) {
			String extension = null;
			String fileName = file.getName();
			boolean hasExtension = false;
			int i = fileName.length() - 1;
			for (; i > 0; i--) {

				if (fileName.charAt(i) == '.') {
					hasExtension = true;
					break;
				}
			}

			if (hasExtension) {
				extension = fileName.substring(i);
			} else {
				extension = "dir";
			}

			// System.out.println("File ext: " + extension);
			return extension;
		} else {
			return null;
		}
	}

	/**
	 * Gets the 404 page
	 * 
	 * @return
	 */
	public File get404page() {
		return new File(Page.SERVER_FOLDER + Page._404_PAGE);
	}

	/**
	 * Gets the index page
	 * 
	 * @return
	 */
	public File getIndexPage() {

		return new File(Page.SERVER_FOLDER + Page.INDEX_PAGE);
	}

	/**
	 * Gets the page typed in by the user.
	 * 
	 * @param fileName
	 * @return
	 */
	public File getUserPage(String fileName) {

		File toReturn = null;
		if (getWorkingDirectory(fileName).equals(Page.SERVER_FOLDER)) {
			toReturn = new File(fileName.replaceAll(Page.SERVER_FOLDER, ""));
		} else {
			toReturn = new File(fileName);
		}
		return toReturn;
	}

	/**
	 * Method to get the type of file based on it's extension.
	 * 
	 * @param fileExtension
	 * @return
	 */
	public int getFileType(String fileExtension) {
		if (fileExtension.equals(".png")) {
			return Page.PNG_FILE;
		} else if (fileExtension.equals(".jpg") || fileExtension.equals("jpeg")) {
			return Page.JPEG_FILE;
		} else if (fileExtension.equals(".txt")) {
			return Page.TXT_FILE;
		} else if (fileExtension.equals(".zip")) {
			return Page.ZIP_FILE;
		} else if (fileExtension.equals(".html")) {
			return Page.HTML_FILE;
		} else if (fileExtension.equals(".pdf")) {
			return Page.PDF_FILE;
		} else if (fileExtension.equals("dir")) {
			return Page.DIRECTORY;
		} else if (fileExtension.equals(".css")) {
			return Page.CSS_FILE;
		} else if (fileExtension.equals(".js")) {
			return Page.JAVASCRIPT_FILE;
		} else if (fileExtension.equals(".java")) {
			return Page.JAVA_FILE;
		} else if (fileExtension.equals(".php")) {
			return Page.PHP_FILE;
		}
		return Page.OTHER_FILE;
	}
	
	/**
	 *  Gets the image used by the specific file format.
	 * @param fileType
	 * @return
	 */
	public String getThumbnailFileImage(int fileType) {
		switch (fileType) {
		case Page.PNG_FILE:
		case Page.JPEG_FILE:
			return "/" + Page.SERVER_FOLDER + "assets/image.png";
		case Page.DIRECTORY:
			return "/" + Page.SERVER_FOLDER + "assets/folder.png";
		case Page.HTML_FILE:
			return "/" + Page.SERVER_FOLDER + "assets/web.png";
		case Page.ZIP_FILE:
			return "/" + Page.SERVER_FOLDER + "assets/zip.png";
		case Page.PDF_FILE:
			return "/" + Page.SERVER_FOLDER + "assets/pdf.png";
		case Page.CSS_FILE:
			return "/" + Page.SERVER_FOLDER + "assets/css.png";
		case Page.JAVA_FILE:
			return "/" + Page.SERVER_FOLDER + "assets/java.png";
		case Page.JAVASCRIPT_FILE:
			return "/" + Page.SERVER_FOLDER + "assets/js.png";
		case Page.DIR_BACK:
			return "/" + Page.SERVER_FOLDER + "assets/dir_back.png";
		case Page.PHP_FILE:
			return "/" + Page.SERVER_FOLDER + "assets/php.png";
		case Page.TXT_FILE:
		case Page.OTHER_FILE:
			return "/" + Page.SERVER_FOLDER + "assets/file.png";
		case Page.AWESOME_FILE:
			return "/" + Page.SERVER_FOLDER + "assets/awesome.png";
		} 
		return null;
	}

	public String getHeader(int fileType) {
		switch (fileType) {
		case Page.PNG_FILE:
			return Page.HEADER_PNG;
		case Page.JPEG_FILE:
			return Page.HEADER_JPG;
		case Page.CSS_FILE:
			return Page.HEADER_CSS;
		case Page.JAVASCRIPT_FILE:
		case Page.JAVA_FILE:
		case Page.PHP_FILE:
			return Page.JAVASCRIPT_HEADER;
		case Page.DIRECTORY:
		case Page.TXT_FILE:
		case Page.OTHER_FILE:
		case Page.HTML_FILE:
			return Page.HEADER_HTML;
		case Page.ZIP_FILE:
			return Page.HEADER_ZIP;
		case Page.PDF_FILE:
			return Page.HEADER_PDF;
		}
		return null;
	}

	/**
	 * Returns the absoluth path relative to the server's relative path
	 * 
	 * @return
	 */
	public String getAbsolutPath() {
		File file = getIndexPage();
		String absPath = file.getAbsolutePath().replaceAll(
				Page.SERVER_FOLDER + file.getName(), "");
		// System.out.println(absPath);
		return absPath;
	}

	/**
	 * Gets the "Access Denied Page"
	 * 
	 * @return
	 */
	public File getAccessDenied() {

		return new File(Page.SERVER_FOLDER + Page.PAGE_DENIED);
	}
}
