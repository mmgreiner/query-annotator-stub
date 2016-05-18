package annotatorstub.annotator;

import annotatorstub.utils.WATRelatednessComputer;
import it.unipi.di.acube.batframework.utils.WikipediaApiInterface;

import java.io.IOException;
import java.util.*;

/**
 * Created by mmgreiner on 03.05.16.
 *
 * An entity is a wikipedia page describing an unambiguous concept.
 * A mention is a continous sequence of terms in a query which refers to an entity
 * Ex:
 */
public class SimpleMention {
    public SimpleMention(String[] mentions, int wikipediaId, String wikipediaTitle, int beginTerm, int endTerm)
    {
        init(mentions, wikipediaId, wikipediaTitle, beginTerm, endTerm);
    }

    static int maxWikis = Integer.MAX_VALUE;

    String mentions;
    int wikipediaId;
    String wikipediaTitle;
    int beginTerm;
    int endTerm;

    /**
     * sets the maximum number of queries to get wikipedia titles that are used during annotation.
     * @param max
     */
    public static void setMaxWikis(int max)
    {
        if (max >= 0)
            maxWikis = max;
    }
    public static void resetMaxWikis()
    {
        maxWikis = Integer.MAX_VALUE;
    }

    public SimpleMention(String[] terms, String mentions, int wikipediaId, String wikipediaTitle)
    {
        String[] mens = mentions.split(" ");
        int b = Arrays.asList(terms).indexOf(mens[0]);
        int e = Arrays.asList(terms).indexOf(mens[mens.length-1]);
        init(mens, wikipediaId, wikipediaTitle, b, e);
    }

    void init(String[] mentions, int wikipediaId, String wikipediaTitle, int beginTerm, int endTerm)
    {
        this.mentions = String.join(" ", mentions);
        this.wikipediaId = wikipediaId;
        this.wikipediaTitle = wikipediaTitle;
        this.beginTerm = beginTerm;
        this.endTerm = endTerm;
    }


    /**
     * Assemble a list of all possible term sequences which will be potential mentions
     * @param terms
     * @return
     */
    public static List<String> getTermSequences(List<String> terms)
    {
        List<String> sequences = new ArrayList<String>();
        for (int b = 0; b < terms.size(); b++)
        {
            for (int e = b + 1; e <= terms.size(); e++)
            {
                String seq = String.join(" ", terms.subList(b, e));
                sequences.add(seq);
            }
        }
        return sequences;
    }


    /**
     * For the given consecutive sequence of terms query, return List of Wikipedia titles
     * @param termSequence
     * @param max - maximum number of titles to get. If -1, all titles are fetched
     * @return
     */
    public static List<SimpleMention> getMentions(String[] query, String termSequence, int max)
    {
        List<SimpleMention> res = new ArrayList<SimpleMention>();
        int wid;
        try {
            wid = WikipediaApiInterface.api().getIdByTitle(termSequence);
            if (wid >= 0) {
                int[] links = WATRelatednessComputer.getLinks(termSequence);
                int m = 0;
                for (int w : links) {
                    String entity = WikipediaApiInterface.api().getTitlebyId(w);
                    SimpleMention mention = new SimpleMention(query, termSequence, w, entity);
                    res.add(mention);

                    if (max > 0 && m >= max)
                        break;
                    m++;
                }
            }
        }
        catch (IOException e) {
            System.out.printf("getTitles: Exception " + e.getMessage());
        }
        return res;
    }

    public static List<SimpleMention> getMentions(String[] query, String termSequence)
    {
        return getMentions(query, termSequence, maxWikis);
    }

    public static List<SimpleMention> getMentions(List<String> query, String termSequence)
    {
        String[] q = query.toArray(new String[0]);
        return getMentions(q, termSequence, maxWikis);
    }


}
