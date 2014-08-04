package Main;

import java.util.ArrayList;

import API.LuceneQuery;

/**
 * The QueryExpansion interface defines the necessary behavior of query expansion
 * modules, all which return new arraylists of queries (should all be size 50).
 * 
 * @author Lauren Mathews v1.0
 * @version 6/24/2014
 */
public interface QueryExpansion{
	
	/**
	 * Returns the new list of queries found when doing query expansion
	 */
	public abstract ArrayList<String> getNewQueries(ArrayList<LuceneQuery> queries);
}