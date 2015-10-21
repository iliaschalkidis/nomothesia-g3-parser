/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.di.nomothesiag3parser;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.openrdf.OpenRDFException;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.http.HTTPRepository;


/**
 *
 * @author Kiddo
 */
public class EntityIndex {
    
    IndexWriter indexWriter;
    Directory directory;
    
    public EntityIndex() throws IOException{
//         Path path = Paths.get("C:/Users/Ilias/Desktop/lucene/indexes/entities");
//         directory = FSDirectory.open(path);
//         IndexWriterConfig config = new IndexWriterConfig(new SimpleAnalyzer());        
//         indexWriter = new IndexWriter(directory, config);
//         indexWriter.deleteAll();
    }
    
    public void closeIndex() throws IOException{
        indexWriter.close();
        directory.close();
    }
    String capitalize(String title){
        
        String caps_title = "";
        int count =0;
        while(count<title.length()){
            switch(title.charAt(count)){
                case 'α':
                case 'ά': caps_title += 'Α';
                          break;
                case 'β': caps_title += 'Β';
                          break;
                case 'γ': caps_title += 'Γ';
                          break;
                case 'δ': caps_title += 'Δ';
                          break;
                case 'ε':
                case 'έ': caps_title += 'Ε';
                          break;
                case 'ζ': caps_title += 'Ζ';
                          break;
                case 'η':
                case 'ή': caps_title += 'Η';
                          break;
                case 'θ': caps_title += 'Θ';
                          break;
                case 'ι':
                case 'ί': caps_title += 'Ι';
                          break;
                case 'κ': caps_title += 'Κ';
                          break;
                case 'λ': caps_title += 'Λ';
                          break;
                case 'μ': caps_title += 'Μ';
                          break;
                case 'ν': caps_title += 'Ν';
                          break;
                case 'ξ': caps_title += 'Ξ';
                          break;
                case 'ο': 
                case 'ό': caps_title += 'Ο';
                          break;
                case 'π': caps_title += 'Π';
                          break;
                case 'ρ': caps_title += 'Ρ';
                          break;
                case 'σ': 
                case 'ς': caps_title += 'Σ';
                          break;   
                case 'τ': caps_title += 'Τ';
                          break;
                case 'υ': 
                case 'ύ': caps_title += 'Υ';
                          break;
                case 'φ': caps_title += 'Φ';
                          break;
                case 'χ': caps_title += 'Χ';
                          break;
                case 'ψ': caps_title += 'Ψ';
                          break;
                case 'ω': 
                case 'ώ': caps_title += 'Ω';
                          break;   
                default:  caps_title += title.charAt(count);
            }
            count++;
        }
        return caps_title;
    }
    
