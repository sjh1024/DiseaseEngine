import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.File;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.lucene.analysis.hunspell.Dictionary;
import org.apache.lucene.document.Document;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.Token;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.SimilarityBase;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefIterator;
import org.apache.lucene.util.Version;

import static java.lang.Math.log;

/**
 * SearchEngine class for LuceneIndexNB.java.
 * Takes queries and browses an index created by Indexer for data.
 *  @author Ben Gildersleeve, Sarah Hall, Talha Siddique
 *  @version 2
 */
public class SearchEngineNB {

    private IndexSearcher searcher;
    private IndexerNB indexStats;
    private IndexerNB trainIndexStats;
    private IndexerNB testIndexStats;
    private IndexSearcher trainSearcher;
    private QueryParser parser;
    private QueryParser trainParser;
    private String similarity;
    private String trainSimilarity;
    private IndexReader read;
    private IndexReader trainRead;
    private PostingsEnum test;
    private int vocabLength = 0;
    private int trainVocabLength;
    private int tDocCount;
    private int trainDocCount;
    private String[] classList;
    private int[] classDocNums;
    private double[] prior;
    private double[][] condprob;
    private int frequency = 0;
    private int count1 = 0;

    private ArrayList<String> vocabList;
    private ArrayList<String> testDisList;
    /** Creates a new instance of SearchEngine. (Constructor)
     * @param indexPath String: The path of the index the SearchEngine will use to find data
     * @param analyzer Analyzer: The analyzer that will be used by the SearchEngine
     */
    public SearchEngineNB(String indexPath, File compData, File trainSubset, File testSubset, Analyzer analyzer, String sim, int nOfDocs, int trainDCount, int testDCount, IndexerNB ind, IndexerNB trainInd, IndexerNB testInd) throws IOException {
        indexStats = ind;
        trainIndexStats = trainInd;
        testIndexStats = testInd;
        classList = new String[] {"emergency","seekmedical","wait"};
        classDocNums = new int[] {53, 70, 10};
        prior = new double[3];
        Path path = FileSystems.getDefault().getPath(indexPath);
        Directory direct = FSDirectory.open(path);
        IndexReader indexReader = DirectoryReader.open(direct);
        Fields fields =  MultiFields.getFields(indexReader);
        //System.out.println(fields.);
        Terms terms = fields.terms("paraText");
        //System.out.println(terms.toString());
        TermsEnum en = terms.iterator();
        vocabList = new ArrayList<>();
        testDisList = new ArrayList<>();
        while(en.next() != null){
            //vocabList.add(terms.toString());
            vocabLength++;
        }
        //("Vocab length:" + vocabLength);
        indexReader.close();
        trainRead = DirectoryReader.open(direct);
        //get vocab length with a different index reader, then close it
       /* IndexReader indexReader = DirectoryReader.open(direct);
        Fields fields =  MultiFields.getFields(indexReader);
        Terms terms = fields.terms("paraText");
        TermsEnum en = terms.iterator();
        while(en.next() != null){
            //System.out.println(Term.toString(en.term()));
            //vocabList.add(Term.toString(en.term()));
            vocabLength++;
        }
        //("Vocab length:" + vocabLength);
        indexReader.close();
        //okay, NOW initialize search engine elements



        */
        searcher = new IndexSearcher(trainRead);
        parser = new QueryParser("paraText", analyzer);
        similarity = sim;
        tDocCount = nOfDocs;

        //Path trainPath = FileSystems.getDefault().getPath(trainIndexPath);
        //Directory trainDirect = FSDirectory.open(trainPath);
        //get vocab length with a different index reader, then close it
        /*IndexReader trainIndexReader = DirectoryReader.open(trainDirect);
        Fields trainFields =  MultiFields.getFields(trainIndexReader);
        Terms trainTerms = fields.terms("paraText");
        TermsEnum trainEn = trainTerms.iterator();
        while(trainEn.next() != null){
            //System.out.println(Term.toString(en.term()));
            vocabList.add(Term.toString(trainEn.term()));
            trainVocabLength++;
        }
        //("Vocab length:" + vocabLength);
        trainIndexReader.close();
        //okay, NOW initialize search engine elements
        trainRead = DirectoryReader.open(trainDirect);
        trainSearcher = new IndexSearcher(trainRead);
        trainParser = new QueryParser("paraText", analyzer);
        trainSimilarity = sim;
        trainDocCount = trainDCount;

         */
        Scanner fileScan = new Scanner(compData);
        while(fileScan.hasNextLine()) {
            String current = fileScan.nextLine();
            Scanner lineScan = new Scanner(current);
            lineScan.useDelimiter(":");
            String disease = lineScan.next();
            Map m = indexStats.getDocMap();
            String t = (String)m.get(disease);
            String[] sym = t.split(",");
            for(int c = 0; c < sym.length;c++) {
                if(!vocabList.contains(sym[c])) {
                    vocabList.add(sym[c]);
                }
            }
        }
        /*
        fileScan = new Scanner(testSubset);
        while(fileScan.hasNextLine()) {
            String current = fileScan.nextLine();
            Scanner lineScan = new Scanner(current);
            lineScan.useDelimiter(":");
            String disease = lineScan.next();
            testDisList.add(disease);
        }
        System.out.println("Printing of test disease list starts");
        printList(testDisList);
        System.out.println("Printing of test disease list ends");*/

    }



