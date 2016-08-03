package MDXQueryProcessor;

import android.text.TextUtils;
import android.util.Log;

import org.apache.commons.lang.StringUtils;
import DataRetrieval.Cube;
import DataStructure.TreeNode;
import Processor.QueryProcessor;
import mobile.parallelolapprocessing.Async.CacheProcessUpto1Level;
import mobile.parallelolapprocessing.Async.Call.MDXUserQuery;
import mobile.parallelolapprocessing.Inflated1;
import mobile.parallelolapprocessing.Inflated2;
import mobile.parallelolapprocessing.Inflated3;
import mobile.parallelolapprocessing.MainActivity;
import mobile.parallelolapprocessing.OriginaQuery;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by KheyaliMitra on 1/5/2016.
 */
public class MDXQProcessor {
    //this is the cached keys from previous queries
    public  static HashMap<String, Cube> CachedKeys =  new HashMap<>();
    List<List<TreeNode>> parentEntiresPerAxis;

    // this list keeps tract of last 10 user selection for dimensions, it will store the root dimension key
    // say Geography , Customer etc so that we can keep tract of user trac ( under which dimension he/she is)
    // will be used when flushing cache
    public static Set<Integer> lastTenSelectedDimensions = new HashSet<>();
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
     * Populates measures key need to be fetched from server for one key combination
     * @param KeyCombination
     * @param MeasureRequestedByUser
     * @return
     */
    private List<Integer> _getNonExistentMeasuresFromCacheByKey(String KeyCombination,List<Integer> MeasureRequestedByUser){

        List<Integer> MeasuresNeedsToFecth = new ArrayList<>();

        HashMap<Integer,Long> measuresForCachedKeyCombination = MainActivity.CachedDataCubes.get(KeyCombination);
        if(measuresForCachedKeyCombination == null) {
            return MeasureRequestedByUser;
        }
        else{
        Iterator<Integer> measuresToCheckIterator =  MeasureRequestedByUser.iterator();
            while(measuresToCheckIterator.hasNext()){
                Integer userMeasureKey = measuresToCheckIterator.next();
                if(!measuresForCachedKeyCombination.containsKey(userMeasureKey)){
                    MeasuresNeedsToFecth.add(userMeasureKey);
                }
            }
        }
        return MeasuresNeedsToFecth;
    }

    private boolean _isMeasuresNonExistentInCacheForAKey(String KeyCombination,List<Integer> MeasureRequestedByUser) {
        HashMap<Integer, Long> measuresForCachedKeyCombination = MainActivity.CachedDataCubes.get(KeyCombination);
        if (measuresForCachedKeyCombination == null) {
            return true;
        }
        else {
            Iterator<Integer> measuresToCheckIterator = MeasureRequestedByUser.iterator();
            while (measuresToCheckIterator.hasNext()) {
                Integer userMeasureKey = measuresToCheckIterator.next();
                if (!measuresForCachedKeyCombination.containsKey(userMeasureKey)) {
                    return true;
                }

            }
            return false;
        }
    }
    public void ProcessUserSelectionWhen100Hit(List<Integer> selectedMeasures,HashMap<Integer,String> measureMap,HashMap<Integer,
            List<TreeNode>> selectedDimension,List<String>filteredKeys) throws  Exception{
        List<List<List<Integer>>> allAxisDetails = this.GetAxisDetails(selectedMeasures, filteredKeys);
        List<List<String>> cellOrdinalCombinations= new ArrayList<>();
        int queryCount =allAxisDetails.size();
        for(int i=0;i<queryCount;i++) {
            cellOrdinalCombinations.add(this.GenerateCellOrdinal(allAxisDetails.get(i)));

        }
        HashMap<Integer,TreeNode> keyValPairsForDimension = GetKeyValuePairOfSelectedDimensionsFromTree(selectedDimension);
        MDXUserQuery.allAxisDetails = allAxisDetails;
        MDXUserQuery.selectedMeasures = selectedMeasures;
        MDXUserQuery.measureMap = measureMap;
        MDXUserQuery.keyValPairsForDimension = keyValPairsForDimension;
        MDXUserQuery.cellOrdinalCombinations = cellOrdinalCombinations;
        startAsyncThreads("Blank");

    }

