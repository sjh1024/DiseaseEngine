import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.miscellaneous.CapitalizationFilter;
import org.apache.lucene.analysis.pattern.PatternTokenizer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.synonym.SynonymFilter;
import org.apache.lucene.analysis.synonym.SynonymMap;

import java.io.Reader;
import java.util.regex.Pattern;

/**
 * Custom analyzer for Team 4's project.
 * I named it after myself because it's funny.
 * @author Sarah Hall
 */
public class HallAnalyzer extends Analyzer {
    private Reader inp;
    private static final Pattern commaDelimited = Pattern.compile(",");
    private SynonymMap MedicalMap;

    public HallAnalyzer(SynonymMap m){
        MedicalMap = m;
    }
    @Override
    protected TokenStreamComponents createComponents(String fieldName){
        //PatternTokenizer tokenizes by commas, NOT Whitespace
        //This makes every symptom its own token.
        //group -1 means that it has the same behavior as String.split, meaning that
        //the symptoms are comma-delimited.
        PatternTokenizer tokenizer = new PatternTokenizer(commaDelimited, -1);
        TokenStream result = new StandardFilter(tokenizer);
        result = new LowerCaseFilter(result);
        //Let's use the standard Stopword filter.
        //There should be no stopwords in a list of symptoms.
        result = new StopFilter(result, StandardAnalyzer.STOP_WORDS_SET);
        //No porter stem filter; seems like it might needlessly shorten symptoms
        //result = new CapitalizationFilter(result);
        result = new SynonymFilter(result, MedicalMap, true);
        System.out.println("RESULT: " + result);
        return new TokenStreamComponents(tokenizer, result);
    }
}
