
package com.di.nomothesia.model;

public interface Fragment {
    
    public String getURI();  
    public int getStatus();
    public void setStatus(int s);
    public void setType(String t);
    public String getType();
}
