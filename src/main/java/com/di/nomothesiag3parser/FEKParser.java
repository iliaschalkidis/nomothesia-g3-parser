/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.di.nomothesiag3parser;

import com.di.nomothesia.model.GovernmentGazette;
import com.di.nomothesia.model.LegalDocumentPro;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openrdf.OpenRDFException;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.rio.RDFFormat;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.http.HTTPRepository;
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
    public FEKParser(String folder_name,String name){
        legaldocs = new ArrayList<>();
        legaldoc = new LegalDocumentPro();
        mods = new ArrayList<>();
        mod_count =1;
        legal_sum = 0;
        legal_ok = 0;
        baseURI = "http://legislation.di.uoa.gr/";
        try {
            File file2 = new File("n3/"+folder_name+"/"+name.replace(".pdf.txt","")+".n3");
            file2.getParentFile().mkdirs();
            writer2 = new PrintWriter(file2);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(FEKParser.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    void readFile(String folder,String fileName) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader( new FileInputStream("src/main/java/com/di/nomothesiag3parser/"+folder+"/"+fileName), "UTF-8"));
        try {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();
            String pre_line = "";
            String mod = "";
            int mod_line = 0;
            while (line != null) {
                
                if(line.matches("[0-9]+")|line.equals("ΕΦΗΜΕΡΙΣ ΤΗΣ ΚΥΒΕΡΝΗΣΕΩΣ (ΤΕΥΧΟΣ ΠΡΩΤΟ)")|line.equals("")){
                }
                else if(line.startsWith("«")){
                    //writer.println(line);
                    if(pre_line.endsWith(":")||pre_line.equals("")){
                        mod += line +"\n";
                        mod_line = 1;
                    }
                    else{
                        sb.append(line);
                        sb.append("\n");
                    }
                }
                else if(line.contains(".»")||line.contains("».")){
                    //writer.println(line);
                    String[] lines = line.split("»");
                    mod += lines[0] +"\n";
                    if(mod.startsWith("«")){
                        mods.add(mod);
                        if(lines.length==2){
                            sb.append(lines[1]);
                        }
                        sb.append("\n");
                    }
                    else{
                        sb.append(mod);
                        if(line.contains("».")){
                            sb.append("».\n");
                        }
                        else if(line.contains(".»")){
                            sb.append(".»\n");
                        }
                    }
                    mod_line = 0;
                    mod="";
                    
                }
                else if(mod_line == 1){
                    //writer.println(line);
                    mod +=line +"\n";
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
    
    void parseLegalDocument(){

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
          FEK = FEK.replace("ΤΕΥΧΟΣ ΠΡΩΤΟ\n", "");
      }
      buffers = FEK.split("\n",2);
      FEK = buffers[1];
      
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
      
      // Iterate over legal documents to produce the appropriate RDF triples
      for(int i =start; i< limit; i++){
          if(legaldocuments.length>2||start==0){
            FEK = legaldocuments[i];
          }
          baseURI = "http://legislation.di.uoa.gr/";
          buffers = FEK.split("\n",2);
          FEK = buffers[1];
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
          else{
              finish = 1;
              Type = buffers[0].split(" ΥΠ’ ΑΡΙΘΜ. ");
          }
          
          // Continue if the legal document meets the very basic standards
          if(finish==0){
              // Produce the Type information
              switch (Type[0]) {
                    case "NOMOΣ":  
                            System.out.println("ΝΟΜΟΣ "+gg.getYear()+"/"+Type[1]);
                            legaldoc.setId("http://legislation.di.uoa.gr/law/"+gg.getYear()+"/"+Type[1]);
                            baseURI += "law/"+gg.getYear()+"/"+Type[1];
                            writer2.println("<"+legaldoc.getId()+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Law>.");
                            break;
                    case "ΠΡΟΕΔΡΙΚΟ ΔΙΑΤΑΓΜΑ":
                            System.out.println("ΠΔ "+gg.getYear()+"/"+Type[1]);
                            legaldoc.setId("http://legislation.di.uoa.gr/pd/"+gg.getYear()+"/"+Type[1]);
                            baseURI += "pd/"+gg.getYear()+"/"+Type[1];
                            writer2.println("<"+legaldoc.getId()+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/PresidentialDecree>.");
                            break;
                    case "ΠΡΑΞΗ ΥΠΟΥΡΓΙΚΟΥ ΣΥΜΒΟΥΛΙΟΥ":
                            System.out.println("ΠΥΣ "+gg.getYear()+"/"+ buffers[0]);
                            legaldoc.setId("http://legislation.di.uoa.gr/amc/"+gg.getYear()+"/"+buffers[0].split(" της ")[0]);
                            baseURI += "amc/"+gg.getYear()+"/"+ buffers[0].split(" της ")[0];
                            writer2.println("<"+legaldoc.getId()+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/ActOfMinisterialCabinet>.");
                            Type[1]= buffers[0].split(" της ")[0];
                            break;
                    default: 
                            System.out.println(Type[0] +" "+gg.getYear()+"/"+Type[1]);
                            legaldoc.setId("http://legislation.di.uoa.gr/law/"+gg.getYear()+"/"+Type[1]);
                            baseURI += "law/"+gg.getYear()+"/"+Type[1];
                            writer2.println("<"+legaldoc.getId()+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Law>.");
                            break;
              }    
              
              // Produce the basic metadata information of the legal document
              writer2.println("<"+legaldoc.getId()+"> <http://legislation.di.uoa.gr/ontology/gazette> <http://legislation.di.uoa.gr/gazette/a/"+legaldoc.getYear()+"/"+legaldoc.getFEK()+">.");
              writer2.println("<http://legislation.di.uoa.gr/gazette/a/"+legaldoc.getYear()+"/"+legaldoc.getFEK()+"> <http://purl.org/dc/terms/title> \"Α/"+legaldoc.getYear()+"/"+legaldoc.getFEK()+"\".");
              writer2.println("<"+baseURI+"> <http://legislation.di.uoa.gr/ontology/views> \"0\"^^<http://www.w3.org/2001/XMLSchema#integer>.");
              writer2.println("<"+baseURI+"> <http://purl.org/dc/terms/created> \""+date+"\"^^<http://www.w3.org/2001/XMLSchema#date>.");
              writer2.println("<"+baseURI+"> <http://legislation.di.uoa.gr/ontology/legislationID> \""+Type[1]+"\"^^<http://www.w3.org/2001/XMLSchema#integer>.");
              writer2.println("<"+baseURI+"> <http://legislation.di.uoa.gr/ontology/tag> \"Ελλάδα\"@el.\n");
              
              // Produce a new version if there are any modifications
              if(!mods.isEmpty()){
                 writer2.println("<"+baseURI+"> <http://www.metalex.eu/metalex/2008-05-02#realizedBy> <"+baseURI+"/"+date+">.");
              }
              baseURI2 = baseURI;
              FEK = FEK.replaceAll("ΕΦΗΜΕΡΙΣ ΤΗΣ ΚΥΒΕΡΝΗΣΕΩΣ \\(ΤΕΥΧΟΣ ΠΡΩΤΟ\\)", " ").replaceAll("\n[0-9]+\n"," ");
             
             // Check if there are citations and produce the appropriate RDF triples
             int cit = 0;
             if(FEK.contains("χοντας υπόψη:\n")){
                 if(FEK.contains("αποφασίζουμε:"))
                    buffers = FEK.split("αποφασίζουμε:");
                 else if(FEK.contains("αποφασίζει:")){
                     buffers =FEK.split(", αποφασίζει:");
                 }
                 else{
                    buffers = FEK.split("ζουμε:");
                 }
                 parseCitations(buffers[0]);
                 FEK = buffers[1];
                 cit = 1;
             }
             else{
                buffers = FEK.split("Εκδίδομε τον ακόλουθο νόμο που ψήφισε η Βουλή:\n");
                FEK = buffers[1];
                buffers[0] = buffers[0].replaceAll("\n", " ");
                legaldoc.setTitle(buffers[0].replace("Ο ΠΡΟΕΔΡΟΣ\nΤΗΣ ΕΛΛΗΝΙΚΗΣ ΔΗΜΟΚΡΑΤΙΑΣ", "").replace("ΤΟ ΥΠΟΥΡΓΙΚΟ ΣΥΜΒΟΥΛΙΟ",""));
             }
             
             // Split to crop legal document's text body from footer
             if(baseURI.contains("/amc/")){
                 buffers = FEK.split("Ο ΠΡΩΘΥΠΟΥΡΓΟΣ");
             }
             else{
                buffers = FEK.split("\nΑθήνα, ");
             }
             
             // Delete some well known annoying printing trash
             FEK = buffers[0].replaceAll("\n[0-9]+\n", " ");
             FEK = FEK.replaceAll("ΕΦΗΜΕΡΙΣ ΤΗΣ ΚΥΒΕΡΝΗΣΕΩΣ (ΤΕΥΧΟΣ ΠΡΩΤΟ)", "");
             
             // Parse the legal document's text body according to its structure
             // There are chapters
             if(FEK.startsWith("ΚΕΦΑΛΑΙΟ")){
                 if(cit==0){
                     writer2.println("<"+baseURI+"> <http://purl.org/dc/terms/title> \""+legaldoc.getTitle().replace("Ο ΠΡΟΕΔΡΟΣ\nΤΗΣ ΕΛΛΗΝΙΚΗΣ ΔΗΜΟΚΡΑΤΙΑΣ", "").replace("−\n", "").replace("\n", " ").replace("−", "-").replaceAll("\\p{C}", " ")+"\"@el.");
                 }
                parseChapters();
             }
             // There are only article
             else if(FEK.startsWith("Άρθρο")||(FEK.contains("Άρθρο"))){
                int count = -1; 
                parseArticles(FEK,count); 
             }
             // There is no structure at all
             else{
                 writer2.println("<"+baseURI+"/article/1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Article>.");
                 writer2.println("<"+baseURI+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/article/1>.");
                 writer2.println("<"+baseURI+"/article/1/paragraph/1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Paragraph>.");
                 writer2.println("<"+baseURI+"/article/1> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/article/1/paragraph/1>.");
                 writer2.println("<"+baseURI+"/article/1/paragraph/1/passage/1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Passage>.");
                 writer2.println("<"+baseURI+"/article/1/paragraph/1> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/article/1/paragraph/1/passage/1>.");
                 writer2.println("<"+baseURI+"article/1/paragraph/1/passage/1> <http://legislation.di.uoa.gr/ontology/text> \""+FEK.replace("−\n", "").replace("−", "-").replace("\n", " ").replaceAll("\\p{C}", " ")+"\"@el.");
             }
             writer2.println("<"+baseURI+"> <http://legislation.di.uoa.gr/ontology/signer>  <http://legislation.di.uoa.gr/signer/1>.\n" +
                "<http://legislation.di.uoa.gr/signer/1> <http://xmlns.com/foaf/0.1/name> \"ΚΑΡΟΛΟΣ ΠΑΠΟΥΛΙΑΣ\"@el.\n" +
                "<http://legislation.di.uoa.gr/signer/1> <http://xmlns.com/foaf/0.1/title> \"ΠΡΟΕΔΡΟΣ ΤΗΣ ΔΗΜΟΚΡΑΤΙΑΣ\"@el.\n");
             legal_ok ++;
          }
        }
        writer2.close();
    }
    
    void parseCitations(String citation_list){
        // Split between citations
        String[] citations = citation_list.split("[0-9]+\\. ");
        
        // Crop legal document's title
        String title = citations[0].split("Έχοντας υπόψη:")[0];
        title = title.replace("Ο ΠΡΟΕΔΡΟΣ\nΤΗΣ ΕΛΛΗΝΙΚΗΣ ΔΗΜΟΚΡΑΤΙΑΣ", "").replace("ΤΟ ΥΠΟΥΡΓΙΚΟ ΣΥΜΒΟΥΛΙΟ","").replace("−\n", "").replaceAll("\n", " ").replace("−", "-");
        legaldoc.setTitle(title.replace("Ο ΠΡΟΕΔΡΟΣ ΤΗΣ ΕΛΛΗΝΙΚΗΣ ΔΗΜΟΚΡΑΤΙΑΣ", "").replace("ΤΟ ΥΠΟΥΡΓΙΚΟ ΣΥΜΒΟΥΛΙΟ","").replace("−\n", "").replaceAll("\n", " ").replace("−", "-"));
        writer2.println("<"+baseURI+"> <http://purl.org/dc/terms/title> \""+title+"\"@el.");
        
        // Produce RDF triples for citations
        for(int i=1; i<citations.length; i++){
            writer2.println("<"+baseURI+"/citation/"+i+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.metalex.eu/metalex/2008-05-02#BibliographicCitation>.");
            writer2.println("<"+baseURI+"/citation/"+i+"> <http://legislation.di.uoa.gr/ontology/context> \""+citations[i].replace("−\n", "").replace("−", "-").replace("\n", " ").replaceAll("\\p{C}", " ")+"\"@el.");
            writer2.println("<"+baseURI+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/citation/"+i+">.");
        }
    }
    
    void parseChapters(){
        // Split between chapters
        String[] buffers = FEK.split("\\bΚΕΦΑΛΑΙΟ\\b ([Α-Ω]|[0-9]|΄)+\n");
         int count = 0;
         // Iterate over chapters
          for(int i=1; i<buffers.length; i++){
              writer2.println("<"+baseURI+"/chapter/"+i+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Chapter>.");
              writer2.println("<"+baseURI+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/chapter/"+i+">.");
              baseURI += "/chapter/"+i;
              // Parse articles
              count = parseArticles(buffers[i],count);
              baseURI = baseURI.split("/chapter")[0];
         }
    
    }
    
    int parseArticles(String chapter, int art_count){
        // Split between articles
        String[] Articles = chapter.split("\n\\bΆρθρο\\b ([0-9]+|πρώτο|δεύτερο)\n");
        int begin = 1;
        int miss = 0;
        // There are chapters
        if(art_count != -1){
            // Title has been already been parsed
            if(Articles[0].startsWith("Άρθρο")||(Articles[0].startsWith("\nΆρθρο"))){
                Articles[0] = Articles[0].replaceFirst("\\bΆρθρο\\b ([0-9]+|πρώτο|δεύτερο|μονό|μόνο)\n", "");
                begin = 0;
                art_count = 0;
            }
            // Title has not been already been parsed
            else{
                String title = Articles[0].replace("Ο ΠΡΟΕΔΡΟΣ\nΤΗΣ ΕΛΛΗΝΙΚΗΣ ΔΗΜΟΚΡΑΤΙΑΣ", "").replace("ΤΟ ΥΠΟΥΡΓΙΚΟ ΣΥΜΒΟΥΛΙΟ","").replace("−\n", "").replace("\n", " ").replace("−", "-").replaceAll("\\p{C}", " ");
                writer2.println("<"+baseURI+"> <http://purl.org/dc/terms/title> \""+title+"\"@el.");
            }
        
        }
        // There are not chapters
        else{
            begin = 0;
            art_count = 0;
            // Chapter's title has been already been parsed
            if(Articles[0].startsWith("Άρθρο")||(Articles[0].startsWith("\nΆρθρο"))){
                 Articles[0] = Articles[0].replaceFirst("\\bΆρθρο\\b ([0-9]+|πρώτο|δεύτερο|μονό|μόνο)\n", "");
                 writer2.println("<"+baseURI+"> <http://purl.org/dc/terms/title> \""+legaldoc.getTitle().replace("Ο ΠΡΟΕΔΡΟΣ\nΤΗΣ ΕΛΛΗΝΙΚΗΣ ΔΗΜΟΚΡΑΤΙΑΣ", "").replace("−\n", "").replace("\n", " ").replace("−", "-").replaceAll("\\p{C}", "")+"\"@el.");
            }
            // Chapter's title has not been already been parsed
            else{
                begin = 1;
                if(legaldoc.getTitle()==null){
                    writer2.println("<"+baseURI+"> <http://purl.org/dc/terms/title> \""+Articles[0].replace("Ο ΠΡΟΕΔΡΟΣ\nΤΗΣ ΕΛΛΗΝΙΚΗΣ ΔΗΜΟΚΡΑΤΙΑΣ", "").replace("−\n", "").replace("\n", " ").replace("−", "-").replaceAll("\\p{C}", "")+"\"@el.");
                }
            }
        }
        if(begin==0){
            miss = 1;
        }
        // Iterate over articles
        for(int j=begin; j<Articles.length; j++){
              int count = j + miss;
              writer2.println("<"+baseURI+"/article/"+(art_count+1)+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Article>.");
              writer2.println("<"+baseURI+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/article/"+(art_count+1)+">.");
              baseURI+="/article/"+(art_count+1);
              // Parse paragraphs
              parseParagraphs(Articles[j],0);
              art_count++;
              baseURI = baseURI.split("/article")[0];
          }
          return art_count;
    }
    
        void parseParagraphs(String article,int type){
            int top = 1;
            String[] Paragraphs = article.split("\n[0-9]+\\. ");
              if(Paragraphs.length>2){
                  if(type==0){
                      if(!Paragraphs[0].endsWith(".")&&!Paragraphs[0].endsWith(":\n")&&!Paragraphs[0].endsWith(":")&& !Paragraphs[0].contains("εξής:")){
                        writer2.println("<"+baseURI+"> <http://purl.org/dc/terms/title> \""+Paragraphs[0].replace("−\n", "").replace("−", "-").replace("\n", " ").replaceAll("\\p{C}", "")+"\".");
                      }
                  }
                  else{
                    writer2.println("<"+baseURI+"/article/1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Article>.");
                    writer2.println("<"+baseURI+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/article/1>.");
                      if(!Paragraphs[0].endsWith(".")&&!Paragraphs[0].endsWith(":\n")&&!Paragraphs[0].endsWith(":")&& !Paragraphs[0].contains("εξής:")){
                          String[] title = Paragraphs[0].split("\n");
                          if(title.length >1){
                              writer2.println("<"+baseURI+"/article/1> <http://purl.org/dc/terms/title> \""+title[1].replace("−\n", "").replace("−", "-").replace("\n", " ").replaceAll("\\p{C}", "")+"\"@el.");
                          }
                      }
                      baseURI+= "/article/1";
                  }
                  if(Paragraphs[0].endsWith(".")||Paragraphs[0].endsWith(":\n")&&!Paragraphs[0].endsWith(":")&& !Paragraphs[0].contains("εξής:")){
                      top = 0;
                      if(Paragraphs[0].matches("[0-9]+\\.(.*[\n]*)*")){
                          Paragraphs[0] = Paragraphs[0].replaceFirst("[0-9]+\\.","");
                      }
                  }
                  int k;
                  for(int z=top; z<Paragraphs.length; z++){
                      if(top==0){
                          k = z+1;
                      }
                      else{
                         k =z;
                      }
                      writer2.println("<"+baseURI+"/paragraph/"+k+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Paragraph>.");
                      writer2.println("<"+baseURI+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/paragraph/"+k+">.");
                      baseURI+="/paragraph/"+k;
                      String[] cases = (Paragraphs[z].split("\n[α-μ]+\\)"));
                      if(cases.length>1){
                          parseCases(cases);
                      }
                      else{
                          cases = (Paragraphs[z].split("\n[α-μ]+\\."));
                          if(cases.length>1){
                              parseCases(cases);
                          }
                          else{
                              String paragraph = Paragraphs[z].replace("−\n", "").replaceAll("\\p{C}", " ");
                              paragraph = paragraph.replace("\n", " ").replaceAll("\\p{C}", " ") + "\n";
                              parsePassages(paragraph);
                              if((paragraph.contains("καταργείται:")||paragraph.contains("τα ακόλουθα εδάφια:")||paragraph.contains("ακόλουθο εδάφιο:")||paragraph.contains("τα εξής εδάφια:")||(paragraph.contains("εξής:")&&!paragraph.endsWith("».\n"))||(paragraph.contains("ακολούθως:")&&!paragraph.contains("ως ακολούθως: α"))||paragraph.endsWith(": "))&&!paragraph.contains("έχει ως εξής:")){
                                  int replays=1;
                                  String mods_count[] = paragraph.split("(καταργείται:|τα ακόλουθα εδάφια:|ακόλουθο εδάφιο:|εξής:|ακολούθως:)");
                                  if(mods_count.length>2){
                                      replays = mods_count.length-1;
                                  }
                                  for(int r=0; r<replays;r++){
                                      if(!mods.isEmpty()&&(type==0)){
                                        String mod = mods.get(0);
                                        mods.remove(0);
                                        writer2.println("<"+baseURI2+">  <http://www.metalex.eu/metalex/2008-05-02#legislativeCompetenceGroundOf> <"+baseURI+"/modification/"+mod_count+">.");
                                        writer2.println("<"+baseURI2+">  <http://www.metalex.eu/metalex/2008-05-02#matterOf> <"+baseURI+"/modification/"+mod_count+">.");
                                        writer2.println("<"+baseURI+"/modification/"+mod_count+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Addition>.");
                                        writer2.println("<"+baseURI+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/modification/"+mod_count+">.");
                                        if(mod.contains("Άρθρο")||mod.contains("Άρθρο")){
                                            baseURI+= "/modification/"+mod_count;
                                            parseParagraphs(mod.replace("«", "").replace("»", "").replaceAll("\\p{C}", " "),1);
                                        }
                                        else {
                                           writer2.println("<"+baseURI+"/modification/"+mod_count+"/passage/1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Passage>.");
                                           writer2.println("<"+baseURI+"/modification/"+mod_count+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/modification/"+mod_count+"/passage/1>.");
                                           writer2.println("<"+baseURI+"/modification/"+mod_count+"/passage/1> <http://legislation.di.uoa.gr/ontology/text> \""+mod.replace("«", "").replace("»", "").replace("−\n", "").replace("−", "-").replace("\n", " ").replaceAll("\\p{C}", "")+"\"@el.");
                                        }
                                        mod_count++;
                                      }
                                      else{
                                        //writer.println("[MISSING!!!]");
                                      }
                                  }
                              }
                          }
                      }
                      if(type==0){
                        baseURI = baseURI.split("/paragraph")[0];
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
                  if(type==1){
                    writer2.println("<"+baseURI+"/article/1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Article>.");
                    writer2.println("<"+baseURI+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/article/1>.");
                    baseURI+= "/article/1";
                  }
                  String[] titles = article.split("\n");
                  
                  if(titles.length>1){
                      String title = titles[0];
                      if(!title.endsWith(".")&&!title.endsWith(":\n")&&!title.endsWith(":")&& !title.contains("εξής:")&&!(title.endsWith("−"))&& ((titles[1].charAt(0)>='Α')&&(titles[1].charAt(0)<='Ω'))){
                         writer2.println("<"+baseURI+"> <http://purl.org/dc/terms/title> \""+title.replace("−\n", "").replace("−", "-").replace("\n", " ").replaceAll("\\p{C}", "")+"\"."); 
                         article = "";
                         for(int k=1;k<titles.length;k++){
                             article += titles[k];
                         }
                      }
                      if(title.endsWith(".")&&title.split("([Α-Ω]+.)+").length>1){
                        writer2.println("<"+baseURI+"> <http://purl.org/dc/terms/title> \""+title.replace("−\n", "").replace("−", "-").replace("\n", " ").replaceAll("\\p{C}", "")+"\"."); 
                        article = "";
                         for(int k=1;k<titles.length;k++){
                             article += titles[k];
                         }
                      }
                      
                  }
                  String paragraph = article.replace("−\n", "").replace("−", "-").replace("\n", " ").replaceAll("\\p{C}", " ");
                  paragraph = paragraph.replace("−\n", "").replace("−", "-").replace("\n", " ").replaceAll("\\p{C}", " ") + "\n";
                  writer2.println("<"+baseURI+"/paragraph/1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Paragraph>.");
                  writer2.println("<"+baseURI+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/paragraph/1>.");
                  baseURI+="/paragraph/1";
                  parsePassages(paragraph);
                  if(type==0){
                    baseURI = baseURI.split("/paragraph")[0];
                  }
                  else{
                    baseURI = baseURI.split("/paragraph")[0]+ "/paragraph"+baseURI.split("/paragraph")[1];
                  }
                  if((paragraph.contains("καταργείται:")||paragraph.contains("τα ακόλουθα εδάφια:")||paragraph.contains("ακόλουθο εδάφιο:")||paragraph.contains("τα εξής εδάφια:")||(paragraph.contains("εξής:")&&!paragraph.endsWith("».\n"))||(paragraph.contains("ακολούθως:")&&!paragraph.contains("ως ακολούθως: α"))||paragraph.endsWith(": "))&&!paragraph.contains("έχει ως εξής:")){
                      int replays=1;
                      String mods_count[] = paragraph.split("(καταργείται:|τα ακόλουθα εδάφια:|ακόλουθο εδάφιο:|εξής:|ακολούθως:)");
                      if(mods_count.length>2){
                          replays = mods_count.length-1;
                      }
                      for(int r=0; r<replays;r++){
                          if(!mods.isEmpty()&&(type==0)){
                            String mod = mods.get(0);
                            mods.remove(0);
                            writer2.println("<"+baseURI2+">  <http://www.metalex.eu/metalex/2008-05-02#legislativeCompetenceGroundOf> <"+baseURI+"/modification/"+mod_count+">.");
                            writer2.println("<"+baseURI2+">  <http://www.metalex.eu/metalex/2008-05-02#matterOf> <"+baseURI+"/modification/"+mod_count+">.");
                            writer2.println("<"+baseURI+"/paragraph/1/modification/"+mod_count+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Addition>.");
                            writer2.println("<"+baseURI+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/paragraph/1/modification/"+mod_count+">.");
                            if(mod.contains("Άρθρο")||mod.contains("Άρθρο")){
                                baseURI+= "/paragraph/1/modification/"+mod_count;
                                parseParagraphs(mod.replace("«", "").replace("»", "").replaceAll("\\p{C}", " "),1);
                            }
                            else {
                               writer2.println("<"+baseURI+"/paragraph/1/modification/"+mod_count+"/passage/1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Passage>.");
                               writer2.println("<"+baseURI+"/paragraph/1/modification/"+mod_count+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/paragraph/1/modification/"+mod_count+"/passage/1>.");
                               writer2.println("<"+baseURI+"/paragraph/1/modification/"+mod_count+"/passage/1> <http://legislation.di.uoa.gr/ontology/text> \""+mod.replace("«", "").replace("»", "").replace("−\n", "").replace("−", "-").replace("\n", " ").replaceAll("\\p{C}", " ")+"\"@el.");
                            }
                            mod_count++;
                          }
                      }
                  }
                  if(type==1){
                    baseURI = baseURI.split("/modification")[0];
                  }
              }
              
    }
    
    void parsePassages(String paragraph){
       
      String[] passages = paragraph.split("\\. ");
      
      if(passages.length>1){
          for(int i=0; i< passages.length-1; i++){
              writer2.println("<"+baseURI+"/passage/"+(i+1)+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Passage>.");
              writer2.println("<"+baseURI+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/passage/"+(i+1)+">.");
              writer2.println("<"+baseURI+"/passage/"+(i+1)+"> <http://legislation.di.uoa.gr/ontology/text> \""+passages[i].replace("−\n", "").replace("−", "-").replace("\n", " ").replaceAll("\\p{C}", " ")+". \"@el.");
          }
      }
      else{
          writer2.println("<"+baseURI+"/passage/1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Passage>.");
          writer2.println("<"+baseURI+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/passage/1>.");
          writer2.println("<"+baseURI+"/passage/1> <http://legislation.di.uoa.gr/ontology/text> \""+paragraph.replace("−\n", "").replace("−", "-").replace("\n", " ").replaceAll("\\p{C}", " ")+"\"@el.");
      }
    }
        
        
    void parseCases(String[] cases){
    
      int count = 0;
      for(int k=0; k<cases.length; k++){
          int flag = 0;
          if(cases[count].charAt(1)==')'){
          }
          else{
              if(count==0&&k==count&&!(cases[count].charAt(1)==')')){
                  count--;
              }
              else{
                flag = 1;
              }
          }
          String case1 = cases[k].replace("−\n", "").replace("−", "-").replaceAll("\\p{C}", "");
          case1= case1.replace("\n", " ").replace("−", "-").replaceAll("\\p{C}", "") + "\n";
          
          if((k==0)&&case1.charAt(1)==')'){
            writer2.println("<"+baseURI+"/case/"+(k+1)+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Case>.");
            writer2.println("<"+baseURI+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/case/"+(k+1)+">.");
            writer2.println("<"+baseURI+"/case/"+(k+1)+"> <http://legislation.di.uoa.gr/ontology/text> \""+case1.substring(3, case1.length()).replace("\n", " ").replace("−", "-").replaceAll("\\p{C}", "")+"\"@el.");
          }
          else{
            if(count==-1){
                writer2.println("<"+baseURI+"/passage/1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Passage>.");
                writer2.println("<"+baseURI+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/passage/1>.");
                writer2.println("<"+baseURI+"/passage/1> <http://legislation.di.uoa.gr/ontology/text> \""+case1.replace("\n", " ").replace("−", "-").replaceAll("\\p{C}", "")+"\"@el.");
            }
            else{
                writer2.println("<"+baseURI+"/case/"+(k+1)+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Case>.");
                writer2.println("<"+baseURI+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/case/"+(k+1)+">.");
                writer2.println("<"+baseURI+"/case/"+(k+1)+"> <http://legislation.di.uoa.gr/ontology/text> \""+case1.replace("\n", " ").replace("−", "-").replaceAll("\\p{C}", "")+"\"@el.");
            }
          }
          
          if((case1.contains("καταργείται:")||case1.contains("τα εξής εδάφια:")||case1.contains("τα ακόλουθα εδάφια:")||(case1.contains("εξής:")&&!case1.endsWith("».\n"))||(case1.contains("ακολούθως:")&&!case1.contains("ως ακολούθως: α"))||(case1.endsWith(": ")))&&!case1.contains("έχει ως εξής:")||case1.contains("ακόλουθο εδάφιο:")){
              if(!mods.isEmpty()&&(count!=-1)){
                String mod = mods.get(0);
                mods.remove(0);
                mod_count++;
                if(mod.contains("Άρθρο")||mod.contains("Άρθρο")){
                    baseURI+= "/case/"+(k+1);
                    parseParagraphs(mod.replace("«", "").replace("»", "").replaceAll("\\p{C}", " "),1);
                }
                else {
                   writer2.println("<"+baseURI2+"/case/"+(k+1)+">  <http://www.metalex.eu/metalex/2008-05-02#legislativeCompetenceGroundOf> <"+baseURI+"/case/"+(k+1)+"/modification/"+mod_count+">.");
                   writer2.println("<"+baseURI2+">  <http://www.metalex.eu/metalex/2008-05-02#matterOf> <"+baseURI+"/modification/"+mod_count+">.");
                   writer2.println("<"+baseURI+"/case/"+(k+1)+"/modification/"+mod_count+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Addition>.");
                   writer2.println("<"+baseURI+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/case/"+(k+1)+"/modification/"+mod_count+">.");
                   writer2.println("<"+baseURI+"/case/"+(k+1)+"/modification/"+mod_count+"/passage/1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Passage>.");
                   writer2.println("<"+baseURI+"/case/"+(k+1)+"/modification/"+mod_count+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/modification/"+mod_count+"/passage/1>.");
                   writer2.println("<"+baseURI+"/case/"+(k+1)+"/modification/"+mod_count+"/passage/1> <http://legislation.di.uoa.gr/ontology/text> \""+mod.replace("«", "").replace("»", "").replace("−\n", "").replace("−", "-").replace("\n", " ").replaceAll("\\p{C}", "")+"\"@el.");
                }
                
              }
              else{
                //writer.println("[MISSING!!!]");
              }
          }
          count++;
          if(flag==1){
            flag=0;
          }
      }
    }
    

    
    
    /**
     * @param args the command line arguments
     * @throws java.lang.Exception
     */
    public static void main(String[] args) throws Exception {
//        FEKParser fp = new FEKParser("2015","FEK_Α36_2015-03-30_2015-03-30R.pdf.txt");
//        fp.readFile("2015","FEK_Α36_2015-03-30_2015-03-30R.pdf.txt");
//        fp.parseLegalDocument();
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
            File folder = new File("src/main/java/com/di/nomothesiag3parser/"+folder_name+"/");
            files = folder.listFiles();
            int flag =0;
            FEKParser fp = null;
            for(int z=0; z<files.length; z++){

                if(files[z].getName().contains(".txt")){
                    try{
                        fp = new FEKParser(folder_name,files[z].getName());
                        fp.readFile(folder_name,files[z].getName());
                        fp.parseLegalDocument();
                    }
                    catch(Exception ex){
                        flag = 1;
                        //ex.printStackTrace();
                        if(fp!=null){
                            fp.writer2.close();
                        }
                    }
                    finally{
                        size += fp.legal_sum;
                        ok += fp.legal_ok;
                        if(flag==0){
                          System.out.println(files[z].getName()+" PARSED SUCCESSFULY INTO "+files[z].getName().replace(".pdf.txt", "")+".n3");
                          count ++;
                        }
                        else{
                          System.out.println(files[z].getName()+" NOT PARSED SUCCESSFULY INTO "+files[z].getName().replace(".pdf.txt", "")+".n3");
                          File file = new File("n3/"+files[z].getName().replace(".pdf.txt", "")+".n3");
                          if(file.delete()){
                                  System.out.println(file.getName().replace(".pdf.txt", "")+".n3" + " DELETED!");
                          }
                          else{
                                  System.out.println("DELETE "+files[z].getName().replace(".pdf.txt","")+ ".n3 FAILED!");
                          }
                        }
                        flag=0;
                    }
                }

            }
            System.out.println("====================================================");
            System.out.println("PARSED SUCCESSFULLY "+count+"/"+files.length+" FILES");
            System.out.println("PARSED SUCCESSFULLY "+ok+"/"+size+" LEGAL DOCS");
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
                if(f.getName().endsWith(".n3")&&(!f.getName().equals("merged.n3"))&&(f.length()!=0)){
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
    
    
    public static void mergeFiles(File[] files, File mergedFile) {
 
		FileWriter fstream = null;
		BufferedWriter out = null;
		try {
			fstream = new FileWriter(mergedFile, true);
			 out = new BufferedWriter(fstream);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
 
		for (File f : files) {
                    if(f.getName().endsWith(".n3")&&(!f.getName().equals("merged.n3"))){
			System.out.println("merging: " + f.getName());
			FileInputStream fis;
			try {
				fis = new FileInputStream(f);
				BufferedReader in = new BufferedReader(new InputStreamReader(fis));
 
				String aLine;
				while ((aLine = in.readLine()) != null) {
					out.write(aLine);
					out.newLine();
				}
 
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
                    }
		}
 
		try {
			out.close();
                        System.out.println("merging: COMPLETED");
		} catch (IOException e) {
			e.printStackTrace();
		}
 
	}
    
}
