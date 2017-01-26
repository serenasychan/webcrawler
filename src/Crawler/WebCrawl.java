package Crawler;

/*
 *  This web crawler searches through metrolyrics.com and downloads lyrics.
 *  Stores visited pages in mysql database `Crawler`
 *  table `Record` with column: URL
 *  table `Song` with columns: Artist, Title, FileName
 * 
 * @author 	Serena Chan
 * @version 1.0
 * @since	2017-01-25
 */

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class WebCrawl 
{
	public static String start = "http://www.metrolyrics.com/top-artists.html";
	public static String baseURL = "metrolyrics.com";
	public static String target = "printlyric";
	public static String[] items = {".jpg", ".jpeg", "add.html?", "?ModPagespeed=noscript"};
	static LinkedList<String> 	todo = new LinkedList<String>();
	public static int count = 0;
	
	public static DB db = new DB();
	
	/**
	 * This method is used to get the HTML at a given URL
	 * @param urlString A URL string
	 * @return The contents (HTML) at that URL
	 */
	public static String readURLContent(String urlString) 
	{
		try 
		{
			URL url = new URL(urlString);
			Scanner scan = new Scanner(url.openStream());

			String content = new String();
			while (scan.hasNext())
				content += scan.nextLine();
			scan.close();
			
			return content;
		} catch (IOException e) {
			return null;
		}
	}
	
	/**
	 * This method returns the song title and artist from a metrolyrics printlyric page
	 * @param doc Jsoup doc of a URL
	 * @return String containing song title and artist
	 */
	public static String findTitle(Document doc)
	{
		String title = doc.select("h1").first().ownText();
		String artist = doc.select("h2").first().ownText();
		return title + " " + artist;
	}
	
	/**
	 * Goes through each URL in the specified.
	 * Unless the domain is small, it will not stop
	 * @param order Choose to use depth first search, breadth first search, or random
	 * @throws SQLException
	 * @throws IOException
	 */
	public static void search(String order) throws SQLException, IOException
	{
		String urlStr;
		while (todo.size() > 0)
		{    
        	if(order == "depth")
        		urlStr = todo.removeLast();
        	else if(order == "breadth")
        		urlStr = todo.removeFirst();
        	else
        		urlStr = todo.remove((int) (Math.random() *  todo.size()));
			
			//check if the given URL is already in database
			String sql = "select * from Record where URL = '"+urlStr+"'";
			ResultSet rs = db.runSql(sql);
			if(rs.next()){
	 
			}else{
				//store the URL to database to avoid parsing again
				sql = "INSERT INTO Crawler.Record (url) VALUES(?)";
				PreparedStatement stmt = db.conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
				stmt.setString(1, urlStr);
				stmt.execute();

				count++;
				String content = readURLContent(urlStr);
				if (content == null)
					continue;
				System.out.print((count) + "(" + todo.size() + "):\t" + urlStr + "\t\t");
				Document doc = Jsoup.parse(content);
				if(urlStr.contains(target))
				{
					String title = findTitle(doc);
					System.out.println(title);
					saveLyrics(urlStr, doc);
				}
				System.out.println();
				
				addLinks(doc);
			}
		}
	}
	
	/**
	 * Checks that the page is valid (can be used to find more links)
	 * @param urlStr A URL string
	 * @return True if it doesn't contain substrings in items, False otherwise
	 */
	public static boolean isAValidPage(String urlStr) 
	{
	    for(int i=0; i < items.length; i++)
	    {
	        if(urlStr.contains(items[i]))
	        {
	            return false;
	        }
	    }
	    return true;
	}
	
	/**
	 * This method finds all valid links in the webpage and adds them
	 * to the queue.
	 * @param doc JSoup contents of webpage
	 */
	public static void addLinks(Document doc)
	{
		//get all links and recursively call the processPage method
		Elements questions = doc.select("a[href]");
		for(Element link: questions){
			String newURL = link.attr("href");
			
			if(newURL.contains(baseURL) && isAValidPage(newURL))
				if(newURL.contains(target))
					todo.offerFirst(newURL);
				else
					todo.add(newURL);
		}
	}
	
	/**
	 * Saves the lyrics in a file if it's a printlyric page
	 * Stores song artist, title, filename into Crawler.Song table
	 * @param URL A URL string
	 * @param doc JSoup document with contents of web page
	 * @throws IOException
	 * @throws SQLException
	 */
	public static void saveLyrics(String URL, Document doc) throws IOException, SQLException
	{
		if(URL.contains(target) && URL.endsWith("html"))
		{
			
			String[] splitedURL = URL.split("/");
			String fileName = splitedURL[splitedURL.length-1];
			System.out.println(count+ "\t" +fileName);
			
			String title = doc.select("h1").first().ownText();
			title = title.substring(0, title.length()-7);
			String artist = doc.select("h2").first().ownText();
			artist = artist.substring(3);

			String sql = "INSERT INTO Crawler.Song (artist, title, filename) VALUES(?,?,?)";
			PreparedStatement stmt = db.conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			stmt.setString(1, artist);
			stmt.setString(2, title);
			stmt.setString(3, fileName);
			stmt.execute();
			
			BufferedWriter writer = new BufferedWriter(new FileWriter("./lyrics/"+fileName));
			writer.write(title + " " + artist);
			writer.newLine();
			Elements verses = doc.select("p.verse");
			for(Element verse : verses){
				writer.write(" "+verse.text());
			}
			writer.close();
		}
	}
	
	public static void main(String[] args) throws IOException, SQLException 
	{		
		//Empty database
		//db.runSql2("TRUNCATE Record;");
		//db.runSql2("TRUNCATE Song;");
		
		todo.add(start);
		//search("depth");
		search("breadth");
		//search("random");
	}
}