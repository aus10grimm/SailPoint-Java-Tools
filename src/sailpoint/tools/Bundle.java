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
public class Bundle {
    private String roleName;
    private String description;
    private SPFilter profile;
    private String application;
    private String sox;
    private String priv;
    private String owner;
    
    public Bundle(String roleName, String description, SPFilter profile, String application, String sox, String priv, String owner){
        super();
        this.roleName = roleName;
        this.description = description;
        this.profile = profile;
        this.application = application;
        this.sox = sox;
        this.priv = priv;
        this.owner = owner;
    }
    
    public String toString() {
        return "Bundle [roleName=" + roleName + ", description=" + description + ", profile=" + profile + ", application=" + application + "SoxCritical=" + sox + "Priv=" + priv + "Owner=" + owner + "]";
    }
    
    public String getRoleName(){
        return  roleName;
    }
    
    public String getDescription(){
        return  description;
    }
    
    public SPFilter getProfile(){
        return  profile;
    }
    
    public String getApplication(){
        return  application;
    }
    
    public String getSox(){
        if(sox.equals("true")){
            return  "TRUE";
        }
        else{return "FALSE";}
    }
     
    public String getPriv(){
        if(priv.equals("true")){
            return  "TRUE";
        }
        else{return "FALSE";}
    }
    
    public String getOwner(){
        return owner;
    }
    
}
