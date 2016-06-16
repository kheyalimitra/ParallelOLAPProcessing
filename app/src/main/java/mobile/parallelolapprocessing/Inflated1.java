package mobile.parallelolapprocessing;

import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import DataRetrieval.Cube;
import DataStructure.TreeNode;
import MDXQueryProcessor.MDXQProcessor;
import mobile.parallelolapprocessing.Async.CacheProcessUpto1Level;

/**
 * Created by KheyaliMitra on 6/16/2016.
 */
public class Inflated1 implements Runnable{

    public List<List<List<Integer>>> allAxisDetails;
    public List<Integer> selectedMeasures;
    public HashMap<Integer, String> measureMap;
    public HashMap<Integer, TreeNode> keyValPairsForDimension;
    public List<List<String>>cellOrdinalCombinations;
    public String olapServerURL;
    public List<Boolean> isAddChildrenToDimension;
    public static List<String> inflatedQueries;
    private long start=0;
    List<List<HashMap<Integer, TreeNode>>> allLeaves;
    List<List<TreeNode>> parentEntiresPerAxis;
    public Inflated1(List<List<List<Integer>>> allAxisDetails, List<Integer> selectedMeasures, HashMap<Integer, String> measureMap,
                        HashMap<Integer, TreeNode> keyValPairsForDimension, List<List<String>> cellOrdinalCombinations, String olapURL, List<List<HashMap<Integer, TreeNode>>> allLeaves,List<List<TreeNode>> parentEntiresPerAxis)
    {
        this.allAxisDetails = allAxisDetails;
        this.selectedMeasures =selectedMeasures;
        this.measureMap = measureMap;
        this.keyValPairsForDimension = keyValPairsForDimension;
        this.cellOrdinalCombinations = cellOrdinalCombinations;
        this.olapServerURL = olapURL;
        this.allLeaves = allLeaves;
        this.parentEntiresPerAxis = parentEntiresPerAxis;
        isAddChildrenToDimension = new ArrayList<>();
        instatiateOtherThreads();
    }
    public void instatiateOtherThreads(){
        if(CacheProcess.inflatedQueries == null)
        {
            CacheProcess.inflatedQueries = new ArrayList<>();
        }
        if(CacheProcessUpto1Level.inflatedQueries == null)
        {
            CacheProcessUpto1Level.inflatedQueries = new ArrayList<>();
        }
        inflatedQueries = new ArrayList<>();
        //isAddChildrenToDimension = new ArrayList<>();
    }
    public List<List<List<Integer>>> generateNewAxisDetails(List<List<HashMap<Integer, TreeNode>>> allLeaves, List<List<List<Integer>>> allAxisDetails) {
        List<List<List<Integer>>> newAxisDetails= new ArrayList<>();
        List<List<Integer>> newAxis= new ArrayList<>();

        //Add measures to the first axis
        newAxis.add(allAxisDetails.get(0).get(0));
        int dimenAxis =1;
        int leavesSize = allLeaves.size();
        if(leavesSize>1) {
            int halfSize = (int)Math.floor(leavesSize/2);
            for (int i = 0; i <halfSize; i++) {
                //Individual axis
                List<HashMap<Integer, TreeNode>> axis = allLeaves.get(i);
                List<Integer> dimensionList = new ArrayList<>();
                for (int j = 0; j < axis.size(); j++) {
                    dimensionList.addAll(axis.get(j).keySet());
                }
                dimensionList.addAll(allAxisDetails.get(0).get(dimenAxis++));

                newAxis.add(dimensionList);
                isAddChildrenToDimension.add(true);
            }
            for (int i = halfSize; i <leavesSize; i++) {

                List<Integer> dimensionList = new ArrayList<>();
                dimensionList.addAll(allAxisDetails.get(0).get(dimenAxis++));
                //dimensionList.addAll(allAxisDetails.get(0).get(dimenAxis++));
                newAxis.add(dimensionList);
                isAddChildrenToDimension.add(false);
            }
        }
        else{
            for (int i = 0; i <leavesSize; i++) {

                List<Integer> dimensionList = new ArrayList<>();
                dimensionList.addAll(allAxisDetails.get(0).get(dimenAxis++));
                //dimensionList.addAll(allAxisDetails.get(0).get(dimenAxis++));
                newAxis.add(dimensionList);
                isAddChildrenToDimension.add(false);
            }
        }

        newAxisDetails.add(newAxis);



        return newAxisDetails;
    }


