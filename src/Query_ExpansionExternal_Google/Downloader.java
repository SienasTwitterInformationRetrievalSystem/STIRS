package Query_ExpansionExternal_Google;

import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.StringTokenizer;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import API.LuceneQuery;
import Main.QueryExpansion;
import Miscellaneous.POSTagger;

/**
 * Version 2.0: Main class for Google External Query Expansion Goes to google
 * and grabs first page of links for each topic (between a certain date)
 * Retrieves the content from top 6 pages (now 6, not 4, because of possibility
 * of bad links in earlier dates) Strips content of "bad" words and
 * non-alphabet/non-numeric words Counts up total of all words for each topic;
 * grabs top 4 Sends back words that appear more than once across links for each
 * topic
 * 
 * @author Chan Tran v1.0
 * @author Lauren Mathews v2.0
 * @version 6/2011 v1.0
 * @version 6/24/2014 v2.0
 */
public class Downloader implements QueryExpansion {

	private GoogleExpansion ge = null;
	private String mostCommon = null, stopWordsColl = null, modelFile = null,
			googleContent = null;
	private boolean updateContent = false;
	public ArrayList<String> newQueries = new ArrayList<String>();
	private int urlsProcessed = 1, amount = 0, atTop = 1;

	/**
	 * Main constructor for the Google Expansion
	 * 
	 * @param harddrive
	 *            The main harddrive location
	 * @param model
	 *            en-pos-maxent.bin for POSTagger
	 * @param stopWords
	 *            englishStop.txt for stop words
	 * @param update
	 *            Whether to update the link content
	 * @param content
	 *            Where to put all the link content
	 */
	public Downloader(String harddrive, String model, String stopWords,
			boolean update, String content) {
		ge = new GoogleExpansion();

		String mF = harddrive.substring(0, harddrive.indexOf("TREC2014"));

		this.modelFile = mF + model;
		this.updateContent = update;
		this.stopWordsColl = harddrive + "/src/Query_ExpansionInternal/"
				+ stopWords;
		this.googleContent = harddrive + content;

		System.err
				.println("(In/For Downloader.java, for Google External Query Expansion)\nDo you need to update the link content (for topics)? Because updateContent is: "
						+ update);

		if (update) {
			System.err
					.println("In/For Downloader.java, for Google External Query Expansion) Don't forget to change the date settings for google searching (in setGoogleLinks).");
			System.err
					.println("In setGoogleLinks.java. Do you need to update the 'advanced search' options?");
		}
	}

	/**
	 * If the expanded query returns more than 4 words, tries to narrow them
	 * down to only nouns.
	 * 
	 * @param expanded
	 *            The common words found within the link
	 * @return The new string with only noun words
	 */
	private String usePOS(String expanded) {
		String newList = "";

		POSTagger tagger = null;
		// only if we're not using external data
		// Creates an instance of the POSTagger class, for finding nouns
		tagger = new POSTagger();
		tagger.initializeTagger(modelFile);

		// words in expanded
		ArrayList<String> wordsInTop = new ArrayList<String>();

		// goes through all the words and separates them nicely
		StringTokenizer sT = new StringTokenizer(expanded);

		String curTok;
		while (sT.hasMoreTokens()) {
			curTok = sT.nextToken();

			wordsInTop.add(curTok);
		}

		// Only writes those that are nouns
		for (int l = 0; l < wordsInTop.size(); l++) {

			String word = wordsInTop.get(l);

			// makes sure to have hashtags and retweets work with POS
			if (word.contains("#") || word.contains("@")) {
				word = word.substring(1);
			}

			String[] tags = tagger.findTags(word);

			String speechCodes = "NN NNS NNP NNPS";

			StringTokenizer sTnoun = new StringTokenizer(speechCodes, " ");
			String nounInput = sTnoun.nextToken();

			// goes through each of the speech codes
			while (sTnoun.hasMoreTokens()) {
				// if the word is a noun
				if (tags[0].equals(nounInput)) {
					// Updates the newQuery line for each loop
					newList = newList + " " + wordsInTop.get(l);
				}

				nounInput = sTnoun.nextToken();
			}
		}

		return newList;
	}

