import org.apache.commons.io.FileUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Test {

    String proxyString = "108.59.14.200:13152";
    Proxy proxy;

    public static void main(String[] args) throws AWTException{
        new Test().testGoogleSearch();
    }

    public void testGoogleSearch() throws AWTException {
        initChromeDriver();

        ChromeOptions options = new ChromeOptions();//.addArguments("--proxy-server=http://" + proxyString);
        //options.addArguments("--headless");
        options.addArguments("--incognito");
        options.addArguments("--start-maximized");

        String[] testnames = {"株式会社ニッソーネット","UTエイム株式会社"};

        WebDriver driver = new ChromeDriver(options);
        driver.get("http://www.google.com/");

        for(String testname : testnames) {
            WebElement searchBox = driver.findElement(By.name("q"));
            searchBox.clear();
            searchBox.sendKeys(testname);
            searchBox.submit();

            String sel = "#rso > div:nth-child(2) > div > div >" +
                    " div.AEprdc.vk_c > div > div:nth-child(4) > div > div.r-i_XYgHl8o4Dc > div >" +
                    " div > a.C8TUKc.rllt__link.a-no-hover-decoration > div > span > div:nth-child(3)";

            String sel2 = "#rhs_block > div > div.kp-blk.knowledge-panel.Wnoohf.OJXvsb > div >" +
                    " div.ifM9O > div:nth-child(2) > div.SALvLe.farUxc.mJ2Mod > div > div:nth-child(4) >" +
                    " div > div > span.LrzXr.zdqRlf.kno-fv > span > span";

            /*WebElement el = driver.findElement(By.cssSelector(sel2));
            WebDriverWait wait = new WebDriverWait(driver, 10);
                    wait.until(ExpectedConditions.visibilityOfAllElements(el));



            String s = el.getText();
            //String s = (new WebDriverWait(driver, 10))
            //        .until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(sel2))).getText();
            System.out.println(s);*/

            int min = 3000, max = 7000;
            int waitFor = generateRandom(min, max);


            fakeUserMovement();
            System.out.println("Waiting for "+waitFor+" seconds");
            pause(waitFor);
        }

        driver.close();
        driver.quit();
    }

    private void fakeUserMovement() {
        try {
            Robot r = new Robot();//construct a Robot object for default screen
            int goToX = generateRandom(1920);
            int goToY = generateRandom(1020);
            Point currentMouseLocation = MouseInfo.getPointerInfo().getLocation();

            for(int mouseGlideLoop = 0; mouseGlideLoop < generateRandom(1,4); mouseGlideLoop++)
                mouseGlide((int)(currentMouseLocation.getX()),
                    (int)(currentMouseLocation.getY()),
                    goToX, goToY, generateRandom(1,4), generateRandom(3000));
        } catch(AWTException exc) {
            System.out.println("Robot Exception!");
        }

    }

    private void mouseGlide(int x1, int y1, int x2, int y2, int t, int n) {
        try {
            Robot r = new Robot();
            double dx = (x2 - x1) / ((double) n);
            double dy = (y2 - y1) / ((double) n);
            double dt = t / ((double) n);
            for (int step = 1; step <= n; step++) {
                Thread.sleep((int) dt);
                r.mouseMove((int) (x1 + dx * step), (int) (y1 + dy * step));
            }
        } catch (AWTException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private int generateRandom(int max) {
        return new Random().nextInt(max);
    }

    private int generateRandom(int min, int max) {
        return new Random().nextInt((max - min) + 1) + min;
    }

    private void pause(Integer milliseconds){
        try {
            TimeUnit.MILLISECONDS.sleep(milliseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private synchronized void initChromeDriver() {
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            URL resource = classLoader.getResource("drivers/chromedriver.exe");
            File directoryMaker = new File(workingDirectory() + "drivers/");
            if (!directoryMaker.exists()) directoryMaker.mkdirs();

            File chromeDriver = new File(workingDirectory() + "drivers/chromedriver.exe");
            if (!chromeDriver.exists()) {
                chromeDriver.createNewFile();
                FileUtils.copyURLToFile(resource, chromeDriver);
            }
            System.setProperty("webdriver.chrome.driver", chromeDriver.getAbsolutePath());
        } catch(IOException exc) {
            System.out.println("Error: "+exc.toString());
        }
    }

    private synchronized String workingDirectory () {
        String workingDirectory;
        String OS = (System.getProperty("os.name")).toUpperCase();
        if (OS.contains("WIN")) workingDirectory = System.getenv("AppData");
        else {
            workingDirectory = System.getProperty("user.home");
            workingDirectory += "/Library/Application Support";
        }
        return workingDirectory+"/DenwaBangouShutoku/";
    }
}
