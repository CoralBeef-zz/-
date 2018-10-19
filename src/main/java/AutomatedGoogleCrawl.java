import org.apache.commons.io.FileUtils;
import org.apache.http.auth.AUTH;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.pagefactory.ByChained;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.awt.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutomatedGoogleCrawl {

    private Connection con = null;
    private PreparedStatement ps = null;
    private int bcounter = 0;
    private int RESULT_COUNTER = 0;
    private int STARTING_POINT = 0;
    private static final int STACK_SIZE = 50;

    private static final String PROXY_SERVER_IP = "108.59.14.200";
    private static final String PROXY_SERVER_PORT = "13152";
    private Proxy proxy;
    private boolean proxyDisabled = true;
    private boolean headlessEnabled = true;

    private String[] header_line = {};
    private static final ArrayList<String> RANDOM_WORDS = new ArrayList<>();
    private final ArrayList<HashMap<String, String>> TSV_DATA = new ArrayList<>();
    private static final GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
    private static final int SCREEN_WIDTH = gd.getDisplayMode().getWidth();
    private static final int SCREEN_HEIGHT = gd.getDisplayMode().getHeight();

    public static void main(String[] args) {
        new AutomatedGoogleCrawl(0, 28819);
    }

    public AutomatedGoogleCrawl(){}
    public AutomatedGoogleCrawl(int startAt, int endAt) {
        startCrawl(startAt, endAt);
    }

    public void startCrawl(int startAt, int endAt) {
        this.STARTING_POINT = startAt;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            String url = "jdbc:mysql:///crawlerling?useUnicode=true&characterEncoding=utf-8";

            con = DriverManager.getConnection(url,"root","");

            testGoogleSearch(startAt, endAt);
        } catch(SQLException | ClassNotFoundException exc) {
            System.out.println("ERROR: "+exc.toString());
        } finally {
            try {
                con.close();
            } catch(SQLException sqle) {}
        }
    }

    public void setHeadless(boolean isHeadless) {
        this.headlessEnabled = isHeadless;
    }

    private void testGoogleSearch(int startAt, int endAt) {

        System.out.println("Searching in "+workingDirectory()+ "data\\kyurica_mynavibaito.tsv");

        setUpProxy(PROXY_SERVER_IP, Integer.parseInt(PROXY_SERVER_PORT));
        getDataFromTSV();
        initChromeDriver();
        prepareRandomWords();
        prepareStatement();


        ChromeOptions options = new ChromeOptions();
        if(!proxyDisabled) options.addArguments("--proxy-server=http://" + (PROXY_SERVER_IP+":"+PROXY_SERVER_PORT) );
        if(headlessEnabled) options.addArguments("--headless");
        options.addArguments("--incognito");
        options.addArguments("--start-maximized");

        WebDriver driver = new ChromeDriver(options);
        driver.get("http://www.google.com/");

        boolean retry = false;

        try {
            for (int loop = startAt; loop < endAt; loop++) {

                if (doAtThisPercentChance(1)) {
                    driver.close();
                    driver.quit();
                    driver = new ChromeDriver(options);
                    driver.get("http://www.google.com/");
                }

                WebElement searchBox = driver.findElement(By.name("q"));

                searchBox.clear();
                if (doAtThisPercentChance(10)) {
                    searchBox.sendKeys(RANDOM_WORDS.get(generateRandom(RANDOM_WORDS.size())));
                    searchBox.submit();
                    loop--;
                } else {
                    searchBox.sendKeys(TSV_DATA.get(loop).get(header_line[1]));

                    searchBox.submit();

                    String sideBoxContent = searchInSideBox(driver);
                    String midBoxContent = searchInMidBox(driver);

                    if (!sideBoxContent.equals(""))
                        TSV_DATA.get(loop).put(header_line[7], sideBoxContent);
                    else if (!midBoxContent.equals(""))
                        TSV_DATA.get(loop).put(header_line[7], midBoxContent);

                    insertToDB(TSV_DATA.get(loop));
                }

                int minimumSearchDelay = 100, maximumSearchDelay = 400;
                pause(generateRandom(minimumSearchDelay, maximumSearchDelay));
            }

            System.out.println("Crawling Done!");
        } catch(Exception exc) {
            System.out.println("Error Detected! "+exc.toString());
            retry = true;
        } finally {
            try {
                ps.executeBatch();
                ps.close();
            } catch(SQLException exc) {
                System.out.println("END ERROR: "+exc.toString());
            }

            System.out.println("Searched from "+startAt+" to "+ (startAt+RESULT_COUNTER) );
            driver.close();
            driver.quit();

            if(retry) new AutomatedGoogleCrawl((startAt+RESULT_COUNTER),TSV_DATA.size());
        }

    }

    private String searchInSideBox(WebDriver driver) {
        try {
            By kgBox = By.cssSelector("#rhs_block > div > div.kp-blk.knowledge-panel > div > div.ifM9O > div:nth-child(2) > div");
            By phoneNumber = By.xpath("//*[starts-with(.,'Phone')]");

            WebElement el = driver.findElement(new ByChained(kgBox, phoneNumber));
            WebDriverWait wait = new WebDriverWait(driver, 20);
            wait.until(ExpectedConditions.visibilityOfAllElements(el));
            String PHONE_NUMBER = el.getText().replaceAll("Phone: ", "");

            return PHONE_NUMBER;
        } catch(Exception exc) {
            return "";
        }
    }

    private String searchInMidBox(WebDriver driver) {
        //#rso > div > div > div > div > div > div:nth-child(4) > div.ccBEnf > div > div > div > a > div > span > div:nth-child(1)
        try {
            By kgBox = By.cssSelector("#rso > div > div > div > div > div > div> div > div > div > div > a > div > span > div:nth-child(3)");
            List<WebElement> knowledgeGraphContents = driver.findElements(kgBox);
            String out = "";

            for (WebElement knowledgeGraphContent : knowledgeGraphContents) {
                //out += extractStringIfMatch(knowledgeGraphContent.getText(), "\\+?\\d+\\-\\d+-\\d+$") + " ";
                String extracted = extractStringIfMatch(knowledgeGraphContent.getText(), "\\+?\\d+\\-\\d+-\\d+$");
                if(!extracted.equals("")) return extracted;
            }
            return out;
        } catch(Exception exc) {
            return "";
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

    private boolean doAtThisPercentChance(int percentChance) {
        return new Random().nextInt(100) < percentChance;
    }

    private void pause(Integer milliseconds){
        try {
            TimeUnit.MILLISECONDS.sleep(milliseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private String extractStringIfMatch(String source, String regex) {
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(source);

        if (m.find()) return m.group(0);
        else return "";
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

    private void getDataFromTSV() {
        try {
            try (BufferedReader reader = new BufferedReader(
                    //new InputStreamReader(AutomatedGoogleCrawl.class.getResourceAsStream(
                    new FileReader(
                            workingDirectory()+ "data\\kyurica_mynavibaito.tsv" ))){
                String line;
                int line_counter = 0;

                while ((line = reader.readLine()) != null) {
                    String[] file_data = line.split("\t");
                    if(line_counter == 0) {
                        header_line = new String[file_data.length];
                        for (int loop = 0; loop < file_data.length; loop++)
                            header_line[loop] = file_data[loop];
                    }
                    else {
                        HashMap<String, String> local_data_temp = new HashMap<>();
                        for(int loop = 0; loop < file_data.length; loop++)
                            local_data_temp.put(header_line[loop], file_data[loop].replaceAll("\"", ""));
                        TSV_DATA.add(local_data_temp);
                    }
                    line_counter++;
                }
            }
        } catch(IOException exc) {
            System.out.println(exc.toString());
        }
    }

    private void insertToDB(HashMap<String, String> hashdata) {
        try {
            bcounter++;
            RESULT_COUNTER++;

            for (int loop = 0; loop < header_line.length; loop++) {
                if(loop==1) System.out.println("["+RESULT_COUNTER+"]["+(STARTING_POINT+RESULT_COUNTER)+"]: "+ hashdata.get(header_line[loop]) );
                ps.setString((loop + 1), hashdata.get(header_line[loop]));
            }

            if(bcounter >= STACK_SIZE) {
                ps.addBatch();
                ps.executeBatch();
                System.out.println("EXECUTE BATCH: "+ps.getFetchSize());
                ps.close();
                prepareStatement();
                bcounter = 0;
            } else ps.addBatch();
        } catch(SQLException exc) {
            System.out.println(exc.toString());
        }
    }

    private void prepareStatement() {
        try {
            String columns = "url,会社名,支店名,事業内容,都道府県,住所,企業url,電話番号,取得条件,抽出元媒体,業種（サイト内項目）";
            String sql = "INSERT INTO kyurica_mynavibaito (" + columns + ") VALUES (";
            String[] columnsx = columns.split(",");
            for (int l = 0; l < columnsx.length; l++) sql += (l != (columnsx.length - 1)) ? "?," : "?);";
            ps = con.prepareStatement(sql);
        } catch(SQLException exc) {
            System.out.println("Exception in statement creation! "+exc.toString());
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
        return workingDirectory+"\\DenwaBangouShutoku\\";
    }

    public synchronized Document getDocument(String url) {
        boolean recon = true;
        Document d = null;
        boolean alreadyPrinted = false;
        while(recon) {
            try {
                d = Jsoup.connect(url).timeout(0)
                        .proxy(this.proxy == null || proxyDisabled ? Proxy.NO_PROXY : this.proxy)
                        .userAgent("Mozilla/5.0 (Windows; U; Windows NT 6.1; it; rv:2.0b4) Gecko/20100818")
                        .get();
                recon=false;
            } catch(IOException se) {
                if(!alreadyPrinted) {
                    System.out.println("Socket Error Detected! Retrying..");
                    alreadyPrinted = true;
                }
            }
        }
        return d;
    }

    public synchronized void setUpProxy(String ipAddress, int port) {
        this.proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ipAddress, port));
    }

    private void prepareRandomWords() {

        Document randomPhraseDocument = getDocument("http://randomphrase.tripod.com/");
        Elements randomPhrases = randomPhraseDocument.select("#body > list > ul");
        for (Element randomPhrase : randomPhrases) {
            String randomPhraseString = randomPhrase.text().replaceFirst("^\\d+\\.+\\s", "");
            RANDOM_WORDS.add(randomPhraseString);
        }

        RANDOM_WORDS.add("Best Monitor Prices in Japan 2014");
        RANDOM_WORDS.add("日本語でモニターは何ですか");
        RANDOM_WORDS.add("FIFA Results");
        RANDOM_WORDS.add("Who is John Galt");
        RANDOM_WORDS.add("うんこ");
        RANDOM_WORDS.add("ミスチル新曲");
        RANDOM_WORDS.add("トイレの使い方");
        RANDOM_WORDS.add("Java　クローラー　作り方");
        RANDOM_WORDS.add("お断りメールの書き方");
    }
}
