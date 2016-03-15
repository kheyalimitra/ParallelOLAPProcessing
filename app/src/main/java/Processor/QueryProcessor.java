package Processor;

import android.support.annotation.NonNull;

import DataRetrieval.*;
import DataStructure.TreeNode;

import java.util.*;

import MDXQueryProcessor.*;
import mobile.parallelolapprocessing.Async.Call.MDXUserQuery;
import mobile.parallelolapprocessing.DimensionTree;
import mobile.parallelolapprocessing.MainActivity;
import mobile.parallelolapprocessing.UI.DimensionMeasureGoogleHTMLTable;

public class QueryProcessor {

    public static final String olapServiceURL = "http://webolap.cmpt.sfu.ca/ElaWebService/Service.asmx";//"http://192.168.0.207/OLAPService/AdventureWorks.asmx";
    public static HashMap<String, HashMap<String, Long>> resultSet = new HashMap<>();

    public void QueryProcessor() {
    }

    public static TreeNode GetRootDimension() throws Exception {
        //Populate dimension tree up to two level
        Dimension dimensionObj = new Dimension(olapServiceURL);
        return dimensionObj.GetRootDimension();
    }

    public static TreeNode GetHierarchyDimension(TreeNode rootDimensionTree, String selection) throws Exception {
        //Populate dimension tree up to two level
        Dimension dimensionObj = new Dimension(olapServiceURL);
        //User clicked on Account>Account Number
        dimensionObj.PopulateLeafNode(rootDimensionTree, selection);
        return rootDimensionTree;
    }

    public static HashMap<Integer, String> GetMeasures() throws Exception {
        Measures measuresObj = new Measures(olapServiceURL);
        return measuresObj.GetMeasures();
    }


    public boolean GetUserRequestedData(int[] entryPerDimension,
                                        TreeNode rootDimensionTree,
                                        List<String> hardcodedInputDim,
                                        Measures measuresObj,
                                        HashMap<Integer, String> measureMap,
                                        List<String> hardcodedInputMeasures) throws Exception {
        //Initialize data cube axis
        DataCubeAxis dca = new DataCubeAxis();

        try {
            //Integer represent axis and List of TreeNode denotes dimensions on that axis
            HashMap<Integer, List<TreeNode>> dimensionsInAxes = dca.GetTreeNodeListForEachAxis(rootDimensionTree, hardcodedInputDim,
                                                                                                entryPerDimension);
            //Measure id to measure string map
            HashMap<Integer, String> selectedMeasureMap = measuresObj.GetHashKeyforSelecteditems(hardcodedInputMeasures, measureMap);

            //Start timer
            DimensionTree.startTimer = System.currentTimeMillis();

            //generates Key combinations
            MDXQProcessor mdxQueryProcessorObj = new MDXQProcessor();
            List<String> generatedKeys = mdxQueryProcessorObj.GenerateKeyCombination(dimensionsInAxes);
            List<String> generatedKeysClone = getCloneOfGivenList(generatedKeys);
            //to store original dimension selection  before sort and merge : this will be used after data is fetched
            //to check if any entries are found in Cache
            List<String> userRequestedKeyCombinations = new ArrayList<>(generatedKeys);
            List<Integer> userRequestedMeasures = initializeStaticVariablesForDisplay(measuresObj, measureMap, hardcodedInputMeasures, dimensionsInAxes, userRequestedKeyCombinations);

            //check from cache
            HashMap<String, List<String>> nonCachedSelections = mdxQueryProcessorObj.checkCachedKeysToRemoveDuplicateEntries(generatedKeys,
                                                                                        selectedMeasureMap.keySet());
            List<String> nonCachedMeasures = nonCachedSelections.get("1");
            List<Integer> selectedMeasureKeyList = new ArrayList<>();
            for (String item : nonCachedMeasures) {
                selectedMeasureKeyList.add(Integer.parseInt(item));
            }
            //List<Integer> selectedMeasureKeyList = measuresObj.GetSelectedKeyList(nonCachedMeasures, measureMap);
            List<String> nonCachedKeys = nonCachedSelections.get("0");
            List<String>nonCachedKeysClone =  getCloneOfGivenList(nonCachedKeys);
            if (nonCachedKeys.size() > 0) {
                // get data from server
                HashMap<String, HashMap<String, Long>> resultSetFromServer = mdxQueryProcessorObj.ProcessUserQuery(selectedMeasureKeyList, selectedMeasureMap, dimensionsInAxes, nonCachedKeys, false, true);
                resultSet = _getQueryResultFromCache(nonCachedKeysClone, resultSet, new ArrayList<>(selectedMeasureMap.keySet()));

                // add values from ser to exisitng result set ( in case there are partial hit from cache)

                // if there is a hit frm cache, take matched part from cache and add it to result set
                //if (userRequestedKeyCombinations.size() != resultSet.size()) {
                //    resultSet = _getQueryResultFromCache(userRequestedKeyCombinations, resultSet, new ArrayList<>(selectedMeasureMap.keySet()));
                //}

            } else {
                // 100% hit
                // use original Atomic keys to fetch records from cache and display to user
                /*if (resultSet.size() == userRequestedKeyCombinations.size()) {

                    MDXUserQuery.isComplete = true;
                    return true;
                } else {
                */    resultSet = _getQueryResultFromCache(generatedKeysClone, resultSet, userRequestedMeasures);

                    MDXUserQuery.isComplete = true;
                    return true;
                //}
            }

        } catch (Exception ex) {

            return false;
        }
        MDXUserQuery.isComplete = true;
        return true;
    }

