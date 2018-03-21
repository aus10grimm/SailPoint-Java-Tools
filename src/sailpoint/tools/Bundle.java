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
    
    public Bundle(String roleName, String description, SPFilter profile, String application, String sox, String priv){
        super();
        this.roleName = roleName;
        this.description = description;
        this.profile = profile;
        this.application = application;
        this.sox = sox;
        this.priv = priv;
    }
    
    public String toString() {
        return "Bundle [roleName=" + roleName + ", description=" + description + ", profile=" + profile + ", application=" + application + "SoxCritical=" + sox + "Priv=" + priv + "]";
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
        if(!sox.equals("")){
            return  sox;
        }
        else{return "false";}
    }
     
    public String getPriv(){
        if(!priv.equals("")){
            return  priv;
        }
        else{return "false";}
    }
    
}