    /**
     * Performs a search in the index based on a query.
     * Prints the number of top "hit" documents specified by n.
     * @param queryString: String to make into a query
     * @param n: Number of top docs to be printed
     * @return TopDocs
     * @throws IOException if search fails
     * @throws ParseException if query cannot be parsed for some reason
     */
    public TopDocs performSearch(String queryString, int n)
            throws IOException, ParseException {
        count1 = 0;
        //frequency = 0;
        Query query = parser.parse(queryString);
        if(similarity.equals("custom")){
            //Grad student similarity function from Assignment 1
            searcher.setSimilarity(new SimilarityBase() {
                protected float score(BasicStats basicStats, float freq, float docLen) {
                    return basicStats.getTotalTermFreq();
                }
                public String toString() {
                    return "";
                }

            });
        } else if(similarity.equals("bnn-bnn")){
            searcher.setSimilarity(new SimilarityBase() {
                protected float score(BasicStats basicStats, float freq, float docLen){
                    double Score = 0;
                    try {
                        if(count1<indexStats.gettDocCount()) {
                            Document curr = getDocument(count1);
                            String currDisease = curr.getField("id").toString();

                            Scanner lineScanner = new Scanner(currDisease);
                            lineScanner.useDelimiter(":");
                            String random = lineScanner.next();
                            String currDisease1 = lineScanner.next();
                            currDisease1 = currDisease1.substring(0, currDisease1.length() - 1);
                            //System.out.println(currDisease1);


                            String queryString = query.toString();
                            Scanner qScan = new Scanner(queryString);

                            while (qScan.hasNext()) {
                                String termString = qScan.next();
                                termString = termString.substring(9);
                                termString = termString.substring(0, 1).toUpperCase() + termString.substring(1);
                                //try {
                                Term t = new Term("paraText", termString);
                                float w_tq = 1;
                                //Now calculate term freq for each doc under it's posting list.
                                //Map m_tf_td = indexStats.getTermFrequencies();
                                //System.out.println(m_tf_td);
                                //Set<String> termFreq = (Set<String>) m_tf_td.get(t.toString());
                                Map m_dis = indexStats.getDocMap();
                                //System.out.println(m_dis.get(currDisease1));
                                String symptom = (String) m_dis.get(currDisease1);
                                float tf_td = 0;
                                //System.out.println("here1: "+t.toString());
                                //System.out.println("here2: "+termString);
                                if (symptom.contains(termString)) {
                                    tf_td = 1;
                                }
                                Map m_df = indexStats.getDocFrequencies();
                                Integer docFreq = (Integer) m_df.get(termString);
                                //System.out.println(docFreq);
                                //System.out.println(docFreq.intValue());
                                float idf = 1;

                                float wf_td = tf_td * idf;
                                Score += w_tq * wf_td;
                            }
                            count1++;
                        }
                    } catch(IOException e){
                        e.printStackTrace();
                    }
                    return (float)Score;
                }
                public String toString(){ return "bnn-bnn";}
            });
        } else if(similarity.equals("btn-btn")){
            searcher.setSimilarity(new SimilarityBase() {
                protected float score(BasicStats basicStats, float freq, float docLen){
                    double Score = 0;
                    try {
                        if(count1<indexStats.gettDocCount()) {
                            Document curr = getDocument(count1);
                            String currDisease = curr.getField("id").toString();

                            Scanner lineScanner = new Scanner(currDisease);
                            lineScanner.useDelimiter(":");
                            String random = lineScanner.next();
                            String currDisease1 = lineScanner.next();
                            currDisease1 = currDisease1.substring(0, currDisease1.length() - 1);
                            //System.out.println(currDisease1);


                            String queryString = query.toString();
                            Scanner qScan = new Scanner(queryString);

                            while (qScan.hasNext()) {
                                String termString = qScan.next();
                                termString = termString.substring(9);
                                termString = termString.substring(0, 1).toUpperCase() + termString.substring(1);
                                //try {
                                Term t = new Term("paraText", termString);
                                float w_tq = 1;
                                //Now calculate term freq for each doc under it's posting list.
                                //Map m_tf_td = indexStats.getTermFrequencies();
                                //System.out.println(m_tf_td);
                                //Set<String> termFreq = (Set<String>) m_tf_td.get(t.toString());
                                Map m_dis = indexStats.getDocMap();
                                //System.out.println(m_dis.get(currDisease1));
                                String symptom = (String) m_dis.get(currDisease1);
                                float tf_td = 0;
                                //System.out.println("here1: "+t.toString());
                                //System.out.println("here2: "+termString);
                                if (symptom.contains(termString)) {
                                    tf_td = 1;
                                }
                                Map m_df = indexStats.getDocFrequencies();
                                Integer docFreq = (Integer) m_df.get(termString);
                                //System.out.println(docFreq);
                                //System.out.println(docFreq.intValue());
                                float idf = 0;
                                if (docFreq!=null && docFreq > 0) {
                                    idf = (float)Math.log10(indexStats.gettDocCount()/docFreq);
                                }

                                float wf_td = tf_td * idf;
                                Score += w_tq * wf_td;
                            }
                            count1++;
                        }
                    } catch(IOException e){
                        e.printStackTrace();
                    }
                    return (float)Score;
                }
                public String toString(){ return "btn-btn";}
            });
        } else if(similarity.equals("bpn-bpn")){
            searcher.setSimilarity(new SimilarityBase() {
                protected float score(BasicStats basicStats, float freq, float docLen){
                    double Score = 0;
                    try {
                        if(count1<indexStats.gettDocCount()) {
                            Document curr = getDocument(count1);
                            String currDisease = curr.getField("id").toString();

                            Scanner lineScanner = new Scanner(currDisease);
                            lineScanner.useDelimiter(":");
                            String random = lineScanner.next();
                            String currDisease1 = lineScanner.next();
                            currDisease1 = currDisease1.substring(0, currDisease1.length() - 1);
                            //System.out.println(currDisease1);


                            String queryString = query.toString();
                            Scanner qScan = new Scanner(queryString);

                            while (qScan.hasNext()) {
                                String termString = qScan.next();
                                termString = termString.substring(9);
                                termString = termString.substring(0, 1).toUpperCase() + termString.substring(1);
                                //try {
                                Term t = new Term("paraText", termString);
                                float w_tq = 1;
                                //Now calculate term freq for each doc under it's posting list.
                                //Map m_tf_td = indexStats.getTermFrequencies();
                                //System.out.println(m_tf_td);
                                //Set<String> termFreq = (Set<String>) m_tf_td.get(t.toString());
                                Map m_dis = indexStats.getDocMap();
                                //System.out.println(m_dis.get(currDisease1));
                                String symptom = (String) m_dis.get(currDisease1);
                                float tf_td = 0;
                                //System.out.println("here1: "+t.toString());
                                //System.out.println("here2: "+termString);
                                if (symptom.contains(termString)) {
                                    tf_td = 1;
                                }
                                Map m_df = indexStats.getDocFrequencies();
                                Integer docFreq = (Integer) m_df.get(termString);
                                //System.out.println(docFreq);
                                //System.out.println(docFreq.intValue());
                                float idf = 0;
                                if (docFreq!= null && docFreq > 0) {
                                    idf = (float)Math.log10((indexStats.gettDocCount() - docFreq) / docFreq);
                                }

                                float wf_td = tf_td * idf;
                                Score += w_tq * wf_td;
                            }
                            count1++;
                        }
                    } catch(IOException e){
                        e.printStackTrace();
                    }
                    return (float)Score;
                }
                public String toString(){ return "bpn-bpn";}
            });
        }
        else{
            searcher.setSimilarity(new BM25Similarity());
        }
        return searcher.search(query, n);
    }

