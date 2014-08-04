package LinkCrawling;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generates a list of tweetIDs and URLs from a given corpus of tweets.
 * 
 * Each tweetID is written to an output file line, followed the URL in that
 * tweet (space separated). If a tweetID has multiple URLs, there will be one
 * line for each tweetID/URL pair.
 * 
 * @author David Purcell v1.0
 * @edited Lauren Mathews v1.5
 * @version 6/2011 v1.0
 * @version 7/10/2014
 */
public final class URLListGen {
	/**
	 * A regular expression for matching URLs.
	 * 
	 * The regex matches URLs with standard "<i>http://</i>", "<i>https://</i>",
	 * "<i>www.</i>", "<i>ftp://</i>", and "<i>file://</i>" protocols.
	 * 
	 * The regex does <strong>not</strong> match URLs like
	 * "<i>youtube.com/example</i>".
	 */
	public static final String URL_REGEX = "\\b((https?|ftp|file)://|"
			+ "(www|ftp)\\.)[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*"
			+ "[-a-zA-Z0-9+&@#/%=~_|]";

	/**
	 * Generates a tweetID/URL listing from a given corpus. The first parameter
	 * is the name of the corpus to read (output.csv)
	 * 
	 * @param inputFile
	 *            output.csv, created by Results.java
	 */
	public static HashMap<String, String> createUrlList(String inputFile)
			throws IOException {
		// checks arguments
		if (!inputFile.contains(".csv")) {
			System.err
					.println("In URLListGen.java (in createUrlList). inputFile doesn't equal output.csv, which is what Results.java makes.");
			System.err.println("inputName: " + inputFile);
			System.exit(0);
		}

		// will contain the list of tweetIDs and urls
		HashMap<String, String> urlList = new HashMap<String, String>();

		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(inputFile));
		} catch (FileNotFoundException ex) {
			System.err
					.println("In URLListGen.java (in createUrlList). Something went wrong with reading the file. ERROR: "
							+ ex.getMessage());
			System.exit(0);
		}

		// starts with first line of tweets
		String line = br.readLine();
		
		if(line.startsWith("Run")){
			line = br.readLine();
			line = br.readLine();
			line = br.readLine();
		}

		// gets the pattern recogniztion program ready
		Pattern urlPattern = Pattern.compile(URLListGen.URL_REGEX);

		// goes through each line of the file
		while (line != null) {
			// skips over lines that are empty
			if (line.isEmpty() || line.equals("")) {
				line = br.readLine();
				continue;
			}

			// tokenizes the line
			StringTokenizer sT = new StringTokenizer(line, ",");

			// grabs the tweetID and topicNum; tweet always third column in
			String curTok = sT.nextToken();
			String topicNum = curTok;
			curTok = sT.nextToken();
			String tweetID = curTok;
			curTok = sT.nextToken();

			// checks for any urls in tweet
			Matcher matcher = urlPattern.matcher(curTok);

			// find urls in tweet
			while (matcher.find()) {
				String url = matcher.group();

				// this website never downloads well
				if (url.contains("reuters")) {
					break;
				}

				// adds to list
				urlList.put(topicNum + " " + tweetID, url);
			}

			line = br.readLine();
		}

		br.close();

		return urlList;
	}

	/**
	 * Returns whether or not the specified text contains a URL. Valid URLs are
	 * specified by the {@linkplain #URL_REGEX} pattern.
	 * 
	 * @param text
	 *            The text to check for URLs.
	 * @return <code>true</code>, if the text contains one or more URLs.
	 * @see #URL_REGEX
	 */
	public static boolean containsUrl(String text) {
		Pattern urlPattern = Pattern.compile(URLListGen.URL_REGEX);
		Matcher matcher = urlPattern.matcher(text);

		return matcher.find();
	}

	/**
	 * Removes all urls from the string.
	 * 
	 * @param text
	 *            The text to check for URLs.
	 * @return The text without the urls.
	 */
	public static String removeURLs(String text) {
		if (containsUrl(text)) {
			Pattern urlPattern = Pattern.compile(URLListGen.URL_REGEX);
			Matcher matcher = urlPattern.matcher(text);

			while (matcher.find()) {
				String url = matcher.group();

				text = text.replace(url, "");
			}

			return text;
		} else {
			return text;
		}
	}

	/**
	 * Returns all the urls found in the string.
	 * 
	 * @param text
	 *            The text to check for URLs.
	 * @return Returns list of urls, null otherwise
	 */
	public ArrayList<String> returnURLs(String text) {
		if (containsUrl(text)) {
			ArrayList<String> urls = new ArrayList<String>();

			Pattern urlPattern = Pattern.compile(URLListGen.URL_REGEX);
			Matcher matcher = urlPattern.matcher(text);

			while (matcher.find()) {
				String url = matcher.group();

				urls.add(url);
			}

			return urls;
		} else {
			return null;
		}
	}
}