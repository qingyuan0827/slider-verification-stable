package org.selenium.verify.ipfoxy;

import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.selenium.verify.common.APIClient;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class SliderAutomatic implements Closeable {

    private WebDriver driver;

    private JavascriptExecutor js;

    APIClient apiClient;
    CrackSlider cs;

    // 浏览器缓存地址
    private String browserTempDirect = "C:\\Users\\yuan\\AppData\\Local\\Google\\Chrome\\User Data\\Default\\ipfoxy";

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
    private String targetUrl = "https://app.ipfoxy.com/registration";

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
                // 同一个号码多次失败，则不继续尝试
                if (tryCount % 2 == 0) {
                    fetchedNumber = "";
                }
                int waitMills = 1000 * 60 * 1;
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
            //driver.quit();
        }
    }

    private void init() {
        EdgeOptions options = new EdgeOptions();
        options.addArguments("--remote-allow-origins=*");
        //String userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36";
        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36";
        options.addArguments("--user-agent=" + userAgent);
        options.addArguments("--disable-gpu");
//        options.addArguments("--disable-dev-shm-usage");
//        options.addArguments("--no-sandbox");
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
        //options.addArguments("--headless=new");

        // 设置代理服务器**********************************
        //options.addArguments("--proxy-server=http://v871.kdltps.com:15818");
        // 设置代理服务器**********************************

        log.info("设置请求头完成");
        driver = new EdgeDriver(options);

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
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        // 输入855，并按tab键切换到手机号输入框
        WebElement searchInput = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("input.el-input__inner")
        ));
        new Actions(driver)
                .click(searchInput)
                .keyDown(Keys.CONTROL)
                .sendKeys("a")
                .keyUp(Keys.CONTROL)
                .sendKeys(Keys.DELETE)
                .sendKeys("855")
                .perform();

        // 等待选项加载并点击
        WebElement option = new WebDriverWait(driver, Duration.ofSeconds(5))
                .ignoring(NoSuchElementException.class)
                .until(d -> d.findElement(By.xpath(
                        "//li[contains(@class,'el-select-dropdown__item')]" +
                                "//span[contains(text(),'柬埔寨(+855)')]"
                )));
//        new Actions(driver)
//                .moveToElement(option)
//                .pause(Duration.ofMillis(300)) // 等待动画效果
//                .click()
//                .perform();

        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].click();", option);
        Thread.sleep(1000);

        // 定位手机号输入框元素并输入内容
        WebElement phoneElement = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("div.phone-content input.el-input__inner")
        ));
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

        // 等待复选框元素可点击
//        WebElement checkbox = wait.until(ExpectedConditions.elementToBeClickable(
//                By.className("recaptcha-checkbox-borderAnimation")
//        ));
        // 勾选复选框
