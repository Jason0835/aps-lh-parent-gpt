package com.zlt.aps.monthplan.common.utils.poi;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.annotation.Excels;
import com.ruoyi.common.core.domain.SysDictData;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.DictUtils;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.utils.file.FileTypeUtils;
import com.ruoyi.common.core.utils.file.ImageUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.exception.CustomException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanMouldDayResult;
import com.zlt.aps.monthplan.api.domain.entity.MpSimulatedResult;
import com.zlt.common.utils.ExcelReadUtils;
import lombok.Getter;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDataValidation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;

/**
 * Excel相关处理
 *
 * @author ruoyi
 */
@Getter
public class ExcelUtilManySheet {

  private static final Logger log = LoggerFactory.getLogger(ExcelUtilManySheet.class);

  /**
   * Excel sheet最大行数，默认65536
   */
  public static final int SHEET_SIZE = 65536;

  private String lang = null;

  /**
   * 导出时字典缓存
   */
  private final ThreadLocal<Map<String, String>> dictDataCach = new ThreadLocal<>();
  /**
   * 导出类型（EXPORT:导出数据；IMPORT：导入模板）
   */
  private Excel.Type type;
  /**
   * 工作薄对象
   */
  private Workbook wb;

  /**
   * 工作表对象
   */
  private List<Sheet> sheets;

  /**
   * 字典对象
   */
  private Sheet dictSheet;
  /**
   * 样式列表
   */
  private Map<String, CellStyle> styles;
  /**
   * 导入导出数据列表
   */
  private List<WorksheetData> list;

  /**
   * 注解列表
   */
  private Map<String,List<Object[]>> fieldsMap;

  /**
   * 最大高度
   */
  private Map<String,Double>  maxHeightMap;
  /**
   * 统计列表
   */
  private final Map<Integer, Double> statistics = Maps.newHashMap();
  /**
   * 数字格式
   */
  private static final DecimalFormat DOUBLE_FORMAT = new DecimalFormat("######0.00");
  /**
   * 排除的字段
   */
  private final Collection<String> exceptField = Lists.newArrayList();

  private final TimeZone timeZone;

  public ExcelUtilManySheet() {
    exceptField.add("id");
    exceptField.add("isDelete");
    this.timeZone = TimeZone.getDefault();
  }

  /**
   * 对list数据源将其里面的数据导入到excel表单
   *
   * @param list     导出数据集合名
   * @return 结果
   */
  public void exportExcelFromList(List<WorksheetData> list) {
    this.init(list, Excel.Type.EXPORT);
    exportExcel2();
  }

  private void exportExcel2() {
    try {
      //获取下拉数据集、下拉列位置集
      Map<String,List<String[]>> downDataListMap = Maps.newHashMap();
      Map<String,List<Integer>> downDataLocationsMap = Maps.newHashMap();
      getDownDataListMap(downDataListMap, downDataLocationsMap);
      WorksheetData worksheetData;
      List<Object[]> fields;
      // 取出一共有多少个sheet.
      int sheetNumber = this.sheets.size();
      List<String[]> downDataList;
      List<Integer> downDataLocationList;
      short maxHeight;
      for (int index = 0; index <= sheetNumber - 1; index++) {
        worksheetData = this.list.get(index);
        fields = this.fieldsMap.get(worksheetData.getSheetName());
        downDataList = downDataListMap.get(worksheetData.getSheetName());
        downDataLocationList = downDataLocationsMap.get(worksheetData.getSheetName());
        maxHeight = this.maxHeightMap.get(worksheetData.getSheetName()).shortValue();
        //填充表头
        Row row = sheets.get(index).createRow(0);
        int column = 0;
        for (Object[] os : fields) {
          Excel excel = (Excel) os[1];
          this.createCell(excel,sheets.get(index), row, column++);
        }
        //为工作页绑定下拉框，并且填充字典页
        if (!CollectionUtils.isEmpty(downDataList)) {
          createExcelWithDict(sheets.get(index),fields, dictSheet, downDataList, downDataLocationList);
        }
        long bmin = System.currentTimeMillis();
        //填充数据
        if (Excel.Type.EXPORT.equals(type)) {
          dictDataCach.set(new HashMap<>());
          fillExcelData(index,sheets.get(index), maxHeight,worksheetData,fields);
          // //自适应宽度(中文支持)
          //setSizeColumn((SXSSFSheet) this.sheet, column);
          dictDataCach.remove();
          addStatisticsRow(sheets.get(index));
        }
        log.debug("填充数据消耗{}", System.currentTimeMillis() - bmin);
      }
    } catch (Exception e) {
      String errorMsg = StringUtils.format(I18nUtil.getMessage("common.error.util.export.excel.exception"), e.getMessage());
      log.error(errorMsg);
    }
  }

