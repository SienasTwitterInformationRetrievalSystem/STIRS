package API;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.thrift.TException;

import cc.twittertools.search.api.TrecSearchThriftClient;
import cc.twittertools.thrift.gen.TResult;

/**
 * Works with STIRS to connect to API (twitter-tools). Grabs all the tweets for
 * each topic in text format. Prints to STIRS index.
 * 
 * Query-Splitting: Split query in half, query API with both and merge results.
 * 
 * @author Tim LaRock v1.0
 * @author Lauren Mathews v1.5
 * @version 7/2014 v1.0
 * @version 7/23/2014 v1.5
 */
public class API_Query {
	int port, num_results, results_one, results_two;
	long max_id;
	private boolean type;
	final String token, host, group;
	String qID, query, runtag, query_one = "", query_two = "",
			harddrive;

	/**
	 * @param port
	 *            9090 for 2011, 9091 for 2013
	 * @param qID
	 *            The id of the query (ex. MB001)
	 * @param query
	 *            The query itself (ex. BBC World Service staff cuts)
	 * @param max_id
	 *            QueryTweetTime from query (ex. 34952194402811904)
	 * @param num_results
	 *            Number of results to return (max is 10,000)
	 * @param runtag
	 *            The tag for the run being created (ex. "myRun")
	 * @param type
	 *            split query switch (true == split, false == normal)
	 */
	public API_Query(int port, String qID, String query, long max_id,
			int numResults, String runtag, boolean type, String hD) {
		this.type = type;
		this.max_id = max_id;
		this.num_results = numResults;
		this.runtag = runtag;

		// host server - only change if TREC says it has changed
		this.host = "nest.umiacs.umd.edu";
		this.port = port;

		// token given by TREC for 2014 Microblog Track
		this.token = "ddf20f";

		// group id given by TREC for 2014 Microblog Track
		this.group = "tw13t016";
		this.qID = qID;
		this.harddrive = hD;

		// query splitting relies on the fact that there are 2+ words
		// in the query. This keeps 1 word queries from doing query splitting,
		// but only if query splitting is turned on (through type)
		if (this.type && !query.trim().contains(" ")) {
			this.type = false;
		}else if(this.type && query.contains("\"")){
			query = query.replace("\"", "");
		}

		// This is when no query splitting is enabled
		if (this.type == false) {
			this.query = query;

			// query split enabled; splits query in "half"
		} else if (this.type == true) {
			// Tokenize the query
			StringTokenizer st = new StringTokenizer(query);

			// return number of st.next() calls until tokenizer returns NULL
			int query_length = st.countTokens();

			// Put the first 'half' of the query into temp
			for (int i = 0; i < query_length / 2; i++){
				this.query_one += " " + st.nextToken();
			}

			// put the remainder of the query into temp2
			while (st.hasMoreTokens()){
				this.query_two += " " + st.nextToken();
			}

			this.query_one = this.query_one.trim();
			this.query_two = this.query_two.trim();
		} else {
			System.out
					.println("In API_Query (in constructor). Something went wrong, most like is \"ERROR: Type null.\"");
		}
	}

