package mobile.parallelolapprocessing.UI;

import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.Result;

import DataStructure.TreeNode;

/**
 * Created by KheyaliMitra on 3/12/2016.
 */
public class DimensionMesuresTable {

    public String GenerateTableForGoogleDatatable(HashMap<String,HashMap<String,Long>>  ResultSet,
                                HashMap<Integer, TreeNode> DimensionRef,
                                HashMap<Integer,String> MeasuresRef){
        StringBuilder sb = new StringBuilder();

        sb.append(this._generateColumns(ResultSet,MeasuresRef));
        sb.append(this._generateRows(ResultSet,DimensionRef));
        return sb.toString();
    }

    private StringBuilder _generateColumns(HashMap<String,HashMap<String,Long>>  ResultSet,HashMap<Integer,String> MeasuresRef){
        StringBuilder sb = new StringBuilder();

        //Add columns for dimensions
        sb.append("data.addColumn('string','Dimension');\n");

        //Generates columns for measures
        for(Map.Entry entryPairResultSet:ResultSet.entrySet()){
            HashMap<String,Long> measures = (HashMap<String,Long>)entryPairResultSet.getValue();
            for(Map.Entry keyValuePair:measures.entrySet()){
                Integer measuresInt = Integer.parseInt(keyValuePair.getKey().toString());
                sb.append("data.addColumn('number',");

                sb.append("'");
                sb.append(MeasuresRef.get(measuresInt));
                sb.append("'");

                sb.append(");\n");
            }
            return sb;
        }


        return sb;
    }

    private StringBuilder _generateRows(HashMap<String,HashMap<String,Long>>  ResultSet,
                                        HashMap<Integer, TreeNode> DimensionRef){
        StringBuilder sb = new StringBuilder();

        sb.append("data.addRows(");
        sb.append("[");
        for(Map.Entry keyValuePair: ResultSet.entrySet() ){

            sb.append("[");

            sb.append("'");
            //Dimension key combination conversion
            sb.append(this._convertKeyToDimension(keyValuePair.getKey().toString(),DimensionRef));
            sb.append("'");

            sb.append(",");

            sb.append(this._convertKeyToMeasures((HashMap<String,Long>)keyValuePair.getValue()));

            sb.append("],\n");
        }

        sb =  new StringBuilder(sb.substring(0, sb.length() - 2));
        sb.append("]);\n");
        return sb;
    }
    private StringBuilder _convertKeyToDimension(String dimensionCombinationKey,
                                          HashMap<Integer, TreeNode> DimensionRef) {
        StringBuilder sb = new StringBuilder();
        String[] dimensions= dimensionCombinationKey.split("#");
        for(int i=0;i<dimensions.length;i++){
            Integer dimensionInt = Integer.parseInt(dimensions[i]);
            sb.append(DimensionRef.get(dimensionInt).getReference());
            if(i != dimensions.length -1) {
                sb.append("<br />");
            }
        }
        return sb;
    }

    private StringBuilder _convertKeyToMeasures(HashMap<String,Long> measures) {
        StringBuilder sb = new StringBuilder();

        for (Map.Entry keyValuePair : measures.entrySet()) {
            Integer val = 0 ;
            if (keyValuePair.getValue() == null) {
                val = 0;
            }else{
                val = (Integer)keyValuePair.getValue();
            }
            sb.append(val);
        }


        return sb;
    }
}
