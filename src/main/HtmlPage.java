package main;

public class HtmlPage {
	
	private String headerContent;
	private String footContent;
	private String content;
	private String headerTitle;
	
	public HtmlPage() {
		
		headerContent = "<!DOCTYPE html><html><head><link rel=\"stylesheet\" type=\"text/css\""
				+ " href=\"/ServerFolder/style.css\"> <title> Marcus Gabilheri Webserver</title> </head> <body>";
		
		footContent = "</div></body></html>";
		
	}
	
	public void setContent(String content) {
		this.content = content;
	}
	
	public String getContent() {
		return content;
	}
	
	public String getFootContent() {
		return footContent;
	}
	
	public String getHtmlPage() {
		
		String htmlPage = headerContent;
		htmlPage += getContent();
		htmlPage += getFootContent();
		return htmlPage;

	}
}
