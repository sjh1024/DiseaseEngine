import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import edu.unh.cs.treccar_v2.Data;
import edu.unh.cs.treccar_v2.read_data.DeserializeData;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

/**
 * This class creates an index for LuceneIndexNB.java to make queries to
 * using a SearchEngine object.
 *
 * @author Ben Gildersleeve, Sarah Hall, M. Rasel Mahmud, Talha Siddique
 * @version 2
 */
public class IndexerNB {

    private IndexWriter indexWriter = null;
    private String iPath;
    private Analyzer ana;

    private int tDocCount = 0;
    private int emergency = 0;
    private int seekmedical = 0;
    private int wait = 0;

    //Contains documents mapped to their symptoms
    //<disease, symptom>
    private Map<String, String> docMap = new HashMap<String, String>();
    //Maps contain number of times a term occurs for each class
    //<term, frequency>
    private Map<String, Integer> emergMap = new HashMap<String, Integer>();
    private Map<String, Integer> seekmedMap = new HashMap<String, Integer>();
    private Map<String, Integer> waitMap = new HashMap<String, Integer>();
    //Contains doc frequencies over all documents.
    //NOTE: FOR OUR PARTICULAR DATASET
    private Map<String, Integer> fullDocFreqs = new HashMap<String, Integer>();
    private Map<String, Set<String>>fullTermFreqs = new HashMap<String, Set<String>>();

    /** Creates a new instance of Indexer (Constructor).
     * @param indexPath String: Path to save index to
     * @param analyzer Analyzer: The analyzer to be used by the indexer.
     * */
    public IndexerNB(String indexPath, Analyzer analyzer ) {
       iPath = indexPath;
       ana = analyzer;
    }
    public Map getDocMap(){ return docMap;}
    public int gettDocCount(){ return tDocCount; }
    public int getEmergency(){ return emergency; }
    public int getSeekmedical(){ return seekmedical; }
    public int getWait(){ return wait; }
    public Map getDocFrequencies(){ return fullDocFreqs; }
    public Map getTermFrequencies(){ return fullTermFreqs; }
    public Map getEmergMap(){ return emergMap; }
    public Map getSeekMedMap(){ return seekmedMap; }
    public Map getWaitMap(){ return waitMap; }


    /**
     * Gets the current instance of IndexWriter (Singleton).
     * @return IndexWriter
     * @throws IOException throws Exception if there is an error in getting IndexWriter.
     */
    public IndexWriter getIndexWriter() throws IOException {
        if (indexWriter == null) {
            Path path = FileSystems.getDefault().getPath(iPath);
            Directory indexDir = FSDirectory.open(path);
            IndexWriterConfig config = new IndexWriterConfig(ana);
            indexWriter = new IndexWriter(indexDir, config);
        }
        return indexWriter;
    }

    /**
     * Closes an existing IndexWriter
     * @throws IOException if closing fails.
     */
    public void closeIndexWriter() throws IOException {
        if (indexWriter != null) {
            indexWriter.close();
        }
    }

    /**
     * Indexes paragraphs from two Strings: a diseaseName (we'll treat this as an ID)
     * and symptoms(We'll treat this as the "text" of the document.
     * The method takes the strings and converts them
     * into documents with two fields: id (StringField) and para(graph)Text (TextField)
     * @param diseaseName: Data.Paragraph to be put into the index
     * @throws IOException
     */
    public void indexParagraph(String diseaseName, String classification, String symptoms) throws IOException {
        /* System.out.println("Indexing paragraphs... "); */
        IndexWriter writer = getIndexWriter();
        Document doc = new Document();
        doc.add(new StringField("id", diseaseName, Field.Store.YES));
        doc.add(new StringField("class", classification, Field.Store.YES));
        doc.add(new TextField("paraText", symptoms, Field.Store.YES));
        writer.addDocument(doc);
        //System.out.println(doc);
    }

