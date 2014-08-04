package Relevance;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import API.LuceneQuery;
import API.RankedTweetList;
import API.Tweet;
import LinkCrawling.URLListGen;
import Main.Module;
import Query_ExpansionExternal_Google.Downloader;
import Query_ExpansionExternal_Google.Link;
import Relevance.GoogleInfo;

/**
 * Grabs a list of links found for each topic. Grabs a list of links found for
 * each tweet of that topic. Compares and adjusts tweet's score for matching
 * sites.
 * 
 * @author Lauren Mathews v1.0
 * @version 7/7/2014 v1.0
 */
public class Google implements Module {

	private static final float DEFAULT_GOOGLE_BONUS = 0.3f;
	private int num_amount = 0, num_to_scrape = 10;
	private String harddrive;
	private boolean createTextFile = false;

	/**
	 * Constructor for Google.java
	 * 
	 * @param hD
	 *            C:\\Users\\Lauren\\workspace\\TREC2014 Used for finding
	 *            Links.txt
	 * @param createText
	 *            Whether to re-create the text file.
	 */
	public Google(String hD, boolean createText) {
		this.harddrive = hD;
		this.createTextFile = createText;
	}

	/**
	 * Goes through and gathers the top 10 links for top 10 tweets of each
	 * topic. If those links are the same (any link) for that tweet and topic,
	 * adds to score.
	 */
	public ArrayList<ArrayList<RankedTweetList>> getResults(
			ArrayList<LuceneQuery> queries,
			ArrayList<ArrayList<RankedTweetList>> rankedTweetLists) {

		System.err
				.println("In setGoogleLinks.java. Do you need to update the 'advanced search' options?");

		HashMap<String, String> months = new HashMap<String, String>();

		// used for google link (see Downloader.java)
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

		System.out.println("Making query links list...");

		ArrayList<GoogleInfo> queryInfo = new ArrayList<GoogleInfo>();

		// goes through each topic and gathers there links from google
		for (int j = 0; j < queries.size(); j++) {
			String gquery = Downloader.gQC(queries.get(j).getQuery());

			ArrayList<Link> links = null;
			try {
				// grabs the links from google
				links = Downloader.setGoogleLinks(gquery, queries.get(j)
						.getQueryTime(), months);
			} catch (IOException e) {
				System.err
						.println("In Google.java (in getResults). Something occurred when grabbing the query's links.");
				System.exit(0);
			}

			System.out.println("TopicNum: " + "MB"
					+ queries.get(j).getQueryNum());

			queryInfo.add(new GoogleInfo("MB" + queries.get(j).getQueryNum(),
					queries.get(j).getQueryTime(), links));

			// prevents Google from blocking us from querying them too much
			try {
				Thread.sleep(5000);
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		}

		System.out.println("Finished making query links.");

		if (createTextFile) {
			System.out.println("Starting to make text file of tweetInfo...");
			createTextFile(rankedTweetLists, months, queryInfo);
			System.out.println("Finished making text file.");
		}

		System.out.println("Starting to gather links for tweets...");

		HashMap<String, ArrayList<Link>> tweetInfo = new HashMap<String, ArrayList<Link>>();

		BufferedReader bfr = null;
		try {
			bfr = new BufferedReader(new FileReader(harddrive
					+ "/src/Relevance/Links.txt"));
		} catch (FileNotFoundException e1) {
			System.err
					.println("In Google.java (in getResults). Something happened when opening the file (1).");
		}

		String inputLine = null;
		try {
			inputLine = bfr.readLine();
		} catch (IOException e1) {
			System.err
					.println("In Google.java (in getResults). Something happened when reading the file (2).");
		}

		// reads in each line into a HashMap (tweetID, links)
		outer: while (inputLine != null) {
			String curID = inputLine;

			try {
				inputLine = bfr.readLine();
			} catch (IOException e) {
				System.err
						.println("In Google.java (in getResults). Something happened when reading the file (5).");
			}

			ArrayList<Link> links = new ArrayList<Link>();
			while (inputLine.contains("/")) {
				// special Link data structure class
				Link newLink = new Link(inputLine, null);

				// adds to arraylist
				links.add(newLink);

				try {
					inputLine = bfr.readLine();
				} catch (IOException e) {
					System.err
							.println("In Google.java (in getResults). Something happened when reading the file (3).");
				}

				if (inputLine == null) {
					// for last info
					tweetInfo.put(curID, links);

					break outer;
				}
			}

			tweetInfo.put(curID, links);
		}

		try {
			bfr.close();
		} catch (IOException e) {
			System.err
					.println("In Google.java (in getResults). Something happened when closing the file (4).");
		}

		System.out
				.println("Finished gathering tweet links. Starting to judge relevance...");

		// goes through the rankList (all tweets for all topics
		for (int i = 0; i < rankedTweetLists.size(); i++) {
			// gets ranked list of tweets for each topic we want to print
			for (int k = 0; k < rankedTweetLists.get(i).size(); k++) {
				// the list of tweets for that topic
				RankedTweetList rtl = rankedTweetLists.get(i).get(k);

				for (int j = 0; j < rtl.size(); j++) {
					Tweet curTweet = rtl.getRankedList().get(j);

					// if it was part of the top 10 collection
					if (tweetInfo.containsKey(String.valueOf(
							curTweet.getTweetID()).trim())) {
						ArrayList<Link> links = tweetInfo.get(String.valueOf(
								curTweet.getTweetID()).trim());

						int percent = rescoreGoogeLinks(queryInfo.get(i).getLinks(),
								links);
						
						float score = curTweet.getScore();
						
						if (percent >= 5) {
							curTweet.setScore(score + DEFAULT_GOOGLE_BONUS);
						}else{
							curTweet.setScore(score - DEFAULT_GOOGLE_BONUS);
						}
					}
				}
			}
		}

		System.out.println("Finished judging relevance.");

		return rankedTweetLists;
	}

	/**
	 * Creates the text file containing the URLs from googling the tweets
	 */
	private void createTextFile(
			ArrayList<ArrayList<RankedTweetList>> rankedTweetLists,
			HashMap<String, String> months, ArrayList<GoogleInfo> queryInfo) {
		BufferedWriter bfw = null;
		try {
			bfw = new BufferedWriter(new FileWriter(harddrive
					+ "/src/Relevance/Links.txt"));
		} catch (IOException e1) {
			System.err
					.println("In Google.java (in createTextFile). Something went wrong creating the .txt file.");
		}

		// goes through the rankList (all tweets for all topics
		for (int i = 0; i < rankedTweetLists.size(); i++) {
			// gets ranked list of tweets for each topic we want to print

			System.out.println("TopicNum: " + queryInfo.get(i).getTopicNum());

			for (int k = 0; k < rankedTweetLists.get(i).size(); k++) {
				// the list of tweets for that topic
				RankedTweetList rtl = rankedTweetLists.get(i).get(k);
				for (int j = 0; j < Math.min(num_to_scrape, rtl.size()); j++) {
					Tweet curTweet = rtl.getRankedList().get(j);

					String tweetCheck = curTweet.getStatus();

					tweetCheck = URLListGen.removeURLs(tweetCheck);

					String tweetStatus = Downloader.gQC(tweetCheck.trim());

					try {
						Thread.sleep(5000);
					} catch (InterruptedException ex) {
						Thread.currentThread().interrupt();
					}

					ArrayList<Link> links = null;
					try {
						links = Downloader.setGoogleLinks(tweetStatus,
								queryInfo.get(i).getQueryTime(), months);
					} catch (IOException e) {
						System.err
								.println("In Google.java (in createTextFile). Something occurred when grabbing the query's links.");
						System.exit(0);
					}

					if (links != null) {
						try {
							bfw.write(Long.toString(curTweet.getTweetID()));
							bfw.newLine();
							bfw.flush();
						} catch (IOException e) {
							System.err
									.println("In Google.java (in createTextFile). Something happened when writing to the file (1).");
						}

						for (int m = 0; m < links.size(); m++) {
							if (links.get(m).getUrl() != null) {
								try {
									bfw.write(links.get(m).getUrl());
									bfw.newLine();
									bfw.flush();
								} catch (IOException e) {
									System.err
											.println("In Google.java (in createTextFile). Something happened when writing to the file (2).");
								}
							}
						}
					}

					num_amount++;

					// So we can see how much has been done
					System.out.println(num_amount);
				}
			}
		}

		try {
			bfw.close();
		} catch (IOException e) {
			System.err
					.println("In Google.java (in createTextFile). Something happened when closing the file (2).");
		}
	}

	/**
	 * Will compare and find any similar urls. Once found, returns true;
	 * 
	 * @param queryLinks
	 *            The arraylist of links from the topic
	 * @param tweetLinks
	 *            The arraylist of links from the tweet
	 * @return True if found any similar urls
	 */
	private int rescoreGoogeLinks(ArrayList<Link> queryLinks,
			ArrayList<Link> tweetLinks) {
		int count = 0;
		
		for (int i = 0; i < queryLinks.size(); i++) {
			for (int j = 0; j < tweetLinks.size(); j++) {
				if (queryLinks.get(i).getUrl()
						.equalsIgnoreCase(tweetLinks.get(j).getUrl())) {
					count++;
				}
			}
		}

		return count;
	}
}