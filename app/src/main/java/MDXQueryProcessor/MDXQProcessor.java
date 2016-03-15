package MDXQueryProcessor;

import android.text.TextUtils;

import org.apache.commons.lang.StringUtils;
import DataRetrieval.Cube;
import DataStructure.TreeNode;
import Processor.QueryProcessor;
import mobile.parallelolapprocessing.Async.Call.MDXUserQuery;
import mobile.parallelolapprocessing.MainActivity;

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
    public HashMap<String,List<String>> checkCachedKeysToRemoveDuplicateEntries(List<String>keys,Set<Integer> selectedMeasures )
    {
        List<Integer> selectedMeasuresKeys = new ArrayList<>(selectedMeasures);
        HashMap<String, List<String>> finalDimensionMeasures =  new HashMap<>();
        Set<String> finalMeasures =  new HashSet<>();
        Set<String> finalDimensions =  new HashSet<>();
        if(MainActivity.CachedDataCubes.size()>0) {
            for (int i = 0; i < keys.size(); i++) {
                if (MainActivity.CachedDataCubes.containsKey(keys.get(i))) {
                    HashMap<Long, Long> measuresSpecificList = MainActivity.CachedDataCubes.get(keys.get(i));
                    for (int j = 0; j < selectedMeasuresKeys.size(); j++) {
                        String measureKey = selectedMeasuresKeys.get(j).toString();
                        if (!measuresSpecificList.containsKey(Long.parseLong(measureKey))) {
                            finalMeasures.add(measureKey);
                            finalDimensions.add(keys.get(i));
                        }
                    }

                } else {
                    for (int j = 0; j < selectedMeasuresKeys.size(); j++) {
                        String measureKey = selectedMeasuresKeys.get(j).toString();
                        finalMeasures.add(measureKey);
                    }
                    finalDimensions.add(keys.get(i));
                }

            }
            finalDimensionMeasures.put("0",new ArrayList<>(finalDimensions));
            finalDimensionMeasures.put("1",new ArrayList<>(finalMeasures));

        }
        else{
            finalDimensionMeasures.put("0",keys);
            List<String> allSelectedMeasures =  new ArrayList<>();
            for(Integer item: selectedMeasuresKeys){
                allSelectedMeasures.add(item.toString());
            }
            finalDimensionMeasures.put("1",allSelectedMeasures);

        }
        return  finalDimensionMeasures;

    }

    private void _drillDownCacheToSearchDimensionEntry(List<String>keys){
        
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
       // Combinations = axisList.get(0).stream().map(String::valueOf).collect(Collectors.toList());
        for(int i=0;i<axisList.get(0).size();i++)
        {
            Combinations.add(axisList.get(0).get(i).toString());
        }
        int nCombinationCount = axisList.size();
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
    public HashMap<String,HashMap<String,Long>> ProcessUserQuery(List<Integer> selectedMeasures,HashMap<Integer,String> measureMap,HashMap<Integer,
            List<TreeNode>> selectedDimension,List<String>filteredKeys,boolean isInnerThreadCalled,boolean isUserThread) throws  Exception{

        //String olapServiceURL="http://192.168.0.207/OLAPService/AdventureWorks.asmx";
        // The following function actually calls copresskey method too to use sort and merge
        // algorithm
        List<List<List<Integer>>> allAxisDetails = this.GetAxisDetails(selectedMeasures,filteredKeys);
        List<List<String>> cellOrdinalCombinations= new ArrayList<>();
        int queryCount =allAxisDetails.size();
        for(int i=0;i<queryCount;i++) {
            cellOrdinalCombinations.add(this.GenerateCellOrdinal(allAxisDetails.get(i)));
        }

        List<String> requestedQueries ;

        List<List<String>> finalMDXQueries = new ArrayList<>();
        HashMap<Integer,TreeNode> keyValPairsForDimension = GetKeyValuePairOfSelectedDimensionsFromTree(selectedDimension);
        requestedQueries = GenerateQueryString(allAxisDetails, selectedMeasures, measureMap, keyValPairsForDimension, false,false);
        finalMDXQueries.add(requestedQueries);
        Cube c =  new Cube(QueryProcessor.olapServiceURL);
        //only for testing:
        //String queries = "select {[Measures].[Internet Sales Amount]} on axis(0), DESCENDANTS({[Geography].[Geography].[All Geographies].[Australia].[New South Wales]},2,LEAVES) on axis(1) ,DESCENDANTS({[Date].[Calendar].[All Periods].[CY 2006].[H1 CY 2006].[Q1 CY 2006]},2,LEAVES) on axis(2) from [Adventure Works]";
        //"select {[Measures].[Internet Sales Amount]} on axis(0), DESCENDANTS({[Customer].[Education].[All Customers]},1,LEAVES) on axis(1) ,DESCENDANTS({[Date].[Calendar].[All Periods].[CY 2008]},4,LEAVES) on axis(2) from [Adventure Works]";
        List<List<Long>>cubeOriginal = c.GetCubeData(finalMDXQueries.get(0).get(0));

        HashMap<String,HashMap<String,Long>> resultSet =  this.CheckAndPopulateCache(cellOrdinalCombinations.get(0),new ArrayList<List<TreeNode>>(), cubeOriginal,isUserThread);// assuming only 1 query entry

      // start this once the previous is done
        MDXUserQuery.allAxisDetails = allAxisDetails;
        MDXUserQuery.selectedMeasures = selectedMeasures;
        MDXUserQuery.measureMap = measureMap;
        MDXUserQuery.keyValPairsForDimension = keyValPairsForDimension;
        MDXUserQuery.cellOrdinalCombinations = cellOrdinalCombinations;

        return resultSet;
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
    public List<String> GenerateQueryString(List<List<List<Integer>>> queryDetails,List<Integer> selectedMeasures,HashMap<Integer,String> measureMap,
                                             HashMap<Integer,TreeNode> keyValPairsForDimension, boolean isAddDescendant,boolean isAddInflatedSiblings ){
        int nQueryCount =queryDetails.size();
        List<String> subQueries =  new ArrayList<>();
        for(int i=0;i<nQueryCount;i++)
        {
            String querystring = this._generateSubQueryString(queryDetails.get(i), selectedMeasures, measureMap, keyValPairsForDimension, isAddDescendant, isAddInflatedSiblings);
            subQueries.add(querystring);

        }
       // subQueries.add("select {[Measures].[Internet Sales Amount]} on axis(0), DESCENDANTS({[Employee].[Employees].[All Employees]},5,LEAVES) on axis(1) ,DESCENDANTS({[Geography].[Geography].[All Geographies]},4,LEAVES) on axis(2) from [Adventure Works]");
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
            TreeNode  node= keyValPairsForDimension.get(dimensionkeyList.get(0));
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

                 if (isAddDescendant)
                    dimensionName += ".children";
                 //if(!dimensionList.contains(dimensionName)) {
                 dimensionList.add(dimensionName);
               // }
            }

            if(isAddDescendant && !isFindSiblings){
                // adding original combinations along with its leaves so that it has both  Education X2005 and EducationX2005.H2 EducationX2005.H3
                for(int j=0;j<originalDimensions.size();j++){
                    dimensionList.add(originalDimensions.get(j));
                }
                //TreeNode parentNode = node.getParent();// whose parent???
                //distance= parentNode.getLevel() - (node.getLevel() - 2);
                axisWiseQuery.add("{" + TextUtils.join(",", dimensionList) + "} on axis(" + (i) + ") ");//+ distance +

                //axisWiseQuery.add("DESCENDANTS({" + TextUtils.join(",", dimensionList) + "},"
                  //      +"1,) on axis(" + (i) + ") ");//+ distance +
                      //   +",LEAVES) on axis(" + (i) + ") ");//+ distance +

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
    public HashMap<Integer,TreeNode> GetKeyValuePairOfSelectedDimensionsFromTree(HashMap<Integer,List<TreeNode>>dimensionTree)
    {
        HashMap<Integer,TreeNode>keyValPairs =  new HashMap<>();
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
    /**
     * Sorts and merges every query cells and optimizes number of entries per axis
     * @param keys
     */
    private List<String> _sortAndMergeKeys(List<String >keys)
    {
        List<String> mergedKeys=new ArrayList<>();
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
    public HashMap<String,HashMap<String,Long>> CheckAndPopulateCache(List<String> combinations, List<List<TreeNode>> parentEntiresPerAxis, List<List<Long>> downloadedCube, boolean isUserQuery) {
        HashMap<String,HashMap<String,Long>> resultSet =  new HashMap<>();
        HashMap<String,Long> totalSum=new HashMap<>();
        // no possible result
        long dummy=0;
        int cellMeasuresCount = downloadedCube.size();
        try {

            for (int i = 0; i < cellMeasuresCount; i++) {

                long cellOrdinal = downloadedCube.get(i).get(1);//1 cell ordinal value : 0 result
                if((int) cellOrdinal< combinations.size()) {
                    String combination = combinations.get((int) cellOrdinal);// cell ordinal :[cellMeasure, cellOrdinal]
                    if (combination != null) {
                        // remove from the combination list. this will help to check if evry combination get covered  in next section
                        combinations.remove((int) cellOrdinal);
                        String dimensionCombination = this._sortKeyCombination(combination.substring(combination.indexOf("#") + 1));
                        String measure = combination.substring(0, combination.indexOf("#"));

                        HashMap<Long, Long> keyVal = new HashMap<>();

                        //update existing cache
                        String[] measureList = measure.replace("[", "").replace("]", "").split(",");
                        HashMap<String,Long> measureWiseResult = _updateMainCache(downloadedCube, totalSum, i, dimensionCombination, keyVal, measureList);
                        resultSet.put(dimensionCombination, measureWiseResult);
                    }
                }

            }
            if( combinations.size()>0){

                for( int i=0;i<combinations.size();i++){
                    String dimensionCombination = this._sortKeyCombination(combinations.get(i).substring(combinations.get(i).indexOf("#") + 1));
                    String measure = combinations.get(i).substring(0, combinations.get(i).indexOf("#"));

                    HashMap<Long, Long> keyVal = new HashMap<>();

                    //update existing cache
                    String[] measureList = measure.replace("[", "").replace("]", "").split(",");

                    HashMap<String,Long> dummyRecords = _updateMainCache(null, totalSum, i, dimensionCombination, keyVal, measureList);
                    if(isUserQuery) {
                       resultSet.put(dimensionCombination,dummyRecords);
                       }
                }
            }
           /* if(parentEntiresPerAxis !=null && parentEntiresPerAxis.size()>0) {
                _addEntryforInflatedParentNodes(totalSum, parentEntiresPerAxis);
            }*/
        }
        catch (Exception ex)
        {
            return null;
        }
        return  resultSet;

    }

    private HashMap<String,Long>  _updateMainCache(List<List<Long>> downloadedCube, HashMap<String, Long> totalSum, int i, String dimensionCombination, HashMap<Long, Long> keyVal, String[] measureList) {
        HashMap<String,Long> measureWiseResult = new HashMap<>();
        long dummy = 0;
        if (MainActivity.CachedDataCubes.containsKey(dimensionCombination)){//MainActivity.CachedDataCubes.containsKey(dimensionCombination)) {
            // measure list per axis
            for (int j = 0; j < measureList.length; j++) {
                // for roll up operation: summing all leaf node values for its parent
                if(totalSum.containsKey(measureList[j])){
                    long sum = totalSum.get(measureList[j]);
                    if(downloadedCube!=null) {
                        sum += downloadedCube.get(i).get(0);
                        totalSum.put(measureList[j], sum);
                    }
                }
                else{
                    if(downloadedCube!=null) {
                        totalSum.put(measureList[j], downloadedCube.get(i).get(0));
                    }
                }
                if(downloadedCube!=null) {
                    MainActivity.CachedDataCubes.get(dimensionCombination).put(Long.parseLong(measureList[j].trim()), downloadedCube.get(i).get(0));
                }

            }
        } else {
            // measure list per axis
            for (int j = 0; j < measureList.length; j++) {

                // for roll up operation: summing all leaf node values for its parent
                if(totalSum.containsKey(measureList[j])){
                    long sum = totalSum.get(measureList[j]);
                    if(downloadedCube!=null) {
                        sum += downloadedCube.get(i).get(0);
                        totalSum.put(measureList[j], sum);
                    }
                }
                else{
                    if(downloadedCube!=null)
                     totalSum.put(measureList[j], downloadedCube.get(i).get(0));
                }
                if(downloadedCube!=null) {
                    keyVal.put(Long.parseLong(measureList[j].trim()), downloadedCube.get(i).get(0));
                    measureWiseResult.put(measureList[j].trim(), downloadedCube.get(i).get(0));
                }
                else {
                    measureWiseResult.put(measureList[j].trim(), dummy);
                    keyVal.put(Long.parseLong(measureList[j].trim()), dummy);

                }

            }

            MainActivity.CachedDataCubes.put(dimensionCombination,keyVal);
                   }
        return  measureWiseResult;
    }

    private void _addEntryforInflatedParentNodes(HashMap<String,Long> totalSum, List<List<TreeNode>> parentEntiresPerAxis) {
        List<String> cellOrdinalCombinations = new ArrayList<>();
        List<List<Integer>> axisDetails = new ArrayList<>();
        List<String> keySetMeasures =  new ArrayList<>();
        keySetMeasures.addAll(totalSum.keySet());
        List<Integer>measures =  new ArrayList<>();
        for(int i=0;i<keySetMeasures.size();i++)
        {
            int val =  Integer.parseInt(keySetMeasures.get(i));
            measures.add(val);
        }
        axisDetails.add(0,measures);
        for(int i=0;i<parentEntiresPerAxis.size();i++){
            List<Integer>keyVal = new ArrayList<>();
            for(int j=0;j<parentEntiresPerAxis.get(i).size();j++)
            {
                keyVal.add(parentEntiresPerAxis.get(i).get(j).getNodeCounter());
            }
            axisDetails.add(keyVal);

        }
        // considering only 1 query  in the list
        cellOrdinalCombinations = GenerateCellOrdinal(axisDetails);

        for( int i=0;i<cellOrdinalCombinations.size();i++){
            String keycombination = cellOrdinalCombinations.get(i);
            String domainKey = keycombination.substring(keycombination.indexOf("#")+1);
            if(MainActivity.CachedDataCubes.containsKey(domainKey)){
                MainActivity.CachedDataCubes.get(domainKey).put(Long.parseLong(measures.get(i).toString()), totalSum.get(measures.get(i).toString()));

            }
            else {
                HashMap<Long, Long> keyVal = new HashMap<>();
                keyVal.put(Long.parseLong(measures.get(i).toString()),totalSum.get(measures.get(i).toString()));
                MainActivity.CachedDataCubes.put(domainKey,keyVal);
            }
        }
    }


}