	/**
	 * This method is called directly from STIRS.java. This method is called
	 * once for each topic. Collects the tweets from the API based on query.
	 * 
	 * @param output_path
	 *            Ex: /home/lmathews/workspace/TREC2014/src/API/index-run/MB001.
	 *            txt
	 */
	public void run_query(String output_path) throws IOException {
		// used to keep track of exceptions; allows 3 exceptions to be
		// thrown until stopping API run, as per TREC standards
		int exCount = 0;

		// this is a search client, and should only be used once; multiple
		// clients can overload the API (not a good idea)
		TrecSearchThriftClient client = new TrecSearchThriftClient(this.host,
				this.port, this.group, this.token);

		try {
			try {
				try {
					// if the query is NOT to be split, return as normal
					if (this.type == false) {
						BufferedWriter writer = new BufferedWriter(
								new FileWriter(output_path));
						
						// searches API and grabs num_results tweets for query
						List<TResult> results = client.search(this.query,
								this.max_id, this.num_results);
						
						// keeps track of assigning rank as results are printed
						//AND
						// keeps track of how many tweets are actually printed
						// (some removed if 1) non-english or 2) retweets)
						int rank = 1;

						// goes through every tweet returned by API
						for (TResult result : results) {
							// weird error not returning full tweet info
							// couldn't find cause of it, so we remove those
							// tweets (sadly)
							if (!result.toString().endsWith(")")) {
								continue;
							}

							// grabs the language of the tweet
							// (API gives us this)
							Object check = result.getLang();
							String lang = "";

							// this is for checking the language
							if (check == null) {
								lang = "null";
							} else {
								lang = check.toString();
							}

							// grabs only the tweet itself
							String tweetText = result.getText();

							// checks the language of the tweet
							if (lang.trim().equalsIgnoreCase("null")) {
								// if null (and they were in 2014), does our own
								// filter; this is consider internal
								lang = APISearch.EnglishFilter(tweetText, lang);

								// if not english, "continues" (doesn't print it
								// to our own Tweet Corpus Index)
								if (!lang.equalsIgnoreCase("en")) {
									continue;
								}
							} else if (!lang.trim().equalsIgnoreCase("en")) {
								continue;
							}

							// if not a retweet (or non-english), prints it
							if (!(tweetText.startsWith("RT") || tweetText
									.startsWith("rt"))) {

								// format of the line
								writer.write(String.format("%s Q0 %d %d %f %s",
										qID, result.id, rank, result.rsv,
										runtag));
								writer.write("# "
										+ result.toString().replaceAll(
												"[\\n\\r]+", " "));
								writer.newLine();
								writer.flush();

								// updates rank
								rank++;
							}
						}

						writer.close();

						// helps us keep track if that index will be less then
						// the amount need for run output results
						if ((rank-1) < 1000 && results.size() > 1000) {
							System.err
									.println("In API_Query (in run_query). (STIRS Problem) index will have less than 1,000!");
							System.err.println("results.size(): "
									+ results.size());
							System.err.println("amount: " + (rank-1));
							System.err.println("Topic Num: " + qID + " - "
									+ this.query);
						}
						// if the query is to be split
					} else {
						// create two output files to hold the separate queries
						String output_one = harddrive + "/src/API/"
								+ "q1_results.txt";
						String output_two = harddrive + "/src/API/"
								+ "q2_results.txt";

						// create two writers to write the query output
						BufferedWriter writer = new BufferedWriter(
								new FileWriter(output_one));

						// Finds all query stuff for FIRST HALF OF QUERY

						List<TResult> results = client.search(this.query_one,
								this.max_id, this.num_results);

						// used later for keeping track of less then 1000 tweet
						// topics
						results_one = results.size();

						int rank = 1;

						for (TResult result : results) {
							if (!result.toString().endsWith(")")) {
								continue;
							}

							Object check = result.getLang();
							String lang = "";

							if (check == null) {
								lang = "null";
							} else {
								lang = check.toString();
							}

							String tweetText = result.getText();

							if (lang.trim().equalsIgnoreCase("null")) {
								lang = APISearch.EnglishFilter(tweetText, lang);

								if (!lang.equalsIgnoreCase("en")) {
									continue;
								}
							} else if (!lang.trim().equalsIgnoreCase("en")) {
								continue;
							}

							if (!(tweetText.startsWith("RT") || tweetText
									.startsWith("rt"))) {
								writer.write(String.format("%s Q0 %d %d %f %s",
										qID, result.id, rank, result.rsv,
										runtag));
								writer.write("# "
										+ result.toString().replaceAll(
												"[\\n\\r]+", " "));
								writer.newLine();
								writer.flush();

								rank++;
							}
						}

						// Reset the writer, repeat process for second half of
						// query
						writer = null;
						writer = new BufferedWriter(new FileWriter(output_two));

						// CLEAR results list to use again
						results.clear();

						// Finds all query stuff for SECOND HALF OF QUERY

						results = client.search(this.query_two, this.max_id,
								this.num_results);

						results_two = results.size();

						// Write the output to a file in standard TREC format
						rank = 1;

						for (TResult result : results) {
							if (!result.toString().endsWith(")")) {
								continue;
							}

							Object check = result.getLang();
							String lang = "";

							if (check == null) {
								lang = "null";
							} else {
								lang = check.toString();
							}

							String tweetText = result.getText();

							if (lang.trim().equalsIgnoreCase("null")) {
								lang = APISearch.EnglishFilter(tweetText, lang);

								if (!lang.equalsIgnoreCase("en")) {
									continue;
								}
							} else if (!lang.trim().equalsIgnoreCase("en")) {
								continue;
							}

							if (!(tweetText.startsWith("RT") || tweetText
									.startsWith("rt"))) {
								writer.write(String.format("%s Q0 %d %d %f %s",
										qID, result.id, rank, result.rsv,
										runtag));
								writer.write("# "
										+ result.toString().replaceAll(
												"[\\n\\r]+", " "));
								writer.newLine();
								writer.flush();

								rank++;
							}
						}

						writer.close();

						// searchAndMerge(output_one, output_two, output_path);
						Merge(output_one, output_two, output_path);
					}
				} catch (TException te) {
					System.err.println("Caught TException: " + te.getMessage());
					exCount++;
					if (exCount > 3) {
						System.err
								.println("Exception threshold (3) reached. Exiting to avoid API Overload.");
						System.exit(1);
					}
				}
			} catch (UnsupportedEncodingException e) {
				System.err
						.println("In API_Query (in run_query). Exception Caught: "
								+ e.getMessage());
				exCount++;
				if (exCount > 3) {
					System.err
							.println("In API_Query (in run_query). Exception threshold (3) reached. Exiting to avoid API Overload.");
					System.exit(1);
				}
			}
		} catch (IOException io) {
			System.err
					.println("In API_Query (in run_query). Caught IOException: "
							+ io.getMessage());
			exCount++;
			if (exCount > 3) {
				System.err
						.println("In API_Query (in run_query). Exception threshold (3) reached. Exiting to avoid API Overload.");
				System.exit(1);
			}
		}
	}

