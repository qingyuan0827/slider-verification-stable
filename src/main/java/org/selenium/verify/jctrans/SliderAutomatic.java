package org.selenium.verify.jctrans;

import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.selenium.verify.common.APIClient;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
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
    private String browserTempDirect = "C:\\Users\\yuan\\AppData\\Local\\Google\\Chrome\\User Data\\Default\\jctrans";
    private String targetUrl = "https://passport.jctrans.com/login?appId=EVT&path=%2F&click_source=undefined";

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
//            if(fetchVerifyCode(1)){ // 最多重试两次
//                successCount++;
//                totalSuccessCount++;
//            } else {
//                log.info("获取短信验证码失败!");
//            }
            if (doSliderVerification()) {
                successCount++;
                totalSuccessCount++;
            } else {
                // 同一个号码多次失败，则不继续尝试
                if (tryCount % 2 == 0) {
                    fetchedNumber = "";
                }
                int waitMills = 1000 * 60 * 5;
                // 获取验证码失败，休眠1分钟
                log.info("获取短信验证码失败，等待" + waitMills/1000 + "秒!");
                Thread.sleep(waitMills);
            }
            long elapsedSec = (System.currentTimeMillis() - startTime) / 1000;
            long elapsedMin = elapsedSec / 60;
            long elapsedHour = elapsedMin / 60;
            elaspedTimeStr = "耗时" + elapsedHour + "小时" + elapsedMin % 60 + "分" + elapsedSec % 60 + "秒";
            log.info("完成" + tryCount + "次滑动验证, 获取" + fetchNumberCount + "次手机号，成功" + successCount
                    + "次，" + elaspedTimeStr + ", 总共成功" + totalSuccessCount + "次");
//            if (Strings.isNullOrEmpty(fetchedNumber) && vcodeBtnDisableCheckedCount > 0) {
//                //closeRegistrationWindow();
//                // 刷新页面
//                driver.navigate().refresh();
//                openRegistrationWindow();
//                vcodeBtnDisableCheckedCount = 0;
//            }
            openRegistrationWindow();
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
        //options.addArguments("--disable-dev-shm-usage");
        //options.addArguments("--no-sandbox");
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

        // 设置代理服务器**********************************
        //options.addArguments("--proxy-server=http://v871.kdltps.com:15818");
        // 设置代理服务器**********************************

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

    private void openRegistrationWindow() throws InterruptedException {
        driver.navigate().refresh();

        // 等待目标选项卡出现
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        WebElement loginWindowLink = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("div.qrcode-guide-tooltip.tooltip-passport")));
        loginWindowLink.click();
        Thread.sleep(1000);

        WebElement phoneloginTab = wait.until(ExpectedConditions.elementToBeClickable(By.id("tab-second")));
        phoneloginTab.click();
        Thread.sleep(1000);

        // 定位“手机号登录”选项卡并点击
        WebElement mobileRadioButton = driver.findElement(By.xpath("//label[contains(@class, 'el-radio-button')]//span[contains(text(), '手机号')]"));
        mobileRadioButton.click();

        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(2));
    }

    private void inputTelNumber() throws Exception {
        driver.switchTo().defaultContent();
        // 找到并点击下拉框以展开选项
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        WebElement dropdownElement = wait.until(ExpectedConditions.elementToBeClickable(
                By.className("google-input-wrap-prepend")
        ));
        // 点击下拉框
        dropdownElement.click();
        Thread.sleep(1000);

        // 等待下拉选项加载完成（可以使用显式等待）
//        WebElement dropdownWrap = wait.until(ExpectedConditions.visibilityOfElementLocated(
//                By.cssSelector("div.el-select-dropdown__wrap.el-scrollbar__wrap")
//        ));
        WebElement dropdownWrap = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(".el-select-dropdown__item")
        ));


        // 找到并点击“855”选项
