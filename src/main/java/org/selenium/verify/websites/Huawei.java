package org.selenium.verify.websites;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.selenium.verify.common.VerifyType;
import org.selenium.verify.exception.TooManyAccessException;
import org.selenium.verify.exception.VerificationException;

import java.io.IOException;
import java.time.Duration;

public class Huawei extends AbstractWebsite{

    public Huawei() {
        super();
    }

    @Override
    void setFailedBlockTime() {
        failedBlockTime = 30 * 1000L;
    }

    @Override
    void setUrl() {
        url = "https://id1.cloud.huawei.com/CAS/portal/loginAuth.html";
    }

    @Override
    void setVerifyType() {
        verifyType = VerifyType.SLIDER;
    }

    @Override
    protected void initLoginButton() {
        loginButton = initElementByText("span","短信验证码登录");
    }

    @Override
    protected void initInput() {
        input = initElementByXpath("input","ht","input_smslogin_account");
    }

    @Override
    protected void initArea() {
        area = initElementByClass("div","hwid-ddrop");
    }

    @Override
    protected void initJianpuzhai() {
        jianpuzhai = initElementByXpath("span","data-ht-item","click_common_li_+855(柬埔寨)");
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
