package com.ruoyi.common.core.utils.poi;

import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.annotation.Excel.ColumnType;
import com.ruoyi.common.core.annotation.Excel.Type;
import com.ruoyi.common.core.annotation.Excels;
import com.ruoyi.common.core.domain.SysDictData;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.DictUtils;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.utils.ServletUtils;
import com.ruoyi.common.core.utils.file.FileTypeUtils;
import com.ruoyi.common.core.utils.file.FileUtils;
import com.ruoyi.common.core.utils.file.ImageUtils;
import com.ruoyi.common.core.utils.reflect.ReflectUtils;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.ruoyi.common.exception.CustomException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common.utils.StringUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.util.IOUtils;
import org.apache.poi.xssf.streaming.SXSSFCell;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDataValidation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Excel相关处理
 *
 * @author ruoyi
 */
public class ExcelUtil<T> {
    private static final Logger log = LoggerFactory.getLogger(ExcelUtil.class);

    public static final String XLSX_FILE = ".xlsx";
    public static final String XLS_FILE = ".xls";

    /**
     * Excel sheet最大行数，默认65536
     */
    public static final int SHEET_SIZE = 65536;

    private String lang = null;

    /**
     * 导出时字典缓存
     */
    private ThreadLocal<Map<String, String>> dictDataCach = new ThreadLocal<>();

    /**
     * 工作表名称
     */
    private String sheetName;

    /**
     * 导出类型（EXPORT:导出数据；IMPORT：导入模板）
     */
    private Type type;

    /**
     * 工作薄对象
     */
    private Workbook wb;

    /**
     * 工作表对象
     */
    private Sheet sheet;

    /**
     * 字典对象
     */
    private Sheet dictSheet;

    public Map<String, CellStyle> getStyles() {
        return styles;
    }

    /**
     * 样式列表
     */
    private Map<String, CellStyle> styles;

    /**
     * 导入导出数据列表
     */
    private List<T> list;

    /**
     * 注解列表
     */
    private List<Object[]> fields;

    /**
     * 最大高度
     */
    private short maxHeight;

    /**
     * 统计列表
     */
    private Map<Integer, Double> statistics = new HashMap<Integer, Double>();

    /**
     * 数字格式
     */
    private static final DecimalFormat DOUBLE_FORMAT = new DecimalFormat("######0.00");

    /**
     * 排除的字段
     */
    private Collection<String> exceptField = new ArrayList();

    private TimeZone timeZone;

    /**
     * 实体对象
     */
    public Class<T> clazz;

    public ExcelUtil(Class<T> clazz) {
        exceptField.add("id");
        exceptField.add("isDelete");
        this.clazz = clazz;
        this.timeZone = TimeZone.getDefault();
    }

    public ExcelUtil(Class<T> clazz, TimeZone timeZone) {
        exceptField.add("id");
        exceptField.add("isDelete");
        this.clazz = clazz;
        this.timeZone = timeZone;
    }

    public ExcelUtil(Class<T> clazz, Collection<String> exceptField) {
        this.clazz = clazz;
        this.exceptField = exceptField;
        this.timeZone = TimeZone.getDefault();
    }

    public ExcelUtil(Class<T> clazz, Collection<String> exceptField, TimeZone timeZone) {
        this.clazz = clazz;
        this.exceptField = exceptField;
        this.timeZone = timeZone;
    }

    public void init(List<T> list, String sheetName, Type type) {
        if (list == null) {
            list = new ArrayList<T>();
        }
        this.list = list;
        this.sheetName = sheetName;
        this.type = type;
        createExcelField();
        createWorkbook();
    }

    /**
     * 对excel表单默认第一个索引名转换成list
     *
     * @param is 输入流
     * @return 转换后集合
     */
    public List<T> importExcel(InputStream is) throws Exception {
        return importExcel(StringUtils.EMPTY, is, 0);
    }

    /**
     * 对excel表单默认第一个索引名转换成list
     *
     * @param is 输入流
     * @return 转换后集合
     */
    public List<T> importExcel(InputStream is, Integer headRowNum) throws Exception {
        return importExcel(StringUtils.EMPTY, is, headRowNum);
    }

