import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import static java.lang.Math.abs;
import static java.lang.Math.pow;

public class Spearman {
    //TODO: Handle case where a document is found in one but not the other.
    public static void main(String[] args) throws FileNotFoundException {
        Scanner input = new Scanner(System.in);
        System.out.println("Enter the directory where the trec run files are stored: ");
        String trecDirectory = input.nextLine();
        File def = new File( trecDirectory + "trecrunfile-default.txt");
        File que = new File( trecDirectory + "trecrunfile-default.txt");
        File tfidf = new File( trecDirectory + "trecrunfile-tfidf.txt");
        File bim= new File( trecDirectory + "trecrunfile-bim.txt");
        File nbayes = new File( trecDirectory + "trecrunfile-naivebayes.txt");
        File kmeans = new File( trecDirectory + "trecrunfile-kmeans.txt");
        File vsm = new File( trecDirectory + "trecrunfile-vectorspacemodel.txt");

        //this is a special scanner made for "looking ahead" for queries
        Scanner queryScanner = new Scanner(que);

        Scanner defScanner = new Scanner(def);
        Scanner tfidfScanner = new Scanner(tfidf);
        Scanner bimScanner = new Scanner(bim);
        Scanner nbayesScanner =  new Scanner(nbayes);
        Scanner kmeansScanner = new Scanner(kmeans);
        Scanner vsmScanner = new Scanner(vsm);

        String qu = "";
        String curqu = "";
        double avtfidf = 0.0;
        double avbim = 0.0;
        double avnbayes = 0.0;
        double avkmeans = 0.0;
        double avvsm = 0.0;
        double numQueries = 0.0;
        System.out.println("Calculating. Please wait...");

        while(queryScanner.hasNextLine()){ //assume all files have same num of lines
            //increment number of queries
            numQueries++;
            //get next line of the query scanner
            String quln = queryScanner.nextLine();
            Scanner querScanner = new Scanner(quln);
            curqu = querScanner.next();

            qu = curqu;

            //begin a running sum for the d^2 of each query
            double sum = 0.0;
            double sumtfidf = 0.0;
            double sumbim = 0.0;
            double sumnbayes = 0.0;
            double sumkmeans = 0.0;
            double sumvsm = 0.0;
            //Begin a count of num pairs per query
            double numPairs = 0;

            //Loop to calculate Spearman's for current query.
            while( qu.equals(curqu) && defScanner.hasNextLine() && queryScanner.hasNextLine()&& nbayesScanner.hasNextLine()
                    && tfidfScanner.hasNextLine() && bimScanner.hasNextLine() && kmeansScanner.hasNextLine()
                    && vsmScanner.hasNextLine()){
                //get next line of files
                String nextdef = defScanner.nextLine();
                String nexttfidf = tfidfScanner.nextLine();
                String nextbim = bimScanner.nextLine();
                String nextnbayes = nbayesScanner.nextLine();
                String nextkmeans = kmeansScanner.nextLine();
                String nextvsm = vsmScanner.nextLine();
                //line scanners to get ranks
                Scanner lineScanner = new Scanner(nextdef);
                Scanner tfidfLine = new Scanner(nexttfidf);
                Scanner bimLine = new Scanner(nextbim);
                Scanner nbayesLine = new Scanner(nextnbayes);
                Scanner kmeansLine = new Scanner (nextkmeans);
                Scanner vsmLine = new Scanner(nextvsm);

                //throw away query, Q0 and ID for each line
                lineScanner.next();
                lineScanner.next();
                lineScanner.next();
                tfidfLine.next();
                tfidfLine.next();
                tfidfLine.next();
                bimLine.next();
                bimLine.next();
                bimLine.next();
                nbayesLine.next();
                nbayesLine.next();
                nbayesLine.next();
                kmeansLine.next();
                kmeansLine.next();
                kmeansLine.next();
                vsmLine.next();
                vsmLine.next();
                vsmLine.next();

                //since we are comparing to default, get default and subtract it from each rank in files
                double comp = lineScanner.nextDouble();
                double tfidfd = tfidfLine.nextDouble();
                double bimd = bimLine.nextDouble();
                double nbayesd = nbayesLine.nextDouble();
                double kmeansd = kmeansLine.nextDouble();
                double vsmd = vsmLine.nextDouble();

                sumtfidf += pow(tfidfd - comp, 2);
                sumbim += pow(bimd - comp, 2);
                sumnbayes += pow(nbayesd - comp, 2);
                sumkmeans += pow(kmeansd - comp, 2);
                sumvsm += pow(vsmd - comp, 2);
                numPairs++;

                //set qu now so we know if we should continue with this query loop
                quln = queryScanner.nextLine();
                querScanner = new Scanner(quln);
                qu = querScanner.next();

            }
            //add to average...
            avtfidf += ( 6 * sumtfidf )/ (numPairs * ( pow( numPairs, 2) - 1));
            avbim += ( 6 * sumbim )/ (numPairs * ( pow( numPairs, 2) - 1));
            avnbayes += ( 6 * sumnbayes )/ (numPairs * ( pow( numPairs, 2) - 1));
            avkmeans += ( 6 * sumkmeans )/ (numPairs * ( pow( numPairs, 2) - 1));
            avvsm += ( 6 * sumvsm )/ (numPairs * ( pow( numPairs, 2) - 1));
            // System.out.println(qu);

        }
        System.out.println("SPEARMAN RANK COEFFICIENT RESULTS");
        System.out.println("tfidf: " + avtfidf/numQueries);
        System.out.println("bim: " + avbim/numQueries);
        System.out.println("nbayes: " + avnbayes/numQueries);
        System.out.println("k-means: " + avkmeans/numQueries);
        System.out.println("vector space model: " + avvsm/numQueries);
    }

}



