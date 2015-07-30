
package com.di.nomothesia.model;

import java.util.ArrayList;
import java.util.List;

public class Paragraph implements Fragment{
    
    private List<Passage> passages;
    private Modification modification;
    private List<Case> caseList;
    private String table;
    private String URI;
    private int id;
    private int status;
    private String type;
    
    public Paragraph() {
        
        this.passages = new ArrayList<Passage>();
        //pass.add(new Passage());
        this.caseList = new ArrayList<Case>();
        //cas.add(new Case());
        
    }
    
    //Setters-Getters for Paragraph
    @Override
    public void setType(String t) {
        this.type = t;
    }

    @Override
    public String getType() {
        return type;
    }
    
    @Override
    public int getStatus() {
        return status;
    }
    
    @Override
    public void setStatus(int s) {
        this.status = s;
    }
    
    public List<Passage> getPassages() {
        return passages;
    }

    public void setPassages(List<Passage> passages) {
        this.passages = passages;
    }

    public Modification getModification() {
        return modification;
    }

    public void setModification(Modification modification) {
        this.modification = modification;
    }

    public List<Case> getCaseList() {
        return caseList;
    }

    public void setCaseList(List<Case> caseList) {
        this.caseList = caseList;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }
    
    @Override
    public String getURI() {
        return URI;
    }

    public void setURI(String uri) {
        this.URI = uri;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
    
}