//        checkbox.click();
        new Actions(driver)
                .keyDown(Keys.TAB)
                .sendKeys(Keys.ENTER)
                .perform();

        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(1));
    }

    private void openSliderFrame() throws Exception {
        // 定位“获取验证码”链接并点击
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(6));
        WebElement getCodeElement = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("button._btnWrapper_omztw_23._info_omztw_126.send-code-button span")
        ));
        // 检查按钮是否被禁用
        while (getCodeElement.getAttribute("disabled") != null) {
            vcodeBtnDisableCheckedCount++;
            log.info("按钮被禁用，等待1分钟");
            Thread.sleep(1000*60); // 等待1分钟
            getCodeElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("button._btnWrapper_omztw_23._info_omztw_126.send-code-button span")
            ));
        }
        getCodeElement.click();
        Thread.sleep(1000);
        log.info("点击获取验证码链接");
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
        long startRefreshBgTime = System.currentTimeMillis();
        long timeout = 1 * 60 * 1000; // 2 minutes in milliseconds

        while (true) {
            try {
                //refreshBg(false);
                //getSliderImage();
                log.info("滑动校验通过!");
                return fetchVerifyCode(1); // 最多重试两次
                // 调用方法，判断 iframe 的 opacity 是否为 0
//                if (isIframeOpacityZero()){
//                    log.info("滑动校验通过!");
//                    return fetchVerifyCode(1); // 最多重试两次
//                } else {
//                    // 使用显式等待，等待 iframe 元素出现
//                    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(6));
//                    WebElement iframeElement = wait.until(ExpectedConditions.presenceOfElementLocated(
//                            By.id("tcaptcha_iframe_dy")));
//                    // 切换到 iframe
//                    driver.switchTo().frame(iframeElement);
//                }
            } catch (NoSuchElementException e) {
                log.info("滑动校验通过!", e);
                return fetchVerifyCode(1); // 最多重试两次
                //close();
            }
            // 检查是否超时
//            if (System.currentTimeMillis() - startRefreshBgTime > timeout) {
//                log.info("1分钟内未通过滑块验证，调用 refreshBg(true)");
//                refreshBg(true);
//                startRefreshBgTime = System.currentTimeMillis();
//            }
        }
    }

    /**
     * 判断指定 iframe 的 opacity 是否为 0
     *
     * @return 如果 opacity 为 0，返回 true；否则返回 false
     */
    public boolean isIframeOpacityZero() {
        // 切换到默认内容（确保操作在顶层文档）
        driver.switchTo().defaultContent();

        // 找到 tcaptcha_transform_dy 元素
        WebElement transformDiv = driver.findElement(By.id("tcaptcha_transform_dy"));

        // 获取 tcaptcha_transform_dy 的 opacity 值
        String opacityValue = transformDiv.getCssValue("opacity");

        // 判断 opacity 是否为 0
        return opacityValue.equals("0");
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

    public void getSliderImage() throws Exception {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(6));
        String img1Path = bgImgDir + "img1.jpg";
        String img2Path = bgImgDir + "img2.jpg";

        // 定位验证码背景元素
        WebElement element = new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.presenceOfElementLocated(
                        By.xpath("//div[@id='slideBg']")
                ));

        // 获取并处理样式属性
        String style = element.getAttribute("style");
        style = style.replace("&quot;", "\""); // 处理HTML转义字符

        // 正则匹配图片URL
        String regex = "background-image:\\s*url\\(\"(.*?)\"\\)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(style);
        String img1 = "";

        if (matcher.find()) {
            img1 = matcher.group(1)
                    .replace("&amp;", "&"); // 处理URL编码
        } else {
            log.error("未找到背景图片 URL");
        }

        // 提取 URL 地址
        String img2 = getSecondBackgroundUrl();

        //String img2 = driver.findElement(By.id("captcha-verify_img_slide")).getAttribute("src");
        if (img2 != null && !img2.isEmpty()) {
            log.info("滑块验证图片下载路径: " + img2);
            //cs.downloadImage(img2, Paths.get("src", "main", "resources", bgImgDir, "img2.jpg"));
            cs.downloadImage(img2, Path.of(img2Path));
            //cropImage(new File(img2), new File(img2));
        }

        if (img1 != null && !img1.isEmpty()) {
            log.info("滑块背景图片下载路径: " + img1);
            //cs.downloadImage(img1, Paths.get("src", "main", "resources", bgImgDir, "img1.jpg"));
            cs.downloadImage(img1, Path.of(img1Path));
            try {
                // 准备方法需要的入参
                // 1、找到滑动按钮位置
                element = driver.findElement(By.xpath("//div[@class='tc-fg-item tc-slider-normal']"));
                // 2、图片位置（相对当前项目）
                //String imgPath = "img/img1.jpg";

                // 3、缺口像素长宽（长宽必须一致）
                //int gapWidth = 80;
                int gapWidth = 80;

                // 4、web图片宽度
                int webWidth = 340;

                // 5、原图片宽度
                int rawWidth = 672;
                //int rawWidth = 518;

                // 调用方法获取返回的移动距离
                double dis = cs.getPos(element, img1Path, gapWidth, webWidth, rawWidth);
                //double dis = cs.match(element, img1Path, img2Path, webWidth, rawWidth);
                //dis = dis + 20;

                // 打印一下移动距离
                log.info("dis=" + dis);

                //cs.slowly(driver, element, dis);
                cs.quicklyMove(driver, element, dis);

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

    public String getSecondBackgroundUrl() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        // 定位第一个tc-fg-item元素
        WebElement targetElement = wait.until(driver -> {
            List<WebElement> elements = driver.findElements(By.cssSelector("div.tc-fg-item"));
            if (elements.size() >= 0) {
                // 根据样式特征验证第一个元素
                return elements.get(1);
            }
            return null;
        });

        // 获取背景图片URL
        String backgroundImage = targetElement.getCssValue("background-image");

        // 提取纯净URL
        return backgroundImage.replaceAll("^url\\(\"|\"\\)$", "").replace("&quot;", "\"");
    }

    public void cropImage(File sourceImage, File outputFile) throws Exception {
        // CSS参数（从元素获取）
        double bgPosX = -70.8333;
        double bgPosY = -247.917;
        double bgWidth = 345.06;
        double bgHeight = 313.69;
        double elementWidth = 60.7143;
        double elementHeight = 60.7143;

        // 加载原始图片
        BufferedImage original = ImageIO.read(sourceImage);

        // 计算实际裁剪坐标（考虑背景尺寸与图片实际尺寸的比例）
        double scaleX = original.getWidth() / bgWidth;
        double scaleY = original.getHeight() / bgHeight;

        // 转换背景定位坐标到实际像素坐标
        int actualX = (int) Math.round(-bgPosX * scaleX);
        int actualY = (int) Math.round(-bgPosY * scaleY);

        // 计算实际裁剪尺寸
        int cropWidth = (int) Math.round(elementWidth * scaleX);
        int cropHeight = (int) Math.round(elementHeight * scaleY);

        // 边界检查
        actualX = Math.max(0, Math.min(actualX, original.getWidth() - cropWidth));
        actualY = Math.max(0, Math.min(actualY, original.getHeight() - cropHeight));

        // 执行裁剪
        BufferedImage cropped = original.getSubimage(
                actualX,
                actualY,
                cropWidth,
                cropHeight
        );

        // 保存结果
        ImageIO.write(cropped, "jpg", outputFile);
    }
}

