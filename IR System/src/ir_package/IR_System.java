package ir_package;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Formatter;
import java.util.Iterator;
import java.util.Scanner;

import org.apache.lucene.analysis.en.EnglishAnalyzer; //contains Stemming for English
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;

import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Fragmenter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleFragmenter;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.SimpleSpanFragmenter;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.jsoup.Jsoup;

public class IR_System {

	private static IndexWriter w;

	// returns a Document for the IndexWriter given a File
	public static Document getDocument(File f) throws Exception {

		org.jsoup.nodes.Document docjsoup = Jsoup.parse(f, "UTF-8");

		Document doc = new Document();

		Field title = new TextField("title", docjsoup.getElementsByTag("title").text(), Store.YES);
		Field path = new TextField("path", f.getAbsolutePath(), Store.YES);
		Field body = new TextField("body", docjsoup.getElementsByTag("body").text(), Store.YES);

		doc.add(title);
		doc.add(body);
		doc.add(path);

		System.out.println("'" + f.getName() + "'" + " parsed");

		return doc;
	}

	// indexes HTML-files in the directory and its sub-directories
	public static void searchSubDirectories(File dir) throws Exception {
		File[] directoryListing = dir.listFiles();
		if (directoryListing != null) {
			for (File child : directoryListing) {
				if (child.isDirectory()) {
					searchSubDirectories(child);
				} else {
					if (child.getName().endsWith(".html")) {
						Document doc = getDocument(child);
						w.addDocument(doc);
					}
				}
			}
		} else {
			System.out.println("first argument needs to be a directory");
			System.exit(-1);
		}
	}
	
	public static void ReuseOrCreateIndex(File documentdir, Directory indexdir, EnglishAnalyzer analyzer) throws IOException, Exception {
		// check whether an index already exists and create a new one...
		if (!DirectoryReader.indexExists(indexdir)){
			System.out.println("creating new index...");
			IndexWriterConfig config = new IndexWriterConfig(analyzer);
			w = new IndexWriter(indexdir, config);
			searchSubDirectories(documentdir);
			w.close();
		}
		// ...or use the existing index
		else {
			System.out.println("found an index in the index_folder");
			System.out.println("make sure that your document collection didn't changed or was modified");
			System.out.println("Do you want to use this index? [y/n]");
			Scanner s = new Scanner(System.in);
			String answer = s.nextLine();
			s.close();
			if(answer.equals("y")) {
			}
			else {
				System.out.println("next time: choose a different index folder that doesn't contain an index or contains the index you want to use");
				System.exit(-1);
			}
		}
	}

	public static void main(String[] args) throws Exception {
		if (args.length < 4) {
			System.out.println("Please type all four required arguments");
			System.exit(-1);
		}

		// save and convert Arguments
		File directory = new File(args[0]);
		File index_file = new File(args[1]);
		String ranking_model = args[2];
		String queryStr = "";
		for (int i = 3; i < args.length; i++)
			queryStr += args[i] + " ";

		// initiate analyzer
		EnglishAnalyzer analyzer = new EnglishAnalyzer();
		Directory index_dir = FSDirectory.open(index_file.toPath());
		
		BM25Similarity bm = new BM25Similarity();
		ClassicSimilarity classic = new ClassicSimilarity();

		ReuseOrCreateIndex(directory, index_dir, analyzer);
		
		// initialize reader and searcher
		IndexReader indexReader = DirectoryReader.open(index_dir);
		IndexSearcher searcher = new IndexSearcher(indexReader);

		// set searcher ranking model
		if (ranking_model.equals("OK")) {
			searcher.setSimilarity(bm);
		} else if (ranking_model.equals("VS")) {
			searcher.setSimilarity(classic);
		} else {
			System.out.println("Third Argument needs to be either 'VS' for Vector-Space Model or 'OK' for Okapi BM25");
			System.exit(-1);
		}

		// initialize the parser
		String[] fields = { "title", "body" };
		MultiFieldQueryParser mfqp = new MultiFieldQueryParser(fields, analyzer);
		
		Query mfquery = mfqp.parse(queryStr);	// parse query string

		// calculate scores and highlight
		TopDocs hits = searcher.search(mfquery, 10);

		// initializing a Formatter, QueryScorer and Highlighter
		SimpleHTMLFormatter formatter = new SimpleHTMLFormatter("", ""); // using empty strings to avoid HTML-tags in the Output
		QueryScorer scorer = new QueryScorer(mfquery);
		Highlighter highlighter = new Highlighter(formatter, scorer);
		
		// initializing a Fragmenter with fragment size of 80
		Fragmenter fragmenter = new SimpleFragmenter(80);
		
		// set fragmenter to highlighter
		highlighter.setTextFragmenter(fragmenter);
		
		// rank and print results
		System.out.println("there are " + hits.totalHits + " hits total:");
		System.out.println("--- Top 10 search results ------- Query: "+queryStr+"------");
		int rank = 1;
		for (ScoreDoc hit : hits.scoreDocs) {			
			Document doc = indexReader.document(hit.doc);
			System.out.println("Rank \tTitle");
			System.out.println(rank + "\t" + doc.get("title") + "\t");
			rank++;										//documents are already sorted by rank
			System.out.println("\tSummary");
			String bodyStr = doc.get("body");
			String[] frags = highlighter.getBestFragments(analyzer, "body", bodyStr, 3); // the best 3 fragments in the
																							// field "body"
			for (String frag : frags) {
				System.out.println("\t... " + frag + " ...");
			}
			System.out.println("\tScore \t\tPath");
			System.out.println("\t" + hit.score + "\t" + doc.get("path"));
			System.out.println();
		}
	}
}
