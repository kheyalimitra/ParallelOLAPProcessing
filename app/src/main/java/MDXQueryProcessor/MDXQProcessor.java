package MDXQueryProcessor;

//import DataRetrieval.CachedCell;
import android.text.TextUtils;

import org.apache.commons.lang.StringUtils;

import DataRetrieval.Cube;
import DataStructure.TreeNode;
//import com.sun.deploy.util.StringUtils;
import DataCaching.*;
//import com.sun.javafx.scene.layout.region.Margins;

import java.util.*;

/**
 * Created by KheyaliMitra on 1/5/2016.
 */
public class MDXQProcessor {
    //this is the cached keys from previous queries
    public  static HashMap<String, Cube> CachedKeys =  new HashMap<>();
    //compressed keys after sort and merge
    private List<String> compressedKeys =  new ArrayList<String>();
    private boolean dataDownloaded; // the data for the current level is downloaded?
    private boolean leafDataReady;  // the data for the leaf level is downloaded?

    /**
     * Generates key combinations for selected and un cached dimension entries for query
     *
     * @param keys selected dimension by user
     *             Example:
     *             1 => List [[TreeNode1],[TreeNode2]]
     *             2 => List [[TreeNode4],[TreeNode7]]
     *             this will  take axis value of each such List of TreeNodes
     *             and find its node counter value to generate all possible combination along all axis
     *
     */
    public List<String> GenerateKeyCombination(HashMap<Integer,List<TreeNode>> keys) {
        List<String> result = new ArrayList<String>();
        //recursively call key generator starting from 0
        result = this._generateKeyCombinations(0, keys, result);
        return result;

    }