    /**
     * Checks user query entry from cache, if found, removes the entry to be fetched from server
     * @param keys
     * @return
     */
    public DimensionKeyCombinationAndMeasures GetAllDimensionAndMeasuresToFetch(List<String>keys,Set<Integer> selectedMeasures )
    {
        List<Integer> selectedMeasuresKeys = new ArrayList<>(selectedMeasures);

        DimensionKeyCombinationAndMeasures keyCombinationAndMeasuresToFetch =  new DimensionKeyCombinationAndMeasures();

        if(MainActivity.CachedDataCubes.size()>0) {
            for (int i = 0; i < keys.size(); i++) {
                String key = keys.get(i);
                if(keyCombinationAndMeasuresToFetch.Measures.size() >= selectedMeasuresKeys.size() ){
                    if(this._isMeasuresNonExistentInCacheForAKey(key,selectedMeasuresKeys)){
                        keyCombinationAndMeasuresToFetch.KeyCombinations.add(key);
                    }

                }else {
                    List<Integer> measuresToAdd = this._getNonExistentMeasuresFromCacheByKey(key, selectedMeasuresKeys);
                    if(measuresToAdd.size() > 0) {
                        keyCombinationAndMeasuresToFetch.Measures.addAll(measuresToAdd);
                        keyCombinationAndMeasuresToFetch.KeyCombinations.add(key);
                    }

                }
            }
        }
        else{
            keyCombinationAndMeasuresToFetch.KeyCombinations =keys;
            keyCombinationAndMeasuresToFetch.Measures = new HashSet<>(selectedMeasuresKeys);
        }
        return  keyCombinationAndMeasuresToFetch;
    }



