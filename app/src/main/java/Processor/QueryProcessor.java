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

	public static final String olapServiceURL="http://webolap.cmpt.sfu.ca/ElaWebService/Service.asmx";//"http://192.168.0.207/OLAPService/AdventureWorks.asmx";
	public static HashMap<String,HashMap<String,Long>> resultSet= new HashMap<>();
	public void QueryProcessor() {
	}

	public static TreeNode GetRootDimension() throws Exception{
		//Populate dimension tree up to two level
		Dimension dimensionObj = new Dimension(olapServiceURL);
		return dimensionObj.GetRootDimension();
	}

	public static TreeNode GetHierarchyDimension(TreeNode rootDimensionTree,String selection) throws Exception{
		//Populate dimension tree up to two level
		Dimension dimensionObj = new Dimension(olapServiceURL);
		//User clicked on Account>Account Number
		dimensionObj.PopulateLeafNode(rootDimensionTree,selection);
		return rootDimensionTree;
	}

	public static HashMap<Integer,String> GetMeasures() throws Exception{
		Measures measuresObj = new Measures(olapServiceURL);
		return measuresObj.GetMeasures();
	}



	public boolean GetUserRequestedQueryData(int[] entryPerDimension, TreeNode rootDimensionTree,List<String>hardcodedInputDim,Measures measuresObj,HashMap<Integer,String>measureMap,
											 List<String> hardcodedInputMeasures) throws Exception {

		DataCubeAxis dca = new DataCubeAxis();

		try {
			HashMap<Integer, List<TreeNode>> selectedDimension = dca.GetTreeNodeListForEachAxis(rootDimensionTree, hardcodedInputDim, entryPerDimension);
			HashMap<Integer, String> selectedMeasures = measuresObj.GetHashKeyforSelecteditems(hardcodedInputMeasures, measureMap);
			DimensionTree.startTimer = System.currentTimeMillis();

			//generates Key combinations
			MDXQProcessor mdxQueryProcessorObj = new MDXQProcessor();
			List<String> generatedKeys = mdxQueryProcessorObj.GenerateKeyCombination(selectedDimension);

			//to store original dimension selection  before sort and merge : this will be used after data is fetched
			//to check if any entries are found in Cache
			List<String> originalAtomicKeys = new ArrayList<>(generatedKeys);
			List<Integer> originalMeasures = initializeStaticVariablesForDisplay(measuresObj, measureMap, hardcodedInputMeasures, selectedDimension, originalAtomicKeys);
			//check from cache
			HashMap<String,List<String>> nonCachedSelections = mdxQueryProcessorObj.checkCachedKeysToRemoveDuplicateEntries(generatedKeys, selectedMeasures.keySet());
			List<String> nonCachedMeasures = nonCachedSelections.get("1");
			List<Integer> selectedMeasureKeyList = new ArrayList<>();
			for( String item : nonCachedMeasures){
				selectedMeasureKeyList.add(Integer.parseInt(item));
			}
			//List<Integer> selectedMeasureKeyList = measuresObj.GetSelectedKeyList(nonCachedMeasures, measureMap);
            List<String> nonCachedKeys = nonCachedSelections.get("0");
			if (nonCachedKeys.size() > 0) {

				 // get data from server
				HashMap<String,HashMap<String,Long>> resultSetFromServer = mdxQueryProcessorObj.ProcessUserQuery(selectedMeasureKeyList, selectedMeasures, selectedDimension, nonCachedKeys, false,true);
				// add values from ser to exisitng result set ( in case there are partial hit from cache)
				resultSet.putAll(resultSetFromServer);
				// if there is a hit frm cache, take matched part from cache and add it to result set
				if(originalAtomicKeys.size() != resultSet.size())
				{
					resultSet = _getQueryResultFromCache(originalAtomicKeys,resultSet, new ArrayList<>(selectedMeasures.keySet()));
				}

			} else {
				// 100% hit
				// use original Atomic keys to fetch records from cache and display to user
				if(resultSet.size()==originalAtomicKeys.size()){

						MDXUserQuery.isComplete = true;
						return true;
					}
				else{
						resultSet = _getQueryResultFromCache(originalAtomicKeys,resultSet,originalMeasures);

						MDXUserQuery.isComplete = true;
						return true;
					}
			}

		} catch (Exception ex) {

			return false;
		}
		MDXUserQuery.isComplete = true;
		return  true;
	}

	@NonNull
	private List<Integer> initializeStaticVariablesForDisplay(Measures measuresObj, HashMap<Integer, String> measureMap, List<String> hardcodedInputMeasures, HashMap<Integer, List<TreeNode>> selectedDimension, List<String> originalAtomicKeys) {
		HashMap<Integer, String> selectedMeasures;
		MDXQProcessor mdxObj = new MDXQProcessor();
		// assign values to static variables to display result in table in google table
		DimensionMeasureGoogleHTMLTable.keyValPairsForDimension =  MDXUserQuery.keyValPairsForDimension = mdxObj.GetKeyValuePairOfSelectedDimensionsFromTree(selectedDimension);
		selectedMeasures = measuresObj.GetHashKeyforSelecteditems(hardcodedInputMeasures, measureMap);
		List<Integer> originalMeasures =  new ArrayList<>(selectedMeasures.keySet());
		DimensionMeasureGoogleHTMLTable.allAxisDetails = MDXUserQuery.allAxisDetails = mdxObj.GetAxisDetails(originalMeasures, originalAtomicKeys);
		MDXUserQuery.cellOrdinalCombinations= new ArrayList<>();
		int queryCount =MDXUserQuery.allAxisDetails.size();
		for(int i=0;i<queryCount;i++) {
            MDXUserQuery.cellOrdinalCombinations.add(mdxObj.GenerateCellOrdinal(MDXUserQuery.allAxisDetails.get(i)));
        }
		DimensionMeasureGoogleHTMLTable.cellOrdinalCombinations = MDXUserQuery.cellOrdinalCombinations;
		DimensionMeasureGoogleHTMLTable.measureMap = MDXUserQuery.measureMap  = _getSelectedMeasureFromMap(measureMap,originalMeasures);
		return originalMeasures;
	}

	private HashMap<Integer, String> _getSelectedMeasureFromMap(HashMap<Integer, String> measureMap, List<Integer> selectedMesureKeyList) {
		HashMap<Integer, String> _measureKeyVal = new HashMap<>();
		for (int i=0;i<selectedMesureKeyList.size();i++){
			int key = selectedMesureKeyList.get(i);
			_measureKeyVal.put(key,measureMap.get(key).toString());
		}
		return _measureKeyVal;
	}

	private HashMap<String, HashMap<String, Long>> _getQueryResultFromCache(List<String> originalAtomicKeys, HashMap<String,HashMap<String, Long>> resultSet, List<Integer>measures) {
		HashMap<String, HashMap<String, Long>> newResultSet =  new HashMap<>(resultSet);
		if(originalAtomicKeys.size()>0)
		{
			for(int i=0;i<originalAtomicKeys.size();i++)
			{
				if(resultSet!=null && resultSet.size()>0){
					String key = originalAtomicKeys.get(i);
					if(!resultSet.containsKey(key)){
						// fetch value from Cache
						if(MainActivity.CachedDataCubes.containsKey(key)){
							// find for specific measures and if found add it to result set
							HashMap<String,Long> measuresResult =  new HashMap<>();
							for(Map.Entry entryPair: MainActivity.CachedDataCubes.get(key).entrySet()) {
								for( int j=0;j<measures.size();j++){
									if (entryPair.getKey()==measures.get(j).toString())
										measuresResult.put(measures.get(j).toString(),Long.parseLong(entryPair.getValue().toString()));
								}
							}
							newResultSet.put(key,measuresResult);
						}
					}
				}
			}
		}
		return newResultSet;
	}




}
