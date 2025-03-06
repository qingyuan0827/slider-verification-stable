package org.selenium.verify.juejin;

import lombok.extern.slf4j.Slf4j;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
public class CrackSlider {

    static {
        // 加载OpenCV库
        try {
            System.load("D:\\Program Files\\opencv\\build\\java\\x64\\opencv_java490.dll");
            log.info("OpenCV loaded successfully.");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Failed to load OpenCV library: " + e.getMessage());
            throw e;
        }    }

    /**
     * 为jpg图像添加alpha通道
     */
    private Mat addAlphaChannel(Mat img) {
        if (img.channels() == 3) {
            Mat alphaChannel = Mat.ones(img.size(), CvType.CV_8UC1);
            Core.multiply(alphaChannel, new Scalar(255), alphaChannel);
            List<Mat> channels = new ArrayList<>();
            Core.split(img, channels);
            channels.add(alphaChannel);
            Core.merge(channels, img);
        }
        return img;
    }

    /**
     * 灰度处理，再对图像进行高斯处理，最后进行边缘检测
     */
    private Mat handelImg(Mat img) {
        Mat gray = new Mat();
        Imgproc.cvtColor(img, gray, Imgproc.COLOR_RGBA2GRAY);
        Mat blur = new Mat();
        Imgproc.GaussianBlur(gray, blur, new Size(5, 5), 1);
        Mat canny = new Mat();
        Imgproc.Canny(blur, canny, 60, 60);
        return canny;
    }

    /**
     * 模板匹配，获取到移动距离
     */
    public double match(WebElement element, String img1Path, String img2Path, int num1, int num2) throws IOException {
        Mat img1 = Imgcodecs.imread(img1Path, Imgcodecs.IMREAD_UNCHANGED);
        Mat img2 = Imgcodecs.imread(img2Path, Imgcodecs.IMREAD_UNCHANGED);

        if (img1.channels() == 3) {
            img1 = addAlphaChannel(img1);
        }

        Mat processedImg1 = handelImg(img1);
        Mat processedImg2 = handelImg(img2);

        Mat result = new Mat();
        Imgproc.matchTemplate(processedImg1, processedImg2, result, Imgproc.TM_CCOEFF_NORMED);
        Core.MinMaxLocResult mmr = Core.minMaxLoc(result);
        double distance = mmr.maxLoc.x * num1 / (double) num2 - element.getLocation().getX();

        return distance;
    }

    /**
     * 轮廓检测，计算缺口位置
     */
    public double getPos(WebElement element, String imgPath, int num, int num1, int num2) throws IOException {
        Mat image = Imgcodecs.imread(imgPath);
        Mat blurred = new Mat();
        Imgproc.GaussianBlur(image, blurred, new Size(5, 5), 0);
        Mat canny = new Mat();
        Imgproc.Canny(blurred, canny, 0, 100);

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(canny, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        log.info("轮廓检测结果：" + contours.size());

        for (MatOfPoint contour : contours) {
            // 将 MatOfPoint 转换为 MatOfPoint2f
            MatOfPoint2f contour2f = new MatOfPoint2f(contour.toArray());
            double area = Imgproc.contourArea(contour);
            double length = Imgproc.arcLength(contour2f, true);

            if (num * num - num * num / 4 < area && area < num * num + num * num / 4 &&
                num * 4 - num * 4 / 4 < length && length < num * 4 + num * 4 / 4) {
                Rect rect = Imgproc.boundingRect(contour);
                log.info("计算出目标区域的坐标及宽高：" + rect.x + ", " + rect.y + ", " + rect.width + ", " + rect.height);
                Imgproc.rectangle(image, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 0, 255), 2);
                Imgcodecs.imwrite("img/test.jpg", image);
                return rect.x * num1 / (double) num2 - element.getLocation().getX();
            }
        }
        return 0;
    }


    /**
     * 将HTTP图片保存到本地
     */
    void downloadImage(String imageUrl, Path destinationFile) {
        Path destinationDir = destinationFile.getParent();

        // 确保目标目录存在
        try {
            Files.createDirectories(destinationDir);  // 创建 img 目录

            // 使用 BufferedInputStream 和 FileOutputStream 下载并保存文件
            try (BufferedInputStream in = new BufferedInputStream(new URL(imageUrl).openStream());
                 FileOutputStream fileOutputStream = new FileOutputStream(destinationFile.toFile())) {
                byte dataBuffer[] = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                    fileOutputStream.write(dataBuffer, 0, bytesRead);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        log.info("文件下载成功: " + destinationFile.toAbsolutePath());
    }
    /**
     * 慢慢滑动滑块模拟人的操作
     */
    public void slowly(WebDriver driver, WebElement element, double distance) {
        Actions actions = new Actions(driver);
        actions.clickAndHold(element).perform();

        Random random = new Random();
        double moved = 0;
        int i = 1;
//        while (moved < distance-10) {
//            double x = random.nextInt(8) + 3;  // 每次移动3到10像素
//            moved += x;
//            actions.moveByOffset((int) x, 0).perform();
//            log.info("第" + i + "次移动了" + x + "，位置为" + element.getLocation().getX() + ", 移动距离为" + moved);
//            i++;
//        }
        double x = distance - 5;  //
        moved += x;
        actions.moveByOffset((int) x, 0).perform();
        log.info("第" + i + "次移动了" + x + "，位置为" + element.getLocation().getX() + ", 移动距离为" + moved);
        i++;
        while (moved < distance) {
            x = random.nextDouble(1,2);  //
//            try {
//                Thread.sleep((long) (x * 1000));
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
            actions.pause((long) (x * 100));
            moved += x;
            actions.moveByOffset((int) x, 0).perform();
            log.info("第" + i + "次移动了" + x + "，位置为" + element.getLocation().getX() + ", 移动距离为" + moved);
            i++;
        }
        log.info("目标distance" + distance + ", 最终位置为" + element.getLocation().getX() + ", 移动距离为" + moved);
        actions.release().perform();
    }

}