package com.zlt.aps.gsq.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.common.core.utils.ExcelUtils;
import com.zlt.aps.gsq.api.domain.dto.GsqScheduleResultImportDTO;
import com.zlt.aps.gsq.api.domain.entity.GsqMachineInfo;
import com.zlt.aps.gsq.api.domain.entity.GsqScheduleResult;
import com.zlt.aps.gsq.domain.vo.GsqScheduleResultExcelParseResult;
import com.zlt.aps.gsq.domain.vo.GsqScheduleResultVo;
import com.zlt.aps.gsq.mapper.GsqMachineInfoMapper;
import com.zlt.aps.gsq.mapper.GsqScheduleResultMapper;
import com.zlt.aps.gsq.service.IGsqScheduleResultExcelService;
import com.zlt.common.utils.ImportExcelUtils;
import com.zlt.core.dao.basedao.BaseDao;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 钢丝圈排程结果模板导入导出服务实现。
 *
 * <p>参照胎面 {@code TmScheduleResultExcelServiceImpl} 的模板导入导出：导出时将
 * {@link GsqScheduleResultVo} 的 {@link Excel#name()} 国际化回写值写入模板第 1 行
 * （隐藏元数据行）的 {@code {fieldName}} 占位符；导入时按第 1 行国际化表头匹配列号，
 * 逐行解析为 {@code List<GsqScheduleResultVo>}。</p>
 *
 * <p>模板业务键：{@code machineCode|steelRingCode}（同排程日期、同机台、同钢丝圈唯一）。
 * 导入仅持久化计划量相关字段（class1~3PlanQty 等），其余展示列不落库。</p>
 *
 * @author APS
 */
@Slf4j
@Service
public class GsqScheduleResultExcelServiceImpl implements IGsqScheduleResultExcelService {

    /** 模板资源路径。 */
    private static final String TEMPLATE_RESOURCE = "excelModel/gsqScheduleResult.xlsx";

    /** 导出工作表名称（导入按此名称匹配工作表）。 */
    private static final String SHEET_NAME = "Sheet1";

    /** 标题日期格式。 */
    private static final String TITLE_DATE_FORMAT = "yyyy年MM月dd日";

    /** 模板标题日期正则。 */
    private static final Pattern TITLE_DATE_PATTERN = Pattern.compile("^(\\d{4}年\\d{2}月\\d{2}日).*$");

    /** 隐藏的国际化表头行索引（第 1 行）。 */
    private static final int HEADER_ROW_INDEX = 0;

    /** 标题行索引（第 2 行）。 */
    private static final int TITLE_ROW_INDEX = 1;

    /** Excel 明细起始行索引，第 5 行。 */
    private static final int DATA_START_ROW_INDEX = 4;

    /** 模板可见班次数（class1~3，对应 I:N 列）。 */
    private static final int IMPORT_SHIFT_COUNT = 3;

    /** 导入数据来源（对齐实体注释：0-自动排程，1-插单，2-导入）。 */
    private static final String IMPORT_DATA_SOURCE = "2";

    @Resource
    private GsqScheduleResultMapper gsqScheduleResultMapper;

    /** 通用批量写入服务。 */
    @Resource
    private BaseDao baseDao;

    @Resource
    private GsqMachineInfoMapper gsqMachineInfoMapper;

    @Resource
    private PlatformTransactionManager platformTransactionManager;

    @Resource
    private IExportLogService iExportLogService;

    @Resource
    private IImportLogService iImportLogService;

    @Resource
    private IImportErrorLogService iImportErrorLogService;

    /**
     * 获取标题占位符 map。
     *
     * @param queryVO 参数
     * @return 标题占位符
     */
    private static Map<String, Object> getTitleMap(GsqScheduleResult queryVO) {
        Map<String, Object> tableMap = new HashMap<>();
        String formatDate = DateUtil.format(queryVO.getScheduleDate(), TITLE_DATE_FORMAT);
        tableMap.put("planDate", formatDate);
        return tableMap;
    }

    /**
     * 库存、计划量、完成量为 0（或 null）时返回 null，使导出单元格留空，不显示无意义的 0。
     *
     * @param value 数值
     * @return 非空且非 0 返回原值，否则返回 null
     */
    private static BigDecimal blankIfZero(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return value;
    }

    /**
     * 按专用模板导出钢丝圈排程结果。
     *
     * @param queryVO 查询条件，必须包含工厂和排程日期
     * @param fileName 导出文件名称
     * @return Excel 文件字节
     * @throws ServiceException 查询条件不完整、模板不存在或文件生成失败时抛出
     */
    @Override
    public byte[] exportDataScheduleResult(GsqScheduleResult queryVO, String fileName) {
        this.validateExportQuery(queryVO);
        Date beginTime = DateUtils.getNowDate();
        Date scheduleDate = DateUtil.beginOfDay(queryVO.getScheduleDate());
        String currentBatchNo = this.resolveCurrentBatchNo(queryVO.getFactoryCode(), scheduleDate);
        List<GsqScheduleResult> resultList = this.listExportResults(queryVO, currentBatchNo);
        Map<String, GsqScheduleResult> previousResultMap = this.buildPreviousResultMap(queryVO);
        List<Map<String, Object>> dataList = this.buildExportDataList(resultList, previousResultMap);
        List<List<Map<String, Object>>> excelDataList = new ArrayList<>();
        if (CollUtil.isNotEmpty(dataList)) {
            excelDataList.add(dataList);
        }

        byte[] resultBytes;
        try (InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(TEMPLATE_RESOURCE)) {
            if (inputStream == null) {
                throw new ServiceException(I18nUtil.getMessage("ui.data.alert.gsq.schedule.excel.templateMissing"));
            }
            Map<String, Object> tableMap = this.getTitleMap(queryVO);
            // 回写第 1 行 @Excel 国际化字段名，作为导入列匹配表头
            this.setExportTitleFieldName(tableMap);
            resultBytes = ExcelUtils.writeMultiList(inputStream, 0, tableMap, excelDataList);
        } catch (IOException exception) {
            log.error("生成钢丝圈排程结果模板失败", exception);
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.gsq.schedule.excel.generateFailed"));
        }
        resultBytes = this.finishExportWorkbook(resultBytes, dataList.isEmpty());
        this.saveExportLog(queryVO, fileName, resultList.size(), beginTime);
        return resultBytes;
    }

    /**
     * 按专用模板导入钢丝圈排程结果。
     *
     * @param importDTO 导入文件和业务条件
     * @param updateSupport 已存在记录是否更新
     * @return 导入结果和错误明细
     * @throws Exception 导入日志或文件读取异常时抛出
     */
    @Override
    public AjaxResult importDataScheduleResult(GsqScheduleResultImportDTO importDTO, boolean updateSupport) throws Exception {
        ImportContext importContext = this.validateImportContext(importDTO);
        Date beginTime = DateUtils.getNowDate();
        ImportLog importLog = ImportExcelUtils.getImportLogAndUploadFile(importContext.getFileBytes(),
                importContext.getImportFilePath(), importContext.getProcedureCode(), importContext.getFunctionName(),
                importContext.getOriFileName(), 1);
        importLog = this.iImportLogService.add(importLog);
        AjaxResult ajaxResult;
        int rowCount = 0;
        try {
            GsqScheduleResultExcelParseResult parseResult = this.parseWorkbook(importContext.getFileBytes(),
                    importDTO.getScheduleResult());
            rowCount = parseResult.getRowList().size();
            ajaxResult = this.doImport(parseResult, importDTO.getScheduleResult(), updateSupport, importLog.getId());
        } catch (ServiceException exception) {
            log.warn("钢丝圈排程结果模板导入校验失败，原因={}", exception.getMessage());
            ajaxResult = AjaxResult.error(exception.getMessage());
        } catch (RuntimeException exception) {
            log.error("钢丝圈排程结果模板导入失败", exception);
            ajaxResult = AjaxResult.error(I18nUtil.getMessage("ui.data.alert.gsq.schedule.excel.importFailed"));
        }
        Date endTime = DateUtils.getNowDate();
        importLog.setRowCount(rowCount);
        importLog.setBeginTime(beginTime);
        importLog.setEndTime(endTime);
        importLog.setSpendTime(DateUtils.getDiffTime(endTime, beginTime));
        ImportExcelUtils.updateImportLogAndFormatMsg(importLog, ajaxResult, this.iImportLogService);
        ImportExcelUtils.saveImportErrorLogs(ajaxResult, this.iImportErrorLogService);
        return ajaxResult;
    }

    /**
     * 校验导出查询条件。
     *
     * @param queryVO 导出查询条件
     * @throws ServiceException 工厂或排程日期为空时抛出
     */
    private void validateExportQuery(GsqScheduleResult queryVO) {
        if (queryVO == null || StrUtil.isBlank(queryVO.getFactoryCode())) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.gsq.schedule.excel.factoryRequired"));
        }
        if (queryVO.getScheduleDate() == null) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.gsq.schedule.excel.dateRequired"));
        }
    }

    /**
     * 校验导入文件和业务上下文。
     *
     * @param importDTO 导入请求
     * @return 导入文件上下文
     * @throws ServiceException 文件、工厂或排程日期为空时抛出
     */
    private ImportContext validateImportContext(GsqScheduleResultImportDTO importDTO) {
        if (importDTO == null || importDTO.getImportContext() == null
                || importDTO.getImportContext().getFileBytes() == null
                || importDTO.getImportContext().getFileBytes().length == 0) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.gsq.schedule.excel.fileRequired"));
        }
        String originalFileName = StrUtil.trim(importDTO.getImportContext().getOriFileName());
        if (StrUtil.isNotBlank(originalFileName)
                && !originalFileName.toLowerCase(Locale.ROOT).endsWith(ExcelUtil.XLSX_FILE)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.gsq.schedule.excel.fileTypeInvalid"));
        }
        if (importDTO.getScheduleResult() == null
                || StrUtil.isBlank(importDTO.getScheduleResult().getFactoryCode())) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.gsq.schedule.excel.factoryRequired"));
        }
        if (importDTO.getScheduleResult().getScheduleDate() == null) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.gsq.schedule.excel.dateRequired"));
        }
        return importDTO.getImportContext();
    }

    /**
     * 查询导出明细（当前批次）。
     *
     * @param queryVO        查询条件
     * @param currentBatchNo 当前有效批次号
     * @return 每条排程结果对应一条导出明细
     */
    private List<GsqScheduleResult> listExportResults(GsqScheduleResult queryVO, String currentBatchNo) {
        LambdaQueryWrapper<GsqScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GsqScheduleResult::getFactoryCode, queryVO.getFactoryCode());
        wrapper.eq(GsqScheduleResult::getScheduleDate, DateUtil.beginOfDay(queryVO.getScheduleDate()));
        wrapper.eq(GsqScheduleResult::getBatchNo, currentBatchNo);
        wrapper.eq(StrUtil.isNotBlank(queryVO.getMachineCode()), GsqScheduleResult::getMachineCode,
                queryVO.getMachineCode());
        wrapper.like(StrUtil.isNotBlank(queryVO.getSteelRingCode()), GsqScheduleResult::getSteelRingCode,
                queryVO.getSteelRingCode());
        wrapper.orderByAsc(GsqScheduleResult::getMachineCode, GsqScheduleResult::getClass1Sequence,
                GsqScheduleResult::getClass2Sequence, GsqScheduleResult::getClass3Sequence,
                GsqScheduleResult::getSteelRingCode, GsqScheduleResult::getId);
        return this.gsqScheduleResultMapper.selectList(wrapper);
    }

    /**
     * 构建前一日当前批次结果映射，用于回填模板"前日计划/完成"列。
     *
     * <p>前一日 = 排程日期减一天；仅取前一日当前批次（同工厂）中 CLASS3 的计划/完成量，
     * 按 {@code buildResultBusinessKey}（机台|钢丝圈代码）建键，供导出明细回填。</p>
     *
     * @param queryVO 导出条件（含工厂与排程日期）
     * @return 前一日当前批次结果映射；无数据时返回空 map
     */
    private Map<String, GsqScheduleResult> buildPreviousResultMap(GsqScheduleResult queryVO) {
        Date scheduleDate = DateUtil.beginOfDay(queryVO.getScheduleDate());
        Date previousDate = DateUtil.offsetDay(scheduleDate, -1);
        String previousBatchNo = this.resolveCurrentBatchNo(queryVO.getFactoryCode(), previousDate);
        LambdaQueryWrapper<GsqScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GsqScheduleResult::getFactoryCode, queryVO.getFactoryCode());
        wrapper.eq(GsqScheduleResult::getScheduleDate, previousDate);
        wrapper.eq(StrUtil.isNotBlank(previousBatchNo), GsqScheduleResult::getBatchNo, previousBatchNo);
        List<GsqScheduleResult> previousList = this.gsqScheduleResultMapper.selectList(wrapper);
        if (CollUtil.isEmpty(previousList)) {
            return Collections.emptyMap();
        }
        return previousList.stream().collect(Collectors.toMap(
                this::buildResultBusinessKey, Function.identity(), (a, b) -> a, HashMap::new));
    }

    /**
     * 将 {@link GsqScheduleResultVo} 各 {@link Excel} 字段的国际化名称写入表头占位符 map，
     * 供模板第 1 行 {@code {fieldName}} 占位符回写，作为导入列匹配表头。
     *
     * @param tableMap 表头占位符 map
     */
    private void setExportTitleFieldName(Map<String, Object> tableMap) {
        for (Field field : GsqScheduleResultVo.class.getDeclaredFields()) {
            Excel attr = field.getAnnotation(Excel.class);
            if (attr == null || (attr.type() != Excel.Type.ALL && attr.type() != Excel.Type.IMPORT)) {
                continue;
            }
            String attrName = StrUtil.blankToDefault(attr.importName(), attr.name());
            if (StrUtil.isNotBlank(attrName)) {
                attrName = attrName.replaceAll("\\{", "").replaceAll("}", "");
                attrName = I18nUtil.getMessage(attrName);
            }
            tableMap.put(field.getName(), attrName);
        }
    }

    /**
     * 构建模板明细数据。
     *
     * <p>钢丝圈实体无 {@code cxRemainQty/materialDesc/curlRollLength/cxMachineCode} 等展示字段，
     * 这些列（C/D/P/Q）不落库且无数据源，导出留空；仅填充与实体对应的持久化字段。
     * 前日计划/完成列由前一日当前批次 CLASS3 数据回填。</p>
     *
     * @param resultList       排程结果
     * @param previousResultMap 前一日当前批次结果（按机台|钢丝圈代码建键）
     * @return 模板明细映射
     */
    private List<Map<String, Object>> buildExportDataList(List<GsqScheduleResult> resultList,
                                                          Map<String, GsqScheduleResult> previousResultMap) {
        if (CollUtil.isEmpty(resultList)) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (GsqScheduleResult result : resultList) {
            Map<String, Object> rowMap = new LinkedHashMap<>();
            GsqScheduleResult previousResult = previousResultMap.get(this.buildResultBusinessKey(result));
            rowMap.put("steelRingCode", result.getSteelRingCode());
            rowMap.put("count", null);
            rowMap.put("cxRemainQty", null);
            rowMap.put("materialDesc", null);
            rowMap.put("specifications", result.getProSize());
            rowMap.put("stockQty", this.blankIfZero(BigDecimalUtils.valueOf(result.getStockQty())));
            rowMap.put("lastDayPlanQty", this.blankIfZero(BigDecimalUtils.valueOf(
                    previousResult == null ? null : previousResult.getClass3PlanQty())));
            rowMap.put("lastDayFinishQty", this.blankIfZero(BigDecimalUtils.valueOf(
                    previousResult == null ? null : previousResult.getClass3FinishQty())));
            rowMap.put("class1PlanQty", this.blankIfZero(BigDecimalUtils.valueOf(result.getClass1PlanQty())));
            rowMap.put("class1FinishQty", this.blankIfZero(BigDecimalUtils.valueOf(result.getClass1FinishQty())));
            rowMap.put("class2PlanQty", this.blankIfZero(BigDecimalUtils.valueOf(result.getClass2PlanQty())));
            rowMap.put("class2FinishQty", this.blankIfZero(BigDecimalUtils.valueOf(result.getClass2FinishQty())));
            rowMap.put("class3PlanQty", this.blankIfZero(BigDecimalUtils.valueOf(result.getClass3PlanQty())));
            rowMap.put("class3FinishQty", this.blankIfZero(BigDecimalUtils.valueOf(result.getClass3FinishQty())));
            rowMap.put("cxPlanQty", null);
            rowMap.put("curlRollLength", null);
            rowMap.put("cxMachineCode", null);
            rowMap.put("machineCode", result.getMachineCode());
            rowMap.put("resultId", result.getId());
            dataList.add(rowMap);
        }
        return dataList;
    }

    /**
     * 完成导出工作簿的隐藏表头行和空模板清理。
     *
     * @param sourceBytes 模板填充后的字节
     * @param emptyData 是否为空模板
     * @return 最终工作簿字节
     * @throws ServiceException 工作簿处理失败时抛出
     */
    private byte[] finishExportWorkbook(byte[] sourceBytes, boolean emptyData) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(sourceBytes);
             XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.getSheetAt(0);
            workbook.setSheetName(0, SHEET_NAME);
            // 隐藏第 1 行国际化表头元数据行
            this.getOrCreateRow(sheet, HEADER_ROW_INDEX).setZeroHeight(true);
            this.normalizeEmptyStringCells(sheet);
            if (emptyData) {
                Row dataRow = this.getOrCreateRow(sheet, DATA_START_ROW_INDEX);
                for (int columnIndex = 0; columnIndex < dataRow.getLastCellNum(); columnIndex++) {
                    this.getOrCreateCell(dataRow, columnIndex).setBlank();
                }
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            log.error("处理钢丝圈排程结果导出工作簿失败", exception);
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.gsq.schedule.excel.generateFailed"));
        }
    }

    /**
     * 将模板引擎生成的空字符串单元格标准化为空白单元格。
     *
     * @param sheet 待处理工作表
     */
    private void normalizeEmptyStringCells(Sheet sheet) {
        for (Row row : sheet) {
            for (Cell cell : row) {
                if (cell.getCellType() == CellType.STRING && StrUtil.isEmpty(cell.getStringCellValue())) {
                    cell.setBlank();
                }
            }
        }
    }

    /**
     * 解析导入工作簿。
     *
     * @param fileBytes Excel 文件字节
     * @param condition 导入条件
     * @return 标题日期和有效明细行
     * @throws ServiceException 模板、标题或日期不合法时抛出
     */
    private GsqScheduleResultExcelParseResult parseWorkbook(byte[] fileBytes, GsqScheduleResult condition) {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(fileBytes))) {
            Sheet sheet = workbook.getSheet(SHEET_NAME);
            if (sheet == null || sheet.getRow(HEADER_ROW_INDEX) == null) {
                throw new ServiceException(I18nUtil.getMessage("ui.data.alert.gsq.schedule.excel.templateInvalid"));
            }
            DataFormatter dataFormatter = new DataFormatter();
            FormulaEvaluator formulaEvaluator = workbook.getCreationHelper().createFormulaEvaluator();
            String title = this.readCellText(sheet.getRow(TITLE_ROW_INDEX), 0, dataFormatter, formulaEvaluator);
            Date scheduleDate = this.parseScheduleDate(title);
            if (!DateUtil.isSameDay(scheduleDate, condition.getScheduleDate())) {
                throw new ServiceException(I18nUtil.getMessage("ui.data.alert.gsq.schedule.excel.dateMismatch"));
            }
            List<GsqScheduleResultVo> rowList = this.parseVoList(sheet, dataFormatter, formulaEvaluator);
            if (rowList.isEmpty()) {
                throw new ServiceException(I18nUtil.getMessage("ui.data.alert.gsq.schedule.excel.noData"));
            }
            GsqScheduleResultExcelParseResult parseResult = new GsqScheduleResultExcelParseResult();
            parseResult.setScheduleDate(DateUtil.beginOfDay(scheduleDate));
            parseResult.setRowList(rowList);
            return parseResult;
        } catch (ServiceException exception) {
            throw exception;
        } catch (Exception exception) {
            log.warn("解析钢丝圈排程结果模板失败", exception);
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.gsq.schedule.excel.templateInvalid"));
        }
    }

    /**
     * 解析模板标题中的排程日期。
     *
     * @param title 模板标题
     * @return 排程日期
     * @throws ServiceException 标题格式不正确时抛出
     */
    private Date parseScheduleDate(String title) {
        Matcher matcher = TITLE_DATE_PATTERN.matcher(StrUtil.blankToDefault(title, ""));
        if (!matcher.matches()) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.gsq.schedule.excel.titleInvalid"));
        }
        try {
            return DateUtil.parse(matcher.group(1), TITLE_DATE_FORMAT);
        } catch (RuntimeException exception) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.gsq.schedule.excel.titleInvalid"));
        }
    }

    /**
     * 按第 1 行国际化表头匹配列号，将数据行解析为 {@link GsqScheduleResultVo} 列表。
     *
     * <p>表头文本与 {@link Excel#name()} 的 {@link I18nUtil#getMessage(String)} 回写值同口径匹配，
     * 列位置变更不影响解析；类型转换按字段类型显式处理（String/BigDecimal/Integer/Long）。</p>
     *
     * @param sheet 模板工作表
     * @param dataFormatter 单元格格式化器
     * @param formulaEvaluator 公式计算器
     * @return 明细行对象列表
     */
    private List<GsqScheduleResultVo> parseVoList(Sheet sheet, DataFormatter dataFormatter,
                                                  FormulaEvaluator formulaEvaluator) {
        // 第 1 行隐藏国际化表头 -> i18n 名 -> 列号
        Row headerRow = sheet.getRow(HEADER_ROW_INDEX);
        Map<String, Integer> headerCellMap = new HashMap<>();
        if (headerRow != null) {
            for (int columnIndex = 0; columnIndex < headerRow.getLastCellNum(); columnIndex++) {
                String headerText = this.readCellText(headerRow, columnIndex, dataFormatter, formulaEvaluator);
                if (StrUtil.isNotBlank(headerText)) {
                    headerCellMap.put(headerText.trim(), columnIndex);
                }
            }
        }
        // VO @Excel 字段 -> 列号
        Map<Integer, Field> fieldsMap = new LinkedHashMap<>();
        for (Field field : GsqScheduleResultVo.class.getDeclaredFields()) {
            Excel attr = field.getAnnotation(Excel.class);
            if (attr == null || (attr.type() != Excel.Type.ALL && attr.type() != Excel.Type.IMPORT)) {
                continue;
            }
            String attrName = StrUtil.blankToDefault(attr.importName(), attr.name());
            if (StrUtil.isNotBlank(attrName)) {
                attrName = attrName.replaceAll("\\{", "").replaceAll("}", "");
                attrName = I18nUtil.getMessage(attrName);
            }
            Integer columnIndex = headerCellMap.get(attrName);
            if (columnIndex != null) {
                field.setAccessible(true);
                fieldsMap.put(columnIndex, field);
            }
        }
        // 第 5 行起逐行解析，全空行停止
        List<GsqScheduleResultVo> rowList = new ArrayList<>();
        for (int rowIndex = DATA_START_ROW_INDEX; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            GsqScheduleResultVo vo = new GsqScheduleResultVo();
            vo.setRowNum(rowIndex + 1);
            int emptyCount = 0;
            for (Map.Entry<Integer, Field> entry : fieldsMap.entrySet()) {
                String text = this.readCellText(row, entry.getKey(), dataFormatter, formulaEvaluator);
                if (StrUtil.isBlank(text)) {
                    emptyCount++;
                    continue;
                }
                Object converted = this.convertCellValue(text, entry.getValue().getType());
                if (converted != null) {
                    try {
                        entry.getValue().set(vo, converted);
                    } catch (IllegalAccessException exception) {
                        // setAccessible(true) 已调用，运行期不会触发；仅满足编译期受检异常处理
                        throw new RuntimeException(exception);
                    }
                }
            }
            if (fieldsMap.isEmpty() || emptyCount == fieldsMap.size()) {
                break;
            }
            rowList.add(vo);
        }
        return rowList;
    }

    /**
     * 按字段类型转换单元格文本。
     *
     * @param text 单元格文本
     * @param fieldType 字段类型
     * @return 转换后的值；无法转换时返回 null
     */
    private Object convertCellValue(String text, Class<?> fieldType) {
        String normalized = text.replace(",", "");
        if (fieldType == String.class) {
            // 数值文本去尾 ".0"
            if (normalized.endsWith(".0")) {
                return normalized.substring(0, normalized.length() - 2);
            }
            return text;
        }
        try {
            BigDecimal decimal = new BigDecimal(normalized);
            if (fieldType == BigDecimal.class) {
                return decimal;
            }
            if (fieldType == Integer.class || fieldType == Integer.TYPE) {
                return decimal.intValue();
            }
            if (fieldType == Long.class || fieldType == Long.TYPE) {
                return decimal.longValue();
            }
        } catch (RuntimeException ignore) {
            return null;
        }
        return text;
    }

    /**
     * 校验并落库导入明细。
     *
     * @param parseResult Excel 解析结果
     * @param condition 导入条件
     * @param updateSupport 已存在记录是否更新
     * @param importLogId 导入日志 ID
     * @return 导入结果
     */
    private AjaxResult doImport(GsqScheduleResultExcelParseResult parseResult, GsqScheduleResult condition,
                                boolean updateSupport, Long importLogId) {
        String factoryCode = condition.getFactoryCode().trim();
        Date scheduleDate = parseResult.getScheduleDate();
        List<GsqScheduleResultVo> rowList = parseResult.getRowList();
        Set<String> machineCodeSet = rowList.stream().map(GsqScheduleResultVo::getMachineCode)
                .filter(StrUtil::isNotBlank).map(String::trim).collect(Collectors.toSet());
        Set<String> validMachineCodeSet = this.loadValidMachineCodes(machineCodeSet);
        Map<String, List<GsqScheduleResult>> existingResultMap = this.loadExistingResultMap(factoryCode,
                scheduleDate, machineCodeSet);

        AjaxResult hardBlockResult = this.checkHardBlock(rowList, existingResultMap, importLogId);
        if (hardBlockResult != null) {
            return hardBlockResult;
        }

        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<GsqScheduleResultVo> validRowList = new ArrayList<>();
        Set<String> importBusinessKeySet = new HashSet<>();
        for (GsqScheduleResultVo row : rowList) {
            this.validateImportRow(row, validMachineCodeSet, existingResultMap, updateSupport,
                    importBusinessKeySet);
            if (CollUtil.isNotEmpty(row.getErrors())) {
                importErrorLogs.add(new ImportErrorLog(importLogId, row.getRowNum(),
                        String.join(";", row.getErrors())));
                continue;
            }
            validRowList.add(row);
        }
        if (validRowList.isEmpty()) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + ",0,"
                    + importErrorLogs.size(), importErrorLogs);
        }

        String batchNo = this.resolveCurrentBatchNo(factoryCode, scheduleDate);
        List<GsqScheduleResult> insertList = new ArrayList<>();
        List<GsqScheduleResult> updateList = new ArrayList<>();
        int insertOrder = 1;
        for (GsqScheduleResultVo row : validRowList) {
            String businessKey = this.buildImportBusinessKey(row.getMachineCode(), row.getSteelRingCode());
            List<GsqScheduleResult> existingList = existingResultMap.get(businessKey);
            if (CollUtil.isNotEmpty(existingList)) {
                GsqScheduleResult target = existingList.get(0);
                this.applyImportPlanFields(row, target);
                updateList.add(target);
                continue;
            }
            GsqScheduleResult target = this.buildInsertedResult(row, factoryCode, scheduleDate, batchNo, insertOrder++);
            insertList.add(target);
        }
        this.persistImportRows(insertList, updateList);
        int successNum = insertList.size() + updateList.size();
        if (CollUtil.isNotEmpty(importErrorLogs)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + ","
                    + successNum + "," + importErrorLogs.size(), importErrorLogs);
        }
        return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
    }

    /**
     * 校验会阻断整批导入的发布状态和重复有效记录。
     *
     * @param rowList 导入行
     * @param existingResultMap 已有记录映射
     * @param importLogId 导入日志 ID
     * @return 阻断结果；不存在阻断时返回 null
     */
    private AjaxResult checkHardBlock(List<GsqScheduleResultVo> rowList,
                                      Map<String, List<GsqScheduleResult>> existingResultMap, Long importLogId) {
        for (GsqScheduleResultVo row : rowList) {
            if (StrUtil.isBlank(row.getMachineCode()) || StrUtil.isBlank(row.getSteelRingCode())) {
                continue;
            }
            String businessKey = this.buildImportBusinessKey(row.getMachineCode(), row.getSteelRingCode());
            List<GsqScheduleResult> existingList = existingResultMap.get(businessKey);
            if (CollUtil.isEmpty(existingList)) {
                continue;
            }
            if (existingList.size() > 1) {
                String message = MessageFormat.format(I18nUtil.getMessage(
                        "ui.data.alert.gsq.schedule.excel.multipleMatched"), row.getRowNum(), businessKey);
                return AjaxResult.error(message,
                        Collections.singletonList(new ImportErrorLog(importLogId, row.getRowNum(), message)));
            }
            if (!ApsConstant.NO_RELEASE.equals(existingList.get(0).getIsRelease())) {
                String message = MessageFormat.format(I18nUtil.getMessage(
                        "ui.data.alert.gsq.schedule.excel.releaseBlocked"), row.getRowNum(), businessKey);
                return AjaxResult.error(message,
                        Collections.singletonList(new ImportErrorLog(importLogId, row.getRowNum(), message)));
            }
        }
        return null;
    }

    /**
     * 校验单条导入数据。
     *
     * @param row 导入行
     * @param validMachineCodeSet 有效机台编码
     * @param existingResultMap 已有记录映射
     * @param updateSupport 已存在记录是否更新
     * @param importBusinessKeySet 文件内业务键集合
     */
    private void validateImportRow(GsqScheduleResultVo row, Set<String> validMachineCodeSet,
                                   Map<String, List<GsqScheduleResult>> existingResultMap,
                                   boolean updateSupport, Set<String> importBusinessKeySet) {
        if (StrUtil.isBlank(row.getSteelRingCode())) {
            row.getErrors().add(this.formatRowMessage("ui.data.alert.gsq.schedule.excel.steelRingRequired", row.getRowNum()));
        } else {
            row.setSteelRingCode(row.getSteelRingCode().trim());
        }
        if (StrUtil.isBlank(row.getMachineCode())) {
            row.getErrors().add(this.formatRowMessage("ui.data.alert.gsq.schedule.excel.machineRequired", row.getRowNum()));
        } else {
            row.setMachineCode(row.getMachineCode().trim());
            if (!validMachineCodeSet.contains(row.getMachineCode())) {
                row.getErrors().add(MessageFormat.format(I18nUtil.getMessage(
                        "ui.data.alert.gsq.schedule.excel.machineNotFound"), row.getRowNum(), row.getMachineCode()));
            }
        }
        this.validatePlanQty(row, row.getClass1PlanQty(), 1);
        this.validatePlanQty(row, row.getClass2PlanQty(), 2);
        this.validatePlanQty(row, row.getClass3PlanQty(), 3);
        if (StrUtil.isBlank(row.getMachineCode()) || StrUtil.isBlank(row.getSteelRingCode())) {
            return;
        }
        String businessKey = this.buildImportBusinessKey(row.getMachineCode(), row.getSteelRingCode());
        if (!importBusinessKeySet.add(businessKey)) {
            row.getErrors().add(MessageFormat.format(I18nUtil.getMessage(
                    "ui.data.alert.gsq.schedule.excel.duplicateInFile"), row.getRowNum(), businessKey));
        }
        boolean existing = CollUtil.isNotEmpty(existingResultMap.get(businessKey));
        if (existing && !updateSupport) {
            row.getErrors().add(MessageFormat.format(I18nUtil.getMessage(
                    "ui.data.alert.gsq.schedule.excel.updateNotSupported"), row.getRowNum(), businessKey));
        }
        if (!existing && !this.hasPositivePlanQty(row)) {
            row.getErrors().add(this.formatRowMessage("ui.data.alert.gsq.schedule.excel.allPlanZero", row.getRowNum()));
        }
    }

    /**
     * 校验班次计划量非负。
     *
     * @param row 导入行
     * @param planQty 计划量
     * @param shiftOrder 班次序号
     */
    private void validatePlanQty(GsqScheduleResultVo row, BigDecimal planQty, int shiftOrder) {
        BigDecimal normalizedPlanQty = BigDecimalUtils.valueOf(planQty);
        if (normalizedPlanQty.compareTo(BigDecimal.ZERO) < 0) {
            row.getErrors().add(MessageFormat.format(I18nUtil.getMessage(
                    "ui.data.alert.gsq.schedule.excel.planNegative"), row.getRowNum(), shiftOrder));
        }
    }

    /**
     * 查询有效机台编码。
     *
     * @param machineCodeSet 导入机台编码
     * @return 有效机台编码集合
     */
    private Set<String> loadValidMachineCodes(Set<String> machineCodeSet) {
        if (machineCodeSet.isEmpty()) {
            return Collections.emptySet();
        }
        LambdaQueryWrapper<GsqMachineInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(GsqMachineInfo::getMachineCode, machineCodeSet);
        return this.gsqMachineInfoMapper.selectList(wrapper).stream()
                .map(GsqMachineInfo::getMachineCode).filter(StrUtil::isNotBlank).collect(Collectors.toSet());
    }

    /**
     * 查询导入业务键对应的当前有效排程结果。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @param machineCodeSet 机台编码集合
     * @return 业务键对应的已有记录集合
     */
    private Map<String, List<GsqScheduleResult>> loadExistingResultMap(String factoryCode, Date scheduleDate,
                                                                      Set<String> machineCodeSet) {
        if (machineCodeSet.isEmpty()) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<GsqScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GsqScheduleResult::getFactoryCode, factoryCode);
        wrapper.eq(GsqScheduleResult::getScheduleDate, scheduleDate);
        wrapper.in(GsqScheduleResult::getMachineCode, machineCodeSet);
        wrapper.orderByAsc(GsqScheduleResult::getId);
        return this.gsqScheduleResultMapper.selectList(wrapper).stream()
                .collect(Collectors.groupingBy(this::buildResultBusinessKey, LinkedHashMap::new,
                        Collectors.toList()));
    }

    /**
     * 构建新增排程结果。
     *
     * @param row 导入行
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @param batchNo 批次号
     * @param insertOrder 新增行序号
     * @return 新增排程结果
     */
    private GsqScheduleResult buildInsertedResult(GsqScheduleResultVo row, String factoryCode,
                                                  Date scheduleDate, String batchNo, int insertOrder) {
        GsqScheduleResult target = new GsqScheduleResult();
        target.setFactoryCode(factoryCode);
        target.setScheduleDate(scheduleDate);
        target.setBatchNo(batchNo);
        target.setOrderNo(batchNo + "-IMPORT-" + IdUtil.fastSimpleUUID().substring(0, 8));
        target.setSteelRingCode(row.getSteelRingCode());
        target.setProSize(row.getSpecifications());
        target.setMachineCode(row.getMachineCode());
        target.setStockQty(this.toInteger(row.getStockQty()));
        target.setDataSource(IMPORT_DATA_SOURCE);
        target.setIsRelease(ApsConstant.NO_RELEASE);
        this.applyImportPlanFields(row, target);
        return target;
    }

    /**
     * 将 class1~3 计划量写入结果。
     *
     * @param row 导入行
     * @param target 目标排程结果
     */
    private void applyImportPlanFields(GsqScheduleResultVo row, GsqScheduleResult target) {
        target.setClass1PlanQty(this.toInteger(row.getClass1PlanQty()));
        target.setClass2PlanQty(this.toInteger(row.getClass2PlanQty()));
        target.setClass3PlanQty(this.toInteger(row.getClass3PlanQty()));
    }

    /**
     * 在短事务中批量保存有效导入数据。
     *
     * @param insertList 新增记录
     * @param updateList 更新记录
     * @throws ServiceException 更新或批量新增失败时抛出
     */
    private void persistImportRows(List<GsqScheduleResult> insertList, List<GsqScheduleResult> updateList) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(this.platformTransactionManager);
        Boolean success = transactionTemplate.execute(transactionStatus -> {
            if (CollUtil.isNotEmpty(updateList)) {
                Integer updatedRows = this.baseDao.updateBatch(updateList);
                if (updatedRows == null || updatedRows != updateList.size()) {
                    throw new ServiceException(I18nUtil.getMessage(
                            "ui.data.alert.gsq.schedule.excel.persistFailed"));
                }
            }
            if (CollUtil.isNotEmpty(insertList)) {
                Integer insertedRows = this.baseDao.saveBatch(insertList);
                if (insertedRows == null || insertedRows != insertList.size()) {
                    throw new ServiceException(I18nUtil.getMessage(
                            "ui.data.alert.gsq.schedule.excel.persistFailed"));
                }
            }
            return Boolean.TRUE;
        });
        if (!Boolean.TRUE.equals(success)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.gsq.schedule.excel.persistFailed"));
        }
    }

    /**
     * 保存导出日志。
     *
     * @param queryVO 导出条件
     * @param fileName 文件名称
     * @param rowCount 导出行数
     * @param beginTime 开始时间
     */
    private void saveExportLog(GsqScheduleResult queryVO, String fileName, int rowCount, Date beginTime) {
        Date endTime = DateUtils.getNowDate();
        ExportLog exportLog = new ExportLog();
        exportLog.setProcedureCode("0");
        exportLog.setFunctionCode("gsqScheduleResult");
        exportLog.setFunctionName(fileName);
        exportLog.setFileName(fileName + ExcelUtil.XLSX_FILE);
        exportLog.setExportParams(queryVO.toString());
        exportLog.setRowCount(rowCount);
        exportLog.setBeginTime(beginTime);
        exportLog.setEndTime(endTime);
        exportLog.setSpendTime(DateUtils.getDiffTime(endTime, beginTime));
        this.iExportLogService.add(exportLog);
    }

    /**
     * 读取单元格文本。
     *
     * @param row Excel 行
     * @param columnIndex 列索引
     * @param dataFormatter 单元格格式化器
     * @param formulaEvaluator 公式计算器
     * @return 去除首尾空白后的文本
     */
    private String readCellText(Row row, int columnIndex, DataFormatter dataFormatter,
                                FormulaEvaluator formulaEvaluator) {
        if (row == null) {
            return "";
        }
        Cell cell = row.getCell(columnIndex);
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return "";
        }
        return StrUtil.trim(dataFormatter.formatCellValue(cell, formulaEvaluator));
    }

    /**
     * 构建结果机台和钢丝圈业务键。
     *
     * @param result 排程结果
     * @return 业务键
     */
    private String buildResultBusinessKey(GsqScheduleResult result) {
        return this.buildImportBusinessKey(result.getMachineCode(), result.getSteelRingCode());
    }

    /**
     * 构建导入机台和钢丝圈业务键。
     *
     * @param machineCode 机台编码
     * @param steelRingCode 钢丝圈代码
     * @return 业务键
     */
    private String buildImportBusinessKey(String machineCode, String steelRingCode) {
        return StrUtil.blankToDefault(machineCode, "").trim() + "|"
                + StrUtil.blankToDefault(steelRingCode, "").trim();
    }

    /**
     * 解析当前有效批次号。
     *
     * <p>直接从排程结果表 {@code T_GSQ_SCHEDULE_RESULT} 取该工厂该日期的最大批次号
     * （与导出/导入的数据同源同工厂，保证批次号与真实数据一致）。若结果表无数据
     * （纯手工导入起点），则回退到手工批次号 "GSQMANUAL" + yyyyMMdd。</p>
     *
     * @param factoryCode  工厂编码
     * @param scheduleDate 排程日期
     * @return 当前批次号
     */
    private String resolveCurrentBatchNo(String factoryCode, Date scheduleDate) {
        String currentBatchNo = this.gsqScheduleResultMapper.getMaxBatchNo(factoryCode,
                DateUtil.format(scheduleDate, "yyyy-MM-dd"));
        if (StrUtil.isBlank(currentBatchNo)) {
            // 结果表无数据（纯手工导入起点），回退到手工批次号
            currentBatchNo = "GSQMANUAL" + DateUtil.format(scheduleDate, "yyyyMMdd");
        }
        return currentBatchNo;
    }

    /**
     * 判断导入行是否至少一个班次计划量大于 0。
     *
     * @param row 导入行
     * @return true 表示至少一个班次计划量大于 0
     */
    private boolean hasPositivePlanQty(GsqScheduleResultVo row) {
        return BigDecimalUtils.valueOf(row.getClass1PlanQty()).compareTo(BigDecimal.ZERO) > 0
                || BigDecimalUtils.valueOf(row.getClass2PlanQty()).compareTo(BigDecimal.ZERO) > 0
                || BigDecimalUtils.valueOf(row.getClass3PlanQty()).compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 将 BigDecimal 安全转换为 Integer（null 返回 null）。
     *
     * @param value 数值
     * @return 转换后的整型
     */
    private Integer toInteger(BigDecimal value) {
        return value == null ? null : value.intValue();
    }

    /**
     * 获取或创建指定行。
     *
     * @param sheet 工作表
     * @param rowIndex 行索引
     * @return Excel 行
     */
    private Row getOrCreateRow(Sheet sheet, int rowIndex) {
        Row row = sheet.getRow(rowIndex);
        return row == null ? sheet.createRow(rowIndex) : row;
    }

    /**
     * 获取或创建指定单元格。
     *
     * @param row Excel 行
     * @param columnIndex 列索引
     * @return Excel 单元格
     */
    private Cell getOrCreateCell(Row row, int columnIndex) {
        Cell cell = row.getCell(columnIndex);
        return cell == null ? row.createCell(columnIndex) : cell;
    }

    /**
     * 格式化只包含行号的多语言错误消息。
     *
     * @param messageKey 多语言 key
     * @param rowNum Excel 行号
     * @return 格式化后的错误消息
     */
    private String formatRowMessage(String messageKey, int rowNum) {
        return MessageFormat.format(I18nUtil.getMessage(messageKey), rowNum);
    }
}