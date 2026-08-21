package com.zlt.aps.tm.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
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
import com.zlt.aps.tm.api.constant.TmScheduleConstants;
import com.zlt.aps.tm.api.domain.dto.TmScheduleResultImportDTO;
import com.zlt.aps.tm.api.domain.entity.TmMachineInfo;
import com.zlt.aps.tm.api.domain.entity.TmParams;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import com.zlt.aps.tm.api.enums.TmScheduleReleaseStatusEnum;
import com.zlt.aps.tm.api.enums.TmVersionMatchModeEnum;
import com.zlt.aps.tm.api.enums.TmYesNoEnum;
import com.zlt.aps.tm.component.TmScheduleBatchNoGenerator;
import com.zlt.aps.tm.domain.vo.*;
import com.zlt.aps.tm.mapper.TmAutoScheduleDataLoadMapper;
import com.zlt.aps.tm.mapper.TmMachineInfoMapper;
import com.zlt.aps.tm.mapper.TmParamsMapper;
import com.zlt.aps.tm.mapper.TmScheduleResultMapper;
import com.zlt.aps.tm.service.ITmScheduleResultExcelService;
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
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 胎面排程结果模板导入导出服务实现。
 *
 * <p>参照 {@code LhMouldChangePlanController} 的模板导入导出：导出时将 {@link TmScheduleResultVo}
 * 的 {@link Excel#name()} 国际化回写值写入模板第 1 行（隐藏元数据行）的 {@code {fieldName}} 占位符；
 * 导入时按第 1 行国际化表头匹配列号，逐行解析为 {@code List<TmScheduleResultVo>}。</p>
 */
@Slf4j
@Service
public class TmScheduleResultExcelServiceImpl implements ITmScheduleResultExcelService {

    /** 模板资源路径。 */
    private static final String TEMPLATE_RESOURCE = "excelModel/tmScheduleResult.xlsx";

    /** 导出工作表名称（导入按此名称匹配工作表）。 */
    private static final String SHEET_NAME = "胎面 TD Mặt lốp";

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

    @Resource
    private TmScheduleResultMapper tmScheduleResultMapper;

    /** 通用批量写入服务。 */
    @Resource
    private BaseDao baseDao;

    @Resource
    private TmMachineInfoMapper tmMachineInfoMapper;

    @Resource
    private TmAutoScheduleDataLoadMapper tmAutoScheduleDataLoadMapper;

    @Resource
    private TmParamsMapper tmParamsMapper;

    @Resource
    private TmScheduleBatchNoGenerator tmScheduleBatchNoGenerator;

    @Resource
    private PlatformTransactionManager platformTransactionManager;

    @Resource
    private IExportLogService iExportLogService;

    @Resource
    private IImportLogService iImportLogService;

    @Resource
    private IImportErrorLogService iImportErrorLogService;

    /**
     * 获取表头与标题占位符 map。
     *
     * @param queryVO 参数
     * @return 标题、4 个班次组标题占位符
     */
    private static Map<String, Object> getTitleMap(TmScheduleResult queryVO) {
        Map<String, Object> tableMap = new HashMap<>();
        String formatDate = DateUtil.format(queryVO.getScheduleDate(), TITLE_DATE_FORMAT);
        String title = "{0}全钢压出工程生产计划单 Đơn kế hoạch sản xuất của công đoạn ép đùn toàn thép";
        tableMap.put("title", MessageFormat.format(title, formatDate));

        // 4 个班次组标题写入对应合并起始单元格 H3/K3/N3/Q3（用户可见的班次+日期）：
        //   标题显示日期：H:J 早班(lastDayTitle)、K:M 中班(midTitle) 显示前一日(D-1)；
        //                 N:P 夜班(nightTitle)、Q:S 早班(dayTitle) 显示当日(D)。
        //   取数来源（见 buildExportDataList）：H:J = 前一日同机台同胎面 CLASS3；
        //                 K:M = 当日 CLASS1；N:P = 当日 CLASS2；Q:S = 当日 CLASS3。
        //   注意：按业务需要 K:M 中班标题显示前一日，但取数来源为当日 CLASS1，二者日期不同。
        String previousDay = DateUtil.format(DateUtils.addDays(queryVO.getScheduleDate(), -1), "MM/dd");
        String currentDay = DateUtil.format(queryVO.getScheduleDate(), "MM/dd");
        tableMap.put("lastDayTitle", MessageFormat.format("早班{0}\nCa sáng {0}", previousDay));
        tableMap.put("midTitle", MessageFormat.format("中班{0}\nCa chiều {0}", previousDay));
        tableMap.put("nightTitle", MessageFormat.format("夜班{0}\nCa đêm {0}", currentDay));
        tableMap.put("dayTitle", MessageFormat.format("早班{0}\nCa sáng {0}", currentDay));
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
     * 按专用模板导出胎面排程结果。
     *
     * @param queryVO 查询条件，必须包含工厂和排程日期
     * @param fileName 导出文件名称
     * @return Excel 文件字节
     * @throws ServiceException 查询条件不完整、模板不存在或文件生成失败时抛出
     */
    @Override
    public byte[] exportDataScheduleResult(TmScheduleResult queryVO, String fileName) {
        this.validateExportQuery(queryVO);
        Date beginTime = DateUtils.getNowDate();
        List<TmScheduleResult> resultList = Boolean.TRUE.equals(queryVO.getExportTemplate())
                ? Collections.emptyList() : this.listExportResults(queryVO);
        List<Map<String, Object>> dataList = this.buildExportDataList(resultList, queryVO);
        List<List<Map<String, Object>>> excelDataList = new ArrayList<>();
        if (CollUtil.isNotEmpty(dataList)) {
            excelDataList.add(dataList);
        }

        byte[] resultBytes;
        try (InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(TEMPLATE_RESOURCE)) {
            if (inputStream == null) {
                throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.excel.templateMissing"));
            }
            Map<String, Object> tableMap = this.getTitleMap(queryVO);
            // 回写第 1 行 @Excel 国际化字段名，作为导入列匹配表头
            this.setExportTitleFieldName(tableMap);
            resultBytes = ExcelUtils.writeMultiList(inputStream, 0, tableMap, excelDataList);
        } catch (IOException exception) {
            log.error("生成胎面排程结果模板失败", exception);
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.excel.generateFailed"));
        }
        resultBytes = this.finishExportWorkbook(resultBytes, dataList.isEmpty());
        this.saveExportLog(queryVO, fileName, resultList.size(), beginTime);
        return resultBytes;
    }

    /**
     * 按专用模板导入胎面排程结果。
     *
     * @param importDTO 导入文件和业务条件
     * @param updateSupport 已存在记录是否更新
     * @return 导入结果和错误明细
     * @throws Exception 导入日志或文件读取异常时抛出
     */
    @Override
    public AjaxResult importDataScheduleResult(TmScheduleResultImportDTO importDTO, boolean updateSupport) throws Exception {
        ImportContext importContext = this.validateImportContext(importDTO);
        Date beginTime = DateUtils.getNowDate();
        ImportLog importLog = ImportExcelUtils.getImportLogAndUploadFile(importContext.getFileBytes(),
                importContext.getImportFilePath(), importContext.getProcedureCode(), importContext.getFunctionName(),
                importContext.getOriFileName(), 1);
        importLog = this.iImportLogService.add(importLog);
        AjaxResult ajaxResult;
        int rowCount = 0;
        try {
            TmScheduleResultExcelParseResult parseResult = this.parseWorkbook(importContext.getFileBytes(),
                    importDTO.getScheduleResult());
            rowCount = parseResult.getRowList().size();
            ajaxResult = this.doImport(parseResult, importDTO.getScheduleResult(), updateSupport, importLog.getId());
        } catch (ServiceException exception) {
            log.warn("胎面排程结果模板导入校验失败，原因={}", exception.getMessage());
            ajaxResult = AjaxResult.error(exception.getMessage());
        } catch (RuntimeException exception) {
            log.error("胎面排程结果模板导入失败", exception);
            ajaxResult = AjaxResult.error(I18nUtil.getMessage("ui.data.alert.tm.schedule.excel.importFailed"));
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
    private void validateExportQuery(TmScheduleResult queryVO) {
        if (queryVO == null || StrUtil.isBlank(queryVO.getFactoryCode())) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.excel.factoryRequired"));
        }
        if (queryVO.getScheduleDate() == null) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.excel.dateRequired"));
        }
    }

    /**
     * 校验导入文件和业务上下文。
     *
     * @param importDTO 导入请求
     * @return 导入文件上下文
     * @throws ServiceException 文件、工厂或模板日期为空时抛出
     */
    private ImportContext validateImportContext(TmScheduleResultImportDTO importDTO) {
        if (importDTO == null || importDTO.getImportContext() == null
                || importDTO.getImportContext().getFileBytes() == null
                || importDTO.getImportContext().getFileBytes().length == 0) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.excel.fileRequired"));
        }
        String originalFileName = StrUtil.trim(importDTO.getImportContext().getOriFileName());
        if (StrUtil.isNotBlank(originalFileName)
                && !originalFileName.toLowerCase(Locale.ROOT).endsWith(ExcelUtil.XLSX_FILE)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.excel.fileTypeInvalid"));
        }
        if (importDTO.getScheduleResult() == null
                || StrUtil.isBlank(importDTO.getScheduleResult().getFactoryCode())) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.excel.factoryRequired"));
        }
        if (importDTO.getScheduleResult().getScheduleDate() == null) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.excel.dateRequired"));
        }
        return importDTO.getImportContext();
    }

    /**
     * 查询导出明细。
     *
     * @param queryVO 查询条件
     * @return 每条排程结果对应一条导出明细
     */
    private List<TmScheduleResult> listExportResults(TmScheduleResult queryVO) {
        LambdaQueryWrapper<TmScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TmScheduleResult::getFactoryCode, queryVO.getFactoryCode());
        wrapper.eq(TmScheduleResult::getScheduleDate, DateUtil.beginOfDay(queryVO.getScheduleDate()));
        wrapper.eq(StrUtil.isNotBlank(queryVO.getBatchNo()), TmScheduleResult::getBatchNo, queryVO.getBatchNo());
        wrapper.like(StrUtil.isNotBlank(queryVO.getOrderNo()), TmScheduleResult::getOrderNo, queryVO.getOrderNo());
        wrapper.eq(StrUtil.isNotBlank(queryVO.getMachineCode()), TmScheduleResult::getMachineCode,
                queryVO.getMachineCode());
        wrapper.like(StrUtil.isNotBlank(queryVO.getTreadCode()), TmScheduleResult::getTreadCode,
                queryVO.getTreadCode());
        wrapper.eq(StrUtil.isNotBlank(queryVO.getGlueCode()), TmScheduleResult::getGlueCode, queryVO.getGlueCode());
        wrapper.eq(StrUtil.isNotBlank(queryVO.getReleaseStatus()), TmScheduleResult::getReleaseStatus,
                queryVO.getReleaseStatus());
        wrapper.eq(StrUtil.isNotBlank(queryVO.getDataSource()), TmScheduleResult::getDataSource,
                queryVO.getDataSource());
        wrapper.eq(StrUtil.isNotBlank(queryVO.getTailFlag()), TmScheduleResult::getTailFlag, queryVO.getTailFlag());
        wrapper.orderByAsc(TmScheduleResult::getMachineCode, TmScheduleResult::getClass1Sequence,
                TmScheduleResult::getClass2Sequence, TmScheduleResult::getClass3Sequence,
                TmScheduleResult::getTreadCode, TmScheduleResult::getId);
        return this.tmScheduleResultMapper.selectList(wrapper);
    }

    /**
     * 将 {@link TmScheduleResultVo} 各 {@link Excel} 字段的国际化名称写入表头占位符 map，
     * 供模板第 1 行 {@code {fieldName}} 占位符回写，作为导入列匹配表头。
     *
     * @param tableMap 表头占位符 map
     */
    private void setExportTitleFieldName(Map<String, Object> tableMap) {
        for (Field field : TmScheduleResultVo.class.getDeclaredFields()) {
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
     * @param resultList 排程结果
     * @param queryVO 查询条件
     * @return 模板明细映射
     */
    private List<Map<String, Object>> buildExportDataList(List<TmScheduleResult> resultList,
                                                           TmScheduleResult queryVO) {
        if (CollUtil.isEmpty(resultList)) {
            return Collections.emptyList();
        }
        Map<String, TmScheduleResult> previousResultMap = this.buildPreviousResultMap(queryVO);
        Map<String, TmScheduleResultFormingDataVo> formingDataMap = this.buildFormingDataMap(
                queryVO.getFactoryCode(), DateUtil.beginOfDay(queryVO.getScheduleDate()));
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (TmScheduleResult result : resultList) {
            Map<String, Object> rowMap = new LinkedHashMap<>();
            TmScheduleResult previousResult = previousResultMap.get(this.buildResultBusinessKey(result));
            // 班次组取数来源（标题显示日期见 getTitleMap；K:M 中班标题显示前一日，但此处取当日 CLASS1）：
            //   H:J = 前一日同机台同胎面 CLASS3 计划量/完成量/顺序
            //   K:M = 当日 CLASS1、N:P = 当日 CLASS2、Q:S = 当日 CLASS3
            rowMap.put("machineCode", result.getMachineCode());
            rowMap.put("treadLength", result.getTreadShoulderLength());
            rowMap.put("cxRemainQty", result.getCxRemainQty());
            rowMap.put("treadCode", result.getTreadCode());
            rowMap.put("materialDesc", result.getMaterialDesc());
            rowMap.put("wholeGlueCode", this.buildExportGlueCode(result));
            rowMap.put("stockQty", this.blankIfZero(result.getSixClockStockQty()));
            rowMap.put("lastDayPlanQty", this.blankIfZero(previousResult == null ? null : previousResult.getClass3PlanQty()));
            rowMap.put("lastDayFinishQty", this.blankIfZero(previousResult == null ? null : previousResult.getClass3FinishQty()));
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
            TmScheduleResultFormingDataVo formingData = formingDataMap.get(result.getTreadCode());
            rowMap.put("cxPlanQty", formingData == null ? BigDecimal.ZERO : formingData.getCxPlanQty());
            rowMap.put("curlRollLength", result.getCurlRollLength());
            rowMap.put("cxMachineCode", formingData == null
                    ? result.getCxMachineCode() : formingData.getCxMachineCode());
            rowMap.put("type", TmYesNoEnum.YES.getCode().equals(result.getTailFlag()) ? "收尾" : "");
            dataList.add(rowMap);
        }
        return dataList;
    }

    /**
     * 组合排程结果的主胶料和基部胶编码，供专用模板胶种列导出使用。
     *
     * @param result 排程结果
     * @return 使用英文逗号分隔的有效胶料编码；无有效编码时返回空字符串
     */
    private String buildExportGlueCode(TmScheduleResult result) {
        return Arrays.asList(result.getGlueCode(), result.getBaseGlueCode()).stream()
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.joining(","));
    }

    /**
     * 查询前一排程日早班数据。
     *
     * @param queryVO 当前导出条件
     * @return 机台和胎面业务键对应的前日排程结果
     */
    private Map<String, TmScheduleResult> buildPreviousResultMap(TmScheduleResult queryVO) {
        Date previousDate = DateUtil.beginOfDay(DateUtil.offsetDay(queryVO.getScheduleDate(), -1));
        LambdaQueryWrapper<TmScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TmScheduleResult::getFactoryCode, queryVO.getFactoryCode());
        wrapper.eq(TmScheduleResult::getScheduleDate, previousDate);
        wrapper.orderByDesc(TmScheduleResult::getId);
        List<TmScheduleResult> previousList = this.tmScheduleResultMapper.selectList(wrapper);
        return previousList.stream()
                .filter(item -> StrUtil.isNotBlank(item.getMachineCode()) && StrUtil.isNotBlank(item.getTreadCode()))
                .collect(Collectors.toMap(this::buildResultBusinessKey, Function.identity(),
                        (first, ignored) -> first, LinkedHashMap::new));
    }

    /**
     * 按当前版本匹配模式汇总成型 CLASS1~4 计划量和成型机台。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @return 胎面编码对应的成型计划量和成型机台
     */
    private Map<String, TmScheduleResultFormingDataVo> buildFormingDataMap(String factoryCode, Date scheduleDate) {
        TmVersionMatchModeEnum mode = this.resolveVersionMatchMode(factoryCode);
        if (TmVersionMatchModeEnum.BOM == mode) {
            return this.buildBomFormingDataMap(factoryCode, scheduleDate);
        }
        return this.buildRecipeFormingDataMap(factoryCode, scheduleDate);
    }

    /**
     * 按 BOM 模式汇总成型计划量。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @return 胎面编码对应的成型计划量和成型机台
     */
    private Map<String, TmScheduleResultFormingDataVo> buildBomFormingDataMap(String factoryCode,
                                                                                Date scheduleDate) {
        List<TmFormingDemandRowVo> rowList = this.tmAutoScheduleDataLoadMapper
                .selectFormingDemandRows(factoryCode, scheduleDate);
        Map<String, TmScheduleResultFormingDataVo> resultMap = new LinkedHashMap<>();
        for (TmFormingDemandRowVo row : this.emptyIfNull(rowList)) {
            if (row == null || StrUtil.isBlank(row.getTreadCode())) {
                continue;
            }
            BigDecimal planQty = BigDecimalUtils.add(row.getClass1PlanQty(), row.getClass2PlanQty(),
                    row.getClass3PlanQty(), row.getClass4PlanQty());
            this.mergeFormingData(resultMap, row.getTreadCode(), planQty, row.getCxMachineCode());
        }
        return resultMap;
    }

    /**
     * 按 RECIPE 模式逐班解析胎面并汇总成型计划量。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @return 胎面编码对应的成型计划量和成型机台
     */
    private Map<String, TmScheduleResultFormingDataVo> buildRecipeFormingDataMap(String factoryCode,
                                                                                   Date scheduleDate) {
        List<TmFormingDemandRecipeRowVo> rowList = this.tmAutoScheduleDataLoadMapper
                .selectFormingDemandRowsByRecipe(factoryCode, scheduleDate);
        if (CollUtil.isEmpty(rowList)) {
            return Collections.emptyMap();
        }
        Set<String> embryoCodeSet = rowList.stream().map(TmFormingDemandRecipeRowVo::getEmbryoCode)
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
        List<TmConstructionTreadRowVo> constructionList = this.tmAutoScheduleDataLoadMapper
                .selectConstructionInfoRows(factoryCode, embryoCodeSet, recipeNoSet);
        Map<String, TmConstructionTreadRowVo> constructionMap = this.emptyIfNull(constructionList).stream()
                .filter(Objects::nonNull)
                .filter(item -> StrUtil.isNotBlank(item.getConstructionCode())
                        && StrUtil.isNotBlank(item.getConstructionVersion()))
                .collect(Collectors.toMap(item -> this.buildConstructionKey(item.getConstructionCode(),
                                item.getConstructionVersion()), Function.identity(),
                        (first, ignored) -> first, LinkedHashMap::new));
        Map<String, TmScheduleResultFormingDataVo> resultMap = new LinkedHashMap<>();
        for (TmFormingDemandRecipeRowVo row : rowList) {
            this.mergeRecipePlanQty(resultMap, constructionMap, row.getEmbryoCode(),
                    row.getClass1RecipeNo(), row.getClass1PlanQty(), row.getCxMachineCode());
            this.mergeRecipePlanQty(resultMap, constructionMap, row.getEmbryoCode(),
                    row.getClass2RecipeNo(), row.getClass2PlanQty(), row.getCxMachineCode());
            this.mergeRecipePlanQty(resultMap, constructionMap, row.getEmbryoCode(),
                    row.getClass3RecipeNo(), row.getClass3PlanQty(), row.getCxMachineCode());
            this.mergeRecipePlanQty(resultMap, constructionMap, row.getEmbryoCode(),
                    row.getClass4RecipeNo(), row.getClass4PlanQty(), row.getCxMachineCode());
        }
        return resultMap;
    }

    /**
     * 解析工厂的施工版本匹配模式。
     *
     * @param factoryCode 工厂编码
     * @return 版本匹配模式，未配置时返回 RECIPE
     */
    private TmVersionMatchModeEnum resolveVersionMatchMode(String factoryCode) {
        LambdaQueryWrapper<TmParams> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TmParams::getFactoryCode, factoryCode);
        wrapper.eq(TmParams::getParamCode, TmScheduleConstants.PARAM_VERSION_MATCH_MODE);
        wrapper.eq(TmParams::getEnableStatus, TmYesNoEnum.YES.getCode());
        List<TmParams> paramsList = this.tmParamsMapper.selectList(wrapper);
        String mode = this.emptyIfNull(paramsList).stream().filter(Objects::nonNull)
                .map(TmParams::getParamValue).filter(StrUtil::isNotBlank).findFirst()
                .orElse(TmScheduleConstants.DEFAULT_VERSION_MATCH_MODE);
        return TmVersionMatchModeEnum.resolve(mode);
    }

    /**
     * 将逐班示方书计划量归入对应胎面。
     *
     * @param resultMap 胎面计划量汇总
     * @param constructionMap 施工版本映射
     * @param embryoCode 胎胚编码
     * @param recipeNo 示方书编号
     * @param planQty 班次计划量
     * @param cxMachineCode 成型机台编码
     */
    private void mergeRecipePlanQty(Map<String, TmScheduleResultFormingDataVo> resultMap,
                                    Map<String, TmConstructionTreadRowVo> constructionMap,
                                    String embryoCode, String recipeNo, BigDecimal planQty,
                                    String cxMachineCode) {
        if (StrUtil.isBlank(embryoCode) || StrUtil.isBlank(recipeNo)) {
            return;
        }
        TmConstructionTreadRowVo construction = constructionMap.get(this.buildConstructionKey(embryoCode, recipeNo));
        if (construction == null || StrUtil.isBlank(construction.getTreadCode())) {
            return;
        }
        this.mergeFormingData(resultMap, construction.getTreadCode(), planQty, cxMachineCode);
    }

    /**
     * 汇总成型计划量并追加去重后的成型机台。
     *
     * @param resultMap 胎面编码对应的成型数据
     * @param treadCode 胎面编码
     * @param planQty 成型计划量
     * @param cxMachineCode 成型机台编码，可包含逗号分隔的多个机台
     */
    private void mergeFormingData(Map<String, TmScheduleResultFormingDataVo> resultMap,
                                  String treadCode, BigDecimal planQty, String cxMachineCode) {
        TmScheduleResultFormingDataVo target = resultMap.computeIfAbsent(treadCode, ignored -> {
            TmScheduleResultFormingDataVo dataVo = new TmScheduleResultFormingDataVo();
            dataVo.setCxPlanQty(BigDecimal.ZERO);
            return dataVo;
        });
        target.setCxPlanQty(BigDecimalUtils.add(target.getCxPlanQty(), planQty));
        this.mergeDistinctMachineCodes(target, cxMachineCode);
    }

    /**
     * 将成型机台拆分、清理并按首次出现顺序去重后写回汇总对象。
     *
     * @param target 成型数据汇总对象
     * @param cxMachineCode 成型机台编码，可包含逗号分隔的多个机台
     */
    private void mergeDistinctMachineCodes(TmScheduleResultFormingDataVo target, String cxMachineCode) {
        if (target == null || StrUtil.isBlank(cxMachineCode)) {
            return;
        }
        Set<String> machineCodeSet = new LinkedHashSet<>();
        this.addMachineCodes(machineCodeSet, target.getCxMachineCode());
        this.addMachineCodes(machineCodeSet, cxMachineCode);
        if (!machineCodeSet.isEmpty()) {
            target.setCxMachineCode(String.join(",", machineCodeSet));
        }
    }

    /**
     * 将逗号分隔的成型机台编码加入目标集合。
     *
     * @param machineCodeSet 成型机台编码集合
     * @param machineCodes 成型机台编码文本
     */
    private void addMachineCodes(Set<String> machineCodeSet, String machineCodes) {
        if (machineCodeSet == null || StrUtil.isBlank(machineCodes)) {
            return;
        }
        Arrays.stream(machineCodes.split(","))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .forEach(machineCodeSet::add);
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
            log.error("处理胎面排程结果导出工作簿失败", exception);
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.excel.generateFailed"));
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
    private TmScheduleResultExcelParseResult parseWorkbook(byte[] fileBytes, TmScheduleResult condition) {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(fileBytes))) {
            Sheet sheet = workbook.getSheet(SHEET_NAME);
            if (sheet == null || sheet.getRow(HEADER_ROW_INDEX) == null) {
                throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.excel.templateInvalid"));
            }
            DataFormatter dataFormatter = new DataFormatter();
            FormulaEvaluator formulaEvaluator = workbook.getCreationHelper().createFormulaEvaluator();
            String title = this.readCellText(sheet.getRow(TITLE_ROW_INDEX), 0, dataFormatter, formulaEvaluator);
            Date scheduleDate = this.parseScheduleDate(title);
            if (!DateUtil.isSameDay(scheduleDate, condition.getScheduleDate())) {
                throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.excel.dateMismatch"));
            }
            List<TmScheduleResultVo> rowList = this.parseVoList(sheet, dataFormatter, formulaEvaluator);
            if (rowList.isEmpty()) {
                throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.excel.noData"));
            }
            TmScheduleResultExcelParseResult parseResult = new TmScheduleResultExcelParseResult();
            parseResult.setScheduleDate(DateUtil.beginOfDay(scheduleDate));
            parseResult.setRowList(rowList);
            return parseResult;
        } catch (ServiceException exception) {
            throw exception;
        } catch (Exception exception) {
            log.warn("解析胎面排程结果模板失败", exception);
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.excel.templateInvalid"));
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
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.excel.titleInvalid"));
        }
        try {
            return DateUtil.parse(matcher.group(1), TITLE_DATE_FORMAT);
        } catch (RuntimeException exception) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.excel.titleInvalid"));
        }
    }

    /**
     * 按第 1 行国际化表头匹配列号，将数据行解析为 {@link TmScheduleResultVo} 列表。
     *
     * <p>表头文本与 {@link Excel#name()} 的 {@link I18nUtil#getMessage(String)} 回写值同口径匹配，
     * 列位置变更不影响解析；类型转换按字段类型显式处理（String/BigDecimal/Integer）。</p>
     *
     * @param sheet 模板工作表
     * @param dataFormatter 单元格格式化器
     * @param formulaEvaluator 公式计算器
     * @return 明细行对象列表
     */
    private List<TmScheduleResultVo> parseVoList(Sheet sheet, DataFormatter dataFormatter,
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
        for (Field field : TmScheduleResultVo.class.getDeclaredFields()) {
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
        List<TmScheduleResultVo> rowList = new ArrayList<>();
        for (int rowIndex = DATA_START_ROW_INDEX; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            TmScheduleResultVo vo = new TmScheduleResultVo();
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
    private AjaxResult doImport(TmScheduleResultExcelParseResult parseResult, TmScheduleResult condition,
                                boolean updateSupport, Long importLogId) {
        String factoryCode = condition.getFactoryCode().trim();
        Date scheduleDate = parseResult.getScheduleDate();
        List<TmScheduleResultVo> rowList = parseResult.getRowList();
        Set<String> machineCodeSet = rowList.stream().map(TmScheduleResultVo::getMachineCode)
                .filter(StrUtil::isNotBlank).map(String::trim).collect(Collectors.toSet());
        Set<String> treadCodeSet = rowList.stream().map(TmScheduleResultVo::getTreadCode)
                .filter(StrUtil::isNotBlank).map(String::trim).collect(Collectors.toSet());
        Set<String> validMachineCodeSet = this.loadValidMachineCodes(factoryCode, machineCodeSet);
        Map<String, List<TmScheduleResult>> existingResultMap = this.loadExistingResultMap(factoryCode,
                scheduleDate, machineCodeSet, treadCodeSet);

        AjaxResult hardBlockResult = this.checkHardBlock(rowList, existingResultMap, importLogId);
        if (hardBlockResult != null) {
            return hardBlockResult;
        }

        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<TmScheduleResultVo> validRowList = new ArrayList<>();
        Set<String> importBusinessKeySet = new HashSet<>();
        for (TmScheduleResultVo row : rowList) {
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

        String batchNo = this.tmScheduleBatchNoGenerator.generate();
        List<TmScheduleResult> insertList = new ArrayList<>();
        List<TmScheduleResult> updateList = new ArrayList<>();
        int insertOrder = 1;
        for (TmScheduleResultVo row : validRowList) {
            String businessKey = this.buildImportBusinessKey(row.getMachineCode(), row.getTreadCode());
            List<TmScheduleResult> existingList = existingResultMap.get(businessKey);
            if (CollUtil.isNotEmpty(existingList)) {
                TmScheduleResult target = existingList.get(0);
                this.applyImportPlanFields(row, target);
                target.setBatchNo(batchNo);
                target.setReleaseStatus(TmScheduleReleaseStatusEnum.NOT_RELEASED.getCode());
                target.setBaseVale(target.getId());
                updateList.add(target);
                continue;
            }
            TmScheduleResult target = this.buildInsertedResult(row, factoryCode, scheduleDate, batchNo, insertOrder++);
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
    private AjaxResult checkHardBlock(List<TmScheduleResultVo> rowList,
                                      Map<String, List<TmScheduleResult>> existingResultMap, Long importLogId) {
        for (TmScheduleResultVo row : rowList) {
            if (StrUtil.isBlank(row.getMachineCode()) || StrUtil.isBlank(row.getTreadCode())) {
                continue;
            }
            String businessKey = this.buildImportBusinessKey(row.getMachineCode(), row.getTreadCode());
            List<TmScheduleResult> existingList = existingResultMap.get(businessKey);
            if (CollUtil.isEmpty(existingList)) {
                continue;
            }
            if (existingList.size() > 1) {
                String message = MessageFormat.format(I18nUtil.getMessage(
                        "ui.data.alert.tm.schedule.excel.multipleMatched"), row.getRowNum(), businessKey);
                return AjaxResult.error(message,
                        Collections.singletonList(new ImportErrorLog(importLogId, row.getRowNum(), message)));
            }
            if (!TmScheduleReleaseStatusEnum.NOT_RELEASED.getCode()
                    .equals(existingList.get(0).getReleaseStatus())) {
                String message = MessageFormat.format(I18nUtil.getMessage(
                        "ui.data.alert.tm.schedule.excel.releaseBlocked"), row.getRowNum(), businessKey);
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
    private void validateImportRow(TmScheduleResultVo row, Set<String> validMachineCodeSet,
                                   Map<String, List<TmScheduleResult>> existingResultMap,
                                   boolean updateSupport, Set<String> importBusinessKeySet) {
        if (StrUtil.isBlank(row.getTreadCode())) {
            row.getErrors().add(this.formatRowMessage("ui.data.alert.tm.schedule.excel.treadRequired", row.getRowNum()));
        } else {
            row.setTreadCode(row.getTreadCode().trim());
        }
        if (StrUtil.isBlank(row.getMachineCode())) {
            row.getErrors().add(this.formatRowMessage("ui.data.alert.tm.schedule.excel.machineRequired", row.getRowNum()));
        } else {
            row.setMachineCode(row.getMachineCode().trim());
            if (!validMachineCodeSet.contains(row.getMachineCode())) {
                row.getErrors().add(MessageFormat.format(I18nUtil.getMessage(
                        "ui.data.alert.tm.schedule.excel.machineNotFound"), row.getRowNum(), row.getMachineCode()));
            }
        }
        this.validatePlanAndSequence(row, row.getClass1PlanQty(), row.getClass1Sequence(), 1);
        this.validatePlanAndSequence(row, row.getClass2PlanQty(), row.getClass2Sequence(), 2);
        this.validatePlanAndSequence(row, row.getClass3PlanQty(), row.getClass3Sequence(), 3);
        if (StrUtil.isBlank(row.getMachineCode()) || StrUtil.isBlank(row.getTreadCode())) {
            return;
        }
        String businessKey = this.buildImportBusinessKey(row.getMachineCode(), row.getTreadCode());
        if (!importBusinessKeySet.add(businessKey)) {
            row.getErrors().add(MessageFormat.format(I18nUtil.getMessage(
                    "ui.data.alert.tm.schedule.excel.duplicateInFile"), row.getRowNum(), businessKey));
        }
        boolean existing = CollUtil.isNotEmpty(existingResultMap.get(businessKey));
        if (existing && !updateSupport) {
            row.getErrors().add(MessageFormat.format(I18nUtil.getMessage(
                    "ui.data.alert.tm.schedule.excel.updateNotSupported"), row.getRowNum(), businessKey));
        }
        if (!existing && !this.hasPositivePlanQty(row)) {
            row.getErrors().add(this.formatRowMessage("ui.data.alert.tm.schedule.excel.allPlanZero", row.getRowNum()));
        }
    }

    /**
     * 校验班次计划量和顺序。
     *
     * @param row 导入行
     * @param planQty 计划量
     * @param sequence 顺序
     * @param shiftOrder 班次序号
     */
    private void validatePlanAndSequence(TmScheduleResultVo row, BigDecimal planQty,
                                         Integer sequence, int shiftOrder) {
        BigDecimal normalizedPlanQty = BigDecimalUtils.valueOf(planQty);
        if (normalizedPlanQty.compareTo(BigDecimal.ZERO) < 0) {
            row.getErrors().add(MessageFormat.format(I18nUtil.getMessage(
                    "ui.data.alert.tm.schedule.excel.planNegative"), row.getRowNum(), shiftOrder));
            return;
        }
        if (normalizedPlanQty.compareTo(BigDecimal.ZERO) > 0 && (sequence == null || sequence <= 0)) {
            row.getErrors().add(MessageFormat.format(I18nUtil.getMessage(
                    "ui.data.alert.tm.schedule.excel.sequenceRequired"), row.getRowNum(), shiftOrder));
        }
    }

    /**
     * 查询工厂有效机台编码。
     *
     * @param factoryCode 工厂编码
     * @param machineCodeSet 导入机台编码
     * @return 有效机台编码集合
     */
    private Set<String> loadValidMachineCodes(String factoryCode, Set<String> machineCodeSet) {
        if (machineCodeSet.isEmpty()) {
            return Collections.emptySet();
        }
        LambdaQueryWrapper<TmMachineInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TmMachineInfo::getFactoryCode, factoryCode);
        wrapper.in(TmMachineInfo::getMachineCode, machineCodeSet);
        return this.tmMachineInfoMapper.selectList(wrapper).stream()
                .map(TmMachineInfo::getMachineCode).filter(StrUtil::isNotBlank).collect(Collectors.toSet());
    }

    /**
     * 查询导入业务键对应的当前有效排程结果。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @param machineCodeSet 机台编码集合
     * @param treadCodeSet 胎面编码集合
     * @return 业务键对应的已有记录集合
     */
    private Map<String, List<TmScheduleResult>> loadExistingResultMap(String factoryCode, Date scheduleDate,
                                                                      Set<String> machineCodeSet,
                                                                      Set<String> treadCodeSet) {
        if (machineCodeSet.isEmpty() || treadCodeSet.isEmpty()) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<TmScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TmScheduleResult::getFactoryCode, factoryCode);
        wrapper.eq(TmScheduleResult::getScheduleDate, scheduleDate);
        wrapper.in(TmScheduleResult::getMachineCode, machineCodeSet);
        wrapper.in(TmScheduleResult::getTreadCode, treadCodeSet);
        wrapper.orderByAsc(TmScheduleResult::getId);
        return this.tmScheduleResultMapper.selectList(wrapper).stream()
                .collect(Collectors.groupingBy(this::buildResultBusinessKey, LinkedHashMap::new,
                        Collectors.toList()));
    }

    /**
     * 构建新增排程结果。
     *
     * @param row 导入行
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @param batchNo 新批次号
     * @param insertOrder 新增行序号
     * @return 新增排程结果
     */
    private TmScheduleResult buildInsertedResult(TmScheduleResultVo row, String factoryCode,
                                                  Date scheduleDate, String batchNo, int insertOrder) {
        TmScheduleResult target = new TmScheduleResult();
        target.setFactoryCode(factoryCode);
        target.setScheduleDate(scheduleDate);
        target.setBatchNo(batchNo);
        target.setOrderNo(batchNo + "-" + String.format("%04d", insertOrder));
        target.setMachineCode(row.getMachineCode());
        target.setTreadCode(row.getTreadCode());
        target.setTreadShoulderLength(row.getTreadLength());
        target.setCxRemainQty(row.getCxRemainQty());
        target.setMaterialDesc(row.getMaterialDesc());
        target.setWholeGlueCode(row.getWholeGlueCode());
        target.setSixClockStockQty(row.getStockQty());
        target.setCurlRollLength(row.getCurlRollLength());
        target.setCxMachineCode(row.getCxMachineCode());
        target.setReleaseStatus(TmScheduleReleaseStatusEnum.NOT_RELEASED.getCode());
        target.setDataSource(TmScheduleConstants.IMPORT_SCHEDULE_DATA_SOURCE);
        target.setTailFlag(TmYesNoEnum.NO.getCode());
        this.applyImportPlanFields(row, target);
        return target;
    }

    /**
     * 将 CLASS1~3 顺序和计划量写入结果。
     *
     * @param row 导入行
     * @param target 目标排程结果
     */
    private void applyImportPlanFields(TmScheduleResultVo row, TmScheduleResult target) {
        BigDecimal class1PlanQty = BigDecimalUtils.valueOf(row.getClass1PlanQty());
        BigDecimal class2PlanQty = BigDecimalUtils.valueOf(row.getClass2PlanQty());
        BigDecimal class3PlanQty = BigDecimalUtils.valueOf(row.getClass3PlanQty());
        target.setClass1PlanQty(class1PlanQty);
        target.setClass1Sequence(class1PlanQty.compareTo(BigDecimal.ZERO) > 0 ? row.getClass1Sequence() : null);
        target.setClass2PlanQty(class2PlanQty);
        target.setClass2Sequence(class2PlanQty.compareTo(BigDecimal.ZERO) > 0 ? row.getClass2Sequence() : null);
        target.setClass3PlanQty(class3PlanQty);
        target.setClass3Sequence(class3PlanQty.compareTo(BigDecimal.ZERO) > 0 ? row.getClass3Sequence() : null);
    }

    /**
     * 在短事务中批量保存有效导入数据。
     *
     * @param insertList 新增记录
     * @param updateList 更新记录
     * @throws ServiceException 更新或批量新增失败时抛出
     */
    private void persistImportRows(List<TmScheduleResult> insertList, List<TmScheduleResult> updateList) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(this.platformTransactionManager);
        Boolean success = transactionTemplate.execute(transactionStatus -> {
            if (CollUtil.isNotEmpty(updateList)) {
                Integer updatedRows = this.baseDao.updateBatch(updateList);
                if (updatedRows == null || updatedRows != updateList.size()) {
                    throw new ServiceException(I18nUtil.getMessage(
                            "ui.data.alert.tm.schedule.excel.persistFailed"));
                }
            }
            if (CollUtil.isNotEmpty(insertList)) {
                Integer insertedRows = this.baseDao.saveBatch(insertList);
                if (insertedRows == null || insertedRows != insertList.size()) {
                    throw new ServiceException(I18nUtil.getMessage(
                            "ui.data.alert.tm.schedule.excel.persistFailed"));
                }
            }
            return Boolean.TRUE;
        });
        if (!Boolean.TRUE.equals(success)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.excel.persistFailed"));
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
    private void saveExportLog(TmScheduleResult queryVO, String fileName, int rowCount, Date beginTime) {
        Date endTime = DateUtils.getNowDate();
        ExportLog exportLog = new ExportLog();
        exportLog.setProcedureCode("0");
        exportLog.setFunctionCode("tmScheduleResult");
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
     * 构建结果机台和胎面业务键。
     *
     * @param result 排程结果
     * @return 业务键
     */
    private String buildResultBusinessKey(TmScheduleResult result) {
        return this.buildImportBusinessKey(result.getMachineCode(), result.getTreadCode());
    }

    /**
     * 构建导入机台和胎面业务键。
     *
     * @param machineCode 胎面机台编码
     * @param treadCode 胎面编码
     * @return 业务键
     */
    private String buildImportBusinessKey(String machineCode, String treadCode) {
        return StrUtil.blankToDefault(machineCode, "").trim() + "|"
                + StrUtil.blankToDefault(treadCode, "").trim();
    }

    /**
     * 构建施工版本关联键。
     *
     * @param constructionCode 施工编码
     * @param constructionVersion 施工版本
     * @return 施工版本关联键
     */
    private String buildConstructionKey(String constructionCode, String constructionVersion) {
        return StrUtil.blankToDefault(constructionCode, "").trim() + "|"
                + StrUtil.blankToDefault(constructionVersion, "").trim();
    }

    /**
     * 判断导入行是否至少一个班次计划量大于 0。
     *
     * @param row 导入行
     * @return true 表示至少一个班次计划量大于 0
     */
    private boolean hasPositivePlanQty(TmScheduleResultVo row) {
        return BigDecimalUtils.valueOf(row.getClass1PlanQty()).compareTo(BigDecimal.ZERO) > 0
                || BigDecimalUtils.valueOf(row.getClass2PlanQty()).compareTo(BigDecimal.ZERO) > 0
                || BigDecimalUtils.valueOf(row.getClass3PlanQty()).compareTo(BigDecimal.ZERO) > 0;
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