  /**
   * 填充 Workbook
   *
   */
  public void exportExcel2(HttpServletResponse response, List<WorksheetData>  list, String fileName) {
    ExcelUtil.setResponseHeader(response, fileName, ExcelUtil.XLSX_FILE);
    exportExcelFromList(list);
  }

  /**
   * 创建统计行
   */
  public void addStatisticsRow(Sheet sheet) {
    if (!CollectionUtils.isEmpty(statistics)) {
      Cell cell;
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
   * 填充excel数据
   *
   * @param index
   *     序号
   */
  public void fillExcelData(int index, Sheet sheet, short maxHeight, WorksheetData worksheetData, List<Object[]> fields) {
    int startNo = index * SHEET_SIZE;
    int endNo = Math.min(startNo + SHEET_SIZE, list.size());

    //只取一次语言,存到缓存
    this.lang = SecurityUtils.getUserLang().toString();

    for (int i = startNo; i < endNo; i++) {
      Row row = sheet.createRow(i + 1 - startNo);
      int column = 0;
      for (Object[] os : fields) {
        Field field = (Field) os[0];
        Excel excel = (Excel) os[1];
        // 设置实体类私有属性可访问
        field.setAccessible(true);
        this.addCell(excel, row,maxHeight,worksheetData, i, field, column++);
      }
    }
  }

  /**
   * 添加单元格
   */
  public Cell addCell(Excel attr, Row row,short maxHeight,WorksheetData worksheetData, int index, Field field, int column) {
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
        Object value = getTargetValue(worksheetData,index, field, attr);
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
        } else if (StringUtils.isNotEmpty(dictType) && value != null && StringUtils.isNotBlank(value.toString())) {
           String dictLabel = convertByDict(value.toString(), dictType);
           log.info("value={},dictType={},dictLabel:{}", value, dictType, dictLabel);
           if(StringUtils.isNotBlank(dictLabel)){
             cell.setCellValue(dictLabel);
           }
        } else {
          // 设置列类型
          setCellVo(value, attr, cell);
        }
        addStatisticsData(column, Convert.toStr(value), attr);
      }
    } catch (Exception e) {
      e.printStackTrace();
      //log.error("导出Excel失败{}", e);
      String errorMsg = StringUtils.format(I18nUtil.getMessage("common.error.util.export.excel.fail"), e);
      log.error(errorMsg);
      throw new CustomException("common.error.util.export.excel.fail", e);
    }
    return cell;
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
        log.error("数字转换异常:{}",e.getMessage());
      }
      statistics.put(index, statistics.get(index) + temp);
    }
  }

  /**
   * 设置单元格信息
   *
   * @param value 单元格值
   * @param attr  注解相关
   * @param cell  单元格信息
   */
  public void setCellVo(Object value, Excel attr, Cell cell) {
    if (Excel.ColumnType.STRING == attr.cellType()) {
      cell.setCellValue(StringUtils.isNull(value) ? attr.defaultValue() : value + attr.suffix());
    } else if (Excel.ColumnType.NUMERIC == attr.cellType()) {
      if (StringUtils.isNotNull(value)) {
        cell.setCellValue(StringUtils.contains(Convert.toStr(value), ".") ? Convert.toDouble(value) : Convert.toInt(value));
      }
    } else if (Excel.ColumnType.IMAGE == attr.cellType()) {
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

  public static Drawing<?> getDrawingPatriarch(Sheet sheet) {
    if (sheet.getDrawingPatriarch() == null) {
      sheet.createDrawingPatriarch();
    }
    return sheet.getDrawingPatriarch();
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
    StringBuilder cellValue = new StringBuilder();
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
      i18ConverterExp = i18ConverterExp.replaceAll("\\{", "").replaceAll("}", "");
      i18ConverterExp = I18nUtil.getMessage(i18ConverterExp);
      //当没有获取到国际化信息时用原始字符串进行解析
      if (StringUtils.isEmpty(i18ConverterExp)) {
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
            propertyString.append(itemArray[1]).append(separator);
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
   * 获取bean中的属性值
   *
   * @param field 字段
   * @param excel 注解
   * @return 最终的属性值
   * @throws Exception
   */
  private Object getTargetValue(WorksheetData worksheetData, int index,Field field, Excel excel) throws Exception {
    Object o;
    if(!CollectionUtils.isEmpty(worksheetData.getSimulatedResults())) {
       MpSimulatedResult simulatedResult = worksheetData.getSimulatedResults().get(index);
       o = field.get(simulatedResult);
    }else{
      FactoryMonthPlanMouldDayResult mouldDayResult = worksheetData.getMouldDayResults().get(index);
      o = field.get(mouldDayResult);
    }
    if (StringUtils.isNotEmpty(excel.targetAttr())) {
      String target = excel.targetAttr();
      if (target.contains(".")) {
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
   * 为工作页绑定下拉框，并且填充字典页
   *
   * @param sheet1            工作页
   * @param dictSheet         字典页
   * @param downDataList      下拉对象集
   * @param downDataLocations 工作页需要下拉的列位置
   */
  public void createExcelWithDict(Sheet sheet1,List<Object[]> fields, Sheet dictSheet, List<String[]> downDataList, List<Integer> downDataLocations) {

    //arr[index]设置字典项在sheet2的位置
    int index = 0;
    Row row;
    Row headRow;
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
        attrName = attrName.replaceAll("\\{", "").replaceAll("}", "");
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
   * 创建单元格
   */
  public Cell createCell(Excel attr, Sheet sheet,Row row, int column) {
    // 创建列
    Cell cell = row.createCell(column);
    // 写入列信息
    //Joran 2020-10-21列名添加国际化转换start
    String attrName = attr.name();
    if (StringUtils.isNotEmpty(attrName)) {
      attrName = attrName.replaceAll("\\{", "").replaceAll("}", "");
      attrName = I18nUtil.getMessage(attrName);
    }
    cell.setCellValue(attrName);
    //Joran 2020-10-21列名添加国际化转换end
    setDataValidation(attr,sheet, column);
    cell.setCellStyle(styles.get("header"));
    return cell;
  }

  /**
   * 创建表格样式
   */
  public void setDataValidation(Excel attr, Sheet sheet, int column) {
    //2020-10-26国际化树形处理start
    String attrName = attr.name();
    if (StringUtils.isNotEmpty(attrName)) {
      attrName = attrName.replaceAll("\\{", "").replaceAll("}", "");
      attrName = I18nUtil.getMessage(attrName);
    }
    //2020-10-26国际化树形处理end
    if (attrName.contains(I18nUtil.getMessage("common.util.remark"))) {
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
   * 设置某些列的值只能输入预制的数据,显示下拉框.
   *
   * @param sheet    要设置的sheet.
   * @param textlist 下拉框显示的内容
   * @param firstRow 开始行
   * @param endRow   结束行
   * @param firstCol 开始列
   * @param endCol   结束列
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
      i18PromptContent = i18PromptContent.replaceAll("\\{", "").replaceAll("}", "");
      i18PromptContent = I18nUtil.getMessage(i18PromptContent);
      //当没有获取到国际化信息时用原始字符串进行解析
      if (StringUtils.isEmpty(i18PromptContent)) {
        i18PromptContent = promptContent;
      }
    }
    //Joran 2020-10-26 解析内容国际化转换end
    dataValidation.createPromptBox(promptTitle, i18PromptContent);
    dataValidation.setShowPromptBox(true);
    sheet.addValidationData(dataValidation);
  }


  /**
   * 填充下拉数据集、下拉位置集
   *
   * @param downDataListMap      下拉对象集
   * @param downDataLocationsMap 工作页需要下拉的列位置
   */
  public void getDownDataListMap(Map<String,List<String[]>> downDataListMap,Map<String, List<Integer>> downDataLocationsMap) {
    this.fieldsMap.forEach((sheetName, fields) -> {
      List<String[]> downDataList = Lists.newArrayList();
      List<Integer> downDataLocations = Lists.newArrayList();
      for (int i = 0; i < fields.size(); i++) {
        Object[] os = fields.get(i);
        Excel excel = (Excel) os[1];
        String dictType = excel.dictType();
        if (StringUtils.isNotBlank(dictType)) {
          List<SysDictData> dictDatas = DictUtils.getDictCache(dictType);
          if (!CollectionUtils.isEmpty(dictDatas)) {
            String[] strArray = dictDatas.stream().map(SysDictData::getDictLabel).toArray(String[]::new);
            downDataList.add(strArray);
            downDataLocations.add(i);
          }
        }
      }
      downDataListMap.put(sheetName, downDataList);
      downDataLocationsMap.put(sheetName, downDataLocations);
    });

  }

  public void init(List<WorksheetData> list,Excel.Type type) {
    if (CollectionUtils.isEmpty(list)) {
      list = Lists.newArrayList();
    }
    createWorkbook();
    this.list = list;
    this.type = type;
    this.sheets = Lists.newArrayList();
    this.maxHeightMap = Maps.newHashMap();
    this.styles = createStyles(wb);
    // 取出一共有多少个sheet.
    int sheetNumber = list.size();
    for (int index = 0; index <= sheetNumber - 1; index++) {
      //创建工作表sheet、单元格样式、设置sheetName
      createSheet(index,list.get(index));
    }
    this.dictSheet = wb.createSheet("Dictionary");
    createExcelField();

  }

  private void createExcelField() {
    this.fieldsMap = Maps.newHashMap();
    for(WorksheetData sheetData : list) {
      List<Object[]>  fields = Lists.newArrayList();
      List<Field> tempFields;
      if(CollectionUtils.isEmpty(sheetData.getSimulatedResults())) {
         tempFields = getClassField(MpSimulatedResult.class);
      }else{
         tempFields = getClassField(FactoryMonthPlanMouldDayResult.class);
      }

      List<Field> newTempFields = tempFields.stream()
          .filter(
              item -> !exceptField.contains(item.getName())
          ).collect(Collectors.toList());

      for (Field field : newTempFields) {
        // 单注解
        if (field.isAnnotationPresent(Excel.class)) {
          putToField(fields,field, field.getAnnotation(Excel.class));
        }
        // 多注解
        if (field.isAnnotationPresent(Excels.class)) {
          Excels attrs = field.getAnnotation(Excels.class);
          Excel[] excels;
          if (attrs != null) {
            excels = attrs.value();
            for (Excel excel : excels) {
              putToField(fields,field, excel);
            }
          }
        }
      }
      fields = fields.stream()
          .sorted(Comparator.comparing(objects -> ((Excel) objects[1]).sort())).collect(Collectors.toList());
      this.maxHeightMap.put(sheetData.getSheetName(),getRowHeight(fields));
      this.fieldsMap.put(sheetData.getSheetName(), fields);
    }

  }

  /**
   * 根据注解获取最大行高
   */
  public double getRowHeight(List<Object[]>  fields) {
    double maxHeight = 0;
    for (Object[] os : fields) {
      Excel excel = (Excel) os[1];
      maxHeight = Math.max(maxHeight, excel.height());
    }
    return  (maxHeight * 20);
  }

  /**
   * 放到字段集合中
   */
  private void putToField(List<Object[]>  fields,Field field, Excel attr) {
    if (attr != null && (attr.type() == Excel.Type.ALL || attr.type() == type)) {
        fields.add(new Object[]{field, attr});
    }
  }

  /**
   * @param tClass
   * @return
   * @author linbn 210924
   */
  public List<Field> getClassField(Class tClass) {
    return new ArrayList<>(Arrays.asList(tClass.getDeclaredFields()));
  }

  private void createSheet(int index, WorksheetData worksheetData) {
    sheets.add(wb.createSheet());
    // 设置工作表的名称.
    wb.setSheetName(index, worksheetData.getSheetName());
  }

  /**
   * 创建表格样式
   *
   * @param wb 工作薄对象
   * @return 样式列表
   */
  private Map<String, CellStyle> createStyles(Workbook wb) {
    // 写入各条记录,每条记录对应excel表中的一行
    Map<String, CellStyle> styles = Maps.newHashMap();
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

  public byte[] exportData(List<WorksheetData> worksheetDatas, String fileName, HttpServletResponse response) throws IOException {
    this.exportExcel2(response, worksheetDatas, fileName);
    return ExcelReadUtils.writeExcel(this.wb);
  }

  /**
   * 创建一个工作簿
   */
  public void createWorkbook() {
    this.wb = new SXSSFWorkbook(2000);
  }
}

