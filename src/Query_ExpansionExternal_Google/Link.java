package Query_ExpansionExternal_Google;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * A "data structure" class used for holding URLs.
 * 
 * @author ??? v1.0
 * @author Lauren Mathews v1.5
 * @version 6/2011 v1.0
 * @version 7/10/2014 v1.5
 */
public class Link {
	private String url = null;
	private String description = null;
	private URL u = null;

	public Link(String newURL, String newDescrip) {
		url = newURL;
		description = newDescrip;

		try {
			u = new URL(newURL);
		} catch (MalformedURLException e) {
			// this way it doesn't stop the program from running
			u = null;
		}
	}

	public String getUrl() {
		if (u == null) {
			return "null";
		} else {
			return u.toString();
		}
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String toString() {
		String rString = "";
		rString += url + " | " + description;
		return rString;
	}
}