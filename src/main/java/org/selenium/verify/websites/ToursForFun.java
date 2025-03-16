package org.selenium.verify.websites;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.selenium.verify.common.VerifyType;
import org.selenium.verify.exception.TooManyAccessException;
import org.selenium.verify.exception.VerificationException;

import java.io.IOException;
import java.time.Duration;

public class ToursForFun extends AbstractWebsite{

    public ToursForFun() {
        super();
    }

    @Override
    void setFailedBlockTime() {
        failedBlockTime = 30 * 1000L;
    }

    @Override
    void setUrl() {
        url = "https://cn.toursforfun.com/";
    }

    @Override
    void setVerifyType() {
        verifyType = VerifyType.SLIDER;
    }

    @Override
    protected void initLoginButton() {
        loginButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//h4//strong[contains(text(),'注册')]")
        ));
    }

    @Override
    protected void initInput() {
        //input = initElementByXpath("input","class","el-input__inner");
        input = wait.until(
                ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"my-login-system\"]/div[1]/div/div[2]/div/div[2]/div[2]/form/div[1]/div/div/input")));
    }

    @Override
    protected void initArea() {
        //area = initElementByClass("div","el-input el-input--suffix");
        area = initElementByClass("div","el-select my-login-system-form-country-code");
    }

    @Override
    protected void initJianpuzhai() {
//        jianpuzhai = wait.until(
//                ExpectedConditions.elementToBeClickable(By.xpath("//span[text()='柬埔寨(+855)']")));
        // 1. 定位国家选择输入框（更精准的定位）
        WebElement countryInput = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//div[contains(@class,'my-login-system-form-country-code')]//input")
        ));


        // 强制展开下拉框（关键修复）
        new Actions(driver)
                .sendKeys(Keys.ARROW_DOWN)
                .pause(Duration.ofMillis(800))
                .perform();

        // 优化后的定位策略
        By optionLocator = By.xpath(
                "//div[contains(@x-placement,'bottom')]//" +
                        "li[contains(@class,'el-select-dropdown__item') and not(contains(@style,'none'))]//" +
                        "span[contains(.,'柬埔寨(+855)')]"
        );

        // 复合等待条件
        WebElement option = new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(driver -> {
                    WebElement e = driver.findElement(optionLocator);
                    return (e.isDisplayed() && e.getSize().getHeight() > 0) ? e : null;
                });

        // 最终点击操作
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].click();",
                option
        );

    }

    @Override
    protected void initPrivacyCheckBox() {

    }

    @Override
    protected void initNextButton() {
        nextButton = initElementByText("span","获取验证码");
    }

    @Override
    void initImage() {
        image = initElementByXpath("img","alt","验证码背景");
        //backgroundImg = uid + "huawei_image.jpg";
        try {
            Thread.sleep(1000L);
            cracker.downloadImage(image.getAttribute("src"), backgroundImg);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        browserWidth = image.getSize().getWidth();
    }

    @Override
    void initSlideImg() {
        slide = initElementByClass("img","yidun_jigsaw");
        //slideImg = uid + "huawei_slide.jpg";
        try {
            Thread.sleep(1000L);
            cracker.downloadImage(slide.getAttribute("src"), slideImg);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    void initSlider() {
        slider = initElementByClass("div","yidun_slider  yidun_slider--hover ");
    }


    @Override
    void setInitLocation() {
        WebElement frameBody = initElementByClass("div","yidun_modal__body");
        initlocation = slider.getLocation().getX() - frameBody.getLocation().getX() - 18;
        System.out.println("起始位置："+initlocation);
    }

    @Override
    void verifySlider() throws VerificationException {
        iFrame = initElementByClass("div", "yidun_modal__wrap");
    }

    @Override
    protected void verifySlideResult() throws TooManyAccessException {
        boolean errorExist = true;
        try{
            initElementByText("span","您的操作过于频繁，请稍后再试");
        }
        catch (Exception e){
            errorExist = false;
        }
        if(errorExist){
            throw new TooManyAccessException("您的操作过于频繁，请稍后再试");
        }

    }
}
