package LinkCrawling;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.cybozu.labs.langdetect.DetectorFactory;
import com.cybozu.labs.langdetect.LangDetectException;

import API.APISearch;

/**
 * Takes a file with the Tweet ID and url(s) which are on the same line and gets
 * the content of that url. This content with the Tweet ID is then sent to a new
 * file. Urls that cannot be read will be sent to a error file with the
 * associated Tweet ID.
 * 
 * Version 1.0: This is a stand-alone file and should be run before TM1 is run
 * (in Stirs.java).
 * 
 * @author Karl Appel v1.0
 * @author Lauren Mathews v1.5
 * @version 6/22/11 v1.0
 * @version 7/10/14 v1.5
 */
public class UrlContentRetrieval {
	@SuppressWarnings("rawtypes")
	public static void main(String[] args) {
		// this should be changed to whatever computer it's running on
		String harddrive = "/home/lmathews/workspace/TREC2014";

		System.out.println("Creating URLListGen...");

		@SuppressWarnings("resource")
		Scanner reader = new Scanner(System.in);
		System.out
				.println("What year's collection (2011, 2012, 2013, or 2014) are you crawling?");
		String ans = reader.nextLine();

		String content = harddrive + "/src/LinkCrawling/contentFile" + ans
				+ ".txt";
		String outputcsv = harddrive + "/output.csv";
		String outputcsvSplit = harddrive + "/output-split.csv";

		// if the retrieval hits an unexpected error (like infinite wait time),
		// stop the program and restart it with the right number + 1.
		// Default is 1
		int indexStart = 73153;

		// grabs the urlList from output.csv
		HashMap<String, String> urlList = null;
		try {
			urlList = URLListGen.createUrlList(outputcsv);
		} catch (IOException e2) {
			System.err
					.println("In UrlContentRetrieval.java. Something went wrong creating the urlList. ERROR: "
							+ e2.getMessage());
			System.exit(0);
		}

		System.out.println("Finished creating URLListGen (regular). Size: "
				+ urlList.size());

		// grabs the urlList from output.csv
		HashMap<String, String> urlListSplit = null;
		try {
			urlListSplit = URLListGen.createUrlList(outputcsvSplit);
		} catch (IOException e2) {
			System.err
					.println("In UrlContentRetrieval.java. Something went wrong creating the urlList. ERROR: "
							+ e2.getMessage());
			System.exit(0);
		}

		// creates list of error urls (don't need to try grabbing them again)
		HashSet<String> errorURLS = new HashSet<String>();

		System.out.println("Finished creating URLListGen-split. Size: "
				+ urlListSplit.size());

		Iterator it = urlListSplit.entrySet().iterator();
		Map.Entry pairs2;
		while (it.hasNext()) {
			pairs2 = (Map.Entry) it.next();
			urlList.put((String) pairs2.getKey(), (String) pairs2.getValue());
		}

		System.out.println("Finished creating URLListGen. Size: "
				+ urlList.size());

		// used for detecting language
		String profileDirectory = harddrive + "/src/API/profiles";

		try {
			DetectorFactory.loadProfile(profileDirectory);
		} catch (LangDetectException e) {
			System.err
					.println("In UrlContentRetrieval. Something went wrong making the profile directory. ERROR: "
							+ e.getMessage());
			System.err.println("Profiles' Index: " + profileDirectory);
			System.exit(0);
		}

		String contentFileLocation = content;

		File contentFile = new File(contentFileLocation);

		// if these files already exist delete them
		Boolean contentFileExist = contentFile.exists();

		if (contentFileExist) {
			contentFile.delete();
		}

		PrintWriter contentFileWriter = null;
		try {
			contentFileWriter = new PrintWriter(contentFile);
		} catch (FileNotFoundException e1) {
			System.err
					.println("In UrlContentRetrieval.java. Something went wrong with creating the content/error files. ERROR: "
							+ e1.getMessage());
			System.exit(0);
		}

		// if we need to go to a certain starting point
		int urlsProcessed = 0;
		int urlsCanUse = 50988;

		System.out.println("Downloading the URLs...");

		// goes through all the urls and tries to retrieve their content
		it = urlList.entrySet().iterator();

		// if we are not starting from the beginning, this runs
		// to the right starting index
		Map.Entry pairs;
		while (it.hasNext() && urlsProcessed != indexStart) {
			pairs = (Map.Entry) it.next();
			urlsProcessed++;
		}

		// goes through each line of the HashMap
		while (it.hasNext()) {
			pairs = (Map.Entry) it.next();

			// grabs each url
			String url = (String) pairs.getValue();

			if (url.startsWith("www.")) {
				url = "http://" + url;
			}

			if (!errorURLS.contains(url) && url != null) {
				String urlPageContent = null;
				Document doc = null;
				// gets content from url
				try {
					// updates the url to long form
					URL urlAddress = new URL(url);
					HttpURLConnection connection = (HttpURLConnection) urlAddress
							.openConnection(Proxy.NO_PROXY);
					connection.setInstanceFollowRedirects(false);
					connection.connect();
					url = connection.getHeaderField("Location");
					connection.getInputStream().close();

					// filters out unwanted urls
					if (url == null || url.contains("reuters")) {
						errorURLS.add(url);

						System.err.println("Url is bad (url: " + url
								+ ")! - Urls Processed: " + urlsProcessed);
						urlsProcessed++;
						continue;
					}

					// gets the url content
					doc = Jsoup.connect(url).get();
					if (doc.hasText()) {
						// converts the content from html to text
						urlPageContent = Jsoup.parse(doc.body().toString())
								.text();

						String lang = APISearch.EnglishFilter(urlPageContent,
								"");

						// checks for english sites only
						if (!lang.equalsIgnoreCase("en")) {
							System.err.println("ERROR (lang: " + lang
									+ ")! url: " + url);

							errorURLS.add(url);

							System.err.println("(Site is " + lang
									+ ") - Urls Processed: " + urlsProcessed);
							urlsProcessed++;
							continue;
						}
					}
				}

				// if the information cannot be retrieved the Tweet ID and url
				// are
				// printed in a error file
				catch (Exception e) {
					System.err.println("ERROR (url: " + url + "): "
							+ e.getMessage());

					errorURLS.add(url);
				}

				// if the information is retrievable the tweet id and url
				// content
				// are placed in a content folder
				if (urlPageContent != null && !urlPageContent.isEmpty()) {
					String javascript = "This site requires JavaScript and Cookies to be enabled. Please change your browser settings or upgrade your browser.";
					if (!(urlPageContent.equalsIgnoreCase(javascript) || urlPageContent
							.contains("| Reuters"))) {
						contentFileWriter.println(pairs.getKey() + " " + url);

						if (!doc.title().isEmpty()) {
							contentFileWriter.println(doc.title());
						} else {
							contentFileWriter.println("TITLEBLANK");
						}

						contentFileWriter.println(urlPageContent.trim());
						contentFileWriter.flush();

						urlsCanUse++;
					} else {
						System.err
								.println("Bad url (Javascript/Reuters). Url: "
										+ url);
					}
				}
			}

			// prints out how many urls have been processed after each is
			// processed
			System.out.println("Urls Processed: " + urlsProcessed);
			System.out.println("Urls Can Use: " + urlsCanUse);
			urlsProcessed++;
		}

		System.out.println("Finished downloading the URLs.");

		contentFileWriter.close();

		// System.out.println("REMEMBER! Each time someone uses the URL Index, TwitterIndexerUrls must be called. WAS NOT CALLED HERE!");
	}
}