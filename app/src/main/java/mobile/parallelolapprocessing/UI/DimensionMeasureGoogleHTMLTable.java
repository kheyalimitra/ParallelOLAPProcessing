package mobile.parallelolapprocessing.UI;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.Result;

import DataStructure.TreeNode;

/**
 * Created by KheyaliMitra on 3/12/2016.
 */
public class DimensionMeasureGoogleHTMLTable implements IDimensionMeasureDisplay {

    /**
     * Get HTML display for google datatable
     * @param ResultSet
     * @param DimensionRef
     * @param MeasuresRef
     * @return
     */
    public String GetDisplay(HashMap<String,HashMap<String,Long>>  ResultSet,
                                HashMap<Integer, TreeNode> DimensionRef,
                                HashMap<Integer,String> MeasuresRef){
        try{
            String text = _generateHTML(ResultSet,DimensionRef,MeasuresRef).toString();
        return text;
        }
        catch(Exception ex){
            String ts = ex.getMessage();
            Log.d("YourTag", ts);
            Log.d("YourTag", ResultSet.toString());
            return ts;
        }

    }

    private StringBuilder _generateHTML(HashMap<String,HashMap<String,Long>>  ResultSet,
                                        HashMap<Integer, TreeNode> DimensionRef,
                                        HashMap<Integer,String> MeasuresRef){

        StringBuilder sb = new StringBuilder("<html><head><title>Measure Dimension Display</title></head><body>\n");
        sb.append("<script type=\"text/javascript\" src=\"https://www.google.com/jsapi?autoload={'modules':[{'name':'visualization','version':'1','packages':['table']}]}\"></script>\n" +
                " <div id=\"table_div\" style=\" width=700px; height: 250px;\"></div>");
        sb.append("<script type=\"text/javascript\">\n");
        sb.append(this._generateCallFunction());
        sb.append(this._generateDrawTableFunction(ResultSet,DimensionRef,MeasuresRef));
        sb.append("</script>\n");
        sb.append("</body></html>");
        return sb;
    }
    private StringBuilder _generateCallFunction(){
        return new StringBuilder("google.setOnLoadCallback(drawTable);\n");
    }
    private StringBuilder _generateDrawTableFunction(HashMap<String,HashMap<String,Long>>  ResultSet,
                                                     HashMap<Integer, TreeNode> DimensionRef,
                                                     HashMap<Integer,String> MeasuresRef){
        StringBuilder sb = new StringBuilder();
        sb.append("function drawTable() {\n" +
                "        var data = new google.visualization.DataTable();");
        sb.append(this._generateColumns(ResultSet, MeasuresRef));
        sb.append(this._generateRows(ResultSet, DimensionRef));
        sb.append("var table = new google.visualization.Table(document.getElementById('table_div'));\n" +
                "\n" +
                "        table.draw(data, {allowHtml: true});\n" +
                "}\n");
        return sb;
    }

    /**
     * Generates columns for for google table
     * @param ResultSet
     * @param MeasuresRef
     * @return
     */
    private StringBuilder _generateColumns(HashMap<String,HashMap<String,Long>>  ResultSet,
                                           HashMap<Integer,String> MeasuresRef){
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

    /**
     * Generates all rows javascript
     * @param ResultSet
     * @param DimensionRef
     * @return
     */
    private StringBuilder _generateRows(HashMap<String,HashMap<String,Long>>  ResultSet,
                                        HashMap<Integer, TreeNode> DimensionRef){
        StringBuilder sb = new StringBuilder();

        sb.append("data.addRows(");
        sb.append("[");
        Boolean isOneRow = false;
        for(Map.Entry keyValuePair: ResultSet.entrySet() ){
            sb.append(this._generateSingleRow(keyValuePair, DimensionRef));
            sb.append(",\n");
            isOneRow = true;
        }

        if(isOneRow){
            sb =  new StringBuilder(sb.substring(0, sb.length() - 2));
        }


        sb.append("]);\n");
        return sb;
    }

    /**
     * Generates single row JSON
     * @param keyValuePair
     * @param DimensionRef
     * @return
     */
    private StringBuilder _generateSingleRow(Map.Entry keyValuePair,HashMap<Integer, TreeNode> DimensionRef){
        StringBuilder sb = new StringBuilder();
        sb.append("[");

        sb.append("'");
        //Dimension key combination conversion
        sb.append(this._convertKeyToDimension(keyValuePair.getKey().toString(),DimensionRef));
        sb.append("'");

        sb.append(",");
        try {
            sb.append(this._convertKeyToMeasures((HashMap<String, Long>) keyValuePair.getValue()));
        }
        catch(Exception ex){
            sb.append(ex.getLocalizedMessage());
        }

        sb.append("]");

        return sb;
    }

    /**
     * Convert dimension combination to dimension readable string
     * @param dimensionCombinationKey
     * @param DimensionRef
     * @return
     */
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

    /**
     * Convert key string measure to string
     * @param measures
     * @return
     */
    private StringBuilder _convertKeyToMeasures(HashMap<String,Long> measures) {

        if(measures.size()  == 0){
            return new StringBuilder("0");
        }

        StringBuilder sb = new StringBuilder();
        int i=0;
        for(Map.Entry keyval:measures.entrySet()){
            Long val = 0L;
            if(keyval.getValue() != null) {
                val = (Long) keyval.getValue();
            }
            sb.append(val+",");
        }

        return new StringBuilder(sb.substring(0,sb.length() - 1));
    }
}
