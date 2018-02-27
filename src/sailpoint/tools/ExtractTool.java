/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sailpoint.tools;

import java.io.File;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import sailpoint.tools.Bundle;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;





/**
 *
 * @author augrimm
 */
public class ExtractTool {
    
    private static final String BUNDLE_FILE_HEADER = "Role,Profile Description,Profile Filter(s),Profile Application";
    private static final String COMMA_DELIMITER = ",";
    private static final String NEW_LINE_SEPARATOR = "\n";
    private static final String SOD_FILE_HEADER = "Application,Entitlement,Conflict";
    
    public static void main(String[] args){
        String bundleFile = args[0];
        String policyFile = args[1];
        
        extractBundles(bundleFile);
        extractSODs(policyFile);
    }
    
    public static void extractBundles(String filePath){    
        List<Bundle> allBundles = new ArrayList(); 
        try {
            File file = new File(filePath);
            DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = dBuilder.parse(file);
            NodeList bundles = doc.getElementsByTagName("Bundle");
            int length = bundles.getLength();
            //System.out.println(length);
            for(int i = 0; i < length; i++){
                Element bundle = (Element)bundles.item(i);
                String name = bundle.getAttribute("name");
                String application = new String();
                String description = new String();
                //SPFilter profile = null;
                String operation = new String();
                String property = new String();
                String cf_operation = new String();
                //System.out.println(name);
                
                Element AppRef = (Element)bundle.getElementsByTagName("ApplicationRef").item(0);
                if (AppRef!=null){
                    Element ref = (Element)AppRef.getChildNodes().item(1);
                    application = ref.getAttribute("name");
                    //System.out.println(application);
                } else {application = "No App";}
                
                Element desc = (Element)bundle.getElementsByTagName("Description").item(0);
                if (desc !=null){
                    description = desc.getTextContent();
                    //System.out.println(description);
                } else {description = "No Description";}
                
                Element constraints = (Element)bundle.getElementsByTagName("Constraints").item(0);
                
                if (constraints != null){
                    
                    NodeList filters = constraints.getChildNodes();
                    Element firstNode = (Element)filters.item(1);
                    String tagName = firstNode.getTagName();
                    if (tagName.equals("CompositeFilter")){
                       cf_operation = firstNode.getAttribute("operation");
                    }
                    NodeList filters2 = constraints.getElementsByTagName("Filter");
                    //List<SPFilter> filter_objs = new ArrayList();
                    for (int k = 0; k < filters2.getLength(); k++){
                        Element filter = (Element)filters2.item(k);
                        operation = filter.getAttribute("operation");
                        property = filter.getAttribute("property");
                        
                        Element list = (Element)filter.getElementsByTagName("List").item(0);
                        if (list != null){
                            NodeList items = list.getElementsByTagName("String");
                            int items_length = items.getLength();
                            String a[] = new String[items_length];
                            for (int j = 0; j < items_length; j++) {
                                a[j] = items.item(j).getTextContent();
                            }
                            SPFilter profile = new SPFilter(operation, property, a);
                            System.out.println(profile.toString());
                            //filter_objs.add(fil);
                            Bundle bund = new Bundle(name, description, profile, application); 
            
                            allBundles.add(bund);
                        }
                    
                    }
                }   
                    
            
                
            }
        } catch (Exception e) {
	System.out.println(e.getMessage());
        }
        
       FileWriter fileWriter = null;
        
        try {
            fileWriter = new FileWriter("RolesExtract.txt");
            fileWriter.append(BUNDLE_FILE_HEADER.toString());
            fileWriter.append(NEW_LINE_SEPARATOR);
            for (Bundle bundle : allBundles) {
                fileWriter.append(String.valueOf(bundle.getRoleName()));
                fileWriter.append(COMMA_DELIMITER);
                fileWriter.append(String.valueOf(bundle.getDescription()));
                fileWriter.append(COMMA_DELIMITER);
                fileWriter.append("\'" + String.valueOf(bundle.getProfile().getFilter()) + "\'");
                fileWriter.append(COMMA_DELIMITER);
                fileWriter.append(String.valueOf(bundle.getApplication()));
                fileWriter.append(NEW_LINE_SEPARATOR);
            }
            
            System.out.println("Success");
            
        } catch (IOException ex) {
            Logger.getLogger(ExtractTool.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try{
                 fileWriter.flush();
                 fileWriter.close();
            }
            catch (IOException e) {
                System.out.println("Error while flushing/closign filewriter");
                e.printStackTrace();
            }
        }
    }
    
    public static void extractSODs(String filePath){
        List<SOD> allSODs = new ArrayList();
        try {
            File file = new File(filePath);
            DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = dBuilder.parse(file);
            
            NodeList policies = doc.getElementsByTagName("Policy");
            int length = policies.getLength();
            //System.out.println(length);
            for(int i = 0; i < length; i++){
                Element policy = (Element)policies.item(i);
                Element owner = (Element)policy.getElementsByTagName("Owner").item(0);
                if (owner != null) {
                    NodeList gencons = policy.getElementsByTagName("GenericConstraint");
                    int gclength = gencons.getLength();
                    //System.out.println(gclength);
                    for(int k = 0; k < gclength; k++){
                        Element gencon = (Element)gencons.item(k);
                        String app = new String();
                        String ent = new String();
                        //List<String> conflict = new ArrayList<String>();
                        NodeList matchterms = gencon.getElementsByTagName("MatchTerm");
                        int mtlength = matchterms.getLength();
                        //System.out.println(mtlength);
                        String con[] = new String[mtlength - 1];
                        for (int j = 0; j < mtlength; j++){
                            //System.out.println(j);
                            Element matchterm = (Element)matchterms.item(j);
                            if (j == 0) {
                                ent = matchterm.getAttribute("value");
                                Element appRef = (Element)matchterm.getElementsByTagName("Reference").item(0);
                                app = appRef.getAttribute("name");
                            }
                            else {
                                con[j-1] = matchterm.getAttribute("value");
                            }
                          
                        }
                        
                        
                        SOD sod = new SOD(app,ent,con);
                        System.out.println(sod.toString());
                        allSODs.add(sod);
                  
                    }
                    
                }    
                      
            }
            
        } catch (Exception e) {
	//System.out.println(e.getMessage());
        }
        
         FileWriter fileWriter = null;
        
        try {
            fileWriter = new FileWriter("PolicyExtract.txt");
            fileWriter.append(SOD_FILE_HEADER.toString());
            fileWriter.append(NEW_LINE_SEPARATOR);
            for (SOD sod : allSODs) {
                fileWriter.append(String.valueOf(sod.getApp()));
                fileWriter.append(COMMA_DELIMITER);
                fileWriter.append(String.valueOf(sod.getEnt()));
                fileWriter.append(COMMA_DELIMITER);
                fileWriter.append(String.valueOf(sod.getCon()));
                fileWriter.append(NEW_LINE_SEPARATOR);
            }
            
            System.out.println("Success");
            
        } catch (IOException ex) {
            Logger.getLogger(ExtractTool.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try{
                 fileWriter.flush();
                 fileWriter.close();
            }
            catch (IOException e) {
                System.out.println("Error while flushing/closign filewriter");
                e.printStackTrace();
            }
        }
    }
    
    public static String setProfile(String att, String operation, String[] ents) {
        String profile = att + "." + operation + "({";
        for(String ent : ents){
            profile = profile + '"' + ent + '"' + ",";
        }
        profile = profile.substring(0, profile.length() - 1);
        profile = profile + "})";
        
        return profile;
     }
    
}

   
