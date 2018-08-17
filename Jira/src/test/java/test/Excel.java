package test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class Excel 
{
	XSSFWorkbook workbook;
	XSSFSheet sheet;
	
	public Excel(String excelPath) throws Exception
	{
		workbook = new XSSFWorkbook(new FileInputStream(new File(excelPath)));
	}
	
	public void closeWorkbook() throws Exception
	{
		workbook.close();
	}
	
	public XSSFSheet getSheet(String sheetName)
	{
		sheet = workbook.getSheet(sheetName);
		return sheet;
	}
	
	public String getStringCellValue(int rowNum, int cellNum)
	{
		String cellValue = null;
		try
		{
			cellValue = sheet.getRow(rowNum).getCell(cellNum).getStringCellValue().trim();
		}
		catch(Exception e)
		{
			try
			{
				cellValue = sheet.getRow(rowNum).getCell(cellNum).getRawValue().trim();
			}
			catch(Exception e1)
			{
				return null;
			}
		}		
		return cellValue;
	}

}
