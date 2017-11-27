import java.io.Console;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Scanner;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;  //contains Stemming for English
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.RAMDirectory;

public class test {
	
	private static IndexWriter w;
	
	//returns a Document for the IndexWriter given a File
	public static Document getDocument(File f) throws Exception {
		  Document doc = new Document();
		  Scanner scan = new Scanner(f).useDelimiter("\\A");
		  Field contents = new TextField("contents", scan.next() , Store.YES); 
		  Field filename = new TextField("filename", f.getName(), Store.YES);
		  scan.close();
		  doc.add(contents);
		  doc.add(filename);
		  System.out.println("'" + f.getName() + "'" + " parsed");
		  return doc;
	}
	
	// indexes HTML-files in the directory and its sub-directories
	public static void searchSubDirectories(File dir) throws Exception {
		File[] directoryListing = dir.listFiles();
		if (directoryListing != null)
		{
			for (File child : directoryListing) {
				if(child.isDirectory()) {
					searchSubDirectories(child);
				}
				else
				{ 
					if(child.getName().endsWith(".html")) {
					Document doc = getDocument(child);
					w.addDocument(doc);
					}
				}
		    }
		} 
		else {
			System.out.println("first argument needs to be a directory");  
		}
	}
	
	public static void main(String[] args) throws Exception {
		if(args.length!=4){
			System.out.println("insufficient Arguments");
			System.exit(-1);
		}
		//save and convert Arguments 
		File directory = new File(args[0]);
		File index_file = new File(args[1]);
		String ranking_model = args[2];
		String query = args[3];
		
		//initiate analyzer and IndexWriter
		EnglishAnalyzer analyzer = new EnglishAnalyzer();
		Directory index = FSDirectory.open(index_file.toPath());
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		w = new IndexWriter(index, config);
		
		File dir = new File(args[0]);
		searchSubDirectories(dir);
        w.close();
	}
}
