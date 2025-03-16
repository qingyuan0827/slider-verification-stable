package org.selenium.verify.common;

import org.apache.commons.io.FileUtils;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

public class CrackSlider {
    private static final Logger log = Logger.getLogger("CrackSlider");
    static {
        System.load("D:\\Program Files\\opencv\\build\\java\\x64\\opencv_java490.dll");
        //System.load("D:\\opencv\\build\\java\\x64\\opencv_java490.dll");
    }

    // 为jpg图像添加alpha通道
    private Mat addAlphaChannel(Mat img) {
        List<Mat> channels = new ArrayList<>();
        Core.split(img, channels); // 分离为 BGR 通道

        // 添加 Alpha 通道
        Mat alphaChannel = Mat.ones(channels.get(0).size(), CvType.CV_8UC1);
        alphaChannel.setTo(new Scalar(255));
        channels.add(alphaChannel);

        // 合并通道
        Mat newImg = new Mat();
        Core.merge(channels, newImg);
        return newImg;
    }

    private Mat handleImage(Mat img) {
        Mat gray = new Mat();
        Imgproc.cvtColor(img, gray, Imgproc.COLOR_RGBA2GRAY);
        // 调整高斯模糊核大小（根据图像分辨率动态计算）
        int kernelSize = (int) (Math.min(img.width(), img.height()) * 0.01) * 2 + 1;
        Imgproc.GaussianBlur(gray, gray, new Size(kernelSize, kernelSize), 0);
        // 动态调整 Canny 阈值（基于图像亮度）
        Scalar meanVal = Core.mean(gray);
        double lowThresh = meanVal.val[0] * 0.5;
        double highThresh = meanVal.val[0] * 1.5;
        Mat edges = new Mat();
        Imgproc.Canny(gray, edges, lowThresh, highThresh);
        return edges;
    }

