package annotatorstub.annotator;

import annotatorstub.utils.WATRelatednessComputer;
import com.sun.tools.javac.util.*;
import it.unipi.di.acube.BingInterface;
import it.unipi.di.acube.batframework.systemPlugins.WikipediaMinerAnnotator;
import it.unipi.di.acube.batframework.utils.AnnotationException;
import it.unipi.di.acube.batframework.utils.WikipediaApiInterface;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by mmgreiner on 02.05.16.
 * <p>
 * implements the Bing interface of the piggy-back according to:
 * "A piggyback System for Joint Entity Mention Detection and Linking in web Queries"
 * see: http://delivery.acm.org/10.1145/2890000/2883061/p567-cornolti.pdf?ip=195.176.110.66&id=2883061&acc=ACTIVE%20SERVICE&key=FC66C24E42F07228%2EA04051DB0C098788%2E4D4702B0C3E38B35%2E4D4702B0C3E38B35&CFID=779631553&CFTOKEN=40382529&__acm__=1462192053_fe4dcd2aa014831a5eae83960d874786
 */
public class BingPiggyBack {


    /**
     * initializes a binger and sets the BingInterface cache.
     */
    public BingPiggyBack(boolean useCache) {
        try {
            BingInterface.setCache("BingCache.tmp");
        } catch (Exception e) {
            System.err.println("BingInterface caching " + e.getMessage());
        }
    }


    public BingPiggyBack()
    {
        this(true);
    }


    List<Snippet> snippets = new ArrayList<>();

    /**
     * C(q): list of snippets returned by bing search.
     * @see #query(String)
     * @return
     */
    public List<String> getSnippets()
    {
        List<String> snips = snippets.stream().map(Snippet::getDescription).collect(Collectors.toList());
        return snips;
    }

    /**
     * B(q): list of all bold portions. Don't really know what is meant by multi-set.
     * @return
     */
    public List<String> getBolds()
    {
        List<String> bolds = new ArrayList<>();
        snippets.stream().forEach(sn -> bolds.addAll(sn.getBoldDescription()));
        return bolds;
    }

    int webTotal = 0;
    /**
     * W(q): gets the total number of web pages found.
     * @return
     */
    public int getWebTotal()
    {
        return webTotal;
    }


    String spellingSuggestion;
    /**
     * if the query contained spelling msitakes, this will contain the spelling correction
     * @return
     */
    public String getSpellingSuggestion() {
        return spellingSuggestion;
    }

    String _query;
    public String getQuery()
    {
        return _query;
    }

    /**
     * U(q): gets the list of Urls returned by query()
     * @see #query(String)
     * @return list of all urls from bing for this query.
     */
    public List<String> getUrls()
    {
        List<String> urls = snippets.stream().map(Snippet::getUrl).collect(Collectors.toList());
        return urls;
    }

    static final Pattern W = Pattern.compile("(\\w+)");

    List<String> terms = new ArrayList<>();

    /**
     * chop the query into individual terms by using regex with pattern \w+
     * @param query: the query to be chopped up
     * @return List of terms.
     */
    public static List<String> chopTerms(String query)
    {
        List<String> ents = new ArrayList<>();
        Matcher matcher = W.matcher(query);
        while (matcher.find())
        {
            ents.add(matcher.group(1));
        }
        return ents;
    }

    void setTerms(String q)
    {
        terms = chopTerms(q);
    }

    List<String> getTerms()
    {
        return terms;
    }

    /**
     * T(e) Get all the mentions for one Term Sequence
     * @see getMentionsAllTermSequences
     * @param termSequence
     * @return
     */
    public List<SimpleMention> getMentionsPerTermSequence(String termSequence)
    {
        List<SimpleMention> mentions = SimpleMention.getMentions(spellingSuggestion.split(" "), termSequence);
        return mentions;
    }

    /**
     * T(e): Returns all the wiki page titles for the given list of terms of a query
     * @param terms
     * @return A map/dictionary with Key = term sequence, and Value = List of all found wiki titles
     */
    public List<SimpleMention> getMentionsAllTermSequences(List<String> terms)
    {
        List<SimpleMention> mentions = new ArrayList<>();
        List<String> sequences = SimpleMention.getTermSequences(terms);
        for (String sequence : sequences) {
            List<SimpleMention> mentionsPerSequence = getMentionsPerTermSequence(sequence);
            mentions.addAll(mentionsPerSequence);
        }
        return mentions;
    }

