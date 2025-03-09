package org.selenium.verify.juejin;

import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.selenium.verify.common.APIClient;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;

@Slf4j
public class SliderAutomatic implements Closeable {

    private WebDriver driver;

    private JavascriptExecutor js;

    APIClient apiClient;
    CrackSlider cs;

    // 浏览器缓存地址
    private String browserTempDirect = "C:\\Users\\yuan\\AppData\\Local\\Google\\Chrome\\User Data\\Default\\juejin";
    private String targetUrl = "https://juejin.cn/";

    String bgImgDir;
    long startTime;
    int tryCount;
    String fetchedNumber;
    int fetchNumberCount;
    int successCount;
    private static int totalSuccessCount;
    private int maxCount;
    //private static final int maxCount = 100;

    private static int accessFrequencyErrorCount;

    private int vcodeBtnDisableCheckedCount;

    public SliderAutomatic(int maxCount, String logDir) {
        bgImgDir = logDir + "img_" + Thread.currentThread().getName() + File.separator;
        this.maxCount = maxCount;
        this.apiClient = new APIClient();
        this.cs = new CrackSlider();
    }


    public Map<String, Object> start() {
        String elaspedTimeStr = null;
        browserTempDirect = browserTempDirect + File.separator + Thread.currentThread().getName();
        startTime = System.currentTimeMillis();
        log.info("执行开始。。。。");
        try {
            while (tryCount < maxCount) {
                try {
                    elaspedTimeStr = process();
                } catch (Exception e) {
                    log.error("发生异常，初始化并继续。", e);
                    tryCount++;
                    close();
                }
            }
            close();
        } finally {
            log.info("执行结束。。。。");
            // 使用 LinkedHashMap 保证顺序
            Map<String, Object> resultMap = new LinkedHashMap<>();
            resultMap.put("threadName", Thread.currentThread().getName());
            resultMap.put("tryCount", tryCount);
            resultMap.put("fetchNumberCount", fetchNumberCount);
            resultMap.put("successCount", successCount);
            resultMap.put("elaspedTimeStr", elaspedTimeStr);
            return resultMap;
        }
    }

    private String process() throws Exception {
        String elaspedTimeStr = null;
        log.info("开始登录");
        init();
        Random random = new Random();
        openRegistrationWindow();
        while (tryCount++ < maxCount) {
            inputTelNumber();
            long sleepMillis = random.nextInt(3000, 4000) + successCount;
            //long sleepMillis = random.nextInt(1000, 2000) + successCount;
            Thread.sleep(sleepMillis);
            //log.info("sleep " + sleepMillis);
            openSliderFrame();
            if (doSliderVerification()) {
                successCount++;
                totalSuccessCount++;
            } else {
                log.info("获取短信验证码失败!");
            }
            long elapsedSec = (System.currentTimeMillis() - startTime) / 1000;
            long elapsedMin = elapsedSec / 60;
            long elapsedHour = elapsedMin / 60;
            elaspedTimeStr = "耗时" + elapsedHour + "小时" + elapsedMin % 60 + "分" + elapsedSec % 60 + "秒";
            log.info("完成" + tryCount + "次滑动验证, 获取" + fetchNumberCount + "次手机号，成功" + successCount
                    + "次，" + elaspedTimeStr + ", 总共成功" + totalSuccessCount + "次");
            if (Strings.isNullOrEmpty(fetchedNumber) && vcodeBtnDisableCheckedCount > 0) {
                closeRegistrationWindow();
                openRegistrationWindow();
                vcodeBtnDisableCheckedCount = 0;
            }
            Thread.sleep(100);
        }
        return elaspedTimeStr;
    }

    @Override
    public void close() {
        if (driver != null) {
            driver.quit();
        }
    }

    private void init() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*");
        //String userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36";
        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36";
        options.addArguments("--user-agent=" + userAgent);
        options.addArguments("--disable-gpu");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--no-sandbox");
        //options.addArguments("--single-process");
        //options.addArguments("--disable-setuid-sandbox");
        // 启用自动化扩展
        options.setExperimentalOption("excludeSwitches", Arrays.asList("enable-automation"));
        options.addArguments("--disable-blink-features=AutomationControlled");
        // 禁用浏览器的安全性
        options.addArguments("--disable-web-security");
        options.addArguments("--allow-running-insecure-content");
        //禁用浏览器的同源策略
        options.addArguments("--disable-features=IsolateOrigins,site-per-process");

        options.addArguments("--user-data-dir=" + browserTempDirect);
        // 设置后台静默模式启动浏览器
        options.addArguments("--headless=new");

        log.info("设置请求头完成");
        driver = new ChromeDriver(options);

