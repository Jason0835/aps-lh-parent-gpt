package com.zlt.aps.common.core.utils;

import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.domain.BorderStyleVo;
import com.zlt.aps.common.core.domain.ExcelCellRangeAddress;
import com.zlt.aps.common.core.domain.ExcelImg;
import com.zlt.aps.common.core.domain.ExcelStyleVo;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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


    // 写入Excel模板数据start

    /**
     * 匹配"{exp}"，不包含数字
     */
    private static final String REG = "\\{([a-zA-Z0-9_]+)\\}";
    /**
     *  匹配"{.exp}"，不包含数字
     */
    private static final String REG_LIST = "\\{\\.([a-zA-Z0-9_]+)\\}";
    private static final Pattern PATTERN = Pattern.compile(REG);
    private static final Pattern PATTERN_LIST = Pattern.compile(REG_LIST);

    //图片
    public static final String IMG = "IMG";

    /**
     * 合并单元格
     */
    public static  final String RANGE_ADDRESS = "RANGE_ADDRESS";

    public static  final String CELL_STYLE = "CELL_STYLE";

    /**
     * 是否显示网格线，不传默认显示
     */
    public static final String DISPLAY_GRIDLINES = "DISPLAY_GRIDLINES";

    public static final String BORDER_STYLE = "BORDER_STYLE";

    private ExcelUtils() {
    }

    /**
     * 根据模板生成Excel文件
     *
     * @param context      表头或表尾数据集合
     * @param dataList     列表
     * @return
     */
    public static byte[] writeExcel(Workbook workbook, int sheetIndex, Map<String, Object> context,
                                    List<Map<String, Object>> dataList) {
        try  {
            Sheet sheet = workbook.getSheetAt(sheetIndex);// 获取配置文件sheet 页
            if (context.containsKey(ExcelUtils.DISPLAY_GRIDLINES)) {
                Object displayGridlines = context.get(ExcelUtils.DISPLAY_GRIDLINES);
                sheet.setDisplayGridlines(Boolean.parseBoolean(displayGridlines.toString()));
            }
            int listStartRowNum = -1;
            for (int i = sheet.getFirstRowNum(); i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    for (int j = 0; j < row.getLastCellNum(); j++) {
                        Cell cell = row.getCell(j);
                        if (cell != null && cell.getCellType().getCode() == CellType.STRING.getCode()) {
                            String cellValue = cell.getStringCellValue();
                            // 获取到列表数据所在行
                            if (listStartRowNum == -1 && cellValue.matches(REG_LIST)) {
                                listStartRowNum = i;
                            }

                            Object newValue = cellValue;
                            Matcher matcher = PATTERN.matcher(cellValue);
                            while (matcher.find()) {
                                String replaceExp = matcher.group();// 匹配到的表达式
                                String key = matcher.group(1);// 获取key
                                Object replaceValue = context.get(key);
                                if (replaceValue == null) {
                                    replaceValue = "";
                                }
                                if (replaceExp.equals(cellValue)) {// 单元格是一个表达式
                                    newValue = replaceValue;
                                } else {// 以字符串替换
                                    newValue = ((String) newValue).replace(replaceExp, replaceValue.toString());
                                }
                            }
                            setCellValue(cell, newValue);

                        }
                    }

                }
            }
            if (-1 != listStartRowNum) {// 如果不为 -1 说明有需要循环的列表表达式
                Row listStartRow = sheet.getRow(listStartRowNum);

                if (CollectionUtils.isEmpty(dataList)) {// 列表数据为空，清空列表表达式行
                    for (int i = 0; i < listStartRow.getLastCellNum(); i++) {
                        Cell cell = listStartRow.getCell(i);
                        if (cell != null) {
                            cell.setCellValue("");
                        }
                    }
                } else {
                    int lastCellNum = listStartRow.getLastCellNum();
                    if (listStartRowNum + 1 <= sheet.getLastRowNum()) {
                        sheet.shiftRows(listStartRowNum + 1, sheet.getLastRowNum(), dataList.size(), true, false);// 列表数据行后面行下移，留出数据填充区域
                    }
                    for (int i = 0; i < dataList.size(); i++) {// 循环列表数据 生成行
                        Map<String, Object> map = dataList.get(i);// 一行数据
                        int newRowNum = listStartRowNum + i + 1;// 保留表达式行
                        Row newRow = sheet.createRow(newRowNum);// 创建新行

                        for (int j = 0; j < lastCellNum; j++) {// 循环遍历单元格

//                            newRow.setHeightInPoints(30); // 设置行高为30个点
                            Cell cell = listStartRow.getCell(j);// 列表数据行

                            // 填充数据
                            if (cell != null) {
                                Cell newCell = newRow.createCell(j);
                                newCell.setCellStyle(cell.getCellStyle());// 设置单元格格式
                                if (cell.getCellType().getCode() == CellType.STRING.getCode()
                                        && cell.getStringCellValue().matches(REG_LIST)) {// 单元格是一个表达式
                                    String cellExp = cell.getStringCellValue();
                                    Matcher matcher = PATTERN_LIST.matcher(cellExp);
                                    matcher.find();
                                    String key = matcher.group(1);// 获取key
                                    Object newValue = map.get(key);
                                    if (newValue == null) {
                                        newValue = "";
                                    }
                                    if(map.containsKey("height")){
                                        newRow.setHeightInPoints(Float.parseFloat(map.get("height").toString())); // 设置行高为30个点

                                    }
                                    setCellValue(newCell, newValue);
                                    // 设置样式

                                    if(map.containsKey("style")){
                                        CellStyle style = workbook.createCellStyle();
                                        //创建字体样式
                                        Font font = workbook.createFont();

                                        //true为加粗，默认为不加粗

                                        Object cellStyleObj = map.get("style");
                                        ExcelStyleVo excelStyleVo = null;
                                        if (cellStyleObj != null) {
                                            String jsonStr = com.alibaba.fastjson.JSON.toJSONString(cellStyleObj);
                                            excelStyleVo = com.alibaba.fastjson.JSON.parseObject(jsonStr, com.zlt.aps.common.core.domain.ExcelStyleVo.class);
                                        }

                                        if(StringUtils.isNotEmpty(excelStyleVo.getFontName())){
                                            font.setFontName(excelStyleVo.getFontName());
                                        }
                                        font.setBold(excelStyleVo.getBold());
                                        if(excelStyleVo.getFontSize() != null){
                                            font.setFontHeightInPoints(excelStyleVo.getFontSize());
                                        }
                                        //将字体样式设置到单元格样式中
                                        style.setFont(font);
                                        if(excelStyleVo.getBorder()){
                                            style.setBorderTop(BorderStyle.THIN);
                                            style.setBorderBottom(BorderStyle.THIN);
                                            style.setBorderLeft(BorderStyle.THIN);
                                            style.setBorderRight(BorderStyle.THIN);
                                        }
                                        if(excelStyleVo.getBorderStyleVo() != null){
                                            BorderStyleVo borderStyleVo = excelStyleVo.getBorderStyleVo();
                                            if(borderStyleVo.getBorderTop() != null){
                                                style.setBorderTop(borderStyleVo.getBorderTop());
                                            }else {
                                                style.setBorderTop(BorderStyle.NONE);
                                            }
                                            if(borderStyleVo.getBorderBottom() != null){
                                                style.setBorderBottom(borderStyleVo.getBorderBottom());
                                            }else {
                                                style.setBorderBottom(BorderStyle.NONE);
                                            }
                                            if(borderStyleVo.getBorderLeft() != null){
                                                style.setBorderLeft(borderStyleVo.getBorderLeft());
                                            }else {
                                                style.setBorderLeft(BorderStyle.NONE);
                                            }
                                            if(borderStyleVo.getBorderRight() != null){
                                                style.setBorderRight(borderStyleVo.getBorderRight());
                                            }else {
                                                style.setBorderRight(BorderStyle.NONE);
                                            }
                                        }
                                        if(excelStyleVo.getColor() != null){
                                            // 设置背景颜色为黄色
                                            style.setFillForegroundColor(excelStyleVo.getColor());
                                            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                                        }
                                        //设置居中
                                        style.setAlignment(HorizontalAlignment.CENTER);
                                        style.setVerticalAlignment(VerticalAlignment.CENTER);
                                        //给单元格添加样式
                                        newCell.setCellStyle(style);

                                    }


                                } else {// 不是表达式复制单元格数据
                                    int cellType = cell.getCellType().getCode();
                                    if (cellType == CellType.NUMERIC.getCode()) {
                                        newCell.setCellValue(cell.getNumericCellValue());
                                    } else if (cellType == CellType.BOOLEAN.getCode()) {
                                        newCell.setCellValue(cell.getBooleanCellValue());
                                    } else if (cellType == CellType.STRING.getCode()) {
                                        newCell.setCellValue(cell.getStringCellValue());
                                    } else if (cellType == CellType.FORMULA.getCode()) {
                                        // 处理公式，待实现
                                    } else {
                                        newCell.setCellValue(cell.getStringCellValue());
                                    }
                                }
                            }
                        }
                    }
                    sheet.removeRow(listStartRow);// 删除list表达式行
                    sheet.shiftRows(listStartRowNum + 1, sheet.getLastRowNum(), -1, true, false);// 数据区域上移一行，覆盖表达式行

                    // 合并单元格处理
                    for (int i = 0; i < lastCellNum; i++) {
                        CellRangeAddress mergedRangeAddress = getMergedRangeAddress(sheet, listStartRowNum, i);
                        // 合并单元格
                        if (mergedRangeAddress != null) {// 合并的单元格
                            i = mergedRangeAddress.getLastColumn();
                            for (int j = 1; j < dataList.size(); j++) {
                                int newRowNum = listStartRowNum + j;
                                sheet.addMergedRegionUnsafe(new CellRangeAddress(newRowNum, newRowNum,
                                        mergedRangeAddress.getFirstColumn(), mergedRangeAddress.getLastColumn()));
                            }
                        }
                    }
                }
            }
            //有图片的话，需要插入图片，需要指定位置
            if(context.containsKey(IMG)){
                List<ExcelImg> excelImgList =  (List<ExcelImg>) context.get(IMG);
                for (ExcelImg excelImg : excelImgList) {
                    byte[] bytes = new byte[0];
                    InputStream imageIn = excelImg.getImgUrl().openStream();
                    if (imageIn != null) {
                        try {
                            bytes = IOUtils.toByteArray(imageIn);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        // 3. 创建绘图对象
                        XSSFDrawing drawing = (XSSFDrawing) sheet.createDrawingPatriarch();
                        // 4. 创建锚点，指定图片位置和大小
                        XSSFClientAnchor anchor = new XSSFClientAnchor(0, 0, 0, 0, excelImg.getStartCellNum()
                                , excelImg.getStartRowNum(), excelImg.getEndCellNum(), excelImg.getEndRowNum());
                        int pictureIndex = workbook.addPicture(bytes, Workbook.PICTURE_TYPE_PNG);
                        XSSFPicture picture = drawing.createPicture(anchor, pictureIndex);
                        imageIn.close();
                    }
                }
            }
            if(context.containsKey(RANGE_ADDRESS)){
                List<ExcelCellRangeAddress> rangeAddressList = (List<ExcelCellRangeAddress>) context.get(RANGE_ADDRESS);
                for (ExcelCellRangeAddress addr : rangeAddressList) {
//                    int index = getMergedRangeAddressIndex(sheet, addr.getFirstRow(), addr.getDateColumn());
//                    if(index != -999){
//                        sheet.removeMergedRegion(index);
//                    }
                    //确保至少有两个单元格
                    if ( addr.getFirstRow() == addr.getLastRow()
                            && addr.getFirstColumn()  == addr.getLastColumn()) {
                        continue;
                    }

                    // 添加新的合并区域，确保至少有两个单元格
                    sheet.addMergedRegion(new CellRangeAddress(addr.getFirstRow(), addr.getLastRow()
                            , addr.getFirstColumn(), addr.getLastColumn()));

                }
                // 合并单元格（将第一行的第1到第4列合并）

            }
            if(context.containsKey(CELL_STYLE)){
//                List<com.zlt.aps.common.core.domain.CellStyle> cellStyleList = (List<com.zlt.aps.common.core.domain.CellStyle>) context.get(CELL_STYLE);
                Object cellStyleObj = context.get(CELL_STYLE);
                List<com.zlt.aps.common.core.domain.CellStyle> cellStyleList = null;
                if (cellStyleObj != null) {
                    String jsonStr = com.alibaba.fastjson.JSON.toJSONString(cellStyleObj);
                    cellStyleList = com.alibaba.fastjson.JSON.parseArray(jsonStr, com.zlt.aps.common.core.domain.CellStyle.class);
                }
                if (cellStyleList != null) {
                    for (com.zlt.aps.common.core.domain.CellStyle cs : cellStyleList) {
                        boolean bold = cs.getBold() != null ? cs.getBold() : false;
                        String fontName = cs.getFontName();
                        CellStyle oldStyle = null;
                        if (cs.getStartRowNum() <= sheet.getLastRowNum()) {
                            oldStyle = sheet.getRow(cs.getStartRowNum()).getCell(cs.getStartCellNum()).getCellStyle(); // 加载原单元格
                        }
                        CellStyle style2 = createColorCellStyle(workbook, cs.getColor(), cs.getWithBorder(), bold, fontName, oldStyle);
                        setCellRangeColor(sheet, cs.getStartRowNum(), cs.getStartCellNum(), cs.getEndRowNum(), cs.getEndCellNum(), style2);
                    }
                }
            }
            // 公式生效
            sheet.setForceFormulaRecalculation(true);
            if(context.containsKey("GROUP_ROW")){
                sheet.groupRow(2, 10);
//                sheet.groupRow(3, 10);

            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            //关闭
            out.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new ExcelException(e.getMessage(), e);
        }
    }
    public ClassLoader getClassLoader(){
        return this.getClass().getClassLoader();
    }
    private static void setCellValue(Cell cell, Object value) {
        if (value instanceof Number) {// 如果是数字类型的设置为数值
            cell.setCellValue(Double.parseDouble(value.toString()));
        } else if (value instanceof Date) {// 如果为时间类型的设置为时间
            cell.setCellValue((Date) value);
        } else if (value instanceof String) {
            cell.setCellValue((String) value);
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else {
            cell.setCellValue(value.toString());
        }
    }

    /**
     * 获取指定行/列的合并单元格区域
     *
     * @param sheet
     * @param row
     * @param column
     * @return CellRangeAddress 不是合并单元格返回null
     */
    private static CellRangeAddress getMergedRangeAddress(Sheet sheet, int row, int column) {
        List<CellRangeAddress> mergedRegions = sheet.getMergedRegions();
        for (CellRangeAddress cellAddresses : mergedRegions) {
            if (row >= cellAddresses.getFirstRow() && row <= cellAddresses.getLastRow()
                    && column >= cellAddresses.getFirstColumn() && column <= cellAddresses.getLastColumn()) {
                return cellAddresses;
            }
        }
        return null;
    }

    /**
     * 获取指定行/列的合并单元格区域
     *
     * @param sheet
     * @param row
     * @param column
     * @return CellRangeAddress 不是合并单元格返回null
     */
    private static int getMergedRangeAddressIndex(Sheet sheet, int row, int column) {
        List<CellRangeAddress> mergedRegions = sheet.getMergedRegions();

        for (int i = 0; i < mergedRegions.size(); i++) {
            CellRangeAddress cellAddresses = mergedRegions.get(i);
            if (row >= cellAddresses.getFirstRow() && row <= cellAddresses.getLastRow()
                    && column >= cellAddresses.getFirstColumn() && column <= cellAddresses.getLastColumn()) {
                return i;
            }
        }
        return -999;
    }

    /**
     * 多个列表支持，按顺序写入excel。 列表数据数量需等于列表表达式数量，不然多余的表达式不会被清空。多余的列表数据不会被写入
     * Map<String, Object> 中的String 只能是字母结尾，不能以数字结尾 否则无法匹配
     * @param templateFile
     * @param sheetIndex sheetIndex
     * @param context
     * @param dataLists
     * @return
     */
    public static byte[] writeMultiList(File templateFile,int sheetIndex, Map<String, Object> context,
                                        List<List<Map<String, Object>>> dataLists) {
        try {
            File temp = templateFile;
            for (List<Map<String, Object>> dataList : dataLists) {
                FileInputStream inputStream = new FileInputStream(temp);
                XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
                byte[] tempBytes = writeExcel(workbook, sheetIndex,context, dataList);
                temp = File.createTempFile("multi_excel", ".excel");
                FileUtils.writeByteArrayToFile(temp, tempBytes);
            }
            return FileUtils.readFileToByteArray(temp);
        } catch (ExcelException e) {
            throw e;
        } catch (Exception e) {
            throw new ExcelException("生成Excel失败！", e);
        }
    }

    /**
     * 多个列表支持，按顺序写入excel。 列表数据数量需等于列表表达式数量，不然多余的表达式不会被清空。多余的列表数据不会被写入
     * Map<String, Object> 中的String 只能是字母结尾，不能以数字结尾 否则无法匹配
     * @param is
     * @param sheetIndex sheetIndex
     * @param context
     * @param dataLists
     * @return
     */
    public static byte[] writeMultiList(InputStream is,int sheetIndex, Map<String, Object> context,
                                        List<List<Map<String, Object>>> dataLists) {
        try {
            File temp = convertInputStreamToFile(is);
            //先取出合并单元格，最后一步才进行合并
            List<ExcelCellRangeAddress> rangeAddressList = (List<ExcelCellRangeAddress>) context.get(RANGE_ADDRESS);
            List<ExcelCellRangeAddress> cellStyleList = (List<ExcelCellRangeAddress>) context.get(CELL_STYLE);
            context.remove(RANGE_ADDRESS);
            context.remove(CELL_STYLE);
            int index = 0;
            int allListCount = dataLists.size();
            if (CollectionUtils.isNotEmpty(dataLists)) {
                for (List<Map<String, Object>> dataList : dataLists) {
                    //如果有多次，则图片不需要多次生成，因此删除
                    if(index != 0){
                        context.remove(IMG);
                    }
                    if((index == allListCount - 1 ) &&  !CollectionUtils.isEmpty(rangeAddressList)){
                        context.put(RANGE_ADDRESS,rangeAddressList);

                    }
                    if((index == allListCount - 1 ) &&  !CollectionUtils.isEmpty(cellStyleList)){
                        context.put(CELL_STYLE,cellStyleList);
                    }
                    index ++;
                    FileInputStream inputStream = new FileInputStream(temp);
                    XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
                    byte[] tempBytes = writeExcel(workbook, sheetIndex,context, dataList);
                    temp = File.createTempFile("multi_excel", ".excel");
                    FileUtils.writeByteArrayToFile(temp, tempBytes);
                }
            } else {
                FileInputStream inputStream = new FileInputStream(temp);
                XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
                byte[] tempBytes = writeExcel(workbook, sheetIndex,context, new ArrayList<>());
                temp = File.createTempFile("multi_excel", ".excel");
                FileUtils.writeByteArrayToFile(temp, tempBytes);
            }
            byte [] output = FileUtils.readFileToByteArray(temp);
//            FileUtils.writeByteArrayToFile(new File("test.xlsx"), output);
            if(temp.exists()) {
                boolean delete = temp.delete();
            }
            is.close();
            return output;
        } catch (ExcelException e) {
            throw e;
        } catch (Exception e) {
            throw new ExcelException("生成Excel失败！", e);
        }
    }

    static class ExcelException extends RuntimeException {
        /**
         *
         */
        private static final long serialVersionUID = -2772261598232964002L;

        public ExcelException(String msg, Throwable e) {
            super(msg, e);
        }

        public ExcelException(String msg) {
            super(msg);
        }
    }

    /**
     *
     * @param originalSheet 旧sheet
     * @param newSheet 新sheet
     * @param oldIndex 旧sheetIndex
     * @param newIndex 新sheetIndex
     */
    public static void copyPageSetup(Sheet originalSheet, Sheet newSheet,int oldIndex,int newIndex) {
        // 获取原始工作表的打印设置
        PrintSetup originalPrintSetup = originalSheet.getPrintSetup();

        // 设置新工作表的打印设置
        PrintSetup newPrintSetup = newSheet.getPrintSetup();
        newPrintSetup.setPaperSize(originalPrintSetup.getPaperSize());
        newPrintSetup.setScale(originalPrintSetup.getScale());
        newPrintSetup.setFitWidth(originalPrintSetup.getFitWidth());
        newPrintSetup.setFitHeight(originalPrintSetup.getFitHeight());
        newPrintSetup.setLeftToRight(originalPrintSetup.getLeftToRight());
        newPrintSetup.setLandscape(originalPrintSetup.getLandscape());
        newPrintSetup.setValidSettings(true);

        // 复制页边距
        newSheet.setMargin(Sheet.LeftMargin, originalSheet.getMargin(Sheet.LeftMargin));
        newSheet.setMargin(Sheet.RightMargin, originalSheet.getMargin(Sheet.RightMargin));
        newSheet.setMargin(Sheet.TopMargin, originalSheet.getMargin(Sheet.TopMargin));
        newSheet.setMargin(Sheet.BottomMargin, originalSheet.getMargin(Sheet.BottomMargin));
        newSheet.setMargin(Sheet.HeaderMargin, originalSheet.getMargin(Sheet.HeaderMargin));
        newSheet.setMargin(Sheet.FooterMargin, originalSheet.getMargin(Sheet.FooterMargin));
    }

    /**
     * 输出到本地
     * @param destination
     * @param input
     * @throws IOException
     */
    public static void writeToLocal(String destination, InputStream input)
            throws IOException {
        int index = 0;
        byte[] bytes = new byte[1024];
        FileOutputStream downloadFile = new FileOutputStream(destination);
        while ((index = input.read(bytes)) > 0) {
            downloadFile.write(bytes, 0, index);
        }
        downloadFile.close();
        input.close();
    }

    public static File convertInputStreamToFile(InputStream inputStream) throws IOException {
        // 创建一个临时文件
        File tempFile = File.createTempFile("inputStreamToFile", ".tmp");
        try (OutputStream outputStream = Files.newOutputStream(tempFile.toPath())) {
            byte[] buffer = new byte[1024];
            int length;
            // 从 InputStream 中读取数据，并写入到临时文件中
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
        }
        return tempFile;
    }

    /**
     * 按文件名生成临时文件
     * @param inputStream
     * @param fileName
     * @param suffix
     * @return
     * @throws IOException
     */
    public static File convertInputStreamToFile(InputStream inputStream,String fileName,String suffix) throws IOException {
        // 创建一个临时文件
        File tempFile = File.createTempFile(fileName, suffix);
        try (OutputStream outputStream = Files.newOutputStream(tempFile.toPath())) {
            byte[] buffer = new byte[1024];
            int length;
            // 从 InputStream 中读取数据，并写入到临时文件中
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
        }
        return tempFile;
    }

    public static XSSFWorkbook readExcel4XSSF(InputStream in) {
        XSSFWorkbook webBook = null;
        try {
            webBook = new XSSFWorkbook(in);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return webBook;
    }

    /**
     * 写入图片
     * @param webBook 工作簿对象
     * @param drawing 画布对象
     * @param bytes 图片
     * @param startRowNum 图片开始行
     * @param endRowNum 图片结束行
     * @param startCellNum 图片开始列
     * @param endCellNum 图片结束列
     */
    public static void writePitcher(XSSFWorkbook webBook, XSSFDrawing drawing, byte[] bytes,
                                    int startRowNum, int endRowNum,
                                    int startCellNum, int endCellNum) {
        int pictureIdx = webBook.addPicture(bytes, Workbook.PICTURE_TYPE_PNG);

        CreationHelper helper = webBook.getCreationHelper();
        ClientAnchor anchor = helper.createClientAnchor();

        // 设置图片的起始位置
        anchor.setCol1(startCellNum);
        anchor.setRow1(startRowNum);

        // 设置图片的结束位置
        anchor.setCol2(endCellNum);
        anchor.setRow2(endRowNum);

        drawing.createPicture(anchor, pictureIdx);
    }

    /**
     * 创建单元格样式，填充颜色为指定的色号
     * @param workbook
     * @param colorCode
     * @param withBorder 是否保留边框
     * @return
     */
    private static CellStyle createColorCellStyle(Workbook workbook, String colorCode, boolean withBorder) {
        return createColorCellStyle(workbook, colorCode, withBorder, false, null, null);
    }

    private static CellStyle createColorCellStyle(Workbook workbook, String colorCode, boolean withBorder, boolean bold, String fontName, CellStyle oldStyle) {
        XSSFCellStyle cellStyle = (XSSFCellStyle) workbook.createCellStyle();
        cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        try {
            java.awt.Color color = java.awt.Color.decode(colorCode);
            byte[] rgb = new byte[]{(byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue()};
            XSSFColor xssfColor = new XSSFColor(rgb, null);
            cellStyle.setFillForegroundColor(xssfColor);
            // 添加边框
            if (withBorder) {
                cellStyle.setBorderBottom(BorderStyle.THIN);
                cellStyle.setBorderTop(BorderStyle.THIN);
                cellStyle.setBorderLeft(BorderStyle.THIN);
                cellStyle.setBorderRight(BorderStyle.THIN);
                java.awt.Color borderColor = java.awt.Color.decode("#000000");
                byte[] borderRGB = new byte[]{(byte) borderColor.getRed(), (byte) borderColor.getGreen(), (byte) borderColor.getBlue()};
                XSSFColor xssfBorderColor = new XSSFColor(borderRGB, null);
                cellStyle.setBottomBorderColor(xssfBorderColor);
                cellStyle.setTopBorderColor(xssfBorderColor);
                cellStyle.setLeftBorderColor(xssfBorderColor);
                cellStyle.setRightBorderColor(xssfBorderColor);
            }
            // 修改字体样式
            if (bold || StringUtils.isNotEmpty(fontName)) {
                Font font = workbook.createFont();
                if (oldStyle != null) { // 原单元格样式复制回去
                    XSSFFont oldFont = (XSSFFont) workbook.getFontAt(oldStyle.getFontIndexAsInt());
                    if (oldFont != null) {
                        font.setFontName(oldFont.getFontName());
                        font.setBold(oldFont.getBold());
                        font.setFontHeightInPoints(oldFont.getFontHeightInPoints());
                    }
                }
                if (StringUtils.isNotEmpty(fontName)) {
                    font.setFontName(fontName);
                }
                if (bold) {
                    font.setBold(true);
                }
                cellStyle.setFont(font);
            } else if (oldStyle != null) { // 否则将原单元格字体原样设置到样式中
                XSSFFont oldFont = (XSSFFont) workbook.getFontAt(oldStyle.getFontIndexAsInt());
                cellStyle.setFont(oldFont);
            }
        } catch (NumberFormatException e) {
            System.out.println("无效的色号：" + colorCode);
        }

        return cellStyle;
    }

    // 设置指定范围内的单元格背景颜色
    private static void setCellRangeColor(Sheet sheet, int startRow, int startCol, int endRow, int endCol, CellStyle style) {
        for (int rowIndex = startRow; rowIndex <= endRow; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                row = sheet.createRow(rowIndex);
            }
            for (int colIndex = startCol; colIndex <= endCol; colIndex++) {
                Cell cell = row.getCell(colIndex);
                if (cell == null) {
                    cell = row.createCell(colIndex);
                }
                cell.setCellStyle(style);
            }
        }
    }

    // 写入Excel模板数据end

    /**
     * 根据下标索引生成对应英文字母后缀，例：0-A，1-B，26-Z，27-AA，28-AB，26*26-BA，26*26*26-ZZ，26*26*26+1-AAA，26*26*26+2-AAB，...
     * @param index 下标
     * @return 字母后缀
     */
    public static String convertIndexToLetter(int index) {
        StringBuilder result = new StringBuilder();
        while (index >= 0) {
            int remainder = index % 26;
            result.insert(0, (char) ('A' + remainder));
            index = (index / 26) - 1;
        }
        return result.toString();
    }
}