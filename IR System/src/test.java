import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.util.Iterator;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
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
	
	private IndexWriter writer;
	
	
	public static void main(String[] args) throws IOException {
		String directory = "/home/LuceneTest/documents";
		String index_path = "/home/LuceneTest/indices";
		String ranking_model = "VS";
		String query = "";
		
		StandardAnalyzer analyzer = new StandardAnalyzer();
		Directory index = new RAMDirectory();
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		IndexWriter w = new IndexWriter(index, config);
		Document doc = new Document();
		Field titleField = new TextField("Essensliste","Kartoffel, Apfel, Melone",Field.Store.YES);
		doc.add(titleField);
        w.addDocument(doc);
        System.out.println(doc.toString());
        w.close();
	}
}
