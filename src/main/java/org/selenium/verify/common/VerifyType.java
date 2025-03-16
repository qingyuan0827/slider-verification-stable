package org.selenium.verify.common;

public enum VerifyType {
    NONE,
    SLIDER,
    DEBUG_NO_CLICK,
    DEBUG_CLICK,
    SLIDER_POS,
    SLIDER_SIMPLE,
    SLIDER_CANVAS;

    public static boolean isSlider(VerifyType verifyType){
        return verifyType == SLIDER || verifyType == SLIDER_POS || verifyType ==SLIDER_CANVAS || verifyType == SLIDER_SIMPLE;
    }

}
