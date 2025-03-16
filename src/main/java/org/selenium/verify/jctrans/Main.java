package org.selenium.verify.jctrans;

import org.selenium.verify.common.XlsxGenerator;
import org.slf4j.MDC;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class Main {
    private static final int maxTry = 250;
    private static final String logDir = System.getProperty("user.dir") + File.separator + "logs" + File.separator;

    //private static String resultFilePath = logDir + "result.xlsx";

    static List<Map<String,Object>> result = new ArrayList<>();
    public static void main(String[] args) throws Exception {
        // 获取当前类的包名
        String packageName = Main.class.getPackage().getName();
        String threadPrefix = packageName.substring(packageName.lastIndexOf('.') + 1) + "-";

        // 创建自定义线程工厂
        ThreadFactory threadFactory = new ThreadFactory() {
            private int threadNumber = 1;

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, threadPrefix + threadNumber++);
                return thread;
            }
        };

        long startTime = System.currentTimeMillis();
        // 创建一个调度线程池
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10, threadFactory);

        // 提交任务，每隔 3 秒开始执行一个任务
        for (int i = 0; i < 1; i++) {
            ScheduledFuture<Void> future = scheduler.schedule(new Task(i), i * 10, TimeUnit.SECONDS);
            //future.get(); // 等待任务完成（可选）
        }

        // 关闭调度线程池
        scheduler.shutdown();
        scheduler.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);

        long elapsedSec = (System.currentTimeMillis() - startTime)/1000;
        long elapsedMin = elapsedSec / 60;
        long elapsedHour = elapsedMin / 60;
        String elaspedTimeStr = "耗时"+ elapsedHour + "小时" + elapsedMin % 60 + "分" + elapsedSec % 60 + "秒";

        //统计结果
        generateReport(elaspedTimeStr);
    }

    private static void generateReport(String elaspedTimeStr) {
        // 计算总和
        int totalTryCount = 0;
        int totalFetchNumberCount = 0;
        int totalSuccessCount = 0;

        for (Map<String, Object> map : result) {
            totalTryCount += (int) map.get("tryCount");
            totalFetchNumberCount += (int) map.get("fetchNumberCount");
            totalSuccessCount += (int) map.get("successCount");
        }
        // 使用 LinkedHashMap 保证顺序
        Map<String, Object> totalMap = new LinkedHashMap<>();
        totalMap.put("threadName", "所有线程");
        totalMap.put("tryCount", totalTryCount);
        totalMap.put("fetchNumberCount", totalFetchNumberCount);
        totalMap.put("successCount", totalSuccessCount);
        totalMap.put("elaspedTimeStr", elaspedTimeStr);
        result.add(totalMap);
        String timeStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")); // 格式化时间
        String resultFileName = logDir + "result" + "_" + timeStamp + ".xlsx"; // 拼接文件名和时间
        new XlsxGenerator().generate(result, resultFileName);
    }

    static class Task implements Callable<Void> {
        private final int taskId;

        public Task(int taskId) {
            this.taskId = taskId;
        }

//        @Override
//        public void run() {
//            // 设置线程的 threadName
//            MDC.put("threadName", Thread.currentThread().getName());
//            System.out.println(Thread.currentThread().getName() + " 开始执行任务：" + MDC.get("threadName"));
//            SliderAutomatic sliderAutomatic = new SliderAutomatic(maxTry, logDir);
//            result.add(sliderAutomatic.start());
//            // 清除 MDC
//            MDC.clear();
//            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//                sliderAutomatic.close();
//                System.out.println("程序退出时调用的函数");
//            }));
//        }

        @Override
        public Void call() throws Exception {
            // 设置线程的 threadName
            MDC.put("threadName", Thread.currentThread().getName());
            System.out.println(Thread.currentThread().getName() + " 开始执行任务：" + MDC.get("threadName"));
            SliderAutomatic sliderAutomatic = new SliderAutomatic(maxTry, logDir);
            result.add(sliderAutomatic.start());
            // 清除 MDC
            MDC.clear();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                sliderAutomatic.close();
                System.out.println("程序退出时调用的函数");
            }));
            return null;
        }
    }
}
