package DataRetrieval;
import org.json.simple.parser.JSONParser;

import java.util.*;

/**
 * Created by KheyaliMitra on 1/3/2016.
 */
public class Measures {
    private String _soap_Address ;//"http://webolap.cmpt.sfu.ca/ElaWebService/Service.asmx";
    private int _nodeCounter=0;
    private String _measureOperationName = "Measures";
    /**
     * Class constructor
     * @param URL
     */
    public Measures(String URL){
        this._soap_Address = URL;
    }
    /***
     * Get measures list form database after query to the server
     * @return a hash table containing unique number and string value (measure names)
     * @throws Exception
     */
    public HashMap<Integer,String> GetMeasures(String OperationName) throws Exception {
        String measureString = this._getJSONString(OperationName);

        //create JSON parser object
        JSONParser parser = new JSONParser();
        //parse dimension string returned SOAP
        Object jsonObject = parser.parse(measureString);

        //call method to iterate through json object and populate list
        HashMap<Integer,String> measureList = _generateMeasureList(jsonObject);
        return measureList;
    }

    /***
     * Get measures list form database after query to the server Assumes measure query operation as "Measures"
     * @return a hash table containing unique number and string value (measure names)
     * @throws Exception
     */
    public HashMap<Integer,String> GetMeasures() throws Exception {
        return this.GetMeasures(this._measureOperationName);
    }

    /**
     * Generates a hashmap where key represents count key for the measure and value contains name of the measure
     * for the selected list
     * @param InputMeasures
     * @return
     */
    public HashMap<Integer,String> GetHashKeyforSelecteditems(List<String>InputMeasures,HashMap<Integer,String> measureList){
        HashMap<Integer,String> measureKeys  = new HashMap<Integer, String>();
        Map<Integer,String> treemap = measureList;
        for (Map.Entry<Integer, String> entry : treemap.entrySet()) {
            for(int i=0;i<InputMeasures.size();i++)
            {
                String val = entry.getValue();
                if (Objects.equals(InputMeasures.get(i), val)) {
                    int key = entry.getKey();
                    measureKeys.put(key,InputMeasures.get(i));
                }
            }

        }
        return measureKeys;
    }

    public List<Integer> GetSelectedKeyList(List<String>InputMeasures,HashMap<Integer,String> measureList){
        List<Integer> hm  = new ArrayList<>();
        Set<Integer> s;
        for (Map.Entry<Integer, String> entry : measureList.entrySet()) {
            for(int i=0;i<InputMeasures.size();i++)
            {
                String val = entry.getValue();
                if (Objects.equals(InputMeasures.get(i), val)) {
                    int key = entry.getKey();
                    hm.add(key);
                }
            }
        }
        return hm;
    }



    /**
     * Iterates through json object and populates hash map
     * @param jsonObject
     * @return hashmap <int, string>
     */
    private HashMap<Integer,String> _generateMeasureList(Object jsonObject) {
        HashMap<Integer, String> measures = new HashMap<>();
        // convert json object into list of strings
        List<String> listDetails = ( List<String>) jsonObject;
        Iterator iterator = listDetails.iterator();
        while (iterator.hasNext()) {
            //add entry of each measures in measures hash map
           measures.put(_nodeCounter, iterator.next().toString().split("\\|")[1]);
            _nodeCounter++;
        }
        return measures;
    }

    /**
     * Generates json string to call web method which will fetch content from server
     * @param operationName
     * @return
     * @throws Exception
     */
    private String _getJSONString(String operationName) throws Exception{
        //Creates soap retrieval object
        Soap soapRetrieval = new Soap();

        //set parameters to make it usable for both root node (parameters=null ) and child nodes (key,value pairs)
        HashMap<String,Object> parameters =null;
        //call getJSON String for root and child tree generation
        String measuresString = soapRetrieval.GetJSONString(this._soap_Address, operationName, null);
        return measuresString;
    }

}