	/**
	 * OLD METHOD - DID NOT WORK BUT KEPT HERE FOR REFERENCE
	 * 
	 * Original tried query splitting by having it: 
	 * 1) Split each query in half and query the API with each half to get tweets 
	 * 2) Search for the other query in the query's tweet results 
	 * 		Ex. Search for "staff cuts" in "BBC World Service" tweet results, and vice versa 
	 * 3) Then merge those results together
	 * 
	 * However, this was found to have 1) low precisio and 2) pretty much what
	 * Lucene does already.
	 * 
	 * So we decided to just go with a straight merge instead, even through
	 * Lucene original score are lower then a plain API search.
	 * 
	 * @param query_one
	 *            Ex. /home/lmathews/workspace/TREC2014/src/API/q1_results.txt
	 * @param query_two
	 *            Ex. /home/lmathews/workspace/TREC2014/src/API/q2_results.txt
	 * @param outputPath
	 *            Ex: /home/lmathews/workspace/TREC2014/src/API/index-run/MB001.
	 *            txt
	 */
	@SuppressWarnings("unused")
	private void searchAndMerge(String query_one, String query_two,
			String outputPath) throws IOException {

		// search the first query for the second
		searchIntersection(query_one, this.query_two, "q1Final.txt");

		// search the second query for the first
		searchIntersection(query_two, this.query_one, "q2Final.txt");

		String q1Final = harddrive + "/src/API/q1Final.txt";
		String q2Final = harddrive + "/src/API/q2Final.txt";

		// merge the two output files
		Merge(q1Final, q2Final, outputPath);
	}

