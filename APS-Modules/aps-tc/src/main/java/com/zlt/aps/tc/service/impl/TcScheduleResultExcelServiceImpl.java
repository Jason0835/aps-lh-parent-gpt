package com.zlt.aps.tc.service.impl;

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
import com.zlt.aps.tc.api.constant.TcScheduleConstants;
import com.zlt.aps.tc.api.domain.dto.TcScheduleResultImportDTO;
import com.zlt.aps.tc.api.domain.entity.*;
import com.zlt.aps.tc.api.enums.TcScheduleReleaseStatusEnum;
import com.zlt.aps.tc.api.enums.TcVersionMatchModeEnum;
import com.zlt.aps.tc.api.enums.TcYesNoEnum;
import com.zlt.aps.tc.component.TcAutoScheduleExecutionGuard;
import com.zlt.aps.tc.domain.vo.*;
import com.zlt.aps.tc.mapper.*;
import com.zlt.aps.tc.service.ITcScheduleResultExcelService;
import com.zlt.aps.tc.service.TcAutoScheduleTaskService;
import com.zlt.aps.tc.service.query.TcManualOptionsService;
import com.zlt.common.utils.ImportExcelUtils;
import com.zlt.core.dao.basedao.BaseDao;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 胎侧排程结果专用模板导入导出服务实现。
 *
 * <p>导出只读取指定工厂、日期的当前有效批次，并按胎侧版本匹配口径补充成型需求和卷曲长度。
 * 导入使用隐藏主键、施工版本和任务版本校验安全回导，新增行则从施工资料重新构造可信快照。</p>
 */
@Slf4j
@Service
public class TcScheduleResultExcelServiceImpl implements ITcScheduleResultExcelService {

    /** 模板资源路径。 */
    private static final String TEMPLATE_RESOURCE = "excelModel/tcScheduleResult.xlsx";

    /** 固定工作表名称。 */
    private static final String SHEET_NAME = "胎侧 SW Hông lốp";

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

    /** 用户可见列数量 A:W。 */
    private static final int VISIBLE_COLUMN_COUNT = 23;

    /** 模板总列数量 A:Z。 */
    private static final int TOTAL_COLUMN_COUNT = 26;

    /** 模板处理班次数。 */
    private static final int IMPORT_SHIFT_COUNT = 3;

    @Resource
    private TcScheduleResultMapper tcScheduleResultMapper;

    @Resource
    private TcScheduleUnplannedMapper tcScheduleUnplannedMapper;

    @Resource
    private TcMachineInfoMapper tcMachineInfoMapper;

    @Resource
    private TcAutoScheduleDataLoadMapper tcAutoScheduleDataLoadMapper;

    @Resource
    private TcParamsMapper tcParamsMapper;

    @Resource
    private TcCurlRollMapper tcCurlRollMapper;

    @Resource
    private TcManualOptionsService tcManualOptionsService;

    @Resource
    private TcAutoScheduleExecutionGuard tcAutoScheduleExecutionGuard;

    @Resource
    private TcAutoScheduleTaskService tcAutoScheduleTaskService;

    @Resource
    private BaseDao baseDao;

    @Resource
    private PlatformTransactionManager platformTransactionManager;

    @Resource
    private IExportLogService iExportLogService;

    @Resource
    private IImportLogService iImportLogService;

    @Resource
    private IImportErrorLogService iImportErrorLogService;

    /**
     * 按专用模板导出胎侧排程结果。
     *
     * @param queryVO 查询条件，必须包含工厂和排程日期
     * @param fileName 导出文件名称
     * @return Excel 文件字节
     * @throws ServiceException 条件不完整、模板缺失或工作簿生成失败时抛出
     */
    @Override
    public byte[] exportDataScheduleResult(TcScheduleResult queryVO, String fileName) {
        this.validateExportQuery(queryVO);
        Date beginTime = DateUtils.getNowDate();
        Date scheduleDate = DateUtil.beginOfDay(queryVO.getScheduleDate());
        String currentBatchNo = this.resolveCurrentBatchNo(queryVO.getFactoryCode(), scheduleDate);
        List<TcScheduleResult> resultList = Boolean.TRUE.equals(queryVO.getExportTemplate())
                ? Collections.emptyList() : this.listExportResults(queryVO, currentBatchNo);
        List<Map<String, Object>> dataList = this.buildExportDataList(resultList, queryVO);
        List<List<Map<String, Object>>> excelDataList = new ArrayList<>();
        if (CollUtil.isNotEmpty(dataList)) {
            excelDataList.add(dataList);
        }

        byte[] resultBytes;
        try (InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(TEMPLATE_RESOURCE)) {
            if (inputStream == null) {
                throw new ServiceException(I18nUtil.getMessage(
                        "ui.data.alert.tc.schedule.excel.templateMissing"));
            }
            Map<String, Object> tableMap = this.getTitleMap(queryVO);
            this.setExportTitleFieldName(tableMap);
            resultBytes = ExcelUtils.writeMultiList(inputStream, 0, tableMap, excelDataList);
        } catch (IOException exception) {
            log.error("生成胎侧排程结果模板失败", exception);
            throw new ServiceException(I18nUtil.getMessage(
                    "ui.data.alert.tc.schedule.excel.generateFailed"));
        }
        resultBytes = this.finishExportWorkbook(resultBytes, dataList.isEmpty());
        this.saveExportLog(queryVO, fileName, resultList.size(), beginTime);
        return resultBytes;
    }

