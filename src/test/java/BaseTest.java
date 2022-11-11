import org.openqa.selenium.By;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.*;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;
import org.testng.Assert;
import org.testng.annotations.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class BaseTest {
    static List<RemoteWebDriver> driverList = new ArrayList<>();
    static ThreadLocal<RemoteWebDriver> driver = new ThreadLocal<>();
    static String remote_url = "http://192.168.56.1:4444/";
    private Capabilities capabilities;
    private static AtomicInteger atomicInteger = new AtomicInteger(0);
    private static ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();

    private static RemoteWebDriver createDriverFromSession(final String sessionId, URL command_executor) {
        CommandExecutor executor = new HttpCommandExecutor(command_executor) {
            @Override
            public Response execute(Command command) throws IOException {
                Response response;
                if (command.getName().equals("newSession")) {
                    response = new Response();
                    response.setSessionId(sessionId);
                    response.setStatus(0);
                    response.setValue(Collections.<String, String>emptyMap());
                } else {
                    response = super.execute(command);
                }
                return response;
            }
        };
        return driver.get();
    }

    @Parameters({"url", "initialTimeout(sec)", "increaseTimeoutDeltaPer(sec)", "unreasonableTimeoutOverhead(sec)", "pollingFrequency(msec)", "browser", "numberOfBrowsers", "headless", "login", "password"})
    //This row commented because should be avoided in case of previous combination failed, so no waste time for driver reassignment for the similar input-data combination:@BeforeTest(alwaysRun = true)////@BeforeMethod
    void setDriver(String url, long initialTimeoutSecs, long increaseTimeoutDeltaPerSecs, long unreasonableTimeoutOverheadSecs, long pollingFrequencymSecs, String browser, int numberOfBrowsers, boolean headless, String login, String password) throws MalformedURLException {

        if (atomicInteger.get() < numberOfBrowsers) {
            switch (browser) {
                case "firefox":
                    System.setProperty("webdriver.gecko.driver","C:\\geckodriver.exe");
                    capabilities = new FirefoxOptions(); ((FirefoxOptions) capabilities).setBinary("C:\\Program Files\\Mozilla Firefox\\firefox.exe"); if (headless) ((FirefoxOptions) capabilities).setHeadless(true);
                    break;
                case "chrome":
                    capabilities = new ChromeOptions(); if (headless) ((ChromeOptions) capabilities).setHeadless(true);
                    break;
                case "edge":
                    capabilities = new EdgeOptions(); if (headless) ((EdgeOptions) capabilities).setHeadless(true);
                    break;
            }
            driver.set(new RemoteWebDriver(new URL(remote_url), capabilities));
            queue.add((driver.get()).getSessionId().toString());
            driver.get().get(url);
            atomicInteger.set(atomicInteger.incrementAndGet());
            driverList.add(driver.get());
        } else {
            String sessionId = Objects.requireNonNull(queue.poll());
            driver.set(createDriverFromSession(sessionId, new URL(remote_url +"grid/api/testsession?session=" + sessionId)));
        }

        Wait<RemoteWebDriver> waitFluent = new FluentWait<>(driver.get())
                .withTimeout(Duration.ofSeconds(initialTimeoutSecs))
                .pollingEvery(Duration.ofMillis(pollingFrequencymSecs))
                .ignoring(NoSuchElementException.class);
        try {//
            waitFluent.until(ExpectedConditions.or(
                    ExpectedConditions.presenceOfElementLocated(By.id("logInPanelHeading")),
                    ExpectedConditions.presenceOfElementLocated(By.id("menu_recruitment_viewRecruitmentModule"))
            ));
        } catch (org.openqa.selenium.TimeoutException toutExc){
            toutExc.printStackTrace();
            closeBrowser(numberOfBrowsers);
            if (initialTimeoutSecs < unreasonableTimeoutOverheadSecs) setDriver(url, initialTimeoutSecs + increaseTimeoutDeltaPerSecs, increaseTimeoutDeltaPerSecs, unreasonableTimeoutOverheadSecs, pollingFrequencymSecs, browser, numberOfBrowsers, headless, login, password);
            else Assert.fail("Due to unreasonable amount of timeout = "+ initialTimeoutSecs +" - this test FAIL during initial login or base menu item to test\n");
        }//

        try {
            driver.get().findElement(By.id("menu_recruitment_viewRecruitmentModule")).click();
        } catch (NoSuchElementException nse) {
            driver.get().findElement(By.id("logInPanelHeading"));
            Login lp = PageFactory.initElements(driver.get(), Login.class);
            lp.loginbutton(login, password);
        }
        driver.get();
    }

    @Parameters({"numberOfBrowsers"})
    @AfterMethod
    public void closeBrowser(int numberOfBrowsers) {
        if (atomicInteger.intValue() >= numberOfBrowsers) queue.add((driver.get()).getSessionId().toString());
    }

/*
    WebDriver getDriver() {
        return driver.get();
    }

    @BeforeSuite(alwaysRun = true)
    public static void setup() {
        Runtime.getRuntime().addShutdownHook(new Thread(BaseTest::tearDown));
    }
*/
    @AfterTest(alwaysRun = true)
    public void afterTest(){
        driverList.forEach(drvr -> {if (drvr != null) drvr.quit();});
    }
/*
    @AfterSuite(alwaysRun = true)
    public static void tearDown() {
        synchronized (lock) {
            if (cleanedUp) return;
            driverList.forEach(drvr -> {if (drvr != null) drvr.quit();});
            cleanedUp = true;
        }
    }
*/
}