//        WebElement kuwaitOption = dropdownWrap.findElement(By.xpath(".//li[contains(text(), '855')]"));
//        kuwaitOption.click();
        // 将目标元素滚动到可见区域
        //WebElement kuwaitOption1 = driver.findElement(By.xpath(".//li[contains(text(), '+250: 卢旺达')]"));

        // 定位并点击“+855 柬埔寨”选项
        WebElement cambodiaOption = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath(".//li[contains(normalize-space(), '+855: 柬埔寨')]")
        ));
        // 滚动到元素位置
        scrollToElement(cambodiaOption);
        //scrollToElementSmoothly(cambodiaOption);

        // 点击元素
        cambodiaOption.click();
        Thread.sleep(1000);

        // 使用 JavaScript 执行点击操作
        clickElementUsingJS(cambodiaOption);

//        WebElement kuwaitOption = dropdownWrap.findElement(By.xpath(".//li[contains(text(), '+855: 柬埔寨')]"));
//        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", kuwaitOption);


        // 定位手机号输入框元素并输入内容
        //Thread.sleep(1000);
        Thread.sleep(100);
        if (Strings.isNullOrEmpty(fetchedNumber)) {
            fetchedNumber = apiClient.fetchPhoneNumber();
            fetchNumberCount++;
        }
        // 找到手机号码输入框
        WebElement phoneNumberInput = driver.findElement(By.xpath("//*[@id=\"pane-second\"]/form/div[2]/div/div/div/div[1]/div/div[2]/div/input"));

        // 清除输入框内容
        phoneNumberInput.clear();
        Thread.sleep(1000);

        // 输入手机号码
        phoneNumberInput.sendKeys(fetchedNumber);
        Thread.sleep(1000);

        // 模拟按下 Tab 键
        phoneNumberInput.sendKeys(Keys.TAB);
        //((JavascriptExecutor) driver).executeScript("arguments[0].dispatchEvent(new KeyboardEvent('keydown', {key: 'Tab'}));", phoneElement);
        log.info("输入手机号: " + fetchedNumber);

        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(1));
    }

    public void scrollToElementSmoothly(WebElement element) throws InterruptedException {
        JavascriptExecutor js = (JavascriptExecutor) driver;

        // 获取当前滚动位置
        Long currentY = (Long) js.executeScript("return window.pageYOffset;");

        // 获取元素的 bounding rectangle top
        Double top = (Double) js.executeScript("return arguments[0].getBoundingClientRect().top;", element);

        // 获取视窗高度
        Long viewportHeight = (long) driver.manage().window().getSize().getHeight();

        // 计算需要滚动的距离，使元素出现在视窗中间
        Long delta = Math.round(top - (viewportHeight / 2));

        int step = delta > 0 ? 100 : -100;

        long remaining = delta;

        int maxAttempts = 10;
        int attempts = 0;

        while (Math.abs(remaining) > Math.abs(step) && attempts < maxAttempts) {
            System.out.println("Before scroll: remaining = " + remaining + ", step = " + step);
            js.executeScript("window.scrollBy(0, " + step + ");");
            Thread.sleep(50);
            remaining -= step;
            currentY += step;

            // 重新获取 top 值，确保滚动到位
            top = (Double) js.executeScript("return arguments[0].getBoundingClientRect().top;", element);
            remaining = Math.round(top - (viewportHeight / 2));
            System.out.println("After scroll: remaining = " + remaining + ", top = " + top);
            attempts++;
        }

        System.out.println("Final scroll: remaining = " + remaining);
        js.executeScript("window.scrollBy(0, " + remaining + ");");
        Thread.sleep(50);
    }


    // 辅助方法：滚动到元素位置
    private void scrollToElement(WebElement element) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
    }

    // 辅助方法：使用 JavaScript 执行点击操作
    private void clickElementUsingJS(WebElement element) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
    }

    private void openSliderFrame() throws Exception {
        // 定位“获取验证码”链接并点击
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(6));
        WebElement getCodeElement = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("a.send-code.darkBlue")));
        // 检查按钮是否被禁用
        while (getCodeElement.getAttribute("disabled") != null) {
            vcodeBtnDisableCheckedCount++;
            log.info("按钮被禁用，等待1分钟");
            Thread.sleep(60000); // 等待1分钟
            getCodeElement = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("a.send-code.darkBlue")));
        }
        getCodeElement.click();
        log.info("点击获取验证码链接");
        Thread.sleep(1000);