    /**
     * 按专用模板导入胎侧排程结果。
     *
     * @param importDTO 导入文件和业务条件
     * @param updateSupport 是否允许覆盖更新
     * @return 导入结果和行级错误
     * @throws Exception 导入日志或文件处理失败时抛出
     */
    @Override
    public AjaxResult importDataScheduleResult(TcScheduleResultImportDTO importDTO,
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
            TcScheduleResultExcelParseResult parseResult = this.parseWorkbook(importContext.getFileBytes(),
                    importDTO.getScheduleResult());
            rowCount = parseResult.getRowList().size();
            ajaxResult = this.doImport(parseResult, importDTO.getScheduleResult(), updateSupport, importLog.getId());
        } catch (ServiceException exception) {
            log.warn("胎侧排程结果模板导入校验失败，原因={}", exception.getMessage());
            ajaxResult = AjaxResult.error(exception.getMessage());
        } catch (RuntimeException exception) {
            log.error("胎侧排程结果模板导入失败", exception);
            ajaxResult = AjaxResult.error(I18nUtil.getMessage(
                    "ui.data.alert.tc.schedule.excel.importFailed"));
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
     * 构造模板动态标题。
     *
     * @param queryVO 导出条件
     * @return 模板占位符和值
     */
    private Map<String, Object> getTitleMap(TcScheduleResult queryVO) {
        Map<String, Object> tableMap = new HashMap<>();
        String currentDate = DateUtil.format(queryVO.getScheduleDate(), TITLE_DATE_FORMAT);
        tableMap.put("title", MessageFormat.format(
                "{0}全钢压出工程生产计划单 Đơn kế hoạch sản xuất của công đoạn ép đùn toàn thép",
                currentDate));
        String previousDay = DateUtil.format(DateUtil.offsetDay(queryVO.getScheduleDate(), -1), "MM/dd");
        String currentDay = DateUtil.format(queryVO.getScheduleDate(), "MM/dd");
        tableMap.put("lastDayTitle", MessageFormat.format("早班{0}\nCa sáng {0}", previousDay));
        tableMap.put("midTitle", MessageFormat.format("中班{0}\nCa chiều {0}", previousDay));
        tableMap.put("nightTitle", MessageFormat.format("夜班{0}\nCa đêm {0}", currentDay));
        tableMap.put("dayTitle", MessageFormat.format("早班{0}\nCa sáng {0}", currentDay));
        tableMap.put("cxPlanTitle", MessageFormat.format(
                "成型产量 Sản lượng TH\n早班{0}到早班{1}\nCa sáng {0} đến ca sáng {1}",
                previousDay, currentDay));
        return tableMap;
    }

    /**
     * 校验导出条件。
     *
     * @param queryVO 导出条件
     * @throws ServiceException 工厂或排程日期为空时抛出
     */
    private void validateExportQuery(TcScheduleResult queryVO) {
        if (queryVO == null || StrUtil.isBlank(queryVO.getFactoryCode())) {
            throw new ServiceException(I18nUtil.getMessage(
                    "ui.data.alert.tc.schedule.excel.factoryRequired"));
        }
        if (queryVO.getScheduleDate() == null) {
            throw new ServiceException(I18nUtil.getMessage(
                    "ui.data.alert.tc.schedule.excel.dateRequired"));
        }
    }

    /**
     * 校验导入上下文。
     *
     * @param importDTO 导入请求
     * @return 文件导入上下文
     * @throws ServiceException 文件、类型、工厂或日期无效时抛出
     */
    private ImportContext validateImportContext(TcScheduleResultImportDTO importDTO) {
        if (importDTO == null || importDTO.getImportContext() == null
                || importDTO.getImportContext().getFileBytes() == null
                || importDTO.getImportContext().getFileBytes().length == 0) {
            throw new ServiceException(I18nUtil.getMessage(
                    "ui.data.alert.tc.schedule.excel.fileRequired"));
        }
        String originalFileName = StrUtil.trim(importDTO.getImportContext().getOriFileName());
        if (StrUtil.isBlank(originalFileName)
                || !originalFileName.toLowerCase(Locale.ROOT).endsWith(ExcelUtil.XLSX_FILE)) {
            throw new ServiceException(I18nUtil.getMessage(
                    "ui.data.alert.tc.schedule.excel.fileTypeInvalid"));
        }
        if (importDTO.getScheduleResult() == null
                || StrUtil.isBlank(importDTO.getScheduleResult().getFactoryCode())) {
            throw new ServiceException(I18nUtil.getMessage(
                    "ui.data.alert.tc.schedule.excel.factoryRequired"));
        }
        if (importDTO.getScheduleResult().getScheduleDate() == null) {
            throw new ServiceException(I18nUtil.getMessage(
                    "ui.data.alert.tc.schedule.excel.dateRequired"));
        }
        return importDTO.getImportContext();
    }

    /**
     * 查询当前有效批次导出明细。
     *
     * @param queryVO 查询条件
     * @param currentBatchNo 当前有效批次
     * @return 排程结果
     */
    private List<TcScheduleResult> listExportResults(TcScheduleResult queryVO, String currentBatchNo) {
        LambdaQueryWrapper<TcScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TcScheduleResult::getFactoryCode, queryVO.getFactoryCode().trim());
        wrapper.eq(TcScheduleResult::getScheduleDate, DateUtil.beginOfDay(queryVO.getScheduleDate()));
        wrapper.eq(TcScheduleResult::getBatchNo, currentBatchNo);
        wrapper.eq(StrUtil.isNotBlank(queryVO.getMachineCode()), TcScheduleResult::getMachineCode,
                queryVO.getMachineCode());
        wrapper.like(StrUtil.isNotBlank(queryVO.getSidewallCode()), TcScheduleResult::getSidewallCode,
                queryVO.getSidewallCode());
        wrapper.orderByAsc(TcScheduleResult::getMachineCode, TcScheduleResult::getClass1Sequence,
                TcScheduleResult::getClass2Sequence, TcScheduleResult::getClass3Sequence,
                TcScheduleResult::getSidewallCode, TcScheduleResult::getConstructionVersion,
                TcScheduleResult::getId);
        return this.emptyIfNull(this.tcScheduleResultMapper.selectList(wrapper));
    }

    /**
     * 将 Excel 字段国际化名称写入模板第 1 行。
     *
     * @param tableMap 模板占位符映射
     */
    private void setExportTitleFieldName(Map<String, Object> tableMap) {
        for (Field field : TcScheduleResultVo.class.getDeclaredFields()) {
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
     * 构造导出明细。
     *
     * @param resultList 当前批次结果
     * @param queryVO 导出条件
     * @return 模板列表数据
     */
    private List<Map<String, Object>> buildExportDataList(List<TcScheduleResult> resultList,
                                                           TcScheduleResult queryVO) {
        if (CollUtil.isEmpty(resultList)) {
            return Collections.emptyList();
        }
        Map<String, TcScheduleResult> previousResultMap = this.buildPreviousResultMap(queryVO);
        Map<String, TcScheduleResultFormingDataVo> formingDataMap = this.buildFormingDataMap(
                queryVO.getFactoryCode(), DateUtil.beginOfDay(queryVO.getScheduleDate()));
        Map<String, BigDecimal> curlLengthMap = this.buildCurlLengthMap(queryVO.getFactoryCode(), resultList);
        return resultList.stream().map(result -> {
            Map<String, Object> rowMap = new LinkedHashMap<>();
            String resultKey = this.buildResultVersionKey(result);
            TcScheduleResult previousResult = previousResultMap.get(resultKey);
            TcScheduleResultFormingDataVo formingData = formingDataMap.get(
                    this.buildSidewallVersionKey(result.getSidewallCode(), result.getConstructionVersion()));
            rowMap.put("machineCode", result.getMachineCode());
            rowMap.put("sidewallLength", result.getSidewallLength());
            rowMap.put("cxRemainQty", this.blankIfZero(formingData == null ? null : formingData.getCxRemainQty()));
            rowMap.put("sidewallCode", result.getSidewallCode());
            rowMap.put("materialDesc", formingData == null ? null : formingData.getMaterialDesc());
            rowMap.put("wholeGlueCode", this.buildExportGlueCode(result));
            rowMap.put("stockQty", this.blankIfZero(result.getStockQty()));
            rowMap.put("lastDayPlanQty", this.blankIfZero(
                    previousResult == null ? null : previousResult.getClass3PlanQty()));
            rowMap.put("lastDayFinishQty", this.blankIfZero(
                    previousResult == null ? null : previousResult.getClass3FinishQty()));
            rowMap.put("lastDaySequence", previousResult == null ? null : previousResult.getClass3Sequence());
            rowMap.put("class1PlanQty", this.blankIfZero(result.getClass1PlanQty()));
            rowMap.put("class1FinishQty", this.blankIfZero(result.getClass1FinishQty()));
            rowMap.put("class1Sequence", result.getClass1Sequence());
            rowMap.put("class2PlanQty", this.blankIfZero(result.getClass2PlanQty()));
            rowMap.put("class2FinishQty", this.blankIfZero(result.getClass2FinishQty()));
            rowMap.put("class2Sequence", result.getClass2Sequence());
            rowMap.put("class3PlanQty", this.blankIfZero(result.getClass3PlanQty()));
            rowMap.put("class3FinishQty", this.blankIfZero(result.getClass3FinishQty()));
            rowMap.put("class3Sequence", result.getClass3Sequence());
            rowMap.put("cxPlanQty", this.blankIfZero(formingData == null ? null : formingData.getCxPlanQty()));
            rowMap.put("curlRollLength", curlLengthMap.get(result.getSidewallCode()));
            rowMap.put("cxMachineCode", formingData == null ? null : formingData.getCxMachineCode());
            rowMap.put("type", TcYesNoEnum.YES.getCode().equals(result.getTailFlag())
                    ? I18nUtil.getMessage("ui.data.alert.tc.schedule.excel.tailType") : "");
            rowMap.put("resultId", result.getId());
            rowMap.put("constructionVersion", result.getConstructionVersion());
            rowMap.put("taskVersion", result.getTaskVersion() == null ? 0L : result.getTaskVersion());
            return rowMap;
        }).collect(Collectors.toList());
    }

    /**
     * 组合排程结果的主胶料和基部胶编码，供专用模板胶种列导出使用。
     *
     * @param result 排程结果
     * @return 使用英文逗号分隔的有效胶料编码；无有效编码时返回空字符串
     */
    private String buildExportGlueCode(TcScheduleResult result) {
        return Arrays.asList(result.getGlueCode(), result.getBaseGlueCode()).stream()
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.joining(","));
    }

    /**
     * 查询前一日当前有效批次的 CLASS3 数据。
     *
     * @param queryVO 当前导出条件
     * @return 机台、胎侧编码和施工版本对应的前日结果
     */
    private Map<String, TcScheduleResult> buildPreviousResultMap(TcScheduleResult queryVO) {
        Date previousDate = DateUtil.beginOfDay(DateUtil.offsetDay(queryVO.getScheduleDate(), -1));
        String previousBatchNo = this.resolveCurrentBatchNo(queryVO.getFactoryCode(), previousDate);
        LambdaQueryWrapper<TcScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TcScheduleResult::getFactoryCode, queryVO.getFactoryCode());
        wrapper.eq(TcScheduleResult::getScheduleDate, previousDate);
        wrapper.eq(TcScheduleResult::getBatchNo, previousBatchNo);
        wrapper.orderByDesc(TcScheduleResult::getId);
        return this.emptyIfNull(this.tcScheduleResultMapper.selectList(wrapper)).stream()
                .filter(item -> StrUtil.isNotBlank(item.getMachineCode())
                        && StrUtil.isNotBlank(item.getSidewallCode())
                        && StrUtil.isNotBlank(item.getConstructionVersion()))
                .collect(Collectors.toMap(this::buildResultVersionKey, Function.identity(),
                        (first, ignored) -> first, LinkedHashMap::new));
    }

