package LinkCrawling;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import API.APISearch;
import API.LuceneQuery;
import API.RankedTweetList;
import API.Tweet;

/**
 * TwitterIndexer uses Apache Lucene to create an index of Twitter tweets.
 * 
 * @author Carl Tompkins v1.0
 * @author Karl Appel v1.5
 * 
 * @version 6/12/2011 v1.0
 * @version 6/27/2011 v1.5
 */
public class TwitterIndexerUrls {
	// Number of files indexed
	static int numDocuments;

	// File that contains the directory of the documents
	File docDir;

	// Where the documents are located
	static String docsPath;

	// Where to store the index
	static String indexPath;

	/**
	 * Constructor that initializes the variables of the indexer.
	 * 
	 * @param indexPath
	 *            Where the index should be stored
	 * @param docsPath
	 *            Path of the corpus file
	 */
	public TwitterIndexerUrls(String index_Path, String docs_Path,
			ArrayList<LuceneQuery> queries,
			ArrayList<ArrayList<RankedTweetList>> rankedTweetLists) {
		TwitterIndexerUrls.indexPath = index_Path;

		@SuppressWarnings("resource")
		Scanner reader = new Scanner(System.in);
		System.out
				.println("What year's collection (2011, 2012, 2013, or 2014) are you crawling?");
		String ans = reader.nextLine();

		String temp = docs_Path.substring(0, docs_Path.indexOf(".txt"));

		TwitterIndexerUrls.docsPath = temp + ans + ".txt";

		TwitterIndexerUrls.numDocuments = 0;

		@SuppressWarnings("unused")
		Date start = new Date();

		HashSet<Long> acceptedTweetIDs = new HashSet<Long>();
		for (int i = 0; i < rankedTweetLists.size(); i++) {
			for (int k = 0; k < rankedTweetLists.get(i).size(); k++) {
				ArrayList<Tweet> tweets = rankedTweetLists.get(i).get(k)
						.getRankedList();

				for (int m = 0; m < tweets.size(); m++) {
					Tweet curTweet = tweets.get(m);

					Long tweetID = curTweet.getTweetID();
					
					acceptedTweetIDs.add(tweetID);
				}
			}
		}

		// creates a separate url index for each topic
		// so each folder contains the lucene index of urls for that topic
		for (int i = 0; i < queries.size(); i++) {
			try {
				// creates the file path
				TwitterIndexerUrls.indexPath = index_Path + "/MB"
						+ queries.get(i).getQueryNum();

				//System.out.println("Writing index file to: " + indexPath);
				//System.out.println("Reading from: " + docsPath);

				// recreates for the topic directory
				Directory dir = FSDirectory.open(new File(TwitterIndexerUrls.indexPath));
				
				//these need to be re-made each time
				Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_31);
				IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_31,
						analyzer);

				docDir = new File(TwitterIndexerUrls.docsPath);

				iwc.setOpenMode(OpenMode.CREATE);

				// increase the RAM buffer. But if you do this, increase the max
				// heap size to the JVM (eg add -Xmx512m or -Xmx1g):
				iwc.setRAMBufferSizeMB(512.0);

				IndexWriter writer = new IndexWriter(dir, iwc);

				// creates the actual index files
				indexDocs(writer, docDir, analyzer, queries.get(i)
						.getQueryNum(), acceptedTweetIDs);

				writer.close();
				@SuppressWarnings("unused")
				Date end = new Date();

				if (numDocuments < 30) {
					System.err
							.println("In TwitterIndexerUrls.java. numDocuments is less than 30! (need at least 30 for urls)");
					System.err.println("Writing index file to: " + TwitterIndexerUrls.indexPath);
					System.err.println("Reading from: " + TwitterIndexerUrls.docsPath);
					System.err.println("Number of files indexed: "
							+ TwitterIndexerUrls.numDocuments);
					System.err.println("(It's possible that those tweets for the run weren't avaliable originally to be link-crawled!)");
				}
				
				//System.out.println("Number of files indexed: " + TwitterIndexerUrls.numDocuments);
			} catch (IOException e) {
				System.err
						.print("In TwitterIndexerUrls.java. Error when creating index. ERROR: "
								+ e.getClass() + " Message: " + e.getMessage());
			}
		}
	}

	/**
	 * 
	 * 
	 * @param writer
	 *            Writer to the index where the given file/dir info will be
	 *            stored
	 * @param file
	 *            The file to index, or the directory to recurse into to find
	 *            files to index
	 * @param queryNum
	 *            The current query number being index
	 * @param accpTweets
	 *            Which tweet IDs were found in the run file, so they can be
	 *            matched. This is because we no longer can fully Link Crawl the
	 *            whole corpus, only the limited amount of tweets we can get
	 *            from an initial clean run. By having this list, we can
	 *            guarantee only those tweetIDs that are in the run are found in
	 *            the index and make up the top 30 urls for each topic.
	 */
	private static void indexDocs(IndexWriter writer, File file,
			Analyzer analyzer, String queryNum, HashSet<Long> accpTweets)
			throws IOException {
		// do not try to index files that cannot be read
		if (file.canRead()) {
			FileInputStream fis;
			try {
				fis = new FileInputStream(file);
			} catch (FileNotFoundException fnfe) {
				// at least on windows, some temporary files raise this
				// exception with an "access denied" message
				// checking if the file can be read doesn't help
				return;
			}

			try {
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(fis, "Cp1252"));

				Pattern p = Pattern.compile("[a-zA-Z0-9]");

				numDocuments = 0;
				String temp = "";

				// goes through each line of the url content file
				String line = reader.readLine();
				while (line != null) {
					// grabs the topicNum, tweetID and url
					String topicNumString = line
							.substring(0, line.indexOf(" "));

					String tweetIDString = line.substring(
							line.indexOf(" ") + 1, line.indexOf(" ") + 19);
					tweetIDString = tweetIDString.trim();
					
					//(above) 19 might have to be changed if tweet ID gets larger
					
					String urlString = line.substring(line.indexOf(" ") + 19);
					
					// grabs the title
					line = reader.readLine();
					String titleString = line;

					// grabs the content
					line = reader.readLine();
					String contentString = line;

					// the following lines are used to grab all the content
					// as content can be on multiple lines
					line = reader.readLine();

					if (line == null) {
						break;
					}

					Matcher m = p.matcher(line);
					
					// helps with finding "empty" lines when grabbing all
					// content
					while (line.isEmpty() || !m.find()) {
						line = reader.readLine();

						if (line == null) {
							break;
						}

						m = p.matcher(line);
					}
					
					StringTokenizer sT = new StringTokenizer(line);

					temp = sT.nextToken();

					// this is used to keep grabbing content on multiple lines
					// until its hits the topic number
					while (!temp.startsWith("MB")) {
						contentString = contentString + " " + line;
						line = reader.readLine();
						
						m = p.matcher(line);

						while (line.isEmpty() || !m.find()) {
							line = reader.readLine();

							m = p.matcher(line);
						}

						sT = new StringTokenizer(line);
						temp = sT.nextToken();
					}
					
					// trims all
					tweetIDString = tweetIDString.trim();
					urlString = urlString.trim();
					titleString = titleString.trim();
					contentString = contentString.trim();
					
					// if not the right topic, skips it
					if (!("MB" + queryNum).equalsIgnoreCase(topicNumString)) {
						continue;
					}
					
					// if tweetID not found in run, skip it
					if (!accpTweets.contains(Long.parseLong(tweetIDString))) {
						continue;
					}

					// make a new, empty document
					Document doc = new Document();

					// Add the tweet ID of the status
					//System.out.println("The tweet ID is " + tweetIDString);
					Field tweetID = new Field("tweetID", tweetIDString,
							Field.Store.YES, Field.Index.NOT_ANALYZED);
					tweetID.setOmitTermFreqAndPositions(true);
					doc.add(tweetID);

					if (!topicNumString.startsWith("MB")
							|| topicNumString.length() != 5) {
						System.err
								.println("In TwitterIndexerUrls.java. (in indexDocs). Something is wrong with topicNumString: "
										+ topicNumString);
					}

					// Add the topic num of the status
					//System.out.println("The topicNum is " + topicNumString);
					Field topicNum = new Field("topicNum", topicNumString,
							Field.Store.YES, Field.Index.NOT_ANALYZED);
					topicNum.setOmitTermFreqAndPositions(true);
					doc.add(topicNum);

					// Add the url
					//System.out.println("The url is " + urlString);
					Field url = new Field("url", urlString, Field.Store.YES,
							Field.Index.ANALYZED);
					url.setOmitTermFreqAndPositions(true);
					doc.add(url);

					// Add the title
					//System.out.println("The title is " + titleString);
					Field title = new Field("title", titleString,
							Field.Store.YES, Field.Index.ANALYZED);
					title.setOmitTermFreqAndPositions(true);
					doc.add(title);

					// Add the content itself. It indexes the content, but it
					// does not store the content itself. This is
					// important to know.

					//System.out.println("The content is " + contentString + "\n");
					Field content = new Field("content", contentString,
							Field.Store.YES, Field.Index.ANALYZED);
					doc.add(content);
					if (writer.getConfig().getOpenMode() == OpenMode.CREATE) {
						// New index, so we just add the document (no old
						// document can be there):
						writer.addDocument(doc, analyzer);
					} else {
						// Existing index (an old copy of this document may
						// have been indexed) so
						// we use updateDocument instead to replace the old
						// one matching the exact
						// path, if present:
						// System.out.println("updating " + file);
						writer.updateDocument(new Term("path", file.getPath()),
								doc);
					}

					// increase document counter
					numDocuments++;
				}

				reader.close();
			} catch (Exception e) {
				System.err
						.println("In TwitterIndexUrls.java (in indexDocs). Exception occurred. ERROR: "
								+ e.getMessage());
				System.exit(0);
			} finally {
				fis.close();
			}
		}
	}
	
	/**
	 * Search the url index for top 30 urls for each topic
	 * 
	 * @param queries
	 *            The topics for the collection
	 * @return A list of 30 urls for each topic
	 */
	public static ArrayList<RankedTweetList> createRankedUrls(
			ArrayList<LuceneQuery> queries, ArrayList<ArrayList<RankedTweetList>> rankedTweetLists, int hitNum, String harddrive) {

		System.out.println("Making the right index...");

		String index_Path = harddrive + "/src/LinkCrawling/URLIndex";
		String docs_Path = harddrive + "/src/LinkCrawling/contentFile.txt";
		
		new TwitterIndexerUrls(index_Path, docs_Path, queries, rankedTweetLists);

		System.out.println("Finished making the right index. Making rankedUrlList (top 30 urls of each query topic)...");

		// will contain, for each query, the top 30 urls with scores
		ArrayList<RankedTweetList> rankedUrlLists = new ArrayList<RankedTweetList>();

		// query url index
		APISearch urlSearch = new APISearch(
				Logger.getLogger(Logger.GLOBAL_LOGGER_NAME), index_Path,
				"content");

		urlSearch.setHitsReturned(hitNum);

		try {
			rankedUrlLists = urlSearch.search(queries);
		} catch (Exception ex) {
			System.err
					.println("In TwitterIndexerUrls.java (in createRankedUrls). Something happened when getting rankedUrlLists. ERROR: "
							+ ex.getMessage());
			System.exit(0);
		}
		
		System.out.println("Finished making rankedUrlList (top 30 urls of each query topic).");

		return rankedUrlLists;
	}
}