	/**
	 * Main function of the class
	 * 
	 * @param queries
	 *            The topic queries created by QueryProcessor.java in Stirs.java
	 */
	public ArrayList<String> getNewQueries(ArrayList<LuceneQuery> queList) {
		System.out.println("Starting Main...");
		ArrayList<LuceneQuery> queries = queList;

		amount = queries.size() * 6;
		
		HashMap<String, String> months = new HashMap<String, String>();

		months.put("Jan", "1");
		months.put("Feb", "2");
		months.put("Mar", "3");
		months.put("Apr", "4");
		months.put("May", "5");
		months.put("Jun", "6");
		months.put("Jul", "7");
		months.put("Aug", "8");
		months.put("Sep", "9");
		months.put("Oct", "10");
		months.put("Nov", "11");
		months.put("Dec", "12");

		try {
			ge = new GoogleExpansion();

			// goes through each query and finds common words
			for (int j = 0; j < queries.size(); j++) {
				atTop = j;
				
				// creates the query with "+" instead of " "
				String gquery = gQC(queries.get(j).getQuery());

				// only need to update the link content if new topics
				if (updateContent) {
					System.out.println("Topic: " + queries.get(j).getQueryNum());
					
					ArrayList<Link> links = setGoogleLinks(gquery,
							queries.get(j).getQueryTime(), months);

					getArticles(links, j, queries);
				}

				// finds the common words for each link content
				findMostCommon(gquery, j, queries);

				// grabs the expanded query
				String expanded = ge.compare(mostCommon);

				// checks if POS is needed
				if (!expanded.isEmpty() && expanded.contains(" ")) {

					StringTokenizer sT = new StringTokenizer(expanded);

					int count = 0;
					while (sT.hasMoreTokens()) {
						count++;

						sT.nextToken();
					}

					// only if there are so many words (to narrow them down)
					if (count >= 5) {
						expanded = usePOS(expanded);
					}
				}

				// adds to list
				newQueries.add(expanded.trim());
				mostCommon = "";
			}
		} catch (Exception e) {
			System.err
					.println("In Downloader.java (in getNewQueries). Something happened when creating new queries. ERROR: "
							+ e.getMessage());
			System.err.println("Topic Num: " + queries.get(atTop).getQueryNum());
			System.err.println("Topic Query: " + queries.get(atTop).getQuery());
			System.exit(0);
		}

		System.out.println("Finished Main.");

		return newQueries;
	}

	/**
	 * Grabs the articles from each of the links found for each topic
	 * 
	 * @param links
	 *            The links gathered from google (for each topic)
	 * @param queryNum
	 *            The current query number
	 */
	private void getArticles(ArrayList<Link> links, int queryNum,
			ArrayList<LuceneQuery> queries) throws IOException,
			InterruptedException {
		int numFetch = 6;

		String urlPageContent = null;

		// updates the file to write content to (1 for each topic)
		String index = googleContent + "/MB"
				+ queries.get(queryNum).getQueryNum() + ".txt";

		BufferedWriter bfw = null;
		try {
			bfw = new BufferedWriter(new FileWriter(index));
		} catch (IOException e1) {
			System.err
					.println("In Downloader.java (in getArticles). Was unable to write to file. ERROR: "
							+ e1.getMessage());
			System.exit(0);
		}

		// will contain content
		// this is for making sure we don't get the same content twice
		HashSet<String> content = new HashSet<String>();

		// grabs the necessary amount (numFetch) of link content
		for (int i = 0; i < numFetch; i++) {

			// if we still need links, but all links done, breaks
			if (i >= links.size()) {
				break;
			}

			// we want only links that are content
			if (links.get(i).getUrl().contains("video")
					|| links.get(i).getUrl().contains("youtube")) {
				numFetch++;
				continue;
			}

			Document doc = null;
			// gets content from url
			try {
				// gets the url content
				doc = Jsoup.connect(links.get(i).getUrl()).get();
				if (doc.hasText()) {
					// converts the content from html to text
					urlPageContent = Jsoup.parse(doc.body().toString()).text();
				}

				// bad page content
				if (urlPageContent
						.equalsIgnoreCase("This site requires JavaScript and Cookies to be enabled. Please change your browser settings or upgrade your browser.")) {
					numFetch++;
					continue;
				} else if (urlPageContent != null && !urlPageContent.isEmpty()
						&& !content.contains(urlPageContent)) {
					content.add(urlPageContent);

					bfw.write(links.get(i).getUrl());
					bfw.newLine();
					bfw.write(urlPageContent);
					bfw.newLine();
					bfw.flush();
				}
			} catch (Exception e) {
				System.err.println("In Downloader.java (in getArticles). ERROR: " + e.getMessage());
				numFetch++;
			}

			System.out.println(urlsProcessed + " (~" + amount + ")");
			urlsProcessed++;
		}
		bfw.close();
	}