    public List<SimpleMention> getMentionsAllTermSequences()
    {
        return getMentionsAllTermSequences(getTerms());
    }


    public static List<SimpleMention> getMentions(String query)
    {
        List<SimpleMention> totalMentions = new ArrayList<>();
        List<String> termSequences = chopTerms(query);

        for (String sequence : termSequences)
        {
            List<SimpleMention> mentionPerSequence = SimpleMention.getMentions(termSequences, sequence);
            totalMentions.addAll(mentionPerSequence);
        }
        return totalMentions;
    }

    /**
     * test the getter functions
     */
    public void test()
    {
        List<String> sl = getSnippets();
        System.out.println(sl);
        sl = getUrls();
        sl = getBolds();
    }


    final static String MyKey = "PETkpKYB5Vw58F0L4tHxgQQYvRFNE/auyiS/81QuUm4";

    static BingInterface bing = new BingInterface(MyKey);

    JSONObject bingresult = null;

    /**
     * query Bing for the given query. The query can contain spelling mistakes.
     * Sets the fields spellingSuggestion and snippets.
     * @param query - can contain spelling mistakes, they are indicated in spellingSuggestion
     * @throws AnnotationException
     */
    void query(String query) throws AnnotationException
    {
        // assume for now that there are no spelling problems.
        _query = query;
        setTerms(_query);

        // prepare query by eliminating any double stuff.
        _query = String.join(" ", getTerms());

        // initialize
        spellingSuggestion = _query;

        try {

            bingresult = this.getBingObject(_query);

            // see: http://datamarket.azure.com/dataset/bing/search#schema for
            // query/response format

            JSONObject result = bingresult.getJSONObject("d").getJSONArray("results").getJSONObject(0);

            // now parse result
            spellingSuggestion = result.getJSONArray("SpellingSuggestions").getJSONObject(0).getString("Value");
            // eliminate white space in between
            spellingSuggestion = String.join(" ", spellingSuggestion);
            setTerms(spellingSuggestion);

            webTotal = Integer.parseInt(result.getString("WebTotal"));

            // get
            JSONArray snips = result.getJSONArray("Web");

            for (int i = 0; i < snips.length(); i++) {
                JSONObject snip = snips.getJSONObject(i);
                Snippet snippet = new Snippet(snip.getString("Title"), snip.getString("Description"), snip.getString("Url"));
                snippets.add(snippet);
            }

        } catch (Exception ex) {
            System.err.println(ex.getMessage());
            System.err.println(ex.getStackTrace());
        }

    }

    public static void setJsonCaching()
    {
        caching = true;
    }

    static boolean caching = false;

    /**
     * with the bing interface already created, searches it for the given query and returns the json result.
     * If caching is on, checks if a file with this query name exists. If yes, reads from this file instead of bing.
     *
     * @param query
     * @return
     * @throws Exception
     */
    private JSONObject getBingObject(String query) throws Exception {
        JSONObject a = null;

        String filename = query.replace(" ", "-") + ".json.cache";
        Path path = Paths.get(filename);

        // from cache?
        if (caching && Files.exists(path)) {
                String txt = new String(Files.readAllBytes(path));
                a = new JSONObject(txt);
        }
        else {
            a = bing.queryBing(query);

            if (caching) {
                PrintWriter out = new PrintWriter(filename);
                out.write(a.toString());
                out.close();
            }
        }
        return a;
    }

    /**
     * Reading text files was and still is cumbersome in Java. This reads the filename and retunrs the string
     * @param path
     * @return
     * @throws IOException
     */
    private String ReadTextFile(String path) throws IOException {
        java.util.List<String> lines = Files.readAllLines(Paths.get(path));
        StringBuilder sb = new StringBuilder();
        for (String s : lines) {
            sb.append(s);
            sb.append(System.lineSeparator());
        }
        return sb.toString();
    }
}

