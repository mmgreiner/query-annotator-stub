package annotatorstub.annotator;

/**
 * Created by mmgreiner on 02.05.16.
 */

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * class representing one Snippet returned from Bing
 */
public class Snippet {

    // bing highlightw word with special characters
    final static char BoldStart = '\uE000';
    final static char BoldEnd = '\uE001';
    static Pattern BoldWord = null;


    // maximum number of Snippets considered for xy
    public static int MaxSnippets = 25;

    String title;
    String description;
    List<String> boldTitles = new ArrayList<String>();
    List<String> boldDescription = new ArrayList<String>();
    String url;

    // getters
    public String getTitle() { return title; }

    /**
     * return the description but without the bold characters
     * @return
     */
    public String getDescription()
    {
        String reg = BoldStart + "|" + BoldEnd;
        String ret = description.replaceAll(reg, "");
        return ret;

    }
    public String getBareDescription() { return description; }
    public String getUrl() { return url; }
    public List<String> getBoldTitles() { return boldTitles; }
    public List<String> getBoldDescription() { return boldDescription; }


    /**
     * initializes a Snippet. Parses 'title' and 'description' and sets boldTitle and boldDescription lists accordingly.
     * @param title
     * @param description
     * @param url
     */
    public Snippet(String title, String description, String url) {
        this.title = title;
        this.description = description;
        this.url = url;

        // pattern do only once
        if (BoldWord == null) {
            String pat = BoldStart + "(\\w+)" + BoldEnd;
            BoldWord = Pattern.compile(pat);
        }

        // title = "hallo " + BoldStart + "Markus" + BoldEnd + " LUkas " + BoldStart + "Clara" + BoldEnd + " end";
        Matcher matcher = BoldWord.matcher(this.title);
        while (matcher.find()) {
            boldTitles.add(matcher.group(1));
        }

        matcher = BoldWord.matcher(this.description);
        while (matcher.find())
            boldDescription.add(matcher.group(1));
    }

}


