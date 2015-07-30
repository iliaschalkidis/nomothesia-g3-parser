
package com.di.nomothesia.model;

public class Passage implements Fragment {
   
    private String URI; 
    private String text;
    private int id;
    private int status;
    private String type;
    
    public Passage() {

    }
    
    //Setters-Getters for Passage
    @Override
    public int getStatus() {
        return status;
    }
    
    @Override
    public void setStatus(int s) {
        this.status = s;
    }
    
    @Override
    public String getURI() {
        return URI;
    }
    
    public void setURI(String URI) {
        this.URI = URI;
    }

    public String getText() {
        return text;
    }
   
    public void setText(String text) {
        this.text = text;
    }

    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public void setType(String t) {
        this.type = t;
    }

    @Override
    public String getType() {
        return type;
    }
    
}
