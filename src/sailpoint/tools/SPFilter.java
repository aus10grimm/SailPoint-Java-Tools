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
public class SPFilter {
    private String operation;
    private String property;
    private String[] v_list;
    
    public SPFilter (String operation, String property, String[] v_list){
        super();
        this.operation = operation;
        this.property = property;
        this.v_list = v_list;
    }
    
    public String toString() {
        
        String start_v_list = " v_list = [";
        String all = new String();
        String end_v_list = "]]";
        for (String s : v_list){
            all = all + "," + s;
        }
        String full = "Filter [operation = " + operation + ", property = " + property + start_v_list + all + end_v_list;
        return full;
    }
    
    public String getFilter() {
        String full = property + "." + operation + "({"; 
        for (String s : v_list) {
            int len = v_list.length;
            if (s == v_list[0]){
                full = full + "\"" + s + "\";" ;
            }
            else if (s == v_list[len - 1]) {
              full = full + "\"" + s + "\"})";
            }
            else {
               full = full + "\"" + s +  "\";";
            }
                
        }
        return full;
    }
    
    
    
}