    public String generateQueryForMeasures(List<Integer>measures,HashMap<Integer,String> measureMap){
        String sqlStatement = "";
        List<String> members=new ArrayList<>();

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
    public void ProcessUserQuery(List<Integer> selectedMeasures,HashMap<Integer,String> measureMap,HashMap<Integer,
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
        // start this once the previous is done
        MDXUserQuery.allAxisDetails = allAxisDetails;
        MDXUserQuery.selectedMeasures = selectedMeasures;
        MDXUserQuery.measureMap = measureMap;
        MDXUserQuery.keyValPairsForDimension = keyValPairsForDimension;
        MDXUserQuery.cellOrdinalCombinations = cellOrdinalCombinations;
        // start ASYNC THREAD
        this.startAsyncThreads(finalMDXQueries.get(0).get(0));
    }
    public void startAsyncThreads(String query){
        try {
            ExecutorService executor = Executors.newFixedThreadPool(1);
            if(query !="Blank") {
                OriginaQuery.isDownloadFinished = false;
                OriginaQuery originalQObj = new OriginaQuery(query);
                executor.execute(originalQObj);
            }
            else{
                OriginaQuery.isDownloadFinished =  true;
            }

            //InflatedQueryDownload_sequentialmanner();
            //executor.execute(in1);
         /*   Inflated2 in2 = new Inflated2(MDXUserQuery.allAxisDetails, MDXUserQuery.selectedMeasures, MDXUserQuery.measureMap, MDXUserQuery.keyValPairsForDimension,
                    MDXUserQuery.cellOrdinalCombinations, QueryProcessor.olapServiceURL,allLeaves,parentEntiresPerAxis);
            executor.execute(in2);*/
//            Inflated3 in3 = new Inflated3(MDXUserQuery.allAxisDetails, MDXUserQuery.selectedMeasures, MDXUserQuery.measureMap, MDXUserQuery.keyValPairsForDimension,
//                    MDXUserQuery.cellOrdinalCombinations, QueryProcessor.olapServiceURL,allLeaves,parentEntiresPerAxis);
//            executor.execute(in3);
            // start another thread to fetch siblings data
//            CacheProcessUpto1Level cacheParentLevelObj = new CacheProcessUpto1Level(MDXUserQuery.allAxisDetails, MDXUserQuery.selectedMeasures, MDXUserQuery.measureMap, MDXUserQuery.keyValPairsForDimension,
//                    MDXUserQuery.cellOrdinalCombinations, QueryProcessor.olapServiceURL, allParents);
//            executor.execute(cacheParentLevelObj);
        }
        catch(Exception e)
        {
            String s = e.getMessage();
        }
    }

    public void InflatedQueryDownload_sequentialmanner() {
        List<List<HashMap<Integer, TreeNode>>> allLeaves = getLeavesPerAxis(MDXUserQuery.keyValPairsForDimension, MDXUserQuery.allAxisDetails.get(0));
        List<List<HashMap<Integer, TreeNode>>> allParents = getSiblingsPerAxis(MDXUserQuery.keyValPairsForDimension, MDXUserQuery.allAxisDetails.get(0));
        //Log.d("Async Query", "Before calling thread pool " + String.valueOf(System.currentTimeMillis()));

        // start parallel thread to fetch inflated data for leaf levels
//            CacheProcess cache = new CacheProcess(MDXUserQuery.allAxisDetails, MDXUserQuery.selectedMeasures, MDXUserQuery.measureMap, MDXUserQuery.keyValPairsForDimension,
//                    MDXUserQuery.cellOrdinalCombinations, QueryProcessor.olapServiceURL);
//            executor.execute(cache);
        Inflated1 in1 = new Inflated1(MDXUserQuery.allAxisDetails, MDXUserQuery.selectedMeasures, MDXUserQuery.measureMap, MDXUserQuery.keyValPairsForDimension,
                MDXUserQuery.cellOrdinalCombinations, QueryProcessor.olapServiceURL,allLeaves,parentEntiresPerAxis, allParents);
        in1.run();
        Inflated2 in2 = new Inflated2(MDXUserQuery.allAxisDetails, MDXUserQuery.selectedMeasures, MDXUserQuery.measureMap, MDXUserQuery.keyValPairsForDimension,
                MDXUserQuery.cellOrdinalCombinations, QueryProcessor.olapServiceURL,allLeaves,parentEntiresPerAxis);
        in2.run();
        Inflated3 in3 = new Inflated3(MDXUserQuery.allAxisDetails, MDXUserQuery.selectedMeasures, MDXUserQuery.measureMap, MDXUserQuery.keyValPairsForDimension,
                MDXUserQuery.cellOrdinalCombinations, QueryProcessor.olapServiceURL,allLeaves,parentEntiresPerAxis);

        in3.run();
        in3.run();
    }


    private List<List<HashMap<Integer,TreeNode>>> getSiblingsPerAxis(HashMap<Integer, TreeNode> keyValPairsForDimension,
                                                                      List<List<Integer>> allAxisDetails) {
        List<List<HashMap<Integer, TreeNode>>> Leaves = new ArrayList<>();
        boolean isRootNode = false;
        Set<TreeNode> visitedTreeNodes =  new HashSet<>();

        for (int i = 1; i < allAxisDetails.size(); i++)// 0 th entry is for measures
        {
            List<HashMap<Integer, TreeNode>> axisWiseList = new ArrayList<>();
            for (int j = 0; j < allAxisDetails.get(i).size(); j++) {
                int key = allAxisDetails.get(i).get(j);

                TreeNode node = keyValPairsForDimension.get(key);
                if (node != null) {

                    node = node.getParent();
                    if (node.getReference().toString() != "Dimensions") {
                        //if this is in root level, we do not need to add that
                        if(!visitedTreeNodes.contains(node)) {

                            visitedTreeNodes.add(node);
                            HashMap<Integer, TreeNode> allLeaves = iterateTreeToGenerateChildren(node);
                            axisWiseList.add(allLeaves);
                        }
                    }
                } else {
                    isRootNode = true;
                    break;
                }
            }
            Leaves.add(axisWiseList);
            if (isRootNode)
                break;
        }
        //if this is in root level, we do not need to add that : simply return blank list
        if(isRootNode){
            return new ArrayList<>();
        }
        return Leaves;
    }
    public List<List<HashMap<Integer, TreeNode>>>getLeavesPerAxis(HashMap<Integer, TreeNode> keyValPairsForDimension,
                                                                  List<List<Integer>> allAxisDetails) {
        List<List<HashMap<Integer, TreeNode>> > Leaves = new ArrayList<>();
        Set <TreeNode> visitedTreeNodes =  new HashSet<TreeNode>();
        this.parentEntiresPerAxis =  new ArrayList<>();
        for (int i = 1; i < allAxisDetails.size(); i++)// 0 th entry is for measures
        {
            List<TreeNode> parents = new ArrayList<>();
            List<HashMap<Integer, TreeNode>> axisWiseList = new ArrayList<>();
            for (int j = 0; j < allAxisDetails.get(i).size(); j++) {

                TreeNode child = keyValPairsForDimension.get(allAxisDetails.get(i).get(j));
                if (child != null) {
                    List<TreeNode> children = child.getChildren();
                    if(children!=null && children.size()==0)
                    {
                        // 1st time this is running and the parent node itself is leaf node:
                        // then go back 1 level up and run for its parent
                        child = child.getParent();

                    }
                    else{
                        // the following section deals with situations like: Education. All Customers.  We know that the server will send values for all under 'All custmers' sections
                        // so if we go to its parent that is Education, its children will be 'All customers' which must be replaced by all those children under it.
                        // Education.Children--> All customers but we need Bachelors, Hgh School  Graduates etc

                        if(children!=null && children.size()>0 && children.size()<=1) {
                            String childName = children.get(0).getReference().toString();
                            if (!childName.contains("All")) {
                                child = child.getParent();
                            } else {
                                child = children.get(0);

                            }
                        }
                    }
                    if(!visitedTreeNodes.contains(child)) {
                        visitedTreeNodes.add(child);
                        HashMap<Integer, TreeNode> allLeaves = getLeaves(child);
                        axisWiseList.add(allLeaves);
                    }

                }
            }
            Leaves.add(axisWiseList);
            parentEntiresPerAxis.add(parents);
        }
        return Leaves;
    }
    public  HashMap<Integer,TreeNode> getLeaves(TreeNode parent){

        return iterateTreeToGenerateChildren(parent);

    }

    public HashMap<Integer,TreeNode> iterateTreeToGenerateChildren(TreeNode parent) {
        HashMap<Integer,TreeNode> allLeaves=new HashMap<>();
        List<TreeNode> children = parent.getChildren();
        if (children.size() == 0) {
            allLeaves.put(parent.getNodeCounter(),parent);

        } else {
            for (int i = 0; i < children.size(); i++) {
                allLeaves.put(children.get(i).getNodeCounter(),children.get(i));
            }
        }
        return allLeaves;
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

        String sqlMeasureStatement = this.generateQueryForMeasures(selectedMeasures, measureMap);
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
        String sqlStatement = this.generateSubQuery(axisWiseQuery);
        return sqlStatement;
    }

    // generating sub-query
    public String generateSubQuery(List<String> hierarchies) {
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
    public void CheckAndPopulateCache(List<String> combinations, List<List<TreeNode>> parentEntiresPerAxis, List<List<Long>> downloadedCube, boolean isUserQuery) {

        HashMap<String,Long> totalSum=new HashMap<>();

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
                        HashMap<Integer, Long> keyVal = new HashMap<>();

                        //update existing cache
                        String[] measureList = measure.replace("[", "").replace("]", "").split(",");
                        _updateMainCache(downloadedCube, totalSum, i, dimensionCombination, keyVal, measureList);

                    }
                }

            }
            if( combinations.size()>0){

                for( int i=0;i<combinations.size();i++){
                    String dimensionCombination = this._sortKeyCombination(combinations.get(i).substring(combinations.get(i).indexOf("#") + 1));
                    String measure = combinations.get(i).substring(0, combinations.get(i).indexOf("#"));

                    HashMap<Integer, Long> keyVal = new HashMap<>();

                    //update existing cache
                    String[] measureList = measure.replace("[", "").replace("]", "").split(",");

                     _updateMainCache(null, totalSum, i, dimensionCombination, keyVal, measureList);

                }
            }

        }
        catch (Exception ex)
        {

        }


    }

    private void  _updateMainCache(List<List<Long>> downloadedCube, HashMap<String, Long> totalSum, int i, String dimensionCombination, HashMap<Integer, Long> keyVal, String[] measureList) {
        long dummy = 0;
        Long cellVal = downloadedCube.get(i).get(0);
        if (MainActivity.CachedDataCubes.containsKey(dimensionCombination)){//MainActivity.CachedDataCubes.containsKey(dimensionCombination)) {
            // measure list per axis
            for (int j = 0; j < measureList.length; j++) {
                String measureKey = measureList[j].trim();

                if(downloadedCube!=null) {
                    MainActivity.CachedDataCubes.get(dimensionCombination).put(Integer.parseInt(measureKey), cellVal);
                }
            }
        } else {
            // measure list per axis
            for (int j = 0; j < measureList.length; j++) {
                String measureKey = measureList[j].trim();
                if(downloadedCube!=null) {
                    keyVal.put(Integer.parseInt(measureKey), cellVal);

                }
                else {

                    keyVal.put(Integer.parseInt(measureKey), dummy);
                }
            }
            MainActivity.CachedDataCubes.put(dimensionCombination,keyVal);
                   }

    }

}
