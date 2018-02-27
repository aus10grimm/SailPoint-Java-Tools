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
    
    public Bundle(String roleName, String description, SPFilter profile, String application){
        super();
        this.roleName = roleName;
        this.description = description;
        this.profile = profile;
        this.application = application;
    }
    
    public String toString() {
        return "Bundle [roleName=" + roleName + ", description=" + description + ", profile=" + profile + ", application=" + application + "]";
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
    
    
}
