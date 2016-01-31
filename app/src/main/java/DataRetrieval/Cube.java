package DataRetrieval;

import DataStructure.TreeNode;
import org.json.simple.parser.JSONParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Created by KheyaliMitra on 1/9/2016.
 */
public class Cube{
    int totalDimensions;
    int totalMeasures;
    int cubeIndex;
    int index;
    List<List<Integer>> data= new ArrayList<>();
    List<Integer[]> celMeasure;
    List<Integer[]> measures = new ArrayList<>();
    HashMap<Integer,List<TreeNode>> children =  new HashMap<>();

    private String _olapServiceURL = "http://192.168.0.207/OLAPService/AdventureWorks.asmx";
    private String _operationName = "subCubeJSON";
    private String _paramName = "input";


    /**
     * Get Cube object
     * @param OlapServiceURL
     */
    public Cube(String OlapServiceURL){
        this._olapServiceURL = OlapServiceURL;
    }
    /**
     * fetch query data from "SubCubeJSON" web method

     * @param operationName
     * @param paramName
     * @param paramValue
     * @return
     * @throws Exception
     */
    public List<List<Long>> GetCubeData(String operationName,String paramName, String paramValue) throws Exception
    {
        Soap sp =  new Soap();
        HashMap<String,Object> param =  new HashMap<>();
        Object obj = paramValue;
        param.put(paramName,obj);
        String result = sp.GetJSONString(this._olapServiceURL,operationName,param);
        //create JSON parser object
        JSONParser parser = new JSONParser();
        //parse dimension string returned SOAP
        Object jsonObject;
        List<List<Long>> dataCube= new ArrayList<>();// should be double
        if(result!=null) {
            result = result.substring(result.indexOf(":")+1,result.indexOf("}"));// to remove 1769={"celMeasure": part and "}" part
            jsonObject = parser.parse(result);
            dataCube =_generateResultListFromJSONObject(jsonObject);
        }
        return   dataCube;
    }

    /**
     * Get Subcube values assuming method name is "subCubeJSON" and param name as "input"
     * @param paramValue
     * @return
     * @throws Exception
     */
    public List<List<Long>> GetCubeData(String paramValue) throws Exception{
        return this.GetCubeData(this._operationName,this._paramName,paramValue);
    }

    /**
     * iterates through json objecct and populate list of lists
     * @param jsonObject
     * @return
     */
    private List<List<Long>> _generateResultListFromJSONObject(Object jsonObject) {
        List<List<Long>> datacube =  new ArrayList<>();
        List<List<Long>> cube = (List<List<Long>>)jsonObject;
        //Iterate through JSON Object to generate list of data from json object.
        Iterator itr = cube.iterator();
        while (itr.hasNext()) {
            //Takes list entries using iterator
            List<Long> subList = (List<Long>)itr.next();
            // adds every sub list to its main list
            datacube.add(subList);
            itr.remove();
        }
        return datacube;
    }
}



