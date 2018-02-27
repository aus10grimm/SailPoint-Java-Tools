/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sailpoint.tools;

/**
 *
 * @author augrimm
 */
public class SOD {
    private String application;
    private String entitlement;
    private String[] conflict;
    
    public SOD(String application, String entitlement, String[] conflict) {
        super();
        this.application = application;
        this.entitlement = entitlement;
        this.conflict = conflict;
    }
    
    public String toString(){
        String all = new String();
        for (String s : conflict){
            if (s == conflict[0]){
                all = s;
            }
            else {
                all = all + ";" + s;
            }
        }
        String full = "SOD applicaiton=[" + application + "] entitlement=[" + entitlement + "} conflict=[" + all + "]";
        return full;
    }
    
    public String getApp(){
        return "\"" + application + "\"";
    }
    
    public String getEnt(){
        return "\"" + entitlement + "\"";
    }
    
    public String getCon(){
        String all = new String();
        for (String s : conflict){
            if (s == conflict[0]){
                all = s;
            }
            else {
                all = all + ";" + s;
            }
        }
        
        return "\"" + all + "\"";
       
    }
    
}