        driver.manage().window().maximize();
        js = (JavascriptExecutor) driver;
        js.executeScript("window.scrollTo(1,100)");
        // 先访问在在加载cookie 否则报错 invalid cookie domain
        driver.get(targetUrl);
        // 刷新页面
        driver.navigate().refresh();
    }

    private void openRegistrationWindow() {
        WebElement loginButtonElement = driver.findElement(By.className("login-button"));
        loginButtonElement.click();

        driver.manage().timeouts().implicitlyWait(java.time.Duration.ofSeconds(2));
    }

    private void inputTelNumber() throws Exception {
        driver.switchTo().defaultContent();
        // 找到并点击下拉框以展开选项
        WebElement dropdownElement = driver.findElement(By.cssSelector(".dropdown-input-container .arrow"));
        dropdownElement.click();
        // 等待下拉选项加载完成（可以使用显式等待）
        WebDriverWait waitDropdown = new WebDriverWait(driver, java.time.Duration.ofSeconds(3));
        waitDropdown.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".dropdown-pannel")));

        // 找到并点击“855”选项
        WebElement kuwaitOption = driver.findElement(By.xpath("//*[@id=\"juejin\"]/div[2]/div[3]/form/div[2]/div[1]/div[1]/div[1]/div/div[1]/div/div[2]/ul/li[91]"));
        kuwaitOption.click();
        // 定位手机号输入框元素并输入内容
        //Thread.sleep(1000);
        Thread.sleep(100);
        WebElement phoneElement = driver.findElement(By.cssSelector("input[name='mobile']"));
        if (Strings.isNullOrEmpty(fetchedNumber)) {
            fetchedNumber = apiClient.fetchPhoneNumber();
            fetchNumberCount++;
        }
        //String number = apiClient.fetchWithExistedNumber("383429409");
        // 使用键盘操作清空输入框内容
        phoneElement.sendKeys(Keys.CONTROL + "a");
        phoneElement.sendKeys(Keys.DELETE);
        Thread.sleep(100);
        phoneElement.sendKeys(fetchedNumber);
        log.info("输入手机号: " + fetchedNumber);

        driver.manage().timeouts().implicitlyWait(java.time.Duration.ofSeconds(1));
    }

    private void openSliderFrame() throws Exception {
        // 定位“获取验证码”链接并点击
        WebDriverWait wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(6));
        WebElement getCodeElement = wait.until(ExpectedConditions.presenceOfElementLocated(By.className("send-vcode-btn")));
        // 检查按钮是否被禁用
        while (getCodeElement.getAttribute("disabled") != null) {
            vcodeBtnDisableCheckedCount++;
            log.info("按钮被禁用，等待1分钟");
            Thread.sleep(60000); // 等待1分钟
            getCodeElement = wait.until(ExpectedConditions.presenceOfElementLocated(By.className("send-vcode-btn")));
        }
        getCodeElement.click();
        log.info("点击获取验证码链接");

        try {
            // 使用显式等待，等待 iframe 元素出现
            WebElement iframeElement = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[@id='captcha_container']/iframe")));
            // 切换到 iframe
            driver.switchTo().frame(iframeElement);
            if (accessFrequencyErrorCount > 0) {
                log.info("重新正常执行，检测到访问太频繁错误信息" + accessFrequencyErrorCount + "次。");
                accessFrequencyErrorCount = 0;
            }
        } catch (TimeoutException e) {
            //throw e;
            // 检查是否存在“访问太频繁，请稍后再试”的错误信息
            List<WebElement> errorElements = driver.findElements(By.className("error-text"));
            if (!errorElements.isEmpty()) {
                if (++accessFrequencyErrorCount % 3 == 0) {
                    throw new Exception("访问太频繁，请稍后再试。");
                } else {
                    int waitMills = 1000 * 60 * 5;
                    //int waitMills = 1000 * 3; // 手工切换热点ip，则无需等待太长时间
                    log.info("检测到访问太频繁错误信息" + accessFrequencyErrorCount + "次，等待" + waitMills/1000
                            + "秒，已完成滑动验证" + successCount + "/" + tryCount + "。");
                    // 可以在这里添加适当的处理逻辑，比如等待一段时间后重试
                    Thread.sleep(waitMills); // 等待5分支后重试
                    openSliderFrame();
                }
            }
        }
    }

    private void closeRegistrationWindow() {
        //WebElement closeButton = driver.findElement(By.xpath("//svg[@class='close-btn']"));
        //WebElement closeButton = driver.findElement(By.cssSelector("svg.close-btn"));
        driver.switchTo().defaultContent();
        // 找到包含 close-btn 的父元素
        WebElement authHeader = driver.findElement(By.cssSelector(".auth-header"));
        // 在父元素下找到 close-btn 元素
        WebElement closeButton = authHeader.findElement(By.cssSelector(".close-btn"));
        // 点击 close-btn 元素
        closeButton.click();

        driver.manage().timeouts().implicitlyWait(java.time.Duration.ofSeconds(2));
    }

    /**
     * 获取验证码，最多重试两次
     *
     * @param retryCount 重试次数
     * @return 是否成功获取验证码
     */
    private boolean fetchVerifyCode(int retryCount) {
        if (retryCount < 0) {
            return false; // 重试次数用尽，返回失败
        }

        try {
            Map.Entry<Integer, String> vCodeResponse = apiClient.getVerifyCode();
            Integer errorCode = vCodeResponse.getKey();
            String codeMsg = vCodeResponse.getValue();
            if (errorCode == 0 && !Strings.isNullOrEmpty(codeMsg)) {
                fetchedNumber = "";
                log.info(codeMsg);
                return true; // 成功获取验证码
            } else if (errorCode == 12) {
                // {"errno":12,"errmsg":"本次取号已释放,请重新取号"}
                fetchedNumber = "";
                return false;
            } else {
                // {"errno":20,"errmsg":"暂未收到短信内容."}
                // 暂停一段时间后重试
                Thread.sleep(retryCount == 1 ? 3000 : 2000); // 第一次重试等待3秒，第二次等待2秒
                return fetchVerifyCode(retryCount - 1); // 递归调用，减少重试次数
            }
        } catch (InterruptedException e) {
            // 处理线程被中断的情况
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private boolean doSliderVerification() throws Exception {
        long startRefreshBgTime = System.currentTimeMillis();
        long timeout = 1 * 60 * 1000; // 2 minutes in milliseconds

        while (true) {
            try {
                WebElement element = driver.findElement(By.id("captcha_verify_image"));
                refreshBg(false);
                getSliderImage(element);
            } catch (NoSuchElementException e) {
                log.info("滑动校验通过!", e);
                return fetchVerifyCode(1); // 最多重试两次
                //close();
            }
            // 检查是否超时
            if (System.currentTimeMillis() - startRefreshBgTime > timeout) {
                log.info("1分钟内未通过滑块验证，调用 refreshBg(true)");
                refreshBg(true);
                startRefreshBgTime = System.currentTimeMillis();
            }
        }
    }

    private void refreshBg(boolean force) {
        // 检查是否出现错误信息
        //By errorMessageLocator = By.cssSelector(".verify-message");
        By captchaSlider = By.className("captcha-slider-box");

        if (driver.findElements(captchaSlider).size() == 0 || force) {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            // 定位刷新按钮并点击
//            By refreshButtonLocator = By.className("vc-captcha-refresh");
//            WebElement refreshButton = wait.until(ExpectedConditions.elementToBeClickable(refreshButtonLocator));
//            refreshButton.click();
            WebElement refreshButton = driver.findElement(By.className("vc-captcha-refresh"));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", refreshButton);

            log.info("背景图片加载失败, 刷新。");
        }
    }

    public void getSliderImage(WebElement element) throws InterruptedException {
        WebDriverWait wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(6));
        String img1Path = bgImgDir + "img1.jpg";
        String img2Path = bgImgDir + "img2.jpg";

        String img1 = element.getAttribute("src");
        WebElement imgSlide = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("captcha-verify_img_slide")));
        String img2 = imgSlide.getAttribute("src");
        //String img2 = driver.findElement(By.id("captcha-verify_img_slide")).getAttribute("src");
        if (img2 != null && !img2.isEmpty()) {
            log.info("滑块验证图片下载路径: " + img2);
            //cs.downloadImage(img2, Paths.get("src", "main", "resources", bgImgDir, "img2.jpg"));
            cs.downloadImage(img2, Path.of(img2Path));
        }

        if (img1 != null && !img1.isEmpty()) {
            log.info("滑块背景图片下载路径: " + img1);
            //cs.downloadImage(img1, Paths.get("src", "main", "resources", bgImgDir, "img1.jpg"));
            cs.downloadImage(img1, Path.of(img1Path));
            try {
                // 准备方法需要的入参
                // 1、找到滑动按钮位置
                element = driver.findElement(By.xpath("//*[@id=\"vc_captcha_box\"]/div/div/div[4]/div/div[2]/div[2]"));

                // 2、图片位置（相对当前项目）
                //String imgPath = "img/img1.jpg";

                // 3、缺口像素长宽（长宽必须一致）
                //int gapWidth = 80;
                int gapWidth = 80;

                // 4、web图片宽度
                int webWidth = 340;

                // 5、原图片宽度
                int rawWidth = 552;
                //int rawWidth = 518;

                // 调用方法获取返回的移动距离
                //double dis = cs.getPos(element, img1Path, gapWidth, webWidth, rawWidth);
                double dis = cs.match(element, img1Path, img2Path, webWidth, rawWidth);
                dis = dis + 20;

                // 打印一下移动距离
                log.info("dis=" + dis);

                cs.slowly(driver, element, dis);

                try {
                    // 让当前线程暂停执行3秒
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    // 处理线程被中断的情况
                    e.printStackTrace();
                }
                //driver.quit();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            log.error("未找到背景图片 URL，刷新背景图片");
            // 定位“刷新”链接并点击
//            WebElement getCodeElement = driver.findElement(By.className("vc-captcha-refresh"));
//            getCodeElement.click();
            WebElement refreshButton = driver.findElement(By.className("vc-captcha-refresh"));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", refreshButton);
            // 让当前线程暂停执行3秒
            Thread.sleep(300);
        }
    }

}

