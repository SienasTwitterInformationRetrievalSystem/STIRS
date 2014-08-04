package API;

import java.util.logging.Logger;

/**
 * Originally: A class that converts a query from the TREC format to one that is
 * easier to use for Lucene
 * 
 * (Example below has been modified) An example of a TREC query is: <top> <num>
 * Number: MB01 </num> <title> Wael Ghonim </title> <querytime> 25th February
 * 2011 04:00:00 +0000 </querytime> <querytweettime> 3857291841983981
 * </querytweettime> </top>
 * 
 * Version 2.5: Converts the query to the format for our system.
 * 
 * @author Matthew Kemmer v1.0
 * @edited Karl Appel v2.0
 * @edited Lauren Mathews v2.5
 * @version 5/23/11 v1.0
 * @version 6/10/12 v2.0
 * @version 6/13/14 v2.5
 */
public class QueryConverter {
	/**
	 * Goes through each line of the query information. Returns a new
	 * LuceneQuery with only the query topic (no info)
	 * 
	 * @param logger
	 *            used for keeping track of errors
	 * @param q
	 *            the full line of the query (topic information)
	 * @return a new Lucene Query object, containing the topic only (no info)
	 */
	public static LuceneQuery convertQuery(Logger logger, String q) {
		// checking arguments
		if (q.isEmpty() || q == null) {
			System.err
					.println("In QueryConverter.java (in convertQuery). q is either empty or null.");
			System.exit(0);
		}

		// splts up the query topic
		String[] text = q.split(" "), vals = { "", "", "", "" };

		// used for while loop
		int i = 0, tagIndex = 0;

		// each tag that will be found in the query in order
		// this is a tag for a regular search
		String[] tags = { "<top>", "<num>", "</num>", "<query>", "</query>",
				"<querytime>", "</querytime>", "<querytweettime>",
				"</querytweettime>", "</top>" };

		// parses through the query and picks out the data inbetween each tag
		while (i < text.length) {
			if (text[i].equals(tags[tagIndex] + tags[tagIndex + 1])) {
				tagIndex += 2;
			} else {
				vals[(tagIndex - 1) / 2] = vals[(tagIndex - 1) / 2] + " "
						+ text[i];
			}
			i++;
		}

		// removes the extraneous "Number: "
		if (vals[0].startsWith("Number")) {
			vals[0] = vals[0].substring(vals[0].indexOf(":") + 1);
		}

		// remove white space
		for (int j = 0; j < vals.length; j++) {
			vals[j] = vals[j].trim();
		}

		// updates logger
		logger.finest("Values extracted: " + vals[0] + "|" + vals[1] + "|"
				+ vals[2] + "|" + vals[3] + "|");

		return new LuceneQuery(vals[0], vals[1], vals[2],
				Long.parseLong(vals[3]), new Long(0), "adhoc");
	}
}