    /**
     * 对excel表单指定表格索引名转换成list
     *
     * @param sheetName 表格索引名
     * @param is        输入流
     * @return 转换后集合
     */
    public List<T> importExcel(String sheetName, InputStream is, Integer headRowNum) throws Exception {
        this.type = Type.IMPORT;
        this.wb = WorkbookFactory.create(is);
        List<T> list = new LinkedList<>();
        Sheet sheet = null;
        if (StringUtils.isNotEmpty(sheetName)) {
            // 如果指定sheet名,则取指定sheet中的内容.
            sheet = wb.getSheet(sheetName);
        } else {
            // 如果传入的sheet名不存在则默认指向第1个sheet.
            sheet = wb.getSheetAt(0);
        }

        if (sheet == null) {
            throw new IOException(I18nUtil.getMessage("common.error.util.file.sheet.noexist"));
        }

        // 获取最后一个非空行的行下标，比如总行数为n，则返回的为n-1
        int rows = sheet.getLastRowNum();

        if (rows > 0) {
            // 定义一个map用于存放excel列的序号和field.
            Map<String, Integer> cellMap = new HashMap<String, Integer>();
            // 获取表头
            Row heard = sheet.getRow(headRowNum);
            for (int i = 0; i < heard.getPhysicalNumberOfCells(); i++) {
                Cell cell = heard.getCell(i);
                if (StringUtils.isNotNull(cell)) {
                    String value = this.getCellValue(heard, i).toString();
                    cellMap.put(value, i);
                } else {
                    cellMap.put(null, i);
                }
            }
            // 有数据时才处理 得到类的所有field.

            List<Field> allFields = getClassField(clazz);
            dictDataCach.set(new HashMap<>());
            // 定义一个map用于存放列的序号和field.
            Map<Integer, Field> fieldsMap = new HashMap<Integer, Field>();
            for (int col = 0; col < allFields.size(); col++) {
                Field field = allFields.get(col);
                Excel attr = field.getAnnotation(Excel.class);
                if (attr != null && (attr.type() == Type.ALL || attr.type() == type)) {
                    // 设置类的私有字段属性可访问.
                    field.setAccessible(true);
                    //Joran 2020-10-26导入名匹配国际化转换start
                    String attrName = "".equals(attr.importName()) ? attr.name() : attr.importName();
                    if (StringUtils.isNotEmpty(attrName)) {
                        attrName = attrName.replaceAll("\\{", "").replaceAll("\\}", "");
                        attrName = I18nUtil.getMessage(attrName);
                    }
                    //Joran 2020-10-26导入名匹配国际化转换end
                    Integer column = cellMap.get(attrName);
                    if (column != null) {
                        fieldsMap.put(column, field);
                    }
                }
            }
            for (int i = headRowNum + 1; i <= rows; i++) {
                // 从第2行开始取数据,默认第一行是表头.
                Row row = sheet.getRow(i);
                // 判断当前行是否是空行
                if (isRowEmpty(row)) {
                    continue;
                }
                T entity = null;
                //统计对象属性为空的个数，如果对象的所有属性都为空则代表已经读取到末尾，不用在继续读取数据了
                int nullCount = 0;
                for (Map.Entry<Integer, Field> entry : fieldsMap.entrySet()) {

                    Object val = "";
                    int column = entry.getKey();
                    if (isMergedRegion(sheet, i, column)) {
                        val = getMergedRegionValue(sheet, i, column);
                    } else {
                        val = this.getCellValue(row, column);
                    }

                    if (ObjectUtils.isEmpty(val)) {
                        nullCount = nullCount + 1;
                    }

                    // 如果不存在实例则新建.
                    entity = (entity == null ? clazz.newInstance() : entity);
                    // 从map中得到对应列的field.
                    Field field = fieldsMap.get(entry.getKey());
                    Excel attr = field.getAnnotation(Excel.class);
                    String dictType = attr.dictType();
                    if (!"".equals(dictType)) {
                        val = convertByDictValueOrCheckbox(String.valueOf(val), dictType);
                    }
                    // 取得类型,并根据对象类型设置值.
                    Class<?> fieldType = field.getType();
                    if (String.class == fieldType) {
                        String s = Convert.toStr(val);
                        if (StringUtils.endsWith(s, ".0")) {
                            val = StringUtils.substringBefore(s, ".0");
                        } else {
                            String dateFormat = field.getAnnotation(Excel.class).dateFormat();
                            if (StringUtils.isNotEmpty(dateFormat)) {
                                val = DateUtils.parseDateToStr(dateFormat, (Date) val);
                            } else {
                                val = Convert.toStr(val);
                            }
                        }
                    } else if (Integer.TYPE == fieldType || Integer.class == fieldType) {
                        val = Convert.toIntExcelUtil(val, Integer.MAX_VALUE);
                    } else if (Long.TYPE == fieldType || Long.class == fieldType) {
                        val = Convert.toLongExcelUtil(val, Long.MAX_VALUE);
                    } else if (Double.TYPE == fieldType || Double.class == fieldType) {
                        String suffix = attr.suffix();
                        if (StringUtils.isNotEmpty(suffix)) {
                            val = val.toString().replace(suffix, "");
                        }
                        val = Convert.toDoubleExcelUtil(val, Double.MAX_VALUE
                                , attr.scale(), RoundingMode.valueOf(attr.roundingMode()));
                    } else if (Float.TYPE == fieldType || Float.class == fieldType) {
                        val = Convert.toFloatExcelUtil(val, Float.MAX_VALUE
                                , attr.scale(), RoundingMode.valueOf(attr.roundingMode()));
                    } else if (BigDecimal.class == fieldType) {
                        val = Convert.toBigDecimalExcelUtil(val, BigDecimal.valueOf(Double.MIN_VALUE)
                                , attr.scale(), RoundingMode.valueOf(attr.roundingMode()));
                    } else if (Date.class == fieldType) {
                        if (val instanceof String) {
                            Date date = DateUtils.parseDate(val);
                            val = StringUtils.isEmpty(String.valueOf(val)) ? date : (date == null ? DateUtils.parseDate("1970-01-01", "yyyy-MM-dd") : date);
                        } else if (val instanceof Double) {
                            val = DateUtils.getJavaDate((Double) val, timeZone);
                        }
                    } else if (Boolean.TYPE == fieldType || Boolean.class == fieldType) {
                        val = Convert.toBool(val, false);
                    }
                    if (StringUtils.isNotNull(fieldType)) {
                        String propertyName = field.getName();
                        if (StringUtils.isNotEmpty(attr.targetAttr())) {
                            propertyName = field.getName() + "." + attr.targetAttr();
                        } else if (StringUtils.isNotEmpty(attr.readConverterExp())) {
                            val = reverseByExp(Convert.toStr(val), attr.readConverterExp(), attr.separator());
                        }
                        ReflectUtils.invokeSetter(entity, propertyName, val);
                    }
                }
                if (nullCount == fieldsMap.size()) {
                    break;
                } else {
                    list.add(entity);
                }

            }
        }
        dictDataCach.remove();
        return list;
    }

    /**
     * 解析Excel的第column列
     *
     * @param is,headRowNum,column
     * @return List<String>
     * @throws Exception
     */
    public List<String> importExcel4Column(InputStream is, int headRowNum, int column) throws Exception {
        this.wb = WorkbookFactory.create(is);
        List<String> list = new ArrayList<String>();
        Sheet sheet = wb.getSheetAt(0);
        if (sheet == null) {
            throw new IOException(I18nUtil.getMessage("common.error.util.file.sheet.noexist"));
        }
        int rows = sheet.getPhysicalNumberOfRows();
        if (rows > 0) {
            for (int i = headRowNum + 0; i < rows; i++) {
                Row row = sheet.getRow(i);
                Object val = this.getCellValue(row, column);
                if (ObjectUtils.isNotEmpty(val)) {
                    list.add(val + "");
                }
            }
        }
        return list;
    }

