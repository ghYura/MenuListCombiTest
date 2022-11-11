import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Select;
import org.testng.*;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.*;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ClickListCombiTest_RememberUserSteps extends BaseTest {

    static Map<String, AtomicInteger> sharedHMpattern = new ConcurrentHashMap<>();
    static Map<String, AtomicInteger> sharedHMpatternPASSED = new ConcurrentHashMap<>();
    static Set<String> sharedSetPattern = ConcurrentHashMap.newKeySet();
    static Set<String> sharedSetPatternPASSED = ConcurrentHashMap.newKeySet();
    static Map<String, AtomicInteger> sharedHM = new ConcurrentHashMap<>();
    static Map<String, AtomicInteger> sharedHMPASSED = new ConcurrentHashMap<>();

    //If some timeout happens inside test-method - then try to retest:
    private ThreadLocal<Boolean> threadLocalRetest = ThreadLocal.withInitial(() -> false);
    private ThreadLocal<Long> threadLocalNewTout = ThreadLocal.withInitial(() -> Long.valueOf(25));

    //If some timeout happens inside test-method - then try to retest:
    private void retest(ITestContext context, long tout, Object... objs) {
        threadLocalRetest.set(true);
        threadLocalNewTout.set(tout);
        testCombinedLists(context, objs);
    }

    //Main combined lists test:
    @Test(dataProvider = "searchData", threadPoolSize = 4)
    public void testCombinedLists(ITestContext context, Object... objects) {
        Set<String> set = new HashSet<>();
        List<String> mainCombiList = new LinkedList<>();
        boolean isOptListsUpdated = false;
        List<String> updatedIdsList = new ArrayList<>();
        Set<String> skiptestSet = new HashSet<>();
        Set<String> skipPASSEDTestSet = new HashSet<>();
        StringBuilder sb = new StringBuilder();
        StringBuilder sbPass;
        for (Object object : objects) {
            String k2v = (String) object;
            sb.append(k2v);
            sb.append(",");
            mainCombiList.add(k2v);
        }
        sb.deleteCharAt(sb.lastIndexOf(","));
        sbPass = new StringBuilder(sb);

        if (context.getSuite().getAttribute("sharedHMpattern") != null) {
            sharedHMpattern = (Map<String, AtomicInteger>) context.getSuite().getAttribute("sharedHMpattern");
        }
        if (context.getSuite().getAttribute("sharedHMpatternPASSED") != null) {
            sharedHMpatternPASSED = (Map<String, AtomicInteger>) context.getSuite().getAttribute("sharedHMpatternPASSED");
        }
        if (context.getSuite().getAttribute("sharedHM") != null) {
            sharedHM = (Map<String, AtomicInteger>) context.getSuite().getAttribute("sharedHM");
        }
        if (context.getSuite().getAttribute("sharedHMPASSED") != null) {
            sharedHMPASSED = (Map<String, AtomicInteger>) context.getSuite().getAttribute("sharedHMPASSED");
        }

        if (context.getSuite().getAttribute("skiptestSet") != null) {
            ((Set<String>) context.getSuite().getAttribute("skiptestSet")).stream().filter(regex -> regex.contains(".*")).forEach(patt -> {
                if (Pattern.matches(patt, sb.toString()) && sharedHMpattern.get(patt).intValue() > Long.parseLong(context.getCurrentXmlTest().getParameter("thresholdGeneralizeSkipToAllMenus"))) {
                    throw new SkipException("Skipping this pattern exception");
                }
            });
        }
        if (context.getSuite().getAttribute("skipPASSEDTestSet") != null) {
            ((Set<String>) context.getSuite().getAttribute("skipPASSEDTestSet")).stream().filter(regex -> regex.contains(".*")).forEach(patt -> {
                if (Pattern.matches(patt, sbPass.toString()) && sharedHMpatternPASSED.get(patt).intValue() > Long.parseLong(context.getCurrentXmlTest().getParameter("thresholdGeneralizeSkipPASSEDToAllMenus"))) {
                    throw new SkipException("Skipping PASSED this pattern exception");
                }
            });
        }
        //Previously failed combinations to skip this following test-run (will be collected (if failed) to suite-attribute much more below):
        if (context.getSuite().getAttribute("skiptestSet") != null && context.getSuite().getAttribute("sharedHM") != null) {
            ((Set<String>) context.getSuite().getAttribute("skiptestSet")).stream().filter(notRegex -> !notRegex.contains(".*")).forEach(e -> {
                if (sb.toString().startsWith(e) && sharedHM.get(e).intValue() > Integer.parseInt(context.getCurrentXmlTest().getParameter("thresholdGeneralizeSkipToLastSingleMenuList"))) {
                    throw new SkipException("Skipping this exception");
                }
            });
        }
        if (context.getSuite().getAttribute("skipPASSEDTestSet") != null && context.getSuite().getAttribute("sharedHMPASSED") != null) {
            ((Set<String>) context.getSuite().getAttribute("skipPASSEDTestSet")).stream().filter(notRegex -> !notRegex.contains(".*")).forEach(e -> {
                if (sbPass.substring(0, sbPass.lastIndexOf("=")).startsWith(e) && sharedHMPASSED.get(e).intValue() > Integer.parseInt(context.getCurrentXmlTest().getParameter("thresholdGeneralizeSkipPASSEDToLastSingleMenuList"))) {
                    throw new SkipException("Skipping PASSED this exception");
                }
            });
        }
        try {//@BeforeMethod-like but replace with this super-class-call due to skip initialization of remote driver also for skipped following test runtime:
            remote_url = context.getCurrentXmlTest().getParameter("remoteUrl");
            super.setDriver(
                    context.getCurrentXmlTest().getParameter("url"),
                    Long.parseLong(context.getCurrentXmlTest().getParameter("initialTimeout(sec)")),
                    Long.parseLong(context.getCurrentXmlTest().getParameter("increaseTimeoutDeltaPer(sec)")),
                    Long.parseLong(context.getCurrentXmlTest().getParameter("unreasonableTimeoutOverhead(sec)")),
                    Long.parseLong(context.getCurrentXmlTest().getParameter("pollingFrequency(msec)")),
                    context.getCurrentXmlTest().getParameter("browser"),
                    Integer.parseInt(context.getCurrentXmlTest().getParameter("numberOfBrowsers")),
                    Boolean.parseBoolean(context.getCurrentXmlTest().getParameter("headless")),
                    context.getCurrentXmlTest().getParameter("login"),
                    context.getCurrentXmlTest().getParameter("password")
            );
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        sb.setLength(0);
        sbPass.setLength(0);
        long duration = (threadLocalRetest.get()) ?
                threadLocalNewTout.get()
                :
                Long.parseLong(context.getCurrentXmlTest().getParameter("initialTimeout(sec)"));
        threadLocalRetest.set(false);
        try {
            //Wait initialization:
            FluentWait<WebDriver> wait = new FluentWait<WebDriver>(driver.get())
                    .withTimeout(Duration.ofSeconds(duration))
                    .pollingEvery(Duration.ofMillis(Long.parseLong(context.getCurrentXmlTest().getParameter("pollingFrequency(msec)"))))
                    .ignoring(org.openqa.selenium.NoSuchElementException.class);

            //Element-web-lists ID (original and current being analized during runtime) initialization:
            Map<String, List<String>> id2listMapOriginal = new LinkedHashMap<>();
            Map<String, List<String>> id2listMap = new LinkedHashMap<>();

            //Check if web-lists ale finally loaded on page and present:
            wait.until(ExpectedConditions.and(
                    ExpectedConditions.presenceOfElementLocated(By.id("candidateSearch_jobTitle")),
                    ExpectedConditions.presenceOfElementLocated(By.id("candidateSearch_jobVacancy")),
                    ExpectedConditions.presenceOfElementLocated(By.id("candidateSearch_hiringManager")),
                    ExpectedConditions.presenceOfElementLocated(By.id("candidateSearch_status")),
                    ExpectedConditions.presenceOfElementLocated(By.id("candidateSearch_modeOfApplication")))
            );

            //Collecting elements to java-list:
            List<WebElement> elList = new LinkedList<>();
            WebElement jobTitle = driver.get().findElement(By.id("candidateSearch_jobTitle"));
            elList.add(jobTitle);
            WebElement jobVacancy = driver.get().findElement(By.id("candidateSearch_jobVacancy"));
            elList.add(jobVacancy);
            WebElement hiringManager = driver.get().findElement(By.id("candidateSearch_hiringManager"));
            elList.add(hiringManager);
            WebElement status = driver.get().findElement(By.id("candidateSearch_status"));
            elList.add(status);
            WebElement modeOfApplication = driver.get().findElement(By.id("candidateSearch_modeOfApplication"));
            elList.add(modeOfApplication);

            //Step to reset web-lists on page for All-state (if after page refresh previous test if(!) could affect current test runtime):
            if (Boolean.parseBoolean(context.getCurrentXmlTest().getParameter("resetLists")))
                elList.forEach(vari -> resetLists(wait, elList.stream().map(e -> e.getAttribute("id")).collect(Collectors.toList()), vari.getAttribute("id"), "All"));
            //Collecting original web-list-elements on page of their list-text()-values into id-to-javaLists map:
            elList.forEach(each -> id2listMapOriginal.put(each.getAttribute("id"), (new Select(driver.get().findElement(By.id(each.getAttribute("id"))))).getOptions().stream().map(WebElement::getText).collect(Collectors.toList())));

            //Core of the combi-test-logic (current combination taken from CSV and analyzing previous and other web-list-content reaction.
            //TEST-CASE: Every user selection step(s) should be saved on the webpage):
            for (int i = 0; i < mainCombiList.size(); i++) {
                String[] mapMas;
                String k2v = mainCombiList.get(i);
                sb.append(k2v);
                sb.append(",");
                sbPass.append(k2v);
                sbPass.append(",");
                mapMas = k2v.split("[=:]");
                try {
                    try {//Select current combi web-list-element step:
                        (new Select(driver.get().findElement(By.id(mapMas[0])))).selectByVisibleText(mapMas[1]);
                        //Check if all elements present after selection-action:
                        wait.until(ExpectedConditions.and(ExpectedConditions.presenceOfElementLocated(By.id("candidateSearch_jobTitle")),
                                ExpectedConditions.presenceOfElementLocated(By.id("candidateSearch_jobVacancy")),
                                ExpectedConditions.presenceOfElementLocated(By.id("candidateSearch_hiringManager")),
                                ExpectedConditions.presenceOfElementLocated(By.id("candidateSearch_status")),
                                ExpectedConditions.presenceOfElementLocated(By.id("candidateSearch_modeOfApplication"))));
                    } catch (org.openqa.selenium.WebDriverException wdExc) {
                        wdExc.printStackTrace();
                    }
                    if (i > 0) {
                        //If element options are updated after previous selection steps. Traversing by clicking on the updated options:
                        if (isOptListsUpdated) {
                            if (updatedIdsList.contains(mapMas[0])) {
                                String[] finalMapMas = mapMas;
                                WebDriver finalDriver = driver.get();
                                id2listMap.get(mapMas[0]).forEach(e -> {
                                    (
                                            new Select(finalDriver.findElement(By.id(finalMapMas[0])))).selectByVisibleText(e);
                                    elList.forEach(each -> id2listMapOriginal.put(each.getAttribute("id"), (new Select(driver.get().findElement(By.id(each.getAttribute("id"))))).getOptions().stream().map(WebElement::getText).collect(Collectors.toList())));
                                });
                            }
                            id2listMap.clear();
                            id2listMap.putAll(id2listMapOriginal);
                        }
                    }
                    if (i == 0) set.add(mapMas[0]);
                    try {
                        wait.until(ExpectedConditions.and(ExpectedConditions.presenceOfElementLocated(By.id("candidateSearch_jobTitle")),
                                ExpectedConditions.presenceOfElementLocated(By.id("candidateSearch_jobVacancy")),
                                ExpectedConditions.presenceOfElementLocated(By.id("candidateSearch_hiringManager")),
                                ExpectedConditions.presenceOfElementLocated(By.id("candidateSearch_status")),
                                ExpectedConditions.presenceOfElementLocated(By.id("candidateSearch_modeOfApplication"))));
                    } catch (org.openqa.selenium.WebDriverException wdExc) {
                        wdExc.printStackTrace();
                    }
                    elList.forEach(e ->
                            id2listMap.put(e.getAttribute("id"), getOptions(wait, elList.stream().map(e1 -> e1.getAttribute("id")).collect(Collectors.toList()), e.getAttribute("id")))
                    );
                    if (!id2listMap.equals(id2listMapOriginal)) {
                        //Element options are updated.
                        isOptListsUpdated = true;
                        //collecting IDs of updated web-lists:
                        id2listMap.keySet().forEach(e -> {
                            if (!id2listMapOriginal.get(e).equals(id2listMap.get(e))) {
                                updatedIdsList.add(e);
                            }
                        });
                    }
                    if (i > 0) {
                        //Iterating previous selection till not current:
                        for (int j = 0; j < i; j++) {
                            String[] mapMas2;
                            String k2v2 = mainCombiList.get(j);
                            mapMas2 = k2v2.split("[=:]");

                            if (!set.add(mapMas[0])) {
                                continue;//skipping
                            }
                            //CORE: if previous web-list choice step not memorized by webpage - current combi-test failed (collecting step combination):
                            if (!(new Select(driver.get().findElement(By.id(mapMas2[0]))).getFirstSelectedOption().getText().equals(mapMas2[1]))) {

                                if (context.getSuite().getAttribute("skiptestSet") != null) {
                                    skiptestSet = (Set<String>) context.getSuite().getAttribute("skiptestSet");
                                }
                                String localStr = sb.substring(0, sb.lastIndexOf("="));
                                String localStr2 = localStr.replaceAll("(=.*,\\s?)+", ".*") + ".*";
                                sharedSetPattern.add(localStr2);
                                skiptestSet.add(localStr2);

                                if (sharedHMpattern.containsKey(localStr2))
                                    sharedHMpattern.replace(localStr2, new AtomicInteger(sharedHMpattern.get(localStr2).incrementAndGet()));
                                else sharedHMpattern.putIfAbsent(localStr2, new AtomicInteger(0));

                                context.getSuite().setAttribute("sharedHMpattern", sharedHMpattern);
                                skiptestSet.add(sb.toString());

                                if (sharedHM.containsKey(localStr))
                                    sharedHM.replace(localStr, new AtomicInteger(sharedHM.get(localStr).incrementAndGet()));
                                else sharedHM.putIfAbsent(localStr, new AtomicInteger(0));
                                if (sharedHM.containsKey(localStr) && sharedHM.get(localStr).intValue() >= Integer.parseInt(context.getCurrentXmlTest().getParameter("thresholdGeneralizeSkipToLastSingleMenuList"))) {
                                    Set<String> refinedSet = skiptestSet.parallelStream().filter(e -> !e.startsWith(localStr)).collect(Collectors.toSet());
                                    skiptestSet.clear();
                                    skiptestSet.addAll(refinedSet);
                                    refinedSet.clear();
                                    skiptestSet.add(localStr);
                                }
                                context.getSuite().setAttribute("sharedHM", sharedHM);
                                context.getSuite().setAttribute("skiptestSet", skiptestSet);
                                sb.setLength(0);
                            } else {
                                if (context.getSuite().getAttribute("skipPASSEDTestSet") != null) {
                                    skipPASSEDTestSet = (Set<String>) context.getSuite().getAttribute("skipPASSEDTestSet");
                                }
                                String localStr = sbPass.substring(0, sbPass.lastIndexOf("="));
                                String localStr2 = localStr.replaceAll("(=.*,\\s?)+", ".*") + ".*";
                                sharedSetPatternPASSED.add(localStr2);

                                skipPASSEDTestSet.add(localStr2);
                                if (sharedHMpatternPASSED.containsKey(localStr2))
                                    sharedHMpatternPASSED.replace(localStr2, new AtomicInteger(sharedHMpatternPASSED.get(localStr2).incrementAndGet()));
                                else sharedHMpatternPASSED.putIfAbsent(localStr2, new AtomicInteger(0));

                                context.getSuite().setAttribute("sharedHMpatternPASSED", sharedHMpatternPASSED);
                                context.getSuite().setAttribute("skipPASSEDTestSet", skipPASSEDTestSet);

                                skipPASSEDTestSet.add(sbPass.substring(0, sbPass.lastIndexOf("=")));

                                if (sharedHMPASSED.containsKey(localStr))
                                    sharedHMPASSED.replace(localStr, new AtomicInteger(sharedHMPASSED.get(localStr).incrementAndGet()));
                                else sharedHMPASSED.putIfAbsent(localStr, new AtomicInteger(0));
                                if (sharedHMPASSED.containsKey(localStr) && sharedHMPASSED.get(localStr).intValue() >= Integer.parseInt(context.getCurrentXmlTest().getParameter("thresholdGeneralizeSkipPASSEDToLastSingleMenuList"))) {
                                    Set<String> refinedSet = skipPASSEDTestSet.parallelStream().filter(e -> !e.startsWith(localStr)).collect(Collectors.toSet());
                                    skipPASSEDTestSet.clear();
                                    skipPASSEDTestSet.addAll(refinedSet);
                                    refinedSet.clear();
                                    skipPASSEDTestSet.add(localStr);
                                }
                                context.getSuite().setAttribute("sharedHMPASSED", sharedHMPASSED);
                                context.getSuite().setAttribute("skipPASSEDTestSet", skipPASSEDTestSet);
                                sbPass.setLength(0);
                            }
                            //CORE: if previous web-list choice step not memorized by webpage - current combi-test failed (assertion):
                            Assert.assertEquals(new Select(driver.get().findElement(By.id(mapMas2[0]))).getFirstSelectedOption().getText(), mapMas2[1]);
                        }
                    }
                    //If web-element not fount at all by some reason:
                } catch (org.openqa.selenium.NoSuchElementException e1) {
                    e1.printStackTrace();
                    wait.until(ExpectedConditions.presenceOfElementLocated(By.id(mapMas[0])));
                    Assert.assertEquals((new Select(driver.get().findElement(By.id(mapMas[0])))).getFirstSelectedOption().getText(), "All");
                    (new Select(driver.get().findElement(By.id(mapMas[0])))).selectByVisibleText((new Select(driver.get().findElement(By.id(mapMas[0])))).getFirstSelectedOption().getText());
                    int finalI = i;
                    (new Select(driver.get().findElement(By.id(mapMas[0])))).getOptions().forEach(each -> {
                        each.click();
                        if (finalI > 0) {
                            for (int j = 0; j < finalI; j++) {
                                String[] mapMas2;
                                String k2v2 = mainCombiList.get(j);
                                mapMas2 = k2v2.split("[=:]");

                                wait.until(ExpectedConditions.presenceOfElementLocated(By.id(mapMas2[0])));
                                Assert.assertEquals(new Select(driver.get().findElement(By.id(mapMas2[0]))).getFirstSelectedOption().getText(), mapMas2[1]);
                            }
                        }
                    });
                }
            }
            //If timeout happened:
        } catch (org.openqa.selenium.TimeoutException timeExc) {
            timeExc.printStackTrace();
            if (threadLocalNewTout.get() >= Long.parseLong(context.getCurrentXmlTest().getParameter("unreasonableTimeoutOverhead(sec)")))
                Assert.fail("Due to unreasonable amount of timeout = " + threadLocalNewTout.get() + " - this test FAIL\n" + Arrays.deepToString(objects));
            else {
                System.out.println("trying to increase timeout time for " + Long.parseLong(context.getCurrentXmlTest().getParameter("increaseTimeoutDeltaPer(sec)")) + " unit for this particular test and rerun it");
                retest(context, threadLocalNewTout.get() + Long.parseLong(context.getCurrentXmlTest().getParameter("increaseTimeoutDeltaPer(sec)")), objects);
            }
            //If html-body was unparsable for some reason:
        } catch (org.openqa.selenium.WebDriverException wdExc) {
            if (wdExc.getMessage().contains("to parse")) retest(context, threadLocalNewTout.get(), objects);
        }
    }

    //Reset all lists to initial value state:
    private void resetLists(FluentWait<WebDriver> wdWait, List<String> idList, String id2select, String selectByGivenHereVisibleText) {
        while (true) {
            try {//Are all elements loaded on page and present? (due to each dependency):
                idList.forEach(e -> wdWait.until(ExpectedConditions.presenceOfElementLocated(By.id(e))));
                break;
            } catch (org.openqa.selenium.WebDriverException wdExc) {
                wdExc.printStackTrace();
            }
        }

        while (true) {
            try {//Is selectable element loaded and present?:
                wdWait.until(ExpectedConditions.presenceOfElementLocated(By.id(id2select)));
                (new Select(driver.get().findElement(By.id(id2select)))).selectByVisibleText(selectByGivenHereVisibleText);
                break;
            } catch (WebDriverException staleExcptn) {
                staleExcptn.printStackTrace();
            }
        }
    }

    //Get options list of given elementID:
    private List<String> getOptions(FluentWait<WebDriver> wdWait, List<String> idList, String id2select) {
        while (true) {
            try {//Are all elements loaded on page and present? (due to each other cross-dependency):
                idList.forEach(e -> wdWait.until(ExpectedConditions.presenceOfElementLocated(By.id(e))));
                break;
            } catch (org.openqa.selenium.WebDriverException parseExc) {
                parseExc.printStackTrace();
            }
        }
        List<String> list2return;
        while (true) {
            try {
                wdWait.until(ExpectedConditions.presenceOfElementLocated(By.id(id2select)));
                list2return = (new Select(driver.get().findElement(By.id(id2select)))).getOptions().stream().map(WebElement::getText).collect(Collectors.toList());
                break;
            } catch (WebDriverException staleExcptn) {
                staleExcptn.printStackTrace();
            }
        }
        return list2return;
    }

    //Combi-data input for test:
    @DataProvider(name = "searchData", parallel = true)
    public static Object[][] data(ITestContext context) throws IOException {
        return getDataFromCSV(context.getCurrentXmlTest().getParameter("inputCombiDataFilePath"));
    }

    //2-way read (forward and backward) combi-data lines from CSV-file:
    private static Object[][] getDataFromCSV(String fileNameroot) throws IOException {
        List<Object[]> records = new ArrayList<>();
        String record;
        String recordReversed;

        long lineCount, lineCountMiddle, curr = 0, currReversed = 0, overlap = 1;

        //finding unique first line in a file (UniqueNonMatchBelowCSVHeading):
        boolean skipFirstLine;
        String firstLine;
        String secondLine;
        try (Stream<String> stream = Files.lines(Path.of(fileNameroot), StandardCharsets.UTF_8)) {
            firstLine = stream.limit(1).collect(Collectors.joining());
        }
        try (Stream<String> stream = Files.lines(Path.of(fileNameroot), StandardCharsets.UTF_8)) {
            secondLine = stream.skip(1).limit(1).collect(Collectors.joining());
        }
        String[] fstArr = firstLine.split("[,:]\\s?");
        String[] sndArr = secondLine.split("[,:]\\s?");
        try (Stream<String> stream = Files.lines(Path.of(fileNameroot), StandardCharsets.UTF_8)) {
            skipFirstLine = stream.parallel().noneMatch(l -> (
                    l.split("[,:]\\s?")[0].contains(fstArr[0]) ||
                            (sndArr.length > 1) ?
                            l.split("[,:]\\s?")[sndArr.length - 1].contains(fstArr[0])
                            :
                            false
            ));
        }
        try (Stream<String> stream2 = Files.lines(Path.of(fileNameroot), StandardCharsets.UTF_8)) {
            lineCount = stream2.count();
        }
        if (skipFirstLine) lineCount--;
        lineCountMiddle = (long) Math.ceil(((double) lineCount) / 2d) + overlap;


        BufferedReader file = new BufferedReader(new InputStreamReader(new FileInputStream(fileNameroot), StandardCharsets.UTF_8));
        if (skipFirstLine) file.readLine();
        BufferedReader fileReversed = new BufferedReader(new InputStreamReader(new ReverseLineInputStream(new File(fileNameroot)), StandardCharsets.UTF_8));

        while ((curr < lineCountMiddle) && (currReversed < lineCountMiddle)) {
            record = file.readLine();
            recordReversed = fileReversed.readLine();
            String[] fields = record.split("[,:]\\s?");
            String[] fieldsReversed = recordReversed.split("[,:]\\s?");
            records.add(fields);
            records.add(fieldsReversed);
            ++curr;
            ++currReversed;
        }

        file.close();
        fileReversed.close();
        Object[][] results = new Object[records.size()][];
        for (int i = 0; i < records.size(); i++) {
            results[i] = records.get(i);
        }
        return results;
    }
}