package com.zlt.aps.tq.service.impl;

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
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.common.core.utils.ExcelUtils;
import com.zlt.aps.tq.api.constant.TqScheduleConstants;
import com.zlt.aps.tq.api.domain.dto.TqScheduleResultImportDTO;
import com.zlt.aps.tq.api.domain.entity.TqMachineInfo;
import com.zlt.aps.tq.api.domain.entity.TqScheduleResult;
import com.zlt.aps.tq.domain.vo.TqScheduleResultExcelParseResult;
import com.zlt.aps.tq.domain.vo.TqScheduleResultVo;
import com.zlt.aps.tq.mapper.TqMachineInfoMapper;
import com.zlt.aps.tq.mapper.TqScheduleResultMapper;
import com.zlt.aps.tq.service.ITqScheduleResultExcelService;
import com.zlt.common.utils.ImportExcelUtils;
import com.zlt.core.dao.basedao.BaseDao;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 胎圈排程结果专用模板导入导出服务实现。
 *
 * <p>导出只读取指定日期的当前有效批次，并按机台、胎圈代码补充前日 CLASS3 数据。
 * 相比胎侧实现做了以下简化：</p>
 * <ul>
 *   <li>无施工版本字段，业务键 = 机台编码 + 胎圈代码</li>
 *   <li>无顺序字段，每班只有计划/完成 2 列，不校验顺序</li>
 *   <li>无卷曲长度和成型需求查询，R 列放空、Q 列由实体 cxClass1~4Plan 求和</li>
 *   <li>使用 RedissonClient 直接加锁，不依赖 ExecutionGuard</li>
 *   <li>批次号简化为 "TQMANUAL" + yyyyMMdd</li>
 *   <li>模板 E1 占位符 {.specifications} 在导出前预处理为 {specifications}</li>
 * </ul>
 *
 * @author APS
 */
@Slf4j
@Service
public class TqScheduleResultExcelServiceImpl implements ITqScheduleResultExcelService {

    /** 模板资源路径。 */
    private static final String TEMPLATE_RESOURCE = "excelModel/tqScheduleResult.xlsx";

    /** 固定工作表名称。 */
    private static final String SHEET_NAME = "胎圈 BD Vòng vành";

    /** 标题日期格式。 */
    private static final String TITLE_DATE_FORMAT = "yyyy年MM月dd日";

    /** 模板标题日期正则。 */
    private static final Pattern TITLE_DATE_PATTERN = Pattern.compile("^(\\d{4}年\\d{2}月\\d{2}日).*$");

    /** 隐藏元数据行索引。 */
    private static final int HEADER_ROW_INDEX = 0;

    /** 标题行索引。 */
    private static final int TITLE_ROW_INDEX = 1;

    /** 明细起始行索引。 */
    private static final int DATA_START_ROW_INDEX = 4;

    /** 用户可见列数量 A:T。 */
    private static final int VISIBLE_COLUMN_COUNT = 20;

    /** 模板总列数量 A:T。 */
    private static final int TOTAL_COLUMN_COUNT = 20;

    /** 模板处理班次数。 */
    private static final int IMPORT_SHIFT_COUNT = 3;

    @Resource
    private TqScheduleResultMapper tqScheduleResultMapper;

    @Resource
    private TqMachineInfoMapper tqMachineInfoMapper;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private PlatformTransactionManager platformTransactionManager;

    @Resource
    private BaseDao baseDao;

    @Resource
    private IExportLogService iExportLogService;

    @Resource
    private IImportLogService iImportLogService;

    @Resource
    private IImportErrorLogService iImportErrorLogService;

    /**
     * 按专用模板导出胎圈排程结果。
     *
     * @param queryVO  查询条件，必须包含工厂和排程日期
     * @param fileName 导出文件名称
     * @return Excel 文件字节
     * @throws ServiceException 条件不完整、模板缺失或工作簿生成失败时抛出
     */
    @Override
    public byte[] exportDataScheduleResult(TqScheduleResult queryVO, String fileName) {
        this.validateExportQuery(queryVO);
        Date beginTime = DateUtils.getNowDate();
        Date scheduleDate = DateUtil.beginOfDay(queryVO.getScheduleDate());
        String currentBatchNo = this.resolveCurrentBatchNo(queryVO.getFactoryCode(), scheduleDate);
        List<TqScheduleResult> resultList = this.listExportResults(queryVO, currentBatchNo);
        List<Map<String, Object>> dataList = this.buildExportDataList(resultList, queryVO);
        List<List<Map<String, Object>>> excelDataList = new ArrayList<>();
        if (CollUtil.isNotEmpty(dataList)) {
            excelDataList.add(dataList);
        }

        byte[] templateBytes = this.loadNormalizedTemplate();
        Map<String, Object> tableMap = this.getTitleMap(queryVO);
        this.setExportTitleFieldName(tableMap);
        byte[] resultBytes;
        try {
            resultBytes = ExcelUtils.writeMultiList(new ByteArrayInputStream(templateBytes), 0, tableMap, excelDataList);
        } catch (ServiceException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.error("生成胎圈排程结果模板失败", exception);
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tq.schedule.excel.generateFailed"));
        }
        resultBytes = this.finishExportWorkbook(resultBytes, dataList.isEmpty());
        this.saveExportLog(queryVO, fileName, resultList.size(), beginTime);
        return resultBytes;
    }