    /**
     * 判断指定的单元格是否是合并单元格
     *
     * @param sheet
     * @param row    行下标
     * @param column 列下标
     * @return
     */
    private boolean isMergedRegion(Sheet sheet, int row, int column) {
        int sheetMergeCount = sheet.getNumMergedRegions();
        for (int i = 0; i < sheetMergeCount; i++) {
            CellRangeAddress range = sheet.getMergedRegion(i);
            int firstColumn = range.getFirstColumn();
            int lastColumn = range.getLastColumn();
            int firstRow = range.getFirstRow();
            int lastRow = range.getLastRow();
            if (row >= firstRow && row <= lastRow) {
                if (column >= firstColumn && column <= lastColumn) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 获取合并单元格的值
     *
     * @param sheet
     * @param row
     * @param column
     * @return
     */
    public Object getMergedRegionValue(Sheet sheet, int row, int column) {
        int sheetMergeCount = sheet.getNumMergedRegions();

        for (int i = 0; i < sheetMergeCount; i++) {
            CellRangeAddress ca = sheet.getMergedRegion(i);
            int firstColumn = ca.getFirstColumn();
            int lastColumn = ca.getLastColumn();
            int firstRow = ca.getFirstRow();
            int lastRow = ca.getLastRow();

            if (row >= firstRow && row <= lastRow) {

                if (column >= firstColumn && column <= lastColumn) {
                    Row fRow = sheet.getRow(firstRow);
                    Cell fCell = fRow.getCell(firstColumn);
                    return getCellValue(fCell);
                }
            }
        }
        return "";
    }


    /**
     * 对list数据源将其里面的数据导入到excel表单
     *
     * @param response  返回数据
     * @param list      导出数据集合
     * @param sheetName 工作表的名称
     * @param fileName  文件名
     * @return 结果
     * @throws IOException
     */
    public void exportExcel(HttpServletResponse response, List<T> list, String sheetName, String fileName) throws IOException {

        ExcelUtil.setResponseHeader(response, fileName, XLSX_FILE);

        this.init(list, sheetName, Type.IMPORT);
        exportExcel(response.getOutputStream());
    }

    /**
     * 对list数据源将其里面的数据导入到excel表单
     *
     * @param response 返回数据
     * @param list     导出数据集合
     * @param fileName 文件名
     * @return 结果
     * @throws IOException
     */
    public Workbook exportExcel2(HttpServletResponse response, List<T> list, String fileName) throws IOException {

        ExcelUtil.setResponseHeader(response, fileName, ExcelUtil.XLSX_FILE);

        return exportExcelFromList(list, fileName);
    }

    /**
     * 对list数据源将其里面的数据导入到excel表单
     *
     * @param list     导出数据集合
     * @param fileName 文件名
     * @return 结果
     * @throws IOException
     */
    public Workbook exportExcelFromList(List<T> list, String fileName) throws IOException {

        this.init(list, fileName, Type.EXPORT);
        return exportExcel2();
    }

    /**
     * 填充 Workbook
     *
     * @return Workbook
     */
    public Workbook exportExcel2() {
        try {

            //获取下拉数据集、下拉列位置集
            List<String[]> downDataList = new ArrayList<>();
            List<Integer> downDataLocations = new ArrayList<>();
            getDownDataList(downDataList, downDataLocations);

            // 取出一共有多少个sheet.
            double sheetNo = Math.ceil(list.size() / SHEET_SIZE);
            for (int index = 0; index <= sheetNo; index++) {

                //创建工作表sheet、单元格样式、设置sheetName
                if (CollectionUtils.isNotEmpty(downDataList)) {
                    createSheetWithDict(sheetNo, index);
                } else {
                    createSheet(sheetNo, index);
                }

                //填充表头
                Row row = sheet.createRow(0);
                int column = 0;
                for (Object[] os : fields) {
                    Excel excel = (Excel) os[1];
                    this.createCell(excel, row, column++);
                }

                //为工作页绑定下拉框，并且填充字典页
                if (CollectionUtils.isNotEmpty(downDataList)) {
                    createExcelWithDict(sheet, dictSheet, downDataList, downDataLocations);
                }

                Long bmin = System.currentTimeMillis();

                //填充数据
                if (Type.EXPORT.equals(type)) {
                    dictDataCach.set(new HashMap<>());
                    fillExcelData(index, row);
                    // //自适应宽度(中文支持)
                    //setSizeColumn((SXSSFSheet) this.sheet, column);
                    dictDataCach.remove();
                    addStatisticsRow();
                }
                log.debug("填充数据消耗{}", System.currentTimeMillis() - bmin);
            }
            return wb;
        } catch (Exception e) {
            String errorMsg = StringUtils.format(I18nUtil.getMessage("common.error.util.export.excel.exception"), e.getMessage());
            log.error(errorMsg);
        } finally {
            return wb;
        }
    }

    //自适应宽度(中文支持)
    private void setSizeColumn(SXSSFSheet sheet, int size) {
        for (int columnNum = 0; columnNum < size; columnNum++) {
            //获取列宽
            int columnWidth = sheet.getColumnWidth(columnNum);
            for (int rowNum = 0; rowNum <= sheet.getLastRowNum(); rowNum++) {
                SXSSFRow currentRow = sheet.getRow(rowNum);
                if (currentRow.getCell(columnNum) != null) {
                    SXSSFCell currentCell = currentRow.getCell(columnNum);
                    if (currentCell.getCellType() == CellType.STRING) {
                        int count = 0;//汉字数量
                        String regEx = "[\\u4e00-\\u9fa5]";
                        Pattern p = Pattern.compile(regEx);
                        Matcher m = p.matcher(currentCell.getStringCellValue());
                        int len = m.groupCount();
                        //获取汉字个数
                        while (m.find()) {
                            for (int i = 0; i <= len; i++) {
                                count = count + 1;
                            }
                        }
                        //因为程序中将汉字编译成一个字符，因此我们在该列字符长度的基础上加上汉字个数计算列宽
                        int length = (currentCell.getStringCellValue().length() + count) * 256;
                        if (columnWidth < length) {
                            columnWidth = length;
                        }
                    }
                }
            }
            //设置列宽
            sheet.setColumnWidth(columnNum, columnWidth);
        }
    }

    /**
     * FileItem类对象创建
     *
     * @param inputStream inputStream
     * @param fileName    fileName
     * @return FileItem
     */
    public static FileItem createFileItem(InputStream inputStream, String fileName) {
        FileItemFactory factory = new DiskFileItemFactory(16, null);
        String textFieldName = "file";
        FileItem item = factory.createItem(textFieldName, MediaType.MULTIPART_FORM_DATA_VALUE, true, fileName);
        int bytesRead = 0;
        byte[] buffer = new byte[8192];
        OutputStream os = null;
        //使用输出流输出输入流的字节
        try {
            os = item.getOutputStream();
            while ((bytesRead = inputStream.read(buffer, 0, 8192)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(e.getMessage());
        } finally {
            org.apache.commons.io.IOUtils.closeQuietly(os, inputStream);
        }
        return item;
    }

    public static void setResponseHeader(HttpServletResponse response, String fileName) {
        setResponseHeader(response, fileName, XLSX_FILE);
    }

    public static void setResponseHeader(HttpServletResponse response, String fileName, String fileType) {
        try {
            String downloadFileName = FileUtils.setFileDownloadHeader(ServletUtils.getRequest(), fileName);
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/vnd.ms-excel;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment;filename=" + downloadFileName + fileType);
            response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
        } catch (Throwable e) {
            RuntimeException ex = new RuntimeException("excel下载没有浏览器系统支持解析的语言");
            log.error(ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * 对list数据源将其里面的数据导入到excel表单
     *
     * @param response  返回数据
     * @param list      导出数据集合
     * @param sheetName 工作表的名称
     * @return 结果
     * @throws IOException
     */
    public void exportExcel(HttpServletResponse response, List<T> list, String sheetName) throws IOException {
        this.exportExcel(response, list, sheetName, "export");
    }

    /**
     * 对list数据源将其里面的数据导入到excel表单
     *
     * @param sheetName 工作表的名称
     * @return 结果
     */
    public void importTemplateExcel(HttpServletResponse response, String sheetName) throws IOException {
        setResponseHeader(response, sheetName, XLSX_FILE);
        this.init(null, sheetName, Type.IMPORT);
        exportExcel(response.getOutputStream());
    }

    /**
     * 对list数据源将其里面的数据导入到excel表单
     *
     * @return 结果
     */
    public void exportExcel(OutputStream out) {
        try {
            writeSheet();
            wb.write(out);
        } catch (Exception e) {
            //log.error("导出Excel异常{}", e.getMessage());
            String errorMsg = StringUtils.format(I18nUtil.getMessage("common.error.util.export.excel.exception"), e.getMessage());
            log.error(errorMsg);
        } finally {
            IOUtils.closeQuietly(wb);
            IOUtils.closeQuietly(out);
        }
    }

    /**
     * 创建写入数据到Sheet
     */
    public void writeSheet() {
        //获取下拉数据集、下拉列位置集
        List<String[]> downDataList = new ArrayList<>();
        List<Integer> downDataLocations = new ArrayList<>();
        getDownDataList(downDataList, downDataLocations);

        // 取出一共有多少个sheet.
        double sheetNo = Math.ceil(list.size() / SHEET_SIZE);
        for (int index = 0; index <= sheetNo; index++) {

            //创建工作表sheet、单元格样式、设置sheetName
            if (CollectionUtils.isNotEmpty(downDataList)) {
                createSheetWithDict(sheetNo, index);
            } else {
                createSheet(sheetNo, index);
            }

            //填充第一个sheet的国际化表头、列宽、样式
            Row row = sheet.createRow(0);
            int column = 0;
            for (Object[] os : fields) {
                Excel excel = (Excel) os[1];
                this.createCell(excel, row, column++);
            }

            //为工作页绑定下拉框，并且填充字典页
            if (CollectionUtils.isNotEmpty(downDataList)) {
                createExcelWithDict(sheet, dictSheet, downDataList, downDataLocations);
            }

            //填充数据
            if (Type.EXPORT.equals(type)) {
                dictDataCach.set(new HashMap<>());
                fillExcelData(index, row);
                dictDataCach.remove();
                addStatisticsRow();
            }
        }
    }

    /**
     * 为工作页绑定下拉框，并且填充字典页
     *
     * @param sheet1            工作页
     * @param dictSheet         字典页
     * @param downDataList      下拉对象集
     * @param downDataLocations 工作页需要下拉的列位置
     */
    public void createExcelWithDict(Sheet sheet1, Sheet dictSheet, List<String[]> downDataList, List<Integer> downDataLocations) {

        //arr[index]设置字典项在sheet2的位置
        int index = 0;
        Row row = null;
        Row headRow = null;
        String[] arr = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"};
        for (int i = 0; i < downDataLocations.size(); i++) {

            //sheet1下拉框位置：以下示意：sheet1的rownum列的第1~99行作为下拉框
            int rownum = downDataLocations.get(i);
            CellRangeAddressList regions = new CellRangeAddressList(1, 99, rownum, rownum);

            //下拉数据范围 "Sheet2!$A$2:$A$50" 代表：Sheet2第A2到A50作为下拉列表来源数据
            String[] downDatas = downDataList.get(i);
            String strFormula = "Dictionary!$" + arr[index] + "$2:$" + arr[index] + "$" + (downDatas.length + 1);

            //为sheet1添加绑定
            DataValidationHelper dVHelper = sheet1.getDataValidationHelper();
            DataValidationConstraint constraint = dVHelper.createFormulaListConstraint(strFormula);
            DataValidation dataValidation = dVHelper.createValidation(constraint, regions);
            dataValidation.setSuppressDropDownArrow(true);
            dataValidation.setShowErrorBox(true);
            sheet1.addValidationData(dataValidation);

            //填充字典页的表头及字典值
            Object[] os = fields.get(rownum);
            Excel excel = (Excel) os[1];
            String attrName = StringUtils.isBlank(excel.importName()) ? excel.name() : excel.importName();
            if (StringUtils.isNotEmpty(attrName)) {
                attrName = attrName.replaceAll("\\{", "").replaceAll("\\}", "");
                attrName = I18nUtil.getMessage(attrName);
            }
            if (index == 0) {
                headRow = dictSheet.createRow(0);
                dictSheet.setColumnWidth(0, 4000);
            } else {
                headRow = dictSheet.getRow(0);
            }
            Cell cell = headRow.createCell(index);
            cell.setCellValue(attrName);
            cell.setCellStyle(styles.get("header"));
            for (int j = 0; j < downDatas.length; j++) {
                if (index == 0) {
                    row = dictSheet.createRow(j + 1);
                    dictSheet.setColumnWidth(j + 1, 4000);
                    row.createCell(index).setCellValue(downDatas[j]);
                } else {
                    int rowCount = dictSheet.getLastRowNum();
                    if (j + 1 <= rowCount) {
                        dictSheet.getRow(j + 1).createCell(index).setCellValue(downDatas[j]);
                    } else {
                        dictSheet.setColumnWidth(j + 1, 4000);
                        dictSheet.createRow(j + 1).createCell(index).setCellValue(downDatas[j]);
                    }
                }
            }
            index++;
        }
    }

    /**
     * 填充下拉数据集、下拉位置集
     *
     * @param downDataList      下拉对象集
     * @param downDataLocations 工作页需要下拉的列位置
     */
    public void getDownDataList(List<String[]> downDataList, List<Integer> downDataLocations) {
        for (int i = 0; i < fields.size(); i++) {
            Object[] os = fields.get(i);
            Excel excel = (Excel) os[1];
            String dictType = excel.dictType();
            if (StringUtils.isNotBlank(dictType)) {
                List<SysDictData> dictDatas = DictUtils.getDictCache(dictType);
                if (CollectionUtils.isNotEmpty(dictDatas)) {
                    List<String> lableList = dictDatas.stream().map(a -> a.getDictLabel()).collect(Collectors.toList());
                    String[] strArray = lableList.toArray(new String[lableList.size()]);
                    downDataList.add(strArray);
                    downDataLocations.add(i);
                }
            }
        }
    }


    /**
     * 填充excel数据
     *
     * @param index 序号
     * @param row   单元格行
     */
    public void fillExcelData(int index, Row row) {
        int startNo = index * SHEET_SIZE;
        int endNo = Math.min(startNo + SHEET_SIZE, list.size());

        //只取一次语言,存到缓存
        this.lang = SecurityUtils.getUserLang().toString();

        for (int i = startNo; i < endNo; i++) {
            row = sheet.createRow(i + 1 - startNo);
            // 得到导出对象.
            T vo = list.get(i);
            int column = 0;
            for (Object[] os : fields) {
                Field field = (Field) os[0];
                Excel excel = (Excel) os[1];
                // 设置实体类私有属性可访问
                field.setAccessible(true);
                this.addCell(excel, row, vo, field, column++);
//                SXSSFSheet sxssfSheet = (SXSSFSheet) sheet;
//                sxssfSheet.trackAllColumnsForAutoSizing();
//                sxssfSheet.autoSizeColumn(column);
            }
        }
    }

    /**
     * 创建表格样式
     *
     * @param wb 工作薄对象
     * @return 样式列表
     */
    private Map<String, CellStyle> createStyles(Workbook wb) {
        // 写入各条记录,每条记录对应excel表中的一行
        Map<String, CellStyle> styles = new HashMap<String, CellStyle>();
        CellStyle style = wb.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderRight(BorderStyle.THIN);
        style.setRightBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setBorderLeft(BorderStyle.THIN);
        style.setLeftBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setBorderTop(BorderStyle.THIN);
        style.setTopBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setBorderBottom(BorderStyle.THIN);
        style.setBottomBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
        Font dataFont = wb.createFont();
        dataFont.setFontName("Arial");
        dataFont.setFontHeightInPoints((short) 10);
        style.setFont(dataFont);
        styles.put(Constants.DATA, style);

        style = wb.createCellStyle();
        style.cloneStyleFrom(styles.get(Constants.DATA));
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font headerFont = wb.createFont();
        headerFont.setFontName("Arial");
        headerFont.setFontHeightInPoints((short) 10);
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(headerFont);
        styles.put("header", style);

        style = wb.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        Font totalFont = wb.createFont();
        totalFont.setFontName("Arial");
        totalFont.setFontHeightInPoints((short) 10);
        style.setFont(totalFont);
        styles.put("total", style);

        style = wb.createCellStyle();
        style.cloneStyleFrom(styles.get("data"));
        style.setAlignment(HorizontalAlignment.LEFT);
        styles.put("data1", style);

        style = wb.createCellStyle();
        style.cloneStyleFrom(styles.get("data"));
        style.setAlignment(HorizontalAlignment.CENTER);
        styles.put("data2", style);

        style = wb.createCellStyle();
        style.cloneStyleFrom(styles.get("data"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        styles.put("data3", style);

        return styles;
    }

    /**
     * 创建单元格
     */
    public Cell createCell(Excel attr, Row row, int column) {
        // 创建列
        Cell cell = row.createCell(column);
        // 写入列信息
        //Joran 2020-10-21列名添加国际化转换start
        String attrName = attr.name();
        if (StringUtils.isNotEmpty(attrName)) {
            attrName = attrName.replaceAll("\\{", "").replaceAll("\\}", "");
            attrName = I18nUtil.getMessage(attrName);
        }
        cell.setCellValue(attrName);
        //Joran 2020-10-21列名添加国际化转换end
        setDataValidation(attr, row, column);
        cell.setCellStyle(styles.get("header"));
        return cell;
    }

    /**
     * 设置单元格信息
     *
     * @param value 单元格值
     * @param attr  注解相关
     * @param cell  单元格信息
     */
    public void setCellVo(Object value, Excel attr, Cell cell) {
        if (ColumnType.STRING == attr.cellType()) {
            cell.setCellValue(StringUtils.isNull(value) ? attr.defaultValue() : value + attr.suffix());
        } else if (ColumnType.NUMERIC == attr.cellType()) {
            if (StringUtils.isNotNull(value)) {
                cell.setCellValue(StringUtils.contains(Convert.toStr(value), ".") ? Convert.toDouble(value) : Convert.toInt(value));
            }
        } else if (ColumnType.IMAGE == attr.cellType()) {
            ClientAnchor anchor = new XSSFClientAnchor(0, 0, 0, 0, (short) cell.getColumnIndex(), cell.getRow().getRowNum(), (short) (cell.getColumnIndex() + 1), cell.getRow().getRowNum() + 1);
            String imagePath = Convert.toStr(value);
            if (StringUtils.isNotEmpty(imagePath)) {
                byte[] data = ImageUtils.getImage(imagePath);
                getDrawingPatriarch(cell.getSheet()).createPicture(anchor,
                        cell.getSheet().getWorkbook().addPicture(data, getImageType(data)));
            }
        }
    }

    /**
     * 获取画布
     */
    public static Drawing<?> getDrawingPatriarch(Sheet sheet) {
        if (sheet.getDrawingPatriarch() == null) {
            sheet.createDrawingPatriarch();
        }
        return sheet.getDrawingPatriarch();
    }

    /**
     * 获取图片类型,设置图片插入类型
     */
    public int getImageType(byte[] value) {
        String type = FileTypeUtils.getFileExtendName(value);
        if ("JPG".equalsIgnoreCase(type)) {
            return Workbook.PICTURE_TYPE_JPEG;
        } else if ("PNG".equalsIgnoreCase(type)) {
            return Workbook.PICTURE_TYPE_PNG;
        }
        return Workbook.PICTURE_TYPE_JPEG;
    }

    /**
     * 创建表格样式
     */
    public void setDataValidation(Excel attr, Row row, int column) {
        //2020-10-26国际化树形处理start
        String attrName = attr.name();
        if (StringUtils.isNotEmpty(attrName)) {
            attrName = attrName.replaceAll("\\{", "").replaceAll("\\}", "");
            attrName = I18nUtil.getMessage(attrName);
        }
        //2020-10-26国际化树形处理end
        if (attrName.indexOf(I18nUtil.getMessage("common.util.remark")) >= 0) {
            sheet.setColumnWidth(column, 6000);
        } else {
            // 设置列宽
            sheet.setColumnWidth(column, (int) ((attr.width() + 0.72) * 256));
        }
        // 如果设置了提示信息则鼠标放上去提示.
        if (StringUtils.isNotEmpty(attr.prompt())) {
            // 这里默认设了2-101列提示.
            setXSSFPrompt(sheet, "", attr.prompt(), 1, 100, column, column);
        }
        // 如果设置了combo属性则本列只能选择不能输入
        if (attr.combo().length > 0) {
            // 这里默认设了2-101列只能选择不能输入.
            setXSSFValidation(sheet, attr.combo(), 1, 100, column, column);
        }
    }

    /**
     * 添加单元格
     */
    public Cell addCell(Excel attr, Row row, T vo, Field field, int column) {
        Cell cell = null;
        try {
            // 设置行高
            row.setHeight(maxHeight);
            // 根据Excel中设置情况决定是否导出,有些情况需要保持为空,希望用户填写这一列.
            if (attr.isExport()) {
                // 创建cell
                cell = row.createCell(column);
                int align = attr.align().value();
                cell.setCellStyle(styles.get(Constants.DATA + (align >= 1 && align <= 3 ? align : "")));

                // 用于读取对象中的属性
                Object value = getTargetValue(vo, field, attr);
                String dateFormat = attr.dateFormat();
                String readConverterExp = attr.readConverterExp();
                String separator = attr.separator();
                String dictType = attr.dictType();
                if (StringUtils.isNotEmpty(dateFormat) && StringUtils.isNotNull(value)) {
                    cell.setCellValue(DateUtils.parseDateToStr(dateFormat, (Date) value));
                } else if (StringUtils.isNotEmpty(readConverterExp) && StringUtils.isNotNull(value)) {
                    cell.setCellValue(convertByExp(Convert.toStr(value), readConverterExp, separator));
                } else if (value instanceof BigDecimal && -1 != attr.scale()) {
                    cell.setCellValue((((BigDecimal) value).setScale(attr.scale(), attr.roundingMode())).toString());
                } else if (StringUtils.isNotEmpty(dictType) && StringUtils.isNotNull(value)) {
                    cell.setCellValue(convertByDictOrCheckbox(String.valueOf(value), dictType));
                } else {
                    // 设置列类型
                    setCellVo(value, attr, cell);
                }
                addStatisticsData(column, Convert.toStr(value), attr);
            }
        } catch (Exception e) {
            //log.error("导出Excel失败{}", e);
            String errorMsg = StringUtils.format(I18nUtil.getMessage("common.error.util.export.excel.fail"), e);
            log.error(errorMsg);
            throw new CustomException("common.error.util.export.excel.fail", e);
        }
        return cell;
    }

    /**
     * 根据复选框解析字段项的值（propertyValue可为为多个值如：2,3）
     *
     * @param propertyValue
     * @param dictType
     * @return
     * @throws Exception
     */
    public String convertByDictOrCheckbox(String propertyValue, String dictType) throws Exception {
        StringBuffer cellValue = new StringBuffer();
        if (propertyValue.contains(",")) {
            String[] ss = propertyValue.split(",");
            for (String s : ss) {
                cellValue.append(convertByDict(s, dictType)).append(",");
            }
        } else {
            return convertByDict(propertyValue, dictType);
        }
        return cellValue.substring(0, cellValue.length() - 1).toString();
    }

    /**
     * 通过字典类型解析导出值（propertyValue为单个值）
     *
     * @param propertyValue 参数值（字典数据键值）
     * @param dictType      字典类型
     * @return 解析后值（字典数据标签）
     * @throws Exception
     */
    public String convertByDict(String propertyValue, String dictType) throws Exception {

        return DictUtils.convertByDictExport(propertyValue, dictType, dictDataCach.get(), lang);
    }

    public String convertByDictValue(String propertyValue, String dictType) throws Exception {
        return DictUtils.convertByDictImport(propertyValue, dictType, dictDataCach.get());
    }

    /**
     * 根据复选框解析字段项的值（propertyValue可为为多个值如：2,3）
     *
     * @param propertyValue
     * @param dictType
     * @return
     * @throws Exception
     */
    public String convertByDictValueOrCheckbox(String propertyValue, String dictType) throws Exception {
        StringBuffer cellValue = new StringBuffer();
        if (propertyValue.contains(",")) {
            String[] ss = propertyValue.split(",");
            for (String s : ss) {
                cellValue.append(convertByDictValue(s, dictType)).append(",");
            }
        } else {
            return convertByDictValue(propertyValue, dictType);
        }
        return cellValue.substring(0, cellValue.length() - 1);
    }

    public static ThreadLocal<Map<String, String>> convertByDictValueUseMap4ValueCheck(ThreadLocal<Map<String, String>> dictDataCach, String dictType) throws Exception {
        return DictUtils.convertByDictValueUseMap4ValueCheck(dictDataCach, dictType);
    }

    /**
     * 增加排除字段
     *
     * @param fieldName
     */
    public void addExceptField(String fieldName) {
        if (StringUtils.isBlank(fieldName)) {
            return;
        }
        exceptField.add(fieldName);
    }

    /**
     * 设置 POI XSSFSheet 单元格提示
     *
     * @param sheet         表单
     * @param promptTitle   提示标题
     * @param promptContent 提示内容
     * @param firstRow      开始行
     * @param endRow        结束行
     * @param firstCol      开始列
     * @param endCol        结束列
     */
    public void setXSSFPrompt(Sheet sheet, String promptTitle, String promptContent, int firstRow, int endRow,
                              int firstCol, int endCol) {
        DataValidationHelper helper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = helper.createCustomConstraint("DD1");
        CellRangeAddressList regions = new CellRangeAddressList(firstRow, endRow, firstCol, endCol);
        DataValidation dataValidation = helper.createValidation(constraint, regions);
        //Joran 2020-10-26 解析内容国际化转换start
        String i18PromptContent = promptContent;
        if (StringUtils.isNotEmpty(i18PromptContent)) {
            i18PromptContent = i18PromptContent.replaceAll("\\{", "").replaceAll("\\}", "");
            i18PromptContent = I18nUtil.getMessage(i18PromptContent);
            if (StringUtils.isEmpty(i18PromptContent)) {//当没有获取到国际化信息时用原始字符串进行解析
                i18PromptContent = promptContent;
            }
        }
        //Joran 2020-10-26 解析内容国际化转换end
        dataValidation.createPromptBox(promptTitle, i18PromptContent);
        dataValidation.setShowPromptBox(true);
        sheet.addValidationData(dataValidation);
    }

    /**
     * 设置某些列的值只能输入预制的数据,显示下拉框.
     *
     * @param sheet    要设置的sheet.
     * @param textlist 下拉框显示的内容
     * @param firstRow 开始行
     * @param endRow   结束行
     * @param firstCol 开始列
     * @param endCol   结束列
     * @return 设置好的sheet.
     */
    public void setXSSFValidation(Sheet sheet, String[] textlist, int firstRow, int endRow, int firstCol, int endCol) {
        DataValidationHelper helper = sheet.getDataValidationHelper();
        // 加载下拉列表内容
        DataValidationConstraint constraint = helper.createExplicitListConstraint(textlist);
        // 设置数据有效性加载在哪个单元格上,四个参数分别是：起始行、终止行、起始列、终止列
        CellRangeAddressList regions = new CellRangeAddressList(firstRow, endRow, firstCol, endCol);
        // 数据有效性对象
        DataValidation dataValidation = helper.createValidation(constraint, regions);
        // 处理Excel兼容性问题
        if (dataValidation instanceof XSSFDataValidation) {
            dataValidation.setSuppressDropDownArrow(true);
            dataValidation.setShowErrorBox(true);
        } else {
            dataValidation.setSuppressDropDownArrow(false);
        }

        sheet.addValidationData(dataValidation);
    }

    /**
     * 解析导出值 0=男,1=女,2=未知
     *
     * @param propertyValue 参数值
     * @param converterExp  翻译注解
     * @param separator     分隔符
     * @return 解析后值
     */
    public static String convertByExp(String propertyValue, String converterExp, String separator) {
        StringBuilder propertyString = new StringBuilder();
        //Joran 2020-10-26 解析内容国际化转换start
        String i18ConverterExp = converterExp;
        if (StringUtils.isNotEmpty(i18ConverterExp)) {
            i18ConverterExp = i18ConverterExp.replaceAll("\\{", "").replaceAll("\\}", "");
            i18ConverterExp = I18nUtil.getMessage(i18ConverterExp);
            if (StringUtils.isEmpty(i18ConverterExp)) {//当没有获取到国际化信息时用原始字符串进行解析
                i18ConverterExp = converterExp;
            }
        }
        //Joran 2020-10-26 解析内容国际化转换end
        String[] convertSource = i18ConverterExp.split(",");
        for (String item : convertSource) {
            String[] itemArray = item.split("=");
            if (StringUtils.containsAny(separator, propertyValue)) {
                for (String value : propertyValue.split(separator)) {
                    if (itemArray[0].trim().equals(value)) {
                        propertyString.append(itemArray[1] + separator);
                        break;
                    }
                }
            } else {
                if (itemArray[0].trim().equals(propertyValue)) {
                    return itemArray[1];
                }
            }
        }
        return StringUtils.stripEnd(propertyString.toString(), separator);
    }

    /**
     * 反向解析值 男=0,女=1,未知=2
     *
     * @param propertyValue 参数值
     * @param converterExp  翻译注解
     * @param separator     分隔符
     * @return 解析后值
     */
    public static String reverseByExp(String propertyValue, String converterExp, String separator) {
        StringBuilder propertyString = new StringBuilder();
        //Joran 2020-10-26 解析内容国际化转换start
        String i18ConverterExp = converterExp;
        if (StringUtils.isNotEmpty(i18ConverterExp)) {
            i18ConverterExp = i18ConverterExp.replaceAll("\\{", "").replaceAll("\\}", "");
            i18ConverterExp = I18nUtil.getMessage(i18ConverterExp);
            if (StringUtils.isEmpty(i18ConverterExp)) {//当没有获取到国际化信息时用原始字符串进行解析
                i18ConverterExp = converterExp;
            }
        }
        //Joran 2020-10-26 解析内容国际化转换end
        String[] convertSource = i18ConverterExp.split(",");
        for (String item : convertSource) {
            String[] itemArray = item.split("=");
            if (StringUtils.containsAny(separator, propertyValue)) {
                for (String value : propertyValue.split(separator)) {
                    if (itemArray[1].equals(value)) {
                        propertyString.append(itemArray[0].trim() + separator);
                        break;
                    }
                }
            } else {
                if (itemArray[1].equals(propertyValue)) {
                    return itemArray[0].trim();
                }
            }
        }
        return StringUtils.stripEnd(propertyString.toString(), separator);
    }

    /**
     * 合计统计信息
     */
    private void addStatisticsData(Integer index, String text, Excel entity) {
        if (entity != null && entity.isStatistics()) {
            Double temp = 0D;
            if (!statistics.containsKey(index)) {
                statistics.put(index, temp);
            }
            try {
                temp = Double.valueOf(text);
            } catch (NumberFormatException e) {
            }
            statistics.put(index, statistics.get(index) + temp);
        }
    }

    /**
     * 创建统计行
     */
    public void addStatisticsRow() {
        if (statistics.size() > 0) {
            Cell cell = null;
            Row row = sheet.createRow(sheet.getLastRowNum() + 1);
            Set<Integer> keys = statistics.keySet();
            cell = row.createCell(0);
            cell.setCellStyle(styles.get("total"));
            cell.setCellValue("合计");

            for (Integer key : keys) {
                cell = row.createCell(key);
                cell.setCellStyle(styles.get("total"));
                cell.setCellValue(DOUBLE_FORMAT.format(statistics.get(key)));
            }
            statistics.clear();
        }
    }

    /**
     * 获取bean中的属性值
     *
     * @param vo    实体对象
     * @param field 字段
     * @param excel 注解
     * @return 最终的属性值
     * @throws Exception
     */
    private Object getTargetValue(T vo, Field field, Excel excel) throws Exception {
        Object o = field.get(vo);
        if (StringUtils.isNotEmpty(excel.targetAttr())) {
            String target = excel.targetAttr();
            if (target.indexOf(".") > -1) {
                String[] targets = target.split("[.]");
                for (String name : targets) {
                    o = getValue(o, name);
                }
            } else {
                o = getValue(o, target);
            }
        }
        return o;
    }

    /**
     * 以类的属性的get方法方法形式获取值
     *
     * @param o
     * @param name
     * @return value
     * @throws Exception
     */
    private Object getValue(Object o, String name) throws Exception {
        if (StringUtils.isNotNull(o) && StringUtils.isNotEmpty(name)) {
            Class<?> clazz = o.getClass();
            Field field = clazz.getDeclaredField(name);
            field.setAccessible(true);
            o = field.get(o);
        }
        return o;
    }

    /**
     * 得到所有定义字段
     */
    private void createExcelField() {
        this.fields = new ArrayList<Object[]>();
        List<Field> tempFields = getClassField(clazz);

        List<Field> newTempFields = tempFields.stream()
                .filter(
                        item -> {
                            if (exceptField.contains(item.getName())) {
                                return false;
                            }
                            return true;
                        }
                ).collect(Collectors.toList());

        for (Field field : newTempFields) {
            // 单注解
            if (field.isAnnotationPresent(Excel.class)) {
                putToField(field, field.getAnnotation(Excel.class));
            }

            // 多注解
            if (field.isAnnotationPresent(Excels.class)) {
                Excels attrs = field.getAnnotation(Excels.class);
                Excel[] excels = attrs.value();
                for (Excel excel : excels) {
                    putToField(field, excel);
                }
            }
        }
        this.fields = this.fields.stream()
                .sorted(Comparator.comparing(objects -> ((Excel) objects[1]).sort())).collect(Collectors.toList());
        this.maxHeight = getRowHeight();
    }

    /**
     * @param tClass
     * @return
     * @author linbn 210924
     */
    public List<Field> getClassField(Class<T> tClass) {

        List<Field> tempFields = new ArrayList<>();
        while (tClass != null) {
            tempFields.addAll(Arrays.asList(tClass.getDeclaredFields()));
            tClass = (Class<T>) tClass.getSuperclass();

            if (StringUtils.equals(tClass.getSimpleName(), BaseEntity.class.getSimpleName())) {
                break;
            }
        }
        return tempFields;
    }

    /**
     * 根据注解获取最大行高
     */
    public short getRowHeight() {
        double maxHeight = 0;
        for (Object[] os : this.fields) {
            Excel excel = (Excel) os[1];
            maxHeight = maxHeight > excel.height() ? maxHeight : excel.height();
        }
        return (short) (maxHeight * 20);
    }

    /**
     * 放到字段集合中
     */
    private void putToField(Field field, Excel attr) {
        if (attr != null && (attr.type() == Type.ALL || attr.type() == type)) {
            this.fields.add(new Object[]{field, attr});
        }
    }

    /**
     * 创建一个工作簿
     */
    public void createWorkbook() {
        this.wb = new SXSSFWorkbook(2000);
    }

    /**
     * 创建工作表
     *
     * @param sheetNo sheet数量
     * @param index   序号
     */
    public void createSheet(double sheetNo, int index) {
        this.sheet = wb.createSheet();
        this.styles = createStyles(wb);
        // 设置工作表的名称.
        if (sheetNo == 0) {
            wb.setSheetName(index, sheetName);
        } else {
            wb.setSheetName(index, sheetName + index);
        }
    }

    /**
     * 创建工作表(带字典sheet页)、创建样式
     *
     * @param sheetNo sheet数量
     * @param index   序号
     */
    public void createSheetWithDict(double sheetNo, int index) {
        this.sheet = wb.createSheet();
        this.styles = createStyles(wb);
        this.dictSheet = wb.createSheet("Dictionary");
        //wb.setSheetHidden(1,true);
        // 设置工作表的名称.
        if (sheetNo == 0) {
            wb.setSheetName(index, sheetName);
        } else {
            wb.setSheetName(index, sheetName + index);
        }
    }

    /**
     * 获取单元格值
     *
     * @param row    获取的行
     * @param column 获取单元格列号
     * @return 单元格值
     */
    public Object getCellValue(Row row, int column) {
        if (row == null) {
            return row;
        }
        Object val = "";
        try {
            Cell cell = row.getCell(column);
            if (StringUtils.isNotNull(cell)) {
                if (cell.getCellType() == CellType.NUMERIC || cell.getCellType() == CellType.FORMULA) {
                    val = cell.getNumericCellValue();
                    if (DateUtil.isCellDateFormatted(cell)) {
                        val = DateUtils.getJavaDate((Double) val, timeZone); // POI Excel 日期格式转换
                    } else {
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
                } else if (cell.getCellType() == CellType.ERROR) {
                    val = cell.getErrorCellValue();
                }

            }
        } catch (Exception e) {
            return val;
        }
        return val;
    }

    /**
     * 判断是否是空行
     *
     * @param row 判断的行
     * @return
     */
    private boolean isRowEmpty(Row row) {
        if (row == null) {
            return true;
        }
        for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取单元格值
     *
     * @param cell 单元格
     * @return 单元格值
     */
    public Object getCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        Object val = "";
        try {
            if (StringUtils.isNotNull(cell)) {
                if (cell.getCellType() == CellType.NUMERIC || cell.getCellType() == CellType.FORMULA) {
                    val = cell.getNumericCellValue();
                    if (DateUtil.isCellDateFormatted(cell)) {
                        val = DateUtils.getJavaDate((Double) val, timeZone); // POI Excel 日期格式转换
                    } else {
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
                } else if (cell.getCellType() == CellType.ERROR) {
                    val = cell.getErrorCellValue();
                }

            }
        } catch (Exception e) {
            return val;
        }
        return val;
    }
}