    @NonNull
    private List<String> getCloneOfGivenList(List<String> generatedKeys) {
        List<String> generatedKeysClone =  new ArrayList<>();
        for(String item: generatedKeys){
            generatedKeysClone.add(item);
        }
        return generatedKeysClone;
    }

    @NonNull
    private List<Integer> initializeStaticVariablesForDisplay(Measures measuresObj, HashMap<Integer, String> measureMap, List<String> hardcodedInputMeasures, HashMap<Integer, List<TreeNode>> selectedDimension, List<String> originalAtomicKeys) {
        HashMap<Integer, String> selectedMeasures;
        MDXQProcessor mdxObj = new MDXQProcessor();
        // assign values to static variables to display result in table in google table
        DimensionMeasureGoogleHTMLTable.keyValPairsForDimension = MDXUserQuery.keyValPairsForDimension = mdxObj.GetKeyValuePairOfSelectedDimensionsFromTree(selectedDimension);
        selectedMeasures = measuresObj.GetHashKeyforSelecteditems(hardcodedInputMeasures, measureMap);
        List<Integer> userRequestedMeasures = new ArrayList<>(selectedMeasures.keySet());
        DimensionMeasureGoogleHTMLTable.allAxisDetails = MDXUserQuery.allAxisDetails = mdxObj.GetAxisDetails(userRequestedMeasures, originalAtomicKeys);
        MDXUserQuery.cellOrdinalCombinations = new ArrayList<>();
        int queryCount = MDXUserQuery.allAxisDetails.size();
        for (int i = 0; i < queryCount; i++) {
            MDXUserQuery.cellOrdinalCombinations.add(mdxObj.GenerateCellOrdinal(MDXUserQuery.allAxisDetails.get(i)));
        }
        DimensionMeasureGoogleHTMLTable.cellOrdinalCombinations = MDXUserQuery.cellOrdinalCombinations;
        DimensionMeasureGoogleHTMLTable.measureMap = MDXUserQuery.measureMap = _getSelectedMeasureFromMap(measureMap, userRequestedMeasures);
        return userRequestedMeasures;
    }

    private HashMap<Integer, String> _getSelectedMeasureFromMap(HashMap<Integer, String> measureMap, List<Integer> selectedMesureKeyList) {
        HashMap<Integer, String> _measureKeyVal = new HashMap<>();
        for (int i = 0; i < selectedMesureKeyList.size(); i++) {
            int key = selectedMesureKeyList.get(i);
            _measureKeyVal.put(key, measureMap.get(key).toString());
        }
        return _measureKeyVal;
    }

    private HashMap<String, HashMap<String, Long>> _getQueryResultFromCache(List<String> originalAtomicKeys, HashMap<String, HashMap<String, Long>> resultSet, List<Integer> measures) {
        HashMap<String, HashMap<String, Long>> newResultSet = new HashMap<>(resultSet);
        if(resultSet==null){
            resultSet = new HashMap<>();
        }
        if (originalAtomicKeys.size() > 0) {
            for (int i = 0; i < originalAtomicKeys.size(); i++) {
                //if (resultSet != null && resultSet.size() > 0) {
                    String key = originalAtomicKeys.get(i);
                    if (!resultSet.containsKey(key)) {
                        // fetch value from Cache
                        if (MainActivity.CachedDataCubes.containsKey(key)) {
                            // find for specific measures and if found add it to result set
                            HashMap<String, Long> measuresResult = new HashMap<>();
                            for (Map.Entry entryPair : MainActivity.CachedDataCubes.get(key).entrySet()) {
                                for (int j = 0; j < measures.size(); j++) {
                                    if (entryPair.getKey().equals( Long.parseLong(measures.get(j).toString()))) {
                                        measuresResult.put(measures.get(j).toString(), Long.parseLong(entryPair.getValue().toString()));
                                        break;
                                    }
                                }
                            }
                            newResultSet.put(key, measuresResult);
                        }
                    }
                }
            //}
        }
        return newResultSet;
    }


}
