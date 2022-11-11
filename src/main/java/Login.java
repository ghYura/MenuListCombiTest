import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.How;

public class Login {
	@FindBy(how = How.ID, using = "txtUsername")
    WebElement User_name;
	@FindBy(how = How.ID, using = "txtPassword")
    WebElement pwd;
	@FindBy(how = How.ID, using = "btnLogin")
    WebElement login;
	
	public void loginbutton(String a, String b) {
		User_name.sendKeys(a);
		pwd.sendKeys(b);
		login.click();
	}
}