    public void run() {
        try {
            start = System.currentTimeMillis();

            Cube c = new Cube(olapServerURL);
            MDXQProcessor mdxQ = new MDXQProcessor();

            //List<List<HashMap<Integer, TreeNode>>> allLeaves = getLeavesPerAxis(keyValPairsForDimension, allAxisDetails.get(0));
            List<List<List<Integer>>> newAxisDetails = generateNewAxisDetails(allLeaves, allAxisDetails);
            List<List<String>> cellOrdinalCombinations = new ArrayList<>();
            int queryCount = allAxisDetails.size();
            for (int i = 0; i < queryCount; i++) {
                cellOrdinalCombinations.add(mdxQ.GenerateCellOrdinal(newAxisDetails.get(i)));
            }

            List<String>  queryListForChildren  = GenerateQueryString(allAxisDetails, selectedMeasures, measureMap,
                    keyValPairsForDimension, true,false);
            if(!CacheProcessUpto1Level.inflatedQueries.contains(queryListForChildren.get(0)) && !inflatedQueries.contains(queryListForChildren.get(0)))
            {
                Log.d("Inflated Query1", "Start data download  " + String.valueOf(System.currentTimeMillis()));
                inflatedQueries.add(queryListForChildren.get(0));
                Log.d("Inflated Query1", "MDX query: " + String.valueOf(queryListForChildren.get(0)));
                List<List<Long>> cubeInflated = c.GetCubeData(queryListForChildren.get(0));
                Log.d("Inflated Query1", "MDX query down load ends: " + String.valueOf(System.currentTimeMillis()));
                mdxQ.CheckAndPopulateCache(cellOrdinalCombinations.get(0), this.parentEntiresPerAxis, cubeInflated, false);// assuming only 1 query entry
                Log.d("Inflated Query1", "process ends " + String.valueOf(System.currentTimeMillis()));
            }
        }
        catch(Exception e) {
            String ex = e.getMessage();
        }
    }
    public List<String> GenerateQueryString(List<List<List<Integer>>> queryDetails,List<Integer> selectedMeasures,HashMap<Integer,String> measureMap,
                                            HashMap<Integer,TreeNode> keyValPairsForDimension, boolean isAddDescendant,boolean isAddInflatedSiblings ){
        int nQueryCount =queryDetails.size();
        List<String> subQueries =  new ArrayList<>();
        for(int i=0;i<nQueryCount;i++)
        {
            String querystring = this._generateSubQueryString(queryDetails.get(i), selectedMeasures, measureMap, keyValPairsForDimension, isAddDescendant, isAddInflatedSiblings);
            subQueries.add(querystring);

        }
         return subQueries;
    }

    private String _generateSubQueryString(List<List<Integer>> queryDetails,List<Integer> selectedMeasures,HashMap<Integer,String> measureMap,
                                           HashMap<Integer,TreeNode> keyValPairsForDimension, boolean isAddDescendant,boolean isFindSiblings ){

        String sqlMeasureStatement = this._generateQueryForMeasures(selectedMeasures, measureMap);
        int axisCount =queryDetails.size();
        List<String> axisWiseQuery =  new ArrayList<>();
        axisWiseQuery.add(sqlMeasureStatement);
        for(int i=1;i<axisCount;i++)// assuming axis 0 is always for measures
        {
            List<Integer> dimensionkeyList = queryDetails.get(i);
            List<String> dimensionList =  new ArrayList<>();
            int dimensionCount = dimensionkeyList.size();
            List<String> originalDimensions = new ArrayList<>();
            TreeNode  node;
            for(int j=0;j<dimensionCount;j++)
            {
                node= keyValPairsForDimension.get(dimensionkeyList.get(j));
                if(isAddDescendant) {
                    // this one is for all dimensions which are currently pointed as leaf node, if that is so, go 1 level up and fetch records for all leaves of same parents
                    originalDimensions.add(node.getHierarchyName().substring(node.getHierarchyName().indexOf(".")+1));
                    if((node.getChildren().size() == 0)|| (isFindSiblings)) {
                        node = node.getParent();
                    }

                }
                String dimensionName = node.getHierarchyName();
                dimensionName =  dimensionName.substring(dimensionName.indexOf(".")+1);// removing [Dimension] part from the string : else it will not execute

                if (isAddChildrenToDimension.get(i-1)) {// since it is not capturing measures details in this list
                    dimensionName += ".children";
                }

                dimensionList.add(dimensionName);

            }

            if(isAddChildrenToDimension.get(i-1) && !isFindSiblings){
                // adding original combinations along with its leaves so that it has both  Education X2005 and EducationX2005.H2 EducationX2005.H3

                    for(int j=0;j<originalDimensions.size();j++){
                        dimensionList.add(originalDimensions.get(j));
                    }

                axisWiseQuery.add("{" + TextUtils.join(",", dimensionList) + "} on axis(" + (i) + ") ");//+ distance +


            }
            else {
                axisWiseQuery.add("{" + TextUtils.join(",", dimensionList) + "} on axis(" + (i) + ") ");
            }
        }
        String sqlStatement = this._generateSubQuery(axisWiseQuery);
        return sqlStatement;
    }

    // generating sub-query
    private String _generateSubQuery(List<String> hierarchies) {
        String query = "select {";
        query += TextUtils.join(",", hierarchies);
        query += "from [Adventure Works]";
        query.replace("&", "ampersand");
        return query;
    }
    private String _generateQueryForMeasures(List<Integer>measures,HashMap<Integer,String> measureMap){
        String sqlStatement = "";
        List<String> members=new ArrayList<String>();

        for (int i = 0; i < measures.size(); i++) {
            String val = measureMap.get(measures.get(i));
            //Maintaining MDX query structure
            members.add("[Measures].[" + val + "]");
        }
        sqlStatement += TextUtils.join(",",members) + "} on axis(0) ";// String.join does not work in android studio
        return  sqlStatement;
    }

}