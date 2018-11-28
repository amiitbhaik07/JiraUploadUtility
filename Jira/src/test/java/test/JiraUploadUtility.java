package test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;
import javax.swing.JOptionPane;
import org.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class JiraUploadUtility {
	static Excel e;
	static String excelName = "JiraUploadExcel";
	static String sheetName;
	static int summaryMapping = -1, gwtMapping = -1, labelMapping = -1, testTypeMapping = -1, uploadStatus = -1,
			testScreenNameMapping = -1, zephyrTestCaseIDMapping = -1, currentSprintMapping = -1, assigneeMapping = -1,
			urlMapping = -1, issueRelatesToMapping = -1, componentsMapping = -1;
	static String currDir = System.getProperty("user.dir");
	static String filePath = currDir + "\\" + excelName + ".xlsx";
	static String defaultProjectName = "", hitCreateAfterEveryTest = "", username = "", password = "",
			runWith = "BROWSER", projectKey = "";
	static boolean boolHitCreateAfterEveryTest = false;
	static int firstTest = 0;
	static String postUrl = "https://jira.mediware.com/rest/api/2/issue/";
	static int successfullyUploaded = 0, errorInUploading = 0;
	static PrintStream printStream;

	public static void main(String[] args) throws Exception {
		try {
			sheetName = "Tests";
			e = new Excel(filePath);
			e.getSheet("Data");
			defaultProjectName = e.getStringCellValue(0, 1);
			hitCreateAfterEveryTest = e.getStringCellValue(1, 1);
			username = e.getStringCellValue(2, 1);
			password = e.getStringCellValue(3, 1);
			runWith = e.getStringCellValue(4, 1);
			projectKey = e.getStringCellValue(5, 1);
			if (defaultProjectName == null || defaultProjectName.equals(""))
				throw new Exception("Please enter default project name in 'Data' sheet of JiraUpload.xlsx");
			if (runWith == null || runWith.equals(""))
				throw new Exception("Please specify whether to run with BROWSER or through API");
			if ((projectKey == null || projectKey.equals("")) && runWith.equalsIgnoreCase("API"))
				throw new Exception("Please specify the Project Key");
			if(runWith.equalsIgnoreCase("API"))
			{
				if(username==null || username.trim().equals(""))
					throw new Exception("Please enter the Username");
				if(password==null || password.trim().equals(""))
					throw new Exception("Please enter the Password");
			}
			if (hitCreateAfterEveryTest != null && !hitCreateAfterEveryTest.equals(""))
				if (hitCreateAfterEveryTest.trim().equalsIgnoreCase("Y"))
					boolHitCreateAfterEveryTest = true;
			e.getSheet(sheetName);
			ArrayList<TestCase> allTestCases = new ArrayList<TestCase>();
			TestCase tc = null;
			setMapping();
			new File(currDir + "\\Logs\\").mkdirs();
			printStream = new PrintStream(new FileOutputStream(new File("Logs\\Log_"+getCurrentTimeStamp()+".txt")));
			printToLogsAndConsole("====================================");
			for (int i = 1; i < e.sheet.getPhysicalNumberOfRows(); i++) {
				try {
					tc = JiraUploadUtility.getTestCase(i);
				} catch (Exception e) {
					tc = null;
					printToLogsAndConsole("Exception : " + e.getMessage());
				}
				if (tc != null)
					if (tc.summary != null && !tc.summary.equals(""))
						allTestCases.add(tc);
			}
			e.closeWorkbook();
			printToLogsAndConsole("====================================");
			printToLogsAndConsole("Total no of Records to process : " + allTestCases.size());
			printToLogsAndConsole("Hit ENTER to continue. (Please close the JiraUpload.xlsx sheet)");
			Scanner sc = new Scanner(System.in);
			sc.nextLine();
			printToLogsAndConsole("====================================");
			int counter = 1;
			for (TestCase t : allTestCases) {
				printToLogsAndConsole(
						"\n\n==============================================================================================================");
				printToLogsAndConsole("Test Number " + (counter++) + " of " + allTestCases.size() + " : " + t.summary);
				if (runWith.equalsIgnoreCase("API"))
					uploadThroughApi(t);
				else
					uploadThroughWeb(t);
			}
			printToLogsAndConsole(
					"==============================================================================================================");
			printToLogsAndConsole(
					"@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ Successfully Completed @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
			printToLogsAndConsole("Upload SUCCESS    :     " + successfullyUploaded + "\nUpload FAILED         :     "
					+ errorInUploading);
			JOptionPane.showMessageDialog(null, "Upload SUCCESS    :     " + successfullyUploaded
					+ "\nUpload FAILED         :     " + errorInUploading);
		} catch (Exception e) {
			JOptionPane.showMessageDialog(null, e.getMessage());
			JOptionPane.showMessageDialog(null, "Upload SUCCESS    :     " + successfullyUploaded
					+ "\nUpload FAILED         :     " + errorInUploading);
			throw e;
		}
	}
	
	public static String getCurrentTimeStamp()
	{
		return new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date());
	}

	public static void printToLogsAndConsole(String msg){
		printStream.println(msg);
		System.out.println(msg);
	}

	public static void uploadThroughApi(TestCase t) throws Exception {
		try {
			String encoder = authenticateUser();
			StringBuffer request = new StringBuffer();
			request.append("{\"fields\": {\"project\": {\"key\": \"");
			request.append(projectKey);
			request.append("\"},\"issuetype\": {\"name\": \"Test\"},\"summary\": \"");
			request.append(t.summary.replace("\n", "\\r\\n").replace("\"", "\\\""));
			request.append("\",\"description\": \"");
			if (t.testScreenName != null && !t.testScreenName.equals(""))
				request.append(t.testScreenName.replace("\n", "\\r\\n").replace("\"", "\\\""));
			if (t.gwtSteps != null && !t.gwtSteps.equals(""))
				request.append("\\r\\n" + t.gwtSteps.replace("\n", "\\r\\n").replace("\"", "\\\""));
			request.append("\"");
			if (t.assignee != null && !t.assignee.equals("")) {
				request.append(",\"assignee\": {\"name\": \"");
				request.append(t.assignee);
			}
			request.append("\"}");
			request.append(",\"labels\": [\"");
			request.append(t.label);
			request.append("\"]");
			if(t.components != null && !t.components.equals(""))
				request.append(",\"components\": [{\"name\" : \""+t.components+"\"}]");
			request.append("},\"update\" : {\"customfield_10006\": [{\"set\": 0}]");
			if (t.issueRelatesTo.length != 0) {
				request.append(
						",\"issuelinks\": [{\"add\": {\"type\": {\"name\": \"Relates\",\"inward\": \"relates to\",\"outward\": \"relates to\"},\"outwardIssue\": {\"key\": \"");
				request.append(t.issueRelatesTo[0].trim());
				request.append("\"}}}]");
			}
			request.append("}}");
			printToLogsAndConsole("*************************************");
			/*printToLogsAndConsole(request.toString());
			printToLogsAndConsole("*************************************");*/
			String response = sendPostRequest(postUrl, request.toString(), encoder);
			/*printToLogsAndConsole(response);
			printToLogsAndConsole("*************************************");*/
			String testCaseId = getTestCaseId(response);
			printToLogsAndConsole("Test Case ID : " + testCaseId);
			printToLogsAndConsole("*************************************");
			boolean linkingErrorFlag = false;
			if (t.issueRelatesTo.length > 1) {
				String putUrl = postUrl + testCaseId;
				for (int i = 1; i < t.issueRelatesTo.length; i++) {
					try {
						request = new StringBuffer();
						request.append(
								"{\"update\": {\"issuelinks\": [{\"add\": {\"type\": {\"name\": \"Relates\",\"inward\": \"relates to\",\"outward\": \"relates to\"},\"outwardIssue\": {\"key\": \"");
						request.append(t.issueRelatesTo[i]);
						request.append("\"}}}]}}");
						//printToLogsAndConsole(request.toString());
						sendPutRequest(putUrl, request.toString(), encoder);
						printToLogsAndConsole("Additionally linked ID '" + t.issueRelatesTo[i] + "' to Test Case '"
								+ testCaseId + "'");
						printToLogsAndConsole("*************************************");
						request.delete(0, request.length());
					} catch (Exception e3) {
						linkingErrorFlag = true;
						printToLogsAndConsole("ERROR : In linking Jira ID '" + t.issueRelatesTo[i]
								+ "' with newly created Test Case '" + testCaseId + "'");
					}
				}
			}
			successfullyUploaded++;
			e = new Excel(filePath);
			e.getSheet(sheetName);
			setMapping();
			if (linkingErrorFlag)
				setUploadValue(t.summary, "TRUE/IssuesPartiallyLinked", testCaseId);
			else
				setUploadValue(t.summary, "TRUE", testCaseId);
			FileOutputStream outputStream = new FileOutputStream(filePath);
			e.workbook.write(outputStream);
			e.closeWorkbook();
		} catch (Exception e1) {
			errorInUploading++;
			e1.printStackTrace();
			e = new Excel(filePath);
			e.getSheet(sheetName);
			setMapping();
			setUploadValue(t.summary, "ERROR", "");
			FileOutputStream outputStream = new FileOutputStream(filePath);
			e.workbook.write(outputStream);
			e.closeWorkbook();
		}
	}

	public static String sendPostRequest(String postUrl, String request, String encoder) {
		StringBuffer jsonString = null;
		try {
			URL url = new URL(postUrl);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Authorization", "Basic " + encoder);
			connection.setRequestProperty("Accept", "application/json");
			connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
			OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), "UTF-8");
			writer.write(request);
			writer.close();
			BufferedReader br = new BufferedReader(new java.io.InputStreamReader(connection.getInputStream()));
			jsonString = new StringBuffer();
			String line;
			while ((line = br.readLine()) != null) {
				jsonString.append(line);
			}
			br.close();
			connection.disconnect();
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage());
		}
		return jsonString.toString();
	}

	public static String sendPutRequest(String putUrl, String request, String encoder) {
		StringBuffer jsonString = null;
		try {
			URL url = new URL(putUrl);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setRequestMethod("PUT");
			connection.setRequestProperty("Authorization", "Basic " + encoder);
			connection.setRequestProperty("Accept", "application/json");
			connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
			OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), "UTF-8");
			writer.write(request);
			writer.close();
			BufferedReader br = new BufferedReader(new java.io.InputStreamReader(connection.getInputStream()));
			jsonString = new StringBuffer();
			String line;
			while ((line = br.readLine()) != null) {
				jsonString.append(line);
			}
			br.close();
			connection.disconnect();
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage());
		}
		return jsonString.toString();
	}

	private static String getTestCaseId(String jsonString) throws Exception {
		JSONObject jsonObject = new JSONObject(jsonString);
		return jsonObject.getString("key");
	}

	private static String authenticateUser() throws Exception {
		Authenticator.setDefault(new Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(username, password.toCharArray());
			}
		});
		String s = username + ":" + password;
		byte[] authBytes = s.getBytes(StandardCharsets.UTF_8);
		String encoded = java.util.Base64.getEncoder().encodeToString(authBytes);
		return encoded;
	}

	public static void uploadThroughWeb(TestCase t) throws Exception {
		String testCaseId = "";
		WebDriver driver = null;
		BasicUtils basic = null;
		printToLogsAndConsole("Uploading : " + t.summary);
		if (firstTest == 0) {
			driver = LaunchBrowsers.launchChrome();
			basic = new BasicUtils(driver);
			basic.justNavigate("https://jira.mediware.com");
			if (username != null && !username.equals("")) {
				basic.typeText(By.id("login-form-username"), username);
				basic.typeText(By.id("login-form-password"), password);
				basic.click(By.id("login"));
			} else
				Thread.sleep(10000);
			basic.click(By.id("browse_link"));
			basic.click(By.xpath(
					"//div[@id='browse_link-content']/descendant::a[contains(text(),'" + defaultProjectName + "')]"));
		}
		testCaseId = "";
		try {
			basic.click(By.id("create_link"));
			if (firstTest == 0) {
				basic.click(By.id("issuetype-field")).typeText(By.id("issuetype-field"), "Test");
				basic.click(By.xpath("//h2[@title='Create Issue']"));
				Thread.sleep(1000);
			}
			firstTest++;
			printToLogsAndConsole("Entering details");			
			basic.typeText(By.id("summary"), t.summary);			
			if (!t.testScreenName.isEmpty()) {
				basic.click(By.id("aui-uid-3"));
				basic.typeText(By.id("description"), t.testScreenName);
			}			
			basic.typeText(By.xpath("//label[contains(text(),'Story Points')]/following-sibling::input"), "0");			
			if (!t.assignee.isEmpty()) {
				basic.typeText(By.id("assignee-field"), t.assignee);
				Thread.sleep(2000);
				basic.pressEnter();
			}			
			basic.typeText(By.id("labels-textarea"), t.label);
			basic.select(By.xpath("//label[contains(text(),'Test Type')]/following-sibling::select"), t.testType);
			if (!t.currentSprint.isEmpty()) {
				basic.typeText(By.id("customfield_10000-field"), t.currentSprint);
				Thread.sleep(2000);
				basic.pressEnter();
			}
			Thread.sleep(500);
			if (t.issueRelatesTo.length != 0) {
				for (int i = 0; i < t.issueRelatesTo.length; i++) {
					basic.typeText(By.id("issuelinks-issues-textarea"), t.issueRelatesTo[i]);
					Thread.sleep(1000);
					basic.pressEnter();
				}
			}
			if (!t.components.isEmpty()) {
				basic.typeText(By.id("components-textarea"), t.components);
			}
			for (int i = 0; i < t.testSteps.size(); i++) {
				basic.typeText(By.xpath(
						"//table[@id='teststep-table']/tbody[contains(@class,'create')]/tr[1]/descendant::textarea[1]"),
						t.testSteps.get(i));
				basic.typeText(By.xpath(
						"//table[@id='teststep-table']/tbody[contains(@class,'create')]/tr[1]/descendant::textarea[3]"),
						t.expectedResult.get(i));
				Thread.sleep(500);
				if ((!t.url.isEmpty()) && i == 0) {
					basic.typeText(By.xpath(
							"//table[@id='teststep-table']/tbody[contains(@class,'create')]/tr[1]/descendant::textarea[2]"),
							t.url);
					Thread.sleep(500);
				}
				basic.click(By.xpath("//input[@class='aui-button' and @value='Add']"));
				Thread.sleep(500);
			}
			Thread.sleep(1500);
			if (boolHitCreateAfterEveryTest)
				basic.click(By.id("create-issue-submit"));
			else
				basic.wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("create-issue-submit")));
			Thread.sleep(500);
			testCaseId = basic.wait
					.until(ExpectedConditions
							.visibilityOfElementLocated(By.xpath("//a[contains(@class,'issue-created-key')]")))
					.getAttribute("data-issue-key");
			successfullyUploaded++;
			e = new Excel(filePath);
			e.getSheet(sheetName);
			setMapping();
			setUploadValue(t.summary, "TRUE", testCaseId);
			FileOutputStream outputStream = new FileOutputStream(filePath);
			e.workbook.write(outputStream);
			e.closeWorkbook();
		} catch (UnhandledAlertException e1) {
			errorInUploading++;
			basic.waitForAlertAndAccept();
			basic.wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("create-issue-submit")));
			Thread.sleep(500);
			e = new Excel(filePath);
			e.getSheet(sheetName);
			setMapping();
			setUploadValue(t.summary, "CANCELLED", "");
			FileOutputStream outputStream = new FileOutputStream(filePath);
			e.workbook.write(outputStream);
			e.closeWorkbook();
		} catch (WebDriverException wde) {
			if (wde.getMessage().contains("reachable")) {
				System.exit(0);
			}
		} catch (Exception e1) {
			errorInUploading++;
			e = new Excel(filePath);
			e.getSheet(sheetName);
			setMapping();
			setUploadValue(t.summary, "ERROR", "");
			FileOutputStream outputStream = new FileOutputStream(filePath);
			e.workbook.write(outputStream);
			e.closeWorkbook();
			JOptionPane.showMessageDialog(null,
					"Some exception occured, please add steps for this test case manually and hit 'Create' button!");
		}
	}

	public static void setUploadValue(String testName, String value, String testCaseId) {
		for (int i = 1; i < e.sheet.getPhysicalNumberOfRows(); i++) {
			if (e.getStringCellValue(i, summaryMapping).equalsIgnoreCase(testName)) {
				try {
					e.sheet.getRow(i).getCell(uploadStatus).setCellValue(value);
				} catch (Exception e1) {
					e.sheet.getRow(i).createCell(uploadStatus).setCellValue(value);
				}
				try {
					e.sheet.getRow(i).getCell(zephyrTestCaseIDMapping).setCellValue(testCaseId);
				} catch (Exception e1) {
					e.sheet.getRow(i).createCell(zephyrTestCaseIDMapping).setCellValue(testCaseId);
				}
				break;
			}
		}
	}

	public static void setMapping() throws Exception {
		for (int i = 0; i < e.sheet.getRow(0).getPhysicalNumberOfCells(); i++) {
			if (e.getStringCellValue(0, i).toLowerCase().trim().equals("title"))
				summaryMapping = i;
			if (e.getStringCellValue(0, i).toLowerCase().trim().equals("gwt"))
				gwtMapping = i;
			if (e.getStringCellValue(0, i).toLowerCase().trim().equals("label"))
				labelMapping = i;
			if (e.getStringCellValue(0, i).toLowerCase().trim().equals("testtype"))
				testTypeMapping = i;
			if (e.getStringCellValue(0, i).toLowerCase().trim().equals("uploadstatus"))
				uploadStatus = i;
			if (e.getStringCellValue(0, i).toLowerCase().trim().equals("description"))
				testScreenNameMapping = i;
			if (e.getStringCellValue(0, i).toLowerCase().trim().equals("jiratestcaseid"))
				zephyrTestCaseIDMapping = i;
			if (e.getStringCellValue(0, i).toLowerCase().trim().equals("currentsprint"))
				currentSprintMapping = i;
			if (e.getStringCellValue(0, i).toLowerCase().trim().equals("assignee"))
				assigneeMapping = i;
			if (e.getStringCellValue(0, i).toLowerCase().trim().equals("url"))
				urlMapping = i;
			if (e.getStringCellValue(0, i).toLowerCase().trim().equals("issuerelatesto"))
				issueRelatesToMapping = i;
			if (e.getStringCellValue(0, i).toLowerCase().trim().equals("components"))
				componentsMapping = i;
		}
		if (summaryMapping == -1)
			throw new Exception("Please add 'Title' column in excel sheet");
		if (gwtMapping == -1 && runWith.equalsIgnoreCase("BROWSER"))
			throw new Exception("Please add 'GWT' column in excel sheet which will contain all GWT steps");
		if (labelMapping == -1)
			throw new Exception("Please add 'Label' column in excel sheet");
		if (uploadStatus == -1)
			throw new Exception("Please add 'UploadStatus' column in excel sheet which will have TRUE of FALSE value");
		if (testScreenNameMapping == -1 && runWith.equalsIgnoreCase("BROWSER"))
			throw new Exception("Please add 'Description' column in excel sheet");
	}

	public static TestCase getTestCase(int rowNumber) {
		TestCase tc = new TestCase();
		String summary, testType = "", testScreenName;
		String gwtStatement = e.getStringCellValue(rowNumber, gwtMapping);
		String label = e.getStringCellValue(rowNumber, labelMapping);
		String upload = e.getStringCellValue(rowNumber, uploadStatus);
		String assignee = null, currentSprint = null, url = null;
		String[] issueRelatesTo;
		String components = null;
		String zephyrTestId;
		tc.gwtSteps = gwtStatement;
		try {
			zephyrTestId = e.getStringCellValue(rowNumber, zephyrTestCaseIDMapping);
		} catch (Exception e) {
		}
		tc.label = label;
		try {
			summary = e.getStringCellValue(rowNumber, summaryMapping);
			tc.summary = summary;
		} catch (Exception e) {
		}
		try {
			testType = e.getStringCellValue(rowNumber, testTypeMapping);
			tc.testType = testType;
		} catch (Exception e) {
		}
		try {
			testScreenName = e.getStringCellValue(rowNumber, testScreenNameMapping);
			tc.testScreenName = testScreenName;
		} catch (Exception e) {
		}
		try {
			summary = e.getStringCellValue(rowNumber, summaryMapping);
			tc.summary = summary;
		} catch (Exception e) {
		}
		try {
			url = e.getStringCellValue(rowNumber, urlMapping);
			tc.url = url;
		} catch (Exception e) {
		}
		try {
			assignee = e.getStringCellValue(rowNumber, assigneeMapping);
			tc.assignee = assignee;
		} catch (Exception e) {
		}
		try {
			currentSprint = e.getStringCellValue(rowNumber, currentSprintMapping);
			tc.currentSprint = currentSprint;
		} catch (Exception e) {
		}
		try {
			issueRelatesTo = e.getStringCellValue(rowNumber, issueRelatesToMapping).replace("\n", "").split(",");
			tc.issueRelatesTo = issueRelatesTo;
		} catch (Exception e) {
		}
		try {
			components = e.getStringCellValue(rowNumber, componentsMapping);
			tc.components = components;
		} catch (Exception e) {
		}
		if (upload.trim().toLowerCase().contains("tr") || upload.trim().contains("1"))
			return null;
		switch (testType) {
		case "None":
			tc.testType = "None";
			break;
		case "Functional":
			tc.testType = "Functional";
			break;
		case "Regression":
			tc.testType = "Regression";
			break;
		case "Automation":
			tc.testType = "Automation";
			break;
		case "Performance":
			tc.testType = "Performance";
			break;
		default:
			tc.testType = "Functional";
			break;
		}
		String currStmt = null, nxtStmt = null, currStep = null;
		StringBuffer singleTestStep = new StringBuffer(), singleExecutionResult = new StringBuffer();
		if (runWith.equalsIgnoreCase("BROWSER")) {
			String[] allSteps = gwtStatement.split("\n");
			for (int i = 0; i < allSteps.length; i++) {
				currStep = allSteps[i];
				if (currStep == null || currStep.trim().equals(""))
					continue;
				if (currStep.trim().toLowerCase().startsWith("given")) {
					currStmt = "given";
					singleTestStep.append(currStep.trim() + "\n");
				} else if (currStep.trim().toLowerCase().startsWith("when")) {
					currStmt = "when";
					if (!singleExecutionResult.toString().equalsIgnoreCase(""))
						tc.expectedResult.add(singleExecutionResult.toString().trim());
					singleExecutionResult.delete(0, singleExecutionResult.length());
					singleTestStep.append(currStep.trim() + "\n");
				} else if (currStep.trim().toLowerCase().startsWith("then")) {
					currStmt = "then";
					tc.testSteps.add(singleTestStep.toString().trim());
					singleTestStep.delete(0, singleTestStep.length());
					singleExecutionResult.append(currStep.trim() + "\n");
				}
				if (i != (allSteps.length - 1)) {
					if (allSteps[i + 1].trim().toLowerCase().startsWith("given"))
						nxtStmt = "given";
					else if (allSteps[i + 1].trim().toLowerCase().startsWith("when"))
						nxtStmt = "when";
					else if (allSteps[i + 1].trim().toLowerCase().startsWith("then"))
						nxtStmt = "then";
					else if (allSteps[i + 1].trim().toLowerCase().startsWith("and")) {
						if (currStmt.equalsIgnoreCase("given"))
							nxtStmt = "given";
						else if (currStmt.equalsIgnoreCase("when"))
							nxtStmt = "when";
						else if (currStmt.equalsIgnoreCase("then"))
							nxtStmt = "then";
					}
				} else {
					nxtStmt = null;
				}
				if (currStep.trim().toLowerCase().startsWith("then")) {
					if (nxtStmt == null)
						tc.expectedResult.add(singleExecutionResult.toString().trim());
				}
				if (currStep.trim().toLowerCase().startsWith("and")) {
					if (currStmt.trim().toLowerCase().startsWith("given")) {
						singleTestStep.append(currStep.trim() + "\n");
					} else if (currStmt.trim().toLowerCase().startsWith("when")) {
						singleTestStep.append(currStep.trim() + "\n");
					} else if (currStmt.trim().toLowerCase().startsWith("then")) {
						singleExecutionResult.append(currStep.trim() + "\n");
						if (nxtStmt == null)
							tc.expectedResult.add(singleExecutionResult.toString().trim());
					}
				}
			}
		}
		printToLogsAndConsole(tc.summary + "\n");
		return tc;
	}
}
