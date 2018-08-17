package test;

import java.util.ArrayList;
import java.util.HashMap;

public class TestCase 
{
	String summary;
	String label = "";
	String testType = "";	// {NONE, FUNCTIONAL, REGRESSION, AUTOMATION, PERFORMANCE}
	String testCaseId = "";
	String testScreenName = "";
	boolean uploadStatus;
	String assignee = "";
	String currentSprint = "";
	String url = "";
	String issueRelatesTo = "";
	String gwtSteps = "";
	String components = "";
	 
	ArrayList<String> testSteps;
	ArrayList<String> expectedResult;

	public TestCase()
	{
		testSteps = new ArrayList<String>();
		expectedResult = new ArrayList<String>();
	}
}
