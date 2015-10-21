/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.di.nomothesiag3parser;

import com.di.nomothesia.model.GovernmentGazette;
import com.di.nomothesia.model.LegalDocumentPro;
import java.nio.file.Files;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openrdf.OpenRDFException;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.rio.RDFFormat;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.http.HTTPRepository;
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
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.document.Field.Store;
/**
 *
 * @author kiddo
 */
public class FEKParser {

    

    String FEK;
    ArrayList<LegalDocumentPro> legaldocs;
    LegalDocumentPro legaldoc;
    ArrayList<String> mods;
    int mod_count;
    PrintWriter writer2;
    String baseURI;
    String baseURI2;
    String date;
    int legal_sum;
    int legal_ok;
    EntityIndex ei;
    int image_count;
    int art_count;
    int chap_count;
    int issue_number;
    int missing_mods;
    String fileName;
    String folderName;
    ArrayList<File> imageFiles;
    ArrayList<Integer> pages;
    String PDF;
    IndexWriter iw;
    String type;
    
    public FEKParser(String folder_name,String name,EntityIndex eindex,IndexWriter indexwriter){
        legaldocs = new ArrayList<>();
        legaldoc = new LegalDocumentPro();
        mods = new ArrayList<>();
        mod_count =1;
        legal_sum = 0;
        legal_ok = 0;
        ei = eindex;
        fileName = name.replace(".txt", "");
        folderName = folder_name;
        pages = new ArrayList();
        baseURI = "http://legislation.di.uoa.gr/";
        imageFiles = new ArrayList();
        iw =indexwriter;
        type = "";
        try {
            File file2 = new File("n3/"+folder_name+"/"+name.replace(".pdf.txt","")+".n3");
            file2.getParentFile().mkdirs();
            writer2 = new PrintWriter(file2);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(FEKParser.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    void readFile(String folder,String fileName) throws Exception {
        //ArrayList<String> images = searchImages();
        
        BufferedReader br = new BufferedReader(new InputStreamReader( new FileInputStream("C://Users/Ilias/Desktop/FEK/"+folder+"/txt/"+fileName), "UTF-8"));
        try {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();
            String pre_line = "";
            String mod = "";
            String pre_mod_line = "";
            int mod_line = 0;
            int page=0;
            int first_page =0;
            while (line != null) {
                
                if(line.isEmpty()||line.matches("(Το παρόν ΦΕΚ επανεκτυπώθηκε λόγω παράλειψης)")||line.matches("Το παρόν ΦΕΚ επανεκτυπώθηκε λόγω σφαλμάτων στο αρχικό ακριβές αντίγραφο")||line.matches("ΔΙΟΡΘΩΣΕΙΣ ΣΦΑΛΜΑΤΩΝ")||line.matches("ΑΝΑΚΟΙΝΩΣΕΙΣ")||line.matches("ΑΠΟΦΑΣΕΙΣ")||line.matches("ΤΕΥΧΟΣ ΠΡΩΤΟ")||line.matches("Το παρόν ΦΕΚ επανεκτυπώθηκε λόγω λάθους")||line.matches("Το παρόν ΦΕΚ επανεκτυπώθηκε λόγω λάθους(.*)")||line.equals("ΕΦΗΜΕΡΙΣ ΤΗΣ ΚΥΒΕΡΝΗΣΕΩΣ (ΤΕΥΧΟΣ ΠΡΩΤΟ)")||line.matches("\\p{C}\\bΕΦΗΜΕΡΙΣ ΤΗΣ ΚΥΒΕΡΝΗΣΕΩΣ (ΤΕΥΧΟΣ ΠΡΩΤΟ)\\b")|line.equals("")||line.equals("ΑΠΟΦΑΣΕΙΣ")){
                }
                else if(line.matches("\\p{C}[0-9]+")||line.matches("[0-9]+")){
//                    if(line.matches("\\p{C}[0-9]+")){
//                        line=line.replaceAll("\\p{C}", "");
//                    }
//                    if(first_page==0){
//                        first_page= Integer.parseInt(line.replace("[", "").replace("]",""));
//                        //System.out.println("FIRST PAGE: "+first_page);
//                    }
//                    page = Integer.parseInt(line.replace("[", "").replace("]",""));
//                    for(int i=0; i<images.size();i++){
//                        int image_page = first_page + Integer.parseInt(images.get(i)) -2;
//                        if (image_page==page){
//                            //System.out.println("FOUND IMAGE IN PAGE:"+ page);
//                            sb.append("[PHOTO_").append(image_page).append("]");
//                            sb.append("\n");
//                            //images.remove(i);
//                            pages.add(image_page);
//                            break;
//                        }
//                    }
                }
                else{
                    sb.append(line);
                    sb.append("\n");
                }
                pre_line =  line;
                line = br.readLine();
            }
            FEK = sb.toString();
        } finally {
            
            br.close();
        }
        
    }
    
    void linkFEK() throws IOException{
        File pdf;
        File fek= new File("pdf/"+folderName+"/GG"+legaldoc.getYear()+"_"+legaldoc.getFEK()+".pdf");
        //System.out.println("PLACE PDF: "+ fileName);
        if(fileName.contains(".pdf")){
          pdf= new File("C://Users/Ilias/Desktop/FEK/"+folderName+"/pdf/"+fileName);
        }
        else{
          pdf= new File("C://Users/Ilias/Desktop/FEK/"+folderName+"/pdf/"+fileName+".pdf");
        }
        Files.copy(pdf.toPath(), fek.toPath());
        writer2.println("<http://legislation.di.uoa.gr/gazette/a/"+legaldoc.getYear()+"/"+legaldoc.getFEK()+"> <http://legislation.di.uoa.gr/ontology/pdfFile> \"GG"+legaldoc.getYear()+"_"+legaldoc.getFEK()+".pdf\".");

    }
    
    void parseSigners(String input){
        String text = "";
        input = input.replace("−", "-");
        String[] lines = input.split("\n");
        int sign_count = 1;
        for(int z=0; z<lines.length; z++){
            if(lines[z].matches("([Α-Ω]|\\.| |-)+")&&lines[z].contains(" ")&&!lines[z].matches("(.*)(ΟΙΚΟΝΟΜΙΚΩΝ|ΥΠΗΡΕΣΙΕΣ|ΠΟΛΙΤΩΝ|ΤΕΧΝΙΚΕΣ|ΤΕΛΙΚΕΣ|ΔΙΟΡΘΩΣΕΙΣ|ΜΕΛΗ|ΥΠΟΥΡΓΟΣ|ΥΠΟΥΡΓΟΙ|ΥΦΥΠΟΥΡΓΟΙ|ΚΟΙΝΩΝΙΚΗΣ|ΑΝΑΠΛΗΡΩΤΗΣ|ΥΦΥΠΟΥΡΓΟΣ|ΠΡΟΕΔΡΟΣ|ΓΕΝΙΚΟΣ|ΔΗΜΟΣΙΑΣ|ΕΘΝΙΚΗΣ|ΕΘΝΙΚΟ|ΔΙΟΙΚΗΤΙΚΗΣ|ΑΝΘΡΩΠΙΝΩΝ|ΚΛΙΜΑΤΙΚΗΣ|ΝΗΣΙΩΤΙΚΗΣ|ΚΑΤΑΛΟΓΟΣ|ΚΑΝΟΝΙΣΤΙΚΕΣ|ΠΑΡΑΡΤΗΜΑ|ΚΑΙ|ΑΜΥΝΑΣ|ΕΘΝΙΚΗΣ|ΗΛΕΚΤΡΟΝΙΚΗΣ|ΑΓΡΟΤΙΚΗΣ|ΔΙΟΙΚΗ|ΔΙΑΒΑΘΜΙΣΜΕ|ΕΜΠΟΡΙΚΗΣ|ΣΥΜΦΩΝΙΑ|ΤΗΣ )(.*)")){
                //lines[z].matches("[Α-Ω]+ [Α-Ω]+")||lines[z].matches("[Α-Ω]+ - [Α-Ω]+ [Α-Ω]+"))&& //System.out.println("SIGNER: " +lines[z]);
                writer2.println("<"+baseURI+"> <http://legislation.di.uoa.gr/ontology/signer>  <"+baseURI+"/signer/"+sign_count+">.\n" +
                "<"+baseURI+"/signer/"+sign_count+"> <http://xmlns.com/foaf/0.1/name> \""+lines[z]+"\"@el.");
                String uri = "";
                uri= this.tagEntity(lines[z]);
                if(!uri.isEmpty()){
                    writer2.println("<"+baseURI+"/signer/"+sign_count+"> <http://legislation.di.uoa.gr/ontology/html> \"<a href=\\\""+uri+"\\\">"+lines[z]+"</a>\".");
                }
                sign_count++;
            } 
        }
        
        if(sign_count==1){
            writer2.println("<"+baseURI2+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI2+"/issue/"+issue_number+">.");
            writer2.println("<"+baseURI2+"/issue/"+issue_number+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/ParsingIssue>.");
            writer2.println("<"+baseURI2+"/issue/"+issue_number+"> <http://legislation.di.uoa.gr/ontology/text> \"Δεν αναγνωρίστηκαν οι υπογραφόντες.\"@el.");
            issue_number++;
        }
                
    }
    
    ArrayList<String> searchImages(){
        File folder = new File("C://Users/Ilias/Desktop/FEK/"+folderName+"/images/");
        File[] files = folder.listFiles();
        ArrayList<String> images = new ArrayList();
        int page_count = 0;
        for(int i=0; i<files.length;i++){
            if (files[i].getName().matches(fileName+"-[0-9]+_1\\.(png|jpg)")){
                //System.out.println("ΙΜΑGE: "+imageFiles[i].getName());
                imageFiles.add(files[i]);
                
            }
        }
        
        Collections.sort(imageFiles, new FileComparator());
        
        for(int i=0; i<imageFiles.size();i++){
                String page = imageFiles.get(i).getName().replaceFirst("(.*)R(\\.pdf)*-", "").replaceAll("_1\\.(png|jpg)", "");
                images.add(page);
                //System.out.println("PAGE: "+page);
        }
        return images;
    }
    
    void parseLegalDocument() throws IOException{

      // Tweak for 2001-2005 GGs
      //FEK = FEK.replace("´", "’");
      // Split between multiple legal documents
      String [] legaldocuments = FEK.split("\n\\([0-9]+\\)\n");
      int limit=0;
      int start = 0;
      
      // Set the apropriate variables if there ara multiple legal documents or not
      if(legaldocuments.length>2){
          // Crop FEK information
          FEK = legaldocuments[0].split("ΠΕΡΙΕΧΟΜΕΝΑ\n")[0];
          legal_sum = legaldocuments.length-1;
          limit = legaldocuments.length;
          start = 1;
      }
      else{
          // Check if there are Acts of Ministerial Cabinets
          if(FEK.contains("ΠΡΑΞΕΙΣ ΥΠΟΥΡΓΙΚΟΥ ΣΥΜΒΟΥΛΙΟΥ")){
              legaldocuments = FEK.split("\nΠράξη ");
              FEK = FEK.split("ΠΡΑΞΕΙΣ ΥΠΟΥΡΓΙΚΟΥ ΣΥΜΒΟΥΛΙΟΥ")[0];
              start = 0;
              limit = legaldocuments.length;
              legal_sum = legaldocuments.length;
          }
          else{
            limit = 2;
            start = 1;
            legal_sum = 1;
          }
      }
      
      // Split in order to capture FEK information
      String[] buffers = FEK.split("Αρ. Φύλλου ");
      buffers = buffers[1].split("\n",2);
      GovernmentGazette  gg = new GovernmentGazette();
      gg.setId(buffers[0]);
      legaldoc.setFEK(gg.getId());
      gg.setIssue("ΠΡΏΤΟ");
      FEK = buffers[1];
      if(FEK.startsWith("ΤΕΥΧΟΣ ΠΡΩΤΟ\n")){
          FEK = FEK.replaceAll("ΤΕΥΧΟΣ ΠΡΩΤΟ\n", "");
      }
      buffers = FEK.split("\n",2);
      FEK = buffers[1].replace("´", "’");
      buffers[0] = buffers[0].replace("´", "’");
      // Capture FEK Date
      String[] Date = buffers[0].split(" ");
      date = Date[2];
      switch (Date[1]) {
            case "Ιανουαρίου":  date +="-01";
                     break;
            case "Φεβρουαρίου":  date +="-02";
                     break;
            case "Μαρτίου":   date +="-03";
                     break;
            case "Απριλίου":   date +="-04";
                     break;
            case "Μαΐου" :   date +="-05";
                     break;
            case "Ιουνίου":   date +="-06";
                     break;
            case "Ιουλίου":   date +="-07";
                     break;
            case "Αυγούστου":   date +="-08";
                     break;
            case "Σεπτεμβρίου":   date +="-09";
                     break;
            case "Οκτωβρίου":  date +="-10";
                     break;
            case "Νοεμβρίου":  date +="-11";
                     break;
            case "Δεκεμβρίου":  date +="-12";
                     break;
            default: 
                     break;
      }            
      if(Integer.parseInt(Date[0])<10){
        date += "-0"+Date[0];
      }
      else{
        date += "-"+Date[0]; 
      }
      gg.setYear(Date[2]);
      legaldoc.setYear(Date[2]);

      int images_sum=0;
      if(!imageFiles.isEmpty()){
          images_sum = imageFiles.size();
      }
      
      this.linkFEK();
      writer2.println("<http://legislation.di.uoa.gr/gazette/a/"+legaldoc.getYear()+"/"+legaldoc.getFEK()+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/GovernmentGazette>.");
      writer2.println("<http://legislation.di.uoa.gr/gazette/a/"+legaldoc.getYear()+"/"+legaldoc.getFEK()+"> <http://purl.org/dc/terms/title> \"Α/"+legaldoc.getYear()+"/"+legaldoc.getFEK()+"\".");
      writer2.println("<http://legislation.di.uoa.gr/gazette/a/"+legaldoc.getYear()+"/"+legaldoc.getFEK()+"> <http://purl.org/dc/terms/created> \""+date+"\"^^<http://www.w3.org/2001/XMLSchema#date>.");
      // Iterate over legal documents to produce the appropriate RDF triples
      for(int i =start; i< limit; i++){
          missing_mods= 0;
          issue_number=1;
          art_count=0;
          chap_count=0;
          image_count=1;
          if(legaldocuments.length>2||start==0){
            FEK = legaldocuments[i];
          }
          baseURI = "http://legislation.di.uoa.gr/";
          buffers = FEK.replace("´", "’").split("\n",2);
          if(buffers.length>1){
            FEK = buffers[1];
          }
          String[] Type = {"",""};
          int finish = 0;
          // Find legal document's ID
          if(buffers[0].contains("ΥΠ’ ΑΡΙΘ.")){
             Type = buffers[0].split(" ΥΠ’ ΑΡΙΘ. ");
          }
          else if(buffers[0].contains("ΥΠ’ ΑΡΙΘΜ.")){
              Type = buffers[0].split(" ΥΠ’ ΑΡΙΘΜ. ");
          }
          else if(buffers[0].contains(" της ")){
              Type[0] = "ΠΡΑΞΗ ΥΠΟΥΡΓΙΚΟΥ ΣΥΜΒΟΥΛΙΟΥ";
          }
          else if(buffers[0].contains("ΠΡΑΞΗ ΝΟΜΟΘΕΤΙΚΟΥ ΠΕΡΙΕΧΟΜΕΝΟΥ")){
              Type[0] = "ΠΡΑΞΗ ΝΟΜΟΘΕΤΙΚΟΥ ΠΕΡΙΕΧΟΜΕΝΟΥ";
          }
          else if(buffers[0].contains("Αριθμ. ")){
               Type[0] = "ΥΠΟΥΡΓΙΚΗ ΑΠΟΦΑΣΗ";
               Type[1] = buffers[0].replace("Αριθμ. ", "").replace(" ", "").replace("/", "_").replace(".","");
          }
          else if(buffers[0].contains("Αριθ. ")){
               Type[0] = "ΥΠΟΥΡΓΙΚΗ ΑΠΟΦΑΣΗ";
               Type[1] = buffers[0].replace("Αριθ. ", "").replace(" ", "").replace("/", "_").replace(".","");
          }
          else{
              System.out.println(buffers[0]);
              finish = 1;
              Type = buffers[0].split(" ΥΠ’ ΑΡΙΘΜ. ");
              //System.out.print("ΑΓΝΩΣΤΟ "+gg.getYear()+"/"+Type[1]+", ");
          }
          
          // Continue if the legal document meets the very basic standards
          if(finish==0){
              // Produce the Type information
              switch (Type[0]) {
                    case "NOMOΣ":  
                            //System.out.print("ΝΟΜΟΣ "+gg.getYear()+"/"+Type[1]+", ");
                            type = "ΝΟΜΟΣ";
                            legaldoc.setId("http://legislation.di.uoa.gr/law/"+gg.getYear()+"/"+Type[1].replace(".",""));
                            baseURI += "law/"+gg.getYear()+"/"+Type[1];
                            writer2.println("<"+legaldoc.getId()+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Law>.");
                            break;
                    case "ΠΡΟΕΔΡΙΚΟ ΔΙΑΤΑΓΜΑ":
                            //System.out.print("ΠΔ "+gg.getYear()+"/"+Type[1]+", ");
                            type = "ΠΡΟΕΔΡΙΚΟ ΔΙΑΤΑΓΜΑ (ΠΔ)";
                            legaldoc.setId("http://legislation.di.uoa.gr/pd/"+gg.getYear()+"/"+Type[1].replace(".",""));
                            baseURI += "pd/"+gg.getYear()+"/"+Type[1];
                            writer2.println("<"+legaldoc.getId()+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/PresidentialDecree>.");
                            break;
                    case "ΠΡΑΞΗ ΥΠΟΥΡΓΙΚΟΥ ΣΥΜΒΟΥΛΙΟΥ":
                            type = "ΠΡΑΞΗ ΥΠΟΥΡΓΙΚΟΥ ΣΥΜΒΟΥΛΙΟΥ (ΠΥΣ)";
                            //System.out.print("ΠΥΣ "+gg.getYear()+"/"+ buffers[0]+", ");
                            legaldoc.setId("http://legislation.di.uoa.gr/amc/"+gg.getYear()+"/"+buffers[0].split(" της ")[0].replace(".",""));
                            baseURI += "amc/"+gg.getYear()+"/"+ buffers[0].split(" της ")[0];
                            writer2.println("<"+legaldoc.getId()+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/ActOfMinisterialCabinet>.");
                            Type[1]= buffers[0].split(" της ")[0];
                            break;
                    case "ΠΡΑΞΗ ΝΟΜΟΘΕΤΙΚΟΥ ΠΕΡΙΕΧΟΜΕΝΟΥ":
                            type = "ΠΡΑΞΗ ΝΟΜΟΘΕΤΙΚΟΥ ΠΕΡΙΕΧΟΜΕΝΟΥ (ΠΝΠ)";
                            //System.out.print("ΠΥΣ "+gg.getYear()+"/"+ buffers[0]+", ");
                            legaldoc.setId("http://legislation.di.uoa.gr/la/"+gg.getYear()+"/"+date);
                            baseURI += "la/"+gg.getYear()+"/"+ date;
                            writer2.println("<"+legaldoc.getId()+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/LegislativeAct>.");
                            Type[1]= date;
                            break;
                    case "ΚΑΝΟΝΙΣΤΙΚΗ ΑΠΟΦΑΣΗ":
                    case "ΚΑΝΟΝΙΣΤΙΚΗ ΔΙΑΤΑΞΗ":
                    case "ΚΑΝΟΝΙΣΜΟΣ":
                            type = "ΚΑΝΟΝΙΣΤΙΚΗ ΔΙΑΤΑΞΗ (ΚΔ)";
                           //System.out.print("ΚΑΝΟΝΙΣΤΙΚΗ ΔΙΑΤΑΞΗ "+gg.getYear()+"/"+Type[1].split("/")[0]+", ");
                            legaldoc.setId("http://legislation.di.uoa.gr/rp/"+gg.getYear()+"/"+Type[1].split("/")[0]);
                            baseURI += "rp/"+gg.getYear()+"/"+Type[1].split("/")[0];
                            writer2.println("<"+legaldoc.getId()+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/RegulatoryProvision>.");
                            break;
                    case "ΥΠΟΥΡΓΙΚΗ ΑΠΟΦΑΣΗ":
                            type = "ΥΠΟΥΡΓΙΚΗ ΑΠΟΦΑΣΗ (ΥΑ)";
                            //System.out.print("ΥΠΟΥΡΓΙΚΗ ΑΠΟΦΑΣΗ "+gg.getYear()+"/"+Type[1].split("/")[0]+", ");
                            legaldoc.setId("http://legislation.di.uoa.gr/md/"+gg.getYear()+"/"+Type[1]);
                            baseURI += "md/"+gg.getYear()+"/"+Type[1];
                            writer2.println("<"+legaldoc.getId()+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/MinisterialDecision>.");
                            break;
                    default: 
                            type = "NΟΜΟΣ";
                            //System.out.print(Type[0] +" "+gg.getYear()+"/"+Type[1]+", ");
                            legaldoc.setId("http://legislation.di.uoa.gr/law/"+gg.getYear()+"/"+Type[1].replace(".",""));
                            baseURI += "law/"+gg.getYear()+"/"+Type[1];
                            writer2.println("<"+legaldoc.getId()+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Law>.");
                            break;
              }    
              
              // Produce the basic metadata information of the legal document
              writer2.println("<"+legaldoc.getId()+"> <http://legislation.di.uoa.gr/ontology/gazette> <http://legislation.di.uoa.gr/gazette/a/"+legaldoc.getYear()+"/"+legaldoc.getFEK()+">.");
              writer2.println("<"+baseURI+"> <http://legislation.di.uoa.gr/ontology/views> \"0\"^^<http://www.w3.org/2001/XMLSchema#integer>.");
              writer2.println("<"+baseURI+"> <http://purl.org/dc/terms/created> \""+date+"\"^^<http://www.w3.org/2001/XMLSchema#date>.");
              writer2.println("<"+baseURI+"> <http://legislation.di.uoa.gr/ontology/legislationID> \""+Type[1]+"\"^^<http://www.w3.org/2001/XMLSchema#integer>.");
              writer2.println("<"+baseURI+"> <http://legislation.di.uoa.gr/ontology/tag> \"Ελλάδα\"@el.");
              

              baseURI2 = baseURI;
             
              
             // Check if there are citations and produce the appropriate RDF triples
             int cit = 0;
             if(FEK.contains("Εκδίδομε τον ακόλουθο νόμο που ψήφισε η Βουλή:\n")){
                buffers = FEK.split("Εκδίδομε τον ακόλουθο νόμο που ψήφισε η Βουλή:\n");
                FEK = buffers[1];
                buffers[0] = buffers[0].replace("−\n", "").replace("\n", " ").replace("−", "-").replaceAll("\\p{C}", " ");
                if(buffers[0].contains("Ο ΠΡΟΕΔΡΟΣ")){
                    buffers[0] = buffers[0].split("Ο ΠΡΟΕΔΡΟΣ")[0];
                }
                else if(buffers[0].contains("Ο ΑΝΤΙΠΡΟΕΔΡΟΣ")){
                    buffers[0] = buffers[0].split("Ο ΑΝΤΙΠΡΟΕΔΡΟΣ")[0];
                }
                else if(buffers[0].contains("ΟΙ ΥΠΟΥΡΓΟΙ")){
                    buffers[0] = buffers[0].split("ΟΙ ΥΠΟΥΡΓΟΙ")[0];
                }
                else if(buffers[0].contains("Ο ΥΠΟΥΡΓΟΣ")){
                    buffers[0] = buffers[0].split("Ο ΥΠΟΥΡΓΟΣ")[0];
                }
                else if(buffers[0].contains("Η ΥΠΟΥΡΓΟΣ")){
                    buffers[0] = buffers[0].split("Η ΥΠΟΥΡΓΟΣ")[0];
                }
                
                legaldoc.setTitle(buffers[0].replaceAll("\\bΟ ΠΡΟΕΔΡΟΣ\\b(.*)", "").replaceAll("\\bΟ ΑΝΤΙΠΡΟΕΔΡΟΣ\\b(.*)", "").replaceAll("\\bΤΗΣ ΕΛΛΗΝΙΚΗΣ ΔΗΜΟΚΡΑΤΙΑΣ\\b(.*)", "").replaceAll("\\bΟΙ ΥΠΟΥΡΓΟΙ\\b(.*)", "").replace("ΤΟ ΥΠΟΥΡΓΙΚΟ ΣΥΜΒΟΥΛΙΟ","").replaceAll("\\bΟ ΥΠΟΥΡΓΟΣ\\b(.*)", "").replace("−\n", "").replace("−", "-").replace("\n", " ").replaceAll("\\p{C}", " "));
                if(legaldoc.getTitle().length()>2000){
                    writer2.println("<"+baseURI2+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI2+"/issue/"+issue_number+">.");
                    writer2.println("<"+baseURI2+"/issue/"+issue_number+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/ParsingIssue>.");
                    writer2.println("<"+baseURI2+"/issue/"+issue_number+"> <http://legislation.di.uoa.gr/ontology/text> \"Πολύ μεγάλος τίτλος, πιθανότατα λάθος.\"@el.");
                    issue_number++;
                }
             }
             else if(FEK.contains("1.Τις διατάξεις:\n")||FEK.contains("΄Εχovτας υπόψη:\n")||FEK.contains("χοντας υπόψη:\n")||FEK.contains("χοντας υπόψη :\n")||FEK.contains("Έχοντας υπ' όψη:\n")||FEK.contains("Έχουσα υπ’ όψει:\n")||FEK.contains("Έχουσα υπ’ όψει :")||FEK.contains("Έχουσα υπ’ όψει:")||FEK.contains("Έχοντας υπόψη τις διατάξεις:")||FEK.contains("Έχουσα υπ’ όψη:\n")||FEK.contains("Έχουσα υπόψη:")||FEK.contains("Έχουσα υπ’ όψιν:")||FEK.contains("Λαβούσα υπ’ όψιν:")||FEK.contains("Έχοντας υπόψιν:")){
                 if(FEK.contains("αποφασίζουμε:")){
                    buffers = FEK.split("αποφασίζουμε:",2);
                 }
                 else if(FEK.contains("αποφα−\nσίζουμε:")){
                    buffers = FEK.split("αποφα−\nσίζουμε:",2);
                 }
                 else if(FEK.contains(", ψηφίζει:")){
                      buffers =FEK.split(", ψηφίζει:",2);
                 }
                 else if(FEK.contains("αποφασίζει τα εξής:")){
                     buffers =FEK.split(", αποφασίζει τα εξής:",2);
                 }
                 else if(FEK.contains("αποφασίζει:")){
                     buffers =FEK.split("αποφασίζει:",2);
                 }
                 else if(FEK.contains("\nΠροβαίνουμε")){
                     buffers =FEK.split("\nΠροβαίνουμε",2);
                     buffers[0] =  buffers[0].replace("υπόψη:\n", "υπόψη:\n1. ");
                     buffers[1] = "Προβαίνουμε" + buffers[1];
                 }
                 else if(FEK.contains("ο οποίος\nέχει ως ακολούθως:")){
                     buffers =FEK.split("ο οποίος\nέχει ως ακολούθως:",2);
                 }
                 else if(FEK.contains("έχοντα\nούτω:")){
                     buffers =FEK.split("έχοντα\nούτω:",2);
                 }
                 else if(FEK.contains("έχοντα ούτω:")){
                     buffers =FEK.split("έχοντα ούτω:",2);
                 }
                 else if(FEK.contains("έχει ως ακολούθως:")){
                     buffers =FEK.split("έχει ως ακολούθως:",2);
                 }
                 else if(FEK.contains("ως κατωτέρω:")){
                     buffers =FEK.split("ως κατωτέρω:",2);
                 }
                 else if(FEK.contains("ως κατω−\nτέρω:")){
                     buffers =FEK.split("ως κατω−\nτέρω:",2);
                 }
                 else if(FEK.contains("συμφώνησαν στα εξής:")){
                     buffers =FEK.split("συμφώνησαν στα εξής:",2);
                 }
                 else{
                    //buffers = FEK.split("ζουμε:",2);
                     if(FEK.contains("\nΜΕΡΟΣ ")){
                        buffers =FEK.split("\nΜΕΡΟΣ ",2);
                        buffers[1] = "\nΜΕΡΟΣ " + buffers[1];
                    }
                    else if(FEK.contains("\nTMHMA ")){
                        buffers =FEK.split("\nTMHMA ",2);
                        buffers[1] = "\nTMHMA " + buffers[1];
                    }
                    else if(FEK.contains("\nΚΕΦΑΛΑΙΟ ")){
                        buffers =FEK.split("\nΚΕΦΑΛΑΙΟ ",2);
                        buffers[1] = "\nΚΕΦΑΛΑΙΟ " + buffers[1];
                    }
                    else if(FEK.contains("\nΆρθρο ")){
                        buffers =FEK.split("\nΆρθρο ",2);
                        buffers[1] = "\nΆρθρο " + buffers[1];
                    }
                 }
                 //System.out.println(buffers[0]);
                 parseCitations(buffers[0]);
                 FEK = buffers[1];
                 cit = 1;
             }
             
             int signer = 0;
             // Split to crop legal document's text body from footer
             if(baseURI.contains("/amc/")){
                 buffers = FEK.split("Ο ΠΡΩΘΥΠΟΥΡΓΟΣ");
                 FEK = buffers[0];
                 parseSigners(buffers[1]);
             }
             else if(FEK.contains("\nΑθήνα, ")){
                buffers = FEK.split("\nΑθήνα, [0-9]+ (Ιανουαρίου|Φεβρουαρίου|Μαρτίου|Απριλίου|Μαΐου|Ιουνίου|Ιουλίου|Αυγούστου|Σεπτεμβρίου|Οκτωβρίου|Νοεμβρίου|Δεκεμβρίου) [0-9]+\n",2);
                signer = 1;
                FEK = buffers[0];
                parseSigners(buffers[1]);
             }
             else{
                writer2.println("<"+baseURI2+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI2+"/issue/"+issue_number+">.");
                writer2.println("<"+baseURI2+"/issue/"+issue_number+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/ParsingIssue>.");
                writer2.println("<"+baseURI2+"/issue/"+issue_number+"> <http://legislation.di.uoa.gr/ontology/text> \"Δεν αναγνωρίστηκαν οι υπογραφόντες.\"@el.");
                issue_number++;
                //System.out.println("HERE "+baseURI2);
            }
             
             // Delete some well known annoying printing trash
             FEK = FEK.replaceAll("ΕΦΗΜΕΡΙΣ ΤΗΣ ΚΥΒΕΡΝΗΣΕΩΣ \\(ΤΕΥΧΟΣ ΠΡΩΤΟ\\)", "");

//              Parse the legal document's text body according to its structure
//              There are parts
             if(FEK.startsWith("ΜΕΡΟΣ")||FEK.contains("\nΜΕΡΟΣ")){
                if(cit==0){
                     String title = legaldoc.getTitle();
                     title= title.replaceAll("\\bΟ ΠΡΟΕΔΡΟΣ\\b(.*)", "").replaceAll("\\bΤΗΣ ΕΛΛΗΝΙΚΗΣ ΔΗΜΟΚΡΑΤΙΑΣ\\b(.*)", "").replaceAll("\\bΟ ΑΝΤΙΠΡΟΕΔΡΟΣ\\b(.*)", "").replaceAll("\\bΟΙ ΥΠΟΥΡΓΟΙ\\b(.*)", "").replace("ΤΟ ΥΠΟΥΡΓΙΚΟ ΣΥΜΒΟΥΛΙΟ(.*)","").replaceAll("\\bΗ ΥΠΟΥΡΓΟΣ\\b(.*)", "").replaceAll("\\bΟ ΥΠΟΥΡΓΟΣ\\b(.*)", "").replace("−\n", "").replace("\n", " ").replace("−", "-").replaceAll("\\p{C}", " ");
                     legaldoc.setTitle(title);
                     writer2.println("<"+baseURI+"> <http://purl.org/dc/terms/title> \""+title+"\"@el.");
                     String HTMLtitle = tagLegislation(title);
                    if(!HTMLtitle.equals(title)){
                        writer2.println("<"+baseURI+">  <http://legislation.di.uoa.gr/ontology/html> \""+HTMLtitle+"\".");
                    }
                }
                if(FEK.contains("\nΠΑΡΑΡΤΗΜΑ")){
                    buffers = FEK.split("\nΠΑΡΑΡΤΗΜΑ [Α-Ω]+\n",2);
                    FEK = buffers[0];
                    parseAppendices(buffers[1]);
                }
                parseParts();
             }
             if(FEK.startsWith("ΤΜΗΜΑ")||FEK.contains("\nΤΜΗΜΑ")){
                if(cit==0){
                     String title = legaldoc.getTitle();
                     title= title.replaceAll("\\bΟ ΠΡΟΕΔΡΟΣ\\b(.*)", "").replaceAll("\\bΤΗΣ ΕΛΛΗΝΙΚΗΣ ΔΗΜΟΚΡΑΤΙΑΣ\\b(.*)", "").replaceAll("\\bΟ ΑΝΤΙΠΡΟΕΔΡΟΣ\\b(.*)", "").replaceAll("\\bΟΙ ΥΠΟΥΡΓΟΙ\\b(.*)", "").replace("ΤΟ ΥΠΟΥΡΓΙΚΟ ΣΥΜΒΟΥΛΙΟ","").replaceAll("\\bΗ ΥΠΟΥΡΓΟΣ\\b(.*)", "").replaceAll("\\bΟ ΥΠΟΥΡΓΟΣ\\b(.*)", "").replace("−\n", "").replace("\n", " ").replace("−", "-").replaceAll("\\p{C}", " ");
                     legaldoc.setTitle(title);
                     writer2.println("<"+baseURI+"> <http://purl.org/dc/terms/title> \""+title+"\"@el.");
                    String HTMLtitle = tagLegislation(title);
                    if(!HTMLtitle.equals(title)){
                        writer2.println("<"+baseURI+">  <http://legislation.di.uoa.gr/ontology/html> \""+HTMLtitle+"\".");
                    }
                }
                if(FEK.contains("\nΠΑΡΑΡΤΗΜΑ")){
                    buffers = FEK.split("\nΠΑΡΑΡΤΗΜΑ [Α-Ω]+\n",2);
                    FEK = buffers[0];
                    parseAppendices(buffers[1]);
                }
                parseSections(FEK,0);
             }
             //There are chapters
             else if(FEK.startsWith("ΚΕΦΑΛΑΙΟ")||FEK.contains("\nΚΕΦΑΛΑΙΟ")){
                 if(cit==0){
                     String title = legaldoc.getTitle();
                     title= title.replaceAll("\\bΟ ΠΡΟΕΔΡΟΣ\\b(.*)", "").replaceAll("\\bΤΗΣ ΕΛΛΗΝΙΚΗΣ ΔΗΜΟΚΡΑΤΙΑΣ\\b(.*)", "").replaceAll("\\bΟ ΑΝΤΙΠΡΟΕΔΡΟΣ\\b(.*)", "").replaceAll("\\bΟΙ ΥΠΟΥΡΓΟΙ\\b(.*)", "").replace("ΤΟ ΥΠΟΥΡΓΙΚΟ ΣΥΜΒΟΥΛΙΟ","").replaceAll("\\bΗ ΥΠΟΥΡΓΟΣ\\b(.*)", "").replaceAll("\\bΟ ΥΠΟΥΡΓΟΣ\\b(.*)", "").replace("−\n", "").replace("\n", " ").replace("−", "-").replaceAll("\\p{C}", " ");
                     legaldoc.setTitle(title);
                     writer2.println("<"+baseURI+"> <http://purl.org/dc/terms/title> \""+title+"\"@el.");
                     String HTMLtitle = tagLegislation(title);
                    if(!HTMLtitle.equals(title)){
                        writer2.println("<"+baseURI+">  <http://legislation.di.uoa.gr/ontology/html> \""+HTMLtitle+"\".");
                    }
                }
                if(FEK.contains("\nΠΑΡΑΡΤΗΜΑ")){
                    buffers = FEK.split("\nΠΑΡΑΡΤΗΜΑ [Α-Ω]+\n",2);
                    FEK = buffers[0];
                    parseAppendices(buffers[1]);
                }
                parseChapters(FEK,0);
             }
             // There are only article
             else if(FEK.startsWith("Άρθρο")||FEK.startsWith("ΑΡΘΡΟ")||FEK.contains("\nΆρθρο")||FEK.contains("Άρθρον")||FEK.contains("\nΆρθρον")||FEK.startsWith("’Αρθρο")||FEK.contains("\n’Αρθρο")){
                 if(cit==0&&!legaldoc.getTitle().startsWith("Άρθρο")){
                     String title = legaldoc.getTitle();
                     title= title.replaceAll("\\bΟ ΠΡΟΕΔΡΟΣ\\b(.*)", "").replaceAll("\\bΤΗΣ ΕΛΛΗΝΙΚΗΣ ΔΗΜΟΚΡΑΤΙΑΣ\\b(.*)", "").replaceAll("\\bΟ ΑΝΤΙΠΡΟΕΔΡΟΣ\\b(.*)", "").replaceAll("\\bΟΙ ΥΠΟΥΡΓΟΙ\\b(.*)", "").replace("ΤΟ ΥΠΟΥΡΓΙΚΟ ΣΥΜΒΟΥΛΙΟ","").replaceAll("\\bΗ ΥΠΟΥΡΓΟΣ\\b(.*)", "").replaceAll("\\bΟ ΥΠΟΥΡΓΟΣ\\b(.*)", "").replace("−\n", "").replace("\n", " ").replace("−", "-").replaceAll("\\p{C}", " ");
                     legaldoc.setTitle(title);
                     writer2.println("<"+baseURI+"> <http://purl.org/dc/terms/title> \""+title+"\"@el.");
                     String HTMLtitle = tagLegislation(title);
                    if(!HTMLtitle.equals(title)){
                        writer2.println("<"+baseURI+">  <http://legislation.di.uoa.gr/ontology/html> \""+HTMLtitle+"\".");
                    }
                }
                else if(legaldoc.getTitle().startsWith("Άρθρο")||legaldoc.getTitle().startsWith("’Αρθρο")){
                    FEK = legaldoc.getTitle() +FEK;
                }
                art_count = -1; 
                parseArticles(FEK); 
             }
             // The legal document is photocopied!!!
             else if(FEK.equals("")){
                 
                 System.out.println("PHOTOCOPIED");
             }
             // There is no structure at all
             else{
                 if(legaldoc.getTitle()!=null&&cit==0){
                     writer2.println("<"+baseURI+"> <http://purl.org/dc/terms/title> \""+legaldoc.getTitle()+"\"@el.");
                     String HTMLtitle = tagLegislation(legaldoc.getTitle());
                    if(!HTMLtitle.equals(legaldoc.getTitle())){
                        writer2.println("<"+baseURI+">  <http://legislation.di.uoa.gr/ontology/html> \""+HTMLtitle+"\".");
                    }
                 }
                 FEK= this.splitImages(FEK);
                 if(!FEK.isEmpty()){
                 writer2.println("<"+baseURI+"/article/1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Article>.");
                 writer2.println("<"+baseURI+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/article/1>.");
                 writer2.println("<"+baseURI+"/article/1/paragraph/1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Paragraph>.");
                 writer2.println("<"+baseURI+"/article/1> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/article/1/paragraph/1>.");
                 writer2.println("<"+baseURI+"/article/1/paragraph/1/passage/1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Passage>.");
                 writer2.println("<"+baseURI+"/article/1/paragraph/1> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/article/1/paragraph/1/passage/1>.");
                 writer2.println("<"+baseURI+"/article/1/paragraph/1/passage/1> <http://legislation.di.uoa.gr/ontology/text> \""+FEK.replace("−\n", "").replace("−", "-").replace("\n", " ").replaceAll("\\p{C}", " ")+"\"@el.");
                 }
             }
             legal_ok ++;
             if(images_sum!=0&&!imageFiles.isEmpty()){
                //System.out.println("IMAGES: "+imageFiles.size()+"/"+images_sum);
                writer2.println("<"+baseURI2+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI2+"/issue/"+issue_number+">.");
                writer2.println("<"+baseURI2+"/issue/"+issue_number+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/ParsingIssue>.");
                writer2.println("<"+baseURI2+"/issue/"+issue_number+"> <http://legislation.di.uoa.gr/ontology/text> \"Πιθανότατα λείπουν εικόνες\"@el.");
                issue_number++;
            }
            if(missing_mods!=0){
                 writer2.println("<"+baseURI2+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI2+"/issue/"+issue_number+">.");
                 writer2.println("<"+baseURI2+"/issue/"+issue_number+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/ParsingIssue>.");
                 if(missing_mods==1){
                    writer2.println("<"+baseURI2+"/issue/"+issue_number+"> <http://legislation.di.uoa.gr/ontology/text> \"Λέιπει μια τροποποίηση\"@el.");
                 }
                 else{
                    writer2.println("<"+baseURI2+"/issue/"+issue_number+"> <http://legislation.di.uoa.gr/ontology/text> \"Λέιπουν "+missing_mods+" τροποποιήσεις\"@el.");
                 }
                 issue_number++;
            }
          }
        }
        if(images_sum!=0&&!imageFiles.isEmpty()){
            //System.out.println("IMAGES: "+imageFiles.size()+"/"+images_sum);
            writer2.println("<http://legislation.di.uoa.gr/gazette/a/"+legaldoc.getYear()+"/"+legaldoc.getFEK()+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI2+"/issue/1>.");
            writer2.println("<"+baseURI2+"/issue/1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/ParsingIssue>.");
            writer2.println("<"+baseURI2+"/issue/1> <http://legislation.di.uoa.gr/ontology/text> \"Λείπουν "+imageFiles.size()+" από τις "+images_sum+" εικόνες\"@el.");
                 
        }
        writer2.close();
    }
    
    void parseAppendices(String input){
        String[]  appendices = FEK.split("\nΠΑΡΑΡΤΗΜΑ [Α-Ω]+\n");
        
        // Produce RDF triples for appendices
        for(int i=0; i<appendices.length; i++){
            //System.out.println("APPENDIX "+(i+1));
            writer2.println("<"+baseURI+"/apendix/"+(i+1)+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Appendix>.");
            writer2.println("<"+baseURI+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/apendix/"+(i+1)+">.");
            writer2.println("<"+baseURI+"/apendix/"+(i+1)+"> <http://legislation.di.uoa.gr/ontology/text> \""+appendices[i]+"\"@el.\n");
        }
    }
    
    void parseCitations(String citation_list){
        // Split between citations
        String[] citations = citation_list.split("[0-9]+\\. ");
         String title = "";
        // Crop legal document's title
        if(citations[0].contains("Έχοντας υπόψη:\n")){
            title = citations[0].split("Έχοντας υπόψη:")[0];
        }
        else if(citations[0].contains("Έχοντας υπόψη :\n")){
            title = citations[0].split("Έχοντας υπόψη :")[0];
        }
        else if(citations[0].contains("΄Εχοντας υπόψη:")){
            title = citations[0].split("΄Εχοντας υπόψη:")[0];
        }
        else if(citations[0].contains("Έχουσα υπ’ όψει:")){
            if(title.contains("Η ΙΕΡΑ")){
                title = citations[0].split("Έχουσα υπ’ όψει:")[0].split("Η ΙΕΡΑ")[0];
            }
            else{
                title = citations[0].split("Έχουσα υπ’ όψει:")[0];
            }
        }
        else if(citations[0].contains("Έχοντας υπ’ όψη:")){
            if(title.contains("Η ΙΕΡΑ")){
                title = citations[0].split("Έχοντας υπ’ όψη:")[0].split("Η ΙΕΡΑ")[0];
            }
            else{
                title = citations[0].split("Έχουσα υπ’ όψει:")[0];
            }
        }
        else if(citations[0].contains("Έχουσα υπ’ όψει :")){
            if(title.contains("Η ΙΕΡΑ")){
                title = citations[0].split("Έχουσα υπ’ όψει :")[0].split("Η ΙΕΡΑ")[0];
            }
            else{
                title = citations[0].split("Έχουσα υπ’ όψει :")[0];
            }
        }
        else if (citations[0].contains("Έχουσα υπ’ όψη:")){
            if(title.contains("Η ΙΕΡΑ")){
                title = citations[0].split("Έχουσα υπ’ όψη:")[0].split("Η ΙΕΡΑ")[0];
            }
            else{
                title = citations[0].split("Έχουσα υπ’ όψη:")[0];
            }
        }
        else if (citations[0].contains("Έχουσα υπόψη:")){
            if(title.contains("Η ΙΕΡΑ")){
                title = citations[0].split("Έχουσα υπόψη:")[0].split("Η ΙΕΡΑ")[0];
            }
            else{
                title = citations[0].split("Έχουσα υπόψη:")[0];
            }
        }
        else if (citations[0].contains("Λαβούσα υπ’ όψιν:")){
            if(title.contains("Η ΙΕΡΑ")){
                title = citations[0].split("Λαβούσα υπ’ όψιν:")[0].split("Η ΙΕΡΑ")[0];
            }
            else{
                title = citations[0].split("Λαβούσα υπ’ όψιν:")[0];
            }
        }
        else if (citations[0].contains("Έχοντας υπόψη τις διατάξεις:")){
            if(title.contains("Η ΙΕΡΑ")){
                title = citations[0].split("Έχοντας υπόψη τις διατάξεις:")[0].split("Η ΙΕΡΑ")[0];
            }
            else{
                title = citations[0].split("Έχοντας υπόψη τις διατάξεις:")[0];
            }
        }
        else if (citations[0].contains("Έχοντας υπόψιν:")){
            if(title.contains("Η ΙΕΡΑ")){
                title = citations[0].split("Έχοντας υπόψιν:")[0].split("Η ΙΕΡΑ")[0];
            }
            else{
                title = citations[0].split("Έχοντας υπόψιν:")[0];
            }
        }
        else if (citations[0].contains("Έχουσα υπ’ όψιν:")){
            if(title.contains("Η ΙΕΡΑ")){
                title = citations[0].split("Έχουσα υπ’ όψιν:")[0].split("Η ΙΕΡΑ")[0];
            }
            else{
                title = citations[0].split("Έχουσα υπ’ όψιν:")[0];
            }
        }

        if(title.contains("Ο ΠΡΟΕΔΡΟΣ")){
            title = title.split("Ο ΠΡΟΕΔΡΟΣ")[0];
        }
        else if(title.contains("Ο ΑΝΤΙΠΡΟΕΔΡΟΣ")){
            title = title.split("Ο ΑΝΤΙΠΡΟΕΔΡΟΣ")[0];
        }
        else if(title.contains("ΟΙ ΥΠΟΥΡΓΟΙ")){
            title = title.split("ΟΙ ΥΠΟΥΡΓΟΙ")[0];
        }
        else if(title.contains("Ο ΥΠΟΥΡΓΟΣ")){
            title = title.split("Ο ΥΠΟΥΡΓΟΣ")[0];
        }
        else if(title.contains("Η ΥΠΟΥΡΓΟΣ")){
            title = title.split("Η ΥΠΟΥΡΓΟΣ")[0];
        }
        
        title = title.replaceAll("\\bΟ ΠΡΟΕΔΡΟΣ\\b(.*)", "").replaceAll("\\bΗ ΙΕΡΑ\\b(.*)", "").replaceAll("\\bΗ ΔΙΑΡΚΗΣ\\b(.*)", "").replaceAll("ΤΗΣ (.*)ΕΚΚΛΗΣΙΑΣ(.*)", "").replaceAll("\\bΟ ΑΝΤΙΠΡΟΕΔΡΟΣ\\b(.*)", "").replaceAll("\\bΤΗΣ ΕΛΛΗΝΙΚΗΣ ΔΗΜΟΚΡΑΤΙΑΣ\\b(.*)", "").replace("ΤΟ ΥΠΟΥΡΓΙΚΟ ΣΥΜΒΟΥΛΙΟ","").replaceAll("\\bΟΙ ΥΠΟΥΡΓΟΙ\\b(.*)", "").replaceAll("\\bΗ ΥΠΟΥΡΓΟΣ\\b(.*)", "").replaceAll("\\bΟ ΥΠΟΥΡΓΟΣ\\b(.*)", "").replace("−\n", "").replaceAll("\n", " ").replace("−", "-");
        legaldoc.setTitle(title.replaceAll("\\bΟ ΠΡΟΕΔΡΟΣ\\b(.*)", "").replaceAll("\\bΟ ΑΝΤΙΠΡΟΕΔΡΟΣ\\b(.*)", "").replaceAll("\\bΤΗΣ ΕΛΛΗΝΙΚΗΣ ΔΗΜΟΚΡΑΤΙΑΣ\\b(.*)", "").replaceAll("\\bΟΙ ΥΠΟΥΡΓΟΙ\\b(.*)", "").replace("ΤΟ ΥΠΟΥΡΓΙΚΟ ΣΥΜΒΟΥΛΙΟ","").replaceAll("\\bΟ ΥΠΟΥΡΓΟΣ\\b(.*)", "").replace("−\n", "").replaceAll("\n", " ").replace("−", "-"));
        if(legaldoc.getTitle().length()>2000){
            writer2.println("<"+baseURI2+"> <http://legislation.di.uoa.gr/ontology/part> <"+baseURI2+"/issue/"+issue_number+">.");
            writer2.println("<"+baseURI2+"/issue/"+issue_number+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/ParsingIssue>.");
            writer2.println("<"+baseURI2+"/issue/"+issue_number+"> <http://legislation.di.uoa.gr/ontology/text> \"Πιθανότατα λάθος τίτλος\"@el.");
            issue_number++;
        }
        writer2.println("<"+baseURI+"> <http://purl.org/dc/terms/title> \""+title+"\"@el.");
        String HTMLtitle = tagLegislation(title);
            if(!HTMLtitle.equals(title)){
                writer2.println("<"+baseURI+">  <http://legislation.di.uoa.gr/ontology/html> \""+HTMLtitle+"\".");
            }
        
        // Produce RDF triples for citations
        for(int i=1; i<citations.length; i++){
            String citation = citations[i].replace("−\n", "").replace("−", "-").replace("\n", " ").replaceAll("\\p{C}", " ");
            writer2.println("<"+baseURI+"/citation/"+i+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.metalex.eu/metalex/2008-05-02#BibliographicCitation>.");
            writer2.println("<"+baseURI+"/citation/"+i+"> <http://legislation.di.uoa.gr/ontology/context> \""+citation+"\"@el.");
            writer2.println("<"+baseURI+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/citation/"+i+">.");
            String HTMLcitation = tagLegislation(citation);
            if(!HTMLcitation.equals(citation)){
                writer2.println("<"+baseURI+"/citation/"+i+"> <http://legislation.di.uoa.gr/ontology/html> \""+HTMLcitation+"\".");
            }
        }
    }
    
    void parseParts() throws IOException{
        // Split between chapters
        String[] parts = FEK.split("\\bΜΕΡΟΣ\\b ([0-9]+|[Α-Ω]+|[A-Z]+|[α-ω]+[ά-ώ]+|΄)+\n");
        // Iterate over parts
        for(int i=1; i<parts.length; i++){
              //System.out.println("PART "+i);
              //System.out.println(parts[i]);
              if(parts[i].startsWith("ΜΕΡΟΣ")||parts[i].startsWith("\nΜΕΡΟΣ")){
                  parts[i] = parts[i].split("\n",2)[1];
              }
              writer2.println("<"+baseURI+"/part/"+i+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Part>.");
              writer2.println("<"+baseURI+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/part/"+i+">.");
              baseURI += "/part/"+i;
              // Parse chapters or articles
              if(parts[i].contains("\nTMHMA")||(parts[i].startsWith("TMHMA"))){
                parseSections(parts[i],1);
              }
              if(parts[i].contains("\nΚΕΦΑΛΑΙΟ")||(parts[i].startsWith("ΚΕΦΑΛΑΙΟ"))){
                parseChapters(parts[i],1);
              }
              else{
                parseArticles(parts[i]);
              }
              baseURI = baseURI.split("/part")[0];
        }
    
    }
    
    void parseSections(String part,int parent) throws IOException{
        // Split between chapters
        int begin =1;
        String[] sections = FEK.split("\\bΤΜΗΜΑ\\b ([0-9]+|[Α-Ω]+|[α-ω]+[ά-ώ]+|΄)+\n");
        if(parent==1){
            if(!sections[0].isEmpty()&&!sections[0].startsWith("ΤΜΗΜΑ")){
                writer2.println("<"+baseURI+"> <http://purl.org/dc/terms/title> \""+sections[0].replace("−\n", "").replace("−", "-").replace("\n", " ").replaceAll("\\p{C}", "")+"\"@el.");
                //System.out.println("TITLE: "+buffers[0].replace("−\n", "").replace("−", "-").replace("\n", " ").replaceAll("\\p{C}", ""));
            }
            if(!sections[0].isEmpty()&&sections[0].startsWith("ΤΜΗΜΑ")){
                sections[0] = sections[0].replaceFirst("ΤΜΗΜΑ ([0-9]+|[Α-Ω]+|[α-ω]+[ά-ώ]+|΄)+\n","");
                begin = 0;
            }
        }
        // Iterate over parts
        for(int i=begin; i<sections.length; i++){
              //System.out.println("PART "+i);
              //System.out.println(parts[i]);
              if(sections[i].startsWith("ΤΜΗΜΑ")||sections[i].startsWith("\nΤΜΗΜΑ")){
                  sections[i] = sections[i].split("\n",2)[1];
              }
              writer2.println("<"+baseURI+"/section/"+i+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Section>.");
              writer2.println("<"+baseURI+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/section/"+i+">.");
              baseURI += "/section/"+i;
              // Parse chapters or articles
              if(sections[i].contains("\nΚΕΦΑΛΑΙΟ")||(sections[i].startsWith("ΚΕΦΑΛΑΙΟ"))){
                parseChapters(sections[i],1);
              }
              else{
                parseArticles(sections[i]);
              }
              baseURI = baseURI.split("/section")[0];
        }
    
    }
    
    void parseChapters(String part,int parent) throws IOException{
        int begin =1;
        // Split between chapters
        String[] buffers = part.split("\\bΚΕΦΑΛΑΙΟ\\b ([Α-Ω]|[0-9]|΄)+\n");
        if(parent==1){
            if(!buffers[0].isEmpty()&&!buffers[0].startsWith("ΚΕΦΑΛΑΙΟ")){
                writer2.println("<"+baseURI+"> <http://purl.org/dc/terms/title> \""+buffers[0].replace("−\n", "").replace("−", "-").replace("\n", " ").replaceAll("\\p{C}", "")+"\"@el.");
                //System.out.println("TITLE: "+buffers[0].replace("−\n", "").replace("−", "-").replace("\n", " ").replaceAll("\\p{C}", ""));
            }
            if(!buffers[0].isEmpty()&&buffers[0].startsWith("ΚΕΦΑΛΑΙΟ")){
                buffers[0] = buffers[0].replaceFirst("\\bΚΕΦΑΛΑΙΟ\\b ([Α-Ω]|[0-9]|΄)+\n","");
                begin = 0;
            }
        }
        // Iterate over chapters
        for(int i=begin; i<buffers.length; i++){
              writer2.println("<"+baseURI+"/chapter/"+i+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Chapter>.");
              writer2.println("<"+baseURI+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/chapter/"+i+">.");
              baseURI += "/chapter/"+i;
              // Parse articles
              parseArticles(buffers[i]);
              baseURI = baseURI.split("/chapter")[0];
        }
    }
    
    ArrayList<String> splitParagraphs(String input){
        ArrayList<String> paragraphs = new ArrayList();
        String current = "";
        String temp = "";
        int flag = 0;
        int par_count = 1;
        int count = 0;
        while(count < input.length()){
            if(input.charAt(count)=='\n'){
                flag = 1;
                temp +="\n";
            }
            else if(input.charAt(count)==Integer.toString(par_count).charAt(0)&&flag!=3){
                if(flag==1 || count==0){
                    if(par_count<=9){
                        flag = 2;
                    }
                    else{
                        flag = 3;
                    }
                    temp += input.charAt(count);
                }
                else{
                    flag =0;
                    temp += input.charAt(count);
                    current += temp;
                    temp = "";
                }
            }
            else if(par_count>9&&input.charAt(count)==Integer.toString(par_count).charAt(1)){
                if(flag==3){
                    flag = 2;
                    temp += input.charAt(count);
                }
                else{
                    flag =0;
                    temp += input.charAt(count);
                    current += temp;
                    temp = "";
                }
            }
            else if(input.charAt(count)=='.'){
                if(flag==2){
                    if((input.length()>=count+3)&&(input.substring(count+1,count+3).matches("[0-9]+\\.[0-9]*"))){
                        flag =0;
                        temp += '.';
                        current += temp;
                        temp = "";
                    }
                   else{
                        paragraphs.add(current);
                        current = "";
                        temp = "";
                        flag =0;
                        par_count ++;
                   }
                }
                else{
                    flag =0;
                    temp += '.';
                    current += temp;
                    temp = "";
                }
            }
            else if(input.charAt(count)==' '){
                if(!current.isEmpty()){
                   flag =0;
                   current += temp;
                   temp = "";
                   current +=  ' ';
                }
            }
            else{
                current += temp;
                current += input.charAt(count);
                flag = 0;
                temp = "";
            }
            
            count++;
        }
        
        if(!current.isEmpty()){
            current += temp;
            paragraphs.add(current);
        }
        
        return paragraphs;
    }
    
    ArrayList<String> splitPassages(String input){
        ArrayList<String> passages = new ArrayList();
        String current = "";
        int flag = 0;
        int count = 0;
        while(count < input.length()){
            if(input.charAt(count)=='.'){
                    flag=1;
                    current+=".";
            }
            else if(input.charAt(count)==' '){
                if(flag==1){
                    if(!current.equals(".")&&!current.equals(". ")&&!current.endsWith("Αριθμ.")&&!current.endsWith("αριθμ.")&&!current.endsWith("παρ.")&&!current.endsWith(" ν.")&&!current.endsWith(" N.")&&!current.endsWith("π.δ.")&&!current.endsWith("Π.Δ.")&&!current.endsWith("Π.δ.")&&!current.split(" ")[current.split(" ").length-1].matches("([Α-Ω]+|[α-ω]+|\\.|\\()+")){
                        current += " ";
                        passages.add(current.replace("ΕΦΗΜΕΡΙΣ ΤΗΣ ΚΥΒΕΡΝΗΣΕΩΣ (ΤΕΥΧΟΣ ΠΡΩΤΟ)", ""));
                        current = "";
                        flag =0;
                    }
                    else{
                        current += " ";
                        flag =0;
                    }
                   
                }
                else if (flag==2){
                    current += " ";
                }
                else{
                    current += " ";
                    flag =0;
                }
            }
            else if(input.charAt(count)==':'){
                if(input.length()>count||input.charAt(count+1)=='$'){
                    current += ":$";
                    passages.add(current);
                    current = "";
                    flag=0;
                    count ++;
                }
                else if(input.length()>count+1||input.charAt(count+2)=='$'){
                    current += ":$";
                    passages.add(current);
                    current = "";
                    flag=0;
                    count +=2;
                }
                else
                {
                    current += ":";
                    passages.add(current.replace("ΕΦΗΜΕΡΙΣ ΤΗΣ ΚΥΒΕΡΝΗΣΕΩΣ (ΤΕΥΧΟΣ ΠΡΩΤΟ)", ""));
                    current = "";
                    flag =0;
                }
            }
            else{
                current += input.charAt(count);
                flag = 0;
            }
            count++;
        }
        
        if(!current.isEmpty()&&(!current.matches("(\\.| |[0-9]+)+"))){
            passages.add(current.replace("ΕΦΗΜΕΡΙΣ ΤΗΣ ΚΥΒΕΡΝΗΣΕΩΣ (ΤΕΥΧΟΣ ΠΡΩΤΟ)", ""));
        }
        
        return passages;
    }
    
    ArrayList<String> splitModifications2(String input){
        ArrayList<String> modifications = new ArrayList();
        Pattern pattern = Pattern.compile(":\\n«((«[^»]*»)|[^»]*)»(\\.)*\\n");
        Matcher matcher = pattern.matcher(input);
        while(matcher.find()) {
            String mod = matcher.group(1);
            modifications.add(mod);
            input = input.replace(mod, ":$\n");
        }
        modifications.add(input);
        return modifications;
    }
    
    ArrayList<String> splitModifications(String input){
        String text = "";
        ArrayList<String> modifications = new ArrayList();
        String temp = "";
        int flag = 0;
        int count = 0;
        int quote_count = 0;
         while(count < input.length()){
             if(input.charAt(count)=='\n'){
                 if(flag==2){
                     temp += input.charAt(count);
                 }
                 else{
                    flag = 1;
                    text+= input.charAt(count);
                 }
             }
             else if(input.charAt(count)=='«'){
                 if(flag==1){
                     flag=2;
                 }
                 else if(flag==2){
                     temp += input.charAt(count);
                     quote_count ++;
                 }
                 else{
                     temp += input.charAt(count);
                 }
             }
             else if(input.charAt(count)=='»'){
                 if(flag==2){
                     if(quote_count==0){
                         if(text.endsWith(":\n")){
                             text =text.substring(0, text.length()-1);
                             modifications.add(temp.replace("««", "«"));
                             text+="$";
                             temp ="";
                             flag=0;
                             quote_count=0;
                         }
                         else{
                             if(text.endsWith("\n")){
                                 text =text.substring(0, text.length()-1);
                             }
                             if(temp.startsWith("«")){
                                 text += temp +"»";
                             }
                             else{
                                text += "«"+temp +"»";
                             }
                             temp = "";
                             flag = 0;
                             quote_count=0;
                         }
                     }
                     else{
                         quote_count --;
                         temp+= input.charAt(count);
                     }
                 }
                 else{
                     text+= input.charAt(count);
                     flag=0;
                 }
           }
           else{
                 if(flag==2){
                     temp += input.charAt(count);
                 }
                 else{
                     text+= input.charAt(count);
                 }
           }
             count++;
         }
        
        modifications.add(text);
        return modifications;
    }
   
    
    String splitImages(String input) throws IOException{
        String text = "";
        ArrayList<String> images = new ArrayList();
        String[] lines = input.split("\n");
        String format = "";
        for(int z=0; z<lines.length; z++){
            if(lines[z].startsWith("[PHOTO_")){
              for(int k=0; k<pages.size();k++){
                  if(Integer.parseInt(lines[z].replace("[PHOTO_","").replace("]",""))==pages.get(k)){
                  if(this.imageFiles.get(k).getName().contains(".jpg")){
                      format = ".jpg";
                  }
                  else{
                      format = ".png";
                  }
                  writer2.println("<"+baseURI2+"/image/"+image_count+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Image>.");
                  writer2.println("<"+baseURI+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI2+"/image/"+image_count+">.");
                  writer2.println("<"+baseURI2+"/image/"+image_count+"> <http://legislation.di.uoa.gr/ontology/imageName> \""+legaldoc.getId().replace("http://legislation.di.uoa.gr/","").toUpperCase().replace("/","_")+"_"+image_count+format+"\".");
                  File image= new File("images/"+folderName+"/"+legaldoc.getId().replace("http://legislation.di.uoa.gr/","").replace("/","_").toUpperCase()+"_"+image_count+format);
                  //System.out.println("PLACE IMAGE: "+ image.getName());
                  Files.copy(this.imageFiles.get(k).toPath(), image.toPath());
                  imageFiles.remove(k);
                  pages.remove(k);
                  image_count++;
                  break;
                  }
              }
            }
            else{
                text += lines[z]+"\n";
            }
        }
        
        
        return text;
    }
    
    ArrayList<String> splitCases(String input){
        ArrayList<String> cases = new ArrayList();
        String[] ionianums = {"α","β","γ","δ","ε","στ","ζ","η","θ","ι","ια","ιβ","ιγ","ιδ","ιε","ιστ","ιζ","ιη","ιθ","κ","κα","κβ","κγ","κδ","κε","κστ","κζ","κη","κθ","λ","λα","λβ","λγ","λδ","λε","λστ","λζ","λη","λθ"};
        int more_cases = 0;
        int count = 1;
        int flag = 0;
        char symbol1 = '\n';
        char symbol2 = ' ';
        if(input.contains("\nα.")&&input.contains("\nβ.")){
            symbol2 = '.';
            more_cases = 1;
            if(input.split("\nα\\.").length>1&&input.split("\nα\\.")[0].contains(":")){
                count=0;
            }
        }
        else if(input.contains(" α.")&&input.contains(" β.")){
            symbol1 = ' ';
            symbol2 = '.';
            more_cases = 1;
            if(input.split(" α\\.").length>1&&input.split(" α\\.")[0].contains(":")){
                count=0;
            }
        }
        if(input.contains("\nα)")&&input.contains("\nβ)")){
            symbol2 = ')';
            more_cases = 1;
            if(input.split("\nα\\)").length>1&&input.split("\nα\\)")[0].contains(":")){
                count=0;
            }
        }
        else if(input.contains(" α)")&&input.contains(" β)")){
            symbol1 = ' ';
            symbol2 = ')';
            more_cases = 1;
            if(input.split(" α\\)").length>1&&input.split(" α\\)")[0].contains(":")){
                count=0;
            }
        }
        else if(input.startsWith("α)")||input.startsWith(" α)")){
            symbol1 = '!';
            symbol2 = '!';
            more_cases = 1;
        }
        
        while(more_cases==1){
            String[] parts = {"a"};
            if(symbol2=='.'){
                parts = input.split(symbol1+ionianums[count]+"\\.",2);
            }
            else if(symbol2==')'){
                parts = input.split(symbol1+ionianums[count]+"\\)",2);
                
            }
            else if(symbol2=='!'){
                parts = input.split(ionianums[count]+"\\)",2);
                
            }
            else if (symbol2=='1'){
               parts = input.split("\n"+count+"\\.",2);
            }
            
            if(parts.length==2){
                if(!parts[0].equals(" ")||!parts[0].isEmpty()||!parts[0].equals("\n")){
                    cases.add(parts[0].replace("ΕΦΗΜΕΡΙΣ ΤΗΣ ΚΥΒΕΡΝΗΣΕΩΣ (ΤΕΥΧΟΣ ΠΡΩΤΟ)", ""));
                    input = parts[1];
                    if(parts[0].equals(", ")||parts[0].equals(",")){
                        input = "";
                        cases.clear();
                        more_cases = 0;
                    }
                }
                else{
                    count--;
                    input = parts[1];
                }
            }
            else{
                more_cases = 0;
            }
            count++;
        }
        
        if(input.length()>=2&&cases.size()>=1){
            cases.add(input.replace("ΕΦΗΜΕΡΙΣ ΤΗΣ ΚΥΒΕΡΝΗΣΕΩΣ (ΤΕΥΧΟΣ ΠΡΩΤΟ)", ""));
        }
        
        for(int j=0; j<cases.size(); j++){
            if(cases.get(j).length()<2){
                cases.remove(j);
                j--;
            }
        }
        
        return cases;
    }
    
    int parseArticles(String chapter) throws IOException{
        // Split between articles
        String[] Articles = chapter.split("\n(Άρθρο|Άρθρον|’Αρθρο|ΑΡΘΡΟ) ([0-9]+|[Α-Ω]+|[α-ω]+[ά-ώ]+)+\n");//πρώτο|δεύτερο|τρίτο|Πρώτο|Δεύτερο|Τρίτο|Τετάρτο)\n");
        int begin = 1;
        int miss = 0;
        // There are chapters
        if(art_count != -1){
            // Title has been already been parsed
            if(Articles[0].startsWith("Άρθρο")||Articles[0].startsWith("\nΆρθρο")||Articles[0].startsWith("\nΆρθρον")||Articles[0].startsWith("\nΆρθρον")||Articles[0].startsWith("\nΑΡΘΡΟ")||Articles[0].startsWith("\n’Αρθρο")){
                Articles[0] = Articles[0].replaceFirst("(Άρθρο|Άρθρον|ΑΡΘΡΟ|’Αρθρο) ([0-9]+|[Α-Ω]+|[α-ω]+[ά-ώ]+)+\n", "");
                begin = 0;
                art_count = 0;
            }
            // Title has not been already been parsed
            else{
                String title = Articles[0];
                if(title.contains("Ο ΠΡΟΕΔΡΟΣ")){
                    title = title.split("Ο ΠΡΟΕΔΡΟΣ")[0];
                }
                else if(title.contains("Ο ΑΝΤΙΠΡΟΕΔΡΟΣ")){
                    title = title.split("Ο ΑΝΤΙΠΡΟΕΔΡΟΣ")[0];
                }
                else if(title.contains("ΟΙ ΥΠΟΥΡΓΟΙ")){
                    title = title.split("ΟΙ ΥΠΟΥΡΓΟΙ")[0];
                }
                else if(title.contains("Ο ΥΠΟΥΡΓΟΣ")){
                    title = title.split("Ο ΥΠΟΥΡΓΟΣ")[0];
                }
                else if(title.contains("Η ΥΠΟΥΡΓΟΣ")){
                    title = title.split("Η ΥΠΟΥΡΓΟΣ")[0];
                }
                //System.out.println("TITLE: "+title);
                title = title.replaceAll("\\bΟ ΑΝΤΙΠΡΟΕΔΡΟΣ\\b(.*)", "").replaceAll("\\bΤΗΣ ΕΛΛΗΝΙΚΗΣ ΔΗΜΟΚΡΑΤΙΑΣ\\b(.*)", "").replace("ΤΟ ΥΠΟΥΡΓΙΚΟ ΣΥΜΒΟΥΛΙΟ","").replaceAll("\\bΟΙ ΥΠΟΥΡΓΟΙ\\b(.*)", "").replaceAll("\\bΟ ΥΠΟΥΡΓΟΣ\\b(.*)", "").replace("−\n", "").replaceAll("\n", " ").replace("−", "-");
                legaldoc.setTitle(title);
                writer2.println("<"+baseURI+"> <http://purl.org/dc/terms/title> \""+title+"\"@el.");
//                String place  = pc.searchPlaces(title);
//                if(!place.isEmpty()){
//                     writer2.println("<"+baseURI+"> <http://legislation.di.uoa.gr/ontology/place> <"+place+">.");
//                }
            }
        
        }
        // There are not chapters
        else{
            begin = 0;
            art_count = 0;
            // Chapter's title has been already been parsed
            if(Articles[0].startsWith("Άρθρο")||Articles[0].startsWith("\nΆρθρο")||Articles[0].startsWith("\nΆρθρον")||Articles[0].startsWith("\nΆρθρον")||Articles[0].startsWith("\nΑΡΘΡΟ")||Articles[0].startsWith("\n’Αρθρο")){
                Articles[0] = Articles[0].replaceFirst("(Άρθρο|Άρθρον|ΑΡΘΡΟ|’Αρθρο) ([0-9]+|[Α-Ω]+|[α-ω]+[ά-ώ]+)+\n", "");
            }
            // Chapter's title has not been already been parsed
            else{
                begin = 1;
                if(legaldoc.getTitle()==null){
                    String title = Articles[0];
                    if(title.contains("Ο ΠΡΟΕΔΡΟΣ")){
                        title = title.split("Ο ΠΡΟΕΔΡΟΣ")[0];
                    }
                    else if(title.contains("Ο ΑΝΤΙΠΡΟΕΔΡΟΣ")){
                        title = title.split("Ο ΑΝΤΙΠΡΟΕΔΡΟΣ")[0];
                    }
                    else if(title.contains("ΟΙ ΥΠΟΥΡΓΟΙ")){
                        title = title.split("ΟΙ ΥΠΟΥΡΓΟΙ")[0];
                    }
                    else if(title.contains("Ο ΥΠΟΥΡΓΟΣ")){
                        title = title.split("Ο ΥΠΟΥΡΓΟΣ")[0];
                    }
                    else if(title.contains("Η ΥΠΟΥΡΓΟΣ")){
                        title = title.split("Η ΥΠΟΥΡΓΟΣ")[0];
                    }
                    writer2.println("<"+baseURI+"> <http://purl.org/dc/terms/title> \""+title.replaceAll("\\bΟ ΑΝΤΙΠΡΟΕΔΡΟΣ\\b(.*)", "").replaceAll("\\bΤΗΣ ΕΛΛΗΝΙΚΗΣ ΔΗΜΟΚΡΑΤΙΑΣ\\b(.*)", "").replace("ΤΟ ΥΠΟΥΡΓΙΚΟ ΣΥΜΒΟΥΛΙΟ","").replaceAll("\\bΟΙ ΥΠΟΥΡΓΟΙ\\b(.*)", "").replaceAll("\\bΟ ΥΠΟΥΡΓΟΣ\\b(.*)", "").replace("−\n", "").replaceAll("\n", " ").replace("−", "-")+"\"@el.");
                }
            }
        }
        if(begin==0){
            miss = 1;
        }
        // Iterate over articles
        for(int j=begin; j<Articles.length; j++){
              int count = j + miss;
              Articles[j] = splitImages(Articles[j]);
              mods = splitModifications(Articles[j]);
              writer2.println("<"+baseURI+"/article/"+(art_count+1)+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Article>.");
              writer2.println("<"+baseURI+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/article/"+(art_count+1)+">.");
              baseURI+="/article/"+(art_count+1);
              // Parse paragraphs
              Articles[j] = mods.get(mods.size()-1);
              mods.remove(mods.size()-1);
              parseParagraphs(Articles[j],0);
              art_count++;
              baseURI = baseURI.split("/article")[0];
              missing_mods += mods.size();
          }
          return art_count;
    }
    
        void parseParagraphs(String article,int type) throws IOException{
            int top = 1;
            ArrayList<String> Paragraphs;
            Paragraphs = this.splitParagraphs(article);
              if(Paragraphs.size()>1){
                  if(type==0){
                      if(Paragraphs.get(0).split(" ")[Paragraphs.get(0).split(" ").length-1].matches("([Α-Ω]+|\\.)+")){
                        writer2.println("<"+baseURI+"> <http://purl.org/dc/terms/title> \""+Paragraphs.get(0).replace("−\n", "").replace("−", "-").replace("\n", " ").replaceAll("\\p{C}", "")+"\"@el.");

                      }
                      else if(!Paragraphs.get(0).endsWith(".")&&!Paragraphs.get(0).endsWith(":\n")&&!Paragraphs.get(0).endsWith(":")&& !Paragraphs.get(0).contains("εξής:")){
                        writer2.println("<"+baseURI+"> <http://purl.org/dc/terms/title> \""+Paragraphs.get(0).replace("−\n", "").replace("−", "-").replace("\n", " ").replaceAll("\\p{C}", "")+"\"@el.");
                      }
                  }
                  else{
                    String[] parts = Paragraphs.get(0).split("\n",2);
                    if(parts.length<2){
                        Paragraphs.remove(0);
                    }
                    else{
                        Paragraphs.set(0, parts[1]);
                    }
                    writer2.println("<"+baseURI+"/article/"+parts[0].split(" ")[1]+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Article>.");
                    writer2.println("<"+baseURI+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/article/"+parts[0].split(" ")[1]+">.");
                      if(!Paragraphs.get(0).endsWith(".")&&!Paragraphs.get(0).endsWith(":\n")&&!Paragraphs.get(0).endsWith(":")&& !Paragraphs.get(0).contains("εξής:")){
                          String[] title = Paragraphs.get(0).split("\n");
                          if(title.length >1){
                              writer2.println("<"+baseURI+"/article/1> <http://purl.org/dc/terms/title> \""+title[1].replace("−\n", "").replace("−", "-").replace("\n", " ").replaceAll("\\p{C}", "")+"\"@el.");
                          }
                      }
                      baseURI+= "/article/"+parts[0].split(" ")[1];
                  }
                  if((Paragraphs.get(0).endsWith(".")&&!(Paragraphs.get(0).split(" ")[Paragraphs.get(0).split(" ").length-1].matches("([Α-Ω]+|\\.)+")))||Paragraphs.get(0).endsWith(":\n")&&!Paragraphs.get(0).endsWith(":")&& !Paragraphs.get(0).contains("εξής:")){
                      top = 0;
                      if(Paragraphs.get(0).matches("[0-9]+\\.(.*[\n]*)*")){
                          Paragraphs.set(0, Paragraphs.get(0).replaceFirst("[0-9]+\\.",""));
                      }
                  }
                  int k;
                  for(int z=top; z<Paragraphs.size(); z++){
                      if(top==0){
                          k = z+1;
                      }
                      else{
                         k =z;
                      }
                      
                      writer2.println("<"+baseURI+"/paragraph/"+k+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Paragraph>.");
                      writer2.println("<"+baseURI+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/paragraph/"+k+">.");
                      baseURI+="/paragraph/"+k;
                      Paragraphs.set(z, splitImages(Paragraphs.get(z)));
                      ArrayList<String> cases = this.splitCases(Paragraphs.get(z));
                      if(cases.size()>1){
                        parseCases(cases,type);
                      }
                      else{
                        String paragraph = Paragraphs.get(z).replace("−\n", "").replaceAll("\\p{C}", " ");
                        if(type==1){
                            paragraph = paragraph.replace("$", "");
                        }
                        parsePassages(paragraph,type);
                      }
                      if(type==0){
                        baseURI = baseURI.split("/paragraph")[0];
                      }
                      else if(type==1){
                        baseURI = baseURI.split("/paragraph")[0]+ "/paragraph"+baseURI.split("/paragraph")[1];
                      }
                      else{
                        baseURI = baseURI.split("/paragraph")[0]+ "/paragraph"+baseURI.split("/paragraph")[1];
                      }
                  }
                  if(type==1){
                    baseURI = baseURI.split("/modification")[0];
                  }
              }
              else{
                  if(type>0){
                    String[] parts = article.split("\n",2);
                    if(parts.length>1){
                        article = parts[1];
                        writer2.println("<"+baseURI+"/article/"+parts[0].split(" ")[1]+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Article>.");
                        writer2.println("<"+baseURI+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/article/"+parts[0].split(" ")[1]+">.");
                        baseURI+= "/article/"+parts[0].split(" ")[1];
                    }
                    else{
                        writer2.println("<"+baseURI+"/article/"+parts[0].split(" ")[1]+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Article>.");
                        writer2.println("<"+baseURI+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/article/"+parts[0].split(" ")[1]+">.");
                        baseURI+= "/article/"+parts[0].split(" ")[1];
                    }
                  }
                  String[] titles = article.split("\n");
                  
                  if(titles.length>1){
                      String title = titles[0];
                      if(!title.endsWith(".")&&!title.endsWith(":\n")&&!title.endsWith(":")&& !title.contains("εξής:")&&!(title.endsWith("−"))&& ((titles[1].charAt(0)>='Α')&&(titles[1].charAt(0)<='Ω'))){
                         writer2.println("<"+baseURI+"> <http://purl.org/dc/terms/title> \""+title.replace("−\n", "").replace("−", "-").replace("\n", " ").replaceAll("\\p{C}", "")+"\"@el."); 
                         article = "";
                         for(int k=1;k<titles.length;k++){
                             article += titles[k] + "\n";
                         }
                      }
                      if(title.endsWith(".")&&title.split("([Α-Ω]+.)+").length>1){
                        writer2.println("<"+baseURI+"> <http://purl.org/dc/terms/title> \""+title.replace("−\n", "").replace("−", "-").replace("\n", " ").replaceAll("\\p{C}", "")+"\"@el."); 
                        article = "";
                         for(int k=1;k<titles.length;k++){
                             article += titles[k] + "\n";
                         }
                      }
                      
                  }
                  
                  writer2.println("<"+baseURI+"/paragraph/1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Paragraph>.");
                  writer2.println("<"+baseURI+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/paragraph/1>.");
                  baseURI+="/paragraph/1";
                  String paragraph = splitImages(article);
                  ArrayList<String> cases = this.splitCases(paragraph);
                  if(cases.size()>1){
                    parseCases(cases,type);
                  }
                  else{
                    paragraph = paragraph.replace("−\n", "").replaceAll("\\p{C}", " ");
                    if(type==1){
                        paragraph = paragraph.replace("$", "");
                    }
                    parsePassages(paragraph,type);
                  }
                  if(type==0){
                    baseURI = baseURI.split("/paragraph")[0];
                  }
                  else if(type==1){
                    baseURI = baseURI.split("/paragraph")[0]+ "/paragraph"+baseURI.split("/paragraph")[1];
                  }
                  else{
                    baseURI = baseURI.split("/paragraph")[0]+ "/paragraph"+baseURI.split("/paragraph")[1];
                  }
                  if(type>0){
                    baseURI = baseURI.split("/modification")[0];
                  }
              }
              
    }
    
    void parsePassages(String paragraph, int type) throws IOException{
       
      ArrayList<String> passages = this.splitPassages(paragraph);
      int mod_flag;
      String passage;
      if(passages.size()>1){
          for(int i=0; i< passages.size(); i++){
              passage = passages.get(i).replace("−\n", "").replace("-\n", "").replace("−", "-").replace("\n", " ").replaceAll("\\p{C}", " ").trim();
              mod_flag = 0;
              if(passage.endsWith("$")){
                  mod_flag = 1;
                  passage = passage.replace("$", "");
              }
              else if(passage.endsWith("$.")){
                  mod_flag = 1;
                  passage = passage.replace("$.", "");
              }
              else if(passage.endsWith("$ ")){
                  mod_flag = 1;
                  passage = passage.replace("$ ", "");
              }
              else if(passage.contains("$")){
                  mod_flag = 1;
                  passage = passage.replace("$", "");
              }
              addDocPassage(iw, legaldoc.getTitle(),"TYPOS",passage, date, baseURI2);
              String HTMLpassage = tagLegislation(passage);
              String HTMLpassage2 = tagEntities(HTMLpassage);
              writer2.println("<"+baseURI+"/passage/"+(i+1)+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Passage>.");
              writer2.println("<"+baseURI+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/passage/"+(i+1)+">.");
              writer2.println("<"+baseURI+"/passage/"+(i+1)+"> <http://legislation.di.uoa.gr/ontology/text> \""+passage.replace("$", "")+"\"@el.");
              if(!HTMLpassage2.equals(passage)){
                  //System.out.println("HTML:"+HTMLpassage);
                  writer2.println("<"+baseURI+"/passage/"+(i+1)+"> <http://legislation.di.uoa.gr/ontology/html> \""+HTMLpassage2.replace("$", "")+"\".");
              }
              if(mod_flag==1){//(passage.contains("καταργείται:")||passage.contains("ως εξής:")||passage.contains("νέο εδάφιο:")||passage.contains("τα ακόλουθα εδάφια:")||passage.contains("ακόλουθο εδάφιο:")||passage.contains("τα εξής εδάφια:")||(passage.contains("εξής:")&&!passage.endsWith("».\n"))||(passage.contains("ακολούθως:")&&!passage.contains("ως ακολούθως: α")))){
                  mod_flag=0;
                  if(!mods.isEmpty()&&type ==0){
                    String mod = mods.get(0);
                    mods.remove(0);
                    writer2.println("<"+baseURI+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/passage/"+(i+1)+"/modification/"+mod_count+">.");
                    //passage = passage.replace("  ", " ");
                    String uri = findModificationTarget(passage);
                    String uri2 = findModificationPatient(passage,uri);
                    createModificationInfo(passage,uri,uri2,"passage/"+(i+1));
                    if(mod.contains("Άρθρο")){
                        baseURI+= "/passage/"+(i+1)+"/modification/"+mod_count;
                        parseParagraphs(mod.replace("$", ""),1);
                        baseURI = baseURI.split("/passage")[0];
                    }
                    else if(mod.split(" ")[0].matches("[0-9]+\\.(.*)")){
                       String num = mod.split("\\.",2)[0].replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω]", "");
                       mod = mod.replaceFirst("[0-9]+\\.", "").replace("$", "");
                       writer2.println("<"+baseURI+"/passage/"+(i+1)+"/modification/"+mod_count+"/paragraph/"+num+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Paragraph>.");
                       writer2.println("<"+baseURI+"/passage/"+(i+1)+"/modification/"+mod_count+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/passage/"+(i+1)+"/modification/"+mod_count+"/paragraph/"+num+">.");
                       ArrayList<String> mod_passages = this.splitPassages(mod);
                       for(int k=0; k< mod_passages.size(); k++){
                            writer2.println("<"+baseURI+"/passage/"+(i+1)+"/modification/"+mod_count+"/paragraph/"+num+"/passage/"+(k+1)+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Passage>.");
                            writer2.println("<"+baseURI+"/passage/"+(i+1)+"/modification/"+mod_count+"/paragraph/"+num+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/passage/"+(i+1)+"/modification/"+mod_count+"/paragraph/"+num+"/passage/"+(k+1)+">.");
                            writer2.println("<"+baseURI+"/passage/"+(i+1)+"/modification/"+mod_count+"/paragraph/"+num+"/passage/"+(k+1)+"> <http://legislation.di.uoa.gr/ontology/text> \""+mod_passages.get(k).replace("−\n", "").replace("−", "-").replace("\n", " ").replaceAll("\\p{C}", " ").replace("$", "")+"\"@el.");
                       }

                    }
//                    else if(mod.matches("[α-ω]+\\)(.*)")){
//                       String num = mod.split(")",2)[0].replace(" ", "");
//                       writer2.println("<"+baseURI+"/passage/"+(i+1)+"/modification/"+mod_count+"/case/1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Paragraph>.");
//                       writer2.println("<"+baseURI+"/passage/"+(i+1)+"/modification/"+mod_count+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/passage/"+(i+1)+"/modification/"+mod_count+"/paragraph/"+num+">.");
//                       
//                    }
                    else {
                       writer2.println("<"+baseURI+"/passage/"+(i+1)+"/modification/"+mod_count+"/passage/1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Passage>.");
                       writer2.println("<"+baseURI+"/passage/"+(i+1)+"/modification/"+mod_count+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/passage/"+(i+1)+"/modification/"+mod_count+"/passage/1>.");
                       writer2.println("<"+baseURI+"/passage/"+(i+1)+"/modification/"+mod_count+"/passage/1> <http://legislation.di.uoa.gr/ontology/text> \""+mod.replace("$", "").replace("−\n", "").replace("−", "-").replace("\n", " ").replaceAll("\\p{C}", " ").replace("$", "")+"\"@el.");
                    }
                    mod_count++;
                  }
              }
          }
      }
      else{
          passage = paragraph.replace("−\n", "").replace("−", "-").replace("\n", " ").replaceAll("\\p{C}", " ").trim();
          mod_flag = 0;
          if(passage.endsWith("$")){
              mod_flag = 1;
              passage = passage.replace("$", "");
          }
          else if(passage.endsWith("$.")){
              mod_flag = 1;
              passage = passage.replace("$.", "");
          }
          else if(passage.endsWith("$ ")){
              mod_flag = 1;
              passage = passage.replace("$ ", "");
          }
          else if(passage.contains("$")){
              mod_flag = 1;
              passage = passage.replace("$", "");
          } 
          String HTMLpassage = tagLegislation(passage);
          String HTMLpassage2 = tagEntities(HTMLpassage);
          writer2.println("<"+baseURI+"/passage/1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Passage>.");
          writer2.println("<"+baseURI+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/passage/1>.");
          writer2.println("<"+baseURI+"/passage/1> <http://legislation.di.uoa.gr/ontology/text> \""+passage.replace("−\n", "").replace("−", "-").replace("\n", " ").replaceAll("\\p{C}", " ").replace("$", "")+"\"@el.");
          if(!HTMLpassage2.equals(passage)){
                  //System.out.println("HTML:"+HTMLpassage);
                  writer2.println("<"+baseURI+"/passage/1> <http://legislation.di.uoa.gr/ontology/html> \""+HTMLpassage2.replace("$", "")+"\".");
          }
          if(mod_flag==1){//((passage.contains("καταργείται:")||passage.contains("νέο εδάφιο:")||passage.contains("τα ακόλουθα εδάφια:")||passage.contains("ακόλουθο εδάφιο:")||passage.contains("τα εξής εδάφια:")||(passage.contains("εξής:")&&!passage.endsWith("».\n"))||(passage.contains("ακολούθως:")&&!passage.contains("ως ακολούθως: α"))||passage.endsWith(": "))&&!passage.contains("έχει ως εξής:")){
            mod_flag=0;
            if(!mods.isEmpty()){
                String mod = mods.get(0);
                mods.remove(0);
                writer2.println("<"+baseURI+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/passage/1/modification/"+mod_count+">.");
                //passage = passage.replace("  ", " ");
                String uri = findModificationTarget(passage);
                String uri2 = findModificationPatient(passage,uri);
                createModificationInfo(passage,uri,uri2,"passage/1");
                if(mod.contains("Άρθρο")||mod.contains("Άρθρο")){
                    baseURI+= "/passage/1/modification/"+mod_count;
                    parseParagraphs(mod.replace("$", ""),1);
                    baseURI = baseURI.split("/passage")[0];
                }
                else if(mod.split(" ")[0].matches("[0-9]+\\.(.*)")){
                   String num = mod.split("\\.",2)[0].replace(" ", "");
                   mod = mod.replaceFirst("[0-9]+\\.", "").replace("$", "");
                   writer2.println("<"+baseURI+"/passage/1/modification/"+mod_count+"/paragraph/"+num+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Paragraph>.");
                   writer2.println("<"+baseURI+"/passage/1/modification/"+mod_count+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/passage/1/modification/"+mod_count+"/paragraph/"+num+">.");
                   ArrayList<String> mod_passages = this.splitPassages(mod);
                   for(int k=0; k< mod_passages.size(); k++){
                        writer2.println("<"+baseURI+"/passage/1/modification/"+mod_count+"/paragraph/"+num+"/passage/"+(k+1)+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Passage>.");
                        writer2.println("<"+baseURI+"/passage/1/modification/"+mod_count+"/paragraph/"+num+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/passage/1/modification/"+mod_count+"/paragraph/"+num+"/passage/"+(k+1)+">.");
                        writer2.println("<"+baseURI+"/passage/1/modification/"+mod_count+"/paragraph/"+num+"/passage/"+(k+1)+"> <http://legislation.di.uoa.gr/ontology/text> \""+mod_passages.get(k).replace("−\n", "").replace("−", "-").replace("\n", " ").replaceAll("\\p{C}", " ").replace("$", "")+"\"@el.");
               
                   }
               
                }
                else {
                   writer2.println("<"+baseURI+"/passage/1/modification/"+mod_count+"/passage/1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Passage>.");
                   writer2.println("<"+baseURI+"/passage/1/modification/"+mod_count+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/passage/1/modification/"+mod_count+"/passage/1>.");
                   writer2.println("<"+baseURI+"/passage/1/modification/"+mod_count+"/passage/1> <http://legislation.di.uoa.gr/ontology/text> \""+mod.replace("$", "").replace("−\n", "").replace("−", "-").replace("\n", " ").replaceAll("\\p{C}", " ").replace("$", "")+"\"@el.");
                }
                mod_count++;
              }
              }      
      }
      
      
    }
    
    private String tagEntities(String passage){
        
        ArrayList<String> entities= new ArrayList();// = passage.split("(((([Α-Ω](\\.|-|')|[Α-ΩΆ-Ώ])+)+[α-ωά-ώ-])*(\\s(&)*(([Α-Ω](\\.|-|'))+|[Α-ΩΆ-Ώ])+[α-ωά-ώ-]*)+)");
        Pattern pattern = Pattern.compile("(((([Α-Ω](\\.|')|[Α-ΩΆ-Ώ0-9])+)+[α-ωά-ώ])*((\\s)*(&)*(-)*(και\\s)*(του\\s)*(της\\s)*(([Α-Ω](\\.|'))+|[Α-ΩΆ-Ώ0-9])+[α-ωά-ώ]*)+)");//"(((([Α-Ω](\\.|-|')|[Α-ΩΆ-Ώ])+)+[α-ωά-ώ-])*(\\s(&)*(([Α-Ω](\\.|-|'))+|[Α-ΩΆ-Ώ])+[α-ωά-ώ-]*)+)");
        Matcher matcher = pattern.matcher(passage);
        while(matcher.find()) {
            String entity = matcher.group(1);
            String tag= tagEntity(entity);
            if(!tag.isEmpty()){
                if(entity.startsWith(" ")){
                    passage = passage.replace(entity," <a href=\\\""+tag+"\\\">"+entity.replaceFirst(" ","")+"</a>");
                }
                else{
                    passage = passage.replace(entity, "<a href=\\\""+tag+"\\\">"+entity+"</a>");
                }
                writer2.println("<"+baseURI2+"> <http://legislation.di.uoa.gr/ontology/hasEntity> <"+tag+">.");
            }
        }
        
       
        return passage;
    } 
    
    private String tagEntity(String ent){
        
        String caps_ent = ei.capitalize(ent.trim());
        //SEARCH ENTITY
        //System.out.println(caps_ent);
        if(caps_ent.length()<30 && caps_ent.length()>10){
            ent  = ei.searchEntity(caps_ent);
        }
        else {
            ent = "";
        }
        return ent;
    }
    
    private void createModificationInfo(String passage, String uri, String uri2,String extension){
        
        writer2.println("<"+baseURI+"/"+extension+"/modification/"+mod_count+">  <http://www.metalex.eu/metalex/2008-05-02#legislativeCompetenceGround> <"+baseURI2+">.");
        
        if(!uri.equals("http://legislation.di.uoa.gr/")){
          writer2.println("<"+uri+"/"+date+">  <http://www.metalex.eu/metalex/2008-05-02#matterOf> <"+baseURI+"/"+extension+"/modification/"+mod_count+">.");
          if(!writer2.toString().contains("<"+uri+"> <http://www.metalex.eu/metalex/2008-05-02#realizedBy> <"+uri+"/"+date+">.")){
            writer2.println("<"+uri+"> <http://www.metalex.eu/metalex/2008-05-02#realizedBy> <"+uri+"/"+date+">.");   
          }
          if(!uri2.equals("")&&!uri2.contains("WRONG")&&!uri.equals("http://legislation.di.uoa.gr/")){
              writer2.println("<"+baseURI+"/"+extension+"/modification/"+mod_count+"> <http://www.metalex.eu/metalex/2008-05-02#patient> <"+uri+"/"+uri2+">.");
          }
        }
        if (passage.split(", όπως τροποποι").length>1){
            passage = passage.split(", όπως τροποποι(.*),")[0];
        }
        else if (passage.split(", όπως αντικ").length>1){
            passage = passage.split(", όπως αντικ(.*),")[0];
        }
        else if (passage.split(", όπως συμπλη").length>1){
            passage = passage.split(", όπως συμπλη(.*),")[0];
        }
        else if (passage.split(", όπως κωδ").length>1){
            passage = passage.split(", όπως συμπλη(.*),")[0];
        }
        
        if(passage.contains("αντικατ")||(passage.contains("τροποπο"))){
            writer2.println("<"+baseURI+"/"+extension+"/modification/"+mod_count+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Edit>.");
        }
        else if(passage.contains("προσθ")||passage.contains("προστ")){
            writer2.println("<"+baseURI+"/"+extension+"/modification/"+mod_count+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Addition>.");
        }
        else{
            writer2.println("<"+baseURI+"/"+extension+"/modification/"+mod_count+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Addition>.");
        }
    }
        
    private String findModificationPatient(String passage, String ld) {
        String patient = "";
        
        //System.out.println("PASSAGE: "+passage);
        
        if (passage.split(", όπως τροποποι").length>1){
            passage = passage.split(", όπως τροποποι")[0];
        }
        else if (passage.split(", όπως αντικ").length>1){
            passage = passage.split(", όπως αντικ")[0];
        }
        else if (passage.split(", όπως συμπλη").length>1){
            passage = passage.split(", όπως συμπλη")[0];
        }
        else if (passage.split(", όπως κωδ").length>1){
            passage = passage.split(", όπως συμπλη")[0];
        }
        else if (passage.split("προστίθεται").length>1){
            passage = passage.split("προστίθεται")[0];
        }
        else if (passage.split("προστίθενται").length>1){
            passage = passage.split("προστίθενται")[0];
        }
        
        if(passage.contains("Το άρθρο ")){
            String[] priors = passage.split("Το άρθρο ");
            if(priors.length>1){
            String[] ids = priors[1].split(" ");
            if(!patient.isEmpty()){
                patient +="/article/"+ids[0];
            }
            else{
                patient +="article/"+ids[0];
            }
            }
        }
        else if(passage.contains("του άρθρου ")){
            String[] priors = passage.split("του άρθρου ");
            if(priors.length>1){
                String[] ids = priors[1].split(" ");
                if(!patient.isEmpty()){
                    patient +="/article/"+ids[0];
                }
                else{
                    patient +="article/"+ids[0];
                }
            }
        }
        else if(passage.contains("άρθρου ")){
            String[] priors = passage.split("άρθρου ");
            if(priors.length>1){
            String[] ids = priors[1].split(" ");
            if(!patient.isEmpty()){
                patient +="/article/"+ids[0];
            }
            else{
                patient +="article/"+ids[0];
            }
            }
        }
        else if(passage.contains("Στο άρθρο ")){
            String[] priors = passage.split("Στο άρθρο ");
            if(priors.length>1){
            String[] ids = priors[1].split(" ");
            if(!patient.isEmpty()){
                patient +="/article/"+ids[0];
            }
            else{
                patient +="article/"+ids[0];
            }
            }
        }

        
        if(passage.contains("παρ. ")){
            String[] priors = passage.replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω\\.]", "#").split("παρ\\.#");
            if(priors.length>1){
            String[] ids = priors[1].split("#");
                    if(!patient.isEmpty()){
                        patient +="/paragraph/"+ids[0];
                    }
                    else{
                        patient +="paragraph/"+ids[0];
                    }
            }
            else{
                patient+="WRONG";
            }
        }
        else if(passage.contains("παρ. ")){
            String[] priors = passage.replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω\\.]", "#").split("παρ\\.#");
            if(priors.length>1){
            String[] ids = priors[1].split("#");
                    if(!patient.isEmpty()){
                        patient +="/paragraph/"+ids[0];
                    }
                    else{
                        patient +="paragraph/"+ids[0];
                    }
            }
            else{
                patient+="WRONG";
            }
        }
        else if(passage.contains("της παραγράφου ")){
            String[] priors = passage.split("της παραγράφου ");
            if(priors.length>1){
            String[] ids = priors[1].split(" ");
                    if(!patient.isEmpty()){
                        patient +="/paragraph/"+ids[0];
                    }
                    else{
                        patient +="paragraph/"+ids[0];
                    }
            }
        }
        else if(passage.contains(" παράγραφο ")){
            String[] priors = passage.split("παράγραφο ");
            if(priors.length>1){
            String[] ids = priors[1].split(" ");
                    if(!patient.isEmpty()){
                        patient +="/paragraph/"+ids[0];
                    }
                    else{
                        patient +="paragraph/"+ids[0];
                    }
            }
        }
        else if(passage.contains("δεύτερη παράγραφος")){
            patient +="/paragraph/2";
        }
        else if(passage.contains("πρώτη παράγραφος")){
            patient +="/paragraph/1";
        }
        
        if(passage.contains("δεύτερο εδάφιο")){
            patient +="/passage/2";
        }
        else if (passage.contains("πρώτο εδάφιο")){
            patient +="/passage/1";
        }
        else if(passage.contains("εδάφιο ")&&!passage.contains("προστίθεται εδάφιο")){
            String[] priors = passage.split("εδάφιο ");
            if(priors.length>1){
            String[] ids = priors[1].split(" ");
            if(ids[0].matches("[0-9]+")){
                    patient +="/passage/"+Integer.parseInt(ids[0]);
            }
            }
        }
        
        if(passage.contains("πρώτη περίπτωση")){
            patient +="/case/1";
        }
        else if (passage.contains("δεύτερη περίπτωση")){
            patient +="/case/2";
        }
//        else if(passage.contains("περίπτωση ")){
//            String[] priors = passage.split("περίπτωση ");
//            if(priors.length>1){
//            String[] ids = priors[1].split(" ");
//            patient +="/case/"+ids[0];
//            }
//        }
//        else if(passage.contains("περίπτωση ")){
//            String[] priors = passage.replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω]", "#").split("περίπτωση#");
//            if(priors.length>1){
//            String[] ids = priors[1].split(" ");
//            patient +="/case/"+ids[0];
//            }
//        }
//        else if(passage.contains("περίπτωση ")){
//            String[] priors = passage.replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω]", "#").split("περίπτωση#");
//            if(priors.length>1){
//            String[] ids = priors[1].split(" ");
//            patient +="/case/"+ids[0];
//            }
//        }
//        else if(passage.contains("της περίπτωσης ")){
//            String[] priors = passage.split("της περίπτωσης ");
//            if(priors.length>1){
//            String[] ids = priors[1].split(" ");
//            patient +="/case/"+ids[0];
//            }
//        }
        
//        if(patient.isEmpty()){
//            System.out.println("PATIENT: -");
//        }
//        else{
//            System.out.println("PATIENT: "+patient);
//        }
        return patient;
    }
    
    private String tagLegislation(String passage){
        String uri = "http://legislation.di.uoa.gr/";
        ArrayList<String> entities= new ArrayList();
        Pattern pattern = Pattern.compile("(Π\\.Δ\\.|Π\\.δ\\.|π\\.δ\\.|Ν\\.|ν\\.δ\\.|ν\\.|N\\.|β\\.δ\\.|Προεδρικού Διατάγματος|Π\\.Δ\\/τος)\\s[0-9]+\\/[0-9]+");
        Matcher matcher = pattern.matcher(passage);
        while(matcher.find()) {
            String leg = matcher.group();
            System.out.println(leg);
            Pattern pattern2 = Pattern.compile("[0-9]+\\/[0-9]+");
            Matcher matcher2 = pattern2.matcher(leg);
            matcher2.find();
            String ids = matcher2.group();
            String[] numbers = ids.split("\\/");
            if(numbers.length==2){
                if(leg.contains("ν.δ.")){
                     uri += "law/";
                }
                else if(leg.contains("π.δ.")){
                     uri += "pd/";
                }
                else if(leg.contains("Π.δ.")){
                     uri += "pd/";
                }
                else if(leg.contains("Π.Δ.")){
                     uri += "pd/";
                }
                else if(leg.contains("ν.")){
                     uri += "law/";
                }
                else if(leg.contains("Ν.")){
                     uri += "law/";
                }
                else if(leg.contains("β.δ.")){
                     uri += "rd/";
                }
                else if(leg.contains("Προεδρικού Διατάγματος")){
                     uri += "pd/";
                }
                else if(leg.contains("ΠΔ/τος")){
                     uri += "pd/";
                }

                uri += numbers[1] + "/" + numbers[0];
                passage = passage.replace(leg," <a href=\\\""+uri+"\\\">"+leg+"</a>");
            }
        }
        
       
        return passage;
        
    }
            
    String findModificationTarget(String passage){
        
        if (passage.split(", όπως τροποποι").length>1){
            passage = passage.split(", όπως τροποποι")[0];
        }
        else if (passage.split(", όπως αντικ").length>1){
            passage = passage.split(", όπως αντικ")[0];
        }
        else if (passage.split(", όπως συμπλη").length>1){
            passage = passage.split(", όπως συμπλη")[0];
        }
        else if (passage.split(", όπως κωδ").length>1){
            passage = passage.split(", όπως συμπλη")[0];
        }
        else if (passage.split("προστίθεται").length>1){
            passage = passage.split("προστίθεται")[0];
        }
        else if (passage.split("προστίθενται").length>1){
            passage = passage.split("προστίθενται")[0];
        }
        
//        System.out.println("PASSAGE: "+passage);

        String uri = "http://legislation.di.uoa.gr/";
        String after= "";
        String id="";
        try{
            if(passage.contains(" ν.δ. ")){
                String[] priors = passage.split(" ν\\.δ\\. ");
                String[] ids;
                if(priors[1].matches("[0-9]+\\/[0-9]+ (.*)")){
                    ids = priors[1].replace("/ ", "/").split(" ",2);
                    after = ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+\\((.*)")){
                    ids = priors[1].replace("/ ", "/").split("\\(",2);
                    after = "(" + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+«(.*)")){
                    ids = priors[1].replace("/ ", "/").split("«",2);
                    after = "«" + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+,(.*)")){
                    ids = priors[1].replace("/ ", "/").split(",",2);
                    after = "," + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+:(.*)")){
                    ids = priors[1].replace("/ ", "/").split(":",2);
                    after = ":" + ids[1];
                    id = ids[0];
                }
                else{
                    return passage;
                }
                ids = ids[0].replace("/ ", "/").split("/");
                uri +="law/"+ids[1].replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω]", "")+"/"+ids[0].replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω]", "");
                //System.out.println("TARGET: "+passage);

                //System.out.println("TARGET: "+uri);
            }
            else if(passage.contains(" ν.δ. ")){
                String[] priors = passage.split(" ν\\.δ\\. ");
                String[] ids;
                if(priors[1].matches("[0-9]+\\/[0-9]+ (.*)")){
                    ids = priors[1].replace("/ ", "/").split(" ",2);
                    after = ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+\\((.*)")){
                    ids = priors[1].replace("/ ", "/").split("\\(",2);
                    after = "(" + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+«(.*)")){
                    ids = priors[1].replace("/ ", "/").split("«",2);
                    after = "«" + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+,(.*)")){
                    ids = priors[1].replace("/ ", "/").split(",",2);
                    after = "," + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+:(.*)")){
                    ids = priors[1].replace("/ ", "/").split(":",2);
                    after = ":" + ids[1];
                    id = ids[0];
                }
                else{
                    return passage;
                }
                ids = ids[0].replace("/ ", "/").split("/");
                uri +="law/"+ids[1].replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω]", "")+"/"+ids[0].replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω]", "");
                //System.out.println("TARGET: "+passage);
                //System.out.println("TARGET: "+uri);
            }
            else if(passage.contains(" ν. ")){
                String[] priors = passage.split(" ν\\. ");
                String[] ids;
                if(priors[1].matches("[0-9]+\\/[0-9]+ (.*)")){
                    ids = priors[1].replace("/ ", "/").split(" ",2);
                    after = ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+\\((.*)")){
                    ids = priors[1].replace("/ ", "/").split("\\(",2);
                    after = "(" + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+«(.*)")){
                    ids = priors[1].replace("/ ", "/").split("«",2);
                    after = "«" + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+,(.*)")){
                    ids = priors[1].replace("/ ", "/").split(",",2);
                    after = "," + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+:(.*)")){
                    ids = priors[1].replace("/ ", "/").split(":",2);
                    after = ":" + ids[1];
                    id = ids[0];
                }
                else{
                    return passage;
                }
                ids = ids[0].replace("/ ", "/").split("/");
                uri +="law/"+ids[1].replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω]", "")+"/"+ids[0].replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω]", "");
                //System.out.println("TARGET: "+passage);
                //System.out.println("TARGET: "+uri);
            }
            else if(passage.contains(" ν.")){
                String[] priors = passage.split(" ν\\.");
                String[] ids;
                if(priors[1].matches("[0-9]+\\/[0-9]+ (.*)")){
                    ids = priors[1].replace("/ ", "/").split(" ",2);
                    after = ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+\\((.*)")){
                    ids = priors[1].replace("/ ", "/").split("\\(",2);
                    after = "(" + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+«(.*)")){
                    ids = priors[1].replace("/ ", "/").split("«",2);
                    after = "«" + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+,(.*)")){
                    ids = priors[1].replace("/ ", "/").split(",",2);
                    after = "," + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+:(.*)")){
                    ids = priors[1].replace("/ ", "/").split(":",2);
                    after = ":" + ids[1];
                    id = ids[0];
                }
                else{
                    return passage;
                }
                ids = ids[0].replace("/ ", "/").split("/");
                uri +="law/"+ids[1].replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω]", "").replace(" ", "")+"/"+ids[0].replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω]", "");
                //System.out.println("TARGET: "+passage);
                //System.out.println("TARGET: "+uri);
            }
            else if(passage.contains(" ν ")){
                String[] priors = passage.split(" ν ");
                String[] ids;
                if(priors[1].matches("[0-9]+\\/[0-9]+ (.*)")){
                    ids = priors[1].replace("/ ", "/").split(" ",2);
                    after = ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+\\((.*)")){
                    ids = priors[1].replace("/ ", "/").split("\\(",2);
                    after = "(" + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+«(.*)")){
                    ids = priors[1].replace("/ ", "/").split("«",2);
                    after = "«" + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+,(.*)")){
                    ids = priors[1].replace("/ ", "/").split(",",2);
                    after = "," + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+:(.*)")){
                    ids = priors[1].replace("/ ", "/").split(":",2);
                    after = ":" + ids[1];
                    id = ids[0];
                }
                else{
                    return passage;
                }
                ids = ids[0].replace("/ ", "/").split("/");
                uri +="law/"+ids[1].replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω]", "").replace(" ", "")+"/"+ids[0].replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω]", "");
                //System.out.println("TARGET: "+passage);
                //System.out.println("TARGET: "+uri);
            }
            else if(passage.contains(" Ν. ")){
                String[] priors = passage.split(" Ν\\. ");
                String[] ids;
                if(priors[1].matches("[0-9]+\\/[0-9]+ (.*)")){
                    ids = priors[1].replace("/ ", "/").split(" ",2);
                    after = ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+\\((.*)")){
                    ids = priors[1].replace("/ ", "/").split("\\(",2);
                    after = "(" + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+«(.*)")){
                    ids = priors[1].replace("/ ", "/").split("«",2);
                    after = "«" + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+,(.*)")){
                    ids = priors[1].replace("/ ", "/").split(",",2);
                    after = "," + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+:(.*)")){
                    ids = priors[1].replace("/ ", "/").split(":",2);
                    after = ":" + ids[1];
                    id = ids[0];
                }
                else{
                    return passage;
                }
                ids = ids[0].replace("/ ", "/").split("/");
                uri +="law/"+ids[1].replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω]", "").replace(" ", "")+"/"+ids[0].replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω]", "");
                //System.out.println("TARGET: "+passage);
            }
            else if(passage.contains(" (ν.")){
                String[] priors = passage.split(" \\(ν\\. ");
                String[] ids;
                if(priors[1].matches("[0-9]+\\/[0-9]+ (.*)")){
                    ids = priors[1].replace("/ ", "/").split(" ",2);
                    after = ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+\\((.*)")){
                    ids = priors[1].replace("/ ", "/").split("\\(",2);
                    after = "(" + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+«(.*)")){
                    ids = priors[1].replace("/ ", "/").split("«",2);
                    after = "«" + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+,(.*)")){
                    ids = priors[1].replace("/ ", "/").split(",",2);
                    after = "," + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+:(.*)")){
                    ids = priors[1].replace("/ ", "/").split(":",2);
                    after = ":" + ids[1];
                    id = ids[0];
                }
                else{
                    return passage;
                }
                ids = ids[0].replace("/ ", "/").split("/");
                uri +="pd/"+ids[1].replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω]", "")+"/"+ids[0].replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω]", "");
                //System.out.println("TARGET: "+passage);
               //System.out.println("TARGET: "+uri);
            }
            else if(passage.matches("(.*) Ν\\.[1-9]+(.*)")){
                String[] priors = passage.split(" Ν\\.");
                String[] ids;
                if(priors[1].matches("[0-9]+\\/[0-9]+ (.*)")){
                    ids = priors[1].replace("/ ", "/").split(" ",2);
                    after = ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+\\((.*)")){
                    ids = priors[1].replace("/ ", "/").split("\\(",2);
                    after = "(" + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+«(.*)")){
                    ids = priors[1].replace("/ ", "/").split("«",2);
                    after = "«" + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+,(.*)")){
                    ids = priors[1].replace("/ ", "/").split(",",2);
                    after = "," + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+:(.*)")){
                    ids = priors[1].replace("/ ", "/").split(":",2);
                    after = ":" + ids[1];
                    id = ids[0];
                }
                else{
                    return passage;
                }
                ids = ids[0].replace("/ ", "/").split("/");
                uri +="law/"+ids[1].replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω]", "").replace(" ", "")+"/"+ids[0].replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω]", "");
                //System.out.println("TARGET: "+uri);
                //System.out.println("TARGET: "+passage1);
            }
            else if(passage.matches("(.*) β\\.δ\\. (.*)")){
                String[] priors = passage.split(" β\\.δ\\. ");
                String[] ids;
                if(priors[1].matches("[0-9]+\\/[0-9]+ (.*)")){
                    ids = priors[1].replace("/ ", "/").split(" ",2);
                    after = ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+\\((.*)")){
                    ids = priors[1].replace("/ ", "/").split("\\(",2);
                    after = "(" + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+«(.*)")){
                    ids = priors[1].replace("/ ", "/").split("«",2);
                    after = "«" + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+,(.*)")){
                    ids = priors[1].replace("/ ", "/").split(",",2);
                    after = "," + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+:(.*)")){
                    ids = priors[1].replace("/ ", "/").split(":",2);
                    after = ":" + ids[1];
                    id = ids[0];
                }
                else{
                    return passage;
                }
                ids = ids[0].replace("/ ", "/").split("/");
                uri +="rd/"+ids[1].replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω]", "").replace(" ", "")+"/"+ids[0].replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω]", "");
                //System.out.println("TARGET: "+uri);
                //System.out.println("TARGET: "+passage);
            }
            else if(passage.matches("(.*) Ν\\.[0-9]+(.*)")){
                String[] priors = passage.split(" Ν\\.");
                String[] ids;
                if(priors[1].matches("[0-9]+\\/[0-9]+ (.*)")){
                    ids = priors[1].replace("/ ", "/").split(" ",2);
                    after = ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+\\((.*)")){
                    ids = priors[1].replace("/ ", "/").split("\\(",2);
                    after = "(" + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+«(.*)")){
                    ids = priors[1].replace("/ ", "/").split("«",2);
                    after = "«" + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+,(.*)")){
                    ids = priors[1].replace("/ ", "/").split(",",2);
                    after = "," + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+:(.*)")){
                    ids = priors[1].replace("/ ", "/").split(":",2);
                    after = ":" + ids[1];
                    id = ids[0];
                }
                else{
                    return passage;
                }
                ids = ids[0].replace("/ ", "/").split("/");
                uri +="law/"+ids[1].replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω]", "").replace(" ", "")+"/"+ids[0].replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω]", "");
                //System.out.println("TARGET: "+passage);
                //System.out.println("TARGET: "+uri);
            }
            else if(passage.matches("(.*) π\\.δ\\. [0-9]+\\/(.*)")){
                String[] priors = passage.split(" π\\.δ\\. ");
                String[] ids;
                if(priors[1].matches("[0-9]+\\/[0-9]+ (.*)")){
                    ids = priors[1].replace("/ ", "/").split(" ",2);
                    after = ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+\\((.*)")){
                    ids = priors[1].replace("/ ", "/").split("\\(",2);
                    after = "(" + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+«(.*)")){
                    ids = priors[1].replace("/ ", "/").split("«",2);
                    after = "«" + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+,(.*)")){
                    ids = priors[1].replace("/ ", "/").split(",",2);
                    after = "," + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+:(.*)")){
                    ids = priors[1].replace("/ ", "/").split(":",2);
                    after = ":" + ids[1];
                    id = ids[0];
                }
                else{
                    return passage;
                }
                ids = ids[0].replace("/ ", "/").split("/");
                uri +="pd/"+ids[1].replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω]", "").replace(" ", "")+"/"+ids[0].replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω]", "");
                //System.out.println("TARGET: "+passage);
                //System.out.println("TARGET: "+uri);
            }
            else if(passage.contains(" Π.δ. ")){
                String[] priors = passage.split(" Π\\.δ\\. ");
                String[] ids;
                if(priors[1].matches("[0-9]+\\/[0-9]+ (.*)")){
                    ids = priors[1].replace("/ ", "/").split(" ",2);
                    after = ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+\\((.*)")){
                    ids = priors[1].replace("/ ", "/").split("\\(",2);
                    after = "(" + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+«(.*)")){
                    ids = priors[1].replace("/ ", "/").split("«",2);
                    after = "«" + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+,(.*)")){
                    ids = priors[1].replace("/ ", "/").split(",",2);
                    after = "," + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+:(.*)")){
                    ids = priors[1].replace("/ ", "/").split(":",2);
                    after = ":" + ids[1];
                    id = ids[0];
                }
                else{
                    return passage;
                }
                ids = ids[0].replace("/ ", "/").split("/");
                uri +="pd/"+ids[1].replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω]", "").replace(" ", "")+"/"+ids[0].replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω]", "");
                //System.out.println("TARGET: "+passage1);
                //System.out.println("TARGET: "+uri);

            }
            else if(passage.contains(" Π.δ.")){
                String[] priors = passage.split(" Π\\.δ\\.");
                String[] ids;
                if(priors[1].matches("[0-9]+\\/[0-9]+ (.*)")){
                    ids = priors[1].replace("/ ", "/").split(" ",2);
                    after = ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+\\((.*)")){
                    ids = priors[1].replace("/ ", "/").split("\\(",2);
                    after = "(" + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+«(.*)")){
                    ids = priors[1].replace("/ ", "/").split("«",2);
                    after = "«" + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+,(.*)")){
                    ids = priors[1].replace("/ ", "/").split(",",2);
                    after = "," + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+:(.*)")){
                    ids = priors[1].replace("/ ", "/").split(":",2);
                    after = ":" + ids[1];
                    id = ids[0];
                }
                else{
                    return passage;
                }
                ids = ids[0].replace("/ ", "/").split("/");
                uri +="pd/"+ids[1].replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω]", "").replace(" ", "")+"/"+ids[0].replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω]", "");
                //System.out.println("TARGET: "+passage1);
                //System.out.println("TARGET: "+uri);
            }
            else if(passage.contains(" Π.Δ. ")){
                String[] priors = passage.split(" Π\\.Δ\\. ");
                String[] ids;
                if(priors[1].matches("[0-9]+\\/[0-9]+ (.*)")){
                    ids = priors[1].replace("/ ", "/").split(" ",2);
                    after = ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+\\((.*)")){
                    ids = priors[1].replace("/ ", "/").split("\\(",2);
                    after = "(" + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+«(.*)")){
                    ids = priors[1].replace("/ ", "/").split("«",2);
                    after = "«" + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+,(.*)")){
                    ids = priors[1].replace("/ ", "/").split(",",2);
                    after = "," + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+:(.*)")){
                    ids = priors[1].replace("/ ", "/").split(":",2);
                    after = ":" + ids[1];
                    id = ids[0];
                }
                else{
                    return passage;
                }
                ids = ids[0].replace("/ ", "/").split("/");
                uri +="pd/"+ids[1].replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω]", "")+"/"+ids[0].replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω]", "");
            }
            else if(passage.contains("του Προεδρικού Διατάγματος")){
               String[] priors = passage.split("του Προεδρικού Διατάγματος ");
               String[] ids = priors[1].replace("/ ", "/").split(" ");
               ids = ids[0].replace("/ ", "/").split("/");
               uri +="pd/"+ids[1].replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω]", "")+"/"+ids[0].replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω]", "");
               //System.out.println("TARGET: "+uri);
            }
        }
        catch (IndexOutOfBoundsException e){
            uri="http://legislation.di.uoa.gr/";
        }

        
        return uri;
    }
    
    String createHTMLpassage(String passage){
        
//        System.out.println("PASSAGE: "+passage);
        String passage1 = "";
        String uri = "http://legislation.di.uoa.gr/";
        String after = "";
        String id = "";
        
        try{
            if(passage.contains("ν.δ. ")){
                String[] priors = passage.split("ν\\.δ\\. ");
                String[] ids;
                if(priors[1].matches("[0-9]+\\/[0-9]+ (.*)")){
                    ids = priors[1].replace("/ ", "/").split(" ",2);
                    after = ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+\\((.*)")){
                    ids = priors[1].replace("/ ", "/").split("\\(",2);
                    after = "(" + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+«(.*)")){
                    ids = priors[1].replace("/ ", "/").split("«",2);
                    after = "«" + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+,(.*)")){
                    ids = priors[1].replace("/ ", "/").split(",",2);
                    after = "," + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+:(.*)")){
                    ids = priors[1].replace("/ ", "/").split(":",2);
                    after = ":" + ids[1];
                    id = ids[0];
                }
                else{
                    return passage;
                }
                ids = ids[0].replace("/ ", "/").split("/");
                uri +="law/"+ids[1].replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω]", "")+"/"+ids[0].replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω]", "");
                passage1 = createHTMLpassage(priors[0]) + " <a href=\\\"" + uri +"\\\">"+ "ν.δ. " + id + "</a> " + createHTMLpassage(after);
                //System.out.println("TARGET: "+passage);

                //System.out.println("TARGET: "+uri);
            }
            else if(passage.contains("ν.δ. ")){
                String[] priors = passage.split("ν\\.δ\\. ");
                String[] ids;
                if(priors[1].matches("[0-9]+\\/[0-9]+ (.*)")){
                    ids = priors[1].replace("/ ", "/").split(" ",2);
                    after = ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+\\((.*)")){
                    ids = priors[1].replace("/ ", "/").split("\\(",2);
                    after = "(" + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+«(.*)")){
                    ids = priors[1].replace("/ ", "/").split("«",2);
                    after = "«" + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+,(.*)")){
                    ids = priors[1].replace("/ ", "/").split(",",2);
                    after = "," + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+:(.*)")){
                    ids = priors[1].replace("/ ", "/").split(":",2);
                    after = ":" + ids[1];
                    id = ids[0];
                }
                else{
                    return passage;
                }
                ids = ids[0].replace("/ ", "/").split("/");
                uri +="law/"+ids[1].replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω]", "")+"/"+ids[0].replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω]", "");
                passage1 = createHTMLpassage(priors[0]) + " <a href=\\\"" + uri +"\\\">"+ "ν.δ. " + id + "</a> " + createHTMLpassage(after);
                //System.out.println("TARGET: "+passage);
                //System.out.println("TARGET: "+uri);
            }
            else if(passage.contains(" ν. ")){
                String[] priors = passage.split(" ν\\. ");
                String[] ids;
                if(priors[1].matches("[0-9]+\\/[0-9]+ (.*)")){
                    ids = priors[1].replace("/ ", "/").split(" ",2);
                    after = ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+\\((.*)")){
                    ids = priors[1].replace("/ ", "/").split("\\(",2);
                    after = "(" + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+«(.*)")){
                    ids = priors[1].replace("/ ", "/").split("«",2);
                    after = "«" + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+,(.*)")){
                    ids = priors[1].replace("/ ", "/").split(",",2);
                    after = "," + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+:(.*)")){
                    ids = priors[1].replace("/ ", "/").split(":",2);
                    after = ":" + ids[1];
                    id = ids[0];
                }
                else{
                    return passage;
                }
                ids = ids[0].replace("/ ", "/").split("/");
                uri +="law/"+ids[1].replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω]", "")+"/"+ids[0].replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω]", "");
                passage1 = createHTMLpassage(priors[0]) + " <a href=\\\"" + uri +"\\\">"+ "ν. " + id + "</a> " + createHTMLpassage(after);
                //System.out.println("TARGET: "+passage);
                //System.out.println("TARGET: "+uri);
            }
            else if(passage.contains(" ν.")){
                String[] priors = passage.split(" ν\\.");
                String[] ids;
                if(priors[1].matches("[0-9]+\\/[0-9]+ (.*)")){
                    ids = priors[1].replace("/ ", "/").split(" ",2);
                    after = ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+\\((.*)")){
                    ids = priors[1].replace("/ ", "/").split("\\(",2);
                    after = "(" + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+«(.*)")){
                    ids = priors[1].replace("/ ", "/").split("«",2);
                    after = "«" + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+,(.*)")){
                    ids = priors[1].replace("/ ", "/").split(",",2);
                    after = "," + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+:(.*)")){
                    ids = priors[1].replace("/ ", "/").split(":",2);
                    after = ":" + ids[1];
                    id = ids[0];
                }
                else{
                    return passage;
                }
                ids = ids[0].replace("/ ", "/").split("/");
                uri +="law/"+ids[1].replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω]", "").replace(" ", "")+"/"+ids[0].replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω]", "");
                passage1 = createHTMLpassage(priors[0]) + " <a href=\\\"" + uri +"\\\">"+ "ν." + id + "</a> " + createHTMLpassage(after);
                //System.out.println("TARGET: "+passage);
                //System.out.println("TARGET: "+uri);
            }
            else if(passage.contains(" ν ")){
                String[] priors = passage.split(" ν ");
                String[] ids;
                if(priors[1].matches("[0-9]+\\/[0-9]+ (.*)")){
                    ids = priors[1].replace("/ ", "/").split(" ",2);
                    after = ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+\\((.*)")){
                    ids = priors[1].replace("/ ", "/").split("\\(",2);
                    after = "(" + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+«(.*)")){
                    ids = priors[1].replace("/ ", "/").split("«",2);
                    after = "«" + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+,(.*)")){
                    ids = priors[1].replace("/ ", "/").split(",",2);
                    after = "," + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+:(.*)")){
                    ids = priors[1].replace("/ ", "/").split(":",2);
                    after = ":" + ids[1];
                    id = ids[0];
                }
                else{
                    return passage;
                }
                ids = ids[0].replace("/ ", "/").split("/");
                uri +="law/"+ids[1].replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω]", "").replace(" ", "")+"/"+ids[0].replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω]", "");
                passage1 = createHTMLpassage(priors[0]) + " <a href=\\\"" + uri +"\\\">"+ "ν." + id + "</a> " + createHTMLpassage(after);
                //System.out.println("TARGET: "+passage);
                //System.out.println("TARGET: "+uri);
            }
            else if(passage.contains(" Ν. ")){
                String[] priors = passage.split(" Ν\\. ");
                String[] ids;
                if(priors[1].matches("[0-9]+\\/[0-9]+ (.*)")){
                    ids = priors[1].replace("/ ", "/").split(" ",2);
                    after = ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+\\((.*)")){
                    ids = priors[1].replace("/ ", "/").split("\\(",2);
                    after = "(" + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+«(.*)")){
                    ids = priors[1].replace("/ ", "/").split("«",2);
                    after = "«" + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+,(.*)")){
                    ids = priors[1].replace("/ ", "/").split(",",2);
                    after = "," + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+:(.*)")){
                    ids = priors[1].replace("/ ", "/").split(":",2);
                    after = ":" + ids[1];
                    id = ids[0];
                }
                else{
                    return passage;
                }
                ids = ids[0].replace("/ ", "/").split("/");
                uri +="law/"+ids[1].replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω]", "").replace(" ", "")+"/"+ids[0].replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω]", "");
                passage1 = createHTMLpassage(priors[0]) + " <a href=\\\"" + uri +"\\\">"+ "Ν. " + id + "</a> " + createHTMLpassage(after);
                //System.out.println("TARGET: "+passage);
            }
            else if(passage.contains("(ν.")){
                String[] priors = passage.split("\\(ν\\. ");
                String[] ids;
                if(priors[1].matches("[0-9]+\\/[0-9]+ (.*)")){
                    ids = priors[1].replace("/ ", "/").split(" ",2);
                    after = ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+\\((.*)")){
                    ids = priors[1].replace("/ ", "/").split("\\(",2);
                    after = "(" + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+«(.*)")){
                    ids = priors[1].replace("/ ", "/").split("«",2);
                    after = "«" + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+,(.*)")){
                    ids = priors[1].replace("/ ", "/").split(",",2);
                    after = "," + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+:(.*)")){
                    ids = priors[1].replace("/ ", "/").split(":",2);
                    after = ":" + ids[1];
                    id = ids[0];
                }
                else{
                    return passage;
                }
                ids = ids[0].replace("/ ", "/").split("/");
                uri +="pd/"+ids[1].replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω]", "")+"/"+ids[0].replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω]", "");
                passage1 = createHTMLpassage(priors[0]) + "(<a href=\\\"" + uri +"\\\">"+ "ν. " + id + "</a> " + createHTMLpassage(after);
                //System.out.println("TARGET: "+passage);
               //System.out.println("TARGET: "+uri);
            }
            else if(passage.matches("(.*)Ν\\.[1-9]+(.*)")){
                String[] priors = passage.split("Ν\\.");
                String[] ids;
                if(priors[1].matches("[0-9]+\\/[0-9]+ (.*)")){
                    ids = priors[1].replace("/ ", "/").split(" ",2);
                    after = ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+\\((.*)")){
                    ids = priors[1].replace("/ ", "/").split("\\(",2);
                    after = "(" + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+«(.*)")){
                    ids = priors[1].replace("/ ", "/").split("«",2);
                    after = "«" + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+,(.*)")){
                    ids = priors[1].replace("/ ", "/").split(",",2);
                    after = "," + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+:(.*)")){
                    ids = priors[1].replace("/ ", "/").split(":",2);
                    after = ":" + ids[1];
                    id = ids[0];
                }
                else{
                    return passage;
                }
                ids = ids[0].replace("/ ", "/").split("/");
                uri +="law/"+ids[1].replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω]", "").replace(" ", "")+"/"+ids[0].replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω]", "");
                //System.out.println("TARGET: "+uri);
                passage1 = createHTMLpassage(priors[0]) + " <a href=\\\"" + uri +"\\\">"+ "Ν. " + id + "</a> " + createHTMLpassage(after);
                //System.out.println("TARGET: "+passage1);
            }
            else if(passage.matches("(.*)β\\.δ\\. (.*)")){
                String[] priors = passage.split("β\\.δ\\. ");
                String[] ids;
                if(priors[1].matches("[0-9]+\\/[0-9]+ (.*)")){
                    ids = priors[1].replace("/ ", "/").split(" ",2);
                    after = ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+\\((.*)")){
                    ids = priors[1].replace("/ ", "/").split("\\(",2);
                    after = "(" + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+«(.*)")){
                    ids = priors[1].replace("/ ", "/").split("«",2);
                    after = "«" + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+,(.*)")){
                    ids = priors[1].replace("/ ", "/").split(",",2);
                    after = "," + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+:(.*)")){
                    ids = priors[1].replace("/ ", "/").split(":",2);
                    after = ":" + ids[1];
                    id = ids[0];
                }
                else{
                    return passage;
                }
                ids = ids[0].replace("/ ", "/").split("/");
                uri +="rd/"+ids[1].replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω]", "").replace(" ", "")+"/"+ids[0].replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω]", "");
                //System.out.println("TARGET: "+uri);
                passage1 = createHTMLpassage(priors[0]) + " <a href=\\\"" + uri +"\\\">"+ "β.δ. " + id + "</a> " + createHTMLpassage(after);
                //System.out.println("TARGET: "+passage);
            }
            else if(passage.matches("(.*)Ν\\.[0-9]+(.*)")){
                String[] priors = passage.split("Ν\\.");
                String[] ids;
                if(priors[1].matches("[0-9]+\\/[0-9]+ (.*)")){
                    ids = priors[1].replace("/ ", "/").split(" ",2);
                    after = ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+\\((.*)")){
                    ids = priors[1].replace("/ ", "/").split("\\(",2);
                    after = "(" + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+«(.*)")){
                    ids = priors[1].replace("/ ", "/").split("«",2);
                    after = "«" + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+,(.*)")){
                    ids = priors[1].replace("/ ", "/").split(",",2);
                    after = "," + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+:(.*)")){
                    ids = priors[1].replace("/ ", "/").split(":",2);
                    after = ":" + ids[1];
                    id = ids[0];
                }
                else{
                    return passage;
                }
                ids = ids[0].replace("/ ", "/").split("/");
                uri +="law/"+ids[1].replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω]", "").replace(" ", "")+"/"+ids[0].replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω]", "");
                passage1 = createHTMLpassage(priors[0]) + " <a href=\\\"" + uri +"\\\">"+ "Ν." + id + "</a> " + createHTMLpassage(after);
                //System.out.println("TARGET: "+passage);
                //System.out.println("TARGET: "+uri);
            }
            else if(passage.matches("(.*)π\\.δ\\. [0-9]+\\/(.*)")){
                String[] priors = passage.split("π\\.δ\\. ");
                String[] ids;
                if(priors[1].matches("[0-9]+\\/[0-9]+ (.*)")){
                    ids = priors[1].replace("/ ", "/").split(" ",2);
                    after = ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+\\((.*)")){
                    ids = priors[1].replace("/ ", "/").split("\\(",2);
                    after = "(" + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+«(.*)")){
                    ids = priors[1].replace("/ ", "/").split("«",2);
                    after = "«" + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+,(.*)")){
                    ids = priors[1].replace("/ ", "/").split(",",2);
                    after = "," + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+:(.*)")){
                    ids = priors[1].replace("/ ", "/").split(":",2);
                    after = ":" + ids[1];
                    id = ids[0];
                }
                else{
                    return passage;
                }
                ids = ids[0].replace("/ ", "/").split("/");
                uri +="pd/"+ids[1].replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω]", "").replace(" ", "")+"/"+ids[0].replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω]", "");
                passage1 = createHTMLpassage(priors[0]) + " <a href=\\\"" + uri +"\\\">"+ "π.δ. " + id + "</a> " + createHTMLpassage(after);
                //System.out.println("TARGET: "+passage);
                //System.out.println("TARGET: "+uri);
            }
            else if(passage.contains("Π.δ. ")){
                String[] priors = passage.split("Π\\.δ\\. ");
                String[] ids;
                if(priors[1].matches("[0-9]+\\/[0-9]+ (.*)")){
                    ids = priors[1].replace("/ ", "/").split(" ",2);
                    after = ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+\\((.*)")){
                    ids = priors[1].replace("/ ", "/").split("\\(",2);
                    after = "(" + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+«(.*)")){
                    ids = priors[1].replace("/ ", "/").split("«",2);
                    after = "«" + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+,(.*)")){
                    ids = priors[1].replace("/ ", "/").split(",",2);
                    after = "," + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+:(.*)")){
                    ids = priors[1].replace("/ ", "/").split(":",2);
                    after = ":" + ids[1];
                    id = ids[0];
                }
                else{
                    return passage;
                }
                ids = ids[0].replace("/ ", "/").split("/");
                uri +="pd/"+ids[1].replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω]", "").replace(" ", "")+"/"+ids[0].replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω]", "");
                passage1 = createHTMLpassage(priors[0]) + " <a href=\\\"" + uri +"\\\">"+ "Π.δ. " + id + "</a> " + createHTMLpassage(after);
                //System.out.println("TARGET: "+passage1);
                //System.out.println("TARGET: "+uri);

            }
            else if(passage.contains("Π.δ.")){
                String[] priors = passage.split(" Π\\.δ\\.");
                String[] ids;
                if(priors[1].matches("[0-9]+\\/[0-9]+ (.*)")){
                    ids = priors[1].replace("/ ", "/").split(" ",2);
                    after = ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+\\((.*)")){
                    ids = priors[1].replace("/ ", "/").split("\\(",2);
                    after = "(" + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+«(.*)")){
                    ids = priors[1].replace("/ ", "/").split("«",2);
                    after = "«" + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+,(.*)")){
                    ids = priors[1].replace("/ ", "/").split(",",2);
                    after = "," + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+:(.*)")){
                    ids = priors[1].replace("/ ", "/").split(":",2);
                    after = ":" + ids[1];
                    id = ids[0];
                }
                else{
                    return passage;
                }
                ids = ids[0].replace("/ ", "/").split("/");
                uri +="pd/"+ids[1].replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω]", "").replace(" ", "")+"/"+ids[0].replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω]", "");
                passage1 = createHTMLpassage(priors[0]) + " <a href=\\\"" + uri +"\\\">"+ "Π.δ." + id + "</a> " + createHTMLpassage(after);
                //System.out.println("TARGET: "+passage1);
                //System.out.println("TARGET: "+uri);
            }
            else if(passage.contains("Π.Δ. ")){
                String[] priors = passage.split("Π\\.Δ\\. ");
                String[] ids;
                if(priors[1].matches("[0-9]+\\/[0-9]+ (.*)")){
                    ids = priors[1].replace("/ ", "/").split(" ",2);
                    after = ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+\\((.*)")){
                    ids = priors[1].replace("/ ", "/").split("\\(",2);
                    after = "(" + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+«(.*)")){
                    ids = priors[1].replace("/ ", "/").split("«",2);
                    after = "«" + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+,(.*)")){
                    ids = priors[1].replace("/ ", "/").split(",",2);
                    after = "," + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+:(.*)")){
                    ids = priors[1].replace("/ ", "/").split(":",2);
                    after = ":" + ids[1];
                    id = ids[0];
                }
                else{
                    return passage;
                }
                ids = ids[0].replace("/ ", "/").split("/");
                uri +="pd/"+ids[1].replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω]", "")+"/"+ids[0].replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω]", "");
               //System.out.println("TARGET: "+uri);
                passage1 = createHTMLpassage(priors[0]) + " <a href=\\\"" + uri +"\\\">"+ "Π.Δ. " + id + "</a> " + createHTMLpassage(after);
                //System.out.println("TARGET: "+passage1);
            }
            else if(passage.contains("Π.Δ/τος ")){
                String[] priors = passage.split("Π\\.Δ\\/τος ");
                String[] ids;
                if(priors[1].matches("[0-9]+\\/[0-9]+ (.*)")){
                    ids = priors[1].replace("/ ", "/").split(" ",2);
                    after = ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+\\((.*)")){
                    ids = priors[1].replace("/ ", "/").split("\\(",2);
                    after = "(" + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+«(.*)")){
                    ids = priors[1].replace("/ ", "/").split("«",2);
                    after = "«" + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+,(.*)")){
                    ids = priors[1].replace("/ ", "/").split(",",2);
                    after = "," + ids[1];
                    id = ids[0];
                }
                else if(priors[1].matches("[0-9]+\\/[0-9]+:(.*)")){
                    ids = priors[1].replace("/ ", "/").split(":",2);
                    after = ":" + ids[1];
                    id = ids[0];
                }
                else{
                    return passage;
                }
                ids = ids[0].replace("/ ", "/").split("/");
                uri +="pd/"+ids[1].replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω]", "")+"/"+ids[0].replaceAll("[^0-9a-zΑ-Ζα-ωΑ-Ω]", "");
               //System.out.println("TARGET: "+uri);
                passage1 = createHTMLpassage(priors[0]) + " <a href=\\\"" + uri +"\\\">"+ "Π.Δ/τος " + id + "</a> " + createHTMLpassage(after);
                //System.out.println("TARGET: "+passage1);
            }
            else{
                passage1= passage;
            }
        }
        catch (IndexOutOfBoundsException e){
            passage1= passage;
        }

        
        return passage1;
    }
        
    void parseCases(ArrayList<String> cases, int type) throws IOException{
    
      int count = 0;
      int mod_flag = 0;
      String case1;
      for(int k=0; k<cases.size(); k++){
          int flag = 0;
              if(cases.get(count).charAt(1)==')'){
              }
              else{
                  if(count==0&&k==count&&!(cases.get(count).charAt(1)==')')){
                      count--;
                  }
                  else{
                    flag = 1;
                  }
              }
              case1 = cases.get(k).replace("−\n", "").replace("−", "-").replaceAll("\\p{C}", "").trim();
              case1= case1.replace("\n", " ").replace("−", "-").replaceAll("\\p{C}", "").trim();
              String mod_case = case1;
              if(case1.endsWith("$")){
                  case1 = case1.replace("$", "");
                  mod_flag=1;
              }
              else if(case1.endsWith("$.")){
                  mod_flag = 1;
                  case1 = case1.replace("$.", "");
              }
              else if(case1.endsWith("$ ")){
                  mod_flag = 1;
                  case1 = case1.replace("$ ", "");
              }
              else if(case1.contains("$")){
                  mod_flag = 1;
                  case1 = case1.replace("$", "");
              }
              String HTMLcase =  tagLegislation(case1);
              String HTMLcase2 = this.tagEntities(HTMLcase);
              if((k==0)&&case1.charAt(1)==')'){
                writer2.println("<"+baseURI+"/case/"+(k+1)+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Case>.");
                writer2.println("<"+baseURI+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/case/"+(k+1)+">.");
                writer2.println("<"+baseURI+"/case/"+(k+1)+"> <http://legislation.di.uoa.gr/ontology/text> \""+case1.substring(3, case1.length()).replace("\n", " ").replace("−", "-").replaceAll("\\p{C}", "").replace("$", "")+"\"@el.");
                if(!HTMLcase2.equals(case1)){
                  //System.out.println("HTML:"+HTMLcase);
                  writer2.println("<"+baseURI+"/case/"+(k+1)+"> <http://legislation.di.uoa.gr/ontology/html> \""+HTMLcase2.replace("$", "")+"\".");
                }
              }
              else{
                if(count==-1){
                    writer2.println("<"+baseURI+"/passage/1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Passage>.");
                    writer2.println("<"+baseURI+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/passage/1>.");
                    writer2.println("<"+baseURI+"/passage/1> <http://legislation.di.uoa.gr/ontology/text> \""+case1.replace("\n", " ").replace("−", "-").replaceAll("\\p{C}", "").replace("$", "")+"\"@el.");
                    if(!HTMLcase2.equals(case1)){
                      //System.out.println("HTML:"+HTMLcase);
                      writer2.println("<"+baseURI+"/passage/1> <http://legislation.di.uoa.gr/ontology/html> \""+HTMLcase2.replace("$", "")+"\".");
                    }
                }
                else{
                    writer2.println("<"+baseURI+"/case/"+(k+1)+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Case>.");
                    writer2.println("<"+baseURI+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/case/"+(k+1)+">.");
                    writer2.println("<"+baseURI+"/case/"+(k+1)+"> <http://legislation.di.uoa.gr/ontology/text> \""+case1.replace("\n", " ").replace("−", "-").replaceAll("\\p{C}", "").replace("$", "")+"\"@el.");
                    if(!HTMLcase2.equals(case1)){
                      //System.out.println("HTML:"+HTMLcase);
                      writer2.println("<"+baseURI+"/case/"+(k+1)+"> <http://legislation.di.uoa.gr/ontology/html> \""+HTMLcase2.replace("$", "")+"\".");
                    }
                }
              }

              if(mod_flag==1&&type==0){//((case1.contains("καταργείται:")||case1.contains("ως εξής:")||case1.contains("νέο εδάφιο:")||case1.contains("τα εξής εδάφια:")||case1.contains("τα ακόλουθα εδάφια:")||(case1.contains("εξής:")&&!case1.endsWith("».\n"))||(case1.contains("ακολούθως:")&&!case1.contains("ως ακολούθως: α"))||(case1.endsWith(": ")))&&!case1.contains("έχει ως εξής:")||case1.contains("ακόλουθο εδάφιο:")){
                  mod_flag=0;
                  if(!mods.isEmpty()&&(count!=-1)){
                    String mod = mods.get(0);
                    mods.remove(0);
                    //case1 = case1.replace("  ", " ");
                    String uri = findModificationTarget(case1);
                    String uri2 = findModificationPatient(case1,uri);
                    createModificationInfo(case1,uri,uri2,"case/"+(k+1));
                    if(mod.contains("Άρθρο")||mod.contains("Άρθρο")){
                        baseURI+= "/case/"+(k+1)+"/modification/"+mod_count;
                        parseParagraphs(mod.replace("$", ""),2);
                        baseURI = baseURI.split("/case")[0];
                    }
                    else if(mod.split(" ")[0].matches("[0-9]+\\.(.*)")){
                       String num = mod.split("\\.",2)[0].replace(" ", "");
                       mod = mod.replaceFirst("[0-9]+\\.", "").replace("$", "");
                       writer2.println("<"+baseURI+"/case/"+(k+1)+"/modification/"+mod_count+"/paragraph/"+num+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Paragraph>.");
                       writer2.println("<"+baseURI+"/case/"+(k+1)+"/modification/"+mod_count+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/case/"+(k+1)+"/modification/"+"/paragraph/"+num+">.");
                       ArrayList<String> mod_passages = this.splitPassages(mod);
                       for(int w=0; w< mod_passages.size(); w++){
                            writer2.println("<"+baseURI+"/passage/1/modification/"+mod_count+"/paragraph/"+num+"/passage/"+(w+1)+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Passage>.");
                            writer2.println("<"+baseURI+"/passage/1/modification/"+mod_count+"/paragraph/"+num+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/passage/1/modification/"+mod_count+"/paragraph/"+num+"/passage/"+(w+1)+">.");
                            writer2.println("<"+baseURI+"/passage/1/modification/"+mod_count+"/paragraph/"+num+"/passage/"+(w+1)+"> <http://legislation.di.uoa.gr/ontology/text> \""+mod_passages.get(k).replace("−\n", "").replace("−", "-").replace("\n", " ").replaceAll("\\p{C}", " ").replace("$", "")+"\"@el.");

                       }

                    }
                    else {
                       writer2.println("<"+baseURI+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/case/"+(k+1)+"/modification/"+mod_count+">.");
                       writer2.println("<"+baseURI+"/case/"+(k+1)+"/modification/"+mod_count+"/passage/1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Passage>.");
                       writer2.println("<"+baseURI+"/case/"+(k+1)+"/modification/"+mod_count+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/modification/"+mod_count+"/passage/1>.");
                       writer2.println("<"+baseURI+"/case/"+(k+1)+"/modification/"+mod_count+"/passage/1> <http://legislation.di.uoa.gr/ontology/text> \""+mod.replace("$", "").replace("−\n", "").replace("−", "-").replace("\n", " ").replaceAll("\\p{C}", "").replace("$", "")+"\"@el.");
                    }
                    mod_count++;
                  }
                  else if(mods.isEmpty()&&(count!=-1)){
                       missing_mods++;
                  }
              }
              count++;
              if(flag==1){
                flag=0;
              }
      }
    }
    

    public static void changeNames(File[] files){
            for(int i=0;i<files.length;i++){
                String name = files[i].getName().replace("Ξ‘", "").replace("R", "R.pdf");
                File f= new File(name);
                files[i].renameTo(f);
            }
    }
    
    private void addDocPassage(IndexWriter w, String fieldtitle, String fieldtype, String text, String fielddate, String fielduri) throws IOException {
      Document doc = new Document();
      doc.add(new TextField("title", fieldtitle, Store.YES));
      doc.add(new TextField("uri", fielduri, Store.YES));
      doc.add(new TextField("type", fieldtype, Store.YES));
      doc.add(new TextField("text", text, Store.YES));
      doc.add(new TextField("date", fielddate, Store.YES));
      
      w.addDocument(doc);
    }
    
    /**
     * @param args the command line arguments
     * @throws java.lang.Exception
     */
    public static void main(String[] args) throws Exception {
        
         Path path = Paths.get("C:/Users/Ilias/Desktop/lucene/indexes/fek");
         Directory directory = FSDirectory.open(path);
         IndexWriterConfig config = new IndexWriterConfig(new SimpleAnalyzer());        
         IndexWriter indexWriter = new IndexWriter(directory, config);
         indexWriter.deleteAll();
         
         
//        
//        EntityIndex  ei= new EntityIndex();
//        ei.findOrganizations();
//        ei.findPlaces();
//        FEKParser fp = new FEKParser("2014","FEK_Α127_2014-06-03_2014-06-04R.pdf.txt",ei,indexWriter);
//        fp.readFile("2014","FEK_Α127_2014-06-03_2014-06-04R.pdf.txt");
//        fp.parseLegalDocument();
//        
//        
        int size = 0;
        int ok = 0;
        File[] files;
        System.out.println("NOMOTHESIA G3 PARSER");
        System.out.println("====================");
        System.out.println("");
        System.out.println("Do you want to transform txt files from specific folder? (Y/N)");
        Scanner in = new Scanner(System.in);
        String transform = in.nextLine();
        if(transform.equals("Y")||transform.equals("y")){
            System.out.println("Give the name of the appropriate folder");
            System.out.print("Folder name: ");
            in = new Scanner(System.in);
            String folder_name = in.nextLine();
            int count = 0;
            File folder = new File("C://Users/Ilias/Desktop/FEK/"+folder_name+"/txt/");
            files = folder.listFiles();
            int flag =0;
            String[] data = {""};
            File n3_folder = new File("n3/"+folder_name+"/");
            // DELETE OLD N3 FILES
            File[] n3_files = n3_folder.listFiles();
            for(int z=0; z<n3_files.length; z++){
                n3_files[z].delete();
            }
            System.out.println("All n3 files in folder n3/"+folder_name+" deleted");
            // DELETE OLD PDF FILES
            File pdf_folder = new File("pdf/"+folder_name+"/");
            File[] pdf_files = pdf_folder.listFiles();
            for(int z=0; z<pdf_files.length; z++){
                pdf_files[z].delete();
            }
            System.out.println("All pdf files in folder pdf/"+folder_name+" deleted");
            // DELETE OLD IMAGE FILES
            File image_folder = new File("images/"+folder_name+"/");
            File[] image_files = image_folder.listFiles();
            for(int z=0; z<image_files.length; z++){
                image_files[z].delete();
            }
            System.out.println("All image files in folder images/"+folder_name+" deleted");
            File file3 = new File("n3/"+folder_name+"/missing.n3");
            PrintWriter writer3 = new PrintWriter(file3);
            FEKParser fp = null;
            EntityIndex  ei= new EntityIndex();
//            ei.findPlaces();
//            ei.findOrganizations();
//            ei.findPeople();
//            ei.closeIndex();
            for(int z=0; z<files.length; z++){

                if(files[z].getName().contains(".txt")){
                    try{
                        data = files[z].getName().replace("FEK_Α","").split("_");
                        data[1] = data[1].split("-")[0];
                        fp = new FEKParser(folder_name,files[z].getName(),ei,indexWriter);
                        fp.readFile(folder_name,files[z].getName());
                        fp.parseLegalDocument();
                    }
                    catch(Exception ex){
                        flag = 1;
                        //ex.printStackTrace();
                        if(fp!=null){
                            fp.writer2.close();
                        }
                        if(fp.legaldoc.getYear()==null||fp.legaldoc.getYear().isEmpty()){
                            flag = 2;
                        }
                    }
                    finally{
                        
                        if(flag==0){
                          size += fp.legal_sum;
                          ok += fp.legal_ok;
                          //System.out.print("\n");
                          System.out.println(files[z].getName()+" PARSED SUCCESSFULY INTO "+files[z].getName().replace(".pdf.txt", "")+".n3");
                          count ++;
                        }
                        else{
                          if(flag==2){
                            //writer3.println("<http://legislation.di.uoa.gr/gazette/a/"+data[1]+"/"+data[0]+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/GovernmentGazette>.");
                            //writer3.println("<http://legislation.di.uoa.gr/gazette/a/"+data[1]+"/"+data[0]+"> <http://purl.org/dc/terms/title> \"Α/"+data[1]+"/"+data[0]+"\".");
                            //linkFEK2(folder_name,data[1],data[0], files[z].getName().replace(".txt", ""),writer3);
                          }
                          System.out.println(files[z].getName()+" NOT PARSED SUCCESSFULY INTO "+files[z].getName().replace(".pdf.txt", "")+".n3");
                          File file = new File("n3/"+files[z].getName().replace(".pdf.txt", "")+".n3");
                          if(file.delete()){
                                  //System.out.println(file.getName().replace(".pdf.txt", "")+".n3" + " DELETED!");
                          }
                          else{
                                  //System.out.println("DELETE "+files[z].getName().replace(".pdf.txt","")+ ".n3 FAILED!");
                          }
                        }
                        flag=0;
                    }
                }

            }
            writer3.close();
            indexWriter.close();
            directory.close();
            System.out.println("====================================================");
            System.out.println("PARSED SUCCESSFULLY "+count+"/"+files.length+" FILES");
            //System.out.println("PARSED SUCCESSFULLY "+ok+"/"+size+" LEGAL DOCS");
            System.out.println("====================================================");
        
        }
        
        System.out.println("Do you want to upload the .n3 files to Sesame RDF Store? (Y/N)");
        in = new Scanner(System.in);
        transform = in.nextLine();
        if(transform.equals("Y")||transform.equals("y")){
            System.out.println("Give the name of the appropriate folder");
            System.out.print("Folder name: ");
            in = new Scanner(System.in);
            String folder_name = in.nextLine();
            File folder_n3 = new File("n3/"+folder_name);
            files = folder_n3.listFiles();
    //        File merge_file = new File("n3/merged.n3");
            uploadFiles(files);
        }
       
    }
    
    public static void uploadFiles(File[] files) throws RepositoryException {

        String sesameServer = "http://localhost:8080/openrdf-sesame";
        String repositoryID = "legislation";

        Repository repo = new HTTPRepository(sesameServer, repositoryID);
        repo.initialize();
            for (File f : files) {
                if(f.getName().endsWith(".n3")&&(f.length()!=0)){
                    try {
                        
                       RepositoryConnection con = repo.getConnection();
                       try {
                          con.add(f, "", RDFFormat.N3);
                       }
                       finally {
                          con.close();
                          System.out.println("uploading: " + f.getName() + "with size "+f.length());
                       }
                    }
                    catch (OpenRDFException e) {
                       // handle exception
                    }
                    catch (java.io.IOException e) {
                       // handle io exception
                    }
                }
            }
            System.out.println("uploading: COMPLETED");

    }
    
    
    static void linkFEK2(String folderName,String Year,String ID, String fileName,PrintWriter writer) throws IOException{
        File fek= new File("pdf/"+folderName+"/GG"+Year+"_"+ID+".pdf");
        //System.out.println("PLACE PDF: "+ fileName);
        File pdf= new File("C://Users/Ilias/Desktop/FEK/"+folderName+"/pdf/"+fileName);
        Files.copy(pdf.toPath(), fek.toPath());
        writer.println("<http://legislation.di.uoa.gr/gazette/a/"+Year+"/"+ID+"> <http://legislation.di.uoa.gr/pdfFile> \"GG"+Year+"_"+ID+".pdf\".");

    }
    
    
    public class FileComparator implements Comparator<File> {
    @Override
    public int compare(File f1, File f2) {
        
        int id1= Integer.parseInt(f1.getName().replaceFirst("(.*)\\.pdf-", "").replaceAll("_1\\.(png|jpg)", ""));
        int id2= Integer.parseInt(f2.getName().replaceFirst("(.*)\\.pdf-", "").replaceAll("_1\\.(png|jpg)", ""));

        return id1 - id2;
    }
    
    
}
    
}
