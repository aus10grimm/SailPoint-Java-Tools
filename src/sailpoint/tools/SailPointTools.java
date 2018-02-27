/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sailpoint.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import static java.lang.System.out;
import static java.time.Clock.system;
import java.lang.String;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.apache.xmlbeans.*;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.util.CellReference;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.xmlbeans.impl.xb.ltgfmt.TestCase.Files;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.DocumentType;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

/**
 *
 * @author augrimm
 */

public class SailPointTools {

    public static String outputPath;
    public static String appName;
    public static String slash;
    public static String entType;
    private static final Pattern rxquote = Pattern.compile("\"");
  
    public static void main(String[] args) throws IOException, TransformerException {
        String file = args[0];
        
         
        if (System.getProperty("os.name").startsWith("Windows")) {
        // includes: Windows 2000,  Windows 95, Windows 98, Windows NT, Windows Vista, Windows XP
            slash = "\\";
        } else {
            slash = "/";
        } 
        
        Path currentRelativePath = Paths.get("");
        outputPath = currentRelativePath.toAbsolutePath().toString() + slash;
        
        InputStream ExcelFileToRead = new FileInputStream(file);
	XSSFWorkbook wb = new XSSFWorkbook(ExcelFileToRead);
        XSSFSheet general = wb.getSheet("Report");
        XSSFSheet roleSheet = wb.getSheet("IT Roles Form data (RD)");
        XSSFSheet entSheet = wb.getSheet("Entitlements Form data (RD)");
        XSSFRow r_num = entSheet.getRow(8);
        XSSFCell ent_type_cell = r_num.getCell(1, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        entType = ent_type_cell.getStringCellValue();
        
        //App info
        XSSFRow approw = general.getRow(0);
        XSSFCell appcell = approw.getCell(1,Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        appName = appcell.getStringCellValue();
        System.out.println(appName);
        File dir = new File(appName);
        dir.mkdir();
        outputPath = currentRelativePath.toAbsolutePath().toString() + slash + appName + slash;
        System.out.println("Outputing files to: " + outputPath);
              
        //Pull ent as csv file
        System.out.println("Creating Entitlemnts CSV");
        convertToCSV(wb);
        System.out.println("Finished Entitlemnts CSV");
       
        //read through for entitlement SODs
        
        if (anyEntSODs(entSheet)){
            System.out.println("Creating Entitlemnts SOD XML");
            entSODs(appName, entSheet);
            System.out.println("Finsihed Entitlemnts SOD XML");
        }
        else{
            System.out.println("No Entitlemnts SODs Defined");
        }
        
        
        if (isSheetEmpty(roleSheet)) {
            System.out.println("No Roles Defined");
        }
        else {
            //create Role.xml
            System.out.println("Creating Roles XML");
            roles(appName, roleSheet);
            System.out.println("Finished Roles XML");
            if (anyRoleSODs(roleSheet)){
                //Create Role SODs
                System.out.println("Creating Roles SOD XML");
                roleSODs(appName, roleSheet);
                System.out.println("Finsihed Roles SOD XML");
            }
            else{
                System.out.println("No Role SODs Defined");
            }
                
        }
       
        System.out.println("Generating bash script and required files");
        generateGetRoleList_txt();
        generateDelEnt_txt();
        generateBashScript();
        generateAppArg_xml();
        generateimport_txt();
               
        System.out.println("Script Completed");
        
    }    
    public static void entSODs(String appName, XSSFSheet sheet) {
        
        Document doc = createEntSOD_XML(appName);
        DataFormatter df = new DataFormatter();

        // Decide which rows to process
        int rowStart = 8;
        int rowEnd = sheet.getLastRowNum();
        
        //System.out.println(rowStart);
        //System.out.println(rowEnd);
        
        for (int rowNum = rowStart; rowNum < rowEnd; rowNum++) {
            XSSFRow r = sheet.getRow(rowNum);
            if (r == null) {
                System.out.println("This is a null row");
            }
            else{
                XSSFCell c = r.getCell(6, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                if ( (c == null) || (c.getStringCellValue() == "") ) {
                    //System.out.println(c.getStringCellValue());
                    //System.out.println("empty");
                    continue;
                } else {
                    //System.out.println(c.getStringCellValue());
                    //System.out.println("not empty");
                    String cellvalue =  df.formatCellValue(c);
                    String[] entSOD = cellvalue.split(";"); 
                    XSSFCell entCell = r.getCell(3, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    String ent = df.formatCellValue(entCell);
                    XSSFCell entTypeCell = r.getCell(1, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    entType = entTypeCell.getStringCellValue();
                    try {
                        buildGenConstraints(entSOD, ent, doc, appName, entType);
                    } catch (TransformerException ex) {
                        Logger.getLogger(SailPointTools.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
    }
    public static Document createEntSOD_XML(String appName) {
    
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
	DocumentBuilder docBuilder = null;
        try {
            docBuilder = docFactory.newDocumentBuilder();
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(SailPointTools.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        Document doc = docBuilder.newDocument();
	
        //Policy
        Element policy = doc.createElement("Policy");
        doc.appendChild(policy);
        Attr certAct = doc.createAttribute("certificationActions");
        certAct.setValue("Remediated,Mitigated,Delegated");
        policy.setAttributeNode(certAct);
        Attr conPg = doc.createAttribute("configPage");
        conPg.setValue("entitlementPolicy.xhtml");
        policy.setAttributeNode(conPg);
        Attr executor = doc.createAttribute("executor");
        executor.setValue("sailpoint.policy.EntitlementSODPolicyExecutor");
        policy.setAttributeNode(executor);
        Attr name = doc.createAttribute("name");
        name.setValue("SunTrust " + appName + " SoD Policy Violation");
        policy.setAttributeNode(name);
        Attr state = doc.createAttribute("state");
        state.setValue("Active");
        policy.setAttributeNode(state);
        Attr type = doc.createAttribute("type");
        type.setValue("EntitlementSOD");
        policy.setAttributeNode(type);
        Attr typeKey = doc.createAttribute("typeKey");
        typeKey.setValue("policy_type_entitlement_sod");
        policy.setAttributeNode(typeKey);
        Attr violationOwnerType = doc.createAttribute("violationOwnerType");
        violationOwnerType.setValue("Manager");
        policy.setAttributeNode(violationOwnerType);
            //Policy Alert
            Element policyAlert = doc.createElement("PolicyAlert");
            policy.appendChild(policyAlert);
            Attr escSty = doc.createAttribute("escalationStyle");
            escSty.setValue("none");
            policyAlert.setAttributeNode(escSty);
                //NotificationEmailTempalteRef
                Element notEmailTempRef = doc.createElement("NotificationEmailTemplateRef");
                policyAlert.appendChild(notEmailTempRef);
                //Reference
                Element notEmailref = doc.createElement("Reference");
                notEmailTempRef.appendChild(notEmailref);
                Attr notEmailclass = doc.createAttribute("class");
                notEmailclass.setValue("sailpoint.object.EmailTemplate");
                notEmailref.setAttributeNode(notEmailclass);
                Attr notEmailname = doc.createAttribute("name");
                notEmailname.setValue("Policy Violation");
                notEmailref.setAttributeNode(notEmailname);
            //Attributes
            Element attributes = doc.createElement("Attributes");
            policy.appendChild(attributes);
                //Map
                Element attMap = doc.createElement("Map");
                attributes.appendChild(attMap);
                Element mapEnt = doc.createElement("entry");
                attMap.appendChild(mapEnt);
                Attr entKey = doc.createAttribute("key");
                entKey.setValue("violationRule");
                mapEnt.setAttributeNode(entKey);
                Attr entValue = doc.createAttribute("value");
                entValue.setValue("SunTrust Policy Formatting Rule");
                mapEnt.setAttributeNode(entValue);
            //Description
            Element description = doc.createElement("Description");
            policy.appendChild(description);
            description.setTextContent("SoD Policy Vioation " + appName + " Application");
            //Owner
            Element owner = doc.createElement("Owner");
            policy.appendChild(owner);
            Element ownerRef = doc.createElement("Reference");
            owner.appendChild(ownerRef);
            Attr ownRefclass = doc.createAttribute("class");
            ownRefclass.setValue("sailpoint.object.Identity");
            ownerRef.setAttributeNode(ownRefclass);
            Attr ownRefname = doc.createAttribute("name");
            ownRefname.setValue("Technology Risk and Compliance");
            ownerRef.setAttributeNode(ownRefname);
            //Generic Constraints
            Element genConstraints = doc.createElement("GenericConstraints");
            policy.appendChild(genConstraints);
            //Build Generic Contraints
            
        return doc;
    }
    public static void buildGenConstraints(String[] entSOD, String ent, Document doc, String appName, String entType) throws TransformerException{
        //System.out.println(ent);
        Element genConstraint = doc.createElement("GenericConstraint");
        NodeList nl = doc.getElementsByTagName("GenericConstraints");
        Node genConstraints = nl.item(0);
        Attr genConName = doc.createAttribute("name");
        genConName.setValue(appName + " SoD Policy Violation of " + ent);
        genConstraint.setAttributeNode(genConName);
        Attr genConVioOwnType = doc.createAttribute("violationOwnerType");
        genConVioOwnType.setValue("None");
        genConstraint.setAttributeNode(genConVioOwnType);
        genConstraints.appendChild(genConstraint);
        Element desc = doc.createElement("Description");
        desc.setTextContent("Conflicting entitlements");
        genConstraint.appendChild(desc);
        Element remAdvice = doc.createElement("RemediationAdvice");
        remAdvice.setTextContent("Navigate to Manage --&gt; Policy Violations to take the corrective measures to either revoke or allow the policy violation.");
        genConstraint.appendChild(remAdvice);
        //orig ent
        Element idSelector = doc.createElement("IdentitySelector");
        genConstraint.appendChild(idSelector);
        Element matchExp = doc.createElement("MatchExpression");
        idSelector.appendChild(matchExp);
        Element matchTerm = doc.createElement("MatchTerm");
        matchExp.appendChild(matchTerm);
        Attr matchTermName = doc.createAttribute("name");
        matchTermName.setValue(entType);
        matchTerm.setAttributeNode(matchTermName);
        Attr matchTermValue = doc.createAttribute("value");
        matchTermValue.setValue(ent);
        matchTerm.setAttributeNode(matchTermValue);
        Element appRef = doc.createElement("ApplicationRef");
        matchTerm.appendChild(appRef);
        Element appRefRef = doc.createElement("Reference");
        appRef.appendChild(appRefRef);
        Attr appRefRefClass = doc.createAttribute("class");
        appRefRefClass.setValue("sailpoint.object.Application");
        appRefRef.setAttributeNode(appRefRefClass);
        Attr appRefRefName = doc.createAttribute("name");
        appRefRefName.setValue(appName);
        appRefRef.setAttributeNode(appRefRefName);
        idSelector = doc.createElement("IdentitySelector");
        genConstraint.appendChild(idSelector);
        matchExp = doc.createElement("MatchExpression");
        idSelector.appendChild(matchExp);
        //Loop for SOD ents
        for (String entSOD1 : entSOD) {
            matchTerm = doc.createElement("MatchTerm");
            matchExp.appendChild(matchTerm);
            matchTermName = doc.createAttribute("name");
            matchTermName.setValue(entType);
            matchTerm.setAttributeNode(matchTermName);
            matchTermValue = doc.createAttribute("value");
            matchTermValue.setValue(entSOD1);
            matchTerm.setAttributeNode(matchTermValue);
            appRef = doc.createElement("ApplicationRef");
            matchTerm.appendChild(appRef);
            appRefRef = doc.createElement("Reference");
            appRef.appendChild(appRefRef);
            appRefRefClass = doc.createAttribute("class");
            appRefRefClass.setValue("sailpoint.object.Application");
            appRefRef.setAttributeNode(appRefRefClass);
            appRefRefName = doc.createAttribute("name");
            appRefRefName.setValue(appName);
            appRefRef.setAttributeNode(appRefRefName);
        }
        
        
        
        finishEntSOD_XML(doc);
}
    public static void finishEntSOD_XML(Document doc) throws TransformerConfigurationException, TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
	Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        DOMImplementation domImpl = doc.getImplementation();
        DocumentType doctype = domImpl.createDocumentType("doctype",
                "sailpoint.dtd",
                "sailpoint.dtd");
        transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, doctype.getPublicId());
        transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, doctype.getSystemId());
	doc.setXmlStandalone(true);
        DOMSource source = new DOMSource(doc);
        String outFile = outputPath + appName + "_EntitlementSOD.xml";  
       	StreamResult result = new StreamResult(new File(outFile));

	// Output to console for testing
	// StreamResult result = new StreamResult(System.out);

	transformer.transform(source, result);

	//System.out.println("File saved!");
      
    }
    public static void roles(String appName, XSSFSheet sheet) {
        
        Document doc = createRoles_XML(appName);
        
        // Decide which rows to process
        int rowStart = 8;
        int rowEnd = sheet.getLastRowNum();
        
        //System.out.println(rowStart);
        //System.out.println(rowEnd);
        
        for (int rowNum = rowStart; rowNum < rowEnd; rowNum++) {
            XSSFRow r = sheet.getRow(rowNum);
            if (r == null) {
                System.out.println("This is a null row");
            }
            else{
                XSSFCell c = r.getCell(4, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                if ( (c == null) || (c.getStringCellValue() == "") ) {
                    //System.out.println(c.getStringCellValue());
                    //System.out.println("empty");
                    continue;
                } else {
                    //System.out.println(c.getStringCellValue());
                    //System.out.println("not empty");
                    String cellvalue =  c.getStringCellValue();
                    String[] bundle = cellvalue.split(";"); 
                    XSSFCell roleCell = r.getCell(1, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    String role = roleCell.getStringCellValue();
                    XSSFCell roleDescCell = r.getCell(2, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    String roleDesc = roleDescCell.getStringCellValue();
                    XSSFCell soxRel = r.getCell(3, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    String soxRelevant = soxRel.getStringCellValue();
                    if (soxRelevant.equals("Yes") || soxRelevant.equals("yes") ){
                       soxRelevant = "true";
                    }
                    else{
                        soxRelevant = "false";
                    }
                    XSSFCell privledge = r.getCell(10, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    String priv = privledge.getStringCellValue();
                    if (priv.equals("Yes") || priv.equals("yes") ){
                       priv = "true";
                    }
                    else{
                        priv = "false";
                    }
                    
                    XSSFCell inher = r.getCell(8, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    ArrayList<String> inheritance = new ArrayList<String>();
                    if (inher == null) {
                        inheritance.add("noInheritance");
                    }
                    else {
                        String inhert = inher.getStringCellValue();
                        String[] strInh = inhert.split(";");
                        for (String strInh1 : strInh) {
                            inheritance.add(strInh1);
                        }
                    }
                    
                  
                  
                    
                    XSSFCell ownerCell = r.getCell(6, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    String owner = ownerCell.getStringCellValue();
                    
                    try {
                        buildRoles(bundle, role, doc, appName, roleDesc, soxRelevant, inheritance, owner, entType, priv);
                            
                    }catch (TransformerException ex) {
                            Logger.getLogger(SailPointTools.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    
                    try {
                        finishRole_XML(doc);
                    } catch (TransformerException ex) {
                        Logger.getLogger(SailPointTools.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    
                }
            }
        }
    }
    public static Document createRoles_XML(String AppName) {
        
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
	DocumentBuilder docBuilder = null;
        try {
            docBuilder = docFactory.newDocumentBuilder();
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(SailPointTools.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        Document doc = docBuilder.newDocument();
                //sailpoint
        Element sailpoint = doc.createElement("sailpoint");
        doc.appendChild(sailpoint);
                
        return doc;
    }
    public static void buildRoles(String[] bundle, String role, Document doc, String appName, String roleDesc, String soxRelevant, ArrayList<String> inheritance, String owner, String entType, String priv)throws TransformerException{
        //Bundle
        Element xmlbundle = doc.createElement("Bundle");
        NodeList nl = doc.getElementsByTagName("sailpoint");
        Node sailpoint = nl.item(0);
        sailpoint.appendChild(xmlbundle);
        Attr displayName = doc.createAttribute("displayName");
        displayName.setValue(role);
        xmlbundle.setAttributeNode(displayName);
        Attr name = doc.createAttribute("name");
        name.setValue(role);
        xmlbundle.setAttributeNode(name);
        Attr type = doc.createAttribute("type");
        type.setValue("it");
        xmlbundle.setAttributeNode(type);
        //attributes
        Element attributes = doc.createElement("Attributes");
        xmlbundle.appendChild(attributes); 
        //map
        Element map = doc.createElement("Map");
        attributes.appendChild(map);
        //entry
        Element priventry = doc.createElement("entry");
        map.appendChild(priventry);
        Attr xmlpriv = doc.createAttribute("key");
        xmlpriv.setValue("Privileged");
        priventry.setAttributeNode(xmlpriv);
        Element entry = doc.createElement("entry");
            //Value
            Element privvalue = doc.createElement("value");
            priventry.appendChild(privvalue);
                //boolean
                Element privbool = doc.createElement("Boolean");
                privvalue.appendChild(privbool);
                privbool.setTextContent(priv);
        map.appendChild(entry);
        Attr xmlsoxRelevant = doc.createAttribute("key");
        xmlsoxRelevant.setValue("SOXRelevant");
        entry.setAttributeNode(xmlsoxRelevant);
            //Value
            Element value = doc.createElement("value");
            entry.appendChild(value);
                //boolean
                Element bool = doc.createElement("Boolean");
                value.appendChild(bool);
                bool.setTextContent(soxRelevant);
        Element nexEntry = doc.createElement("entry");
        map.appendChild(nexEntry);
        Attr key = doc.createAttribute("key");
        key.setValue("mergeTemplates");
        nexEntry.setAttributeNode(key);
        Attr kValue = doc.createAttribute("value");
        kValue.setValue("false");
        nexEntry.setAttributeNode(kValue);
        //Description
        Element desciption = doc.createElement("Description");
        xmlbundle.appendChild(desciption); 
        desciption.setTextContent(roleDesc);
        //NEED TO GRAB INHERITANCES
        if (!inheritance.get(0).equals("noInheritance")){
            //Inheritance
            Element xmlInheritance = doc.createElement("Inheritance");
            xmlbundle.appendChild(xmlInheritance);
            for (String inheritance1 : inheritance) {
                Element referemce = doc.createElement("Reference");
                xmlInheritance.appendChild(referemce);
                Attr spclass = doc.createAttribute("class");
                spclass.setValue("sailpoint.object.Bundle");
                referemce.setAttributeNode(spclass);
                Attr spname = doc.createAttribute("name");
                spname.setValue(inheritance1);
                referemce.setAttributeNode(spname);
            }
        }
       //owner
       Element xmlowner = doc.createElement("Owner");
       xmlbundle.appendChild(xmlowner); 
       Element ownReferemce = doc.createElement("Reference");
       xmlowner.appendChild(ownReferemce);
       Attr sptclass = doc.createAttribute("class");
       sptclass.setValue("sailpoint.object.Identity");
       ownReferemce.setAttributeNode(sptclass);
       Attr sptname = doc.createAttribute("name");
       sptname.setValue(owner);
       ownReferemce.setAttributeNode(sptname);
       //Profiles
       Element profiles = doc.createElement("Profiles");
       xmlbundle.appendChild(profiles);
        //profile
        Element profile = doc.createElement("Profile");
        profiles.appendChild(profile);
            //appRef
            Element appRef = doc.createElement("ApplicationRef");
            profile.appendChild(appRef);
            Element refApp = doc.createElement("Reference");
            appRef.appendChild(refApp);
            Attr appClass = doc.createAttribute("class");
            appClass.setValue("sailpoint.object.Application");
            refApp.setAttributeNode(appClass);
            Attr appNameXML = doc.createAttribute("name");
            appNameXML.setValue(appName);
            refApp.setAttributeNode(appNameXML);
            //Constraints
            Element contraints = doc.createElement("Constraints");
            profile.appendChild(contraints);
            Element compositeFilter = doc.createElement("CompositeFilter");
            contraints.appendChild(compositeFilter);
            Attr operation = doc.createAttribute("operation");
            operation.setValue("AND");
            compositeFilter.setAttributeNode(operation);
            //Filter
            Element filter = doc.createElement("Filter");
            compositeFilter.appendChild(filter);
            Attr filterOp = doc.createAttribute("operation");
            filterOp.setValue("CONTAINS_ALL");
            filter.setAttributeNode(filterOp);
            Attr property = doc.createAttribute("property");
            property.setValue(entType);
            filter.setAttributeNode(property);
            //Value
            Element filValue = doc.createElement("Value");
            filter.appendChild(filValue);
            //List
            Element list = doc.createElement("List");
            filValue.appendChild(list);
            for(String bundle1 : bundle){
                Element xmlstring = doc.createElement("String");
                list.appendChild(xmlstring);
                xmlstring.setTextContent(bundle1);
                
            }
        
    }
    public static void finishRole_XML(Document doc) throws TransformerConfigurationException, TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
	Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        DOMImplementation domImpl = doc.getImplementation();
        DocumentType doctype = domImpl.createDocumentType("doctype",
                "sailpoint.dtd",
                "sailpoint.dtd");
        transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, doctype.getPublicId());
        transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, doctype.getSystemId());
	doc.setXmlStandalone(true);
        DOMSource source = new DOMSource(doc);
        String outputFile = outputPath + appName + "_Roles.xml"; 
       	StreamResult result = new StreamResult(new File(outputFile));

	// Output to console for testing
	// StreamResult result = new StreamResult(System.out);

	transformer.transform(source, result);

	//System.out.println("File saved!");
    }
    static void convertToCSV (XSSFWorkbook csv) throws UnsupportedEncodingException, FileNotFoundException, IOException {
    
    Workbook wb = csv;
    FormulaEvaluator fe = null;
    fe = wb.getCreationHelper().createFormulaEvaluator();
  
    DataFormatter formatter = new DataFormatter();
    String outputFile = outputPath + appName + "_Entitlements.csv";
    PrintStream out = new PrintStream(new FileOutputStream(outputFile), true, "UTF-8");
    byte[] bom = {(byte)0xEF, (byte)0xBB, (byte)0xBF};
    out.write(bom);
    {
    Sheet sheet = wb.getSheet("CSV");
    for (int r = 0, rn = sheet.getLastRowNum() ; r <= rn ; r++) {
        Row row = sheet.getRow(r);
        if ( row == null ) { out.println(','); continue; }
        boolean firstCell = true;
        for (int c = 0, cn = row.getLastCellNum() ; c < cn ; c++) {
            Cell cell = row.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            if ( ! firstCell ) out.print(',');
            if ( cell != null ) {
                if ( fe != null ) cell = fe.evaluateInCell(cell);
                String value = formatter.formatCellValue(cell);
                if ( cell.getCellTypeEnum() == CellType.FORMULA ) {
                    value = "=" + value;
                }
                out.print(encodeValue(value));
            }
            firstCell = false;
        }
        out.println();
    }
        }
    }
    static private String encodeValue(String value) {
        boolean needQuotes = false;
        if ( value.indexOf(',') != -1 || value.indexOf('"') != -1 ||
             value.indexOf('\n') != -1 || value.indexOf('\r') != -1 )
            needQuotes = true;
        Matcher m = rxquote.matcher(value);
        if ( m.find() ) needQuotes = true; value = m.replaceAll("\"\"");
        if ( needQuotes ) return "\"" + value + "\"";
        else return value;
    }
    public static void roleSODs (String appName, XSSFSheet sheet) {
        Document doc = createRoleSOD_XML(appName);
             
        int rowStart = 8;
        int rowEnd = sheet.getLastRowNum();
        
        //System.out.println(rowStart);
        //System.out.println(rowEnd);
        
        for (int rowNum = rowStart; rowNum < rowEnd; rowNum++) {
            XSSFRow r = sheet.getRow(rowNum);
            if (r == null) {
                System.out.println("This is a null row");
            }
            else{
                XSSFCell c = r.getCell(5, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                if ( (c == null) || (c.getStringCellValue() == "") ) {
                    //System.out.println(c.getStringCellValue());
                    //System.out.println("empty");
                    continue;
                } else {
                    //System.out.println(c.getStringCellValue());
                    //System.out.println("not empty");
                    String cellvalue =  c.getStringCellValue();
                    String[] roleSOD = cellvalue.split(";"); 
                    XSSFCell roleCell = r.getCell(1, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    String role = roleCell.getStringCellValue();
                    try {
                        buildRoleSODs(roleSOD, role, doc, appName, cellvalue);
                    } catch (TransformerException ex) {
                        Logger.getLogger(SailPointTools.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            
        }
        try {
            finishRoleSOD_XML(doc);
        } catch (TransformerException ex) {
            Logger.getLogger(SailPointTools.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    public static Document createRoleSOD_XML(String appName) {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
	DocumentBuilder docBuilder = null;
        try {
            docBuilder = docFactory.newDocumentBuilder();
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(SailPointTools.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        Document doc = docBuilder.newDocument();
        
        //Policy
        Element policy = doc.createElement("Policy");
        doc.appendChild(policy);
        Attr certAct = doc.createAttribute("certificationActions");
        certAct.setValue("Remediated,Mitigated,Delegated");
        policy.setAttributeNode(certAct);
        Attr conPg = doc.createAttribute("configPage");
        conPg.setValue("sodpolicy.xhtml");
        policy.setAttributeNode(conPg);
        Attr executor = doc.createAttribute("executor");
        executor.setValue("sailpoint.policy.SODPolicyExecutor");
        policy.setAttributeNode(executor);
        Attr name = doc.createAttribute("name");
        name.setValue("SunTrust " + appName + " Role SoD Policy Violation");
        policy.setAttributeNode(name);
        Attr state = doc.createAttribute("state");
        state.setValue("Active");
        policy.setAttributeNode(state);
        Attr type = doc.createAttribute("type");
        type.setValue("SOD");
        policy.setAttributeNode(type);
        Attr typeKey = doc.createAttribute("typeKey");
        typeKey.setValue("policy_type_sod");
        policy.setAttributeNode(typeKey);
        Attr violationOwnerType = doc.createAttribute("violationOwnerType");
        violationOwnerType.setValue("Manager");
        policy.setAttributeNode(violationOwnerType);
            //Policy Alert
            Element policyAlert = doc.createElement("PolicyAlert");
            policy.appendChild(policyAlert);
            Attr escSty = doc.createAttribute("escalationStyle");
            escSty.setValue("none");
            policyAlert.setAttributeNode(escSty);
                //NotificationEmailTempalteRef
                Element notEmailTempRef = doc.createElement("NotificationEmailTemplateRef");
                policyAlert.appendChild(notEmailTempRef);
                //Reference
                Element notEmailref = doc.createElement("Reference");
                notEmailTempRef.appendChild(notEmailref);
                Attr notEmailclass = doc.createAttribute("class");
                notEmailclass.setValue("sailpoint.object.EmailTemplate");
                notEmailref.setAttributeNode(notEmailclass);
                Attr notEmailname = doc.createAttribute("name");
                notEmailname.setValue("Policy Violation");
                notEmailref.setAttributeNode(notEmailname);
            //Attributes
            Element attributes = doc.createElement("Attributes");
            policy.appendChild(attributes);
                //Map
                Element attMap = doc.createElement("Map");
                attributes.appendChild(attMap);
                Element mapEnt = doc.createElement("entry");
                attMap.appendChild(mapEnt);
                Attr entKey = doc.createAttribute("key");
                entKey.setValue("violationRule");
                mapEnt.setAttributeNode(entKey);
                Attr entValue = doc.createAttribute("value");
                entValue.setValue("SunTrust Policy Formatting Rule");
                mapEnt.setAttributeNode(entValue);
            //Owner
            Element owner = doc.createElement("Owner");
            policy.appendChild(owner);
            Element ownerRef = doc.createElement("Reference");
            owner.appendChild(ownerRef);
            Attr ownRefclass = doc.createAttribute("class");
            ownRefclass.setValue("sailpoint.object.Identity");
            ownerRef.setAttributeNode(ownRefclass);
            Attr ownRefname = doc.createAttribute("name");
            ownRefname.setValue("Technology Risk and Compliance");
            ownerRef.setAttributeNode(ownRefname);
            //SOD Constraints
            Element sodConstraints = doc.createElement("SODConstraints");
            policy.appendChild(sodConstraints);
            //Build Generic Contraints
        
        return doc;
    }
    public static void finishRoleSOD_XML(Document doc) throws TransformerConfigurationException, TransformerException{
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
	Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        DOMImplementation domImpl = doc.getImplementation();
        DocumentType doctype = domImpl.createDocumentType("doctype",
                "sailpoint.dtd",
                "sailpoint.dtd");
        transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, doctype.getPublicId());
        transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, doctype.getSystemId());
	doc.setXmlStandalone(true);
        DOMSource source = new DOMSource(doc);
        String outputFile = outputPath + appName + "_RoleSOD.xml";
       	StreamResult result = new StreamResult(new File(outputFile));

	// Output to console for testing
	// StreamResult result = new StreamResult(System.out);

	transformer.transform(source, result);
    }
    public static void buildRoleSODs(String[] roleSOD, String role, Document doc, String appName, String combined) throws TransformerException{
        Element sodConstraint = doc.createElement("SODConstraint");
        NodeList nl = doc.getElementsByTagName("SODConstraints");
        Node sodConstraints = nl.item(0);
        Attr sodConName = doc.createAttribute("name");
        sodConName.setValue(appName + " SoD Policy Violation of " + role);
        sodConstraint.setAttributeNode(sodConName);
        Attr sodConVioOwnType = doc.createAttribute("violationOwnerType");
        sodConVioOwnType.setValue("None");
        sodConstraint.setAttributeNode(sodConVioOwnType);
        sodConstraints.appendChild(sodConstraint);
        Element sodDescription = doc.createElement("Description");
        sodDescription.setTextContent(role + " conflicts with " + combined);
        sodConstraint.appendChild(sodDescription);
        Element leftBundles = doc.createElement("LeftBundles");
        sodConstraint.appendChild(leftBundles);
        Element lbRef = doc.createElement("Reference");
        leftBundles.appendChild(lbRef);
        Attr lbRefClass = doc.createAttribute("class");
        lbRefClass.setValue("sailpoint.object.Bundle");
        lbRef.setAttributeNode(lbRefClass);
        Attr lbRefName = doc.createAttribute("name");
        lbRefName.setValue(role);
        lbRef.setAttributeNode(lbRefName);
        Element remAdvice = doc.createElement("RemediationAdvice");
        remAdvice.setTextContent("Navigate to Manage -- Policy Violations to take the corrective measures to either revoke or allow the policy violation.");
        sodConstraint.appendChild(remAdvice);
        Element rightBundle = doc.createElement("RightBundles");
        sodConstraint.appendChild(rightBundle);
        
        for (String roleSOD1 : roleSOD) {
            Element rbRef = doc.createElement("Reference");
            rightBundle.appendChild(rbRef);
            Attr rbRefClass = doc.createAttribute("class");
            rbRefClass.setValue("sailpoint.object.Bundle");
            rbRef.setAttributeNode(rbRefClass);
            Attr rbRefName= doc.createAttribute("name");
            rbRefName.setValue(roleSOD1);
            rbRef.setAttributeNode(rbRefName);
     
        }
     }
    public static Boolean isSheetEmpty(XSSFSheet sheet){
       Iterator rows = sheet.rowIterator();
       while (rows.hasNext()) {
           XSSFRow row = (XSSFRow) rows.next();
           Iterator cells = row.cellIterator();
           while (cells.hasNext()) {
                XSSFCell cell = (XSSFCell) cells.next();
                if(!cell.getStringCellValue().isEmpty()){
                    return false;
                }
           }
       }
       return true;
    }
    public static Boolean anyEntSODs(XSSFSheet sheet){
        int rowStart = 8;
        int rowEnd = sheet.getLastRowNum();
        for (int rowNum = rowStart; rowNum < rowEnd; rowNum++) {
            XSSFRow r = sheet.getRow(rowNum);
            if (r == null) {
                System.out.println("This is a null row");
            }
            else{
                XSSFCell c = r.getCell(6, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                if ( (c == null) || (c.getStringCellValue() == "") ) {
                    continue;
                }
                else {
                    return true;
                }
                   
            }
        }
        return false;
    }
    public static Boolean anyRoleSODs(XSSFSheet sheet){
        int rowStart = 8;
        int rowEnd = sheet.getLastRowNum();
        for (int rowNum = rowStart; rowNum < rowEnd; rowNum++) {
            XSSFRow r = sheet.getRow(rowNum);
            if (r == null) {
                System.out.println("This is a null row");
            }
            else{
                XSSFCell c = r.getCell(5, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                if ( (c == null) || (c.getStringCellValue() == "") ) {
                    continue;
                }
                else {
                    return true;
                }
                   
            }
        }
        return false;
    }
    public static void generateGetRoleList_txt() throws FileNotFoundException, UnsupportedEncodingException {
        String line = "search bundle id where application '" + appName + "' > roleList.txt";
        String outputFile = outputPath + "getRoleList.txt";
        PrintStream out = new PrintStream(new FileOutputStream(outputFile), true, "UTF-8");
        out.println(line);
    }
    public static void generateDelEnt_txt()throws FileNotFoundException, UnsupportedEncodingException {
        String line = "rule DeleteEntitlements '" + appName + ".xml'";
        String outputFile = outputPath + "deleteEntitlements.txt";
        PrintStream out = new PrintStream(new FileOutputStream(outputFile), true, "UTF-8");
        out.println(line);
    }
    public static void generateAppArg_xml() throws FileNotFoundException, UnsupportedEncodingException{
        String appLine = "<entry key='app_arg' value='" + appName +"'/>";
        List<String> lines = Arrays.asList("<?xml version='1.0' encoding='UTF-8'?>","<!DOCTYPE Map PUBLIC \"sailpoint.dtd\" \"sailpoint.dtd\">","<Map>",appLine,"</Map>" );
        String outputFile = outputPath + appName + ".xml";
        PrintStream out = new PrintStream(new FileOutputStream(outputFile), true, "UTF-8");
        lines.forEach((_item) -> {
            out.println(_item);
        });
     }
    public static void generateBashScript() throws FileNotFoundException, UnsupportedEncodingException{
        List<String> lines = Arrays.asList("#!/bin/bash"," ./iiq console < getRoleList.txt","sed -i -e 's/^/delete bundle /' roleList.txt","./iiq console < roleList.txt","./iiq console < deleteEntitlements.txt","./iiq console < import.txt");
        String outputFile = outputPath + "bash.sh";
        PrintStream out = new PrintStream(new FileOutputStream(outputFile), true, "UTF-8");
        lines.forEach((_item) -> {
            out.print(_item + "\n");
        });
    }
    public static void generateimport_txt() throws FileNotFoundException, UnsupportedEncodingException{
        String one = "importManagedAttribute '" + appName + "_Entitlements.csv'";
        String two = "import '" + appName + "_EntitlementSOD.xml'";
        String three = "import '" + appName + "_Roles.xml'";
        String four = "import '" + appName + "_RoleSOD.xml'";
        List<String> lines = Arrays.asList(one,two,three,four);
        String outputFile = outputPath + "import.txt";
        PrintStream out = new PrintStream(new FileOutputStream(outputFile), true, "UTF-8");
        lines.forEach((_item) -> {
            out.println(_item);
        });
    }
    
}

