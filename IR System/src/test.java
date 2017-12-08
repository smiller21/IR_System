import java.io.File;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import javax.swing.JTable;

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
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.jsoup.Jsoup;

public class test {

	private static IndexWriter w;

	// returns a Document for the IndexWriter given a File
	public static Document getDocument(File f) throws Exception {

		org.jsoup.nodes.Document docjsoup = Jsoup.parse(f, "UTF-8");

		Document doc = new Document();

		Field title = new TextField("title", docjsoup.getElementsByTag("title").text(), Store.YES);
		Field path = new TextField("path", f.getAbsolutePath(), Store.YES);
		Field filename = new TextField("filename", f.getName(), Store.YES);
		Field body = new TextField("body", docjsoup.getElementsByTag("body").text(), Store.YES);

		doc.add(title);
		doc.add(filename);
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
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		
		//set IndexWriter ranking model
		if(ranking_model.equals("OK")) {
			BM25Similarity bm = new BM25Similarity();
			config.setSimilarity(bm);
		}
		else if(ranking_model.equals("VS")){
			ClassicSimilarity classic = new ClassicSimilarity();
			config.setSimilarity(classic);
		}
		else {
			System.out.println("Ranking Model can only be 'VS' for Vector-Space-Model or 'OK'for Okapi BM25");
			System.exit(-1);
		}

		//check if index is empty and create a new one
		if (indexdir.listAll().length == 0) {
			System.out.println("creating new index...");
			w = new IndexWriter(indexdir, config);
			searchSubDirectories(directory);
			w.close();
		}
		//or use the existing index
		else {
			System.out.println("found an index in the index_folder");
		}
		
		//initialize reader and searcher
		IndexReader indexReader = DirectoryReader.open(indexdir);
		IndexSearcher searcher = new IndexSearcher(indexReader);
		
		//set searcher ranking model
		if(ranking_model.equals("OK")) {
			BM25Similarity bm = new BM25Similarity();
			searcher.setSimilarity(bm);
		}
		else if(ranking_model.equals("VS")){
			ClassicSimilarity classic = new ClassicSimilarity();
			searcher.setSimilarity(classic);
		}

		//initialize the parser
		String[] fields = { "title", "filename", "body" };
		MultiFieldQueryParser mfqp = new MultiFieldQueryParser(fields, analyzer);
		System.out.println("Parsing and Stemming the QueryString:\t" + queryStr);

		//parse query string
		Query mfquery = mfqp.parse(queryStr);
		System.out.println("with MultiFieldQueryParser and EnglishAnalyzer to: \n" + mfquery.toString());
		System.out.println("--------Searching:---------");

		//calculate scores
		TopDocs hits = searcher.search(mfquery, Integer.MAX_VALUE);
		System.out.println("there are " + hits.totalHits + " hits:"); //TODO: add summary
		System.out.format("%-10.10s \t %-30.30s \t %-15.15s \t %-100.100s", "Rank", "Title", "Score", "Path");
		for (ScoreDoc hit : hits.scoreDocs) {
			System.out.format("\n%-10d \t %-30.30s \t %-15.15s \t %-100.100s", 
					hit.doc, indexReader.document(hit.doc).getField("filename").stringValue(), 
					hit.score, indexReader.document(hit.doc).getField("path").stringValue());
		}

		System.out.println("\n---------------------------");
	}
}