	/**
	 * Uses GoogleExpansion.java to find most common words for each topic
	 * 
	 * @param query
	 *            The current query topic
	 * @param queryNum
	 *            The current query number
	 */
	private void findMostCommon(String query, int queryNum,
			ArrayList<LuceneQuery> queries) {
		// grabs the index (1 for each topic)
		String index = googleContent + "/MB"
				+ queries.get(queryNum).getQueryNum() + ".txt";

		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(index));
		} catch (FileNotFoundException e) {
			System.err
					.println("In Downloader.java (in findMostCommon). Something happened when the index file was created.");
			System.exit(0);
		}

		String doc = "";
		try {
			// goes through each line of the file
			String line = br.readLine();
			String url = line;
			outer: while (line != null) {
				line = br.readLine();

				line = line.replace("\t", "");
				line = line.replace("(", "");
				line = line.replace(")", "");
				line = line.replace(":", "");
				line = line.replace(";", "");
				line = line.replace("U.", "US");
				line = line.replace("}", "");
				line = line.replace("{", "");

				doc = line;

				line = br.readLine();

				if (line == null) {
					break;
				}

				// for grabbing content that spans multiple lines
				while (!line.startsWith("http")) {
					line = line.replace("\t", "");
					line = line.replace("(", "");
					line = line.replace(")", "");
					line = line.replace(":", "");
					line = line.replace(";", "");
					line = line.replace("U.", "US");
					line = line.replace("}", "");
					line = line.replace("{", "");

					doc += line;

					line = br.readLine();

					if (line == null) {
						break outer;
					}
				}

				mostCommon += ge.mostCommon(doc, query, url, stopWordsColl)
						+ " ";

				if (mostCommon.isEmpty()) {
					System.err
							.println("In Downloader.java (in findMostCommon). mostCommon is empty.");
					System.err.println("query: " + query);
					System.exit(0);
				}

				url = line;
			}

			br.close();
		} catch (IOException e) {
			System.err
					.println("In Downloader.java (in findMostCommon). ERROR: "
							+ e.getMessage());
		}
	}

	/**
	 * Creates the Google link for each topic and grabs the urls from the first
	 * page of Google
	 * 
	 * @param query
	 *            The current query
	 * @return An arraylist of type Link that contains all the links found on
	 *         first page
	 */
	public static ArrayList<Link> setGoogleLinks(String query,
			String queryTime, HashMap<String, String> months)
			throws IOException {
		ArrayList<Link> links = new ArrayList<Link>();

		String year = "2013";
		String month1 = "2";
		String day1 = "1";
		String month2 = "";
		String day2 = "";

		StringTokenizer sT = new StringTokenizer(queryTime);

		String curTok;
		for (int i = 0; i < 6; i++) {
			curTok = sT.nextToken();

			if (i == 1) {
				month2 = months.get(curTok);
			}

			if (i == 2) {
				if (curTok.startsWith("0")) {
					day2 = curTok.substring(1);
				} else {
					day2 = curTok;
				}
			}

			if (i == 5) {
				year = curTok;
			}
		}

		// this is for special searching between dates
		String advancedSearch = "&biw=1408&bih=692&source=lnt&tbs=cdr%3A1%2Ccd_min%3A"
				+ month1
				+ "%2F"
				+ day1
				+ "%2F"
				+ year
				+ "%2Ccd_max%3A"
				+ month2 + "%2F" + day2 + "%2F" + year + "&tbm=";

		// creates the url
		String googleStart = "http://www.google.com/search?hl=en&q=";
		String googleEnd = "&btnG=Google+Search";
		String googleQuery = googleStart + query + advancedSearch + googleEnd;

		// the html content of Googling the query
		String googlePage = getURL(googleQuery);

		boolean reachedEnd = false;
		int linkStartIndex, linkEndIndex = 0;

		// we want to start looking for links after "About # results"
		int startIndex = googlePage.indexOf("results");

		if (startIndex < 0) {
			// System.err.println("In Downloader.java (in setGoogleLinks). Could not find 'results' in string.");

			return null;
		}

		// want to ignore sponsored links
		String checkSponsored = googlePage.substring(startIndex,
				googlePage.indexOf("<p>", startIndex));

		// sets the google page to where "About # results" ends
		String numResults = null;
		if (checkSponsored.indexOf("About") > 0
				&& checkSponsored.lastIndexOf("results") + 7 > checkSponsored
						.indexOf("About")) {
			numResults = checkSponsored.substring(
					checkSponsored.indexOf("About"),
					checkSponsored.lastIndexOf("results") + 7);
		} else if (checkSponsored.contains("did not match any documents.")) {
			return null;
		}

		if (numResults != null && googlePage.indexOf(numResults) > 0) {
			googlePage = googlePage.substring(googlePage.indexOf(numResults));
		} else {
			return null;
		}

		// want to ignore sponsored links
		if (checkSponsored.contains("Sponsored Links")) {
			System.err.println("Sponsored Found!");
			startIndex = googlePage.indexOf("<p>", startIndex);
			int startIndex2 = googlePage.indexOf("</table></p><p>");
			startIndex = Math.max(startIndex, startIndex2);
		}

		// google page only has 10 links
		int count = 0;

		outer: while (!reachedEnd && count < 10) {

			// helps find the link
			linkStartIndex = googlePage.indexOf("<a href=");
			linkEndIndex = googlePage.indexOf("&amp;", linkStartIndex);
			String link = googlePage.substring(linkStartIndex, linkEndIndex);

			// updates the page past the whole link
			googlePage = googlePage.substring(linkEndIndex + 5);

			// special links only added
			if (!link.contains("webcache") && !link.contains("q=related")
					&& !link.contains("news?q=") && !link.contains("/search")
					&& !link.contains("youtube") && !link.contains("video")
					&& !link.contains("<table cellpadding")
					&& !link.contains("/imgres") && !link.contains("/images")
					&& !link.contains(".pdf")) {

				link = link.replaceAll("<a href=\"", "");
				link = link.replaceAll("/\"", "/");
				link = link.replaceAll("/url\\?q=", "");
				link = link.replaceAll("%3D", "=");
				link = link.replaceAll("%3F", "?");
				link = link.replaceAll("%26", "&");
				link = link.replaceAll("%2B", "+");

				// these either are false links or ending
				if (link.contains("/language_tools?hl=en")) {
					break;
				} else if (link.contains("/aclk?sa=")) {
					continue;
				} else if (link.contains("reuters")) {
					continue;
				}

				// special Link data structure class
				Link newLink = new Link(link, null);

				if (newLink.getUrl().equals("null")) {
					continue;
				}

				// goes through and makes sure none of the links
				// have been found twice
				for (int k = 0; k < links.size(); k++) {
					if (links.get(k).getUrl().equals(link)) {
						continue outer;
					}
				}

				// adds to arraylist
				links.add(newLink);

				// updates link amount found
				count++;
			}
		}
		return links;
	}

	/**
	 * Grabs the HTML of the Google page (for each query)
	 * 
	 * @param url
	 *            The full Google url for topic
	 * @return The HTML content as a string
	 */
	private synchronized static String getURL(String url) {
		StringBuffer text = new StringBuffer(1024);

		try {
			URL u = new URL(url);
			URLConnection con = u.openConnection();

			// to fool google and any other page that might not like to be
			// queried by an application
			con.setRequestProperty("User-Agent",
					"Mozilla/4.0 (compatible; MSIE 5.5; Windows NT 5.0; H010818)");

			BufferedReader urlrdr = new BufferedReader(new InputStreamReader(
					con.getInputStream()));
			int c;
			while ((c = urlrdr.read()) != -1) {
				text.append((char) c);
			}
		} catch (Exception e) {
			System.err
					.println("In Downloader.java (in getURL). Something went wrong. ERROR: "
							+ e.getMessage());
			System.err.println("url: " + url);

			System.exit(0);
		}
		return text.toString();
	}

	/**
	 * gQC, or googleQueryConverter, converts regular queries to Google-friendly
	 * queries. Spaces are changed to +, and characters are encoded.
	 */
	public static String gQC(String query) {
		query = query.replaceAll(" ", "+");
		return query;
	}
}