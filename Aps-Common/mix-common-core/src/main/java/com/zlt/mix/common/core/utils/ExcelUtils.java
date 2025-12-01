package com.zlt.mix.common.core.utils;

import com.ruoyi.common.core.utils.DateUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.Date;

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

    public static String getCellValue(Cell cell) {
        Object val = null;
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC || cell.getCellType() == CellType.FORMULA) {
            val = cell.getNumericCellValue();
            //当val值为大值时，很可能解析为科学计数法显示
            String valStr = val + "";
            if (valStr.contains("E")) {
                BigDecimal realValue = new BigDecimal(valStr);
                val = realValue.toPlainString();
            }else {
                if ((Double) val % 1 != 0) {
                    val = new BigDecimal(val.toString());
                } else {
                    val = new DecimalFormat("0").format(val);
                }
            }
        } else if (cell.getCellType() == CellType.STRING) {
            val = cell.getStringCellValue();
        } else if (cell.getCellType() == CellType.BOOLEAN) {
            val = cell.getBooleanCellValue();
        }
        if (val == null) {
            return null;
        }
        return val + "";
    }

    public static Integer getIntegerValue(String val) {
        Integer integerVal = null;
        if (val == null) {
            return integerVal;
        }
        try {
            if (val.endsWith(".0")) {
                val = val.substring(0, val.indexOf(".0"));
            }
            integerVal = Integer.parseInt(val);
        } catch (Exception e) {
            return null;
        }
        return integerVal;
    }

    public static Double getDoubleValue(String val) {
        Double db = null;
        if (val == null) {
            return db;
        }
        try {
            db = Double.valueOf(val);
        } catch (Exception e) {
            return null;
        }
        return db;
    }

    public static Date getDateValue(String val, String parsePatterns) throws ParseException {
        Date db = null;
        if (val == null) {
            return DateUtils.parseDate("1970-01-01", parsePatterns);
        }
        try {
            db = DateUtils.parseDate(val, parsePatterns);
        } catch (Exception e) {
            return DateUtils.parseDate("1970-01-01", parsePatterns);
        }
        return db;
    }

    public static boolean checkObjFieldIsNull(Object obj) throws IllegalAccessException {
        boolean flag = true;

        Class clazz = obj.getClass();
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            //设置属性是可以访问的(私有的也可以)
            field.setAccessible(true);
            Object value = null;
            try {
                value = field.get(obj);
                // 只要有1个属性不为空,那么就不是所有的属性值都为空
                if (value != null) {
                    flag = false;
                    break;
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        return flag;
    }
}