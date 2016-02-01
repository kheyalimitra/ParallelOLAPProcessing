package Processor;
import DataCaching.DataCaching;
import DataRetrieval.*;
import DataStructure.TreeNode;
import DataCaching.*;
import java.util.*;
import MDXQueryProcessor.*;

public class QueryProcessor {

	public static final String olapServiceURL="http://192.168.0.207/OLAPService/AdventureWorks.asmx";
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





	public static void StartActivity() throws Exception{


		//Populate dimension tree up to two level
		Dimension dimensionObj = new Dimension(olapServiceURL);
		TreeNode rootDimensionTree = dimensionObj.GetRootDimension();

		//Populates measures in a Hashmap
		Measures measuresObj = new Measures(olapServiceURL);
		HashMap<Integer,String> measureMap = measuresObj.GetMeasures();

		//User Input => Simulation for fetching child nodes for dimensions
		//User expanded on the particular node

		//User clicked on Account>Account Number
		dimensionObj.PopulateLeafNode(rootDimensionTree,"[Dimension].[Account].[Account Number]");
		dimensionObj.PopulateLeafNode(rootDimensionTree,"[Dimension].[Account].[Account Type]");
		dimensionObj.PopulateLeafNode(rootDimensionTree,"[Dimension].[Customer].[Country]");
		dimensionObj.PopulateLeafNode(rootDimensionTree,"[Dimension].[Geography].[Country]");
		dimensionObj.PopulateLeafNode(rootDimensionTree,"[Dimension].[Geography].[Geography]");
		dimensionObj.PopulateLeafNode(rootDimensionTree,"[Dimension].[Product].[Color]");
		dimensionObj.PopulateLeafNode(rootDimensionTree,"[Dimension].[Employee].[Gender]");
		dimensionObj.PopulateLeafNode(rootDimensionTree,"[Dimension].[Date].[Calendar Quarter of Year]");

		//query generation test: must come from user, now it is hardcoded
		List<String> hardcodedInputDim = new ArrayList<String>();
		//hardcodedInputDim.add("[Dimension].[Account].[Account Type].[All Accounts]");
		//hardcodedInputDim.add("[Dimension].[Geography].[Country]");
		//hardcodedInputDim.add("[Dimension].[Geography].[Geography]");
		hardcodedInputDim.add("[Dimension].[Account].[Account Type].[All Accounts].[Revenue]");
		hardcodedInputDim.add("[Dimension].[Account].[Account Type].[All Accounts].[Expenditures]");
		//hardcodedInputDim.add("[Dimension].[Date].[Calendar Quarter of Year]");
		//,
		hardcodedInputDim.add("[Dimension].[Employee].[Gender].[All Employees].[Male]");
		hardcodedInputDim.add("[Dimension].[Employee].[Gender].[All Employees].[Female]");


		//this is hardcoded now should come from user input
		int [] entryPerDimension ={2,2} ;
		DataCubeAxis dca = new DataCubeAxis();
		HashMap<Integer,List<TreeNode>> selectedDimension = dca.GetTreeNodeListForEachAxis(rootDimensionTree,hardcodedInputDim,entryPerDimension);

		//generates Key combinations
		MDXQProcessor mdxQueryProcessorObj = new MDXQProcessor();
		List<String> generatedKeys= mdxQueryProcessorObj.GenerateKeyCombination(selectedDimension);

		//to store original dimension selection  before sort and merge : this will be used after data is fetched
		//to check if any entries are found in Cache
		List<String> originalAtomicKeys =new ArrayList<>(generatedKeys);

		//check from cache
		List<String> nonCachedKeys = mdxQueryProcessorObj.checkCachedKeysToRemoveDuplicateEntries(generatedKeys);

		if(nonCachedKeys.size()>0) {
			List<String> hardcodedInputMeasures = new ArrayList<String>();
			hardcodedInputMeasures.add("Internet Sales Amount");
			hardcodedInputMeasures.add("Internet Order Count");
			HashMap<Integer, String> selectedMeasures = measuresObj.GetHashKeyforSelecteditems(hardcodedInputMeasures, measureMap);
			List<Integer> selectedMesureKeyList = measuresObj.GetSelectedKeyList(hardcodedInputMeasures, measureMap);
			mdxQueryProcessorObj.ProcessUserQuery(selectedMesureKeyList, selectedMeasures, selectedDimension, nonCachedKeys,false);
		}
		else{
			// 100% hit
			// use original Atomic keys to fetch records from cache and display to user
		}
		////
//		//2nd set
//		/////
//		hardcodedInputDim = new ArrayList<String>();
//		//hardcodedInputDim.add("[Dimension].[Account].[Account Type].[All Accounts]");
//		//hardcodedInputDim.add("[Dimension].[Geography].[Country]");
//		//hardcodedInputDim.add("[Dimension].[Geography].[Geography]");
//		hardcodedInputDim.add("[Dimension].[Account].[Account Type].[All Accounts].[Revenue]");
//		hardcodedInputDim.add("[Dimension].[Account].[Account Type].[All Accounts].[Expenditures]");
//		//hardcodedInputDim.add("[Dimension].[Date].[Calendar Quarter of Year]");
//		//,
//		hardcodedInputDim.add("[Dimension].[Employee].[Gender].[All Employees].[Male]");
//		hardcodedInputDim.add("[Dimension].[Employee].[Gender].[All Employees].[Female]");
//
//		//DataCubeAxis dca = new DataCubeAxis();
//		selectedDimension = dca.GetTreeNodeListForEachAxis(rootDimensionTree,hardcodedInputDim,entryPerDimension);
//
//		//generates Key combinations
//		mdxQueryProcessorObj = new MDXQProcessor();
//		generatedKeys= mdxQueryProcessorObj.GenerateKeyCombination(selectedDimension);
//
//		//to store original dimension selection  before sort and merge : this will be used after data is fetched
//		//to check if any entries are found in Cache
//		QueryProcessor.originalAtomicKeys =new ArrayList<>(generatedKeys);
//
//		nonCachedKeys = mdxQueryProcessorObj.checkCachedKeysToRemoveDuplicateEntries(generatedKeys);
//		if(nonCachedKeys.size()>0) {
//			List<String> hardcodedInputMeasures = new ArrayList<String>();
//			hardcodedInputMeasures = new ArrayList<String>();
//			hardcodedInputMeasures.add("Internet Sales Amount");
//			hardcodedInputMeasures.add("Internet Order Count");
//			HashMap<Integer, String> selectedMeasures = measuresObj.GetHashKeyforSelecteditems(hardcodedInputMeasures, measureMap);
//			List<Integer> selectedMesureKeyList = measuresObj.GetSelectedKeyList(hardcodedInputMeasures, measureMap);
//			mdxQueryProcessorObj.ProcessUserQuery(selectedMesureKeyList, selectedMeasures, selectedDimension, nonCachedKeys,false);
//		}
//		else
//		{
//			// get data from cache:
//		}


	}




}
