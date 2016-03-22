package mobile.parallelolapprocessing.UI;

import android.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.WeakHashMap;

import DataStructure.TreeNode;

/**
 * Created by jayma on 3/20/2016.
 */
public class DimensionMeasuresGoogleHTMLBarChart implements IDimensionMeasureDisplay {
    @Override
    public String GetDisplay(WeakHashMap<String, WeakHashMap<Integer, Long>> CacheContent, List<String> UserSelectedKeyCombinations, List<Integer> UserSeletedMeasures, HashMap<Integer, TreeNode> DimensionReference, HashMap<Integer, String> MeasureReference) {
        try{
            String text = _generateHTML(CacheContent,UserSelectedKeyCombinations,
                    UserSeletedMeasures,DimensionReference,MeasureReference).toString();
            return text;
        }
        catch(Exception ex){
            String ts = ex.getMessage();
            return ts;
        }
    }
    private StringBuilder _generateHTML(WeakHashMap<String, WeakHashMap<Integer, Long>> CacheContent,
                                        List<String> UserSelectedKeyCombinations,
                                        List<Integer> UserSeletedMeasures,
                                        HashMap<Integer, TreeNode> DimensionReference,
                                        HashMap<Integer, String> MeasureReference){

        StringBuilder sb = new StringBuilder("<html><head><title>Measure Dimension Display:Bar</title></head><body>\n");

        sb.append("<script type=\"text/javascript\" src=\"https://www.gstatic.com/charts/loader.js\"></script>\n" +
                " \n" +
                " <div id=\"chart_div\" style=\" width=1000px; height: 400px;\">\n" +
                "</div><script type=\"text/javascript\">\n");
        sb.append("google.charts.load('current', {'packages':['bar']});\n" +
                "google.charts.setOnLoadCallback(drawChart);");
        sb.append(this._generateDrawBarChartFunction(CacheContent, UserSelectedKeyCombinations,
                UserSeletedMeasures,DimensionReference,MeasureReference));
        sb.append("</script>\n");
        sb.append("</body></html>");
        return sb;
    }

    private StringBuilder _generateDrawBarChartFunction(WeakHashMap<String, WeakHashMap<Integer, Long>> CacheContent,
                                                     List<String> UserSelectedKeyCombinations,
                                                     List<Integer> UserSeletedMeasures,
                                                     HashMap<Integer, TreeNode> DimensionReference,
                                                     HashMap<Integer, String> MeasureReference){

        StringBuilder sb = new StringBuilder();
        sb.append("function drawChart() {\n" +
                "  var data = new  google.visualization.arrayToDataTable(");
        sb.append("[");
        sb.append(this._generateHeader(UserSeletedMeasures, MeasureReference));
        sb.append(",");
        sb.append(this._generateRows(CacheContent, UserSelectedKeyCombinations, UserSeletedMeasures,
                DimensionReference));
        sb.append(" var options = {\n" +
                "          chart: {\n" +
                "            },\n" +
                "          bars: 'vertical',\n" +
                "          vAxis: {format: 'decimal'},\n" +
                "          height: 300,\n" +
                "        };");
        sb.append(" var chart = new google.charts.Bar(document.getElementById('chart_div'));\n" +
                "\n" +
                "chart.draw(data, google.charts.Bar.convertOptions(options));\n" +
                "}\n");
        return sb;
    }

    /**
     * Generates columns for for google table
     * @param UserSeletedMeasures
     * @param MeasureReferencef
     * @return
     */
    private StringBuilder _generateHeader(List<Integer> UserSeletedMeasures,
                                           HashMap<Integer, String> MeasureReferencef) {
        StringBuilder sb = new StringBuilder();
        sb.append("['Measures',");
        //Add columns for all selected measures
        for (Integer measureInt : UserSeletedMeasures) {
            sb.append("'");
            sb.append(MeasureReferencef.get(measureInt));
            sb.append("',");
        }
        sb.delete(sb.lastIndexOf(","),sb.length());
        sb.append("]\n");
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
        Boolean isOneRow = false;
        for(String keyCombination: UserSelectedKeyCombinations ){
            sb.append(this._generateSingleRow(keyCombination,CacheContent,
                    UserSeletedMeasures,DimensionReference));
            sb.append(",\n");
            isOneRow = true;
        }

        if(isOneRow){
            sb.delete(sb.lastIndexOf(","),sb.length());
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
        sb.append("',");

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
                sb.append("\\n");
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
                Long val = keyValPair.get(measureInt);
                if (val != null) {
                    sb.append(val + ",");

                }else{
                    sb.append("-1" + ",");
                }
            }else{
                sb.append("-2" + ",");
            }
        }
        StringBuilder newSB = new StringBuilder();
        if(sb.length()>0) {
            newSB = new StringBuilder(sb.substring(0, sb.length() - 1));
        }
        return newSB;
    }

}
