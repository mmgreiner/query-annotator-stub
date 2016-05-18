package annotatorstub.annotator;

import it.unipi.di.acube.batframework.data.*;
import it.unipi.di.acube.batframework.problems.Sa2WSystem;
import it.unipi.di.acube.batframework.utils.AnnotationException;
import it.unipi.di.acube.batframework.utils.ProblemReduction;
import sun.java2d.pipe.SpanShapeRenderer;

import java.util.*;

/**
 * Created by mmgreiner on 01.05.16.
 */
public class Smath1Annotator implements Sa2WSystem {
    private static long lastTime = -1;
    private static float threshold = -1f;

    public long getLastAnnotationTime() {
        return lastTime;
    }

    public HashSet<Tag> solveC2W(String text) throws AnnotationException {
        return ProblemReduction.A2WToC2W(solveA2W(text));
    }

    public HashSet<Annotation> solveD2W(String text, HashSet<Mention> mentions) throws AnnotationException {
        return ProblemReduction.Sa2WToD2W(solveSa2W(text), mentions, threshold);
    }

    public HashSet<Annotation> solveA2W(String text) throws AnnotationException {
        return ProblemReduction.Sa2WToA2W(solveSa2W(text), threshold);
    }

    public HashSet<ScoredTag> solveSc2W(String text) throws AnnotationException {
        return ProblemReduction.Sa2WToSc2W(solveSa2W(text));
    }


    public HashSet<ScoredAnnotation> solveSa2W(String query) throws AnnotationException {
        lastTime = System.currentTimeMillis();
        int start = 0;


        while (start < query.length() && !Character.isAlphabetic(query.charAt(start)))
            start++;
        int end = start;
        while (end < query.length() && Character.isAlphabetic(query.charAt(end)))
            end++;

        BingPiggyBack.setJsonCaching();
        BingPiggyBack binger = new BingPiggyBack();

        binger.query(query);

        // compute the entities from the correct spelling
        String correctedSpelling = binger.getSpellingSuggestion();

        SimpleMention.setMaxWikis(50);

        List<SimpleMention>mentions = binger.getMentionsAllTermSequences();
        String bolds = String.join(" ", binger.getBolds());

        List<SimpleMention>boldMentions = BingPiggyBack.getMentions(bolds);

        HashSet<ScoredAnnotation> result = new HashSet<>();

        mentions.stream().forEach((mention) ->
                result.add(new ScoredAnnotation(mention.beginTerm, mention.endTerm - mention.beginTerm, mention.wikipediaId, 0.1f)));

        lastTime = System.currentTimeMillis() - lastTime;
        return result;

    }

    public String getName() {
        return "Smaph-1 annotator";
    }

}