    /**
     * 按施工版本匹配模式构造成型数据。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @return 胎侧编码和施工版本对应的成型数据
     */
    private Map<String, TcScheduleResultFormingDataVo> buildFormingDataMap(String factoryCode,
                                                                           Date scheduleDate) {
        if (TcVersionMatchModeEnum.BOM == this.resolveVersionMatchMode(factoryCode)) {
            return this.buildBomFormingDataMap(factoryCode, scheduleDate);
        }
        return this.buildRecipeFormingDataMap(factoryCode, scheduleDate);
    }

    /**
     * 按 BOM 模式汇总 CLASS1~4 成型计划量和展示字段。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @return 成型数据
     */
    private Map<String, TcScheduleResultFormingDataVo> buildBomFormingDataMap(String factoryCode,
                                                                              Date scheduleDate) {
        Map<String, TcScheduleResultFormingDataVo> resultMap = new LinkedHashMap<>();
        this.emptyIfNull(this.tcAutoScheduleDataLoadMapper.selectFormingDemandRows(factoryCode, scheduleDate))
                .stream().filter(Objects::nonNull)
                .filter(row -> StrUtil.isNotBlank(row.getSidewallCode())
                        && StrUtil.isNotBlank(row.getConstructionVersion()))
                .forEach(row -> this.mergeFormingData(resultMap, row.getSidewallCode(),
                        row.getConstructionVersion(), BigDecimalUtils.add(row.getClass1PlanQty(),
                                row.getClass2PlanQty(), row.getClass3PlanQty(), row.getClass4PlanQty()),
                        row.getCxRemainQty(), row.getMaterialDesc(), row.getCxMachineCode()));
        return resultMap;
    }

    /**
     * 按 RECIPE 模式逐班解析施工版本并汇总成型数据。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @return 成型数据
     */
    private Map<String, TcScheduleResultFormingDataVo> buildRecipeFormingDataMap(String factoryCode,
                                                                                 Date scheduleDate) {
        List<TcFormingDemandRecipeRowVo> rowList = this.emptyIfNull(
                this.tcAutoScheduleDataLoadMapper.selectFormingDemandRowsByRecipe(factoryCode, scheduleDate));
        Set<String> embryoCodeSet = rowList.stream().map(TcFormingDemandRecipeRowVo::getEmbryoCode)
                .filter(StrUtil::isNotBlank).collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> recipeNoSet = new LinkedHashSet<>();
        rowList.forEach(row -> {
            this.addNotBlank(recipeNoSet, row.getClass1RecipeNo());
            this.addNotBlank(recipeNoSet, row.getClass2RecipeNo());
            this.addNotBlank(recipeNoSet, row.getClass3RecipeNo());
            this.addNotBlank(recipeNoSet, row.getClass4RecipeNo());
        });
        if (embryoCodeSet.isEmpty() || recipeNoSet.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, TcConstructionSidewallRowVo> constructionMap = this.emptyIfNull(
                this.tcAutoScheduleDataLoadMapper.selectConstructionInfoRows(
                        factoryCode, embryoCodeSet, recipeNoSet)).stream()
                .filter(Objects::nonNull)
                .filter(item -> StrUtil.isNotBlank(item.getConstructionCode())
                        && StrUtil.isNotBlank(item.getConstructionVersion()))
                .collect(Collectors.toMap(item -> this.buildConstructionKey(item.getConstructionCode(),
                                item.getConstructionVersion()), Function.identity(),
                        (first, ignored) -> first, LinkedHashMap::new));
        Map<String, TcScheduleResultFormingDataVo> resultMap = new LinkedHashMap<>();
        rowList.forEach(row -> this.mergeRecipeRow(resultMap, constructionMap, row));
        return resultMap;
    }

    /**
     * 将一条成型 RECIPE 行按胎侧版本归并，余量只对同一成型行和胎侧版本累计一次。
     *
     * @param resultMap 汇总结果
     * @param constructionMap 施工版本映射
     * @param row 成型需求行
     */
    private void mergeRecipeRow(Map<String, TcScheduleResultFormingDataVo> resultMap,
                                Map<String, TcConstructionSidewallRowVo> constructionMap,
                                TcFormingDemandRecipeRowVo row) {
        Map<String, BigDecimal> rowPlanMap = new LinkedHashMap<>();
        Map<String, TcConstructionSidewallRowVo> rowConstructionMap = new LinkedHashMap<>();
        this.mergeRecipeShift(rowPlanMap, rowConstructionMap, constructionMap, row.getEmbryoCode(),
                row.getClass1RecipeNo(), row.getClass1PlanQty());
        this.mergeRecipeShift(rowPlanMap, rowConstructionMap, constructionMap, row.getEmbryoCode(),
                row.getClass2RecipeNo(), row.getClass2PlanQty());
        this.mergeRecipeShift(rowPlanMap, rowConstructionMap, constructionMap, row.getEmbryoCode(),
                row.getClass3RecipeNo(), row.getClass3PlanQty());
        this.mergeRecipeShift(rowPlanMap, rowConstructionMap, constructionMap, row.getEmbryoCode(),
                row.getClass4RecipeNo(), row.getClass4PlanQty());
        rowPlanMap.forEach((key, planQty) -> {
            TcConstructionSidewallRowVo construction = rowConstructionMap.get(key);
            this.mergeFormingData(resultMap, construction.getSidewallCode(), construction.getSidewallVersion(),
                    planQty, row.getCxRemainQty(), row.getMaterialDesc(), row.getCxMachineCode());
        });
    }

    /**
     * 归并一班成型计划量到行内胎侧版本。
     *
     * @param rowPlanMap 行内计划量
     * @param rowConstructionMap 行内施工映射
     * @param constructionMap 全量施工映射
     * @param embryoCode 胎胚编码
     * @param recipeNo 示方书编号
     * @param planQty 班次计划量
     */
    private void mergeRecipeShift(Map<String, BigDecimal> rowPlanMap,
                                  Map<String, TcConstructionSidewallRowVo> rowConstructionMap,
                                  Map<String, TcConstructionSidewallRowVo> constructionMap,
                                  String embryoCode, String recipeNo, BigDecimal planQty) {
        if (StrUtil.isBlank(embryoCode) || StrUtil.isBlank(recipeNo)) {
            return;
        }
        TcConstructionSidewallRowVo construction = constructionMap.get(
                this.buildConstructionKey(embryoCode, recipeNo));
        if (construction == null || StrUtil.isBlank(construction.getSidewallCode())
                || StrUtil.isBlank(construction.getSidewallVersion())) {
            return;
        }
        String key = this.buildSidewallVersionKey(construction.getSidewallCode(),
                construction.getSidewallVersion());
        rowPlanMap.merge(key, BigDecimalUtils.valueOf(planQty), BigDecimal::add);
        rowConstructionMap.putIfAbsent(key, construction);
    }

    /**
     * 合并成型计划量和展示字段。
     *
     * @param resultMap 汇总结果
     * @param sidewallCode 胎侧编码
     * @param constructionVersion 胎侧施工版本
     * @param planQty 成型计划量
     * @param remainQty 成型余量
     * @param materialDesc 物料描述
     * @param cxMachineCode 成型机台
     */
    private void mergeFormingData(Map<String, TcScheduleResultFormingDataVo> resultMap,
                                  String sidewallCode, String constructionVersion,
                                  BigDecimal planQty, BigDecimal remainQty,
                                  String materialDesc, String cxMachineCode) {
        String key = this.buildSidewallVersionKey(sidewallCode, constructionVersion);
        TcScheduleResultFormingDataVo target = resultMap.computeIfAbsent(key, ignored -> {
            TcScheduleResultFormingDataVo dataVo = new TcScheduleResultFormingDataVo();
            dataVo.setSidewallCode(sidewallCode);
            dataVo.setConstructionVersion(constructionVersion);
            dataVo.setCxPlanQty(BigDecimal.ZERO);
            dataVo.setCxRemainQty(BigDecimal.ZERO);
            return dataVo;
        });
        target.setCxPlanQty(BigDecimalUtils.add(target.getCxPlanQty(), planQty));
        target.setCxRemainQty(BigDecimalUtils.add(target.getCxRemainQty(), remainQty));
        if (StrUtil.isBlank(target.getMaterialDesc()) && StrUtil.isNotBlank(materialDesc)) {
            target.setMaterialDesc(materialDesc);
        }
        if (StrUtil.isBlank(target.getCxMachineCode()) && StrUtil.isNotBlank(cxMachineCode)) {
            target.setCxMachineCode(cxMachineCode);
        }
    }

    /**
     * 解析工厂施工版本匹配模式。
     *
     * @param factoryCode 工厂编码
     * @return BOM 或 RECIPE，未配置时返回 RECIPE
     */
    private TcVersionMatchModeEnum resolveVersionMatchMode(String factoryCode) {
        LambdaQueryWrapper<TcParams> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TcParams::getFactoryCode, factoryCode);
        wrapper.eq(TcParams::getParamCode, TcScheduleConstants.PARAM_VERSION_MATCH_MODE);
        wrapper.eq(TcParams::getEnableStatus, TcYesNoEnum.YES.getCode());
        String mode = this.emptyIfNull(this.tcParamsMapper.selectList(wrapper)).stream()
                .filter(Objects::nonNull).map(TcParams::getParamValue)
                .filter(StrUtil::isNotBlank).findFirst()
                .orElse(TcScheduleConstants.DEFAULT_VERSION_MATCH_MODE);
        return TcVersionMatchModeEnum.resolve(mode);
    }

