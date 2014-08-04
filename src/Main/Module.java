package Main;

import java.util.ArrayList;

import API.RankedTweetList;
import API.LuceneQuery;

/**
 * The Module interface defines the necessary behavior of modules that modify
 * tweet rankings.
 * 
 * @author David Purcell v1.0
 * @edited Lauren Mathews v1.5
 * @version 6/2011 v1.0
 * @version 6/19/2014 v1.5
 */
public interface Module {

	/**
	 * Returns the results of a Module after processing the given queries and
	 * baseline results (or Module modified).
	 * 
	 * This method should only return <code>null</code> if a critical exception
	 * occurs.
	 * 
	 * @param queries A list of queries.
	 * @param rankedTweetLists A list of baseline or Module modified results for the queries.
	 * @return An adjusted list of tweet rankings for the queries.
	 */
	public abstract ArrayList<ArrayList<RankedTweetList>> getResults(ArrayList<LuceneQuery> queries,
			ArrayList<ArrayList<RankedTweetList>> rankedTweetLists);
}