    /**
     * Gets a document from an IndexSearcher.
     * @param docId: ID of the document to grab from the IndexSearcher
     * @return Document
     * @throws IOException if getting the document fails
     */
    public Document getDocument(int docId) throws IOException {
        return searcher.doc(docId);
    }

    public void trainBernoulliNB() {

        condprob = new double[vocabList.size()][classList.length];
        for(int i = 0; i < classList.length; i++) {
            float Nc = classDocNums[i];
            //prior[i] = Nc/ trainDocCount;
            //System.out.println(Nc);
            //System.out.println(indexStats.gettDocCount());
            prior[i] = Nc/ trainIndexStats.gettDocCount();
            /*Map m = indexStats.getDocMap();
            String symptoms = (String)m.get(classList[i]);
            //String[] symptomArray = symptoms.split(",");
            for(int j = 0; j < vocabList.size(); j++) {

                int Nct = 0;//need this (Given a class, count the number of docs under that class, containing term t)
                condprob[j][i] = (Nct + 1.0)/(Nc + 2.0);
            }*/
            for(int j = 0; j < vocabList.size(); j++) {
                Integer Nct = 0;
                if(i == 0) {
                    Map m_Nct = trainIndexStats.getEmergMap();
                    Nct = (Integer)m_Nct.get(vocabList.get(j));
                    //System.out.println(vocabList.get(j));
                    //System.out.println(Nct);
                } else if(i == 1) {
                    Map m_Nct = trainIndexStats.getSeekMedMap();
                    Nct = (Integer)m_Nct.get(vocabList.get(j));
                } else {
                    Map m_Nct = trainIndexStats.getWaitMap();
                    Nct = (Integer)m_Nct.get(vocabList.get(j));
                }
                if(Nct!=null) {
                    condprob[j][i] = (Nct + 1.0) / (Nc + 2.0);
                }
            }
        }
        System.out.println("Classifier training complete");
    }

