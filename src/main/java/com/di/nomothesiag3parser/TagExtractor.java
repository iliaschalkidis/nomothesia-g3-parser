/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.di.nomothesiag3parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
//import java.util.Arrays;
//import java.util.Comparator;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;

public class TagExtractor {
    public ArrayList<String> extractTags(String input){
        ArrayList<String> tags = new ArrayList();
        input = input.replace("−\n", "").replace("−", "-").replace("\n", " ").replaceAll("\\p{C}", " ").replaceAll("[0-9]+","").replace(".", " ");
        List<String> stopWords = Arrays.asList("ο","η","το","οι","τα","του","της","των","τον","την","και","κι","κ","είμαι","είσαι","είναι","είμαστε","είστε","στο","στον","στη","στην","μα" ,"αλλά","από","για","προς","με","σε","ως","παρά","αντί","κατά","μετά","θα","να","δε","δεν","μη","μην","επι","ενώ","εάν","αν","τότε","που" ,"πως" ,"ποιός" ,"ποιά","ποιό","ποιοι","ποιες","ποιων","ποιους","αυτός","αυτή","αυτό","αυτοί","αυτών","αυτούς","αυτές","αυτά","εκείνος","εκείνη","εκείνο","εκείνοι","εκείνες","εκείνα","εκείνων","εκείνους","όπως","όμως","ίσως","όσο","ότι","ΚΕΦΑΛΑΙΟ","Άρθρο");
        String[] keys = input.split(" ");
        String[] uniqueKeys;
        int count = 0;
        //System.out.println(text);
        uniqueKeys = getUniqueKeys(keys);

        for(String key: uniqueKeys)
        {
            if(null == key)
            {
                break;
            }           
            for(String s : keys)
            {
                if(key.equals(s))
                {
                    count++;
                }               
            }
            if(((!stopWords.contains(key)) || key.equals(""))&&count>10){
                tags.add(key);
            }
            count=0;
        }
        
        return tags;
    }
    
    
    private static String[] getUniqueKeys(String[] keys)
    {
        String[] uniqueKeys = new String[keys.length];

        uniqueKeys[0] = keys[0];
        int uniqueKeyIndex = 1;
        boolean keyAlreadyExists = false;

        for(int i=1; i<keys.length ; i++)
        {
            for(int j=0; j<=uniqueKeyIndex; j++)
            {
                if(keys[i].equals(uniqueKeys[j]))
                {
                    keyAlreadyExists = true;
                }
            }           

            if(!keyAlreadyExists)
            {
                uniqueKeys[uniqueKeyIndex] = keys[i];
                uniqueKeyIndex++;               
            }
            keyAlreadyExists = false;
        }       
        return uniqueKeys;
    }
}