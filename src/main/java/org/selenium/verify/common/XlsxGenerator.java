package org.selenium.verify.common;

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class XlsxGenerator {
    public void generate(List<Map<String, Object>> result, String fileName) {
        // 生成xlsx文件
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             //FileOutputStream fileOut = new FileOutputStream("Action属性表.xlsx")) {
             FileOutputStream fileOut = new FileOutputStream(fileName)) {

            XSSFSheet sheet = workbook.createSheet("result");

            // 创建表头行
            XSSFRow headerRow = sheet.createRow(0);
            // 获取所有 key 并转换为 List
            Set<String> keys = result.get(0).keySet();
            List<String> keyList = new ArrayList<>(keys);
            // 依次取出每个 key 及其下标
            for (int i = 0; i < keyList.size(); i++) {
                XSSFCell cell = headerRow.createCell(i);
                cell.setCellValue(keyList.get(i));
            }

            int rowIndex = 1; // 从第二行开始写入数据
            for (Map<String, Object> map : result) {
                XSSFRow row = sheet.createRow(rowIndex++);
                // 将 map.values() 转换为 List
                List<Object> values = new ArrayList<>(map.values());
                // 依次取出每个 key 及其下标
                for (int i = 0; i < values.size(); i++) {
                    Object value = values.get(i);
                    if (value instanceof String) {
                        row.createCell(i).setCellValue((String) value);
                    } else if (value instanceof Integer) {
                        row.createCell(i).setCellValue((Integer) value);
                    }
                }
            }

            workbook.write(fileOut);
            System.out.println("生成" + fileName + "成功！");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