    /**
     * Builds indexes from a file.
     * The method first erases any existing index,
     * then uses indexParagraphs for all Data.Paragraph objects
     * in an input file.
     * @param f: File to be broken down and indexed
     * @throws IOException if indexing fails for any reason
     */
    public void rebuildIndexes(File f) throws IOException {
        getIndexWriter();
        // Index all paragraph entries
        Scanner fileScanner = new Scanner(f);
        //System.out.println(f);
        while(fileScanner.hasNextLine()) { //for every document...
            String curLine = fileScanner.nextLine();
            //System.out.println(curLine);
            Scanner lineScanner = new Scanner(curLine);
            //Separate diseaseName and paragraph by colon.
            lineScanner.useDelimiter(":");
            //get the diseaseName
            String disease = lineScanner.next();
            //get the classification
            String classif = lineScanner.next();
            //great, now get the diseaseSymptoms all as one block of text
            String symptoms = lineScanner.next();
            //tokenize the symptoms
            //String[] tokenizedSymptoms = symptoms.(",");
            String[] tokenizedSymptoms = symptoms.split(",");
            //populate class maps
            if(classif.equals("emergency")){
                emergency++;
                for(String term: tokenizedSymptoms){
                    if(emergMap.get(term) != null){
                        Integer currentNumTerms = (Integer) emergMap.get(term);
                        //System.out.println("here-2:"+currentNumTerms);
                        emergMap.put(term, ++currentNumTerms);
                        //System.out.println("here-3:"+(Integer)emergMap.get(term));
                    }
                    else{
                        emergMap.put(term, 1);
                    }
                }
            }
            else if(classif.equals("seekmedical")){
                seekmedical++;
                for(String term: tokenizedSymptoms){
                    if(seekmedMap.get(term) != null){
                        //System.out.println("FOUND A TERM THAT ALREADY EXISTS");
                        Integer currentNumTerms = seekmedMap.get(term);
                        //System.out.println(currentNumTerms);
                        seekmedMap.replace(term, ++currentNumTerms);
                    }
                    else{
                        seekmedMap.put(term, 1);
                    }
                }
            }
            else if(classif.equals("wait")){
                wait++;
                for(String term: tokenizedSymptoms){
                    if(waitMap.get(term) != null){
                        Integer currentNumTerms = waitMap.get(term);
                        waitMap.replace(term, ++currentNumTerms);
                    }
                    else{
                        waitMap.put(term, 1);
                    }
                }
            }
            else{
                System.out.println(disease);
            }
            //Populate doc freq map (Remember: You're looking at 1 document in each iteration
            //of the while loop)
            Set<String> termsInCurDoc = new HashSet<String>();
            //Set<String> termsButItsAnArray = new ArrayList<String>();
            /*for(String term: tokenizedSymptoms){
                if(fullDocFreqs.get(term) != null){
                    if(!termsInCurDoc.contains(term)){
                        Integer currentNumTerms = fullDocFreqs.get(term);
                        currentNumTerms = currentNumTerms.intValue()+1;
                        fullDocFreqs.replace(term, currentNumTerms);
                    }
                }
                else{
                    fullDocFreqs.put(term, 1);
                    termsInCurDoc.add(term);
                    //termsButItsAnArray.add(term);
                }

            }*/
            //debugging output, uncomment if necessary.
            //System.out.println("Disease: " + disease);
            //System.out.println("Symptoms: " + symptoms);
            fullTermFreqs.put(disease, termsInCurDoc);
            indexParagraph(disease, classif, symptoms);
            docMap.put(disease, symptoms);
            tDocCount++;
        }
        //System.out.println("emergencies: " + emergency + "\nmedical: " + seekmedical + "\nwait: "+ wait);
        //System.out.println("Total num of docs in corpus: " + tDocCount);
        //System.out.println("Whole Map: " + Collections.singletonList(fullDocFreqs));
        closeIndexWriter();
    }

}