    void findPlaces() throws IOException{

        String sesameServer = "http://localhost:8080/openrdf-sesame";
        String repositoryID = "legislation";

        // Connect to Sesame
        Repository repo = new HTTPRepository(sesameServer, repositoryID);
        
        try {
            repo.initialize();
        } catch (RepositoryException ex) {
            Logger.getLogger(EntityIndex.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        TupleQueryResult result;
        
        try {
           
            RepositoryConnection con = repo.getConnection();
            
            try {
                
                String queryString = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
                "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
                "\n" +
                "SELECT DISTINCT ?name ?id \n" +
                "WHERE{\n" +
                "?id <http://geo.linkedopendata.gr/gag/ontology/έχει_επίσημο_όνομα> ?name." +
                "}\n" ;
                
                //System.out.println(queryString);
                TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
                result = tupleQuery.evaluate();

                try {
                    // iterate the result set
                    while (result.hasNext()) {
                        
                        BindingSet bindingSet = result.next();
                        String name = bindingSet.getValue("name").toString();
                        name = name.replace("^^<http://www.w3.org/2001/XMLSchema#string>", "");
                        name = trimDoubleQuotes(name);
                        String id = bindingSet.getValue("id").toString();
                        addDoc(indexWriter,name,id);
                        System.out.println(name);
                    }
                    
                }
                finally {
                    result.close();
                }    
            }
            finally {
                con.close();
            }
        }
        catch (OpenRDFException e) {
            // handle exception
        }
    }
    
    void findOrganizations() throws IOException{

        String sesameServer = "http://localhost:8080/openrdf-sesame";
        String repositoryID = "legislation";

        // Connect to Sesame
        Repository repo = new HTTPRepository(sesameServer, repositoryID);
        
        try {
            repo.initialize();
        } catch (RepositoryException ex) {
            Logger.getLogger(EntityIndex.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        TupleQueryResult result;
        
        try {
           
            RepositoryConnection con = repo.getConnection();
            
            try {
                
                String queryString = "PREFIX pb: <http://geo.linkedopendata.gr/public-buildings/ontology/>\n" +
                "\n" +
                "SELECT DISTINCT ?name ?id \n" +
                "WHERE{\n" +
                "?id pb:έχει_όνομα_υπηρεσίας ?name." +
                "}\n" ;
                
                //System.out.println(queryString);
                TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
                result = tupleQuery.evaluate();

                try {
                    // iterate the result set
                    while (result.hasNext()) {
                        
                        BindingSet bindingSet = result.next();
                        String name = bindingSet.getValue("name").toString();
                        name = name.replace("^^<http://www.w3.org/2001/XMLSchema#string>", "");
                        name = trimDoubleQuotes(name);
                        String id = bindingSet.getValue("id").toString();
                        addDoc(indexWriter,name,id);
                        System.out.println(name);
                    }
                    
                }
                finally {
                    result.close();
                }    
            }
            finally {
                con.close();
            }
        }
        catch (OpenRDFException e) {
            // handle exception
        }
    }
    
    void findPeople() throws IOException{

        String sesameServer = "http://localhost:8080/openrdf-sesame";
        String repositoryID = "dbpedia";

        // Connect to Sesame
        Repository repo = new HTTPRepository(sesameServer, repositoryID);
        
        try {
            repo.initialize();
        } catch (RepositoryException ex) {
            Logger.getLogger(EntityIndex.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        TupleQueryResult result;
        
        try {
           
            RepositoryConnection con = repo.getConnection();
            
            try {
                
                String queryString = "PREFIX ontology: <http://dbpedia.org/ontology/>\n" +
                "PREFIX prop: <http://el.dbpedia.org/property/>\n" +
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "\n" +
                "select distinct ?id ?name ?image\n" +
                "where {\n" +
                "?id rdf:type ontology:Politician.\n" +
                "?id foaf:name ?name.\n" +
                "\n" +
                "{?id prop:εθνικότητα <http://el.dbpedia.org/resource/Έλληνας>} UNION {?id prop:εθνικότητα \"Ελληνική\"@el} UNION {?id prop:εθνικότητα <http://el.dbpedia.org/resource/Έλληνες>}\n" +
                "\n" +
                "}";
                
                //System.out.println(queryString);
                TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
                result = tupleQuery.evaluate();

                try {
                    // iterate the result set
                    while (result.hasNext()) {
                        
                        BindingSet bindingSet = result.next();
                        String name = bindingSet.getValue("name").toString();
                        name = name.replace("^^<http://www.w3.org/2001/XMLSchema#string>", "");
                        name = this.capitalize(trimDoubleQuotes(name.replace("@el", "")));
                        String id = bindingSet.getValue("id").toString().replace("resource", "page");
                        addDoc(indexWriter,name,id);
                        System.out.println(name);
                    }
                    
                }
                finally {
                    result.close();
                }    
            }
            finally {
                con.close();
            }
        }
        catch (OpenRDFException e) {
            // handle exception
        }
    }
    
    public String tagEntity(String ent){
        return "";
    }
    
    
    
    public static String trimDoubleQuotes(String text) {
        
        int textLength = text.length();
        
        if (textLength >= 2 && text.charAt(0) == '"' && text.charAt(textLength - 1) == '"') {
            return text.substring(1, textLength - 1);
        }
        
        return text;
    
    }
    
    public String searchEntity(String text) {   
        //Apache Lucene searching text inside .txt files
        String uri = "";
        try {   
            Path path = Paths.get("C:/Users/Ilias/Desktop/lucene/indexes/entities");
            Directory directory2 = FSDirectory.open(path);       
            IndexReader indexReader =  DirectoryReader.open(directory2);
            IndexSearcher indexSearcher = new IndexSearcher(indexReader);
            QueryParser queryParser = new QueryParser("entity",  new StandardAnalyzer());
            String text2 = "\"" + text + "\"~"; //text.replace(" ", "~ ").replace("-~", "-") + "~";
            Query query = queryParser.parse(text2);
            TopDocs topDocs = indexSearcher.search(query,10);
            if(topDocs.totalHits>0){
               
                //System.out.print(" | TOTAL_HITS: " + topDocs.totalHits);
                for (ScoreDoc scoreDoc : topDocs.scoreDocs) { 
                    if(scoreDoc.score>3){
                        Document document = indexSearcher.doc(scoreDoc.doc);
                        float query_size = (float) text.length();
                        float entity_size = (float) document.get("entity").length();
                        float limit = query_size/entity_size;
                        if(text.contains(" ") && limit >=0.8){
                            System.out.print("QUERY: " + text);
                            System.out.print(" | RESULT: " + document.get("entity"));
                            System.out.println(" | SCORE: " +scoreDoc.score);
                            System.out.println(" | LIMIT: " + limit);
                            uri = document.get("uri");
                        }
                        else if(limit >=0.7){
                            System.out.print("QUERY: " + text);
                            System.out.print(" | RESULT: " + document.get("entity"));
                            System.out.println(" | SCORE: " +scoreDoc.score);
                            System.out.println(" | LIMIT: " + limit);
                            uri = document.get("uri");
                        }
                    }
                    break;
                }
            }
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
        
        return uri;
    }
    
     private void addDoc(IndexWriter w, String entity, String uri) throws IOException {
      Document doc = new Document();
      doc.add(new TextField("entity", entity, Store.YES));
      doc.add(new TextField("uri", uri, Store.YES));
      indexWriter.addDocument(doc);
     }
}