    public double match(int x, String img1Path, String img2Path, int num1, int num2) throws IOException {
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
        double distance = mmr.maxLoc.x * num1 / (double) num2 - x;

        return distance;
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
     * 去掉图像周围的透明区域
     *
     * @param inputPath  输入图像路径
     * @param outputPath 输出图像路径
     * @return 是否成功处理
     */
    public boolean removeTransparentArea(String inputPath, String outputPath) throws IOException {
        // 读取图像
        //Mat image = Imgcodecs.imread(inputPath, Imgcodecs.IMREAD_UNCHANGED); // 读取包含透明通道的图像
        Mat image = loadAndCheckImage(inputPath);

        // 检查图像是否包含透明通道
        if (image.channels() != 4) {
            System.out.println("图像不包含透明通道，无需处理");
            return false;
        }

        // 提取 Alpha 通道
        Mat alphaChannel = new Mat();
        Core.extractChannel(image, alphaChannel, 3); // 提取第 4 个通道（Alpha 通道）

        // 找到非透明区域的边界
        MatOfPoint points = new MatOfPoint();
        Core.findNonZero(alphaChannel, points); // 找到所有非零像素的位置
        Rect boundingRect = Imgproc.boundingRect(points); // 计算非透明区域的外接矩形

        // 裁剪图像
        Mat croppedImage = new Mat(image, boundingRect);
        if (croppedImage.empty()) {
            return false;
        }
        // 保存裁剪后的图像
        boolean success = Imgcodecs.imwrite(outputPath, croppedImage);
        if (success) {
            System.out.println("裁剪后的图像已保存：" + outputPath);
        } else {
            System.out.println("保存图像失败，请检查路径：" + outputPath);
        }

        return success;
    }

    private Mat loadAndCheckImage(String imagePath) throws IOException {
        if (!checkFileExists(imagePath)) {
            throw new IOException("文件不存在或无法访问: " + imagePath);
        }
        Mat img = Imgcodecs.imread(imagePath, Imgcodecs.IMREAD_UNCHANGED);
        if (img.empty()) {
            throw new IOException("图像加载失败: " + imagePath);
        }
        return img;
    }

    private boolean checkFileExists(String filePath) {
        Path path = Paths.get(filePath);
        return Files.exists(path) && Files.isRegularFile(path);
    }

    /**
     * 轮廓检测，计算缺口位置
     */
    public double getPos(int x, String imgPath, int num, int num1, int num2) throws IOException {
        Mat image = Imgcodecs.imread(imgPath);
        Mat blurred = new Mat();
        Imgproc.GaussianBlur(image, blurred, new Size(5, 5), 0);
        Mat canny = new Mat();
        Imgproc.Canny(blurred, canny, 0, 100);

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(canny, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
//        log.info("轮廓检测结果：" + contours.size());

        for (MatOfPoint contour : contours) {
            // 将 MatOfPoint 转换为 MatOfPoint2f
            MatOfPoint2f contour2f = new MatOfPoint2f(contour.toArray());
            double area = Imgproc.contourArea(contour);
            double length = Imgproc.arcLength(contour2f, true);
//            System.out.println("轮廓面积："+area + "，周长"+length);
//            System.out.println("are的大小是:"+area);
            if (num * num - num * num / 4 < area && area < num * num + num * num / 4 &&
                    num * 4 - num * 4 / 4 < length && length < num * 4 + num * 4 / 4) {
                Rect rect = Imgproc.boundingRect(contour);
//                log.info("计算出目标区域的坐标及宽高：" + rect.x + ", " + rect.y + ", " + rect.width + ", " + rect.height);
                Imgproc.rectangle(image, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 0, 255), 2);
                Imgcodecs.imwrite("img/test.jpg", image);
                return rect.x * num1 / (double) num2 - x;
            }
        }
        return 0;
    }

    // 下载图片到本地
    public void downloadImage(String url, String savePath) throws IOException {
//        System.out.println("Download url:" +url);
        FileUtils.copyURLToFile(new URL(url), new File(savePath));
    }

    // 模拟滑块拖动
    public void slowlyDrag(WebDriver driver, WebElement element, double distance) {
        Actions actions = new Actions(driver);
        actions.clickAndHold(element).perform();
        Random rand = new Random();
        double moved = 0;
        int i = 0;
        
        while (moved < distance) {
            int x;
            if(distance - moved <10){
                x = (int) (distance - moved + 1);
            }
            else{
                x = rand.nextInt(8) + 3; // 3-10像素
            }
            actions.moveByOffset(x, 0).perform();
            moved += x;
//            System.out.printf("第%d次移动，累计位移%.0f像素%n", ++i, moved);
        }
        actions.release().perform();
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
//        log.info("第" + i + "次移动了" + x + "，位置为" + element.getLocation().getX() + ", 移动距离为" + moved);
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
//            log.info("第" + i + "次移动了" + x + "，位置为" + element.getLocation().getX() + ", 移动距离为" + moved);
            i++;
        }
        log.info("目标distance" + distance + ", 最终位置为" + element.getLocation().getX() + ", 移动距离为" + moved);
        actions.release().perform();
    }

    public void quicklyMove(WebDriver driver, WebElement element, double distance) {
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

    public void extractAndSave(String inputPath, String outputPath, int x,int y,int width ,int height) throws Exception {
        final Rect TARGET_ROI = new Rect(x, y, width, height); // (x,y,width,height)
        // 读取原图
        Mat src = Imgcodecs.imread(inputPath);
        if (src.empty()) {
            throw new Exception("图片加载失败: " + inputPath);
        }

        // 验证坐标有效性
        if (TARGET_ROI.x < 0 || TARGET_ROI.y < 0 ||
                TARGET_ROI.x + TARGET_ROI.width > src.cols() ||
                TARGET_ROI.y + TARGET_ROI.height > src.rows()) {
            throw new Exception(String.format(
                    "坐标越界: 原图尺寸 %dx%d，目标区域 %s",
                    src.cols(), src.rows(), TARGET_ROI.toString()
            ));
        }

        // 截取指定区域
        Mat roi = new Mat(src, TARGET_ROI);

        // 保存结果
        Imgcodecs.imwrite(outputPath, roi);
    }


    public static void main(String[] args) throws IOException {
        CrackSlider crackSlider = new CrackSlider();

        crackSlider.removeTransparentArea("cap_union_new_getcapbysig.png","output.png");

//        double d = crackSlider.getPos(0,"1741850078304tengxun_image.jpg",120,340,672);
//
//        System.out.println("距离为："+d+"像素");
    }

}