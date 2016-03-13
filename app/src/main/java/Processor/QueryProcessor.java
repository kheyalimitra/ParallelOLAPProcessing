package Processor;
import DataRetrieval.*;
import DataStructure.TreeNode;
import java.util.*;
import MDXQueryProcessor.*;
import mobile.parallelolapprocessing.Async.Call.MDXUserQuery;
import mobile.parallelolapprocessing.DimensionTree;
import mobile.parallelolapprocessing.MainActivity;

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
			//check from cache
			List<String> nonCachedKeys = mdxQueryProcessorObj.checkCachedKeysToRemoveDuplicateEntries(generatedKeys, selectedMeasures.keySet());

			if (nonCachedKeys.size() > 0) {

				List<Integer> selectedMesureKeyList = measuresObj.GetSelectedKeyList(hardcodedInputMeasures, measureMap);
                // get data from server
				HashMap<String,HashMap<String,Long>> resultSetFromServer = mdxQueryProcessorObj.ProcessUserQuery(selectedMesureKeyList, selectedMeasures, selectedDimension, nonCachedKeys, false);
				// add values from ser to exisitng result set ( in case there are partial hit from cache)
				resultSet.putAll(resultSetFromServer);
				// if there is a hit frm cache, take matched part from cache and add it to result set
				if(originalAtomicKeys.size() != resultSet.size())
				{
					resultSet = _getQueryResultFromCache(originalAtomicKeys,resultSet, new ArrayList<Integer>(selectedMeasures.keySet()));
				}

			} else {
				// 100% hit
				// use original Atomic keys to fetch records from cache and display to user
					if(resultSet.size()==originalAtomicKeys.size()){

						MDXUserQuery.isComplete = true;
						return true;
					}
				else{
						resultSet = _getQueryResultFromCache(originalAtomicKeys,resultSet,new ArrayList<Integer>(selectedMeasures.keySet()));

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
