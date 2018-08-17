package test;

import java.awt.event.KeyEvent;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

public class BasicUtils 
{
	WebDriver driver;
	WebDriverWait wait;
	
	public BasicUtils(WebDriver driver)
	{
		this.driver = driver;
		wait = new WebDriverWait(driver, 200);
	}
	
	public BasicUtils justNavigate(String url)
	{
		driver.get(url);
		return this;
	}
	
	public BasicUtils click(By by)
	{
		wait.until(ExpectedConditions.elementToBeClickable(by)).click();
		return this;
	}
	
	public BasicUtils pressEnter()
	{
		new Actions(driver).sendKeys(Keys.ENTER).build().perform();
		return this;
	}
	
	public BasicUtils typeText(By by, String text)
	{
		wait.until(ExpectedConditions.elementToBeClickable(by)).sendKeys(text);
		return this;
	}
	
	public String getText(By by)
	{
		String text = wait.until(ExpectedConditions.visibilityOfElementLocated(by)).getText();
		
		if(text==null || text.equalsIgnoreCase(""))
			text = wait.until(ExpectedConditions.visibilityOfElementLocated(by)).getAttribute("innerHTML");
		
		return text;
	}
	
	public BasicUtils select(By by, String text)
	{
		new Select(wait.until(ExpectedConditions.visibilityOfElementLocated(by))).selectByVisibleText(text);
		return this;
	}
	
	public BasicUtils waitForAlertAndAccept()
	{
		wait.until(ExpectedConditions.alertIsPresent()).accept();
		return this;
	}

	public BasicUtils switchToIframe(By iframe)
	{
		wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(iframe));
		return this;
	}
	
	public BasicUtils switchBackToParent()
	{
		driver.switchTo().defaultContent();
		return this;
	}
	
	public boolean isElementPresentAtThisMoment(By element)
	{
		try
		{
			driver.findElement(element);
			return true;
		}
		catch(Exception e)
		{
			return false;
		}
	}
}