    public void testBernoulliNB(String[] d) {
        Map m = indexStats.getDocMap();
        double[] score = new double[classList.length];
        int[] indexArray = new int[d.length];
        printArray(d);
        for(int c = 0; c < d.length; c++) {
            //printArray(d[c]);
            String symptoms = (String) m.get(d[c]);
            //System.out.println(""+c+": "+symptoms);
            String[] termsFromD = symptoms.split(","); //probably done this???(list of symptoms from the dataset for disease d[c])
            //System.out.println(Arrays.toString(termsFromD));
            //printArray(termsFromD);
            for (int i = 0; i < classList.length; i++) {
                score[i] = Math.log10(prior[i]);
                for (int j = 0; j < vocabList.size(); j++) {
                    for (int k = 0; k < termsFromD.length; k++) {
                        if (termsFromD[k].equals(vocabList.get(j))) {
                            score[i] += Math.log10(condprob[j][i]);
                        } else {
                            score[i] += Math.log10(1 - condprob[j][i]);
                        }
                    }
                }
            }
            int index = indexOfMaxScore(score);
            indexArray[c] = index;
        }

        printStringArray(classList,indexArray);
    }
    public void testBernoulliNBWithArrayOutput(String[] d, String[] a) {
        Map m = indexStats.getDocMap();
        double[] score = new double[classList.length];
        int[] indexArray = new int[d.length];
        printArray(d);
        for(int c = 0; c < d.length; c++) {
            //printArray(d[c]);
            String symptoms = (String) m.get(d[c]);
            //System.out.println(""+c+": "+symptoms);
            String[] termsFromD = symptoms.split(","); //probably done this???(list of symptoms from the dataset for disease d[c])
            //System.out.println(Arrays.toString(termsFromD));
            //printArray(termsFromD);
            for (int i = 0; i < classList.length; i++) {
                score[i] = Math.log10(prior[i]);
                for (int j = 0; j < vocabList.size(); j++) {
                    for (int k = 0; k < termsFromD.length; k++) {
                        if (termsFromD[k].equals(vocabList.get(j))) {
                            score[i] += Math.log10(condprob[j][i]);
                        } else {
                            score[i] += Math.log10(1 - condprob[j][i]);
                        }
                    }
                }
            }
            int index = indexOfMaxScore(score);
            indexArray[c] = index;
        }

        createStringArray(classList,indexArray,a);
    }





    public int indexOfMaxScore(double[] s) {

        int index = 0;
        double max = s[0];
        for(int i = 0; i < s.length; i++) {
            if(s[i] >= max) {
                max = s[i];
                index = i;
            }
        }
        //System.out.println("Max:"+max);
        //System.out.println("index:"+index);
        return index;
    }

    public void printStringArray(String[] classList, int[] iArray) {
        for(int i = 0; i < iArray.length; i++) {
            System.out.println(""+ classList[iArray[i]]);
        }
    }

    public void createStringArray(String[] classList, int[] iArray, String[] d) {
        for(int i = 0; i < iArray.length; i++) {
            d[i] = ""+ classList[iArray[i]];
        }
    }

    public void printArray(Object[] o) {
        for(int i=0; i<o.length;i++) {
            System.out.println(o[i]);
        }
    }

    public void printList(ArrayList<String> a) {
        for(int i = 0; i < a.size(); i++) {
            System.out.println(a.get(i));
        }
    }
        
}