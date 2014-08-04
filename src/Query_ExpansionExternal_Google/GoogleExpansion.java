package Query_ExpansionExternal_Google;

import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * This class expands the query by using Google. It will search Google and find
 * documents that are relevant to the query. It will find the most common words
 * used within the document and return the value as a String. It uses the
 * Article class to remove the symbols and stop words that are unnecessary.
 * 
 * @author Chan Tran v1.0
 * @edited Lauren Mathews v1.5
 * @version 7/22/2011 v1.0
 * @version 6/24/2014 v1.5
 */
public class GoogleExpansion {
	private String document;
	private int countCommon;
	private ArrayList<String> txt = new ArrayList<String>();
	private ArrayList<String> mostCommon = new ArrayList<String>();
	private ArrayList<String> all = new ArrayList<String>();
	private ArrayList<String> fileOut = new ArrayList<String>();
	private ArrayList<Integer> num = new ArrayList<Integer>();
	private StringTokenizer token;

	public GoogleExpansion() {
		document = "";
		countCommon = 0;
	}

	/**
	 * This method finds the most common word or words in a given text or
	 * document. It will loop through each word and find out with word/words
	 * occurs the most and returns the values of the words as a String.
	 */
	public String mostCommon(String doc, String query, String url,
			String stopWords) {
		Article art = null;
		try {
			art = new Article(stopWords);
		} catch (Exception e) {
			System.err
					.println("In GoogleExpansion.java (in mostCommon). Something happened when running Article.java (constructor). ERROR: "
							+ e.getMessage());
			System.exit(0);
		}

		try {
			document = art.notWantedQuery(doc.toLowerCase().trim(), query, url);
		} catch (Exception e) {
			System.err
					.println("In GoogleExpansion.java (in mostCommon). Something happened when running Article.java (notWantedQuery). ERROR: "
							+ e.getMessage());
			System.exit(0);
		}

		token = new StringTokenizer(document, " ");

		// Loops through the doc to find each individual word and adds it to an
		// ArrayList of String.
		while (token.hasMoreTokens()) {
			txt.add(token.nextToken());
		}

		removeQueryWords(query);

		// Counts the number of times the word occurs within the entire text.
		for (int i = 0; i < txt.size(); i++) {
			countCommon = 0;
			for (int j = 0; j < txt.size(); j++) {
				if (txt.get(i).equalsIgnoreCase(txt.get(j)))
					countCommon++;
			}
			num.add(countCommon);
		}

		// Finds the four most re-occurring words in the text.
		int count = 0;
		while (count < 4) {
			int highest = 0;
			int index = 0;

			for (int i = 0; i < num.size(); i++) {
				if (num.get(i) > highest) {
					highest = num.get(i);
					index = i;
				}
			}

			if (highest > 1) {
				String wordAt = txt.get(index);
				mostCommon.add(txt.get(index));
				all.add(txt.get(index));

				// Removes the word after it is accounted for.
				for (int i = 0; i < txt.size(); i++) {
					if (txt.get(i).equalsIgnoreCase(wordAt)) {
						txt.remove(i);
						num.remove(i);
						i = 0;
					}
				}
			}
			count++;
		}

		if (mostCommon.size() == 0) {
			for (int i = 0; i < Math.min(4, txt.size()); i++) {
				mostCommon.add(txt.get(i).toLowerCase());
			}
		}

		String out = "";

		for (int i = 0; i < mostCommon.size(); i++) {
			if (i == mostCommon.size() - 1)
				out += mostCommon.get(i);
			else
				out += mostCommon.get(i) + " ";
		}

		mostCommon.clear();
		txt.clear();
		num.clear();
		return out;
	}

	/**
	 * Removes any words that are contained in the query itself
	 * 
	 * @param query The current query to check
	 */
	private void removeQueryWords(String query) {
		token = new StringTokenizer(query.replace(",", " ").trim(), "+");

		while (token.hasMoreTokens()) {
			String indivQuery = token.nextToken().trim();
			for (int i = 0; i < txt.size(); i++) {
				if (indivQuery.equalsIgnoreCase(txt.get(i))) {
					txt.remove(i);
					i = 0;
				}
			}
		}
	}

	/**
	 * @return The words common to all links for each topic
	 */
	public String compare(String allTxt) {
		String out = "";
		ArrayList<String> every = new ArrayList<String>();
		token = new StringTokenizer(allTxt, " ");

		while (token.hasMoreTokens()) {
			every.add(token.nextToken());
		}

		@SuppressWarnings("unchecked")
		ArrayList<String> removable = (ArrayList<String>) every.clone();
		ArrayList<Integer> frequency = new ArrayList<Integer>();
		int size = every.size();

		for (int i = 0; i < size; i++) {
			int numFound = 0;
			
			for (int j = 0; j < removable.size(); j++) {
				if (every.get(i).equalsIgnoreCase(removable.get(j))) {
					removable.remove(j);
					j--;
					numFound++;
				}
			}
			
			frequency.add(numFound);
		}

		for (int i = 0; i < frequency.size(); i++) {
			int freq = frequency.get(i);
			if (freq > 1) {
				out += every.get(i) + " ";
			}
		}
		
		out = out.trim();
		all.clear();
		fileOut.add(out);
		
		return out;
	}
}