	/**
	 * Search for the other query in the query's tweet results Ex. Search for
	 * "staff cuts" in "BBC World Service" tweet results, and vice versa
	 * 
	 * @param input
	 *            Ex. /home/lmathews/workspace/TREC2014/src/API/q1_results.txt
	 * @param query
	 *            Ex. "BBC World Service" (first or second half of tweet)
	 * @param output_file
	 *            Ex: /home/lmathews/workspace/TREC2014/src/API/index-run/MB001.
	 *            txt
	 */
	private void searchIntersection(String input, String query,
			String output_file) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(input));
		BufferedWriter bw = new BufferedWriter(new FileWriter(harddrive
				+ "/src/API/" + output_file));

		// keeps track of reading in each line of br
		String line = "";

		// makes sure we don't print the same id twice
		HashSet<Long> tweetIDs = new HashSet<Long>();

		// holds each tweet's tweetID
		long tweetid = 0;

		// goes through each line (tweet) in results/file
		outer: while ((line = br.readLine()) != null) {
			// empty token
			String token = "";

			// weird error not returning full tweet info
			// couldn't find cause of it, so we remove those
			// tweets (sadly)
			if (!line.endsWith(")")) {
				continue;
			}

			// Tokenize the query
			StringTokenizer st = new StringTokenizer(query);

			// while there are more words in the query
			while (st.hasMoreTokens()) {
				token = st.nextToken();

				// if the line does NOT contain "RT" and DOES contain the
				// current token from the query, write it to the output file

				// used to find the tweetID of tweet
				StringTokenizer st2 = new StringTokenizer(line, " ");

				for (int i = 0; i < 3; i++) {
					if (i == 2) {
						tweetid = Long.parseLong(st2.nextToken());
					}

					st2.nextToken();
				}

				// if tweetID already printed, skips it
				if (tweetIDs.contains(tweetid)) {
					continue outer;
				}

				// used for checking for retweets
				String text = line.substring(line.indexOf("text:") + 5,
						line.indexOf(", followers_count:"));

				if ((!(text.startsWith("RT") || text.startsWith("rt")))
						&& line.contains(token)) {
					bw.write(line);
					bw.newLine();
					bw.flush();

					tweetIDs.add(tweetid);
				}
			}
		}

		br.close();
		bw.close();
	}

	/**
	 * Merges two separate halfs of a query together, in order of score
	 * 
	 * @param query_one
	 *            Ex. /home/lmathews/workspace/TREC2014/src/API/q1_results.txt
	 * @param query_two
	 *            Ex. /home/lmathews/workspace/TREC2014/src/API/q2_results.txt
	 * @param output
	 *            Ex: /home/lmathews/workspace/TREC2014/src/API/index-run/MB001.
	 *            txt
	 */
	private void Merge(String query_one, String query_two, String output)
			throws IOException {
		BufferedReader br = null;
		BufferedReader br2 = null;

		// the one with more results returned be in br
		if (results_one >= results_two) {
			br = new BufferedReader(new FileReader(query_one));
			br2 = new BufferedReader(new FileReader(query_two));
		} else {
			br = new BufferedReader(new FileReader(query_two));
			br2 = new BufferedReader(new FileReader(query_one));
		}

		BufferedWriter bw = new BufferedWriter(new FileWriter(output));

		// readLines for each reader
		String line = "", line2 = "";

		// keeps track of tweetIDs between readers
		long tweetid = 0, tweetid2 = 0;

		// keeps track of scores between readers
		float score = 0, score2 = 0;

		// keeps track of rank AND how many tweets are printed
		int rank = 1;

		// keeps track of which tweetIDs printed (we don't want duplicates)
		HashSet<Long> tweetIDs = new HashSet<Long>();

		// reads in initial each file line
		line = br.readLine();
		line2 = br2.readLine();

		// goes through each line of br
		while (line != null) {
			// weird error not returning full tweet info
			// couldn't find cause of it, so we remove those
			// tweets (sadly)
			if (!line.endsWith(")")) {
				line = br.readLine();
				continue;
			}

			// goes through next line of br2, if needed
			if (line2 != null) {
				// weird error not returning full tweet info
				// couldn't find cause of it, so we remove those
				// tweets (sadly)
				if (!line2.endsWith(")")) {
					line2 = br2.readLine();
					continue;
				}

				// for comparison purposes, finds score and tweetID
				// of both lines in files tweets
				StringTokenizer st = new StringTokenizer(line, " ");
				StringTokenizer st2 = new StringTokenizer(line2, " ");

				for (int i = 0; i < 6; i++) {
					if (i == 2) {
						tweetid = Long.parseLong(st.nextToken());
						tweetid2 = Long.parseLong(st2.nextToken());
					} else if (i == 3) {
						score = Float.parseFloat(st.nextToken());
						score2 = Float.parseFloat(st2.nextToken());
					}

					st.nextToken();
					st2.nextToken();
				}

				// Decide which tweet should be written to output first
				if (score > score2
						&& !(tweetIDs.contains(tweetid) || tweetIDs
								.contains(tweetid2))) {
					bw.write(updateRank(line, rank));
					bw.newLine();
					bw.flush();

					rank++;
					tweetIDs.add(tweetid);
					line = br.readLine();
				} else if (score < score2
						&& !(tweetIDs.contains(tweetid) || tweetIDs
								.contains(tweetid2))) {
					bw.write(updateRank(line2, rank));
					bw.newLine();
					bw.flush();

					rank++;
					tweetIDs.add(tweetid2);
					line2 = br2.readLine();
				} else if (tweetIDs.contains(tweetid)) {
					line = br.readLine();
				} else if (tweetIDs.contains(tweetid2)) {
					line2 = br2.readLine();
				}

				// if line2 becomes null, it's reaches the end of its file
				// that means no need to compare, as the last lines of br
				// will have a score less than last line from br2
			} else {
				bw.write(updateRank(line, rank));
				bw.newLine();
				bw.flush();

				rank++;
				tweetIDs.add(tweetid);
				line = br.readLine();
			}
		}

		// helps us keep track if that index will be less then
		// the amount need for run output results
		if ((rank - 1) < 1000 && Math.max(results_one, results_two) > 1000) {
			System.err
					.println("In API_Query (in Merge). (STIRS Problem) index will have less than 1,000!");
			System.err.println("results.size(): "
					+ Math.max(results_one, results_two));
			System.err.println("amount: " + (rank - 1));
			System.err.println("Topic Num: " + qID + " - " + this.query_one
					+ "+" + this.query_two);
		}

		br.close();
		br2.close();
		bw.close();
	}

	/**
	 * Used in query splitting only Updates the rank as the tweets are printed
	 * 
	 * @param line
	 *            Tweet, as returned by API
	 * @param rank
	 *            The rank to give it
	 * @return The new line with correct rank in it
	 */
	private String updateRank(String line, int rank) {
		// the line that will be printed
		String newLine = "";

		String curTok = "";

		// keeps track of when at rank field
		int i = 0;

		// tokenize the line
		StringTokenizer st = new StringTokenizer(line);

		while (st.hasMoreTokens()) {
			curTok = st.nextToken();

			// overwrite the old rank with the desired rank
			if (i == 3) {
				newLine += (Integer.toString(rank) + " ");
			} else {
				newLine += (curTok + " ");
			}

			i++;
		}

		return newLine;
	}
}