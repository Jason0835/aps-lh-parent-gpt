package com.zlt.aps.lh.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.RowStateEnum;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.common.engine.domain.LhDayPlanAdjustVo;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.lh.api.domain.dto.LhDayPlanAdjustRequireImportDTO;
import com.zlt.aps.lh.api.domain.entity.LhDayPlanAdjustRequire;
import com.zlt.aps.lh.api.domain.vo.LhDayPlanAdjustRequireExportVo;
import com.zlt.aps.lh.api.domain.vo.LhDayPlanAdjustRequireImportVo;
import com.zlt.aps.lh.api.domain.vo.LhDayPlanAdjustRequireSummaryVo;
import com.zlt.aps.lh.api.enums.DeleteFlagEnum;
import com.zlt.aps.lh.mapper.FactoryMonthPlanProductionFinalResultMapper;
import com.zlt.aps.lh.mapper.LhDayPlanAdjustRequireMapper;
import com.zlt.aps.lh.mapper.MpFactoryProductionVersionMapper;
import com.zlt.aps.lh.service.ILhDayPlanAdjustRequireService;
import com.zlt.aps.maindata.mapper.MdmRawMaterialConversionEntityMapper;
import com.zlt.aps.mdm.api.domain.entity.MdmRawMaterialConversion;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import com.zlt.aps.mp.api.domain.entity.MpFactoryProductionVersion;
import com.zlt.aps.utils.BeanCopyUtils;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.utils.ImportExcelUtils;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.sysdef.domain.SysDocType;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.YearMonth;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 硫化日计划调整需求服务实现。
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class LhDayPlanAdjustRequireServiceImpl extends AbstractDocService<LhDayPlanAdjustRequire>
        implements ILhDayPlanAdjustRequireService {

    private static final String FINAL_FLAG = "1";
    private static final int DEFAULT_PAGE_NUM = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int ADJUST_SLOT_COUNT = 3;
    private static final String ADJUST_ID_FIELD_TEMPLATE = "adjustId%d";
    private static final String ADJUST_QTY_FIELD_TEMPLATE = "adjustQty%d";
    private static final String ADJUST_REASON_FIELD_TEMPLATE = "adjustReason%d";
    private static final String ADJUSTER_FIELD_TEMPLATE = "adjuster%d";
    private static final String ADJUST_TIME_FIELD_TEMPLATE = "adjustTime%d";
    /** 导入 Excel 第一条数据所在行号，第 1 行为表头 */
    private static final int IMPORT_FIRST_DATA_ROW = 2;
    /** 导入重复校验使用的业务唯一字段，工厂取页面工厂不参与 Excel 重复判断 */
    private static final String[] IMPORT_REPEAT_FIELDS = {"yearMonth", "materialCode", "productStatus"};
    /** 年月位数 */
    private static final int YEAR_MONTH_LENGTH = 6;
    /** 无登录人时的兜底操作人 */
    private static final String SYSTEM_USER = "system";

    @Resource
    private LhDayPlanAdjustRequireMapper lhDayPlanAdjustRequireMapper;

    @Resource
    private FactoryMonthPlanProductionFinalResultMapper monthPlanMapper;

    @Resource
    private MpFactoryProductionVersionMapper productionVersionMapper;

    @Resource
    private MdmRawMaterialConversionEntityMapper rawMaterialConversionMapper;

    @Resource
    private IImportLogService importLogService;

    @Resource
    private IImportErrorLogService importErrorLogService;

    /**
     * 查询月计划基础行，并批量回填调整明细和胎面胶 TD。
     *
     * @param queryVO 查询条件
     * @return 分页结果
     */
    @Override
    public TableDataInfo listPage(LhDayPlanAdjustRequire queryVO) {
        this.normalizeQuery(queryVO);
        String productionVersion = this.resolveFinalProductionVersion(queryVO);
        if (StringUtils.isBlank(productionVersion)) {
            return this.buildTableData(new ArrayList<LhDayPlanAdjustRequire>(0), 0);
        }

        List<FactoryMonthPlanProductionFinalResult> monthPlanList = this.queryMonthPlanList(queryVO, productionVersion);
        List<LhDayPlanAdjustRequire> allRows = this.aggregateMonthPlanRows(queryVO, productionVersion, monthPlanList);
        boolean sortAdjustQty = Arrays.asList("adjustQty1", "adjustQty2", "adjustQty3")
                .contains(queryVO.getOrderByColumn())
                && Arrays.asList("asc", "desc").contains(queryVO.getIsAsc());
        if (sortAdjustQty) {
            this.fillAdjustRows(allRows);
            Comparator<BigDecimal> quantityOrder = "desc".equals(queryVO.getIsAsc())
                    ? Comparator.reverseOrder() : Comparator.naturalOrder();
            allRows.sort(Comparator.comparing(
                    (LhDayPlanAdjustRequire row) -> (BigDecimal) row.getFieldValueByFieldName(queryVO.getOrderByColumn()),
                    Comparator.nullsLast(quantityOrder)));
        }
        List<LhDayPlanAdjustRequire> pageRows = this.pageRows(allRows, queryVO.getPageNum(), queryVO.getPageSize());
        if (!sortAdjustQty) {
            this.fillAdjustRows(pageRows);
        }
        this.fillTreadGlueTd(pageRows, queryVO.getFactoryCode());
        pageRows.forEach(this::calculateAdjustedTotalQty);
        return this.buildTableData(pageRows, allRows.size());
    }

    /**
     * 汇总当前查询条件下全部月计划行的调整量。
     *
     * @param queryVO 查询条件
     * @return 调整1、调整2、调整3和调整后总合计
     */
    @Override
    public LhDayPlanAdjustRequireSummaryVo summary(LhDayPlanAdjustRequire queryVO) {
        this.normalizeQuery(queryVO);
        LhDayPlanAdjustRequireSummaryVo summaryVo = new LhDayPlanAdjustRequireSummaryVo();
        String productionVersion = this.resolveFinalProductionVersion(queryVO);
        if (StringUtils.isBlank(productionVersion)) {
            return summaryVo;
        }
        List<FactoryMonthPlanProductionFinalResult> monthPlanList = this.queryMonthPlanList(queryVO, productionVersion);
        List<LhDayPlanAdjustRequire> allRows = this.aggregateMonthPlanRows(queryVO, productionVersion, monthPlanList);
        this.fillAdjustRows(allRows);
        for (LhDayPlanAdjustRequire row : allRows) {
            summaryVo.setAdjustQty1(BigDecimalUtils.add(summaryVo.getAdjustQty1(), row.getAdjustQty1()));
            summaryVo.setAdjustQty2(BigDecimalUtils.add(summaryVo.getAdjustQty2(), row.getAdjustQty2()));
            summaryVo.setAdjustQty3(BigDecimalUtils.add(summaryVo.getAdjustQty3(), row.getAdjustQty3()));
            summaryVo.setAdjustedTotalQty(BigDecimalUtils.add(
                    summaryVo.getAdjustedTotalQty(),
                    BigDecimalUtils.add(row.getMonthPlanQty(),
                            BigDecimalUtils.add(row.getAdjustQty1(),
                                    BigDecimalUtils.add(row.getAdjustQty2(), row.getAdjustQty3())))));
        }
        return summaryVo;
    }

    /**
     * 保存当前列表行的三个调整槽位。
     *
     * @param entity 当前行数据
     */
    @Override
    public void saveRow(LhDayPlanAdjustRequire entity) {
        this.normalizeQuery(entity);
        this.validateSaveRequest(entity);
        String productionVersion = this.resolveFinalProductionVersion(entity);
        if (StringUtils.isBlank(productionVersion)) {
            throw new IllegalArgumentException(I18nUtil.getMessage(
                    "ui.data.alert.lhDayPlanAdjustRequire.finalVersionMissing"));
        }

        FactoryMonthPlanProductionFinalResult sourcePlan = this.findSourceMonthPlan(entity, productionVersion);
        if (Objects.isNull(sourcePlan)) {
            throw new IllegalArgumentException(I18nUtil.getMessage(
                    "ui.data.alert.lhDayPlanAdjustRequire.monthPlanMissing"));
        }

        for (int adjustIndex = 1; adjustIndex <= ADJUST_SLOT_COUNT; adjustIndex++) {
            BigDecimal adjustQty = (BigDecimal) entity.getFieldValueByFieldName(
                    String.format(ADJUST_QTY_FIELD_TEMPLATE, adjustIndex));
            String adjustReason = StringUtils.trim((String) entity.getFieldValueByFieldName(
                    String.format(ADJUST_REASON_FIELD_TEMPLATE, adjustIndex)));
            this.validateAdjustSlot(entity.getMaterialCode(), adjustIndex, adjustQty, adjustReason);
            this.saveAdjustSlot(entity, sourcePlan, adjustIndex, adjustQty, adjustReason);
        }
    }

    /**
     * 按查询条件导出全部匹配行。
     *
     * <p>导出复用列表的归并口径：先确定有效定稿版本，再按物料和产品状态归并行，
     * 然后回填三次调整。导出不分页、不应用调整量排序，导出列与导入模板保持同一套紧凑列。</p>
     *
     * @param queryVO 查询条件，与列表一致
     * @param fileName 导出文件名
     * @return Excel 文件字节
     */
    @Override
    public byte[] exportData(LhDayPlanAdjustRequire queryVO, String fileName) {
        this.normalizeQuery(queryVO);
        List<LhDayPlanAdjustRequireExportVo> exportRows = this.buildExportRows(queryVO);
        ExcelUtil<LhDayPlanAdjustRequireExportVo> excelUtil =
                new ExcelUtil<LhDayPlanAdjustRequireExportVo>(LhDayPlanAdjustRequireExportVo.class);
        try (Workbook workbook = excelUtil.exportExcelFromList(exportRows, fileName);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException(I18nUtil.getMessage(
                    "ui.data.alert.lhDayPlanAdjustRequire.exportFailed"), exception);
        }
    }

    /**
     * 构建导出数据。
     *
     * @param queryVO 查询条件
     * @return 导出行，无有效版本或月计划时返回空列表
     */
    private List<LhDayPlanAdjustRequireExportVo> buildExportRows(LhDayPlanAdjustRequire queryVO) {
        String productionVersion = this.resolveFinalProductionVersion(queryVO);
        if (StringUtils.isBlank(productionVersion)) {
            return new ArrayList<LhDayPlanAdjustRequireExportVo>(0);
        }
        List<FactoryMonthPlanProductionFinalResult> monthPlanList = this.queryMonthPlanList(queryVO, productionVersion);
        List<LhDayPlanAdjustRequire> allRows = this.aggregateMonthPlanRows(queryVO, productionVersion, monthPlanList);
        if (CollectionUtils.isEmpty(allRows)) {
            return new ArrayList<LhDayPlanAdjustRequireExportVo>(0);
        }
        this.fillAdjustRows(allRows);
        this.fillTreadGlueTd(allRows, queryVO.getFactoryCode());
        allRows.forEach(this::calculateAdjustedTotalQty);
        return allRows.stream().map(this::toExportVo).collect(Collectors.toList());
    }

    /**
     * 列表行转换为导出行，列顺序与导入模板保持一致。
     *
     * @param row 列表行
     * @return 导出行
     */
    private LhDayPlanAdjustRequireExportVo toExportVo(LhDayPlanAdjustRequire row) {
        LhDayPlanAdjustRequireExportVo exportVo = new LhDayPlanAdjustRequireExportVo();
        exportVo.setYearMonth(this.formatYearMonth(row.getYearMonth()));
        exportVo.setMaterialCode(row.getMaterialCode());
        exportVo.setProductStatus(row.getProductStatus());
        exportVo.setAdjustQty1(row.getAdjustQty1());
        exportVo.setAdjustReason1(row.getAdjustReason1());
        exportVo.setAdjustQty2(row.getAdjustQty2());
        exportVo.setAdjustReason2(row.getAdjustReason2());
        exportVo.setAdjustQty3(row.getAdjustQty3());
        exportVo.setAdjustReason3(row.getAdjustReason3());
        exportVo.setFactoryCode(row.getFactoryCode());
        return exportVo;
    }

    /**
     * 把 YYYYMM 格式年月转换为 yyyy-MM 文本。
     *
     * @param yearMonth 年月
     * @return 展示文本，空值返回空字符串
     */
    private String formatYearMonth(Integer yearMonth) {
        if (Objects.isNull(yearMonth)) {
            return StringUtils.EMPTY;
        }
        String yearMonthText = String.valueOf(yearMonth);
        if (yearMonthText.length() != YEAR_MONTH_LENGTH) {
            return yearMonthText;
        }
        return yearMonthText.substring(0, 4) + "-" + yearMonthText.substring(4);
    }

    /**
     * 导入硫化日计划调整需求。
     *
     * <p>导入只维护已存在月计划 SKU 的三次调整，不允许通过导入新增月计划外的 SKU。
     * 判断链：行级先校验 SKU 是否在有效月计划中，槽位级再看是否存在未删除记录，
     * 最后按页面 updateSupport 决定已存在槽位是否允许覆盖。</p>
     *
     * @param importDTO 导入文件上下文及页面工厂、年月
     * @param updateSupport 已存在调整槽位是否允许覆盖
     * @return 导入结果
     * @throws Exception 导入日志或文件处理异常
     */
    @Override
    public AjaxResult importData(LhDayPlanAdjustRequireImportDTO importDTO, boolean updateSupport) throws Exception {
        this.validateImportRequest(importDTO);
        ImportContext importContext = importDTO.getImportContext();
        Date beginTime = DateUtils.getNowDate();
        ImportLog importLog = ImportExcelUtils.getImportLogAndUploadFile(importContext.getFileBytes(),
                importContext.getImportFilePath(), importContext.getProcedureCode(), importContext.getFunctionName(),
                importContext.getOriFileName(), 1);
        importLog = this.importLogService.add(importLog);
        Date endTime;
        AjaxResult ajaxResult;
        int rowCount = 0;
        try {
            List<LhDayPlanAdjustRequireImportVo> importRows =
                    new ExcelUtil<>(LhDayPlanAdjustRequireImportVo.class).importExcel(
                            new ByteArrayInputStream(importContext.getFileBytes()));
            rowCount = CollectionUtils.size(importRows);
            ajaxResult = this.doImport(importRows, importDTO, updateSupport, importLog.getId());
        } catch (RuntimeException exception) {
            ajaxResult = AjaxResult.error(exception.getMessage());
        }
        endTime = DateUtils.getNowDate();
        importLog.setRowCount(rowCount);
        importLog.setBeginTime(beginTime);
        importLog.setEndTime(endTime);
        importLog.setSpendTime(DateUtils.getDiffTime(endTime, beginTime));
        ImportExcelUtils.updateImportLogAndFormatMsg(importLog, ajaxResult, this.importLogService);
        ImportExcelUtils.saveImportErrorLogs(ajaxResult, this.importErrorLogService);
        return ajaxResult;
    }

    /**
     * 校验导入请求的文件和页面上下文。
     *
     * @param importDTO 导入请求
     */
    private void validateImportRequest(LhDayPlanAdjustRequireImportDTO importDTO) {
        if (Objects.isNull(importDTO) || Objects.isNull(importDTO.getImportContext())
                || Objects.isNull(importDTO.getImportContext().getFileBytes())
                || importDTO.getImportContext().getFileBytes().length == 0) {
            throw new IllegalArgumentException(I18nUtil.getMessage(
                    "ui.data.alert.lhDayPlanAdjustRequire.importFileRequired"));
        }
        if (StringUtils.isBlank(importDTO.getFactoryCode())) {
            throw new IllegalArgumentException(I18nUtil.getMessage(
                    "ui.data.alert.lhDayPlanAdjustRequire.factoryRequired"));
        }
        if (Objects.isNull(importDTO.getYearMonth())) {
            throw new IllegalArgumentException(I18nUtil.getMessage(
                    "ui.data.alert.lhDayPlanAdjustRequire.yearMonthRequired"));
        }
    }

    /**
     * 执行导入的两轮校验和写入。
     *
     * @param importRows    解析出的导入行
     * @param importDTO     导入请求
     * @param updateSupport 已存在槽位是否允许覆盖
     * @param importLogId   导入日志 ID
     * @return 导入结果
     */
    private AjaxResult doImport(List<LhDayPlanAdjustRequireImportVo> importRows,
                                LhDayPlanAdjustRequireImportDTO importDTO,
                                boolean updateSupport,
                                Long importLogId) {
        if (CollectionUtils.isEmpty(importRows)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.lhDayPlanAdjustRequire.importFileEmpty"));
        }
        // Excel 年月必须与页面搜索年月一致，避免一个文件误覆盖其他月份数据
        this.validateImportYearMonth(importRows, importDTO.getYearMonth());

        int successNum = 0;
        int failureNum = 0;
        List<ImportErrorLog> importErrorLogs = new ArrayList<ImportErrorLog>();
        List<LhDayPlanAdjustRequire> insertList = new ArrayList<LhDayPlanAdjustRequire>();
        List<LhDayPlanAdjustRequire> updateList = new ArrayList<LhDayPlanAdjustRequire>();
        // 第一轮失败行号集合，第二轮只处理第一轮通过的行
        Set<Integer> invalidRowIndexSet = new HashSet<Integer>();

        // 第一轮：导入 VO 字段注解必填校验、字典型校验、数值校验和 Excel 内重复校验
        for (int i = 0; i < importRows.size(); i++) {
            int errorNum = i + IMPORT_FIRST_DATA_ROW;
            LhDayPlanAdjustRequireImportVo importVo = importRows.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, importVo);
            ImportExcelValidatedUtils.validatedRepeat(importRows, importVo, i, IMPORT_FIRST_DATA_ROW,
                    importLogId, validated, IMPORT_REPEAT_FIELDS);
            if (CollectionUtils.isNotEmpty(validated)) {
                failureNum++;
                invalidRowIndexSet.add(i);
                importErrorLogs.addAll(validated);
            }
        }

        // 第二轮前批量预取有效月计划 SKU 集合和已存在调整槽位，循环中不再查库
        Set<String> monthPlanSkuKeys = this.loadMonthPlanSkuKeys(importDTO);
        Map<String, LhDayPlanAdjustRequire> existSlotMap =
                this.loadExistSlotMap(importDTO.getFactoryCode(), importDTO.getYearMonth());
        String currentUser = StringUtils.defaultIfBlank(SecurityUtils.getUsername(), SYSTEM_USER);
        Date currentTime = new Date();

        // 第二轮：业务校验与写入收集
        for (int i = 0; i < importRows.size(); i++) {
            if (invalidRowIndexSet.contains(i)) {
                continue;
            }
            int errorNum = i + IMPORT_FIRST_DATA_ROW;
            LhDayPlanAdjustRequireImportVo importVo = importRows.get(i);
            String materialCode = StringUtils.trim(importVo.getMaterialCode());
            String productStatus = StringUtils.trim(importVo.getProductStatus());
            StringBuilder rowMessage = new StringBuilder();
            // 行级校验：SKU 必须存在于当前有效定稿月计划，导入不允许新增月计划外 SKU
            String skuKey = this.buildMaterialStatusKey(materialCode, productStatus);
            if (!monthPlanSkuKeys.contains(skuKey)) {
                rowMessage.append(I18nUtil.getMessage("ui.data.alert.lhDayPlanAdjustRequire.monthPlanMissing"));
            }
            // 槽位级校验：数量与原因必须同时填写，两者都为空表示该槽位不参与导入
            boolean hasAdjustmentValue = false;
            for (int adjustIndex = 1; adjustIndex <= ADJUST_SLOT_COUNT; adjustIndex++) {
                BigDecimal adjustQty = this.getImportAdjustQty(importVo, adjustIndex);
                String adjustReason = StringUtils.trim(this.getImportAdjustReason(importVo, adjustIndex));
                if (Objects.isNull(adjustQty) && StringUtils.isBlank(adjustReason)) {
                    continue;
                }
                hasAdjustmentValue = true;
                if (Objects.isNull(adjustQty)) {
                    rowMessage.append(MessageFormat.format(I18nUtil.getMessage(
                            "ui.data.alert.lhDayPlanAdjustRequire.adjustQtyRequired"), materialCode, adjustIndex));
                    continue;
                }
                if (StringUtils.isBlank(adjustReason)) {
                    rowMessage.append(MessageFormat.format(I18nUtil.getMessage(
                            "ui.data.alert.lhDayPlanAdjustRequire.adjustReasonRequired"), materialCode, adjustIndex));
                }
            }
            if (rowMessage.length() > 0) {
                failureNum++;
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, errorNum, rowMessage.toString(),
                        importErrorLogs);
                continue;
            }
            if (!hasAdjustmentValue) {
                continue;
            }
            // 槽位级写入判断：无记录则新增，有记录按 updateSupport 决定覆盖或记失败
            boolean rowSuccess = this.collectSlotWrites(importVo, importDTO, existSlotMap, updateSupport,
                    currentUser, currentTime, importLogId, errorNum, insertList, updateList,
                    importErrorLogs);
            if (rowSuccess) {
                successNum++;
            } else {
                failureNum++;
            }
        }

        if (CollectionUtils.isNotEmpty(insertList)) {
            insertList.forEach(row -> row.setRowState(RowStateEnum.ADDED));
            successNum += baseDao.saveBatch(insertList);
        }
        updateList.forEach(lhDayPlanAdjustRequireMapper::updateById);
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + ","
                    + successNum + "," + failureNum, importErrorLogs);
        }
        return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
    }

    /**
     * 校验 Excel 年月与页面搜索年月是否整批一致。
     *
     * @param importRows 导入行
     * @param yearMonth  页面搜索年月
     */
    private void validateImportYearMonth(List<LhDayPlanAdjustRequireImportVo> importRows, Integer yearMonth) {
        String pageYearMonth = this.formatYearMonth(yearMonth);
        String firstRowYearMonth = importRows.stream()
                .map(LhDayPlanAdjustRequireImportVo::getYearMonth)
                .filter(StringUtils::isNotBlank)
                .map(this::normalizeYearMonthText)
                .findFirst()
                .orElse(pageYearMonth);
        if (!StringUtils.equals(pageYearMonth, firstRowYearMonth)) {
            throw new IllegalArgumentException(MessageFormat.format(I18nUtil.getMessage(
                    "ui.data.alert.lhDayPlanAdjustRequire.importYearMonthMismatch"),
                    pageYearMonth, firstRowYearMonth));
        }
    }

    /**
     * 把导入的年月文本统一成 yyyy-MM，兼容 yyyyMM 和 yyyy-MM 两种写法。
     *
     * @param yearMonthText 导入年月文本
     * @return yyyy-MM 文本
     */
    private String normalizeYearMonthText(String yearMonthText) {
        String trimmed = StringUtils.remove(StringUtils.trim(yearMonthText), "-");
        if (trimmed.length() != YEAR_MONTH_LENGTH || !StringUtils.isNumeric(trimmed)) {
            return StringUtils.trim(yearMonthText);
        }
        return trimmed.substring(0, 4) + "-" + trimmed.substring(4);
    }

    /**
     * 批量预取当前工厂和年月有效定稿月计划中的 SKU 集合。
     *
     * @param importDTO 导入请求
     * @return SKU 键集合，键为 materialCode|productStatus
     */
    private Set<String> loadMonthPlanSkuKeys(LhDayPlanAdjustRequireImportDTO importDTO) {
        LhDayPlanAdjustRequire normalizedQuery = new LhDayPlanAdjustRequire();
        normalizedQuery.setFactoryCode(importDTO.getFactoryCode());
        normalizedQuery.setYearMonth(importDTO.getYearMonth());
        this.normalizeQuery(normalizedQuery);
        String productionVersion = this.resolveFinalProductionVersion(normalizedQuery);
        if (StringUtils.isBlank(productionVersion)) {
            throw new IllegalArgumentException(I18nUtil.getMessage(
                    "ui.data.alert.lhDayPlanAdjustRequire.finalVersionMissing"));
        }
        List<FactoryMonthPlanProductionFinalResult> monthPlanList =
                this.queryMonthPlanList(normalizedQuery, productionVersion);
        return monthPlanList.stream()
                .filter(Objects::nonNull)
                .filter(plan -> StringUtils.isNotBlank(plan.getMaterialCode()))
                .map(plan -> this.buildMaterialStatusKey(
                        StringUtils.trim(plan.getMaterialCode()), StringUtils.trim(plan.getProductStatus())))
                .collect(Collectors.toSet());
    }

    /**
     * 批量预取当前工厂和年月已存在的调整槽位。
     *
     * @param factoryCode 工厂编码
     * @param yearMonth   年月
     * @return 槽位键到调整记录的映射，槽位键为 materialCode|productStatus|adjustCount
     */
    private Map<String, LhDayPlanAdjustRequire> loadExistSlotMap(String factoryCode, Integer yearMonth) {
        List<LhDayPlanAdjustRequire> existList = lhDayPlanAdjustRequireMapper.selectList(
                new LambdaQueryWrapper<LhDayPlanAdjustRequire>()
                        .eq(LhDayPlanAdjustRequire::getFactoryCode, factoryCode)
                        .eq(LhDayPlanAdjustRequire::getYearMonth, yearMonth)
                        .orderByDesc(LhDayPlanAdjustRequire::getUpdateTime)
                        .orderByDesc(LhDayPlanAdjustRequire::getId));
        return existList.stream()
                .filter(adjust -> Objects.nonNull(adjust.getAdjustCount()))
                .collect(Collectors.toMap(
                        adjust -> this.buildImportSlotKey(adjust.getMaterialCode(),
                                adjust.getProductStatus(), adjust.getAdjustCount()),
                        Function.identity(),
                        (first, second) -> first));
    }

    /**
     * 收集单个导入行三个槽位的写入动作。
     *
     * @param importVo      导入行
     * @param importDTO     导入请求
     * @param existSlotMap  已存在槽位映射
     * @param updateSupport 已存在槽位是否允许覆盖
     * @param currentUser   当前登录人
     * @param currentTime   当前时间
     * @param importLogId   导入日志 ID
     * @param errorNum      Excel 行号
     * @param insertList    待新增列表
     * @param updateList    待更新列表
     * @param importErrorLogs 导入错误日志
     * @return 当前行是否存在被拒绝的槽位
     */
    private boolean collectSlotWrites(LhDayPlanAdjustRequireImportVo importVo,
                                      LhDayPlanAdjustRequireImportDTO importDTO,
                                      Map<String, LhDayPlanAdjustRequire> existSlotMap,
                                      boolean updateSupport,
                                      String currentUser,
                                      Date currentTime,
                                      Long importLogId,
                                      int errorNum,
                                      List<LhDayPlanAdjustRequire> insertList,
                                      List<LhDayPlanAdjustRequire> updateList,
                                      List<ImportErrorLog> importErrorLogs) {
        String materialCode = StringUtils.trim(importVo.getMaterialCode());
        String productStatus = StringUtils.trim(importVo.getProductStatus());
        int yearMonth = importDTO.getYearMonth();
        boolean rowSuccess = true;
        for (int adjustIndex = 1; adjustIndex <= ADJUST_SLOT_COUNT; adjustIndex++) {
            BigDecimal adjustQty = this.getImportAdjustQty(importVo, adjustIndex);
            String adjustReason = StringUtils.trim(this.getImportAdjustReason(importVo, adjustIndex));
            // 数量与原因都为空表示不修改该槽位，导入不承担清空职责
            if (Objects.isNull(adjustQty) && StringUtils.isBlank(adjustReason)) {
                continue;
            }
            String slotKey = this.buildImportSlotKey(materialCode, productStatus, adjustIndex);
            LhDayPlanAdjustRequire existing = existSlotMap.get(slotKey);
            if (Objects.isNull(existing)) {
                LhDayPlanAdjustRequire insertRow = new LhDayPlanAdjustRequire();
                insertRow.setFactoryCode(importDTO.getFactoryCode());
                insertRow.setYear(yearMonth / 100);
                insertRow.setMonth(yearMonth % 100);
                insertRow.setYearMonth(yearMonth);
                insertRow.setProductStatus(productStatus);
                insertRow.setMaterialCode(materialCode);
                insertRow.setAdjustCount(adjustIndex);
                insertRow.setPlanQty(adjustQty);
                insertRow.setReason(adjustReason);
                insertRow.setAdjuster(currentUser);
                insertRow.setCreateBy(currentUser);
                insertRow.setCreateTime(currentTime);
                insertRow.setUpdateBy(currentUser);
                insertRow.setUpdateTime(currentTime);
                insertRow.setIsDelete(0);
                insertList.add(insertRow);
                existSlotMap.put(slotKey, insertRow);
                continue;
            }
            if (!updateSupport) {
                // 未勾选允许更新已有数据时，已存在槽位按唯一性冲突记失败
                rowSuccess = false;
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, errorNum,
                        MessageFormat.format(I18nUtil.getMessage("import.validated.unique"), errorNum),
                        importErrorLogs);
                continue;
            }
            existing.setPlanQty(adjustQty);
            existing.setReason(adjustReason);
            existing.setAdjuster(currentUser);
            existing.setUpdateBy(currentUser);
            existing.setUpdateTime(currentTime);
            updateList.add(existing);
        }
        return rowSuccess;
    }

    /**
     * 读取指定序号的导入调整量。
     *
     * @param importVo    导入行
     * @param adjustIndex 调整序号
     * @return 调整量
     */
    private BigDecimal getImportAdjustQty(LhDayPlanAdjustRequireImportVo importVo, int adjustIndex) {
        if (adjustIndex == 1) {
            return importVo.getAdjustQty1();
        }
        if (adjustIndex == 2) {
            return importVo.getAdjustQty2();
        }
        return importVo.getAdjustQty3();
    }

    /**
     * 读取指定序号的导入调整原因。
     *
     * @param importVo    导入行
     * @param adjustIndex 调整序号
     * @return 调整原因
     */
    private String getImportAdjustReason(LhDayPlanAdjustRequireImportVo importVo, int adjustIndex) {
        if (adjustIndex == 1) {
            return importVo.getAdjustReason1();
        }
        if (adjustIndex == 2) {
            return importVo.getAdjustReason2();
        }
        return importVo.getAdjustReason3();
    }

    /**
     * 生成导入调整槽位键。
     *
     * @param materialCode  物料编码
     * @param productStatus 产品状态
     * @param adjustCount   调整序号
     * @return 槽位键
     */
    private String buildImportSlotKey(String materialCode, String productStatus, int adjustCount) {
        return this.buildMaterialStatusKey(materialCode, productStatus) + "|" + adjustCount;
    }

    /**
     * 规范查询年月、工厂和分页参数。
     *
     * @param queryVO 查询条件
     */
    private void normalizeQuery(LhDayPlanAdjustRequire queryVO) {
        if (Objects.isNull(queryVO)) {
            throw new IllegalArgumentException(I18nUtil.getMessage(
                    "ui.data.alert.lhDayPlanAdjustRequire.requestRequired"));
        }
        if (StringUtils.isBlank(queryVO.getFactoryCode())) {
            queryVO.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        queryVO.setFactoryCode(StringUtils.trim(queryVO.getFactoryCode()));
        queryVO.setMaterialCode(StringUtils.trim(queryVO.getMaterialCode()));
        queryVO.setMaterialDesc(StringUtils.trim(queryVO.getMaterialDesc()));
        queryVO.setProductStatus(StringUtils.trim(queryVO.getProductStatus()));
        if (Objects.isNull(queryVO.getYearMonth())) {
            Date currentDate = new Date();
            int currentYear = DateUtil.year(currentDate);
            int currentMonth = DateUtil.month(currentDate) + 1;
            queryVO.setYearMonth(currentYear * 100 + currentMonth);
        }
        int year = queryVO.getYearMonth() / 100;
        int month = queryVO.getYearMonth() % 100;
        if (year < 1000 || year > 9999 || month < 1 || month > 12) {
            throw new IllegalArgumentException(I18nUtil.getMessage(
                    "ui.data.alert.lhDayPlanAdjustRequire.yearMonthInvalid"));
        }
        queryVO.setYear(year);
        queryVO.setMonth(month);
        if (Objects.isNull(queryVO.getPageNum()) || queryVO.getPageNum() < 1) {
            queryVO.setPageNum(DEFAULT_PAGE_NUM);
        }
        if (Objects.isNull(queryVO.getPageSize()) || queryVO.getPageSize() < 1) {
            queryVO.setPageSize(DEFAULT_PAGE_SIZE);
        }
    }

    /**
     * 获取当前年月最新定稿排产版本。
     *
     * @param queryVO 查询条件
     * @return 排产版本；不存在时返回空
     */
    private String resolveFinalProductionVersion(LhDayPlanAdjustRequire queryVO) {
        List<MpFactoryProductionVersion> versionList = productionVersionMapper.selectList(
                new LambdaQueryWrapper<MpFactoryProductionVersion>()
                        .eq(MpFactoryProductionVersion::getFactoryCode, queryVO.getFactoryCode())
                        .eq(MpFactoryProductionVersion::getYear, queryVO.getYear())
                        .eq(MpFactoryProductionVersion::getMonth, queryVO.getMonth())
                        .eq(MpFactoryProductionVersion::getIsFinal, FINAL_FLAG)
                        .orderByDesc(MpFactoryProductionVersion::getUpdateTime)
                        .orderByDesc(MpFactoryProductionVersion::getId)
                        .last("limit 1"));
        if (CollectionUtils.isEmpty(versionList)) {
            return null;
        }
        return StringUtils.trim(versionList.get(0).getProductionVersion());
    }

    /**
     * 查询有效排产版本的月计划明细。
     *
     * @param queryVO           查询条件
     * @param productionVersion 排产版本
     * @return 月计划明细
     */
    private List<FactoryMonthPlanProductionFinalResult> queryMonthPlanList(
            LhDayPlanAdjustRequire queryVO, String productionVersion) {
        return monthPlanMapper.selectList(new LambdaQueryWrapper<FactoryMonthPlanProductionFinalResult>()
                .eq(FactoryMonthPlanProductionFinalResult::getFactoryCode, queryVO.getFactoryCode())
                .eq(FactoryMonthPlanProductionFinalResult::getYear, queryVO.getYear())
                .eq(FactoryMonthPlanProductionFinalResult::getMonth, queryVO.getMonth())
                .eq(FactoryMonthPlanProductionFinalResult::getProductionVersion, productionVersion)
                .like(StringUtils.isNotBlank(queryVO.getMaterialCode()),
                        FactoryMonthPlanProductionFinalResult::getMaterialCode, queryVO.getMaterialCode())
                .like(StringUtils.isNotBlank(queryVO.getMaterialDesc()),
                        FactoryMonthPlanProductionFinalResult::getMaterialDesc, queryVO.getMaterialDesc())
                .eq(StringUtils.isNotBlank(queryVO.getProductStatus()),
                        FactoryMonthPlanProductionFinalResult::getProductStatus, queryVO.getProductStatus()));
    }

    /**
     * 按物料和产品状态归并月计划量。
     *
     * @param queryVO           查询条件
     * @param productionVersion 排产版本
     * @param monthPlanList     月计划明细
     * @return 归并列表
     */
    private List<LhDayPlanAdjustRequire> aggregateMonthPlanRows(
            LhDayPlanAdjustRequire queryVO,
            String productionVersion,
            List<FactoryMonthPlanProductionFinalResult> monthPlanList) {
        Map<String, LhDayPlanAdjustRequire> rowMap = new LinkedHashMap<String, LhDayPlanAdjustRequire>();
        if (CollectionUtils.isEmpty(monthPlanList)) {
            return new ArrayList<LhDayPlanAdjustRequire>(0);
        }
        monthPlanList.stream()
                .filter(Objects::nonNull)
                .filter(plan -> StringUtils.isNotBlank(plan.getMaterialCode()))
                .forEach(plan -> {
                    String materialCode = StringUtils.trim(plan.getMaterialCode());
                    String productStatus = StringUtils.trim(plan.getProductStatus());
                    String rowKey = this.buildMaterialStatusKey(materialCode, productStatus);
                    LhDayPlanAdjustRequire row = rowMap.computeIfAbsent(rowKey,
                            key -> this.createMonthPlanRow(queryVO, productionVersion, plan));
                    row.setMonthPlanQty(BigDecimalUtils.add(
                            row.getMonthPlanQty(), BigDecimalUtils.valueOf(plan.getTotalQty())));
                    if (StringUtils.isBlank(row.getMaterialDesc()) && StringUtils.isNotBlank(plan.getMaterialDesc())) {
                        row.setMaterialDesc(StringUtils.trim(plan.getMaterialDesc()));
                    }
                    if (StringUtils.isBlank(row.getMesMaterialCode())
                            && StringUtils.isNotBlank(plan.getMesMaterialCode())) {
                        row.setMesMaterialCode(StringUtils.trim(plan.getMesMaterialCode()));
                    }
                    if (Objects.nonNull(plan.getDisplaySeq())
                            && (Objects.isNull(row.getDisplaySeq()) || plan.getDisplaySeq() < row.getDisplaySeq())) {
                        row.setDisplaySeq(plan.getDisplaySeq());
                    }
                });
        /*return rowMap.values().stream()
                .sorted(Comparator.comparing(LhDayPlanAdjustRequire::getDisplaySeq,
                                Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(LhDayPlanAdjustRequire::getMaterialCode,
                                Comparator.nullsLast(String::compareTo))
                        .thenComparing(LhDayPlanAdjustRequire::getProductStatus,
                                Comparator.nullsLast(String::compareTo)))
                .collect(Collectors.toList());*/
        return rowMap.values().stream()
                .sorted(Comparator.comparing(LhDayPlanAdjustRequire::getMaterialDesc,
                                Comparator.nullsLast(String::compareTo))
                        .thenComparing(LhDayPlanAdjustRequire::getProductStatus,
                                Comparator.nullsLast(String::compareTo)))
                .collect(Collectors.toList());
    }

    /**
     * 创建月计划列表基础行。
     *
     * @param queryVO           查询条件
     * @param productionVersion 排产版本
     * @param plan              月计划明细
     * @return 基础行
     */
    private LhDayPlanAdjustRequire createMonthPlanRow(
            LhDayPlanAdjustRequire queryVO,
            String productionVersion,
            FactoryMonthPlanProductionFinalResult plan) {
        LhDayPlanAdjustRequire row = new LhDayPlanAdjustRequire();
        row.setFactoryCode(queryVO.getFactoryCode());
        row.setYear(queryVO.getYear());
        row.setMonth(queryVO.getMonth());
        row.setYearMonth(queryVO.getYearMonth());
        row.setProductionVersion(productionVersion);
        row.setMaterialCode(StringUtils.trim(plan.getMaterialCode()));
        row.setMaterialDesc(StringUtils.trim(plan.getMaterialDesc()));
        row.setMesMaterialCode(StringUtils.trim(plan.getMesMaterialCode()));
        row.setProductStatus(StringUtils.trim(plan.getProductStatus()));
        row.setDisplaySeq(plan.getDisplaySeq());
        row.setMonthPlanQty(BigDecimal.ZERO);
        return row;
    }

    /**
     * 对归并后的列表执行内存分页。
     *
     * @param allRows  全部归并行
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @return 当前页
     */
    private List<LhDayPlanAdjustRequire> pageRows(
            List<LhDayPlanAdjustRequire> allRows, int pageNum, int pageSize) {
        int fromIndex = Math.min((pageNum - 1) * pageSize, allRows.size());
        int toIndex = Math.min(fromIndex + pageSize, allRows.size());
        return new ArrayList<LhDayPlanAdjustRequire>(allRows.subList(fromIndex, toIndex));
    }

    /**
     * 批量回填当前页三次调整。
     *
     * @param pageRows 当前页月计划行
     */
    private void fillAdjustRows(List<LhDayPlanAdjustRequire> pageRows) {
        if (CollectionUtils.isEmpty(pageRows)) {
            return;
        }
        LhDayPlanAdjustRequire firstRow = pageRows.get(0);
        Set<String> materialCodes = pageRows.stream()
                .map(LhDayPlanAdjustRequire::getMaterialCode)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
        List<LhDayPlanAdjustRequire> adjustList = lhDayPlanAdjustRequireMapper.selectList(
                new LambdaQueryWrapper<LhDayPlanAdjustRequire>()
                        .eq(LhDayPlanAdjustRequire::getFactoryCode, firstRow.getFactoryCode())
                        .eq(LhDayPlanAdjustRequire::getYearMonth, firstRow.getYearMonth())
                        .in(LhDayPlanAdjustRequire::getMaterialCode, materialCodes)
                        .orderByDesc(LhDayPlanAdjustRequire::getUpdateTime)
                        .orderByDesc(LhDayPlanAdjustRequire::getId));
        Map<String, LhDayPlanAdjustRequire> rowMap = pageRows.stream()
                .collect(Collectors.toMap(
                        row -> this.buildMaterialStatusKey(row.getMaterialCode(), row.getProductStatus()),
                        row -> row,
                        (first, second) -> first,
                        LinkedHashMap::new));
        Set<String> filledSlotSet = new LinkedHashSet<String>();
        Set<String> latestAdjustSet = new LinkedHashSet<String>();
        for (LhDayPlanAdjustRequire adjust : adjustList) {
            if (Objects.isNull(adjust.getAdjustCount())
                    || adjust.getAdjustCount() < 1
                    || adjust.getAdjustCount() > ADJUST_SLOT_COUNT) {
                continue;
            }
            String rowKey = this.buildMaterialStatusKey(adjust.getMaterialCode(), adjust.getProductStatus());
            LhDayPlanAdjustRequire row = rowMap.get(rowKey);
            if (Objects.isNull(row)) {
                continue;
            }
            if (latestAdjustSet.add(rowKey)) {
                row.setAdjuster(adjust.getAdjuster());
                row.setUpdateTime(adjust.getUpdateTime());
            }
            String slotKey = this.buildAdjustSlotKey(row, adjust.getAdjustCount());
            if (!filledSlotSet.add(slotKey)) {
                continue;
            }
            row.setFieldValueByFieldName(String.format(ADJUST_ID_FIELD_TEMPLATE, adjust.getAdjustCount()),
                    adjust.getId());
            row.setFieldValueByFieldName(String.format(ADJUST_QTY_FIELD_TEMPLATE, adjust.getAdjustCount()),
                    adjust.getPlanQty());
            row.setFieldValueByFieldName(String.format(ADJUST_REASON_FIELD_TEMPLATE, adjust.getAdjustCount()),
                    adjust.getReason());
            row.setFieldValueByFieldName(String.format(ADJUSTER_FIELD_TEMPLATE, adjust.getAdjustCount()),
                    adjust.getAdjuster());
            row.setFieldValueByFieldName(String.format(ADJUST_TIME_FIELD_TEMPLATE, adjust.getAdjustCount()),
                    adjust.getUpdateTime());
        }
    }

    /**
     * 批量查询并回填胎面胶 TD。
     *
     * @param pageRows    当前页月计划行
     * @param factoryCode 工厂编码
     */
    private void fillTreadGlueTd(List<LhDayPlanAdjustRequire> pageRows, String factoryCode) {
        if (CollectionUtils.isEmpty(pageRows)) {
            return;
        }
        Set<String> materialCodes = pageRows.stream()
                .map(LhDayPlanAdjustRequire::getMaterialCode)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
        Set<String> productStatuses = pageRows.stream()
                .map(LhDayPlanAdjustRequire::getProductStatus)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
        if (materialCodes.isEmpty() || productStatuses.isEmpty()) {
            return;
        }
        List<MdmRawMaterialConversion> conversionList = rawMaterialConversionMapper.selectList(
                new LambdaQueryWrapper<MdmRawMaterialConversion>()
                        .eq(StringUtils.isNotBlank(factoryCode),
                                MdmRawMaterialConversion::getFactoryCode, factoryCode)
                        .in(MdmRawMaterialConversion::getMaterialCode, materialCodes)
                        .in(MdmRawMaterialConversion::getConstructionStage, productStatuses)
                        .likeRight(MdmRawMaterialConversion::getRawMaterialName, "AQT"));
        Map<String, String> treadGlueMap = conversionList.stream()
                .filter(conversion -> StringUtils.isNotBlank(conversion.getMaterialCode()))
                .filter(conversion -> StringUtils.isNotBlank(conversion.getConstructionStage()))
                .filter(conversion -> StringUtils.isNotBlank(conversion.getRawMaterialName()))
                .collect(Collectors.groupingBy(
                        conversion -> this.buildMaterialStatusKey(
                                conversion.getMaterialCode(), conversion.getConstructionStage()),
                        LinkedHashMap::new,
                        Collectors.mapping(
                                conversion -> this.removeAqPrefix(conversion.getRawMaterialName()),
                                Collectors.collectingAndThen(
                                        Collectors.toCollection(LinkedHashSet::new),
                                        names -> String.join(",", names)))));
        pageRows.forEach(row -> row.setTreadGlueTd(treadGlueMap.get(
                this.buildMaterialStatusKey(row.getMaterialCode(), row.getProductStatus()))));
    }

    /**
     * 计算调整后合计。
     *
     * @param row 当前列表行
     */
    private void calculateAdjustedTotalQty(LhDayPlanAdjustRequire row) {
        BigDecimal adjustedTotalQty = BigDecimalUtils.valueOf(row.getMonthPlanQty());
        for (int adjustIndex = 1; adjustIndex <= ADJUST_SLOT_COUNT; adjustIndex++) {
            BigDecimal adjustQty = (BigDecimal) row.getFieldValueByFieldName(
                    String.format(ADJUST_QTY_FIELD_TEMPLATE, adjustIndex));
            adjustedTotalQty = BigDecimalUtils.add(adjustedTotalQty, adjustQty);
        }
        row.setAdjustedTotalQty(adjustedTotalQty);
    }

    /**
     * 校验保存主键字段。
     *
     * @param entity 当前行数据
     */
    private void validateSaveRequest(LhDayPlanAdjustRequire entity) {
        if (StringUtils.isBlank(entity.getFactoryCode())) {
            throw new IllegalArgumentException(I18nUtil.getMessage(
                    "ui.data.alert.lhDayPlanAdjustRequire.factoryRequired"));
        }
        if (StringUtils.isBlank(entity.getMaterialCode())) {
            throw new IllegalArgumentException(I18nUtil.getMessage(
                    "ui.data.alert.lhDayPlanAdjustRequire.materialCodeRequired"));
        }
        if (StringUtils.isBlank(entity.getProductStatus())) {
            throw new IllegalArgumentException(I18nUtil.getMessage(
                    "ui.data.alert.lhDayPlanAdjustRequire.productStatusRequired"));
        }
    }

    /**
     * 校验单个调整槽位的数量和原因必须同时填写或同时清空。
     *
     * @param materialCode 物料编码
     * @param adjustIndex  调整序号
     * @param adjustQty    调整量
     * @param adjustReason 调整原因
     */
    private void validateAdjustSlot(
            String materialCode,
            int adjustIndex,
            BigDecimal adjustQty,
            String adjustReason) {
        if (Objects.isNull(adjustQty) && StringUtils.isNotBlank(adjustReason)) {
            throw new IllegalArgumentException(MessageFormat.format(I18nUtil.getMessage(
                    "ui.data.alert.lhDayPlanAdjustRequire.adjustQtyRequired"), materialCode, adjustIndex));
        }
        if (Objects.nonNull(adjustQty) && StringUtils.isBlank(adjustReason)) {
            throw new IllegalArgumentException(MessageFormat.format(I18nUtil.getMessage(
                    "ui.data.alert.lhDayPlanAdjustRequire.adjustReasonRequired"), materialCode, adjustIndex));
        }
    }

    /**
     * 查询保存行对应的有效月计划。
     *
     * @param entity            当前行数据
     * @param productionVersion 排产版本
     * @return 月计划来源快照
     */
    private FactoryMonthPlanProductionFinalResult findSourceMonthPlan(
            LhDayPlanAdjustRequire entity, String productionVersion) {
        List<FactoryMonthPlanProductionFinalResult> planList = monthPlanMapper.selectList(
                new LambdaQueryWrapper<FactoryMonthPlanProductionFinalResult>()
                        .eq(FactoryMonthPlanProductionFinalResult::getFactoryCode, entity.getFactoryCode())
                        .eq(FactoryMonthPlanProductionFinalResult::getYear, entity.getYear())
                        .eq(FactoryMonthPlanProductionFinalResult::getMonth, entity.getMonth())
                        .eq(FactoryMonthPlanProductionFinalResult::getProductionVersion, productionVersion)
                        .eq(FactoryMonthPlanProductionFinalResult::getMaterialCode, entity.getMaterialCode())
                        .eq(FactoryMonthPlanProductionFinalResult::getProductStatus, entity.getProductStatus())
                        .orderByAsc(FactoryMonthPlanProductionFinalResult::getDisplaySeq)
                        .orderByAsc(FactoryMonthPlanProductionFinalResult::getId));
        if (CollectionUtils.isEmpty(planList)) {
            return null;
        }
        FactoryMonthPlanProductionFinalResult sourcePlan = planList.get(0);
        sourcePlan.setMaterialDesc(planList.stream()
                .map(FactoryMonthPlanProductionFinalResult::getMaterialDesc)
                .filter(StringUtils::isNotBlank)
                .map(StringUtils::trim)
                .findFirst()
                .orElse(null));
        sourcePlan.setMesMaterialCode(planList.stream()
                .map(FactoryMonthPlanProductionFinalResult::getMesMaterialCode)
                .filter(StringUtils::isNotBlank)
                .map(StringUtils::trim)
                .findFirst()
                .orElse(null));
        return sourcePlan;
    }

    /**
     * 新增、更新或清空一个调整槽位。
     *
     * @param request      当前行请求
     * @param sourcePlan   月计划来源行
     * @param adjustIndex  调整序号
     * @param adjustQty    调整量
     * @param adjustReason 调整原因
     */
    private void saveAdjustSlot(
            LhDayPlanAdjustRequire request,
            FactoryMonthPlanProductionFinalResult sourcePlan,
            int adjustIndex,
            BigDecimal adjustQty,
            String adjustReason) {
        List<LhDayPlanAdjustRequire> existingList = lhDayPlanAdjustRequireMapper.selectList(
                new LambdaQueryWrapper<LhDayPlanAdjustRequire>()
                        .eq(LhDayPlanAdjustRequire::getFactoryCode, request.getFactoryCode())
                        .eq(LhDayPlanAdjustRequire::getYearMonth, request.getYearMonth())
                        .eq(LhDayPlanAdjustRequire::getMaterialCode, request.getMaterialCode())
                        .eq(LhDayPlanAdjustRequire::getProductStatus, request.getProductStatus())
                        .eq(LhDayPlanAdjustRequire::getAdjustCount, adjustIndex)
                        .orderByDesc(LhDayPlanAdjustRequire::getUpdateTime)
                        .orderByDesc(LhDayPlanAdjustRequire::getId));
        if (existingList.size() > 1) {
            throw new IllegalArgumentException(I18nUtil.getMessage(
                    "ui.data.alert.lhDayPlanAdjustRequire.notUnique"));
        }
        LhDayPlanAdjustRequire existing = existingList.isEmpty() ? null : existingList.get(0);
        if (Objects.isNull(adjustQty) && StringUtils.isBlank(adjustReason)) {
            if (Objects.nonNull(existing)) {
                lhDayPlanAdjustRequireMapper.deleteById(existing.getId());
            }
            return;
        }
        if (Objects.nonNull(existing)
                && this.isSameAdjustValue(existing.getPlanQty(), adjustQty)
                && Objects.equals(existing.getReason(), adjustReason)) {
            return;
        }

        String currentUser = StringUtils.defaultIfBlank(SecurityUtils.getUsername(), "system");
        Date currentTime = new Date();
        LhDayPlanAdjustRequire target = Objects.isNull(existing)
                ? new LhDayPlanAdjustRequire() : existing;
        target.setFactoryCode(request.getFactoryCode());
        target.setYear(request.getYear());
        target.setMonth(request.getMonth());
        target.setYearMonth(request.getYearMonth());
        target.setProductStatus(request.getProductStatus());
        target.setMaterialCode(request.getMaterialCode());
        target.setMaterialDesc(StringUtils.trim(sourcePlan.getMaterialDesc()));
        target.setMesMaterialCode(StringUtils.trim(sourcePlan.getMesMaterialCode()));
        target.setAdjustCount(adjustIndex);
        target.setPlanQty(adjustQty);
        target.setReason(adjustReason);
        target.setAdjuster(currentUser);
        target.setUpdateBy(currentUser);
        target.setUpdateTime(currentTime);
        if (Objects.isNull(existing)) {
            target.setCreateBy(currentUser);
            target.setCreateTime(currentTime);
            target.setIsDelete(0);
            lhDayPlanAdjustRequireMapper.insert(target);
            return;
        }
        lhDayPlanAdjustRequireMapper.updateById(target);
    }

    /**
     * 比较调整量，忽略BigDecimal的小数位差异。
     *
     * @param existingQty 已保存调整量
     * @param requestQty  请求调整量
     * @return 是否相同
     */
    private boolean isSameAdjustValue(BigDecimal existingQty, BigDecimal requestQty) {
        if (Objects.isNull(existingQty) || Objects.isNull(requestQty)) {
            return Objects.equals(existingQty, requestQty);
        }
        return existingQty.compareTo(requestQty) == 0;
    }

    /**
     * 生成物料和产品状态组合键。
     *
     * @param materialCode  物料编码
     * @param productStatus 产品状态/示方类型
     * @return 组合键
     */
    private String buildMaterialStatusKey(String materialCode, String productStatus) {
        return StringUtils.defaultString(materialCode).trim() + "|"
                + StringUtils.defaultString(productStatus).trim();
    }

    /**
     * 生成已回填调整槽位键。
     *
     * @param row         月计划行
     * @param adjustCount 调整序号
     * @return 槽位键
     */
    private String buildAdjustSlotKey(LhDayPlanAdjustRequire row, int adjustCount) {
        return this.buildMaterialStatusKey(row.getMaterialCode(), row.getProductStatus()) + "|" + adjustCount;
    }

    /**
     * 去掉胶名 AQ 前缀。
     *
     * @param rawMaterialName 原材料名称
     * @return 页面展示胶名
     */
    private String removeAqPrefix(String rawMaterialName) {
        String materialName = StringUtils.trim(rawMaterialName);
        return materialName.startsWith("AQ") ? materialName.substring(2) : materialName;
    }

    /**
     * 构建统一分页响应。
     *
     * @param rows  当前页数据
     * @param total 总数
     * @return 分页响应
     */
    private TableDataInfo buildTableData(List<LhDayPlanAdjustRequire> rows, long total) {
        TableDataInfo tableDataInfo = new TableDataInfo();
        tableDataInfo.setCode(HttpStatus.SUCCESS);
        tableDataInfo.setRows(rows);
        tableDataInfo.setTotal(total);
        tableDataInfo.setMsg(I18nUtil.getMessage("common.msg.base.query.success"));
        return tableDataInfo;
    }

    @Override
    public String[] getQueryFormulas() {
        return new String[0];
    }

    @Override
    public List<LhDayPlanAdjustVo> getMonthPlanLhDayAdjustList(YearMonth yearMonth, List<String> factoryList, List<String> materialCodeList) {
        if (null == yearMonth || CollectionUtils.isEmpty(factoryList)) {
            return Collections.emptyList();
        }
        List<LhDayPlanAdjustRequire> monthLhDayPlanAdjustRequireList = Lists.newArrayList();
        int year = yearMonth.getYear();
        int month = yearMonth.getMonthValue();
        for (String factoryCode : factoryList) {
            MpFactoryProductionVersion version = getFinalProductionVersion(factoryCode, year, month);
            List<LhDayPlanAdjustRequire> singleFactoryList = getProductionVersionYearMonthLhDayPlanAdjustInfo(version, materialCodeList);
            if (CollectionUtils.isNotEmpty(singleFactoryList)) {
                monthLhDayPlanAdjustRequireList.addAll(singleFactoryList);
            }
        }
        if (CollectionUtils.isEmpty(monthLhDayPlanAdjustRequireList)) {
            return Collections.emptyList();
        }
        List<LhDayPlanAdjustVo> resultList = BeanCopyUtils.copyBeanList(monthLhDayPlanAdjustRequireList, LhDayPlanAdjustVo.class);
        return resultList;
    }

    @Override
    protected String getDocTypeCode() {
        return "0";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("0");
        return sysDocType;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("factoryCode", "yearMonth", "materialCode", "productStatus", "adjustCount");
    }

    /**
     * 获取定稿排产版本
     *
     * @param factoryCode 分厂编码
     * @param year        年份
     * @param month       月份
     * @return 定稿排产版本，不存在返回null
     */
    private MpFactoryProductionVersion getFinalProductionVersion(String factoryCode, int year, int month) {
        if (StringUtils.isBlank(factoryCode)) {
            return null;
        }
        LambdaQueryWrapper<MpFactoryProductionVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MpFactoryProductionVersion::getFactoryCode, factoryCode)
                .eq(MpFactoryProductionVersion::getYear, year)
                .eq(MpFactoryProductionVersion::getMonth, month)
                .eq(MpFactoryProductionVersion::getIsFinal, "1")
                .eq(MpFactoryProductionVersion::getIsDelete, DeleteFlagEnum.NORMAL.getCode())
                .orderByDesc(MpFactoryProductionVersion::getUpdateTime)
                .orderByDesc(MpFactoryProductionVersion::getId)
                .last("LIMIT 1");
        return productionVersionMapper.selectOne(wrapper);
    }

    /**
     * 根据排产版本号，获取对应年月的硫化日计划调整信息
     *
     * @param version          排产版本信息
     * @param materialCodeList 需要查询的Sku信息
     * @return
     */
    private List<LhDayPlanAdjustRequire> getProductionVersionYearMonthLhDayPlanAdjustInfo(MpFactoryProductionVersion version, List<String> materialCodeList) {
        if (null == version) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<LhDayPlanAdjustRequire> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LhDayPlanAdjustRequire::getFactoryCode, version.getFactoryCode())
                .eq(LhDayPlanAdjustRequire::getYear, version.getYear())
                .eq(LhDayPlanAdjustRequire::getMonth, version.getMonth())
                .in(CollectionUtils.isNotEmpty(materialCodeList), LhDayPlanAdjustRequire::getMaterialCode, materialCodeList)
                .eq(LhDayPlanAdjustRequire::getIsDelete, DeleteFlagEnum.NORMAL.getCode());
        List<LhDayPlanAdjustRequire> dataResult = lhDayPlanAdjustRequireMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(dataResult)) {
            return Collections.emptyList();
        }
        return dataResult;
    }
}