    /**
     * 按专用模板导入胎圈排程结果。
     *
     * @param importDTO     导入文件和业务条件
     * @param updateSupport 是否允许覆盖更新
     * @return 导入结果和行级错误
     * @throws Exception 导入日志或文件处理失败时抛出
     */
    @Override
    public AjaxResult importDataScheduleResult(TqScheduleResultImportDTO importDTO,
                                               boolean updateSupport) throws Exception {
        ImportContext importContext = this.validateImportContext(importDTO);
        Date beginTime = DateUtils.getNowDate();
        ImportLog importLog = ImportExcelUtils.getImportLogAndUploadFile(importContext.getFileBytes(),
                importContext.getImportFilePath(), importContext.getProcedureCode(), importContext.getFunctionName(),
                importContext.getOriFileName(), 1);
        importLog = this.iImportLogService.add(importLog);
        AjaxResult ajaxResult;
        int rowCount = 0;
        try {
            TqScheduleResultExcelParseResult parseResult = this.parseWorkbook(importContext.getFileBytes(),
                    importDTO.getScheduleResult());
            rowCount = parseResult.getRowList().size();
            ajaxResult = this.doImport(parseResult, importDTO.getScheduleResult(), updateSupport, importLog.getId());
        } catch (ServiceException exception) {
            log.warn("胎圈排程结果模板导入校验失败，原因={}", exception.getMessage());
            ajaxResult = AjaxResult.error(exception.getMessage());
        } catch (RuntimeException exception) {
            log.error("胎圈排程结果模板导入失败", exception);
            ajaxResult = AjaxResult.error(I18nUtil.getMessage("ui.data.alert.tq.schedule.excel.importFailed"));
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

    @Override
    public byte[] downloadTemplate(TqScheduleResult queryVO) {
        // 校验排程日期（用于填充模板标题日期）
        if (queryVO == null || queryVO.getScheduleDate() == null) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tq.schedule.excel.dateRequired"));
        }
        byte[] templateBytes = this.loadNormalizedTemplate();
        Map<String, Object> tableMap = this.getTitleMap(queryVO);
        this.setExportTitleFieldName(tableMap);
        byte[] resultBytes;
        try {
            resultBytes = ExcelUtils.writeMultiList(new ByteArrayInputStream(templateBytes), 0, tableMap,
                    Collections.emptyList());
        } catch (ServiceException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.error("生成胎圈排程结果空白模板失败", exception);
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tq.schedule.excel.generateFailed"));
        }
        // 空模板：清除数据行
        return this.finishExportWorkbook(resultBytes, true);
    }

    // ==================== 导出相关方法 ====================

    /**
     * 校验导出条件。
     *
     * @param queryVO 导出条件
     * @throws ServiceException 工厂或排程日期为空时抛出
     */
    private void validateExportQuery(TqScheduleResult queryVO) {
        if (queryVO == null || StrUtil.isBlank(queryVO.getFactoryCode())) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tq.schedule.excel.factoryRequired"));
        }
        if (queryVO.getScheduleDate() == null) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tq.schedule.excel.dateRequired"));
        }
    }

    /**
     * 解析当前有效批次号。
     *
     * <p>直接从排程结果表 {@code T_TQ_SCHEDULE_RESULT} 取该工厂该日期的最大批次号
     * （与导出/导入的数据同源同工厂，保证批次号与真实数据一致），
     * 不再依赖自动排程记录表 {@code T_TQ_AUTO_SCHEDULE_RECORD}（全局每日期流水，
     * 会因多工厂/多次排程而超前于真实数据）。若结果表无数据（纯手工导入起点），
     * 则回退到手工批次号 "TQMANUAL" + yyyyMMdd。</p>
     *
     * @param factoryCode   工厂编码
     * @param scheduleDate  排程日期
     * @return 当前批次号
     */
    private String resolveCurrentBatchNo(String factoryCode, Date scheduleDate) {
        // 优先取结果表当前批次号（与真实数据强一致）
        String currentBatchNo = this.tqScheduleResultMapper.getMaxBatchNo(factoryCode,
                DateUtil.format(scheduleDate, "yyyy-MM-dd"));
        if (StrUtil.isBlank(currentBatchNo)) {
            // 结果表无数据（纯手工导入起点），回退到手工批次号
            currentBatchNo = "TQMANUAL" + DateUtil.format(scheduleDate, "yyyyMMdd");
        }
        return currentBatchNo;
    }

    /**
     * 查询当前有效批次导出明细。
     *
     * @param queryVO        查询条件
     * @param currentBatchNo 当前有效批次
     * @return 排程结果
     */
    private List<TqScheduleResult> listExportResults(TqScheduleResult queryVO, String currentBatchNo) {
        LambdaQueryWrapper<TqScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StrUtil.isNotBlank(queryVO.getFactoryCode()), TqScheduleResult::getFactoryCode,
                queryVO.getFactoryCode());
        wrapper.eq(TqScheduleResult::getScheduleDate, DateUtil.beginOfDay(queryVO.getScheduleDate()));
        wrapper.eq(TqScheduleResult::getBatchNo, currentBatchNo);
        wrapper.eq(StrUtil.isNotBlank(queryVO.getMachineCode()), TqScheduleResult::getMachineCode,
                queryVO.getMachineCode());
        wrapper.like(StrUtil.isNotBlank(queryVO.getBeadCode()), TqScheduleResult::getBeadCode,
                queryVO.getBeadCode());
        wrapper.orderByAsc(TqScheduleResult::getMachineCode, TqScheduleResult::getClass1Sequence,
                TqScheduleResult::getId);
        return this.emptyIfNull(this.tqScheduleResultMapper.selectList(wrapper));
    }

    /**
     * 构造导出明细。
     *
     * @param resultList 当前批次结果
     * @param queryVO    导出条件
     * @return 模板列表数据
     */
    private List<Map<String, Object>> buildExportDataList(List<TqScheduleResult> resultList,
                                                          TqScheduleResult queryVO) {
        if (CollUtil.isEmpty(resultList)) {
            return Collections.emptyList();
        }
        Map<String, TqScheduleResult> previousResultMap = this.buildPreviousResultMap(queryVO);
        return resultList.stream().map(result -> {
            Map<String, Object> rowMap = new LinkedHashMap<>();
            String resultKey = this.buildResultKey(result);
            TqScheduleResult previousResult = previousResultMap.get(resultKey);
            // A 列 胎圈代码
            rowMap.put("beadCode", result.getBeadCode());
            // B 列 个数（暂不填充）
            rowMap.put("count", null);
            // C 列 成型余量（暂放空）
            rowMap.put("cxRemainQty", null);
            // D 列 物料描述（暂放空）
            rowMap.put("materialDesc", null);
            // E 列 规格（对应实体 proSize）
            rowMap.put("specifications", result.getProSize());
            // F 列 整条胶料组合编码（暂放空）
            rowMap.put("wholeGlueCode", null);
            // G 列 机台编码
            rowMap.put("machineCode", result.getMachineCode());
            // H 列 库存量
            rowMap.put("stockQty", this.blankIfZero(BigDecimalUtils.valueOf(result.getStockQty())));
            // I:J 列 前日（前一日 3 班）计划/完成
            rowMap.put("lastDayPlanQty", this.blankIfZero(
                    BigDecimalUtils.valueOf(previousResult == null ? null : previousResult.getClass3PlanQty())));
            rowMap.put("lastDayFinishQty", this.blankIfZero(
                    BigDecimalUtils.valueOf(previousResult == null ? null : previousResult.getClass3FinishQty())));
            // K:L 列 1 班计划/完成
            rowMap.put("class1PlanQty", this.blankIfZero(BigDecimalUtils.valueOf(result.getClass1PlanQty())));
            rowMap.put("class1FinishQty", this.blankIfZero(BigDecimalUtils.valueOf(result.getClass1FinishQty())));
            // M:N 列 2 班计划/完成
            rowMap.put("class2PlanQty", this.blankIfZero(BigDecimalUtils.valueOf(result.getClass2PlanQty())));
            rowMap.put("class2FinishQty", this.blankIfZero(BigDecimalUtils.valueOf(result.getClass2FinishQty())));
            // O:P 列 3 班计划/完成
            rowMap.put("class3PlanQty", this.blankIfZero(BigDecimalUtils.valueOf(result.getClass3PlanQty())));
            rowMap.put("class3FinishQty", this.blankIfZero(BigDecimalUtils.valueOf(result.getClass3FinishQty())));
            // Q 列 成型产量 = cxClass1~4Plan 之和
            BigDecimal cxPlanQty = BigDecimalUtils.add(result.getCxClass1Plan(), result.getCxClass2Plan(),
                    result.getCxClass3Plan(), result.getCxClass4Plan());
            rowMap.put("cxPlanQty", this.blankIfZero(cxPlanQty));
            // R 列 标准要求/卷曲长度（暂放空）
            rowMap.put("curlRollLength", null);
            // S 列 成型机台编码（暂放空）
            rowMap.put("cxMachineCode", null);
            // T 列 类型（暂放空）
            rowMap.put("type", "");
            return rowMap;
        }).collect(Collectors.toList());
    }

    /**
     * 查询前一日当前有效批次的 CLASS3 数据。
     *
     * <p>胎圈业务键为机台编码 + 胎圈代码（无施工版本）。</p>
     *
     * @param queryVO 当前导出条件
     * @return 机台编码和胎圈代码对应的前日结果
     */
    private Map<String, TqScheduleResult> buildPreviousResultMap(TqScheduleResult queryVO) {
        Date previousDate = DateUtil.beginOfDay(DateUtil.offsetDay(queryVO.getScheduleDate(), -1));
        String previousBatchNo = this.resolveCurrentBatchNo(queryVO.getFactoryCode(), previousDate);
        LambdaQueryWrapper<TqScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StrUtil.isNotBlank(queryVO.getFactoryCode()), TqScheduleResult::getFactoryCode,
                queryVO.getFactoryCode());
        wrapper.eq(TqScheduleResult::getScheduleDate, previousDate);
        wrapper.eq(TqScheduleResult::getBatchNo, previousBatchNo);
        wrapper.orderByDesc(TqScheduleResult::getId);
        return this.emptyIfNull(this.tqScheduleResultMapper.selectList(wrapper)).stream()
                .filter(item -> StrUtil.isNotBlank(item.getMachineCode())
                        && StrUtil.isNotBlank(item.getBeadCode()))
                .collect(Collectors.toMap(this::buildResultKey, Function.identity(),
                        (first, ignored) -> first, LinkedHashMap::new));
    }

    /**
     * 构造机台编码和胎圈代码业务键。
     *
     * @param result 排程结果
     * @return 业务键
     */
    private String buildResultKey(TqScheduleResult result) {
        return StrUtil.blankToDefault(result.getMachineCode(), "").trim() + "|"
                + StrUtil.blankToDefault(result.getBeadCode(), "").trim();
    }

    /**
     * 构造模板动态标题。
     *
     * @param queryVO 导出条件
     * @return 模板占位符和值
     */
    private Map<String, Object> getTitleMap(TqScheduleResult queryVO) {
        Map<String, Object> tableMap = new HashMap<>();
        String currentDate = DateUtil.format(queryVO.getScheduleDate(), TITLE_DATE_FORMAT);
        tableMap.put("planDate", currentDate);
        return tableMap;
    }

    /**
     * 将 Excel 字段国际化名称写入模板第 1 行。
     *
     * @param tableMap 模板占位符映射
     */
    private void setExportTitleFieldName(Map<String, Object> tableMap) {
        for (Field field : TqScheduleResultVo.class.getDeclaredFields()) {
            Excel attr = field.getAnnotation(Excel.class);
            if (attr == null || (attr.type() != Excel.Type.ALL && attr.type() != Excel.Type.IMPORT)) {
                continue;
            }
            tableMap.put(field.getName(), this.resolveExcelHeaderName(attr));
        }
    }

    /**
     * 解析 Excel 注解国际化表头。
     *
     * @param attr Excel 注解
     * @return 当前语言表头
     */
    private String resolveExcelHeaderName(Excel attr) {
        String attrName = StrUtil.blankToDefault(attr.importName(), attr.name());
        if (StrUtil.isBlank(attrName)) {
            return "";
        }
        return I18nUtil.getMessage(attrName.replaceAll("\\{", "").replaceAll("}", ""));
    }

    /**
     * 加载模板资源并规范化 E 列占位符。
     *
     * @return 规范化后的模板字节
     * @throws ServiceException 模板缺失时抛出
     */
    private byte[] loadNormalizedTemplate() {
        try (InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(TEMPLATE_RESOURCE)) {
            if (inputStream == null) {
                throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tq.schedule.excel.templateMissing"));
            }
            return this.normalizeTemplate(inputStream);
        } catch (IOException exception) {
            log.error("读取胎圈排程结果模板失败", exception);
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tq.schedule.excel.templateMissing"));
        }
    }

    /**
     * 规范化模板：将第 1 行 E 列（索引 4）的错误占位符 {.specifications} 修正为 {specifications}。
     *
     * @param inputStream 模板输入流
     * @return 规范化后的模板字节
     * @throws ServiceException 工作簿处理失败时抛出
     */
    private byte[] normalizeTemplate(InputStream inputStream) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(HEADER_ROW_INDEX);
            if (headerRow != null) {
                Cell specCell = headerRow.getCell(4);
                if (specCell != null && CellType.STRING == specCell.getCellType()
                        && "{.specifications}".equals(specCell.getStringCellValue())) {
                    specCell.setCellValue("{specifications}");
                }
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            log.error("规范化胎圈排程结果模板失败", exception);
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tq.schedule.excel.generateFailed"));
        }
    }

    /**
     * 完成导出工作簿的隐藏元数据和空模板清理。
     *
     * @param sourceBytes 模板填充后字节
     * @param emptyData   是否为空模板
     * @return 最终工作簿字节
     * @throws ServiceException 工作簿处理失败时抛出
     */
    private byte[] finishExportWorkbook(byte[] sourceBytes, boolean emptyData) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(sourceBytes);
             XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.getSheetAt(0);
            workbook.setSheetName(0, SHEET_NAME);
            this.getOrCreateRow(sheet, HEADER_ROW_INDEX).setZeroHeight(true);
            for (int columnIndex = VISIBLE_COLUMN_COUNT; columnIndex < TOTAL_COLUMN_COUNT; columnIndex++) {
                sheet.setColumnHidden(columnIndex, true);
            }
            this.normalizeEmptyStringCells(sheet);
            if (emptyData) {
                Row dataRow = this.getOrCreateRow(sheet, DATA_START_ROW_INDEX);
                for (int columnIndex = 0; columnIndex < TOTAL_COLUMN_COUNT; columnIndex++) {
                    this.getOrCreateCell(dataRow, columnIndex).setBlank();
                }
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            log.error("处理胎圈排程结果导出工作簿失败", exception);
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tq.schedule.excel.generateFailed"));
        }
    }

    /**
     * 将空字符串单元格标准化为空白单元格。
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
     * 保存导出日志。
     *
     * @param queryVO   导出条件
     * @param fileName  文件名称
     * @param rowCount  行数
     * @param beginTime 开始时间
     */
    private void saveExportLog(TqScheduleResult queryVO, String fileName, int rowCount, Date beginTime) {
        Date endTime = DateUtils.getNowDate();
        ExportLog exportLog = new ExportLog();
        exportLog.setProcedureCode("0");
        exportLog.setFunctionCode("tqScheduleResult");
        exportLog.setFunctionName(fileName);
        exportLog.setFileName(fileName + ExcelUtil.XLSX_FILE);
        exportLog.setExportParams(queryVO.toString());
        exportLog.setRowCount(rowCount);
        exportLog.setBeginTime(beginTime);
        exportLog.setEndTime(endTime);
        exportLog.setSpendTime(DateUtils.getDiffTime(endTime, beginTime));
        this.iExportLogService.add(exportLog);
    }

    // ==================== 导入相关方法 ====================

    /**
     * 校验导入上下文。
     *
     * @param importDTO 导入请求
     * @return 文件导入上下文
     * @throws ServiceException 文件、类型、工厂或日期无效时抛出
     */
    private ImportContext validateImportContext(TqScheduleResultImportDTO importDTO) {
        if (importDTO == null || importDTO.getImportContext() == null
                || importDTO.getImportContext().getFileBytes() == null
                || importDTO.getImportContext().getFileBytes().length == 0) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tq.schedule.excel.fileRequired"));
        }
        String originalFileName = StrUtil.trim(importDTO.getImportContext().getOriFileName());
        if (StrUtil.isBlank(originalFileName)
                || !originalFileName.toLowerCase(Locale.ROOT).endsWith(ExcelUtil.XLSX_FILE)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tq.schedule.excel.fileTypeInvalid"));
        }
        if (importDTO.getScheduleResult() == null
                || StrUtil.isBlank(importDTO.getScheduleResult().getFactoryCode())) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tq.schedule.excel.factoryRequired"));
        }
        if (importDTO.getScheduleResult().getScheduleDate() == null) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tq.schedule.excel.dateRequired"));
        }
        return importDTO.getImportContext();
    }

    /**
     * 解析导入工作簿。
     *
     * @param fileBytes Excel 文件字节
     * @param condition 导入条件
     * @return 标题日期和有效明细
     * @throws ServiceException 模板、工作表、标题或日期无效时抛出
     */
    private TqScheduleResultExcelParseResult parseWorkbook(byte[] fileBytes, TqScheduleResult condition) {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(fileBytes))) {
            Sheet sheet = workbook.getSheet(SHEET_NAME);
            if (sheet == null || sheet.getRow(HEADER_ROW_INDEX) == null) {
                throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tq.schedule.excel.templateInvalid"));
            }
            DataFormatter dataFormatter = new DataFormatter();
            FormulaEvaluator formulaEvaluator = workbook.getCreationHelper().createFormulaEvaluator();
            Date scheduleDate = this.parseScheduleDate(this.readCellText(
                    sheet.getRow(TITLE_ROW_INDEX), 0, dataFormatter, formulaEvaluator));
            if (!DateUtil.isSameDay(scheduleDate, condition.getScheduleDate())) {
                throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tq.schedule.excel.dateMismatch"));
            }
            List<TqScheduleResultVo> rowList = this.parseVoList(sheet, dataFormatter, formulaEvaluator);
            if (rowList.isEmpty()) {
                throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tq.schedule.excel.noData"));
            }
            TqScheduleResultExcelParseResult parseResult = new TqScheduleResultExcelParseResult();
            parseResult.setScheduleDate(DateUtil.beginOfDay(scheduleDate));
            parseResult.setRowList(rowList);
            return parseResult;
        } catch (ServiceException exception) {
            throw exception;
        } catch (Exception exception) {
            log.warn("解析胎圈排程结果模板失败", exception);
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tq.schedule.excel.templateInvalid"));
        }
    }

    /**
     * 解析标题中的排程日期。
     *
     * @param title 模板标题
     * @return 排程日期
     * @throws ServiceException 标题日期格式无效时抛出
     */
    private Date parseScheduleDate(String title) {
        Matcher matcher = TITLE_DATE_PATTERN.matcher(StrUtil.blankToDefault(title, ""));
        if (!matcher.matches()) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tq.schedule.excel.titleInvalid"));
        }
        try {
            return DateUtil.parse(matcher.group(1), TITLE_DATE_FORMAT);
        } catch (RuntimeException exception) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tq.schedule.excel.titleInvalid"));
        }
    }

    /**
     * 按隐藏国际化表头解析明细。
     *
     * @param sheet            模板工作表
     * @param dataFormatter    单元格格式化器
     * @param formulaEvaluator 公式计算器
     * @return 明细行
     */
    private List<TqScheduleResultVo> parseVoList(Sheet sheet, DataFormatter dataFormatter,
                                                 FormulaEvaluator formulaEvaluator) {
        Map<Integer, Field> fieldMap = this.resolveImportFieldMap(
                sheet.getRow(HEADER_ROW_INDEX), dataFormatter, formulaEvaluator);
        List<TqScheduleResultVo> rowList = new ArrayList<>();
        for (int rowIndex = DATA_START_ROW_INDEX; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row excelRow = sheet.getRow(rowIndex);
            if (excelRow == null) {
                continue;
            }
            TqScheduleResultVo row = new TqScheduleResultVo();
            row.setRowNum(rowIndex + 1);
            int emptyCount = 0;
            for (Map.Entry<Integer, Field> entry : fieldMap.entrySet()) {
                String text = this.readCellText(excelRow, entry.getKey(), dataFormatter, formulaEvaluator);
                if (StrUtil.isBlank(text)) {
                    emptyCount++;
                    continue;
                }
                Object convertedValue = this.convertCellValue(text, entry.getValue(), row);
                if (convertedValue == null) {
                    continue;
                }
                try {
                    entry.getValue().set(row, convertedValue);
                } catch (IllegalAccessException exception) {
                    throw new RuntimeException(exception);
                }
            }
            if (emptyCount == fieldMap.size()) {
                break;
            }
            rowList.add(row);
        }
        return rowList;
    }

    /**
     * 解析并校验隐藏元数据表头。
     *
     * @param headerRow        隐藏表头行
     * @param dataFormatter    单元格格式化器
     * @param formulaEvaluator 公式计算器
     * @return 列索引对应字段
     * @throws ServiceException 缺少字段或表头被修改时抛出
     */
    private Map<Integer, Field> resolveImportFieldMap(Row headerRow, DataFormatter dataFormatter,
                                                      FormulaEvaluator formulaEvaluator) {
        Map<String, Integer> headerCellMap = new HashMap<>();
        for (int columnIndex = 0; columnIndex < headerRow.getLastCellNum(); columnIndex++) {
            String headerText = this.readCellText(headerRow, columnIndex, dataFormatter, formulaEvaluator);
            if (StrUtil.isNotBlank(headerText)) {
                headerCellMap.put(headerText.trim(), columnIndex);
            }
        }
        Map<Integer, Field> fieldMap = new LinkedHashMap<>();
        for (Field field : TqScheduleResultVo.class.getDeclaredFields()) {
            Excel attr = field.getAnnotation(Excel.class);
            if (attr == null || (attr.type() != Excel.Type.ALL && attr.type() != Excel.Type.IMPORT)) {
                continue;
            }
            Integer columnIndex = headerCellMap.get(this.resolveExcelHeaderName(attr));
            if (columnIndex == null) {
                throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tq.schedule.excel.hiddenHeaderInvalid"));
            }
            field.setAccessible(true);
            fieldMap.put(columnIndex, field);
        }
        if (fieldMap.size() != TOTAL_COLUMN_COUNT) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tq.schedule.excel.hiddenHeaderInvalid"));
        }
        return fieldMap;
    }

    /**
     * 将单元格文本转换为字段类型，并把格式错误记录为行级错误。
     *
     * @param text  单元格文本
     * @param field 目标字段
     * @param row   导入行
     * @return 转换值；格式错误时返回 null
     */
    private Object convertCellValue(String text, Field field, TqScheduleResultVo row) {
        Class<?> fieldType = field.getType();
        String normalized = text.replace(",", "");
        if (fieldType == String.class) {
            return normalized.endsWith(".0") ? normalized.substring(0, normalized.length() - 2) : text;
        }
        try {
            BigDecimal decimal = new BigDecimal(normalized);
            if (fieldType == BigDecimal.class) {
                return decimal;
            }
            if (fieldType == Integer.class || fieldType == Integer.TYPE) {
                return decimal.intValueExact();
            }
            if (fieldType == Long.class || fieldType == Long.TYPE) {
                return decimal.longValueExact();
            }
        } catch (RuntimeException exception) {
            row.getErrors().add(MessageFormat.format(I18nUtil.getMessage(
                    "ui.data.alert.tq.schedule.excel.invalidNumber"), row.getRowNum(),
                    this.resolveExcelHeaderName(field.getAnnotation(Excel.class))));
            return null;
        }
        return text;
    }

    /**
     * 在工厂日期级 Redisson 执行锁内校验并持久化导入。
     *
     * @param parseResult  解析结果
     * @param condition    导入条件
     * @param updateSupport 是否允许更新
     * @param importLogId  导入日志 ID
     * @return 导入结果
     */
    private AjaxResult doImport(TqScheduleResultExcelParseResult parseResult, TqScheduleResult condition,
                                boolean updateSupport, Long importLogId) {
        String factoryCode = condition.getFactoryCode().trim();
        Date scheduleDate = parseResult.getScheduleDate();
        String lockKey = TqScheduleConstants.IMPORT_LOCK_KEY_PREFIX + factoryCode + ":"
                + DateUtil.formatDate(scheduleDate);
        RLock lock = this.redissonClient.getLock(lockKey);
        boolean locked = false;
        try {
            locked = lock.tryLock(5, TimeUnit.SECONDS);
            if (!locked) {
                return this.buildHardBlockResult(I18nUtil.getMessage(
                        "ui.data.alert.tq.schedule.excel.concurrentTask"), importLogId, 0);
            }
            return this.doImportWithLock(parseResult.getRowList(), factoryCode, scheduleDate,
                    updateSupport, importLogId);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tq.schedule.excel.lockFailed"));
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 在已取得执行锁后完成导入校验和短事务写入。
     *
     * @param rowList       导入行
     * @param factoryCode   工厂编码
     * @param scheduleDate  排程日期
     * @param updateSupport 是否允许更新
     * @param importLogId   导入日志 ID
     * @return 导入结果
     */
    private AjaxResult doImportWithLock(List<TqScheduleResultVo> rowList, String factoryCode,
                                        Date scheduleDate, boolean updateSupport, Long importLogId) {
        String batchNo = this.resolveCurrentBatchNo(factoryCode, scheduleDate);
        List<TqScheduleResult> currentResultList = this.loadCurrentBatchResults(factoryCode, scheduleDate, batchNo);
        Map<Long, TqScheduleResult> allRequestedResultMap = this.loadRequestedResults(rowList);
        AjaxResult hardBlockResult = this.checkHardBlocks(rowList, currentResultList,
                allRequestedResultMap, scheduleDate, batchNo, importLogId);
        if (hardBlockResult != null) {
            return hardBlockResult;
        }

        Set<String> machineCodeSet = rowList.stream().map(TqScheduleResultVo::getMachineCode)
                .filter(StrUtil::isNotBlank).map(String::trim).collect(Collectors.toSet());
        Set<String> validMachineCodeSet = this.loadValidMachineCodes(machineCodeSet);
        Set<Long> importResultIdSet = new HashSet<>();
        List<TqScheduleResultVo> validRowList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogList = new ArrayList<>();
        for (TqScheduleResultVo row : rowList) {
            TqScheduleResult existing = row.getResultId() == null ? null
                    : allRequestedResultMap.get(row.getResultId());
            this.validateImportRow(row, existing, validMachineCodeSet, updateSupport, importResultIdSet);
            if (CollUtil.isNotEmpty(row.getErrors())) {
                importErrorLogList.add(this.buildRowError(importLogId, row));
            } else {
                validRowList.add(row);
            }
        }
        if (validRowList.isEmpty()) {
            return this.buildImportResult(0, importErrorLogList);
        }

        Map<Integer, TqScheduleResult> proposalMap = new LinkedHashMap<>();
        validRowList.forEach(row -> proposalMap.put(row.getRowNum(),
                this.buildProposal(row, allRequestedResultMap.get(row.getResultId()),
                        factoryCode, scheduleDate, batchNo)));
        int successNum = this.persistImportRows(validRowList, proposalMap);
        return this.buildImportResult(successNum, importErrorLogList);
    }

    /**
     * 校验会阻断整批导入的一致性条件。
     *
     * @param rowList             导入行
     * @param currentResultList   当前批次结果
     * @param requestedResultMap  导入主键对应结果
     * @param scheduleDate        排程日期
     * @param batchNo             当前批次
     * @param importLogId         导入日志 ID
     * @return 阻断结果；不存在阻断时返回 null
     */
    private AjaxResult checkHardBlocks(List<TqScheduleResultVo> rowList,
                                       List<TqScheduleResult> currentResultList,
                                       Map<Long, TqScheduleResult> requestedResultMap,
                                       Date scheduleDate, String batchNo, Long importLogId) {
        // 发布状态校验：当前批次中存在已发布记录则阻断
        TqScheduleResult released = currentResultList.stream().filter(item ->
                !TqScheduleConstants.RELEASE_STATUS_NOT_PUBLISHED.equals(item.getReleaseStatus()))
                .findFirst().orElse(null);
        if (released != null) {
            return this.buildHardBlockResult(MessageFormat.format(I18nUtil.getMessage(
                    "ui.data.alert.tq.schedule.excel.releaseBlocked"), released.getId()), importLogId, 0);
        }
        for (TqScheduleResultVo row : rowList) {
            if (row.getResultId() == null) {
                continue;
            }
            TqScheduleResult current = requestedResultMap.get(row.getResultId());
            // 批次和日期一致性校验（胎圈按工厂+日期+批次定位当前批次，导入行主键需落在该批次内）
            if (current == null || !DateUtil.isSameDay(scheduleDate, current.getScheduleDate())
                    || !Objects.equals(batchNo, current.getBatchNo())) {
                return this.buildHardBlockResult(MessageFormat.format(I18nUtil.getMessage(
                        "ui.data.alert.tq.schedule.excel.identityChanged"), row.getRowNum()),
                        importLogId, row.getRowNum());
            }
            // 发布状态校验
            if (!TqScheduleConstants.RELEASE_STATUS_NOT_PUBLISHED.equals(current.getReleaseStatus())) {
                return this.buildHardBlockResult(MessageFormat.format(I18nUtil.getMessage(
                        "ui.data.alert.tq.schedule.excel.releaseBlocked"), current.getId()),
                        importLogId, row.getRowNum());
            }
            // 业务键校验：机台编码 + 胎圈代码必须一致
            boolean identityChanged = !Objects.equals(StrUtil.trim(row.getMachineCode()),
                    StrUtil.trim(current.getMachineCode()))
                    || !Objects.equals(StrUtil.trim(row.getBeadCode()),
                    StrUtil.trim(current.getBeadCode()));
            if (identityChanged) {
                return this.buildHardBlockResult(MessageFormat.format(I18nUtil.getMessage(
                        "ui.data.alert.tq.schedule.excel.identityChanged"), row.getRowNum()),
                        importLogId, row.getRowNum());
            }
            // 版本校验
            long expectedVersion = row.getTaskVersion() == null ? -1L : row.getTaskVersion();
            long currentVersion = current.getTaskVersion() == null ? 0L : current.getTaskVersion();
            if (expectedVersion != currentVersion) {
                return this.buildHardBlockResult(MessageFormat.format(I18nUtil.getMessage(
                        "ui.data.alert.tq.schedule.excel.versionChanged"), row.getRowNum()),
                        importLogId, row.getRowNum());
            }
        }
        return null;
    }

    /**
     * 校验普通行错误（无顺序校验）。
     *
     * @param row                   导入行
     * @param existing              更新目标，新增时为空
     * @param validMachineCodeSet   有效机台编码
     * @param updateSupport         是否允许更新
     * @param importResultIdSet     文件内结果主键集合
     */
    private void validateImportRow(TqScheduleResultVo row, TqScheduleResult existing,
                                   Set<String> validMachineCodeSet, boolean updateSupport,
                                   Set<Long> importResultIdSet) {
        // 胎圈代码必填
        if (StrUtil.isBlank(row.getBeadCode())) {
            row.getErrors().add(this.formatRowMessage(
                    "ui.data.alert.tq.schedule.excel.beadRequired", row.getRowNum()));
        } else {
            row.setBeadCode(row.getBeadCode().trim());
        }
        // 机台编码必填 + 存在性校验
        if (StrUtil.isBlank(row.getMachineCode())) {
            row.getErrors().add(this.formatRowMessage(
                    "ui.data.alert.tq.schedule.excel.machineRequired", row.getRowNum()));
        } else {
            row.setMachineCode(row.getMachineCode().trim());
            if (!validMachineCodeSet.contains(row.getMachineCode())) {
                row.getErrors().add(MessageFormat.format(I18nUtil.getMessage(
                        "ui.data.alert.tq.schedule.excel.machineNotFound"),
                        row.getRowNum(), row.getMachineCode()));
            }
        }
        // 每班计划量非负 + 计划量 >= 已有完成量
        for (int shiftOrder = 1; shiftOrder <= IMPORT_SHIFT_COUNT; shiftOrder++) {
            BigDecimal planQty = this.getBigDecimalField(row,
                    TqScheduleConstants.SHIFT_PLAN_QTY_FIELD_TEMPLATE, shiftOrder);
            BigDecimal finishQty = existing == null ? BigDecimal.ZERO : this.getBigDecimalField(existing,
                    TqScheduleConstants.SHIFT_FINISH_QTY_FIELD_TEMPLATE, shiftOrder);
            this.validatePlanQty(row, planQty, finishQty, shiftOrder);
        }
        // 主键存在时校验更新许可，新增时至少一班计划量为正
        if (row.getResultId() != null) {
            if (!importResultIdSet.add(row.getResultId())) {
                row.getErrors().add(MessageFormat.format(I18nUtil.getMessage(
                        "ui.data.alert.tq.schedule.excel.duplicateResultId"),
                        row.getRowNum(), row.getResultId()));
            }
            if (!updateSupport) {
                row.getErrors().add(this.formatRowMessage(
                        "ui.data.alert.tq.schedule.excel.updateNotSupported", row.getRowNum()));
            }
        } else if (!this.hasPositivePlanQty(row)) {
            row.getErrors().add(this.formatRowMessage(
                    "ui.data.alert.tq.schedule.excel.allPlanZero", row.getRowNum()));
        }
    }

    /**
     * 校验每班计划量非负且不小于已有完成量。
     *
     * @param row        导入行
     * @param planQty    计划量
     * @param finishQty  已有完成量
     * @param shiftOrder 班次序号
     */
    private void validatePlanQty(TqScheduleResultVo row, BigDecimal planQty,
                                 BigDecimal finishQty, int shiftOrder) {
        BigDecimal normalizedPlanQty = BigDecimalUtils.valueOf(planQty);
        if (normalizedPlanQty.compareTo(BigDecimal.ZERO) < 0) {
            row.getErrors().add(MessageFormat.format(I18nUtil.getMessage(
                    "ui.data.alert.tq.schedule.excel.planNegative"), row.getRowNum(), shiftOrder));
        }
        if (normalizedPlanQty.compareTo(BigDecimalUtils.valueOf(finishQty)) < 0) {
            row.getErrors().add(MessageFormat.format(I18nUtil.getMessage(
                    "ui.data.alert.tq.schedule.excel.planLessThanFinish"), row.getRowNum(), shiftOrder));
        }
    }

    /**
     * 构造更新或新增提案（无施工快照查询）。
     *
     * @param row          导入行
     * @param existing     已有结果
     * @param factoryCode  工厂编码
     * @param scheduleDate 排程日期
     * @param batchNo      当前批次
     * @return 待持久化提案
     */
    private TqScheduleResult buildProposal(TqScheduleResultVo row, TqScheduleResult existing,
                                           String factoryCode, Date scheduleDate, String batchNo) {
        TqScheduleResult target = new TqScheduleResult();
        if (existing != null) {
            BeanUtils.copyProperties(existing, target);
            target.setTaskVersion((existing.getTaskVersion() == null ? 0L : existing.getTaskVersion()) + 1L);
            target.setBaseVale(existing.getId());
        } else {
            target.setFactoryCode(factoryCode);
            target.setScheduleDate(scheduleDate);
            target.setBatchNo(batchNo);
            target.setOrderNo(batchNo + "-IMPORT-" + IdUtil.fastSimpleUUID().substring(0, 8));
            target.setBeadCode(row.getBeadCode());
            target.setMachineCode(row.getMachineCode());
            target.setReleaseStatus(TqScheduleConstants.RELEASE_STATUS_NOT_PUBLISHED);
            target.setDataSource(TqScheduleConstants.IMPORT_SCHEDULE_DATA_SOURCE);
            target.setTaskVersion(0L);
        }
        this.applyImportPlanFields(row, target);
        return target;
    }

    /**
     * 写入 CLASS1~3 计划量（无顺序字段）。
     *
     * @param row    导入行
     * @param target 目标结果
     */
    private void applyImportPlanFields(TqScheduleResultVo row, TqScheduleResult target) {
        for (int shiftOrder = 1; shiftOrder <= IMPORT_SHIFT_COUNT; shiftOrder++) {
            BigDecimal planQty = BigDecimalUtils.valueOf(this.getBigDecimalField(row,
                    TqScheduleConstants.SHIFT_PLAN_QTY_FIELD_TEMPLATE, shiftOrder));
            target.setFieldValueByFieldName(String.format(
                    TqScheduleConstants.SHIFT_PLAN_QTY_FIELD_TEMPLATE, shiftOrder), planQty.intValue());
        }
    }

    /**
     * 判断导入行是否至少一班计划量为正。
     *
     * @param row 导入行
     * @return true 表示至少一班为正
     */
    private boolean hasPositivePlanQty(TqScheduleResultVo row) {
        for (int shiftOrder = 1; shiftOrder <= IMPORT_SHIFT_COUNT; shiftOrder++) {
            if (this.getBigDecimalField(row, TqScheduleConstants.SHIFT_PLAN_QTY_FIELD_TEMPLATE,
                    shiftOrder).compareTo(BigDecimal.ZERO) > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * 在短事务中二次版本校验并批量新增、更新。
     *
     * @param persistRowList 待持久化行
     * @param proposalMap    行号对应提案
     * @return 成功行数
     * @throws ServiceException 二次版本校验或批量写入失败时抛出并回滚
     */
    private int persistImportRows(List<TqScheduleResultVo> persistRowList,
                                  Map<Integer, TqScheduleResult> proposalMap) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(this.platformTransactionManager);
        Integer successNum = transactionTemplate.execute(transactionStatus -> {
            // 二次查询更新目标行，校验版本和发布状态
            List<Long> updateIdList = persistRowList.stream()
                    .map(row -> proposalMap.get(row.getRowNum()))
                    .filter(item -> item.getId() != null)
                    .map(TqScheduleResult::getId)
                    .collect(Collectors.toList());
            Map<Long, TqScheduleResult> lockedResultMap = updateIdList.isEmpty()
                    ? Collections.emptyMap()
                    : this.emptyIfNull(this.tqScheduleResultMapper.selectBatchIds(updateIdList)).stream()
                            .collect(Collectors.toMap(TqScheduleResult::getId, Function.identity()));
            List<TqScheduleResult> updateList = new ArrayList<>();
            List<TqScheduleResult> insertList = new ArrayList<>();
            for (TqScheduleResultVo row : persistRowList) {
                TqScheduleResult proposal = proposalMap.get(row.getRowNum());
                if (row.getResultId() == null) {
                    insertList.add(proposal);
                    continue;
                }
                TqScheduleResult locked = lockedResultMap.get(row.getResultId());
                long lockedVersion = locked == null || locked.getTaskVersion() == null
                        ? 0L : locked.getTaskVersion();
                if (locked == null || row.getTaskVersion() == null
                        || lockedVersion != row.getTaskVersion()
                        || !TqScheduleConstants.RELEASE_STATUS_NOT_PUBLISHED
                                .equals(locked.getReleaseStatus())) {
                    throw new ServiceException(I18nUtil.getMessage(
                            "ui.data.alert.tq.schedule.excel.concurrentChanged"));
                }
                this.applyImportPlanFields(row, locked);
                locked.setTaskVersion(lockedVersion + 1L);
                locked.setBaseVale(locked.getId());
                updateList.add(locked);
            }
            if (CollUtil.isNotEmpty(updateList)) {
                Integer updatedRows = this.baseDao.updateBatch(updateList);
                if (updatedRows == null || updatedRows != updateList.size()) {
                    throw new ServiceException(I18nUtil.getMessage(
                            "ui.data.alert.tq.schedule.excel.persistFailed"));
                }
            }
            if (CollUtil.isNotEmpty(insertList)) {
                Integer insertedRows = this.baseDao.saveBatch(insertList);
                if (insertedRows == null || insertedRows != insertList.size()) {
                    throw new ServiceException(I18nUtil.getMessage(
                            "ui.data.alert.tq.schedule.excel.persistFailed"));
                }
            }
            return updateList.size() + insertList.size();
        });
        if (successNum == null) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tq.schedule.excel.persistFailed"));
        }
        return successNum;
    }

    /**
     * 查询有效机台编码（只按机台编码查询，不校验工厂）。
     *
     * @param machineCodeSet 导入机台编码
     * @return 有效机台编码
     */
    private Set<String> loadValidMachineCodes(Set<String> machineCodeSet) {
        if (machineCodeSet.isEmpty()) {
            return Collections.emptySet();
        }
        LambdaQueryWrapper<TqMachineInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(TqMachineInfo::getMachineCode, machineCodeSet);
        return this.emptyIfNull(this.tqMachineInfoMapper.selectList(wrapper)).stream()
                .map(TqMachineInfo::getMachineCode).filter(StrUtil::isNotBlank)
                .collect(Collectors.toSet());
    }

    /**
     * 查询当前批次结果。
     *
     * @param factoryCode  工厂编码
     * @param scheduleDate 排程日期
     * @param batchNo      当前批次
     * @return 当前结果
     */
    private List<TqScheduleResult> loadCurrentBatchResults(String factoryCode, Date scheduleDate, String batchNo) {
        LambdaQueryWrapper<TqScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StrUtil.isNotBlank(factoryCode), TqScheduleResult::getFactoryCode, factoryCode);
        wrapper.eq(TqScheduleResult::getScheduleDate, scheduleDate);
        wrapper.eq(TqScheduleResult::getBatchNo, batchNo);
        wrapper.orderByAsc(TqScheduleResult::getId);
        return this.emptyIfNull(this.tqScheduleResultMapper.selectList(wrapper));
    }

    /**
     * 按导入隐藏主键批量查询结果。
     *
     * @param rowList 导入行
     * @return 主键对应结果
     */
    private Map<Long, TqScheduleResult> loadRequestedResults(List<TqScheduleResultVo> rowList) {
        List<Long> resultIdList = rowList.stream().map(TqScheduleResultVo::getResultId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (resultIdList.isEmpty()) {
            return Collections.emptyMap();
        }
        return this.emptyIfNull(this.tqScheduleResultMapper.selectBatchIds(resultIdList)).stream()
                .collect(Collectors.toMap(TqScheduleResult::getId, Function.identity()));
    }

    // ==================== 工具方法 ====================

    /**
     * 构造整批阻断响应。
     *
     * @param message     阻断消息
     * @param importLogId 导入日志 ID
     * @param rowNum      Excel 行号，无法定位时为 0
     * @return 错误响应
     */
    private AjaxResult buildHardBlockResult(String message, Long importLogId, int rowNum) {
        return AjaxResult.error(message, Collections.singletonList(
                new ImportErrorLog(importLogId, rowNum, message)));
    }

    /**
     * 构造行错误日志。
     *
     * @param importLogId 导入日志 ID
     * @param row         导入行
     * @return 行错误日志
     */
    private ImportErrorLog buildRowError(Long importLogId, TqScheduleResultVo row) {
        return new ImportErrorLog(importLogId, row.getRowNum(), String.join(";", row.getErrors()));
    }

    /**
     * 构造包含成功数和失败明细的导入响应。
     *
     * @param successNum   成功数
     * @param errorLogList 行错误
     * @return 导入响应
     */
    private AjaxResult buildImportResult(int successNum, List<ImportErrorLog> errorLogList) {
        if (CollUtil.isNotEmpty(errorLogList)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + ","
                    + successNum + "," + errorLogList.size(), errorLogList);
        }
        return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
    }

    /**
     * 读取单元格文本。
     *
     * @param row              Excel 行
     * @param columnIndex      列索引
     * @param dataFormatter    格式化器
     * @param formulaEvaluator 公式计算器
     * @return 去除首尾空白的文本
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
     * 通过反射获取 BigDecimal 动态字段。
     *
     * <p>胎圈 VO 未提供 getFieldValueByFieldName 方法，统一用反射访问实体和 VO 班次字段。</p>
     *
     * @param source        字段来源对象
     * @param fieldTemplate 字段名模板
     * @param shiftOrder    班次序号
     * @return 数值，空值返回 0
     */
    private BigDecimal getBigDecimalField(Object source, String fieldTemplate, int shiftOrder) {
        if (source == null) {
            return BigDecimal.ZERO;
        }
        String fieldName = String.format(fieldTemplate, shiftOrder);
        Object value = this.readFieldValue(source, fieldName);
        return BigDecimalUtils.valueOf(value);
    }

    /**
     * 通过反射读取对象字段值。
     *
     * @param source    字段来源对象
     * @param fieldName Java 字段名
     * @return 字段值
     */
    private Object readFieldValue(Object source, String fieldName) {
        try {
            Field field = source.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(source);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalArgumentException("胎圈排程结果字段不存在: " + fieldName, exception);
        }
    }

    /**
     * 空值或零值转为空单元格。
     *
     * @param value 数值
     * @return 非零原值，否则返回 null
     */
    private BigDecimal blankIfZero(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) == 0 ? null : value;
    }

    /**
     * 格式化只包含行号的多语言消息。
     *
     * @param messageKey 多语言键
     * @param rowNum     Excel 行号
     * @return 格式化消息
     */
    private String formatRowMessage(String messageKey, int rowNum) {
        return MessageFormat.format(I18nUtil.getMessage(messageKey), rowNum);
    }

    /**
     * 获取或创建 Excel 行。
     *
     * @param sheet     工作表
     * @param rowIndex  行索引
     * @return Excel 行
     */
    private Row getOrCreateRow(Sheet sheet, int rowIndex) {
        Row row = sheet.getRow(rowIndex);
        return row == null ? sheet.createRow(rowIndex) : row;
    }

    /**
     * 获取或创建 Excel 单元格。
     *
     * @param row        Excel 行
     * @param columnIndex 列索引
     * @return Excel 单元格
     */
    private Cell getOrCreateCell(Row row, int columnIndex) {
        Cell cell = row.getCell(columnIndex);
        return cell == null ? row.createCell(columnIndex) : cell;
    }

    /**
     * 将空列表标准化为空集合。
     *
     * @param list 原列表
     * @param <T>  元素类型
     * @return 非空列表
     */
    private <T> List<T> emptyIfNull(List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }
}
