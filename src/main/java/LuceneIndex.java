
import edu.unh.cs.treccar_v2.Data;
import edu.unh.cs.treccar_v2.read_data.DeserializeData;
import jdk.nashorn.internal.runtime.regexp.joni.Regex;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.synonym.SynonymMap;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField; //might not need
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.SimilarityBase;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.CharsRef;
import org.apache.lucene.util.Version;

import java.io.*;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public class LuceneIndex {

    /**
     * Create a SynonymMap object for the corpus.
     * @return SynonymMap
     */
    private static SynonymMap buildMedicalSynonymMap(){
        SynonymMap.Builder building = new SynonymMap.Builder(true);
        building.add(new CharsRef("polydypsia"), new CharsRef("excess" + SynonymMap.WORD_SEPARATOR + "thirst"), true);
        building.add(new CharsRef("excess" + SynonymMap.WORD_SEPARATOR + "thirst"), new CharsRef("polydypsia") , true);
        SynonymMap m = null;
        try{
            m = building.build();
        } catch(IOException e){
            System.out.println(e);
        }
        return m;
    }
    /**
     * Main method runs the Assignment 1 Program
     *
     * @param args String[]
     */

    public static void main(String[] args) {
        String[] SIMIL = {"default", "custom", "bnn-bnn", "btn-btn", "bpn-bpn", "u-l", "u-jm", "u-ds"};
        //Add "bnn-bnn", "btn-btn", "bpn-bpn", when completed.
        SynonymMap map = buildMedicalSynonymMap();

        Analyzer a;
        try {
            //Grab user input for file names/directories
            Scanner input = new Scanner(System.in);
            System.out.print("***************************************************\n\n" +
                    "WELCOME TO TEAM 4'S PROJECT!\n\n" +
                    "***************************************************\n");
            System.out.println("Enter location of corpus, training, test, and relevance files (should be all in one folder)");
            String test200Directory = input.nextLine();
            System.out.println("Enter location to save TREC run file(s): ");
            String trecDirectory = input.nextLine();
            System.out.println("Enter search similarity (bm25 or experiments)");
            String similarity = input.nextLine();
            System.out.println("Enter analyzer to use(default or custom)");
            String analyzer = input.nextLine();
            if(analyzer.equals("custom")){
                a = new HallAnalyzer(map);
            }
            else{
                a = new StandardAnalyzer();
            }
            System.out.println("Enter list of symptoms separated by commas");
            String query = input.nextLine();
            System.out.println("Enter location to save index: ");
            String indexDirectory = input.nextLine();
            System.out.println("Enter location of query file: ");
            String queryDirectory = input.nextLine();

            String teamName = "team_4";

            //Initialize files for corpus, test subset and train subset.
            File corpus = new File(test200Directory + "diseasecorpus.txt");
            File testData = new File(test200Directory + "diseaseTestSet.txt");
            File trainData = new File(test200Directory + "diseaseTrainSet.txt");
            File relevanceTextFile = new File(test200Directory + "relevanceText.txt");
            File queryList = new File(queryDirectory + "allQueries.txt");

            //build indexes
            System.out.println("Building indexes. Please wait...");
            Indexer indexer = new Indexer(indexDirectory, a);
            indexer.rebuildIndexes(corpus);
            System.out.println("Done building indexes.");

            //Initialize TREC run file
            File resultFile = new File(trecDirectory +"trecrunfile-" + similarity + ".txt");
            //System.out.println(trecDirectory +"trecrunfile-" + similarity + ".txt");
            resultFile.createNewFile();
            FileWriter fileWriter = new FileWriter(resultFile, true);

            System.out.println("Performing searches. Please wait...");
            SearchEngine se = new SearchEngine(indexDirectory,
                    trainData,
                    testData,
                    a,
                    similarity, indexer.gettDocCount(), indexer.gettDocCount(), indexer.gettDocCount(), indexer);
            Map<String, String> docMap = indexer.getDocMap();
            Map<String, Set<String>> fullTermFreqs = indexer.getTermFrequencies();

            TopDocs topDocs = se.performSearch(query, 5, relevanceTextFile, docMap, fullTermFreqs);
            ScoreDoc[] hits = topDocs.scoreDocs;
            for(int i = 0; i < hits.length; i++){
                Document doc = se.getDocument(hits[i].doc);
                String writeToFile = query + " Q0 " + doc.get("id") + " " + (i+1) + " " + hits[i].score
                        + " " + teamName + "-" + similarity + "\n";
                //System.out.println(writeToFile);
                fileWriter.write(writeToFile);
                System.out.println(doc.get("id")
                        + " " + doc.get("paraText")
                        + " (" + hits[i].score + ")");

            }
            System.out.println("Results found: " + topDocs.totalHits);
            fileWriter.close();

            System.out.println("Search complete.");

            if(similarity.equals("all")){ //build files for all similarities; for grading
                for(int i = 0; i < SIMIL.length; i++){
                    File resultFileA = new File(trecDirectory +  "trecrunfile-" + SIMIL[i] + ".txt");
                    resultFileA.createNewFile();
                    FileWriter fileWriterA = new FileWriter(resultFileA, true);

                    System.out.println("Creating " + SIMIL[i] + " output file. Please wait...");
                    SearchEngine seA = new SearchEngine(indexDirectory,
                            trainData,
                            testData,
                            a,
                            SIMIL[i].toLowerCase(), indexer.gettDocCount(), indexer.gettDocCount(), indexer.gettDocCount(), indexer);

                    //outlines file
                    //File pageFile = new File(test200Directory + "test200/test200-train/train.pages.cbor-outlines.cbor");

                    //FileInputStream fInputStream = new FileInputStream(pageFile);
                    //Iterable<Data.Page> k = DeserializeData.iterableAnnotations(fInputStream);

                    //File resultFile = new File(trecDirectory);



                    TopDocs topPage = seA.performSearch(query, 5, relevanceTextFile, docMap, fullTermFreqs);

                    // queryId Q0 parId rank score teamName-methodName
                    ScoreDoc[] results = topPage.scoreDocs;
                    for (int j = 0; j < results.length; j++) {
                        Document par = seA.getDocument(results[j].doc);
                        //Add query id
                        String writeToFile = query + " Q0 " + par.get("id") + " " + j + " " + results[j].score
                                + " " + teamName + "-" + similarity + "\n";
                        //System.out.println(writeToFile);
                        fileWriterA.write(writeToFile);
                    }


                    fileWriter.close();
                    System.out.println( SIMIL[i] + " output file done.");
                }

                System.out.println("\n\nSearches complete. See results in output TREC run files: \n"
                        + trecDirectory + "trecrunfile-default.txt\n" + trecDirectory + "trecrunfile-custom.txt\n"  +
                        trecDirectory + "trecrunfile-bnnbnn.txt\n" + trecDirectory + "trecrunfile-btnbtn.txt\n" + trecDirectory + "trecrunfile-bpnbpn.txt\n"  +
                        trecDirectory + "trecrunfile-u-l.txt\n" + trecDirectory + "trecrunfile-u-jm.txt" + trecDirectory + "trecrunfile-u-ds.txt");
            }



            File cfe = new File(indexDirectory + "_0.cfe");
            File cfs = new File( indexDirectory + "_0.cfs");
            File si = new File( indexDirectory + "_0.si");
            File segments = new File( indexDirectory + "segments_1");
            File writelock = new File( indexDirectory + "write.lock");

            System.gc();
            //boolean ce = cfe.delete();
            //boolean cs = cfs.delete();
            //boolean sid = si.delete();
            //boolean seg = segments.delete();
            //boolean wi = writelock.delete();

            //if( wi && seg && sid && cs && ce ){
             //   System.out.println("Index deleted successfully.");
            //}
            //else{
             //   System.out.println("INDEX DELETION FAILED, PLEASE DELETE ANY RESIDUAL FILES MANUALLY");
            //}


        } catch (ParseException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}

