package com.zlt.aps.common.core.utils;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;

public class ExcelUtils {

    /**
     * 取得WorkBook对象
     * xls:HSSFWorkbook,03版
     * xlsx:XSSFWorkbook,07版
     */
    public static Workbook readExcel(String filePath) {
        InputStream in = null;
        Workbook work = null;
        try {
            in = new FileInputStream(filePath);
            if (filePath.endsWith(".xls")) {
                work= new HSSFWorkbook(in);
            }
            if (filePath.endsWith(".xlsx")) {
                try {
                    work= new XSSFWorkbook(in);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("文件路径错误");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("文件输入流错误");
            e.printStackTrace();
        }
        return work;
    }
    /**
     * 取得WorkBook对象
     * xls:HSSFWorkbook,03版
     * xlsx:XSSFWorkbook,07版
     */
    public static Workbook readExcel(InputStream in) {
        Workbook work = null;
        try {
            work= new XSSFWorkbook(in);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return work;
    }

    /**
     * 设置单元格样式（水平居中、垂直居中、周围边框）
     * @param workbook
     * @return CellStyle
     */
    public static CellStyle createCellStyle(Workbook workbook){
        CellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setAlignment(HorizontalAlignment.CENTER);
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        cellStyle.setBorderBottom(BorderStyle.THIN);
        cellStyle.setBorderTop(BorderStyle.THIN);
        cellStyle.setBorderLeft(BorderStyle.THIN);
        cellStyle.setBorderRight(BorderStyle.THIN);
        return cellStyle;
    }

    /**
     * 设置单元格样式（水平居中、垂直居中、周围边框）
     * @param workbook
     * @return CellStyle
     */
    public static CellStyle getAlignmentLeftCellStyle(Workbook workbook){
        CellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setAlignment(HorizontalAlignment.LEFT);
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        cellStyle.setBorderBottom(BorderStyle.THIN);
        cellStyle.setBorderTop(BorderStyle.THIN);
        cellStyle.setBorderLeft(BorderStyle.THIN);
        cellStyle.setBorderRight(BorderStyle.THIN);
        return cellStyle;
    }


    public static void writeExcel(HttpServletResponse response, Workbook work, String fileName) throws IOException {
        OutputStream out = null;
        try {
            out = response.getOutputStream();
            response.setContentType("application/ms-excel;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment;filename="
                    .concat(String.valueOf(URLEncoder.encode(fileName + ".xls", "UTF-8"))));
            work.write(out);
        } catch (IOException e) {
            System.out.println("输出流错误");
            e.printStackTrace();
        } finally {
            out.close();
        }
    }


}