    /**
     * Checks user query entry from cache, if found, removes the entry to be fetched from server
     * @param keys
     * @return
     */
    public List<String> checkCachedKeysToRemoveDuplicateEntries(List<String>keys)
    {
        for(int i=0;i<keys.size();i++)
        {
            if(DataCaching.CachedDataCubes.containsKey(keys.get(i)))
            {
                keys.remove(i--);// to reset the index field for iteration
            }
        }
        return  keys;

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

    public List<String> GenerateCellOrdinal(List<List<Integer>> axisList) {
        List<String> Combinations = new ArrayList<>();
        List<String> tempCombinations = new ArrayList<>();

        int nCombinationCount = axisList.size();

        for(int i=0;i<nCombinationCount;i++){
            Combinations.add(axisList.get(i).toString());
        }
        //Combinations = axisList.get(0).stream().map(String::valueOf).collect(Collectors.toList());
        for(int i=1; i<nCombinationCount;i++){
            List<Integer> axis = axisList.get(i);

            int nDimensionCount = axis.size();
            for(int j=0;j<nDimensionCount;j++){

                for(int k=0; k<Combinations.size();k++){
                    tempCombinations.add(Combinations.get(k) +"#"+ axis.get(j).toString());
                }
            }

            Combinations = tempCombinations;
            tempCombinations = new ArrayList<>();
        }
        return Combinations;
    }

    /**
     * Get current query information on each axis
     * First (0) axis contains measures and rest will contain dimensions
     * @param selectedMeasures
     * @param filteredKeys
     * @return
     */
    public List<List<List<Integer>>> GetAxisDetails(List<Integer> selectedMeasures, List<String>filteredKeys) {
        List<String> compressedKeys = _compressFilteredKeys(filteredKeys);
        return this._getAxisWiseDimensionDetails(compressedKeys,selectedMeasures);
    }
        /**
         * Generate query which is used to fetch data from server
         * @param selectedDimension
         * @param filteredKeys
         */
    public void ProcessUserQuery(List<Integer> selectedMeasures,HashMap<Integer,String> measureMap,HashMap<Integer,
            List<TreeNode>> selectedDimension,List<String>filteredKeys) throws  Exception{
        String olapServiceURL="http://192.168.0.207/OLAPService/AdventureWorks.asmx";
        List<List<List<Integer>>> allAxisDetails = this.GetAxisDetails(selectedMeasures,filteredKeys);
        List<List<String>> cellOrdinalCombinations= new ArrayList<>();
        int queryCount =allAxisDetails.size();
        for(int i=0;i<queryCount;i++) {
            cellOrdinalCombinations.add(this.GenerateCellOrdinal(allAxisDetails.get(i)));
        }

        List<String> inflatedQueries;
        List<String> requestedQueries ;

        List<List<String>> finalMDXQueries = new ArrayList<>();
        HashMap<Integer,TreeNode> keyValPairsForDimension = _getKeyValuePairOfSelectedDimensionsFromTree(selectedDimension);
        requestedQueries = _generateQueryString(allAxisDetails,selectedMeasures,measureMap,keyValPairsForDimension,false);
        inflatedQueries = _generateQueryString(allAxisDetails,selectedMeasures,measureMap,keyValPairsForDimension,true);
        finalMDXQueries.add(requestedQueries);
        finalMDXQueries.add(inflatedQueries);
        //below section should be split in to two separate thread
        Cube c =  new Cube(olapServiceURL);
        List<List<Long>>cubeOriginal = c.GetCubeData(finalMDXQueries.get(0).get(0));
        List<List<Long>>cubeInflated = c.GetCubeData(finalMDXQueries.get(1).get(0));
        this._checkAndPopulateCache(cellOrdinalCombinations.get(0),cubeOriginal);// assuming only 1 query entry
        this._checkAndPopulateCache(cellOrdinalCombinations.get(0),cubeInflated);// assuming only 1 query entry

    }
    private String _sortKeyCombination(String key)
    {
            String[]  keysPerEntry = key.split("#");
            List<Integer> sortedKeys = new ArrayList<>();
            for (int j = 0; j < keysPerEntry.length; j++) {
                sortedKeys.add(Integer.parseInt(keysPerEntry[j]));
            }
            Collections.sort(sortedKeys);
            String newKey = sortedKeys.toString();
            newKey = newKey.replaceAll("\\s+", "").replace("[", "").replace("]", "").replace(",", "#");// replace [X,X,X] to X#X#X
           return newKey;

    }
    private List<String> _generateQueryString(List<List<List<Integer>>> queryDetails,List<Integer> selectedMeasures,HashMap<Integer,String> measureMap,
                                             HashMap<Integer,TreeNode> keyValPairsForDimension, boolean isAddDescendant ){
        int nQueryCount =queryDetails.size();
        List<String> subQueries =  new ArrayList<>();
        for(int i=0;i<nQueryCount;i++)
        {
            subQueries.add(this._generateSubQueryString(queryDetails.get(i),selectedMeasures,measureMap,keyValPairsForDimension,isAddDescendant));
        }
        return subQueries;
    }

    private String _generateSubQueryString(List<List<Integer>> queryDetails,List<Integer> selectedMeasures,HashMap<Integer,String> measureMap,
                                                   HashMap<Integer,TreeNode> keyValPairsForDimension, boolean isAddDescendant ){

        String sqlMeasureStatement = this._generateQueryForMeasures(selectedMeasures,measureMap);
        int axisCount =queryDetails.size();
        List<String> axisWiseQuery =  new ArrayList<>();
        axisWiseQuery.add(sqlMeasureStatement);
        for(int i=1;i<axisCount;i++)// assuming axis 0 is always for measures
        {
            List<Integer> dimensionkeyList = queryDetails.get(i);
            List<String> dimensionList =  new ArrayList<>();
            int dimensionCount = dimensionkeyList.size();
            int distance=0;
            TreeNode  node= keyValPairsForDimension.get(dimensionkeyList.get(0));
            for(int j=0;j<dimensionCount;j++)
            {
                node= keyValPairsForDimension.get(dimensionkeyList.get(j));
                String dimensionName = node.getHierarchyName();
                dimensionName =  dimensionName.substring(dimensionName.indexOf(".")+1);// removing [Dimension] part from the string : else it will not execute
                dimensionList.add(dimensionName);

            }
            if(isAddDescendant){
                TreeNode parentNode = node.getParent();// whose parent???
                distance= parentNode.getLevel() - (node.getLevel() - 2);

                 axisWiseQuery.add("DESCENDANTS({" + TextUtils.join(",", dimensionList) + "},"
                         + distance + ",LEAVES) on axis(" + (i) + ") ");

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
    private HashMap<Integer,TreeNode> _getKeyValuePairOfSelectedDimensionsFromTree(HashMap<Integer,List<TreeNode>>dimensionTree)
    {
        HashMap<Integer,TreeNode>keyValPairs =  new HashMap<Integer, TreeNode>();
        for(int i=0;i<dimensionTree.size();i++)
        {
            List<TreeNode>chidren = dimensionTree.get(i);
            //Iterate through JSON Object to generate parent and its child nodes and then finally attach to its root node.
            Iterator nodeIterator = chidren.iterator();
            while (nodeIterator.hasNext()) {
                TreeNode node =(TreeNode) nodeIterator.next();

                keyValPairs.put(node.getNodeCounter(),node);//+"#"+i
            }
        }
        return keyValPairs;
    }
    private List<List<List<Integer>>> _getAxisWiseDimensionDetails(List<String>selectedKeys,List<Integer> selectedMeasures){
        List<List<List<Integer>>> subHierarchies = new ArrayList<>();
        // example of keys: "194,195#201,202"
        // Hierarchy Selection:
        // [Customer].[Country].[All Customers].[Australia] --> 194
        // [Customer].[Country].[All Customers].[Canada] --> 195
        // [Product].[Color].[All Products].[Black] --> 201
        // [Product].[Color].[All Products].[Blue] --> 202
        int nCount = selectedKeys.size();
        for (int i = 0; i < nCount; ++i) {
            //Separate all axises
            String[] axises = selectedKeys.get(i).split("#");
            List<List<Integer>> axisList = new ArrayList<>();
            //Add measures at the beginning of each queries
            axisList.add(selectedMeasures);
            for(int j=0;j<axises.length;j++){
                //Split out all the dimensions for an axis
                String[] dimensions = axises[j].split(",");
                List<Integer> dimensionList = new ArrayList<>();

                for(int k=0;k<dimensions.length;k++){
                    //Add each dimension to axis
                    dimensionList.add(Integer.parseInt(dimensions[k]));
                }
                axisList.add(dimensionList);
            }
            subHierarchies.add(axisList);
        }

        return subHierarchies;
    }
    /**
     * This is the main method to optimise query cells. It compresses all un cached keys using sort and merge method
     * @param keys
     * @return
     */
    private List<String> _compressFilteredKeys(List<String>keys)
    {
        if(keys.size()>1) {
            return _sortAndMergeKeys(keys);
        }
        else
            return keys;

    }

    private String _generateKeys(List<TreeNode>Nodes)
    {
        String key = "";
        for (int i = 0; i < Nodes.size(); ++i) {
            if (key == "") {
                key += Nodes.get(i).getNodeCounter();
            } else {
                key += ("#" + Nodes.get(i).getNodeCounter());
            }
        }
        return key;
    }
    /**
     * Sorts and merges every query cells and optimizes number of entries per axis
     * @param keys
     */
    private List<String> _sortAndMergeKeys(List<String >keys)
    {
        List<String> mergedKeys=new ArrayList<String>();
        int flag=0;
        for(int i=0;i<keys.size()-1;i++)
        {
            String[] keys1 = keys.get(i).split("#");
            String[] keys2 = keys.get(i+1).split("#");

            // validates keys if they are candidate for merging,The two cells can only be merged if only one of their ids differ in the same dimension.
            int index = _validateKeys(keys1, keys2);
            // if not valid
            if (index != -1) {
                // combine keys with ',' separation
                String[] combinedKeys = _combineKeys(keys1, keys2, index);
                // replace old one with new one and remove i+1 th one since it is already merged.
                keys.set(i, StringUtils.join(Arrays.asList(combinedKeys), "#"));
                keys.remove(i+1);// remove the immediate next element which is got merged with ith element
                mergedKeys = keys;
                flag = 1;
            }
        }
        if(flag==1)
        {
            _sortAndMergeKeys(keys);
        }
        return mergedKeys;
    }

    /**
     * combined two entries from selected dimensions with ','
     * @param keys1
     * @param keys2
     * @param index
     * @return
     */
    private String[] _combineKeys(String[] keys1, String[] keys2, int index)
    {
        String[] subKeys1 = keys1[index].split(",");
        String[]  subKeys2 = keys2[index].split(",");
        String[] mergedKeys =new  String[subKeys1.length+subKeys2.length];
        System.arraycopy(subKeys1,0,mergedKeys,0,subKeys1.length);
        System.arraycopy(subKeys2,0,mergedKeys,subKeys1.length,subKeys2.length);
        Arrays.sort(mergedKeys);
        String newKey = StringUtils.join(Arrays.asList(mergedKeys),",");
        keys1[index] = newKey;
        return  keys1;
    }

    /**
     * validates two keys if they can be combined: are candidate for merging,The two cells can only be merged
     * if only one of their ids differ in the same dimension.
     * @param keys1
     * @param keys2
     * @return
     */
    private int _validateKeys(String[] keys1, String[] keys2)
    {
        int count=0;
        int index =-1;
        for(int i=0;i<keys1.length;i++)
        {
            if(!keys1[i].equals(keys2[i])){
                count++;
                if(count==1)// checking for only 1 mismatch
                {
                    index =i;
                }
            }

        }
        //not a candidate for merging, these 2 keys differs in more than 1 way
        if(count>1)
        {
            index=-1;
        }

        return  index;
    }

    /**
     * recursively generates keys for un cached dimension entries for query
     * @param axisIndex
     * @param keys
     * @param result
     * @return
     */
    private List<String> _generateKeyCombinations(int axisIndex, HashMap<Integer, List<TreeNode>> keys, List<String> result) {
        //end condition
        if(axisIndex>=keys.size())
        {
            return result;
        }

        List<String> newResult = new ArrayList<String>();
        // loop through each axis entry
        for(int i=0;i<keys.get(axisIndex).size();i++)
        {
            // if this is the 1st recursion, no saved keys in result:
            if(result.size()==0)
            {
                newResult.add(String.valueOf( keys.get(axisIndex).get(i).getNodeCounter()) );// can not implicitly convert int to String
            }
            else
            {
                //for every save entry in result list , append new ones
                for(int j=0;j<result.size();j++)
                {
                    newResult.add(result.get(j)+"#"+String.valueOf( keys.get(axisIndex).get(i).getNodeCounter()));
                }
            }

        }
        result = newResult;
        return  _generateKeyCombinations(axisIndex+1,keys,result);
    }
    private void _checkAndPopulateCache(List<String>combinations,  List<List<Long>> downloadedCube) {
        int cellMeasuresCount = downloadedCube.size();
        for(int i=0;i<cellMeasuresCount;i++)
        {
            long cellOrdinal = downloadedCube.get(i).get(1);
            String combination = combinations.get((int)cellOrdinal);// cell ordinal :[cellMeasure, cellOrdinal]
            if(combination!=null)
            {
                String dimensionCombination = this._sortKeyCombination(combination.substring(combination.indexOf("#")+1));
                String measure = combination.substring(0,combination.indexOf("#"));

                HashMap<Long, Long> keyVal =  new HashMap<>();
                //update existing cache
                if(DataCaching.CachedDataCubes.containsKey(dimensionCombination)){
                    DataCaching.CachedDataCubes.get(dimensionCombination).put(Long.parseLong(measure),downloadedCube.get(i).get(0));
                }else{
                    keyVal.put(Long.parseLong(measure), downloadedCube.get(i).get(0));
                    DataCaching.CachedDataCubes.put(combination.substring(combination.indexOf("#")+1),keyVal);
                }

            }
        }
    }

}