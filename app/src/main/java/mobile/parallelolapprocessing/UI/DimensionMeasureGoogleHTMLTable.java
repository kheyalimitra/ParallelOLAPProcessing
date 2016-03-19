package mobile.parallelolapprocessing.UI;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javax.xml.transform.Result;

import DataStructure.TreeNode;
import mobile.parallelolapprocessing.Async.ParameterWrapper.MDXUserQueryInput;

/**
 * Created by KheyaliMitra on 3/12/2016.
 */
public class DimensionMeasureGoogleHTMLTable implements IDimensionMeasureDisplay {
     public static Long DataDisplaySize  = 8L;// 8 bytes
    /***
     * Get HTML display for Table
     * @param CacheContent
     * @param UserSelectedKeyCombinations
     * @param UserSeletedMeasures
     * @param DimensionReference
     * @param MeasureReference
     * @return
     */
    public String GetDisplay(WeakHashMap<String, WeakHashMap<Integer, Long>> CacheContent,
                             List<String> UserSelectedKeyCombinations,
                             List<Integer> UserSeletedMeasures,
                             HashMap<Integer, TreeNode> DimensionReference,
                             HashMap<Integer, String> MeasureReference) {
        try{
            // flush previous data size:
            DimensionMeasureGoogleHTMLTable.DataDisplaySize = 8L;
            String text = _generateHTML(CacheContent,UserSelectedKeyCombinations,
                    UserSeletedMeasures,DimensionReference,MeasureReference).toString();
            return text;
        }
        catch(Exception ex){
            String ts = ex.getMessage();
            Log.d("YourTag", ts);
            //Log.d("YourTag", ResultSet.toString());
            return ts;
        }
    }
    private StringBuilder _generateHTML(WeakHashMap<String, WeakHashMap<Integer, Long>> CacheContent,
                                        List<String> UserSelectedKeyCombinations,
                                        List<Integer> UserSeletedMeasures,
                                        HashMap<Integer, TreeNode> DimensionReference,
                                        HashMap<Integer, String> MeasureReference){

        StringBuilder sb = new StringBuilder("<html><head><title>Measure Dimension Display</title></head><body>\n");
        sb.append("<script type=\"text/javascript\" src=\"https://www.google.com/jsapi?autoload={'modules':[{'name':'visualization','version':'1','packages':['table']}]}\"></script>\n" +
                " <div id=\"table_div\" style=\" width=700px; height: 250px;\"></div>");
        sb.append("<script type=\"text/javascript\">\n");
        sb.append(this._generateCallFunction());
        sb.append(this._generateDrawTableFunction(CacheContent,UserSelectedKeyCombinations,
                UserSeletedMeasures,DimensionReference,MeasureReference));
        sb.append("</script>\n");
        sb.append("</body></html>");
        return sb;
    }
    private StringBuilder _generateCallFunction(){
        return new StringBuilder("google.setOnLoadCallback(drawTable);\n");
    }
    private StringBuilder _generateDrawTableFunction(WeakHashMap<String, WeakHashMap<Integer, Long>> CacheContent,
                                                     List<String> UserSelectedKeyCombinations,
                                                     List<Integer> UserSeletedMeasures,
                                                     HashMap<Integer, TreeNode> DimensionReference,
                                                     HashMap<Integer, String> MeasureReference){

        StringBuilder sb = new StringBuilder();
        sb.append("function drawTable() {\n" +
                "        var data = new google.visualization.DataTable();");
        sb.append(this._generateColumns(UserSeletedMeasures, MeasureReference));
        sb.append(this._generateRows(CacheContent,UserSelectedKeyCombinations, UserSeletedMeasures,
                DimensionReference));
        sb.append("var table = new google.visualization.Table(document.getElementById('table_div'));\n" +
                "\n" +
                "        table.draw(data, {allowHtml: true});\n" +
                "}\n");
        return sb;
    }

    /**
     * Generates columns for for google table
     * @param UserSeletedMeasures
     * @param MeasureReferencef
     * @return
     */
    private StringBuilder _generateColumns(List<Integer> UserSeletedMeasures,
                                           HashMap<Integer, String> MeasureReferencef) {
        StringBuilder sb = new StringBuilder();

        //Add columns for dimensions
        sb.append("data.addColumn('string','Dimension');\n");

        //Add columns for all selected measures
        for (Integer measureInt : UserSeletedMeasures) {
            sb.append("data.addColumn('number',");

            sb.append("'");
            sb.append(MeasureReferencef.get(measureInt));
            sb.append("'");

            sb.append(");\n");
        }
        return sb;
    }

    /**
     * Generates all rows for javascript
     * @param CacheContent
     * @param UserSelectedKeyCombinations
     * @param UserSeletedMeasures
     * @param DimensionReference
     * @return
     */
    private StringBuilder _generateRows(WeakHashMap<String, WeakHashMap<Integer, Long>> CacheContent,
                                        List<String> UserSelectedKeyCombinations,
                                        List<Integer> UserSeletedMeasures,
                                        HashMap<Integer, TreeNode> DimensionReference){
        StringBuilder sb = new StringBuilder();

        sb.append("data.addRows(");
        sb.append("[");
        Boolean isOneRow = false;
        for(String keyCombination: UserSelectedKeyCombinations ){
            sb.append(this._generateSingleRow(keyCombination,CacheContent,
                     UserSeletedMeasures,DimensionReference));
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
     * @param CacheContent
     * @param KeyCombination
     * @param UserSeletedMeasures
     * @param DimensionReference
     * @return
     */
    private StringBuilder _generateSingleRow(String KeyCombination,
                                             WeakHashMap<String, WeakHashMap<Integer, Long>> CacheContent,
                                             List<Integer> UserSeletedMeasures,
                                             HashMap<Integer, TreeNode> DimensionReference){
    StringBuilder sb = new StringBuilder();
        sb.append("[");

        sb.append("'");
        //Dimension key combination conversion
        sb.append(this._convertKeyToDimension(KeyCombination,DimensionReference));
        sb.append("'");

        sb.append(",");
        try {
            sb.append(this._getAllMeasureValues(KeyCombination,CacheContent,UserSeletedMeasures));
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
     * Get measure values
     * @param KeyCombination
     * @param CacheContent
     * @param UserSeletedMeasures
     * @return
     */
    private StringBuilder _getAllMeasureValues(String KeyCombination,
                                                WeakHashMap<String, WeakHashMap<Integer, Long>> CacheContent,
                                                List<Integer> UserSeletedMeasures) {

        StringBuilder sb = new StringBuilder();
        int i=0;
        for(Integer measureInt:UserSeletedMeasures){
            WeakHashMap<Integer, Long> keyValPair = CacheContent.get(KeyCombination);
            if(keyValPair!=null) {
                // calculating total size of data which is displayed (approximately)
                DataDisplaySize *= 2;// [cellordinal, value] pair and both are Long
                Long val = keyValPair.get(measureInt);
                if (val != null) {
                    sb.append(val + ",");

                }
            }
        }
        StringBuilder newSB = new StringBuilder();
        if(sb.length()>0) {
            newSB = new StringBuilder(sb.substring(0, sb.length() - 1));
        }
        return newSB;
    }


}
