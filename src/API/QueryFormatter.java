package API;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Formats the queries properly so that they are all on a single line This is
 * done by just combining the lines together from the given page that they
 * provided us with queries
 * 
 * Version 1.5: returns an arraylist instead of a new file
 * 
 * @author Karl Appel v1.0
 * @edited Lauren Mathews v1.5
 * @version 6/2011 v.1.0
 * @version 6/13/14 v1.5
 */
public class QueryFormatter {
	/**
	 * Formats the queries - originally on multiple lines. Makes it so it's 50
	 * lines long
	 * 
	 * @param inputFile
	 *            org_test_topics.txt from TREC
	 */
	public static ArrayList<String> formatsQueries(String inputFile)
			throws IOException {
		// makes sure the inputFile is the right name
		if (inputFile.isEmpty() || inputFile == null) {
			System.err
					.println("In QueryFormatter.java (in formatsQueries). Something is wrong with the inputFile.");
			System.err.println("inputFile: " + inputFile);
			System.exit(0);
		}

		// the queries arraylist to be returned
		ArrayList<String> queList = new ArrayList<String>();

		// reads in the inputFile (org_test_topics.txt)
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(inputFile));
		} catch (FileNotFoundException e) {
			System.err
					.println("In QueryFormatter.java (in formatsQueries). Something wrong with reading in org_test_topics.txt");
			System.err.println("inputFile: " + inputFile);
			System.exit(0);
		}

		// each new line
		String line = reader.readLine();

		// will be the new line to be added
		String newLine = "";

		// goes through each line of the file
		while (line != null) {
			// goes through each topic (starts with <top>)
			if (line != null && line.equals("<top>")) {
				// goes through each line of the topic
				while (!line.equals("</top>")) {
					newLine = newLine + line;
					line = reader.readLine();
				}

				newLine = newLine + "</top>";

				// adds to the queList
				queList.add(newLine);

				// clears it for next topic
				newLine = "";
			}

			line = reader.readLine();
		}

		reader.close();

		return queList;
	}
}