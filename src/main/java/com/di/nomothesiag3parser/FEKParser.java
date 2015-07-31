/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.di.nomothesiag3parser;

import com.di.nomothesia.model.Chapter;
import com.di.nomothesia.model.GovernmentGazette;
import com.di.nomothesia.model.LegalDocumentPro;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
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
    PrintWriter writer;
    PrintWriter writer2;
    String baseURI;
    String baseURI2;
    String date;
    public FEKParser(String folder_name,String name){
        legaldocs = new ArrayList<>();
        legaldoc = new LegalDocumentPro();
        mods = new ArrayList<>();
        mod_count =1;
        baseURI = "http://legislation.di.uoa.gr/";
        try {
            File file = new File("html/"+folder_name+"/"+name.replace(".pdf.txt","")+".html");
            File file2 = new File("n3/"+folder_name+"/"+name.replace(".pdf.txt","")+".n3");
            file.getParentFile().mkdirs();
            file2.getParentFile().mkdirs();
            writer = new PrintWriter(file);
            writer2 = new PrintWriter(file2);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(FEKParser.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    void readFile(String folder,String fileName) throws Exception {
        //BufferedReader br = new BufferedReader(new FileReader("src/main/java/com/di/nomothesiag3parser/2014/"+fileName));
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
//        for(int i=0; i<mods.size(); i++){
//            writer.println("MODIFICATION "+(i+1)+": "+ mods.get(i));
//             writer.println("_________________________________________________________________________________________________________________");
//        }
        
    }
    
    void parseLegalDocument(){

      String [] legaldocuments = FEK.split("\n\\([0-9]+\\)\n");
      
      if(legaldocuments.length>2){
          FEK = legaldocuments[0].split("ΠΕΡΙΕΧΟΜΕΝΑ\n")[0] + legaldocuments[1];
          System.out.println("LEGAL DOCS: "+(legaldocuments.length-1));
      }
 
      String[] buffers = FEK.split("Αρ. Φύλλου ");
      buffers = buffers[1].split("\n",2);
      GovernmentGazette  gg = new GovernmentGazette();
      gg.setId(buffers[0]);
      writer.print(
"<!DOCTYPE html>\n" +
"<html lang=\"en\">\n" +
"    <head>\n" +
"\n" +
"        <meta charset=\"utf-8\">\n" +
"        <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">\n" +
"        <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n" +
"        <link rel=\"shortcut icon\" href=\"images/logo.png\" >\n" +
"        <title>ΝΟΜΟΘΕΣΙΑ</title>\n" +
"        <!-- Bootstrap -->\n" +
"        <link rel=\"stylesheet\" href=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.1/css/bootstrap.min.css\">\n" +
"        <link rel=\"stylesheet\" href=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.1/css/bootstrap-theme.min.css\">\n" +
"        <link href='http://fonts.googleapis.com/css?family=Jura&subset=latin,greek' rel='stylesheet' type='text/css'>\n" +
"        <link href='http://fonts.googleapis.com/css?family=Comfortaa&subset=latin,greek' rel='stylesheet' type='text/css'>\n" +
"        <link rel=\"stylesheet\" type=\"text/css\" href=\"//cdn.datatables.net/plug-ins/3cfcc339e89/integration/bootstrap/3/dataTables.bootstrap.css\">\n" +
"\n" +
"        <!-- HTML5 shim and Respond.js for IE8 support of HTML5 elements and media queries -->\n" +
"        <!-- WARNING: Respond.js doesn't work if you view the page via file:// -->\n" +
"        <!--[if lt IE 9]>\n" +
"          <script src=\"https://oss.maxcdn.com/html5shiv/3.7.2/html5shiv.min.js\"></script>\n" +
"          <script src=\"https://oss.maxcdn.com/respond/1.4.2/respond.min.js\"></script>\n" +
"        <![endif]-->\n" +
"\n" +
"        <!-- Load CSS -->\n" +
"        <link href=\"css/navbar.css\" rel=\"stylesheet\"/>\n" +
"        <link href=\"css/bootstrap-social.css\" rel=\"stylesheet\"/>\n" +
"        <link href=\"//maxcdn.bootstrapcdn.com/font-awesome/4.2.0/css/font-awesome.min.css\" rel=\"stylesheet\">\n" +
"        <link href=\"http://code.google.com/apis/maps/documentation/javascript/examples/default.css\" rel=\"stylesheet\" type=\"text/css\" />\n" +
"\n" +
"        <!-- jQueryUI Calendar-->\n" +
"        <link href=\"http://ajax.googleapis.com/ajax/libs/jqueryui/1.8/themes/base/jquery-ui.css\" rel=\"stylesheet\" type=\"text/css\"/>  \n" +
"\n" +
"        <!-- jQuery (necessary for Bootstrap's JavaScript plugins) -->\n" +
"        <script src=\"https://ajax.googleapis.com/ajax/libs/jquery/1.11.1/jquery.min.js\"></script>\n" +
"\n" +
"        <!-- Include all compiled plugins (below), or include individual files as needed -->\n" +
"        <script src=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.1/js/bootstrap.min.js\"></script>\n" +
"        <script src=\"//code.jquery.com/jquery-1.10.2.js\"></script>\n" +
"        <script src=\"//code.jquery.com/ui/1.11.2/jquery-ui.js\"></script>\n" +
"\n" +
"        <script>\n" +
"            $(document).ready(function () {\n" +
"\n" +
"                //Check to see if the window is top if not then display button\n" +
"                $(window).scroll(function () {\n" +
"                    if ($(this).scrollTop() > 100) {\n" +
"                        $('.scrollToTop').fadeIn();\n" +
"                    } else {\n" +
"                        $('.scrollToTop').fadeOut();\n" +
"                    }\n" +
"                });\n" +
"\n" +
"                //Click event to scroll to top\n" +
"                $('.scrollToTop').click(function () {\n" +
"                    $('html, body').animate({scrollTop: 0}, 800);\n" +
"                    return false;\n" +
"                });\n" +
"\n" +
"            });\n" +
"        </script>\n" +
"\n" +
"        <script type=\"text/javascript\" language=\"javascript\" src=\"//cdn.datatables.net/plug-ins/3cfcc339e89/integration/bootstrap/3/dataTables.bootstrap.js\"></script>\n" +
"\n" +
"        <style>\n" +
"            #footer {\n" +
"                position:absolute;\n" +
"                width:100%;\n" +
"                height:60px;   /* Height of the footer */\n" +
"                /*background:#6cf;*/\n" +
"            }\n" +
"\n" +
"            #share-buttons img {\n" +
"                width: 35px;\n" +
"                padding: 5px;\n" +
"                border: 0;\n" +
"                box-shadow: 0;\n" +
"                display: inline;\n" +
"            }\n" +
"\n" +
"        </style>\n" +
"    </style>\n" +
"\n" +
"</head>\n" +
"\n" +
"<body>");
      writer.println("<!-- Navigation Bar -->\n" +
"        <div id=\"custom-bootstrap-menu\" class=\"navbar navbar-default \" role=\"navigation\">\n" +
"            <div class=\"container-fluid\">\n" +
"                <div class=\"navbar-header\"><a class=\"navbar-brand\"  href=\"/\"><img style=\"height: 40px; margin-top: -10px;\" src=\"images/logo.png\"</img></a>\n" +
"                <a class=\"navbar-brand\"  href=\"/\" style=\"font-family:'Jura'; font-size: 33px\">ΝΟΜΟΘΕΣΙΑ ΦΕΚ TRANSFORMER</a>\n" +
"                <button type=\"button\" class=\"navbar-toggle\" data-toggle=\"collapse\" data-target=\".navbar-menubuilder\">\n" +
"                    <span class=\"sr-only\">Toggle navigation</span>\n" +
"                    <span class=\"icon-bar\"></span>\n" +
"                    <span class=\"icon-bar\"></span>\n" +
"                    <span class=\"icon-bar\"></span>\n" +
"                </button>\n" +
"            </div>\n" +
"\n" +
"            <div class=\"collapse navbar-collapse navbar-menubuilder\">\n" +
"                <ul class=\"nav navbar-nav navbar-left\">\n" +
"                </ul>\n" +
"\n" +
"            </div>\n" +
"        </div>\n" +
"    </div>");
//      System.println("METADATA");
//      writer.println("==========");
//      writer.println("FEK_ISSUE: "+gg.getId());
      legaldoc.setFEK(gg.getId());
      gg.setIssue("ΠΡΏΤΟ");
      FEK = buffers[1];
      //System.out.println(FEK);
      if(FEK.startsWith("ΤΕΥΧΟΣ ΠΡΩΤΟ\n")){
          FEK = FEK.replace("ΤΕΥΧΟΣ ΠΡΩΤΟ\n", "");
      }
            
      buffers = FEK.split("\n",2);
      FEK = buffers[1];
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
      //System.out.println("DATE: "+date);
      legaldoc.setYear(Date[2]);
      buffers = FEK.split("\n",2);
      FEK = buffers[1];
      String[] Type;
      if(buffers[0].contains("ΥΠ’ ΑΡΙΘ.")){
         Type = buffers[0].split(" ΥΠ’ ΑΡΙΘ. ");
      }
      else{
          Type = buffers[0].split(" ΥΠ’ ΑΡΙΘΜ. ");
      }
      
      switch (Type[0]) {
            case "NOMOΣ":  
                    legaldoc.setId("http://legislation.di.uoa.gr/law/"+gg.getYear()+"/"+Type[1]);
                    baseURI += "law/"+gg.getYear()+"/"+Type[1];
                    writer2.append("<"+legaldoc.getId()+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Law>.");
                    writer2.append("\n");
                    break;
            case "ΠΡΟΕΔΡΙΚΟ ΔΙΑΤΑΓΜΑ":
                    legaldoc.setId("http://legislation.di.uoa.gr/pd/"+gg.getYear()+"/"+Type[1]);
                    baseURI += "pd/"+gg.getYear()+"/"+Type[1];
                    writer2.append("<"+legaldoc.getId()+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/PresidentialDecree>.");
                    writer2.append("\n");
                    break;
            default: 
                    legaldoc.setId("http://legislation.di.uoa.gr/law/"+gg.getYear()+"/"+Type[1]);
                    baseURI += "law/"+gg.getYear()+"/"+Type[1];
                    writer2.append("<"+legaldoc.getId()+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Law>.");
                    writer2.append("\n");
                    break;
      }    
      writer2.append("<"+legaldoc.getId()+"> <http://legislation.di.uoa.gr/ontology/gazette> <http://legislation.di.uoa.gr/gazette/a/"+legaldoc.getYear()+"/"+legaldoc.getFEK()+">.");
      writer2.append("\n");
      writer2.append("<http://legislation.di.uoa.gr/gazette/a/"+legaldoc.getYear()+"/"+legaldoc.getFEK()+"> <http://legislation.di.uoa.gr/ontology/gazette> \"Α/"+legaldoc.getYear()+"/"+legaldoc.getFEK()+"\".");
      writer2.append("\n");
      writer2.append("<"+baseURI+"> <http://legislation.di.uoa.gr/ontology/views> \"0\"^^<http://www.w3.org/2001/XMLSchema#integer>.");
      writer2.append("\n");
      writer2.append("<"+baseURI+"> <http://purl.org/dc/terms/created> \""+date+"\"^^<http://www.w3.org/2001/XMLSchema#date>.");
      writer2.append("\n");
      writer2.append("<"+baseURI+"> <http://legislation.di.uoa.gr/ontology/legislationID> \""+Type[1]+"\"^^<http://www.w3.org/2001/XMLSchema#integer>.");
      writer2.append("\n");
      writer2.append("<"+baseURI+"> <http://legislation.di.uoa.gr/ontology/signer>  <http://legislation.di.uoa.gr/signer/1>. \n" +
"<http://legislation.di.uoa.gr/signer/1> <http://xmlns.com/foaf/0.1/name> \"ΚΑΡΟΛΟΣ ΠΑΠΟΥΛΙΑΣ\"@el.\n" +
"<http://legislation.di.uoa.gr/signer/1> <http://xmlns.com/foaf/0.1/title> \"ΠΡΟΕΔΡΟΣ ΤΗΣ ΔΗΜΟΚΡΑΤΙΑΣ\"@el.\n"+
              "<"+baseURI+"> <http://legislation.di.uoa.gr/ontology/tag> \"test\"@el.\n");
       if(mods.size()!=0){
         writer2.append("<"+baseURI+"> <http://www.metalex.eu/metalex/2008-05-02#realizedBy> <"+baseURI+"/"+date+">.");
         writer2.append("\n");
       }
       baseURI2 = baseURI;
       FEK = FEK.replaceAll("ΕΦΗΜΕΡΙΣ ΤΗΣ ΚΥΒΕΡΝΗΣΕΩΣ \\(ΤΕΥΧΟΣ ΠΡΩΤΟ\\)", " ").replaceAll("\n[0-9]+\n"," ");
//      writer.println("ID: "+ legaldoc.getId());
     writer.println("<div class=\"container\">\n" +
"        <div class=\"row\">\n" +
"            <div class=\"col-md-12\">  " +
"           <a href=\"#\" class=\"scrollToTop\"><img src=\"images/newup.png\"/></a>");
     if(FEK.contains("Έχοντας υπόψη:\n")){
         buffers = FEK.split("αποφασίζουμε:");
         parseCitations(buffers[0]);
         FEK = buffers[1];
     }
     else{
        buffers = FEK.split("Εκδίδομε τον ακόλουθο νόμο που ψήφισε η Βουλή:\n");
        FEK = buffers[1];
        buffers[0] = buffers[0].replaceAll("\n", " ");
        legaldoc.setTitle(buffers[0].replace(" Ο ΠΡΟΕΔΡΟΣ ΤΗΣ ΕΛΛΗΝΙΚΗΣ ΔΗΜΟΚΡΑΤΙΑΣ", ""));
     }
     
     
     
//     writer.println("TITLE: "+legaldoc.getTitle());
//     writer.println("____________________________________________________________________________________________________");
//     writer.println();
     buffers = FEK.split("\nΑθήνα, ");
     FEK = buffers[0].replaceAll("\n[0-9]+\n", " ");
     FEK = FEK.replaceAll("ΕΦΗΜΕΡΙΣ ΤΗΣ ΚΥΒΕΡΝΗΣΕΩΣ (ΤΕΥΧΟΣ ΠΡΩΤΟ)", "");
     
     if(FEK.startsWith("ΚΕΦΑΛΑΙΟ")){
        parseChapters();
     }
     else if(FEK.startsWith("Άρθρο")||(FEK.contains("Άρθρο"))){
        int count = -1; 
        parseArticles(FEK,count); 
     }
     else{
         //System.out.println("SIMPLE LAW");
         writer.println("<div id=\"article-1\">");
         writer.println(FEK.replace("−\n", "").replace("\n", " "));
         writer.println("</div>");
         writer2.append("<"+baseURI+"/article/1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Article>.");
         writer2.append("\n");
         writer2.append("<"+baseURI+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/article/1>.");
         writer2.append("\n");
         writer2.append("<"+baseURI+"/article/1/paragraph/1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Paragraph>.");
         writer2.append("\n");
         writer2.append("<"+baseURI+"/article/1> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/article/1/paragraph/1>.");
         writer2.append("\n");
         writer2.append("<"+baseURI+"/article/1/paragraph/1/passage/1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Passage>.");
         writer2.append("\n");
         writer2.append("<"+baseURI+"/article/1/paragraph/1> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/article/1/paragraph/1/passage/1>.");
         writer2.append("\n");
         writer2.append("<"+baseURI+"article/1/paragraph/1/passage/1> <http://legislation.di.uoa.gr/ontology/text> \""+FEK.replace("−\n", "").replace("\n", " ").replaceAll("\\p{C}", "")+"\"@el.");
         writer2.append("\n");
     }
     writer.println("<br/>");
     writer.println(buffers[1].split("20[0-9][0-9]")[1].replace("Θεωρήθηκε και τέθηκε η Μεγάλη Σφραγίδα του Κράτους.", ""));
     writer.println("</div></div></div>");
     writer.println("</body>\n" +
"</html>");
     writer.close();
     writer2.close();
    }
    
    void parseCitations(String citation_list){
        String[] citations = citation_list.split("[0-9]+\\. ");
        citation_list = citations[0].split("Έχοντας υπόψη:")[0].replaceAll("\n", " ");
        legaldoc.setTitle(citation_list.replace(" Ο ΠΡΟΕΔΡΟΣ ΤΗΣ ΕΛΛΗΝΙΚΗΣ ΔΗΜΟΚΡΑΤΙΑΣ", ""));
        writer.println("<span style=\"text-align: center;\"><h4>"+legaldoc.getTitle()+"</h4></span>");
        writer.println("<ol>");
        writer2.append("<"+baseURI+"> <http://purl.org/dc/terms/title> \""+legaldoc.getTitle()+"\"@el.");
        writer2.append("\n");
        for(int i=1; i<citations.length; i++){
            writer.println("<li>"+citations[i].replace("−\n", "").replace("\n", " ")+"</li>");
            writer2.append("<"+baseURI+"/citation/"+i+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.metalex.eu/metalex/2008-05-02#BibliographicCitation>.");
            writer2.append("\n");
            writer2.append("<"+baseURI+"/citation/"+i+"> <http://legislation.di.uoa.gr/ontology/context> \""+citations[i].replace("−\n", "").replace("\n", " ").replaceAll("\\p{C}", " ")+"\"@el.");
            writer2.append("\n");
            writer2.append("<"+baseURI+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/citation/"+i+">.");
            writer2.append("\n");
        }
        writer.println("</ol>");
    }
    
    void parseChapters(){
        //String[] buffers = FEK.split("\\bΚΕΦΑΛΑΙΟ\\b ([1-9])+\n");
        String[] buffers = FEK.split("\\bΚΕΦΑΛΑΙΟ\\b ([Α-Ω]|[0-9]|΄)+\n");
         int count = 0;
          for(int i=1; i<buffers.length; i++){
              writer.println("<div id=\"chapter-"+i+"\">");
              writer.println("<span style=\"text-align: center; font-size: 12px;\"><h3>ΚΕΦΑΛΑΙΟ "+i+"</h3></span>");
              writer2.append("<"+baseURI+"/chapter/"+i+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Chapter>.");
              writer2.append("\n");
              writer2.append("<"+baseURI+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/chapter/"+i+">.");
              baseURI += "/chapter/"+i;
              writer2.append("\n");
//              writer.println("ΚΕΦΑΛΑΙΟ "+ i);
              count = parseArticles(buffers[i],count);
              writer.println("<br/>");
              writer.println("<span style=\"text-align: center; font-size: 12px;\"><h3>____</h3></span>");
              writer.println("<br/>");
              writer.println("</div>");
              baseURI = baseURI.split("/chapter")[0];
         }
    
    }
    
    int parseArticles(String chapter, int art_count){
        String[] Articles = chapter.split("\n\\bΆρθρο\\b ([0-9]+|πρώτο|δεύτερο)\n");
        int begin = 1;
        int miss = 0;
        if(art_count != -1){
        String title = Articles[0].replace("\n", " ").replaceAll("\\p{C}", "");
        Chapter chap = new Chapter();
        chap.setTitle(title);
          writer.println("<span style=\"text-align: center; font-size: 12px;\"><h3>"+title+"</h3></span>");
          writer.println("<br/>");
          writer2.append("<"+baseURI+"> <http://purl.org/dc/terms/title> \""+title+"\"@el.");
          writer2.append("\n");
//              writer.println("ΚΕΦΑΛΑΙΟ "+ i)
          //writer.println(title);
          //"================================================");
          writer.println();
        }
        else{
            begin = 0;
            art_count = 0;
            if(Articles[0].startsWith("Άρθρο")||(Articles[0].startsWith("\nΆρθρο"))){
                 Articles[0] = Articles[0].replaceFirst("\\bΆρθρο\\b ([0-9]+|πρώτο|δεύτερο|μονό|μόνο)\n", "");
                 writer2.append("<"+baseURI+"> <http://purl.org/dc/terms/title> \""+legaldoc.getTitle().replace("\n", " ").replace("−\n", "").replaceAll("\\p{C}", "")+"\"@el.");
                 writer2.append("\n");
            }
            else{
                //System.out.println(Articles[0]);
                //System.out.println(Articles[0].replace("\n", " "));
                begin = 1;
                if(legaldoc.getTitle()==null){
                    writer.println("<span style=\"text-align: center;\"><h4>"+Articles[0].replace("\n", " ").replace("−\n", "").replaceAll("\\p{C}", "")+"</h4></span>");
                    writer2.append("<"+baseURI+"> <http://purl.org/dc/terms/title> \""+Articles[0].replace("\n", " ").replace("−\n", "").replaceAll("\\p{C}", "")+"\"@el.");
                    writer2.append("\n");
                }
            }
        }
        if(begin==0){
            miss = 1;
        }
        for(int j=begin; j<Articles.length; j++){
              int count = j + miss;
              writer.println("<div id=\"article-"+count+"\">");
              writer.println("<span style=\"text-align: center; font-size: 12px;\"><h4>Άρθρο "+(art_count+1)+"</h4></span>");
              writer2.append("<"+baseURI+"/article/"+count+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Article>.");
              writer2.append("\n");
              writer2.append("<"+baseURI+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/article/"+count+">.");
              writer2.append("\n");
              baseURI+="/article/"+count;
//              writer.println("Άρθρο "+ (art_count+1));
              parseParagraphs(Articles[j],0);
              //writer.println("======================================");
              writer.println("</div>");
              art_count++;
              baseURI = baseURI.split("/article")[0];
          }
          //writer.println("================================================");
//          writer.println("");
          return art_count;
    }
    
    void parseCases(String[] cases){
    
      int count = 0;
      for(int k=0; k<cases.length; k++){
          String[] nums = {"","α","β","γ","δ","ε","στ","ζ","η","θ"};
          String[] decnums = {"","ι","κ","λ","μ"};
          //System.out.println(k);
          //System.out.println((k/10)+" break "+(k%10));
          int flag = 0;
          if(cases[count].charAt(1)==')'){
             if(count ==0){
                    writer.println("<ol class=\"special-list\" style=\"list-style-type: none;\">");
             }
             writer.println("<li data-number=\""+decnums[((count+1)/10)]+nums[((count+1)%10)]+"\">");
          }
          else{

              if(count==0&&k==count&&!(cases[count].charAt(1)==')')){
                  count--;
              }
              else{
                if(count ==0){
                    writer.println("<ol class=\"special-list\" style=\"list-style-type: none;\">");
                }
                writer.println("<li data-number=\""+decnums[((count+1)/10)]+nums[((count+1)%10)]+"\">");
                //writer.print(decnums[((count+1)/10)]+nums[((count+1)%10)]+")");
                flag = 1;
              }
          }
          String case1 = cases[k].replace("−\n", "").replaceAll("\\p{C}", "");
          case1= case1.replace("\n", " ").replaceAll("\\p{C}", "") + "\n";
          writer2.append("<"+baseURI+"/case/"+k+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Case>.");
          writer2.append("\n");
          if((k==0)&&case1.charAt(1)==')'){
            writer.println(case1.substring(3, case1.length()));
            writer2.append("<"+baseURI+"/case/"+k+"> <http://legislation.di.uoa.gr/ontology/text> \""+case1.substring(3, case1.length())+"\"@el.");
          }
          else{
            writer.println(case1);
            writer2.append("<"+baseURI+"/case/"+k+"> <http://legislation.di.uoa.gr/ontology/text> \""+case1+"\"@el.");
          }
          writer2.append("\n");

          if((case1.contains("καταργείται:")||case1.contains("τα εξής εδάφια:")||case1.contains("τα ακόλουθα εδάφια:")||(case1.contains("εξής:")&&!case1.endsWith("».\n"))||(case1.contains("ακολούθως:")&&!case1.contains("ως ακολούθως: α"))||case1.endsWith(": "))&&!case1.contains("έχει ως εξής:")||case1.contains("ακόλουθο εδάφιο:")){
              //writer.println("====MOD=====");
             // writer.println("");
              writer.println("<div class=\"mod\">");
//                                  writer.print("«");
              if(!mods.isEmpty()){
                String mod = mods.get(0);
                mods.remove(0);
                if(mod.contains("Άρθρο")||mod.contains("Άρθρο")){
                    parseParagraphs(mod.replace("«", "").replace("»", "").replaceAll("\\p{C}", ""),1);
                }
                else {
                   writer.println(mod.replace("«", "").replace("»", "").replace("−\n", "").replace("\n", " ").replaceAll("\\p{C}", "") + "\n");
                }
                
              }
              else{
                writer.println("[MISSING!!!]");
              }
              //writer.println("=========");
//                                  writer.println("»");
//                                  writer.println("");
              writer.println("</div>");
          }
          count++;
          if(flag==1){
            writer.println("</li>");
            flag=0;
          }
      }
      writer.println("</ol>");
    }
    
    void parseParagraphs(String article,int type){
//        writer.println("###################################################");
//        writer.print(article);
//        writer.println("###################################################");
        int top = 1;
        String[] Paragraphs = article.split("\n[0-9]+\\. ");
              if(Paragraphs.length>2){
                  if(type==0){
                    //writer.println("ARTICLE_TITLE: "+ Paragraphs[0].replace("\n", ""));
                      if(!Paragraphs[0].endsWith(".")&&!Paragraphs[0].endsWith(":\n")){
                        writer.println("<span style=\"text-align: center; font-size: 12px;\"><h4>"+Paragraphs[0].replace("\n", " ").replaceAll("\\p{C}", "")+"</h4></span>");
                        writer2.append("<"+baseURI+"> <http://purl.org/dc/terms/title> \""+Paragraphs[0].replace("\n", " ").replaceAll("\\p{C}", "")+"\".");
                      }
//                    writer.println(Paragraphs[0].replace("\n", " "));
                  }
                  else{
                    writer2.append("<"+baseURI+"/article/1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Article>.");
                    writer2.append("\n");
                    writer2.append("<"+baseURI+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/article/1>.");
                    writer2.append("\n");
                      if(!Paragraphs[0].endsWith(".")&&!Paragraphs[0].endsWith(":\n")){
                      String[] title = Paragraphs[0].split("\n");
                      //writer.println("ARTICLE "+ title[0].split(" ")[1]);
                      //writer.println("ARTICLE_TITLE: "+ title[1]);
                      writer.println("<span style=\"text-align: center; font-size: 12px;\"><h4>Άρθρο "+title[0].split(" ")[1].replaceAll("\\p{C}", "")+"</h4></span>");
                      //writer.println("Άρθρο "+ title[0].split(" ")[1]);
                      //System.out.println("["+Paragraphs[0]+"]");
                      if(title.length >1){
                      writer.println("<span style=\"text-align: center; font-size: 12px;\"><h4>"+title[1].replaceAll("\\p{C}", "")+"</h4></span>");
                      writer2.append("<"+baseURI+"/article/1> <http://purl.org/dc/terms/title> \""+title[1].replaceAll("\\p{C}", "")+"\"@el.");
                      }
//                      writer.println(title[1]);
                      }
                      baseURI+= "/article/1";
                  }
                  //writer.println("======================================");
                  writer.println("<ol>");
                  if(Paragraphs[0].endsWith(".")||Paragraphs[0].endsWith(":\n")){
                      top = 0;
                      if(Paragraphs[0].matches("[0-9]+\\.(.*[\n]*)*")){
                          Paragraphs[0] = Paragraphs[0].replaceFirst("[0-9]+\\.","");
                      }
                  }
                  for(int z=top; z<Paragraphs.length; z++){
                      writer2.append("<"+baseURI+"/paragraph/"+z+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Paragraph>.");
                      writer2.append("\n");
                      writer2.append("<"+baseURI+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/paragraph/"+z+">.");
                      writer2.append("\n");
                      baseURI+="/paragraph/"+z;
                      
//                      writer.println("PARAGRAPH "+ z);
//                      writer.println("===========");
//                      writer.print(z+". ");
                      writer.println("<li>");
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
                              String paragraph = Paragraphs[z].replace("−\n", "").replaceAll("\\p{C}", "");
                              paragraph = paragraph.replace("\n", " ").replaceAll("\\p{C}", "") + "\n";
                              writer.println(paragraph);
                              writer2.append("<"+baseURI+"/passage/1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Passage>.");
                              writer2.append("\n");
                              writer2.append("<"+baseURI+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/passage/1>.");
                              writer2.append("\n");
                              writer2.append("<"+baseURI+"/passage/1> <http://legislation.di.uoa.gr/ontology/text> \""+paragraph.replace("\n", "").replaceAll("\\p{C}", "")+"\"@el.");
                              writer2.append("\n");
                              if((paragraph.contains("καταργείται:")||paragraph.contains("τα ακόλουθα εδάφια:")||paragraph.contains("ακόλουθο εδάφιο:")||paragraph.contains("τα εξής εδάφια:")||(paragraph.contains("εξής:")&&!paragraph.endsWith("».\n"))||(paragraph.contains("ακολούθως:")&&!paragraph.contains("ως ακολούθως: α"))||paragraph.endsWith(": "))&&!paragraph.contains("έχει ως εξής:")){
                                  //writer.println("====MOD=====");
                                 // writer.println("");
                                  writer.println("<div class=\"mod\">");
    //                              writer.print("«");
                                  if(!mods.isEmpty()){
                                    String mod = mods.get(0);
                                    mods.remove(0);
                                    writer2.append("<"+baseURI2+">  <http://www.metalex.eu/metalex/2008-05-02#legislativeCompetenceGroundOf> <"+baseURI+"/modification/"+mod_count+">.");
                                    writer2.append("\n");
                                    writer2.append("<"+baseURI2+">  <http://www.metalex.eu/metalex/2008-05-02#matterOf> <"+baseURI+"/modification/"+mod_count+">.");
                                    writer2.append("\n");
                                    writer2.append("<"+baseURI+"/modification/"+mod_count+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Addition>.");
                                    writer2.append("\n");
                                    writer2.append("<"+baseURI+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/modification/"+mod_count+">.");
                                    writer2.append("\n");
                                    if(mod.contains("Άρθρο")||mod.contains("Άρθρο")){
                                        baseURI+= "/paragraph/"+z+"/modification/"+mod_count;
                                        parseParagraphs(mod.replace("«", "").replace("»", "").replaceAll("\\p{C}", " "),1);
                                    }
                                    else {
                                       writer.println(mod.replace("«", "").replace("»", "").replace("−\n", "").replace("\n", " ").replaceAll("\\p{C}", " ") + "\n");
                                       writer2.append("<"+baseURI+"/modification/"+mod_count+"/passage/1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Passage>.");
                                       writer2.append("\n");
                                       writer2.append("<"+baseURI+"/modification/"+mod_count+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/modification/"+mod_count+"/passage/1>.");
                                       writer2.append("\n");
                                       writer2.append("<"+baseURI+"/modification/"+mod_count+"/passage/1> <http://legislation.di.uoa.gr/ontology/text> \""+mod.replace("«", "").replace("»", "").replace("−\n", "").replace("\n", " ").replaceAll("\\p{C}", "")+"\"@el.");
                                       writer2.append("\n");
                                    }
                                    mod_count++;
                                  }
                                  else{
                                    writer.println("[MISSING!!!]");
                                  }
                                  //writer.println("=========");
    //                              writer.println("»");
    //                              writer.println("");
                                  writer.println("</div>");
                              }
                          }
                          writer.println("</li>");
    //                      String Passages[] = paragraph.split("\\. ");
    //                      for(int k=0; k<Passages.length; k++){
    //                        writer.println(Passages[k]+".");
    //                      }
                          //writer.println("===========");
                      }
                      if(type==0){
                        baseURI = baseURI.split("/paragraph")[0];
                      }
                      else{
                        baseURI = baseURI.split("/paragraph")[0]+ "/paragraph"+baseURI.split("/paragraph")[1];
                      }
                  }
                  writer.println("</ol>");
                  if(type==0){
                    baseURI = baseURI.split("/paragraph")[0];
                  }
                  else{
                    baseURI = baseURI.split("/paragraph")[0]+ "/paragraph"+baseURI.split("/paragraph")[1];
                  }
              }
              else{
                  if(type==1){
                    writer2.append("<"+baseURI+"/article/1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Article>.");
                    writer2.append("\n");
                    writer2.append("<"+baseURI+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/article/1>.");
                    writer2.append("\n");
                    baseURI+= "/article/1";
                  }
//                  if(type==0){
//                      String[] buffers = article.split("\n");
//                      int counter=0;
//                      while(counter<buffers.length&&!buffers[ counter].endsWith(".")&&!buffers[ counter].endsWith(":")&&!buffers[ counter].endsWith("−")&&!(buffers[counter+1].charAt(0)>='Α'&&buffers[counter+1].charAt(0)<='Ω'&&counter>0)){
//                          if(counter==0){
//                            writer.println("<span style=\"text-align: center; font-size: 12px;\"><h4>");
//                          }
//                          writer.print(buffers[counter]+" ");
//                          counter++;
//                      }
//                      if(counter>0){
//                          if(!(buffers[counter].endsWith("−")||buffers[counter].endsWith("/")||buffers[counter].endsWith(".")||buffers[counter].endsWith(":")||buffers[counter].endsWith(","))){
//                           writer.print(buffers[counter]+" ");
//                            counter++;
//                          }
//                           writer.println("</h4></span>");
//                      }
//                      article = "";
//                      for(int z=counter; z<buffers.length; z++){
//                            article += buffers[z].replace("−","");
//                            if(!buffers[z].endsWith("−")){
//                                article += " ";
//                            }
//                      }
//                    //writer.println("ARTICLE_TITLE: ");
////                    writer.println();
//                  }
//                  else{
//                      String[] buffers = article.split("\n");
//                      int counter=0;
//                      while(counter<buffers.length&&!buffers[ counter].endsWith(".")&&!buffers[ counter].endsWith(":")&&!buffers[ counter].endsWith("−")&&!(buffers[counter+1].charAt(0)>='Α'&&buffers[counter+1].charAt(0)<='Ω'&&counter>0)){
//                          if(counter==0){
//                            writer.println("<span style=\"text-align: center; font-size: 12px;\"><h4>");
//                          }
//                          writer.print(buffers[counter]+" ");
//                          counter++;
//                      }
//                      if(counter>0){
//                          if(!(buffers[counter].endsWith("−")||buffers[counter].endsWith("/")||buffers[counter].endsWith(".")||buffers[counter].endsWith(":")||buffers[counter].endsWith(","))){
//                           writer.print(buffers[counter]+" ");
//                            counter++;
//                          }
//                           writer.println("</h4></span>");
//                      }
//                      article = "";
//                      for(int z=counter; z<buffers.length; z++){
//                            article += buffers[z].replace("−","");
//                            if(!buffers[z].endsWith("−")){
//                                article += " ";
//                            }
//                      }
//                      //writer.println("ARTICLE "+ title[1]);
//                      //writer.println("ARTICLE_TITLE: ");
////                      writer.println("Άρθρο "+ title[1]);
////                      writer.println();
//                  }
                  //writer.println("======================================");
//                  writer.println("");
                  //writer.println("PARAGRAPH 1");
                  //writer.println("===========");
                  String paragraph = article.replace("−\n", "").replaceAll("\\p{C}", " ");
                  paragraph = paragraph.replace("\n", " ").replaceAll("\\p{C}", " ") + "\n";
                  writer.println(paragraph);
                  writer2.append("<"+baseURI+"/paragraph/1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Paragraph>.");
                  writer2.append("\n");
                  writer2.append("<"+baseURI+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/paragraph/1>.");
                  writer2.append("\n");
                  writer2.append("<"+baseURI+"/paragraph/1/passage/1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Passage>.");
                  writer2.append("\n");
                  writer2.append("<"+baseURI+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/paragraph/1/passage/1>.");
                  writer2.append("\n");
                  writer2.append("<"+baseURI+"/paragraph/1/passage/1> <http://legislation.di.uoa.gr/ontology/text> \""+paragraph.replace("\n", "").replaceAll("\\p{C}", "")+"\"@el.");
                  writer2.append("\n");
                  if((paragraph.contains("καταργείται:")||paragraph.contains("τα ακόλουθα εδάφια:")||paragraph.contains("ακόλουθο εδάφιο:")||paragraph.contains("τα εξής εδάφια:")||(paragraph.contains("εξής:")&&!paragraph.endsWith("».\n"))||(paragraph.contains("ακολούθως:")&&!paragraph.contains("ως ακολούθως: α"))||paragraph.endsWith(": "))&&!paragraph.contains("έχει ως εξής:")){
                      //writer.println("====MOD2=====");
                      writer.println("<div class=\"mod\">");
                      if(!mods.isEmpty()){
                        String mod = mods.get(0);
                        mods.remove(0);
                        writer2.append("<"+baseURI2+">  <http://www.metalex.eu/metalex/2008-05-02#legislativeCompetenceGroundOf> <"+baseURI+"/modification/"+mod_count+">.");
                        writer2.append("\n");
                        writer2.append("<"+baseURI2+">  <http://www.metalex.eu/metalex/2008-05-02#matterOf> <"+baseURI+"/modification/"+mod_count+">.");
                        writer2.append("\n");
                        writer2.append("<"+baseURI+"/paragraph/1/modification/"+mod_count+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Addition>.");
                        writer2.append("\n");
                        writer2.append("<"+baseURI+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/paragraph/1/modification/"+mod_count+">.");
                        writer2.append("\n");
                        if(mod.contains("Άρθρο")||mod.contains("Άρθρο")){
                            baseURI+= "/paragraph/1/modification/"+mod_count;
                            parseParagraphs(mod.replace("«", "").replace("»", "").replaceAll("\\p{C}", " "),1);
                        }
                        else {
                           writer.println(mod.replace("«", "").replace("»", "").replace("−\n", "").replace("\n", " ").replaceAll("\\p{C}", " ") + "\n");
                           writer2.append("<"+baseURI+"/modification/"+mod_count+"/passage/1> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://legislation.di.uoa.gr/ontology/Passage>.");
                           writer2.append("\n");
                           writer2.append("<"+baseURI+"/paragraph/1/modification/"+mod_count+"> <http://www.metalex.eu/metalex/2008-05-02#part> <"+baseURI+"/paragraph/1/modification/"+mod_count+"/passage/1>.");
                           writer2.append("\n");
                           writer2.append("<"+baseURI+"/paragraph/1/modification/"+mod_count+"/passage/1> <http://legislation.di.uoa.gr/ontology/text> \""+mod.replace("«", "").replace("»", "").replace("−\n", "").replace("\n", " ").replaceAll("\\p{C}", "")+"\"@el.");
                           writer2.append("\n");
                        }
                        mod_count++;
                      }
                      else{
                        writer.println("[MISSING!!!]");
                      }
                      writer.println("</div>");
                      //writer.println("=========");
                  }
//                  String Passages[] = paragraph.split("\\. ");
//                  for(int k=0; k<Passages.length; k++){
//                    writer.println(Passages[k]+".");
//                  }
                  //writer.println("===========");
              }
              if(type==1){
                  baseURI = baseURI.split("/modification")[0];
              }
    }
    
    
    /**
     * @param args the command line arguments
     * @throws java.lang.Exception
     */
    public static void main(String[] args) throws Exception {
//        FEKParser fp = new FEKParser("FEK_Α5_2014-01-08_2014-01-08R.pdf.txt");
//                fp.readFile("FEK_Α5_2014-01-08_2014-01-08R.pdf.txt");
//                fp.parseLegalDocument();
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
            for(int z=0; z<files.length; z++){

                if(files[z].getName().contains(".txt")){
                    try{
                        FEKParser fp = new FEKParser(folder_name,files[z].getName());
                        fp.readFile(folder_name,files[z].getName());
                        fp.parseLegalDocument();
                    }
                    catch(Exception ex){
                        flag = 1;
                        //ex.printStackTrace();
                    }
                    finally{
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
