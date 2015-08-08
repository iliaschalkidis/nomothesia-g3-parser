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
import java.util.Arrays;
import java.util.List;
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
    ArrayList<String> pre_mod_lines;
    int mod_count;
    PrintWriter writer2;
    String baseURI;
    String baseURI2;
    String date;
    String mod_target;
    int legal_sum;
    int legal_ok;
    PlaceConnector pc;
    
    public FEKParser(String folder_name,String name,PlaceConnector pc){
        legaldocs = new ArrayList<>();
        legaldoc = new LegalDocumentPro();
        mods = new ArrayList<>();
        pre_mod_lines = new ArrayList<>();
        mod_count =1;
        legal_sum = 0;
        legal_ok = 0;
        this.pc = pc;
        mod_target = "";
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
            String pre_mod_line = "";
            int mod_line = 0;
            while (line != null) {
                
                if(line.matches("[0-9]+")|line.equals("ΕΦΗΜΕΡΙΣ ΤΗΣ ΚΥΒΕΡΝΗΣΕΩΣ (ΤΕΥΧΟΣ ΠΡΩΤΟ)")|line.equals("")){
                }
                else if(line.startsWith("«")){
                    //writer.println(line);
                    if(pre_line.endsWith(":")||pre_line.equals("")){
                        mod += line +"\n";
                        mod_line = 1;
                        pre_mod_line = pre_line;
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
                        pre_mod_lines.add(pre_mod_line);
                        pre_mod_line  = "";
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
//                            System.out.print("ΝΟΜΟΣ "+gg.getYear()+"/"+Type[1]+", ");
                            legaldoc.setId("http://legislation.di.uoa.gr/law/"+gg.getYear()+"/"+Type[1].replace(".",""));
                            baseURI += "law/"+gg.getYear()+"/"+Type[1];
                            writer2.println("<"+legaldoc.getId()+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Law>.");
                            break;
                    case "ΠΡΟΕΔΡΙΚΟ ΔΙΑΤΑΓΜΑ":
//                            System.out.print("ΠΔ "+gg.getYear()+"/"+Type[1]+", ");
                            legaldoc.setId("http://legislation.di.uoa.gr/pd/"+gg.getYear()+"/"+Type[1].replace(".",""));
                            baseURI += "pd/"+gg.getYear()+"/"+Type[1];
                            writer2.println("<"+legaldoc.getId()+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/PresidentialDecree>.");
                            break;
                    case "ΠΡΑΞΗ ΥΠΟΥΡΓΙΚΟΥ ΣΥΜΒΟΥΛΙΟΥ":
//                            System.out.print("ΠΥΣ "+gg.getYear()+"/"+ buffers[0]+", ");
                            legaldoc.setId("http://legislation.di.uoa.gr/amc/"+gg.getYear()+"/"+buffers[0].split(" της ")[0].replace(".",""));
                            baseURI += "amc/"+gg.getYear()+"/"+ buffers[0].split(" της ")[0];
                            writer2.println("<"+legaldoc.getId()+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/ActOfMinisterialCabinet>.");
                            Type[1]= buffers[0].split(" της ")[0];
                            break;
                    case "ΚΑΝΟΝΙΣΤΙΚΗ ΔΙΑΤΑΞΗ":
//                            System.out.print("ΚΑΝΟΝΙΣΤΙΚΗ ΔΙΑΤΑΞΗ "+gg.getYear()+"/"+Type[1].split("/")[0]+", ");
                            legaldoc.setId("http://legislation.di.uoa.gr/rp/"+gg.getYear()+"/"+Type[1].split("/")[0]);
                            baseURI += "rp/"+gg.getYear()+"/"+Type[1].split("/")[0];
                            writer2.println("<"+legaldoc.getId()+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/RegulatoryProvision>.");
                            break;
                    default: 
                            System.out.print(Type[0] +" "+gg.getYear()+"/"+Type[1]+", ");
                            legaldoc.setId("http://legislation.di.uoa.gr/law/"+gg.getYear()+"/"+Type[1].replace(".",""));
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
              

              baseURI2 = baseURI;
              FEK = FEK.replaceAll("ΕΦΗΜΕΡΙΣ ΤΗΣ ΚΥΒΕΡΝΗΣΕΩΣ \\(ΤΕΥΧΟΣ ΠΡΩΤΟ\\)", " ").replaceAll("\n[0-9]+\n"," ");
             
              if(!mods.isEmpty()){
                  writer2.println("<"+baseURI2+"> <http://www.metalex.eu/metalex/2008-05-02#realizedBy> <"+baseURI2+"/"+date+">.");
              }
              
             // Check if there are citations and produce the appropriate RDF triples
             int cit = 0;
             if(FEK.contains("χοντας υπόψη:\n")||(FEK.contains("Έχουσα υπ’ όψει:\n")||(FEK.contains("Έχουσα υπ’ όψη:\n")))){
                 if(FEK.contains("αποφασίζουμε:")){
                    buffers = FEK.split("αποφασίζουμε:");
                 }
                 else if(FEK.contains(", ψηφίζει:")){
                      buffers =FEK.split(", ψηφίζει:");
                 }
                 else if(FEK.contains("αποφασίζει τα εξής:")){
                     buffers =FEK.split(", αποφασίζει τα εξής:");
                 }
                 else if(FEK.contains("αποφασίζει:")){
                     buffers =FEK.split(", αποφασίζει:");
                 }
                 else{
                    buffers = FEK.split("ζουμε:");
                 }
                 //System.out.println(buffers[0]);
                 parseCitations(buffers[0]);
                 FEK = buffers[1];
                 cit = 1;
             }
             else{
                buffers = FEK.split("Εκδίδομε τον ακόλουθο νόμο που ψήφισε η Βουλή:\n");
                FEK = buffers[1];
                buffers[0] = buffers[0].replace("−\n", "").replace("\n", " ").replace("−", "-").replaceAll("\\p{C}", " ");
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
             FEK = buffers[0].replaceAll("\n[0-9]+\n", "\n");
             FEK = FEK.replaceAll("ΕΦΗΜΕΡΙΣ ΤΗΣ ΚΥΒΕΡΝΗΣΕΩΣ \\(ΤΕΥΧΟΣ ΠΡΩΤΟ\\)", "");

             // Parse the legal document's text body according to its structure
             // There are chapters
             if(FEK.startsWith("ΚΕΦΑΛΑΙΟ")){
                 if(cit==0){
                     String title = legaldoc.getTitle();
                     title= title.replace("Ο ΠΡΟΕΔΡΟΣ\nΤΗΣ ΕΛΛΗΝΙΚΗΣ ΔΗΜΟΚΡΑΤΙΑΣ", "").replace("−\n", "").replace("\n", " ").replace("−", "-").replaceAll("\\p{C}", " ");
                     legaldoc.setTitle(title);
                     writer2.println("<"+baseURI+"> <http://purl.org/dc/terms/title> \""+title+"\"@el.");
                     String place  = pc.searchPlaces(title);
                     if(!place.isEmpty()){
                         writer2.println("<"+baseURI+"> <http://legislation.di.uoa.gr/ontology/place> <"+place+">.");
                     }
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
                 if(legaldoc.getTitle()!=null&&cit==0){
                     writer2.println("<"+baseURI+"> <http://purl.org/dc/terms/title> \""+legaldoc.getTitle()+"\"@el.");
                     String place  = pc.searchPlaces(legaldoc.getTitle());
                     if(!place.isEmpty()){
                         writer2.println("<"+baseURI+"> <http://legislation.di.uoa.gr/ontology/place> <"+place+">.");
                     }
                 }
                 writer2.println("<"+baseURI+"/article/1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Article>.");
                 writer2.println("<"+baseURI+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/article/1>.");
                 writer2.println("<"+baseURI+"/article/1/paragraph/1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Paragraph>.");
                 writer2.println("<"+baseURI+"/article/1> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/article/1/paragraph/1>.");
                 writer2.println("<"+baseURI+"/article/1/paragraph/1/passage/1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Passage>.");
                 writer2.println("<"+baseURI+"/article/1/paragraph/1> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/article/1/paragraph/1/passage/1>.");
                 writer2.println("<"+baseURI+"/article/1/paragraph/1/passage/1> <http://legislation.di.uoa.gr/ontology/text> \""+FEK.replace("−\n", "").replace("−", "-").replace("\n", " ").replaceAll("\\p{C}", " ")+"\"@el.");
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
         String title = "";
        // Crop legal document's title
        if(citations[0].contains("Έχοντας υπόψη:\n")){
            title = citations[0].split("Έχοντας υπόψη:")[0];
        }
        else if(citations[0].contains("΄Εχοντας υπόψη:")){
            title = citations[0].split("΄Εχοντας υπόψη:")[0];
        }
        else if(citations[0].contains("Έχουσα υπ’ όψει:")){
            title = citations[0].split("Έχουσα υπ’ όψει:")[0].split("Η ΙΕΡΑ")[0];
        }
        else if (citations[0].contains("Έχουσα υπ’ όψη:")){
            title = citations[0].split("Έχουσα υπ’ όψει:")[0].split("Η ΙΕΡΑ")[0];
        }
        
        title = title.replace("Ο ΠΡΟΕΔΡΟΣ\nΤΗΣ ΕΛΛΗΝΙΚΗΣ ΔΗΜΟΚΡΑΤΙΑΣ", "").replace("ΤΟ ΥΠΟΥΡΓΙΚΟ ΣΥΜΒΟΥΛΙΟ","").replace("−\n", "").replaceAll("\n", " ").replace("−", "-");
        legaldoc.setTitle(title.replace("Ο ΠΡΟΕΔΡΟΣ ΤΗΣ\nΕΛΛΗΝΙΚΗΣ ΔΗΜΟΚΡΑΤΙΑΣ", "").replace("ΤΟ ΥΠΟΥΡΓΙΚΟ ΣΥΜΒΟΥΛΙΟ","").replace("−\n", "").replaceAll("\n", " ").replace("−", "-"));
        writer2.println("<"+baseURI+"> <http://purl.org/dc/terms/title> \""+title+"\"@el.");
        String place  = pc.searchPlaces(title);
        if(!place.isEmpty()){
             writer2.println("<"+baseURI+"> <http://legislation.di.uoa.gr/ontology/place> <"+place+">.");
        }
        
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
            }
            else if(input.charAt(count)==' '){
                if(flag==1){
                    if(!current.endsWith(" παρ")&&!current.endsWith(" ν")){//&&!current.split(" ")[current.split(" ").length-1].matches("([Α-Ω]|[α-ω]|[0-9]|\\.)+")){
                        current += ". ";
                        passages.add(current.replace("ΕΦΗΜΕΡΙΣ ΤΗΣ ΚΥΒΕΡΝΗΣΕΩΣ (ΤΕΥΧΟΣ ΠΡΩΤΟ)", ""));
                        current = "";
                        flag =0;
                    }
                    else{
                        current += ". ";
                        flag =0;
                    }
                   
                }
                else{
                    current += " ";
                    flag =0;
                }
            }
            else if(input.charAt(count)==':'){
                current += ":";
                passages.add(current.replace("ΕΦΗΜΕΡΙΣ ΤΗΣ ΚΥΒΕΡΝΗΣΕΩΣ (ΤΕΥΧΟΣ ΠΡΩΤΟ)", ""));
                current = "";
                flag =0;
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
        }
        else if(input.contains(" α.")&&input.contains(" β.")){
            symbol1 = ' ';
            symbol2 = '.';
            more_cases = 1;
        }
        if(input.contains("\nα)")&&input.contains("\nβ)")){
            symbol2 = ')';
            more_cases = 1;
        }
        else if(input.contains(" α)")&&input.contains(" β)")){
            symbol1 = ' ';
            symbol2 = ')';
            more_cases = 1;
        }
        
        while(more_cases==1){
            String[] parts = {"a"};
            if(symbol2=='.'){
                parts = input.split(symbol1+ionianums[count-1]+"\\.",2);
            }
            else if(symbol2==')'){
                parts = input.split(symbol1+ionianums[count-1]+"\\)",2);
                
            }
            else if (symbol2=='1'){
               parts = input.split("\n"+count+"\\.",2);
            }
            
            if(parts.length==2){
                if(parts[0].startsWith(" ")){
                    parts[0] = parts[0].replaceFirst(" ", "");
                }
                cases.add(parts[0].replace("ΕΦΗΜΕΡΙΣ ΤΗΣ ΚΥΒΕΡΝΗΣΕΩΣ (ΤΕΥΧΟΣ ΠΡΩΤΟ)", ""));
                input = parts[1];
                if(parts[0].equals(", ")||parts[0].equals(" ")||parts[0].equals(",")){
                    input = "";
                    cases.clear();
                    more_cases = 0;
                }
            }
            else{
                more_cases = 0;
            }
            count++;
        }
        
        if(!input.isEmpty()&&cases.size()>=1){
            cases.add(input.replace("ΕΦΗΜΕΡΙΣ ΤΗΣ ΚΥΒΕΡΝΗΣΕΩΣ (ΤΕΥΧΟΣ ΠΡΩΤΟ)", ""));
        }
        
        
        return cases;
    }
    
    int parseArticles(String chapter, int art_count){
        // Split between articles
        String[] Articles = chapter.split("\n\\bΆρθρο\\b ([0-9]+|πρώτο|δεύτερο)\n");
        //System.out.println("ARTICLES "+ chapter.split("\n\\bΆρθρο\\b ([0-9]+|πρώτο|δεύτερο)\n").length);
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
                String title = Articles[0];
                title = title.replace("Ο ΠΡΟΕΔΡΟΣ\nΤΗΣ ΕΛΛΗΝΙΚΗΣ ΔΗΜΟΚΡΑΤΙΑΣ", "").replace("ΤΟ ΥΠΟΥΡΓΙΚΟ ΣΥΜΒΟΥΛΙΟ","").replace("−\n", "").replace("\n", " ").replace("−", "-").replaceAll("\\p{C}", " ");
                legaldoc.setTitle(title);
                writer2.println("<"+baseURI+"> <http://purl.org/dc/terms/title> \""+title+"\"@el.");
                String place  = pc.searchPlaces(title);
                if(!place.isEmpty()){
                     writer2.println("<"+baseURI+"> <http://legislation.di.uoa.gr/ontology/place> <"+place+">.");
                }
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
              //System.out.println("ARTICLE "+ art_count+1);
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
            ArrayList<String> Paragraphs = new ArrayList();
            Paragraphs = this.splitParagraphs(article);
              if(Paragraphs.size()>2){
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
                      ArrayList<String> cases = this.splitCases(Paragraphs.get(z));
                      if(cases.size()>1){
                        parseCases(cases);
                      }
                      else{
                        String paragraph = Paragraphs.get(z).replace("−\n", "").replaceAll("\\p{C}", " ");
                        parsePassages(Paragraphs.get(z),type);
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
                             article += titles[k];
                         }
                      }
                      if(title.endsWith(".")&&title.split("([Α-Ω]+.)+").length>1){
                        writer2.println("<"+baseURI+"> <http://purl.org/dc/terms/title> \""+title.replace("−\n", "").replace("−", "-").replace("\n", " ").replaceAll("\\p{C}", "")+"\"@el."); 
                        article = "";
                         for(int k=1;k<titles.length;k++){
                             article += titles[k];
                         }
                      }
                      
                  }
                  String paragraph = article.replace("−\n", "").replace("−", "-").replaceAll("\\p{C}", " ");
                  paragraph = paragraph.replace("−\n", "").replaceAll("\\p{C}", " ");
                  writer2.println("<"+baseURI+"/paragraph/1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Paragraph>.");
                  writer2.println("<"+baseURI+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/paragraph/1>.");
                  baseURI+="/paragraph/1";
                  parsePassages(paragraph,type);
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
    
    void parsePassages(String paragraph, int type){
       
      ArrayList<String> passages = this.splitPassages(paragraph);
      
      if(passages.size()>1){
          for(int i=0; i< passages.size(); i++){
              String passage = passages.get(i).replace("−\n", "").replace("-\n", "").replace("−", "-").replace("\n", " ").replaceAll("\\p{C}", " ");
              writer2.println("<"+baseURI+"/passage/"+(i+1)+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Passage>.");
              writer2.println("<"+baseURI+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/passage/"+(i+1)+">.");
              writer2.println("<"+baseURI+"/passage/"+(i+1)+"> <http://legislation.di.uoa.gr/ontology/text> \""+passage+"\"@el.");
              if((passage.contains("καταργείται:")||passage.contains("ως εξής:")||passage.contains("νέο εδάφιο:")||passage.contains("τα ακόλουθα εδάφια:")||passage.contains("ακόλουθο εδάφιο:")||passage.contains("τα εξής εδάφια:")||(passage.contains("εξής:")&&!passage.endsWith("».\n"))||(passage.contains("ακολούθως:")&&!passage.contains("ως ακολούθως: α")))){
                  if(!mods.isEmpty()&&type ==0){
                    String mod = mods.get(0);
                    mods.remove(0);
                    writer2.println("<"+baseURI2+">  <http://www.metalex.eu/metalex/2008-05-02#legislativeCompetenceGroundOf> <"+baseURI+"/modification/"+mod_count+">.");
                    writer2.println("<"+baseURI2+">  <http://www.metalex.eu/metalex/2008-05-02#matterOf> <"+baseURI+"/modification/"+mod_count+">.");
                    writer2.println("<"+baseURI+"/passage/"+(i+1)+"/modification/"+mod_count+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Addition>.");
                    writer2.println("<"+baseURI+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/passage/"+(i+1)+"/modification/"+mod_count+">.");
                    if(mod.contains("Άρθρο")||mod.contains("Άρθρο")){
                        baseURI+= "/passage/"+(i+1)+"/modification/"+mod_count;
                        parseParagraphs(mod,1);
                        baseURI = baseURI.split("/passage")[0];
                    }
                    else {
                       writer2.println("<"+baseURI+"/passage/"+(i+1)+"/modification/"+mod_count+"/passage/1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Passage>.");
                       writer2.println("<"+baseURI+"/passage/"+(i+1)+"/modification/"+mod_count+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/passage/"+(i+1)+"/modification/"+mod_count+"/passage/1>.");
                       writer2.println("<"+baseURI+"/passage/"+(i+1)+"/modification/"+mod_count+"/passage/1> <http://legislation.di.uoa.gr/ontology/text> \""+mod.replace("«", "").replace("»", "").replace("−\n", "").replace("−", "-").replace("\n", " ").replaceAll("\\p{C}", " ")+"\"@el.");
                    }
                    mod_count++;
                  }
              }
          }
      }
      else{
          String passage = paragraph.replace("−\n", "").replace("−", "-").replace("\n", " ").replaceAll("\\p{C}", " ");
          writer2.println("<"+baseURI+"/passage/1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Passage>.");
          writer2.println("<"+baseURI+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/passage/1>.");
          writer2.println("<"+baseURI+"/passage/1> <http://legislation.di.uoa.gr/ontology/text> \""+paragraph.replace("−\n", "").replace("−", "-").replace("\n", " ").replaceAll("\\p{C}", " ")+"\"@el.");
          if((passage.contains("καταργείται:")||passage.contains("νέο εδάφιο:")||passage.contains("τα ακόλουθα εδάφια:")||passage.contains("ακόλουθο εδάφιο:")||passage.contains("τα εξής εδάφια:")||(passage.contains("εξής:")&&!passage.endsWith("».\n"))||(passage.contains("ακολούθως:")&&!passage.contains("ως ακολούθως: α"))||passage.endsWith(": "))&&!passage.contains("έχει ως εξής:")){
            if(!mods.isEmpty()){
                String mod = mods.get(0);
                mods.remove(0);
                writer2.println("<"+baseURI2+">  <http://www.metalex.eu/metalex/2008-05-02#legislativeCompetenceGroundOf> <"+baseURI+"/modification/"+mod_count+">.");
                writer2.println("<"+baseURI2+"/"+date+">  <http://www.metalex.eu/metalex/2008-05-02#matterOf> <"+baseURI+"/modification/"+mod_count+">.");
                writer2.println("<"+baseURI+"/passage/1/modification/"+mod_count+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Addition>.");
                writer2.println("<"+baseURI+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/passage/1/modification/"+mod_count+">.");
                //DEFINE MODIFICATION TARGET AND PATIENT
//                if(!localmod_target.equals(mod_target)&&!localmod_target.equals("")){
//                    writer2.println("<"+mod_target+"> <http://www.metalex.eu/metalex/2008-05-02#realizedBy> <"+mod_target+"/"+date+">.");
//                    writer2.println("<"+mod_target+"/"+date+">  <http://www.metalex.eu/metalex/2008-05-02#matterOf> <"+baseURI+"/modification/"+mod_count+">.");
//                    mod_target = localmod_target;
//                }
//                else if (localmod_target.equals("")&&mod_count==0){
//                    writer2.println("<"+baseURI2+"> <http://www.metalex.eu/metalex/2008-05-02#realizedBy> <"+baseURI2+"/"+date+">.");
//                    writer2.println("<"+baseURI2+"/"+date+">  <http://www.metalex.eu/metalex/2008-05-02#matterOf> <"+baseURI+"/modification/"+mod_count+">.");
//                }
//                else if(!localmod_target.equals("")){
//                    writer2.println("<"+mod_target+"/"+date+">  <http://www.metalex.eu/metalex/2008-05-02#matterOf> <"+baseURI+"/modification/"+mod_count+">.");
//                }
//                else{
//                   writer2.println("<"+baseURI2+"/"+date+">  <http://www.metalex.eu/metalex/2008-05-02#matterOf> <"+baseURI+"/modification/"+mod_count+">."); 
//                }
                if(mod.contains("Άρθρο")||mod.contains("Άρθρο")){
                    baseURI+= "/passage/1/modification/"+mod_count;
                    parseParagraphs(mod,1);
                    baseURI = baseURI.split("/passage")[0];
                }
                else {
                   writer2.println("<"+baseURI+"/passage/1/modification/"+mod_count+"/passage/1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Passage>.");
                   writer2.println("<"+baseURI+"/passage/1/modification/"+mod_count+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/passage/1/modification/"+mod_count+"/passage/1>.");
                   writer2.println("<"+baseURI+"/passage/1/modification/"+mod_count+"/passage/1> <http://legislation.di.uoa.gr/ontology/text> \""+mod.replace("«", "").replace("»", "").replace("−\n", "").replace("−", "-").replace("\n", " ").replaceAll("\\p{C}", " ")+"\"@el.");
                }
                mod_count++;
              }
              }      
      }
      
      
    }
        
    private String findModificationTargetPatient(String passage) {
        String patient = "";
        
        
        if(passage.contains("του άρθρου ")){
            String[] priors = passage.split("του άρθρου ");
            String[] ids = priors[1].split(" ");
            if(!patient.isEmpty()){
                patient +="/article/"+ids[0];
            }
            else{
                patient +="article/"+ids[0];
            }
        }
        else if(passage.contains("Το άρθρο ")){
            String[] priors = passage.split("το άρθρο ");
            String[] ids = priors[1].split(" ");
            if(!patient.isEmpty()){
                patient +="/article/"+ids[0];
            }
            else{
                patient +="article/"+ids[0];
            }
        }
        else if(passage.contains("Το άρθρο ")){
            String[] priors = passage.split("το άρθρο ");
            String[] ids = priors[1].split(" ");
            if(!patient.isEmpty()){
                patient +="/article/"+ids[0];
            }
            else{
                patient +="article/"+ids[0];
            }
        }
        
        if(passage.contains("παρ.")){
            String[] priors = passage.split("παρ. ");
            String[] ids = priors[1].split(" ");
            if(ids[0].matches("[0-9]+")){
                if(!patient.isEmpty()){
                    patient +="/paragraph/"+Integer.parseInt(ids[0]);
                }
                else{
                    patient +="paragraph/"+Integer.parseInt(ids[0]);
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
        else if(passage.contains("εδάφιο ")){
            String[] priors = passage.split("εδάφιο ");
            String[] ids = priors[1].split(" ");
            if(ids[0].matches("[0-9]+")){
                    patient +="/passage/"+Integer.parseInt(ids[0]);
            }
        }
        
        if(patient.isEmpty()){
            System.out.println("PATIENT-FEED: "+passage);
            System.out.println("PATIENT: -");
        }
        else{
            System.out.println("PATIENT-FEED: "+passage);
            System.out.println("PATIENT: "+patient);
        }
        return patient;
    }
   
    String findModificationTarget(String passage){
        String uri = "http://legislation.di.uoa.gr/";
        if(passage.contains(" ν. ")){
            String[] priors = passage.split(" ν. ");
            String[] ids = priors[1].split(" ");
            ids = ids[0].split("/");
            uri +="law/"+ids[1].replaceAll("(:|\\.|,| )+", "").replace(" ", "")+"/"+ids[0].replaceAll("(:|\\.|,| )+", "").replace(" ", "");
            System.out.println("TARGET: "+uri);
        }
        else if(passage.contains(" ν ")){
            String[] priors = passage.split(" ν ");
            String[] ids = priors[1].split(" ");
            ids = ids[0].split("/");
            uri +="law/"+ids[1].replaceAll("(:|\\.|,| )+", "").replace(" ", "")+"/"+ids[0].replaceAll("(:|\\.|,| )+", "").replace(" ", "");
            System.out.println("TARGET: "+uri);
        }
        else if(passage.contains("Ν.")){
            String[] priors = passage.split("Ν.");
            String[] ids = priors[1].split(" ");
            ids = ids[0].split("/");
            uri +="law/"+ids[1].replaceAll("(:|\\.|,| )+", "").replace(" ", "")+"/"+ids[0].replace(" ", "").replace("\\u00a0", "");
            System.out.println("TARGET: "+uri);
        }
        else if(passage.contains("Π.δ.")){
            String[] priors = passage.split("Π.δ.");
            String[] ids = priors[1].split(" ");
            ids = ids[0].split("/");
            uri +="pd/"+ids[1].replaceAll("(:|\\.|,| )+", "").replace(" ", "")+"/"+ids[0].replaceAll("(:|\\.|,| )+", "").replace(" ", "");
            //System.out.println("TARGET: "+uri);
        }
        else if(passage.contains("Πδ")){
            String[] priors = passage.split("Πδ( )*");
            String[] ids = priors[1].split(" ");
            ids = ids[0].split("/");
            uri +="pd/"+ids[1].replaceAll("(:|\\.|,| )+", "").replace(" ", "")+"/"+ids[0].replaceAll("(:|\\.|,| )+", "".replace(" ", ""));
            //System.out.println("TARGET: "+uri);
        }
        else if(passage.contains("ΠΔ")){
            String[] priors = passage.split("ΠΔ");
            String[] ids = priors[1].split(" ");
            ids = ids[0].split("/");
            uri +="pd/"+ids[1].replaceAll("(:|\\.|,| )+", "").replace(" ", "")+"/"+ids[0].replaceAll("(:|\\.|,| )+", "").replace(" ", "");
            //System.out.println("TARGET: "+uri);
        }
        else{
           // System.out.println("TARGET: "+passage);
        }
        
        return uri;
    }
        
    void parseCases(ArrayList<String> cases){
    
      int count = 0;
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
          String case1 = cases.get(k).replace("−\n", "").replace("−", "-").replaceAll("\\p{C}", "");
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
          
          if((case1.contains("καταργείται:")||case1.contains("ως εξής:")||case1.contains("νέο εδάφιο:")||case1.contains("τα εξής εδάφια:")||case1.contains("τα ακόλουθα εδάφια:")||(case1.contains("εξής:")&&!case1.endsWith("».\n"))||(case1.contains("ακολούθως:")&&!case1.contains("ως ακολούθως: α"))||(case1.endsWith(": ")))&&!case1.contains("έχει ως εξής:")||case1.contains("ακόλουθο εδάφιο:")){
              if(!mods.isEmpty()&&(count!=-1)){
                writer2.println("<"+baseURI2+">  <http://www.metalex.eu/metalex/2008-05-02#legislativeCompetenceGroundOf> <"+baseURI+"/case/"+(k+1)+"/modification/"+mod_count+">.");
                writer2.println("<"+baseURI2+"/"+date+">  <http://www.metalex.eu/metalex/2008-05-02#matterOf> <"+baseURI+"/modification/"+mod_count+">.");
                String mod = mods.get(0);
                mods.remove(0);
                 //DEFINE MODIFICATION TARGET AND PATIENT
//                String localmod_target = findModificationTarget(case1);
//                //String part_local_mod = findModificationTargetPatient(case1);
//                if(!localmod_target.equals(mod_target)&&!localmod_target.equals("")){
//                    writer2.println("<"+mod_target+"> <http://www.metalex.eu/metalex/2008-05-02#realizedBy> <"+mod_target+"/"+date+">.");
//                    writer2.println("<"+mod_target+"/"+date+">  <http://www.metalex.eu/metalex/2008-05-02#matterOf> <"+baseURI+"/modification/"+mod_count+">.");
//                    mod_target = localmod_target;
//                }
//                else if (localmod_target.equals("")&&mod_count==0){
//                    writer2.println("<"+baseURI2+"> <http://www.metalex.eu/metalex/2008-05-02#realizedBy> <"+baseURI2+"/"+date+">.");
//                    writer2.println("<"+baseURI2+"/"+date+">  <http://www.metalex.eu/metalex/2008-05-02#matterOf> <"+baseURI+"/modification/"+mod_count+">.");
//                }
//                else if(!localmod_target.equals("")){
//                    writer2.println("<"+mod_target+"/"+date+">  <http://www.metalex.eu/metalex/2008-05-02#matterOf> <"+baseURI+"/modification/"+mod_count+">.");
//                }
//                else{
//                   writer2.println("<"+baseURI2+"/"+date+">  <http://www.metalex.eu/metalex/2008-05-02#matterOf> <"+baseURI+"/modification/"+mod_count+">."); 
//                }
                if(mod.contains("Άρθρο")||mod.contains("Άρθρο")){
                    baseURI+= "/case/"+(k+1)+"/modification/"+mod_count;
                    parseParagraphs(mod.replace("«", "").replace("»", "").replaceAll("\\p{C}", " "),2);
                    baseURI = baseURI.split("/case")[0];
                }
                else {
                   writer2.println("<"+baseURI+"/case/"+(k+1)+"/modification/"+mod_count+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Addition>.");
                   writer2.println("<"+baseURI+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/case/"+(k+1)+"/modification/"+mod_count+">.");
                   writer2.println("<"+baseURI+"/case/"+(k+1)+"/modification/"+mod_count+"/passage/1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Passage>.");
                   writer2.println("<"+baseURI+"/case/"+(k+1)+"/modification/"+mod_count+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/modification/"+mod_count+"/passage/1>.");
                   writer2.println("<"+baseURI+"/case/"+(k+1)+"/modification/"+mod_count+"/passage/1> <http://legislation.di.uoa.gr/ontology/text> \""+mod.replace("«", "").replace("»", "").replace("−\n", "").replace("−", "-").replace("\n", " ").replaceAll("\\p{C}", "")+"\"@el.");
                }
                mod_count++;
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
//        FEKParser fp = new FEKParser("2015","FEK_Α49_2015-05-13_2015-05-13R.pdf.txt");
//        fp.readFile("2015","FEK_Α49_2015-05-13_2015-05-13R.pdf.txt");
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
            PlaceConnector pc = new PlaceConnector();
            pc.findPlaces();
            for(int z=0; z<files.length; z++){

                if(files[z].getName().contains(".txt")){
                    try{
                        fp = new FEKParser(folder_name,files[z].getName(),pc);
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
                        
                        if(flag==0){
                          size += fp.legal_sum;
                          ok += fp.legal_ok;
                          System.out.print("\n");
                          System.out.println(files[z].getName()+" PARSED SUCCESSFULY INTO "+files[z].getName().replace(".pdf.txt", "")+".n3");
                          count ++;
                        }
                        else{
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
