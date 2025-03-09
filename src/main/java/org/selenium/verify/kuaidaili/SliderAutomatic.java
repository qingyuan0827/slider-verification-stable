package org.selenium.verify.kuaidaili;

import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.NoSuchElementException;
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
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;

@Slf4j
public class SliderAutomatic implements Closeable {

    private WebDriver driver;

    private JavascriptExecutor js;

    APIClient apiClient;
    CrackSlider cs;

    // 浏览器缓存地址
    private String browserTempDirect = "C:\\Users\\yuan\\AppData\\Local\\Google\\Chrome\\User Data\\Default\\kuaidaili";

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
    private String targetUrl = "https://www.kuaidaili.com/regist/";

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
        //openRegistrationWindow();
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
//                closeRegistrationWindow();
//                openRegistrationWindow();
                vcodeBtnDisableCheckedCount = 0;
            }
            // 刷新页面
            close();
            init();
            //driver.navigate().refresh();
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

    private void openRegistrationWindow() {
        WebElement loginButtonElement = driver.findElement(By.className("login-button"));
        loginButtonElement.click();

        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(2));
    }

    private void inputTelNumber() throws Exception {

        driver.switchTo().defaultContent();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(6));

        // 找到并点击下拉框以展开选项
        WebElement dropdownElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.className("select")
        ));
        dropdownElement.click();
        Thread.sleep(1000);
        // 等待下拉选项加载完成（可以使用显式等待）
        WebElement panel = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.className("popover2")
        ));

        // 找到并点击“855”选项
        // 在面板中查找目标选项（柬埔寨）
        WebElement option = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//li[.//span[contains(text(), 'Cambodia')]]")
        ));
        // 点击选项
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", option);
        Thread.sleep(1000);

        // 定位手机号输入框元素并输入内容
        WebElement phoneElement = driver.findElement(By.className("register-input"));
        //if (Strings.isNullOrEmpty(fetchedNumber)) {
            fetchedNumber = apiClient.fetchPhoneNumber();
            fetchNumberCount++;
        //}
        //String number = apiClient.fetchWithExistedNumber("383429409");
        // 使用键盘操作清空输入框内容
        phoneElement.sendKeys(Keys.CONTROL + "a");
        Thread.sleep(1000);
        phoneElement.sendKeys(Keys.DELETE);
        Thread.sleep(1000);
        phoneElement.sendKeys(fetchedNumber);
        log.info("输入手机号: " + fetchedNumber);

        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(1));
    }

    private void openSliderFrame() throws Exception {
        // 定位“获取验证码”链接并点击
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(6));
        WebElement getCodeElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("sendvcode_btn")
        ));        // 检查按钮是否被禁用
        while (getCodeElement.getAttribute("disabled") != null) {
            vcodeBtnDisableCheckedCount++;
            log.info("按钮被禁用，等待1分钟");
            Thread.sleep(60000); // 等待1分钟
            getCodeElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.id("sendvcode_btn")
            ));        }
        getCodeElement.click();
        Thread.sleep(1000);
        log.info("点击获取验证码链接");

        try {
            // 使用显式等待，等待 iframe 元素出现
            WebElement modalElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("div.layui-layer.layui-layer-page")
            ));
            // 切换到 iframe
            //driver.switchTo().frame(iframeElement);
            if (accessFrequencyErrorCount > 0) {
                log.info("重新正常执行，检测到访问太频繁错误信息" + accessFrequencyErrorCount + "次。");
                accessFrequencyErrorCount = 0;
            }
        } catch (TimeoutException e) {
            fetchVerifyCode(1);
            throw e;
            // 检查是否存在“访问太频繁，请稍后再试”的错误信息
//            List<WebElement> errorElements = driver.findElements(By.className("error-text"));
//            if (!errorElements.isEmpty()) {
//                if (++accessFrequencyErrorCount % 3 == 0) {
//                    throw new Exception("访问太频繁，请稍后再试。");
//                } else {
//                    //int waitMills = 1000 * 60 * 5;
//                    int waitMills = 1000 * 3; // 手工切换热点ip，则无需等待太长时间
//                    log.info("检测到访问太频繁错误信息" + accessFrequencyErrorCount + "次，等待" + waitMills/1000
//                            + "秒，已完成滑动验证" + successCount + "/" + count + "。");
//                    // 可以在这里添加适当的处理逻辑，比如等待一段时间后重试
//                    Thread.sleep(waitMills); // 等待5分支后重试
//                    openSliderFrame();
//                }
//            }
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
        while (true) {
            try {
                // 等待页面加载完成
                // 等待页面加载完成
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
//                WebElement modalElement = wait.until(ExpectedConditions.presenceOfElementLocated(
//                        By.cssSelector("div.layui-layer.layui-layer-page")
//                ));
                WebElement element = driver.findElement(By.cssSelector("div.layui-layer.layui-layer-page"));

                //wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".geetest_panel_box.geetest_panelshowslide")));

                // 定位背景图片的 <canvas> 元素
                WebElement canvasElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div#captcha canvas:first-child")));
                WebElement sliceElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div#captcha canvas.block")));


                // 检查 <canvas> 元素的状态
                /*JavascriptExecutor js = (JavascriptExecutor) driver;
                Long canvasWidth = (Long) js.executeScript("return arguments[0].width;", canvasElement);
                Long canvasHeight = (Long) js.executeScript("return arguments[0].height;", canvasElement);
                log.info("Canvas 宽度: " + canvasWidth + ", 高度: " + canvasHeight);

                String canvasContent = (String) js.executeScript("return arguments[0].toDataURL('image/png');", canvasElement);
                log.info("Canvas 内容: " + canvasContent);

                if (canvasContent == null || canvasContent.isEmpty() || canvasContent.equals("data:,")) {
                    log.error("<canvas> 元素内容为空或无效");
                    return false;
                }*/

                // 使用 JavaScript 将 <canvas> 转换为 Base64 图片数据
                String script = "return arguments[0].toDataURL('image/png').substring(22);";
                String base64Image = (String) js.executeScript(script, canvasElement);
                String base64ImageSlice = (String) js.executeScript(script, sliceElement);

                // 检查 Base64 数据是否有效
                if (base64Image == null || base64Image.isEmpty() || base64ImageSlice == null || base64ImageSlice.isEmpty()) {
                    log.error("Base64 数据为空");
                    return false;
                }

                // 将 Base64 数据解码为图片文件
                byte[] imageBytes = Base64.getDecoder().decode(base64Image);
                byte[] imageSliceBytes = Base64.getDecoder().decode(base64ImageSlice);

                if (imageBytes == null || imageBytes.length == 0 || imageSliceBytes == null || imageSliceBytes.length == 0) {
                    log.error("Base64 解码失败");
                    return false;
                }


                try (FileOutputStream fos = new FileOutputStream(Paths.get("src", "main", "resources", "img", "img1.jpg").toFile())) {
                    fos.write(imageBytes);
                }
                try (FileOutputStream fos = new FileOutputStream(Paths.get("src", "main", "resources", "img", "img2.jpg").toFile())) {
                    fos.write(imageSliceBytes);
                }

                // 去掉透明区域
                //String img2PathTmp = "E:\\auto-register\\slider-verification\\src\\main\\resources\\img\\img2tmp.jpg";
                String img2Path = "E:\\auto-register\\slider-verification\\src\\main\\resources\\img\\img2.jpg";
                cs.removeTransparentArea(img2Path,img2Path);

                //WebElement element = driver.findElement(By.id("captcha_verify_image"));
                getSliderImage(canvasElement);
                // 检查父元素是否可见
//                List<WebElement> errorPanels = driver.findElements(By.className("geetest_panel_error"));
//                if (!errorPanels.isEmpty()) {
//                    WebElement errorPanel = errorPanels.get(0);
//                    if (errorPanel.isDisplayed()) { // 判断父元素是否可见
//                        // 如果父元素可见，点击子元素
//                        WebElement errorElement = errorPanel.findElement(By.className("geetest_panel_error_content"));
//                        errorElement.click();
//                        System.out.println("检测到错误提示，已点击重试。");
//                    } else {
//                        System.out.println("错误提示未显示。");
//                    }
//                } else {
//                    System.out.println("未检测到错误提示。");
//                }
            } catch (NoSuchElementException e) {
                log.info("滑动校验通过!", e);
                return fetchVerifyCode(1);
                //close();
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

    public void moveSlider(WebElement element) {
        // 等待滑动轨道元素出现
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        WebElement track = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("div#nc_1_n1t.nc_scale")
        ));

        // 获取滑动轨道的宽度
        int trackWidth = track.getSize().getWidth();
        System.out.println("滑动轨道的宽度为: " + trackWidth + "px");


        // 等待滑块元素出现
        WebElement slider = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("span#nc_1_n1z.btn_slide")
        ));

        cs.slowly(driver, slider, trackWidth);

        try {
            // 让当前线程暂停执行3秒
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            // 处理线程被中断的情况
            e.printStackTrace();
        }
    }

    public void getSliderImage(WebElement element) {
        try {
            // 准备方法需要的入参
            // 1、找到滑动按钮位置
            //element = driver.findElement(By.xpath("//*[@id=\"vc_captcha_box\"]/div/div/div[4]/div/div[2]/div[2]"));
            element = driver.findElement(By.className("sliderBox"));
            WebElement iframeElement = driver.findElement(By.cssSelector("div.layui-layer.layui-layer-page"));

            // 2、图片位置（相对当前项目）
            //String imgPath = "img/img1.jpg";
            String img1Path = "E:\\auto-register\\slider-verification\\src\\main\\resources\\img\\img1.jpg";
            String img2Path = "E:\\auto-register\\slider-verification\\src\\main\\resources\\img\\img2.jpg";

            // 3、缺口像素长宽（长宽必须一致）
            //int gapWidth = 80;
            int gapWidth = 40;

            // 4、web图片宽度
            int webWidth = 278;

            // 5、原图片宽度
            int rawWidth = 280;
            //int rawWidth = 518;

            // 调用方法获取返回的移动距离
            //double dis = cs.getPos(element, img1Path, gapWidth, webWidth, rawWidth);
            double dis = cs.match(iframeElement, element, img1Path, img2Path, webWidth, rawWidth);
            //dis = dis - 5;

            // 打印一下移动距离
            log.info("dis=" + dis);

            //cs.move(driver, element, getMoveSteps((int) dis));
            //cs.simulateDragX(driver, element, dis);
            cs.slowly(driver, element, dis);

            try {
                // 让当前线程暂停执行5秒
                Thread.sleep(3000);
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