    /**
     * 查询导出胎侧对应的卷曲长度。
     *
     * @param factoryCode 工厂编码
     * @param resultList 排程结果
     * @return 胎侧编码对应卷曲长度
     */
    private Map<String, BigDecimal> buildCurlLengthMap(String factoryCode,
                                                       List<TcScheduleResult> resultList) {
        Set<String> sidewallCodeSet = resultList.stream().map(TcScheduleResult::getSidewallCode)
                .filter(StrUtil::isNotBlank).collect(Collectors.toSet());
        if (sidewallCodeSet.isEmpty()) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<TcCurlRoll> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TcCurlRoll::getFactoryCode, factoryCode);
        wrapper.in(TcCurlRoll::getSidewallCode, sidewallCodeSet);
        return this.emptyIfNull(this.tcCurlRollMapper.selectList(wrapper)).stream()
                .filter(item -> StrUtil.isNotBlank(item.getSidewallCode()))
                .collect(Collectors.toMap(TcCurlRoll::getSidewallCode, TcCurlRoll::getCurlLength,
                        (first, ignored) -> first, LinkedHashMap::new));
    }

    /**
     * 完成导出工作簿的隐藏元数据和空模板清理。
     *
     * @param sourceBytes 模板填充后字节
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
            log.error("处理胎侧排程结果导出工作簿失败", exception);
            throw new ServiceException(I18nUtil.getMessage(
                    "ui.data.alert.tc.schedule.excel.generateFailed"));
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
     * 解析导入工作簿。
     *
     * @param fileBytes Excel 文件字节
     * @param condition 导入条件
     * @return 标题日期和有效明细
     * @throws ServiceException 模板、工作表、标题或日期无效时抛出
     */
    private TcScheduleResultExcelParseResult parseWorkbook(byte[] fileBytes, TcScheduleResult condition) {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(fileBytes))) {
            Sheet sheet = workbook.getSheet(SHEET_NAME);
            if (sheet == null || sheet.getRow(HEADER_ROW_INDEX) == null) {
                throw new ServiceException(I18nUtil.getMessage(
                        "ui.data.alert.tc.schedule.excel.templateInvalid"));
            }
            DataFormatter dataFormatter = new DataFormatter();
            FormulaEvaluator formulaEvaluator = workbook.getCreationHelper().createFormulaEvaluator();
            Date scheduleDate = this.parseScheduleDate(this.readCellText(
                    sheet.getRow(TITLE_ROW_INDEX), 0, dataFormatter, formulaEvaluator));
            if (!DateUtil.isSameDay(scheduleDate, condition.getScheduleDate())) {
                throw new ServiceException(I18nUtil.getMessage(
                        "ui.data.alert.tc.schedule.excel.dateMismatch"));
            }
            List<TcScheduleResultVo> rowList = this.parseVoList(sheet, dataFormatter, formulaEvaluator);
            if (rowList.isEmpty()) {
                throw new ServiceException(I18nUtil.getMessage(
                        "ui.data.alert.tc.schedule.excel.noData"));
            }
            TcScheduleResultExcelParseResult parseResult = new TcScheduleResultExcelParseResult();
            parseResult.setScheduleDate(DateUtil.beginOfDay(scheduleDate));
            parseResult.setRowList(rowList);
            return parseResult;
        } catch (ServiceException exception) {
            throw exception;
        } catch (Exception exception) {
            log.warn("解析胎侧排程结果模板失败", exception);
            throw new ServiceException(I18nUtil.getMessage(
                    "ui.data.alert.tc.schedule.excel.templateInvalid"));
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
            throw new ServiceException(I18nUtil.getMessage(
                    "ui.data.alert.tc.schedule.excel.titleInvalid"));
        }
        try {
            return DateUtil.parse(matcher.group(1), TITLE_DATE_FORMAT);
        } catch (RuntimeException exception) {
            throw new ServiceException(I18nUtil.getMessage(
                    "ui.data.alert.tc.schedule.excel.titleInvalid"));
        }
    }

    /**
     * 按隐藏国际化表头解析明细。
     *
     * @param sheet 模板工作表
     * @param dataFormatter 单元格格式化器
     * @param formulaEvaluator 公式计算器
     * @return 明细行
     * @throws ServiceException 缺少任一可见或隐藏字段表头时抛出
     */
    private List<TcScheduleResultVo> parseVoList(Sheet sheet, DataFormatter dataFormatter,
                                                 FormulaEvaluator formulaEvaluator) {
        Map<Integer, Field> fieldMap = this.resolveImportFieldMap(
                sheet.getRow(HEADER_ROW_INDEX), dataFormatter, formulaEvaluator);
        List<TcScheduleResultVo> rowList = new ArrayList<>();
        for (int rowIndex = DATA_START_ROW_INDEX; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row excelRow = sheet.getRow(rowIndex);
            if (excelRow == null) {
                continue;
            }
            TcScheduleResultVo row = new TcScheduleResultVo();
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
     * @param headerRow 隐藏表头行
     * @param dataFormatter 单元格格式化器
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
        for (Field field : TcScheduleResultVo.class.getDeclaredFields()) {
            Excel attr = field.getAnnotation(Excel.class);
            if (attr == null || (attr.type() != Excel.Type.ALL && attr.type() != Excel.Type.IMPORT)) {
                continue;
            }
            Integer columnIndex = headerCellMap.get(this.resolveExcelHeaderName(attr));
            if (columnIndex == null) {
                throw new ServiceException(I18nUtil.getMessage(
                        "ui.data.alert.tc.schedule.excel.hiddenHeaderInvalid"));
            }
            field.setAccessible(true);
            fieldMap.put(columnIndex, field);
        }
        if (fieldMap.size() != TOTAL_COLUMN_COUNT) {
            throw new ServiceException(I18nUtil.getMessage(
                    "ui.data.alert.tc.schedule.excel.hiddenHeaderInvalid"));
        }
        return fieldMap;
    }

    /**
     * 将单元格文本转换为字段类型，并把格式错误记录为行级错误。
     *
     * @param text 单元格文本
     * @param field 目标字段
     * @param row 导入行
     * @return 转换值；格式错误时返回 null
     */
    private Object convertCellValue(String text, Field field, TcScheduleResultVo row) {
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
                    "ui.data.alert.tc.schedule.excel.invalidNumber"), row.getRowNum(),
                    this.resolveExcelHeaderName(field.getAnnotation(Excel.class))));
            return null;
        }
        return text;
    }

    /**
     * 在工厂日期级执行锁内校验并持久化导入。
     *
     * @param parseResult 解析结果
     * @param condition 导入条件
     * @param updateSupport 是否允许更新
     * @param importLogId 导入日志 ID
     * @return 导入结果
     */
    private AjaxResult doImport(TcScheduleResultExcelParseResult parseResult, TcScheduleResult condition,
                                boolean updateSupport, Long importLogId) {
        String factoryCode = condition.getFactoryCode().trim();
        Date scheduleDate = parseResult.getScheduleDate();
        String lockToken = this.tcAutoScheduleExecutionGuard.acquire(factoryCode, scheduleDate);
        try {
            if (this.tcAutoScheduleTaskService.findActive(factoryCode, scheduleDate) != null) {
                return this.buildHardBlockResult(I18nUtil.getMessage(
                        "ui.data.alert.tc.schedule.excel.activeTaskBlocked"), importLogId, 0);
            }
            return this.doImportWithLock(parseResult.getRowList(), factoryCode, scheduleDate,
                    updateSupport, importLogId);
        } finally {
            this.tcAutoScheduleExecutionGuard.release(factoryCode, scheduleDate, lockToken);
        }
    }

    /**
     * 在已取得执行锁后完成导入校验和短事务写入。
     *
     * @param rowList 导入行
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @param updateSupport 是否允许更新
     * @param importLogId 导入日志 ID
     * @return 导入结果
     */
    private AjaxResult doImportWithLock(List<TcScheduleResultVo> rowList, String factoryCode,
                                        Date scheduleDate, boolean updateSupport, Long importLogId) {
        String batchNo = this.resolveCurrentBatchNo(factoryCode, scheduleDate);
        List<TcScheduleResult> currentResultList = this.loadCurrentBatchResults(factoryCode, scheduleDate, batchNo);
        Map<Long, TcScheduleResult> allRequestedResultMap = this.loadRequestedResults(rowList);
        AjaxResult hardBlockResult = this.checkHardBlocks(rowList, currentResultList,
                allRequestedResultMap, factoryCode, scheduleDate, batchNo, importLogId);
        if (hardBlockResult != null) {
            return hardBlockResult;
        }

        Set<String> newSidewallCodeSet = rowList.stream().filter(row -> row.getResultId() == null)
                .map(TcScheduleResultVo::getSidewallCode).filter(StrUtil::isNotBlank)
                .map(String::trim).collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, TcScheduleResult> constructionSnapshotMap;
        try {
            constructionSnapshotMap = this.tcManualOptionsService.resolveUniqueConstructions(
                    factoryCode, newSidewallCodeSet);
        } catch (ServiceException exception) {
            return this.buildHardBlockResult(exception.getMessage(), importLogId, 0);
        }

        Set<String> machineCodeSet = rowList.stream().map(TcScheduleResultVo::getMachineCode)
                .filter(StrUtil::isNotBlank).map(String::trim).collect(Collectors.toSet());
        Set<String> validMachineCodeSet = this.loadValidMachineCodes(factoryCode, machineCodeSet);
        Set<Long> importResultIdSet = new HashSet<>();
        List<TcScheduleResultVo> validRowList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogList = new ArrayList<>();
        for (TcScheduleResultVo row : rowList) {
            TcScheduleResult existing = row.getResultId() == null ? null
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

        Map<Integer, TcScheduleResult> proposalMap = new LinkedHashMap<>();
        validRowList.forEach(row -> proposalMap.put(row.getRowNum(),
                this.buildProposal(row, allRequestedResultMap.get(row.getResultId()),
                        constructionSnapshotMap.get(row.getSidewallCode()), factoryCode, scheduleDate, batchNo)));
        this.validateFinalSequences(validRowList, proposalMap, currentResultList);
        validRowList.stream().filter(row -> CollUtil.isNotEmpty(row.getErrors()))
                .forEach(row -> importErrorLogList.add(this.buildRowError(importLogId, row)));
        List<TcScheduleResultVo> persistRowList = validRowList.stream()
                .filter(row -> CollUtil.isEmpty(row.getErrors())).collect(Collectors.toList());
        if (persistRowList.isEmpty()) {
            return this.buildImportResult(0, importErrorLogList);
        }

        int successNum = this.persistImportRows(factoryCode, scheduleDate, batchNo,
                persistRowList, proposalMap);
        return this.buildImportResult(successNum, importErrorLogList);
    }

    /**
     * 校验会阻断整批导入的一致性条件。
     *
     * @param rowList 导入行
     * @param currentResultList 当前批次结果
     * @param requestedResultMap 导入主键对应结果
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @param batchNo 当前批次
     * @param importLogId 导入日志 ID
     * @return 阻断结果；不存在阻断时返回 null
     */
    private AjaxResult checkHardBlocks(List<TcScheduleResultVo> rowList,
                                       List<TcScheduleResult> currentResultList,
                                       Map<Long, TcScheduleResult> requestedResultMap,
                                       String factoryCode, Date scheduleDate, String batchNo,
                                       Long importLogId) {
        TcScheduleResult released = currentResultList.stream().filter(item ->
                !TcScheduleReleaseStatusEnum.NOT_RELEASED.getCode().equals(item.getReleaseStatus()))
                .findFirst().orElse(null);
        if (released != null) {
            return this.buildHardBlockResult(MessageFormat.format(I18nUtil.getMessage(
                    "ui.data.alert.tc.schedule.excel.releaseBlocked"), released.getId()),
                    importLogId, 0);
        }
        for (TcScheduleResultVo row : rowList) {
            if (row.getResultId() == null) {
                continue;
            }
            TcScheduleResult current = requestedResultMap.get(row.getResultId());
            if (current == null || !Objects.equals(factoryCode, current.getFactoryCode())
                    || !DateUtil.isSameDay(scheduleDate, current.getScheduleDate())
                    || !Objects.equals(batchNo, current.getBatchNo())) {
                return this.buildHardBlockResult(MessageFormat.format(I18nUtil.getMessage(
                        "ui.data.alert.tc.schedule.excel.identityChanged"), row.getRowNum()),
                        importLogId, row.getRowNum());
            }
            if (!TcScheduleReleaseStatusEnum.NOT_RELEASED.getCode().equals(current.getReleaseStatus())) {
                return this.buildHardBlockResult(MessageFormat.format(I18nUtil.getMessage(
                        "ui.data.alert.tc.schedule.excel.releaseBlocked"), current.getId()),
                        importLogId, row.getRowNum());
            }
            boolean identityChanged = !Objects.equals(StrUtil.trim(row.getMachineCode()),
                    StrUtil.trim(current.getMachineCode()))
                    || !Objects.equals(StrUtil.trim(row.getSidewallCode()),
                    StrUtil.trim(current.getSidewallCode()))
                    || !Objects.equals(StrUtil.trim(row.getConstructionVersion()),
                    StrUtil.trim(current.getConstructionVersion()));
            if (identityChanged) {
                return this.buildHardBlockResult(MessageFormat.format(I18nUtil.getMessage(
                        "ui.data.alert.tc.schedule.excel.identityChanged"), row.getRowNum()),
                        importLogId, row.getRowNum());
            }
            long expectedVersion = row.getTaskVersion() == null ? -1L : row.getTaskVersion();
            long currentVersion = current.getTaskVersion() == null ? 0L : current.getTaskVersion();
            if (expectedVersion != currentVersion) {
                return this.buildHardBlockResult(MessageFormat.format(I18nUtil.getMessage(
                        "ui.data.alert.tc.schedule.excel.versionChanged"), row.getRowNum()),
                        importLogId, row.getRowNum());
            }
        }
        return null;
    }

    /**
     * 校验普通行错误。
     *
     * @param row 导入行
     * @param existing 更新目标，新增时为空
     * @param validMachineCodeSet 有效机台编码
     * @param updateSupport 是否允许更新
     * @param importResultIdSet 文件内结果主键集合
     */
    private void validateImportRow(TcScheduleResultVo row, TcScheduleResult existing,
                                   Set<String> validMachineCodeSet, boolean updateSupport,
                                   Set<Long> importResultIdSet) {
        if (StrUtil.isBlank(row.getSidewallCode())) {
            row.getErrors().add(this.formatRowMessage(
                    "ui.data.alert.tc.schedule.excel.sidewallRequired", row.getRowNum()));
        } else {
            row.setSidewallCode(row.getSidewallCode().trim());
        }
        if (StrUtil.isBlank(row.getMachineCode())) {
            row.getErrors().add(this.formatRowMessage(
                    "ui.data.alert.tc.schedule.excel.machineRequired", row.getRowNum()));
        } else {
            row.setMachineCode(row.getMachineCode().trim());
            if (!validMachineCodeSet.contains(row.getMachineCode())) {
                row.getErrors().add(MessageFormat.format(I18nUtil.getMessage(
                        "ui.data.alert.tc.schedule.excel.machineNotFound"),
                        row.getRowNum(), row.getMachineCode()));
            }
        }
        for (int shiftOrder = 1; shiftOrder <= IMPORT_SHIFT_COUNT; shiftOrder++) {
            BigDecimal planQty = this.getBigDecimalField(row,
                    TcScheduleConstants.SHIFT_PLAN_QTY_FIELD_TEMPLATE, shiftOrder);
            Integer sequence = this.getIntegerField(row,
                    TcScheduleConstants.SHIFT_SEQUENCE_FIELD_TEMPLATE, shiftOrder);
            BigDecimal finishQty = existing == null ? BigDecimal.ZERO : this.getBigDecimalField(existing,
                    TcScheduleConstants.SHIFT_FINISH_QTY_FIELD_TEMPLATE, shiftOrder);
            this.validatePlanAndSequence(row, planQty, finishQty, sequence, shiftOrder);
        }
        if (row.getResultId() != null) {
            if (!importResultIdSet.add(row.getResultId())) {
                row.getErrors().add(MessageFormat.format(I18nUtil.getMessage(
                        "ui.data.alert.tc.schedule.excel.duplicateResultId"),
                        row.getRowNum(), row.getResultId()));
            }
            if (!updateSupport) {
                row.getErrors().add(this.formatRowMessage(
                        "ui.data.alert.tc.schedule.excel.updateNotSupported", row.getRowNum()));
            }
        } else if (!this.hasPositivePlanQty(row)) {
            row.getErrors().add(this.formatRowMessage(
                    "ui.data.alert.tc.schedule.excel.allPlanZero", row.getRowNum()));
        }
    }

    /**
     * 校验计划量、完成量和顺序。
     *
     * @param row 导入行
     * @param planQty 计划量
     * @param finishQty 已有完成量
     * @param sequence 顺序
     * @param shiftOrder 班次序号
     */
    private void validatePlanAndSequence(TcScheduleResultVo row, BigDecimal planQty,
                                         BigDecimal finishQty, Integer sequence, int shiftOrder) {
        BigDecimal normalizedPlanQty = BigDecimalUtils.valueOf(planQty);
        if (normalizedPlanQty.compareTo(BigDecimal.ZERO) < 0) {
            row.getErrors().add(MessageFormat.format(I18nUtil.getMessage(
                    "ui.data.alert.tc.schedule.excel.planNegative"), row.getRowNum(), shiftOrder));
        }
        if (normalizedPlanQty.compareTo(BigDecimal.ZERO) > 0 && (sequence == null || sequence <= 0)) {
            row.getErrors().add(MessageFormat.format(I18nUtil.getMessage(
                    "ui.data.alert.tc.schedule.excel.sequenceRequired"), row.getRowNum(), shiftOrder));
        }
        if (normalizedPlanQty.compareTo(BigDecimalUtils.valueOf(finishQty)) < 0) {
            row.getErrors().add(MessageFormat.format(I18nUtil.getMessage(
                    "ui.data.alert.tc.schedule.excel.planLessThanFinish"),
                    row.getRowNum(), shiftOrder));
        }
    }

    /**
     * 构造更新或新增提案。
     *
     * @param row 导入行
     * @param existing 已有结果
     * @param constructionSnapshot 新增行施工快照
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @param batchNo 当前批次
     * @return 待持久化提案
     */
    private TcScheduleResult buildProposal(TcScheduleResultVo row, TcScheduleResult existing,
                                           TcScheduleResult constructionSnapshot,
                                           String factoryCode, Date scheduleDate, String batchNo) {
        TcScheduleResult target = new TcScheduleResult();
        if (existing != null) {
            BeanUtils.copyProperties(existing, target);
            target.setTaskVersion((existing.getTaskVersion() == null ? 0L : existing.getTaskVersion()) + 1L);
            target.setBaseVale(existing.getId());
        } else {
            if (constructionSnapshot == null) {
                throw new ServiceException(I18nUtil.getMessage(
                        "ui.data.alert.tc.schedule.excel.constructionNotFound"));
            }
            BeanUtils.copyProperties(constructionSnapshot, target);
            target.setFactoryCode(factoryCode);
            target.setScheduleDate(scheduleDate);
            target.setBatchNo(batchNo);
            target.setOrderNo(batchNo + "-IMPORT-" + IdUtil.fastSimpleUUID().substring(0, 8));
            target.setMachineCode(row.getMachineCode());
            target.setReleaseStatus(TcScheduleReleaseStatusEnum.NOT_RELEASED.getCode());
            target.setDataSource(TcScheduleConstants.IMPORT_SCHEDULE_DATA_SOURCE);
            target.setTaskVersion(0L);
            target.setTailFlag(TcYesNoEnum.NO.getCode());
        }
        this.applyImportPlanFields(row, target);
        return target;
    }

    /**
     * 写入 CLASS1~3 计划量和顺序。
     *
     * @param row 导入行
     * @param target 目标结果
     */
    private void applyImportPlanFields(TcScheduleResultVo row, TcScheduleResult target) {
        for (int shiftOrder = 1; shiftOrder <= IMPORT_SHIFT_COUNT; shiftOrder++) {
            BigDecimal planQty = BigDecimalUtils.valueOf(this.getBigDecimalField(row,
                    TcScheduleConstants.SHIFT_PLAN_QTY_FIELD_TEMPLATE, shiftOrder));
            Integer sequence = this.getIntegerField(row,
                    TcScheduleConstants.SHIFT_SEQUENCE_FIELD_TEMPLATE, shiftOrder);
            target.setFieldValueByFieldName(String.format(
                    TcScheduleConstants.SHIFT_PLAN_QTY_FIELD_TEMPLATE, shiftOrder), planQty);
            target.setFieldValueByFieldName(String.format(
                    TcScheduleConstants.SHIFT_SEQUENCE_FIELD_TEMPLATE, shiftOrder),
                    planQty.compareTo(BigDecimal.ZERO) > 0 ? sequence : null);
        }
    }

    /**
     * 校验每个受影响机台每班最终顺序唯一且从 1 连续。
     *
     * @param validRowList 初步有效导入行
     * @param proposalMap 行号对应提案
     * @param currentResultList 当前批次结果
     */
    private void validateFinalSequences(List<TcScheduleResultVo> validRowList,
                                        Map<Integer, TcScheduleResult> proposalMap,
                                        List<TcScheduleResult> currentResultList) {
        List<TcScheduleResult> finalResultList = this.buildFinalResultList(
                validRowList, proposalMap, currentResultList);
        for (TcScheduleResultVo row : validRowList) {
            TcScheduleResult proposal = proposalMap.get(row.getRowNum());
            for (int shiftOrder = 1; shiftOrder <= IMPORT_SHIFT_COUNT; shiftOrder++) {
                final int currentShiftOrder = shiftOrder;
                List<Integer> sequenceList = finalResultList.stream()
                        .filter(item -> Objects.equals(item.getMachineCode(), proposal.getMachineCode()))
                        .filter(item -> this.getBigDecimalField(item,
                                TcScheduleConstants.SHIFT_PLAN_QTY_FIELD_TEMPLATE, currentShiftOrder)
                                .compareTo(BigDecimal.ZERO) > 0)
                        .map(item -> this.getIntegerField(item,
                                TcScheduleConstants.SHIFT_SEQUENCE_FIELD_TEMPLATE, currentShiftOrder))
                        .collect(Collectors.toList());
                if (!this.isContinuousSequence(sequenceList)) {
                    row.getErrors().add(MessageFormat.format(I18nUtil.getMessage(
                            "ui.data.alert.tc.schedule.excel.sequenceConflict"),
                            row.getRowNum(), proposal.getMachineCode(), currentShiftOrder));
                }
            }
        }
    }

    /**
     * 构造应用提案后的当前批次结果快照。
     *
     * @param validRowList 有效行
     * @param proposalMap 提案映射
     * @param currentResultList 当前结果
     * @return 最终结果快照
     */
    private List<TcScheduleResult> buildFinalResultList(List<TcScheduleResultVo> validRowList,
                                                        Map<Integer, TcScheduleResult> proposalMap,
                                                        List<TcScheduleResult> currentResultList) {
        Map<Long, TcScheduleResult> updateProposalMap = validRowList.stream()
                .map(row -> proposalMap.get(row.getRowNum()))
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(TcScheduleResult::getId, Function.identity()));
        List<TcScheduleResult> finalResultList = currentResultList.stream()
                .map(item -> updateProposalMap.getOrDefault(item.getId(), item))
                .collect(Collectors.toCollection(ArrayList::new));
        validRowList.stream().map(row -> proposalMap.get(row.getRowNum()))
                .filter(item -> item.getId() == null).forEach(finalResultList::add);
        return finalResultList;
    }

    /**
     * 判断顺序是否为 1 到 N 的唯一连续序列。
     *
     * @param sequenceList 已排序顺序
     * @return true 表示唯一且连续
     */
    private boolean isContinuousSequence(List<Integer> sequenceList) {
        if (sequenceList.stream().anyMatch(Objects::isNull)) {
            return false;
        }
        List<Integer> sortedSequenceList = sequenceList.stream().sorted().collect(Collectors.toList());
        for (int index = 0; index < sortedSequenceList.size(); index++) {
            if (sortedSequenceList.get(index) != index + 1) {
                return false;
            }
        }
        return true;
    }

    /**
     * 在短事务中锁定当前批次并批量新增、更新。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @param batchNo 当前批次
     * @param persistRowList 待持久化行
     * @param proposalMap 行号对应提案
     * @return 成功行数
     * @throws ServiceException 二次版本校验、顺序校验或批量写入失败时抛出并回滚
     */
    private int persistImportRows(String factoryCode, Date scheduleDate, String batchNo,
                                  List<TcScheduleResultVo> persistRowList,
                                  Map<Integer, TcScheduleResult> proposalMap) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(this.platformTransactionManager);
        Integer successNum = transactionTemplate.execute(transactionStatus -> {
            List<TcScheduleResult> lockedResultList = this.emptyIfNull(
                    this.tcScheduleResultMapper.selectScopeForUpdate(factoryCode, scheduleDate, batchNo));
            Map<Long, TcScheduleResult> lockedResultMap = lockedResultList.stream()
                    .collect(Collectors.toMap(TcScheduleResult::getId, Function.identity()));
            List<TcScheduleResult> updateList = new ArrayList<>();
            List<TcScheduleResult> insertList = new ArrayList<>();
            for (TcScheduleResultVo row : persistRowList) {
                TcScheduleResult proposal = proposalMap.get(row.getRowNum());
                if (row.getResultId() == null) {
                    insertList.add(proposal);
                    continue;
                }
                TcScheduleResult locked = lockedResultMap.get(row.getResultId());
                long lockedVersion = locked == null || locked.getTaskVersion() == null
                        ? 0L : locked.getTaskVersion();
                if (locked == null || row.getTaskVersion() == null
                        || lockedVersion != row.getTaskVersion()
                        || !TcScheduleReleaseStatusEnum.NOT_RELEASED.getCode()
                        .equals(locked.getReleaseStatus())) {
                    throw new ServiceException(I18nUtil.getMessage(
                            "ui.data.alert.tc.schedule.excel.concurrentChanged"));
                }
                this.applyImportPlanFields(row, locked);
                locked.setTaskVersion(lockedVersion + 1L);
                locked.setBaseVale(locked.getId());
                updateList.add(locked);
            }
            List<TcScheduleResult> finalResultList = lockedResultList.stream()
                    .filter(item -> updateList.stream().noneMatch(update ->
                            Objects.equals(update.getId(), item.getId())))
                    .collect(Collectors.toCollection(ArrayList::new));
            finalResultList.addAll(updateList);
            finalResultList.addAll(insertList);
            this.assertFinalSequences(finalResultList, persistRowList, proposalMap);
            if (CollUtil.isNotEmpty(updateList)) {
                Integer updatedRows = this.baseDao.updateBatch(updateList);
                if (updatedRows == null || updatedRows != updateList.size()) {
                    throw new ServiceException(I18nUtil.getMessage(
                            "ui.data.alert.tc.schedule.excel.persistFailed"));
                }
            }
            if (CollUtil.isNotEmpty(insertList)) {
                Integer insertedRows = this.baseDao.saveBatch(insertList);
                if (insertedRows == null || insertedRows != insertList.size()) {
                    throw new ServiceException(I18nUtil.getMessage(
                            "ui.data.alert.tc.schedule.excel.persistFailed"));
                }
            }
            return updateList.size() + insertList.size();
        });
        if (successNum == null) {
            throw new ServiceException(I18nUtil.getMessage(
                    "ui.data.alert.tc.schedule.excel.persistFailed"));
        }
        return successNum;
    }

    /**
     * 在事务内再次校验受影响机台顺序。
     *
     * @param finalResultList 最终结果
     * @param persistRowList 持久化行
     * @param proposalMap 提案映射
     * @throws ServiceException 最终顺序不唯一或不连续时抛出
     */
    private void assertFinalSequences(List<TcScheduleResult> finalResultList,
                                      List<TcScheduleResultVo> persistRowList,
                                      Map<Integer, TcScheduleResult> proposalMap) {
        Set<String> touchedMachineCodeSet = persistRowList.stream()
                .map(row -> proposalMap.get(row.getRowNum()).getMachineCode())
                .filter(StrUtil::isNotBlank).collect(Collectors.toSet());
        for (String machineCode : touchedMachineCodeSet) {
            for (int shiftOrder = 1; shiftOrder <= IMPORT_SHIFT_COUNT; shiftOrder++) {
                final int currentShiftOrder = shiftOrder;
                List<Integer> sequenceList = finalResultList.stream()
                        .filter(item -> Objects.equals(machineCode, item.getMachineCode()))
                        .filter(item -> this.getBigDecimalField(item,
                                TcScheduleConstants.SHIFT_PLAN_QTY_FIELD_TEMPLATE, currentShiftOrder)
                                .compareTo(BigDecimal.ZERO) > 0)
                        .map(item -> this.getIntegerField(item,
                                TcScheduleConstants.SHIFT_SEQUENCE_FIELD_TEMPLATE, currentShiftOrder))
                        .collect(Collectors.toList());
                if (!this.isContinuousSequence(sequenceList)) {
                    throw new ServiceException(I18nUtil.getMessage(
                            "ui.data.alert.tc.schedule.excel.concurrentChanged"));
                }
            }
        }
    }

    /**
     * 查询有效机台编码。
     *
     * @param factoryCode 工厂编码
     * @param machineCodeSet 导入机台编码
     * @return 有效机台编码
     */
    private Set<String> loadValidMachineCodes(String factoryCode, Set<String> machineCodeSet) {
        if (machineCodeSet.isEmpty()) {
            return Collections.emptySet();
        }
        LambdaQueryWrapper<TcMachineInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TcMachineInfo::getFactoryCode, factoryCode);
        wrapper.eq(TcMachineInfo::getMachineStatus, TcYesNoEnum.YES.getCode());
        wrapper.in(TcMachineInfo::getMachineCode, machineCodeSet);
        return this.emptyIfNull(this.tcMachineInfoMapper.selectList(wrapper)).stream()
                .map(TcMachineInfo::getMachineCode).filter(StrUtil::isNotBlank)
                .collect(Collectors.toSet());
    }

    /**
     * 查询当前批次结果。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @param batchNo 当前批次
     * @return 当前结果
     */
    private List<TcScheduleResult> loadCurrentBatchResults(String factoryCode, Date scheduleDate,
                                                           String batchNo) {
        LambdaQueryWrapper<TcScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TcScheduleResult::getFactoryCode, factoryCode);
        wrapper.eq(TcScheduleResult::getScheduleDate, scheduleDate);
        wrapper.eq(TcScheduleResult::getBatchNo, batchNo);
        wrapper.orderByAsc(TcScheduleResult::getId);
        return this.emptyIfNull(this.tcScheduleResultMapper.selectList(wrapper));
    }

    /**
     * 按导入隐藏主键批量查询结果。
     *
     * @param rowList 导入行
     * @return 主键对应结果
     */
    private Map<Long, TcScheduleResult> loadRequestedResults(List<TcScheduleResultVo> rowList) {
        List<Long> resultIdList = rowList.stream().map(TcScheduleResultVo::getResultId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (resultIdList.isEmpty()) {
            return Collections.emptyMap();
        }
        return this.emptyIfNull(this.tcScheduleResultMapper.selectBatchIds(resultIdList)).stream()
                .collect(Collectors.toMap(TcScheduleResult::getId, Function.identity()));
    }

    /**
     * 解析当前有效批次；无结果和未排任务时使用人工日期批次。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @return 当前批次
     */
    private String resolveCurrentBatchNo(String factoryCode, Date scheduleDate) {
        LambdaQueryWrapper<TcScheduleResult> resultWrapper = new LambdaQueryWrapper<>();
        resultWrapper.select(TcScheduleResult::getBatchNo);
        resultWrapper.eq(TcScheduleResult::getFactoryCode, factoryCode);
        resultWrapper.eq(TcScheduleResult::getScheduleDate, scheduleDate);
        List<String> batchNoList = this.emptyIfNull(
                this.tcScheduleResultMapper.selectList(resultWrapper)).stream()
                .map(TcScheduleResult::getBatchNo).filter(StrUtil::isNotBlank)
                .collect(Collectors.toCollection(ArrayList::new));

        LambdaQueryWrapper<TcScheduleUnplanned> unplannedWrapper = new LambdaQueryWrapper<>();
        unplannedWrapper.select(TcScheduleUnplanned::getBatchNo);
        unplannedWrapper.eq(TcScheduleUnplanned::getFactoryCode, factoryCode);
        unplannedWrapper.eq(TcScheduleUnplanned::getScheduleDate, scheduleDate);
        batchNoList.addAll(this.emptyIfNull(
                this.tcScheduleUnplannedMapper.selectList(unplannedWrapper)).stream()
                .map(TcScheduleUnplanned::getBatchNo).filter(StrUtil::isNotBlank)
                .collect(Collectors.toList()));
        return batchNoList.stream().max(String::compareTo)
                .orElseGet(() -> "TCMANUAL" + DateUtil.format(scheduleDate, "yyyyMMdd"));
    }

    /**
     * 保存导出日志。
     *
     * @param queryVO 导出条件
     * @param fileName 文件名称
     * @param rowCount 行数
     * @param beginTime 开始时间
     */
    private void saveExportLog(TcScheduleResult queryVO, String fileName,
                               int rowCount, Date beginTime) {
        Date endTime = DateUtils.getNowDate();
        ExportLog exportLog = new ExportLog();
        exportLog.setProcedureCode("0");
        exportLog.setFunctionCode("tcScheduleResult");
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
     * 构造整批阻断响应。
     *
     * @param message 阻断消息
     * @param importLogId 导入日志 ID
     * @param rowNum Excel 行号，无法定位时为 0
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
     * @param row 导入行
     * @return 行错误日志
     */
    private ImportErrorLog buildRowError(Long importLogId, TcScheduleResultVo row) {
        return new ImportErrorLog(importLogId, row.getRowNum(), String.join(";", row.getErrors()));
    }

    /**
     * 构造包含成功数和失败明细的导入响应。
     *
     * @param successNum 成功数
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
     * @param row Excel 行
     * @param columnIndex 列索引
     * @param dataFormatter 格式化器
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
     * 获取 BigDecimal 动态字段。
     *
     * @param source 字段来源对象
     * @param fieldTemplate 字段名模板
     * @param shiftOrder 班次序号
     * @return 数值，空值返回 0
     */
    private BigDecimal getBigDecimalField(Object source, String fieldTemplate, int shiftOrder) {
        if (source == null) {
            return BigDecimal.ZERO;
        }
        Object value;
        if (source instanceof TcScheduleResult) {
            value = ((TcScheduleResult) source).getFieldValueByFieldName(
                    String.format(fieldTemplate, shiftOrder));
        } else {
            value = ((TcScheduleResultVo) source).getFieldValueByFieldName(
                    String.format(fieldTemplate, shiftOrder));
        }
        return BigDecimalUtils.valueOf(value);
    }

    /**
     * 获取 Integer 动态字段。
     *
     * @param source 字段来源对象
     * @param fieldTemplate 字段名模板
     * @param shiftOrder 班次序号
     * @return 顺序值
     */
    private Integer getIntegerField(Object source, String fieldTemplate, int shiftOrder) {
        if (source == null) {
            return null;
        }
        Object value;
        if (source instanceof TcScheduleResult) {
            value = ((TcScheduleResult) source).getFieldValueByFieldName(
                    String.format(fieldTemplate, shiftOrder));
        } else {
            value = ((TcScheduleResultVo) source).getFieldValueByFieldName(
                    String.format(fieldTemplate, shiftOrder));
        }
        return value instanceof Number ? ((Number) value).intValue() : null;
    }

    /**
     * 构造机台、胎侧和施工版本键。
     *
     * @param result 排程结果
     * @return 版本业务键
     */
    private String buildResultVersionKey(TcScheduleResult result) {
        return StrUtil.blankToDefault(result.getMachineCode(), "").trim() + "|"
                + this.buildSidewallVersionKey(result.getSidewallCode(), result.getConstructionVersion());
    }

    /**
     * 构造胎侧编码和施工版本键。
     *
     * @param sidewallCode 胎侧编码
     * @param constructionVersion 施工版本
     * @return 版本业务键
     */
    private String buildSidewallVersionKey(String sidewallCode, String constructionVersion) {
        return StrUtil.blankToDefault(sidewallCode, "").trim() + "|"
                + StrUtil.blankToDefault(constructionVersion, "").trim();
    }

    /**
     * 构造施工编码和版本键。
     *
     * @param constructionCode 施工编码
     * @param constructionVersion 施工版本
     * @return 施工关联键
     */
    private String buildConstructionKey(String constructionCode, String constructionVersion) {
        return StrUtil.blankToDefault(constructionCode, "").trim() + "|"
                + StrUtil.blankToDefault(constructionVersion, "").trim();
    }

    /**
     * 判断导入行是否至少一班计划量为正。
     *
     * @param row 导入行
     * @return true 表示至少一班为正
     */
    private boolean hasPositivePlanQty(TcScheduleResultVo row) {
        for (int shiftOrder = 1; shiftOrder <= IMPORT_SHIFT_COUNT; shiftOrder++) {
            if (this.getBigDecimalField(row, TcScheduleConstants.SHIFT_PLAN_QTY_FIELD_TEMPLATE,
                    shiftOrder).compareTo(BigDecimal.ZERO) > 0) {
                return true;
            }
        }
        return false;
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
     * 向集合添加非空文本。
     *
     * @param target 目标集合
     * @param value 待添加文本
     */
    private void addNotBlank(Set<String> target, String value) {
        if (StrUtil.isNotBlank(value)) {
            target.add(value.trim());
        }
    }

    /**
     * 格式化只包含行号的多语言消息。
     *
     * @param messageKey 多语言键
     * @param rowNum Excel 行号
     * @return 格式化消息
     */
    private String formatRowMessage(String messageKey, int rowNum) {
        return MessageFormat.format(I18nUtil.getMessage(messageKey), rowNum);
    }

    /**
     * 获取或创建 Excel 行。
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
     * 获取或创建 Excel 单元格。
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
     * 将空列表标准化为空集合。
     *
     * @param list 原列表
     * @param <T> 元素类型
     * @return 非空列表
     */
    private <T> List<T> emptyIfNull(List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }
}
