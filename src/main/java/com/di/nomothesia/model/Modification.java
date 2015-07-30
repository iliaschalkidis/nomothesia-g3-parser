
package com.di.nomothesia.model;

public class Modification {

    private Fragment fragment;
    private String URI;
    private String Type;
    private String patient;
    private LegalDocumentPro competenceGround;

    public Modification() {
        this.competenceGround = new LegalDocumentPro();
    }
    
    //Setters-Getters for Modification
    
    public LegalDocumentPro getCompetenceGround() {
        return competenceGround;
    }

    public String getPatient() {
        return patient;
    }

    public void setPatient(String patient) {
        this.patient = patient;
    }
    
    public Fragment getFragment() {
        return fragment;
    }

    public void setFragment(Fragment fragment) {
        this.fragment = fragment;
    }

    public String getURI() {
        return URI;
    }

    public void setURI(String URI) {
        this.URI = URI;
    }

    public String getType() {
        return Type;
    }

    public void setType(String Type) {
        this.Type = Type;
    }
    
}