//        try {
//            // 使用显式等待，等待 iframe 元素出现
//            WebElement iframeElement = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[@id='captcha_container']/iframe")));
//            // 切换到 iframe
//            driver.switchTo().frame(iframeElement);
//            if (accessFrequencyErrorCount > 0) {
//                log.info("重新正常执行，检测到访问太频繁错误信息" + accessFrequencyErrorCount + "次。");
//                accessFrequencyErrorCount = 0;
//            }
//        } catch (TimeoutException e) {
//            throw e;
//        }
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

        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(2));
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
            Integer errorCode = apiClient.getVerifyCode().getKey();
            String codeMsg = apiClient.getVerifyCode().getValue();
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
        } catch (Exception e) {
            fetchVerifyCode(retryCount - 1);
            return false;
            //throw new RuntimeException(e);
        }
    }


    private boolean doSliderVerification() throws Exception {
        long startRefreshBgTime = System.currentTimeMillis();
        long timeout = 1 * 60 * 1000; // 2 minutes in milliseconds

        Thread.sleep(1000);
        while (true) {
            // 检查是否超时
            if (System.currentTimeMillis() - startRefreshBgTime > timeout) {
                log.info("1分钟内未通过滑块验证，调用 refreshBg(true)");
                //refreshBg(true);
                startRefreshBgTime = System.currentTimeMillis();
            }
            //if (isElementHidden(By.cssSelector("div.verify-refresh[data-v-263d90aa]"), 10)){
            /*
            * <p class="el-message__content">验证码将会发送到您的手机，5分钟后失效。请不要与任何人分享您的验证码。</p>
            * */
            if (isElementPresent(By.cssSelector("p.el-message__content"), 7)) {
                log.info("滑动校验通过!");
                return fetchVerifyCode(1); // 最多重试两次
            } else {
                getSliderImage();
            }
        }
    }

    private boolean isElementPresent(By locator, int timeoutInSeconds) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutInSeconds));
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(locator));
            return true;
        } catch (TimeoutException e) {
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean isElementHidden(By locator, int timeoutInSeconds) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutInSeconds));
        try {
            WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
            String display = element.getCssValue("display");
            System.out.println("display:" + display);
            return "none".equalsIgnoreCase(display);
        } catch (TimeoutException e) {
            // 元素在指定时间内没有出现，可能也被认为是隐藏的
            System.out.println("TimeoutException e:" + e.getMessage());
            return true;
        } catch (StaleElementReferenceException e) {
            // 元素在等待期间变得不适用，可能需要重新查找
            return isElementHidden(locator, timeoutInSeconds);
        } catch (Exception e) {
            // 其他异常，可能需要根据具体情况处理
            e.printStackTrace();
            System.out.println("Exception e:" + e.getMessage());
            return false;
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

    public void getSliderImage() throws InterruptedException, IOException {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(6));
        String img1Path = bgImgDir + "img1.jpg";
        String img2Path = bgImgDir + "img2.jpg";
        Path destinationDir = Path.of(img1Path).getParent();
        // 确保目标目录存在
        Files.createDirectories(destinationDir);  // 创建 img 目录

        WebElement iframeElement = driver.findElement(By.className("verifybox"));


        try {
            // 找到所有带有 data-v-263d90aa 属性的 img 元素
            List<WebElement> imgElements = driver.findElements(By.cssSelector("img[data-v-263d90aa]"));

            // 遍历每个 img 元素
            for (int i = 0; i < imgElements.size(); i++) {
                System.out.println(imgElements.size() + ". 第 " + i + " 个图片的 src: " + imgElements.get(i).getAttribute("src"));
                WebElement imgElement = imgElements.get(i);
                String src = imgElement.getAttribute("src");

                // 检查 src 是否是 Base64 数据
                if (src != null && src.startsWith("data:image")) {
                    // 提取 Base64 数据部分
                    String base64Data = src.split(",")[1];

                    // 解码 Base64 数据
                    byte[] imageBytes = Base64.getDecoder().decode(base64Data);

                    // 保存图片到本地
                    String filePath = bgImgDir + "img" + (i+1) + ".jpg"; // 图片保存路径
                    FileUtils.writeByteArrayToFile(new File(filePath), imageBytes);
                    System.out.println("图片已保存到: " + filePath);
                } else {
                    System.out.println("第 " + i + " 个图片的 src 不是 Base64 数据");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 移除图片透明区域
        cs.removeTransparentArea(img2Path,img2Path);

        // 读取图片并获取宽度
//        Mat image = Imgcodecs.imread(img1Path);
//        int rawWidth = image.width();
//        System.out.println("原始图片宽度: " + rawWidth);
//
//        // 获取网页上图片显示的宽度
//        int webWidth = imgElement.getSize().getWidth();
//        System.out.println("网页上图片宽度: " + webWidth);

        try {
            // 准备方法需要的入参
            // 1、找到滑动按钮位置
            WebElement element = driver.findElement(By.className("verify-move-block"));

            // 2、图片位置（相对当前项目）
            //String imgPath = "img/img1.jpg";

            // 3、缺口像素长宽（长宽必须一致）
            //int gapWidth = 80;
            int gapWidth = 35;

            // 4、web图片宽度
            int webWidth = 330;

            // 5、原图片宽度
            int rawWidth = 330;
            //int rawWidth = 518;

            // 调用方法获取返回的移动距离
            //double dis = cs.getPos(iframeElement, element, img1Path, gapWidth, webWidth, rawWidth);
            double dis = cs.match(iframeElement, element, img1Path, img2Path, webWidth, rawWidth);
            //dis = dis + 20;

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
    }

    public void getSliderImage2() throws InterruptedException, IOException {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(6));
        String img1Path = bgImgDir + "img1.jpg";
        String img2Path = bgImgDir + "img2.jpg";
        Path destinationDir = Path.of(img1Path).getParent();
        // 确保目标目录存在
        Files.createDirectories(destinationDir);  // 创建 img 目录

        WebElement iframeElement = driver.findElement(By.className("verifybox"));


        // 1. 定位元素
        WebElement imgElement = driver.findElement(By.cssSelector("img[data-v-263d90aa]"));
        WebElement img2Element = driver.findElement(By.cssSelector("img[data-v-263d90aa]"));

        // 2. 获取src属性
        String src = imgElement.getAttribute("src");
        System.out.println("src: " + src);
        // 3. 提取base64数据
        String base64Data = src.split(",")[1];
        // 4. 解码base64数据
        byte[] imageBytes = Base64.getDecoder().decode(base64Data);
        log.info("滑块背景图片下载路径: " + img1Path);

        // 5. 保存图片到本地
        FileOutputStream fos = new FileOutputStream(img1Path);
        fos.write(imageBytes);
        fos.close();

        // 读取图片并获取宽度
        Mat image = Imgcodecs.imread(img1Path);
        int rawWidth = image.width();
        System.out.println("原始图片宽度: " + rawWidth);

        // 获取网页上图片显示的宽度
        int webWidth = imgElement.getSize().getWidth();
        System.out.println("网页上图片宽度: " + webWidth);

        try {
            // 准备方法需要的入参
            // 1、找到滑动按钮位置
            WebElement element = driver.findElement(By.className("verify-move-block"));

            // 2、图片位置（相对当前项目）
            //String imgPath = "img/img1.jpg";

            // 3、缺口像素长宽（长宽必须一致）
            //int gapWidth = 80;
            int gapWidth = 35;

            // 4、web图片宽度
            //int webWidth = 310;

            // 5、原图片宽度
            //int rawWidth = 330;
            //int rawWidth = 518;

            // 调用方法获取返回的移动距离
            double dis = cs.getPos(iframeElement, element, img1Path, gapWidth, webWidth, rawWidth);
            //double dis = cs.match(element, img1Path, img2Path, webWidth, rawWidth);
            //dis = dis + 20;

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
    }

}

