import java.io.Console;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.Scanner;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer; //contains Stemming for English
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.TFIDFSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.RAMDirectory;
import org.jsoup.Jsoup;

public class test {

	private static IndexWriter w;

	// returns a Document for the IndexWriter given a File
	public static Document getDocument(File f) throws Exception {

		org.jsoup.nodes.Document docjsoup = Jsoup.parse(f, "UTF-8");

		Document doc = new Document();

		Field title = new TextField("title", docjsoup.getElementsByTag("title").text(), Store.YES);
		Field filename = new TextField("filename", f.getName(), Store.YES);
		Field body = new TextField("body", docjsoup.getElementsByTag("body").text(), Store.YES);

		doc.add(title);
		doc.add(filename);
		doc.add(body);

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
	
	public static Date getLastModified(File directory) {
	    File[] files = directory.listFiles();
	    if (files.length == 0) return new Date(directory.lastModified());
	    Arrays.sort(files, new Comparator<File>() {
	        public int compare(File o1, File o2) {
	            return new Long(o2.lastModified()).compareTo(o1.lastModified()); //latest 1st
	        }});
	    return new Date(files[0].lastModified());
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

		// initiate analyzer and IndexWriter
		EnglishAnalyzer analyzer = new EnglishAnalyzer();
		Directory indexdir = FSDirectory.open(index_file.toPath());

		// if there is no index in the index_folder, create one
		int filecount=0;
		for(int i=0; i<indexdir.listAll().length; i++) {
//			if()
		}
		
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		
		//initialize reader and searcher
		IndexReader indexReader = DirectoryReader.open(indexdir);
		IndexSearcher searcher = new IndexSearcher(indexReader);
		if(args[2].equals("OK")) {
			BM25Similarity bm = new BM25Similarity();
			config.setSimilarity(bm);
			searcher.setSimilarity(bm);
		}
		else if(args[2].equals("VS")){
			ClassicSimilarity classic = new ClassicSimilarity();
			config.setSimilarity(classic);
			searcher.setSimilarity(classic);
		}
		else {
			System.out.println("Ranking Model can only be 'VS' for Vector-Space-Model or 'OK'for Okapi BM25");
			System.exit(-1);
		}
		
		if (indexdir.listAll().length == 0) {
			System.out.println("creating new index...");
			w = new IndexWriter(indexdir, config);

			searchSubDirectories(directory);
			w.close();
		}
		else {
			System.out.println("found an index in the index_folder");
		}

		// parsing the query
		String[] fields = { "title", "filename", "body" };
		MultiFieldQueryParser mfqp = new MultiFieldQueryParser(fields, analyzer);
		System.out.println("Parsing the QueryString:\t" + queryStr);

		Query mfquery = mfqp.parse(queryStr);
		System.out.println("with MultiFieldQueryParser to:\t" + mfquery.toString());

		System.out.println("--------Searching:---------");

		TopDocs hits = searcher.search(mfquery, Integer.MAX_VALUE);
		System.out.println("there are " + hits.totalHits + " hits:");
		System.out.println("\tDoc \t\tScore \t\t\tFilename");
		for (ScoreDoc hit : hits.scoreDocs) {
			System.out.println("\t" + hit.doc + "\t\t" + hit.score + "\t\t"
					+ indexReader.document(hit.doc).getField("filename").stringValue());

		}

		System.out.println("---------------------------");

	}
}
