package API;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

import API.QueryFormatter;

/**
 * Originally: The TwitterSearch class queries a user-specified index and
 * returns multiple results.
 * 
 * Version 2.0: Originally creates an arraylist of the 4 tags for Lucene; now
 * used to create an arraylist of the queries (topics).
 * 
 * @author Carl Tompkins v1.0
 * @edited Lauren Mathews v2.0
 * @version 6/7/11 v1.0
 * @version 6/13/14 v2.0
 */
public class QueryProcessor {
	private ArrayList<LuceneQuery> luceneQueryList = new ArrayList<LuceneQuery>();

	/**
	 * Processes the Query appropriately with the proper format needed for our
	 * system
	 * 
	 * @param inputFile
	 *            the inputfile where the queries are located
	 *            (org_test_topics.txt)
	 * @param logger
	 *            the logger that keeps track of the system information
	 */
	public QueryProcessor(String inputFile, Logger logger)
			throws IOException {

		// helps with figuring out what's happening
		System.out.println("Starting QueryFormatter...");

		// returns a list of query information, size 50, one topic on each line
		ArrayList<String> queList = QueryFormatter.formatsQueries(inputFile);

		// if something went wrong with Query Formatter
		if (queList.isEmpty() || queList == null || queList.size() < 50) {
			System.err
					.println("In QueryProcessor.java. Something is wrong with queList (list of topics).");
			System.exit(0);
		}

		// helps with figuring out what's happening
		System.out
				.println("Finished QueryFormatter. Starting QueryProcessor...");

		// goes through and adds the query to the queries list (topics only)
		for (int i = 0; i < queList.size(); i++) {
			luceneQueryList.add(QueryConverter.convertQuery(logger,
					queList.get(i)));
		}

		System.out.println("Finished QueryProcessor.");
	}

	/**
	 * @return luceneQueryList the lucene query all properly formatted
	 */
	public ArrayList<LuceneQuery> getSanitizedQueries() {
		return luceneQueryList;
	}
}