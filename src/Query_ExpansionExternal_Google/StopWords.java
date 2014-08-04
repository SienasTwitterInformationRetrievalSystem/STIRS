package Query_ExpansionExternal_Google;

/**
 * Built-in stoplist words (from SMART)
 * 
 * @author Edoardo Airoldi v1.0
 * @author Chan Tran (edited some code) v1.5
 * @edited Lauren Mathews v2.0
 * @version 7/22/2011 v1.5
 * @version 6/24/2014 v2.0
 */
public class StopWords {

	public static final String[] LONG = { "a", "a:hover", "able", "about",
			"above", "according", "accordingly", "account", "across",
			"actually", "advertisement", "after", "afterwards", "again",
			"against", "all", "allow", "allows", "almost", "alone", "along",
			"already", "also", "although", "always", "am", "among", "amongst",
			"an", "and", "another", "any", "anybody", "anyhow", "anyone",
			"anything", "anyway", "anyways", "anywhere", "apart", "appear",
			"appreciate", "appropriate", "are", "around", "as", "aside", "ask",
			"asking", "associated", "at", "available", "away", "awfully", "b",
			"be", "became", "because", "become", "becomes", "becoming", "been",
			"before", "beforehand", "behind", "being", "believe", "below",
			"beside", "besides", "best", "better", "between", "beyond", "blog",
			"blogs", "both", "brief", "browser", "business", "but", "by", "c",
			"came", "can", "cannot", "cant", "cause", "causes", "cbs",
			"certain", "certainly", "changes", "clearly", "co", "com", "come",
			"comes", "comment", "comments", "concerning", "concept",
			"consequently", "consider", "considering", "contain", "containing",
			"contains", "contact", "content", "corresponding", "could",
			"course", "currently", "current", "d", "daylife", "definitely",
			"described", "despite", "did", "different", "div", "do", "does",
			"doing", "done", "don't", "down", "downloads", "download",
			"during", "e", "each", "east", "edu", "eg", "eight", "either",
			"else", "elsewhere", "enough", "entirely", "english", "email",
			"especially", "et", "etc", "even", "ever", "every", "everybody",
			"everyone", "everything", "everywhere", "ex", "exactly", "example",
			"except", "f", "far", "false", "few", "fifth", "first", "five",
			"followed", "following", "follows", "follow", "for", "former",
			"formerly", "forth", "forum", "four", "from", "further",
			"furthermore", "g", "get", "gets", "getting", "given", "gives",
			"go", "good", "goes", "going", "gone", "got", "gotten",
			"greetings", "guardian", "h", "h1", "had", "happens", "hardly",
			"has", "have", "having", "he", "he'", "he's", "hello", "help",
			"hence", "her", "here", "hereafter", "hereby", "herein",
			"hereupon", "hers", "herself", "hi", "him", "himself", "his",
			"hither", "homepage", "home", "hopefully", "how", "howbeit",
			"however", "huffpost", "hr", "html", "i", "ie", "if", "it's",
			"i'm", "ignored", "immediate", "in", "inasmuch", "inc", "indeed",
			"indicate", "indicated", "indicates", "inner", "insofar",
			"instead", "into", "inward", "is", "it", "its", "itself", "j",
			"just", "k", "keep", "keeps", "kept", "know", "knows", "known",
			"l", "la", "language", "last", "lately", "later", "latter",
			"latterly", "least", "less", "lest", "let", "li", "like", "liked",
			"likely", "list", "little", "look", "looking", "looks", "log",
			"ltd", "m", "mainly", "many", "may", "maybe", "me", "mean",
			"meanwhile", "media", "merely", "might", "more", "more...",
			"moreover", "most", "mostly", "moved", "much", "must", "my", "mr",
			"mrs", "miss", "myself", "n", "name", "namely", "nd", "near",
			"nearly", "necessary", "need", "needs", "neither", "never",
			"nevertheless", "new", "news", "next", "nine", "no", "nobody",
			"non", "none", "noone", "nor", "north", "normally", "not",
			"notebook:", "nothing", "novel", "now", "nowhere", "nytimescom",
			"o", "obviously", "of", "off", "often", "oh", "ok", "okay", "old",
			"on", "once", "one", "ones", "only", "onto", "or", "other",
			"others", "otherwise", "ought", "our", "ours", "ourselves", "out",
			"outside", "over", "overall", "own", "p", "particular",
			"particularly", "password", "page", "per", "perhaps",
			"permanently", "pdf", "people", "photo", "photos", "placed",
			"please", "plus", "possible", "premium", "presumably", "probably",
			"provides", "q", "que", "quite", "qv", "r", "rather", "rd", "re",
			"read", "really", "reasonably", "recommended", "regarding",
			"regardless", "regards", "reply", "related", "relatively",
			"respectively", "reuters", "right", "room", "rs", "s", "said",
			"said:", "same", "saw", "say", "saying", "says", "script",
			"search", "second", "secondly", "see", "seeing", "seem", "seemed",
			"seeming", "seems", "seen", "self", "selves", "sensible", "sent",
			"serious", "seriously", "seven", "several", "shall", "share",
			"shares", "she", "should", "since", "six", "site", "skip", "so",
			"some", "somebody", "somehow", "someone", "something", "sometime",
			"sometimes", "somewhat", "somewhere", "soon", "source", "south",
			"sorry", "specified", "specify", "specifying", "still", "sub",
			"such", "sup", "sure", "t", "take", "taken", "tell", "tends",
			"term", "terms", "th", "than", "thank", "thanks", "thanx", "that",
			"thats", "the", "their", "theirs", "them", "themselves", "then",
			"thence", "there", "thereafter", "thereby", "therefore", "therein",
			"theres", "thereupon", "these", "they", "think", "third", "this",
			"thorough", "thoroughly", "those", "though", "three", "through",
			"throughout", "thru", "thus", "times", "to", "together", "told",
			"too", "took", "toward", "towards", "tried", "tries", "truly",
			"true", "try", "trying", "twice", "two", "tv", "u", "ul", "un",
			"under", "unfortunately", "unknown", "unless", "unlikely", "until",
			"unto", "up", "upon", "us", "use", "used", "useful", "uses",
			"using", "uss", "usually", "uucp", "v", "value", "various", "very",
			"via", "video", "view", "viz", "vs", "w", "want", "wants", "was",
			"watch", "way", "we", "weekdays", "weekly", "welcome", "well",
			"went", "were", "west", "what", "whatever", "when", "whence",
			"whenever", "where", "whereafter", "whereas", "whereby", "wherein",
			"whereupon", "wherever", "whether", "which", "while", "whither",
			"who", "whoever", "whole", "whom", "whose", "why", "will",
			"willing", "wish", "with", "within", "without", "wonder", "world",
			"would", "x", "y", "yes", "yet", "york", "you", "your", "yours",
			"yourself", "yourselves", "z", "zero", "menu", "back", "main",
			"dont" };

	public static final String[] SYMBOLS = { "~", "!", "@", "#", "$", "%", "^",
			"&", "&#160;", ":hover", "==", "*", "(", ")", "_", "+", "`", "--",
			"1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "0px", "1px",
			"2px", "3px", "4px", "5px", "6px", "7px", "8px", "9px", "10px",
			"-", "=", "{", "}", "|", "[", "]", "\\", ":", "\"", ";", "¢",
			"}#site-tools", "#sL", "<", ">", "?", "&amp;", ",", ".", "/",
			"mr.", "mrs.", "ms.", "miss", "sr.", "mister", "misses", "senior", };

	public static final String[] SHORT = { "about", "all", "am", "an", "and",
			"are", "as", "at",

			"be", "been", "but", "by",

			"can", "cannot",

			"did", "do", "does", "doing", "done",

			"for", "from",

			"had", "has", "have", "having",

			"if", "in", "is", "it", "its",

			"of", "on",

			"that", "the", "they", "these", "this", "those", "to", "too",

			"want", "wants", "was", "what", "which", "will", "with", "would" };
}