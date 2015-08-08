/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.di.nomothesiag3parser;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
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
public class PlaceConnector {
    ArrayList<String> places;
    ArrayList<String> names;
    ArrayList<String> uris;
    ArrayList<String> legaldocs;
    ArrayList<String> legaldocs_uris;
    
    String searchPlaces(String title){
        
        ArrayList<Integer> possibles = new ArrayList();
        String place = "";
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
        //System.out.println("TITLE: "+caps_title);
        
        for(int i=0; i<names.size(); i++){
            if(caps_title.contains(names.get(i))){
                possibles.add(i);
                //place = uris.get(i);
//                System.out.println("TITLE: "+caps_title);
//                System.out.println("PLACE: "+places.get(i));
//                break;
            }
        }
        ArrayList<Integer> perifereies = new ArrayList();
        ArrayList<Integer> dimoi = new ArrayList();
        ArrayList<Integer> dimot_en = new ArrayList();
        ArrayList<Integer> perif_en = new ArrayList();
 //System.out.println("=====================================================================================");
        for(int i=0; i<possibles.size(); i++){
           
//            System.out.println("POSSIBLE     "+i+" "+places.get(possibles.get(i)));
//            System.out.println("POSSIBLE URI "+i+" "+uris.get(possibles.get(i)));
            if(places.get(possibles.get(i)).contains("ΔΗΜΟΣ")){
                dimoi.add(possibles.get(i));
            }
            else if(places.get(possibles.get(i)).contains("ΠΕΡΙΦΕΡΕΙΑ ")){
                perifereies.add(possibles.get(i));
            }
            else if(places.get(possibles.get(i)).contains("ΠΕΡΙΦΕΡΕΙΑΚΗ")){
                perif_en.add(possibles.get(i));
            }
//            else{
//                dimot_en.add(possibles.get(i));
//            }
        }
        
//        if(dimot_en.size()==1){
//            place = uris.get(dimot_en.get(0));
//        }
//        else{
            if(dimoi.size()==1){
                place = uris.get(dimoi.get(0));
            }
            else{
                if(perifereies.size()==1){
                    place = uris.get(perifereies.get(0));
                    if(!perif_en.isEmpty()){
                         place = uris.get(perif_en.get(0));
                    }
                }
                
            }
//        }
        
//        if(dimot_en.size()>0&&place.isEmpty()){
//            place = uris.get(dimot_en.get(0));
//        }
//        else 
        if (dimoi.size()>0&&place.isEmpty()){
            place = uris.get(dimoi.get(0));
        }
        else if (perifereies.size()>0&&place.isEmpty()){
            place = uris.get(perifereies.get(0));
        }
        else if(!possibles.isEmpty()&&place.isEmpty()){
            place = uris.get(possibles.get(0));
        }

        
//        if(!place.isEmpty()){
//            System.out.println("TITLE: "+title);
//            System.out.println("PLACE: "+place);
//            System.out.println("=====================================================================================");
//        }
        return place;
    }
    
    void findPlaces(){
        places = new ArrayList();
        uris = new ArrayList();
        names = new ArrayList();
        String sesameServer = "http://localhost:8080/openrdf-sesame";
        String repositoryID = "legislation";

        // Connect to Sesame
        Repository repo = new HTTPRepository(sesameServer, repositoryID);
        
        try {
            repo.initialize();
        } catch (RepositoryException ex) {
            Logger.getLogger(PlaceConnector.class.getName()).log(Level.SEVERE, null, ex);
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
                        name = name.split("\"")[1];
                        //name = trimDoubleQuotes(name);
                        if(name.contains("ΔΗΜΟΣ")){
                            if(name.length()>12){
                                name = name.replace("ΔΗΜΟΣ ", "");
                                uris.add(bindingSet.getValue("id").toString());
                                name = name.substring(0, name.length()-1);
                                names.add(name);
                                places.add(bindingSet.getValue("name").toString());
                            }
                            else{
                                uris.add(bindingSet.getValue("id").toString());
                                names.add(name);
                                places.add(bindingSet.getValue("name").toString());
                            }
                        }
                        else if(name.contains("ΠΕΡΙΦΕΡΕΙΑ ")&&!name.contains("ΠΕΡΙΦΕΡΕΙΑ ΑΤΤΙΚΗΣ")){
                            if(name.length()>17){
                                name = name.replace("ΠΕΡΙΦΕΡΕΙΑ ", "");
                                uris.add(bindingSet.getValue("id").toString());
                                name = name.substring(0, name.length()-1);
                                names.add(name);
                                places.add(bindingSet.getValue("name").toString());
                            }
                            else{
                                uris.add(bindingSet.getValue("id").toString());
                                names.add(name);
                                places.add(bindingSet.getValue("name").toString());
                            }
                        }
                        else if(name.contains("ΠΕΡΙΦΕΡΕΙΑ ΑΤΤΙΚΗΣ")){
                                uris.add(bindingSet.getValue("id").toString());
                                names.add(name);
                                places.add(bindingSet.getValue("name").toString());
                        }
                        else if(name.contains("ΠΕΡΙΦΕΡΕΙΑΚΗ ΕΝΟΤΗΤΑ")){
                           uris.add(bindingSet.getValue("id").toString());
                           names.add(name);
                           places.add(bindingSet.getValue("name").toString());
                        }
//                        else if(name.equals("ΕΛΛΑΔΑ")){
//                        }
//                        else {
//                           if(name.length()>24){
//                                name = name.replace("ΔHMOTIKH ΕNOTHTA ", "");
//                                uris.add(bindingSet.getValue("id").toString());
//                                name = name.substring(0, name.length()-1);
//                                names.add(name);
//                                places.add(bindingSet.getValue("name").toString());
//                           }
//                           else{
//                                uris.add(bindingSet.getValue("id").toString());
//                                names.add(name);
//                                places.add(bindingSet.getValue("name").toString());
//                            }
//                        }
                        
                        
                        
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
        System.out.print("PLACES: ");
        for(int i=0; i<places.size();i++){
         System.out.println(places.get(i)+", ");   
        }
    }
    
    
      public static String trimDoubleQuotes(String text) {
        
        int textLength = text.length();
        
        if (textLength >= 2 && text.charAt(0) == '"' && text.charAt(textLength - 1) == '"') {
            return text.substring(1, textLength - 1);
        }
        
        return text;
    
    }
    
}
