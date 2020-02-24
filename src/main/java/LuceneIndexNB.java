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
import java.util.Scanner;

public class LuceneIndexNB {

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

        SynonymMap map = buildMedicalSynonymMap();

        Analyzer a;
        try {
            //Grab user input for file names/directories
            Scanner input = new Scanner(System.in);
            System.out.print("***************************************************\n\n" +
                    "WELCOME TO TEAM 4'S PROJECT!\n\n" +
                    "***************************************************\n");
            System.out.println("Enter location of corpus, training and test files (should be all in one folder)");
            String test200Directory = input.nextLine();
            System.out.println("Enter location to save TREC run file(s): ");
            String trecDirectory = input.nextLine();
            System.out.println("Enter search similarity (bnn-bnn or btn-btn or bpn-bpn or bm25 or custom)");
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
            System.out.println("Enter location to save index of complete data: ");
            String indexDirectory = input.nextLine();
            System.out.println("Enter location to save index of training data: ");
            String trainIndexDirectory = input.nextLine();
            System.out.println("Enter location to save index of test data: ");
            String testIndexDirectory = input.nextLine();

            String teamName = "team_4";

            //Initialize files for corpus, test subset and train subset.
            File corpus = new File(test200Directory + "diseasesclassedspaces.txt");
            File testData = new File(test200Directory + "diseasesTestSet2.txt");
            File trainData = new File(test200Directory + "diseaseTrainSet2.txt");
            File testDisList = new File(test200Directory + "testDiseaseList.txt");
            File testDisClasses = new File(test200Directory + "testDiseaseClasses.txt");

            //build indexes
            System.out.println("Building indexes. Please wait...");
            IndexerNB indexer = new IndexerNB(indexDirectory, a);
            indexer.rebuildIndexes(corpus);
            IndexerNB trainIndexer = new IndexerNB(trainIndexDirectory, a);
            trainIndexer.rebuildIndexes(corpus);
            IndexerNB testIndexer = new IndexerNB(testIndexDirectory, a);
            testIndexer.rebuildIndexes(corpus);
            System.out.println("Done building indexes.");

            //Initialize TREC run file
            File resultFile = new File(trecDirectory +"trecrunfile.txt");
            //System.out.println(trecDirectory +"trecrunfile-" + similarity + ".txt");
            resultFile.createNewFile();
            FileWriter fileWriter = new FileWriter(resultFile, true);


            System.out.println("Performing searches. Please wait...");
            SearchEngineNB se = new SearchEngineNB(indexDirectory, corpus,
                    trainData,
                    testData,
                    a,
                    similarity, indexer.gettDocCount(), indexer.gettDocCount(), indexer.gettDocCount(), indexer, trainIndexer,testIndexer);
            TopDocs topDocs = se.performSearch(query, 5);
            ScoreDoc[] hits = topDocs.scoreDocs;
            String[] dis = new String[5];
            for(int i = 0; i < hits.length; i++){
                Document doc = se.getDocument(hits[i].doc);
                System.out.println(doc.get("id")
                        + " " + doc.get("paraText")
                        + " (" + hits[i].score + ")");
                dis[i] = doc.get("id");

            }
            System.out.println("Results found: " + topDocs.totalHits);
            fileWriter.close();

            System.out.println("Search complete.");

            System.out.println("Training classifier");
            se.trainBernoulliNB();

            System.out.println("Generating classification result");

            se.testBernoulliNB(dis);
            System.out.println("Classification result ends here");

            System.out.println("Testing begins");
            String[] testDisL = new String[39];
            String[] testDisC = new String[39];
            String[] predDisL = new String[39];
            fileToArray(testDisList,testDisL);
            fileToArray(testDisClasses,testDisC);
            se.testBernoulliNBWithArrayOutput(testDisL,predDisL);
            countNumOfMatches(predDisL,testDisC);

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

    public static void fileToArray(File f, String[] d) throws IOException{
        Scanner k = new Scanner(f);
        int c = 0;
        while(k.hasNextLine()) {
            d[c] = "" + k.nextLine();
            c++;
        }
    }

    public static void countNumOfMatches(String[] a, String[] b) {
        int count = 0;
        for(int i = 0; i < a.length; i++) {
            if(a[i].equals(b[i])) {
                count++;
            }
        }
        System.out.println(count + " number of classes match out of "+a.length+" classes");
        //double p = ((double)count/(double)a.length)*100;
        //System.out.println("Percentage match: "+ p +"%");
    }
}

