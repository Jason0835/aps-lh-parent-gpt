package com.zlt.aps.cx.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.api.gateway.system.service.ISysDictDataCacheService;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.SysDictData;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.domain.CellStyle;
import com.zlt.aps.common.core.utils.ExcelUtils;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.cx.api.domain.entity.CxStructureTreadConfig;
import com.zlt.aps.cx.entity.config.CxKeyProduct;
import com.zlt.aps.cx.entity.config.CxParamConfig;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.cx.entity.schedule.LhScheduleResult;
import com.zlt.aps.cx.mapper.*;
import com.zlt.aps.cx.service.CxScheduleDetailService;
import com.zlt.aps.cx.service.CxScheduleResultService;
import com.zlt.aps.cx.vo.CxScheduleResultTemplateImportVO;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.maindata.mapper.MdmMaterialConsumeDetailMapper;
import com.zlt.aps.maindata.mapper.MdmMaterialInfoEntityMapper;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import com.zlt.aps.mp.api.domain.entity.MdmMaterialConsumeDetail;
import com.zlt.aps.mp.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.mp.api.domain.entity.MpStructureAllocation;
import com.zlt.aps.mp.api.service.IFactoryMonthPlanProductionFinalResultRemoteService;
import com.zlt.aps.mp.api.service.IMpStructureAllocationRemoteService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.common.utils.PubUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 成型排程结果服务实现类
 *
 * @author APS Team
 */
@Slf4j
@Service
public class CxScheduleResultServiceImpl extends AbstractDocService<CxScheduleResult>
        implements CxScheduleResultService {

    @Autowired
    private CxScheduleResultMapper cxScheduleResultMapper;

    @Autowired
    private CxScheduleDetailService cxScheduleDetailService;

    @Autowired
    private IMpStructureAllocationRemoteService mpStructureAllocationRemoteService;

    @Autowired
    private IFactoryMonthPlanProductionFinalResultRemoteService factoryMonthPlanProductionFinalResultRemoteService;

    @Autowired
    private ISysDictDataCacheService sysDictDataCacheService;

    @Autowired
    private LhScheduleResultMapper lhScheduleResultMapper;

    @Autowired
    private LhFinishQtyMapper lhFinishQtyMapper;

    @Autowired
    private CxKeyProductMapper cxKeyProductMapper;

    @Autowired
    private CxStructureTreadConfigMapper cxStructureTreadConfigMapper;

    @Resource
    private CxParamConfigMapper cxParamConfigMapper;

    @Resource
    private MdmMaterialConsumeDetailMapper mdmMaterialConsumeDetailMapper;

    @Autowired
    private MdmMaterialInfoEntityMapper materialInfoEntityMapper;

    @Override
    public List<CxScheduleResult> listByScheduleDate(LocalDate scheduleDate) {
        return cxScheduleResultMapper.selectList(new LambdaQueryWrapper<CxScheduleResult>()
                .eq(CxScheduleResult::getScheduleDate, scheduleDate.atStartOfDay())
                .orderByAsc(CxScheduleResult::getCxMachineCode));
    }

    @Override
    public List<CxScheduleResult> listByLhScheduleIds(List<Long> lhScheduleIds) {
        if (CollectionUtils.isEmpty(lhScheduleIds)) {
            return Collections.emptyList();
        }
        List<Long> queryIds = lhScheduleIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(queryIds)) {
            return Collections.emptyList();
        }
        QueryWrapper<CxScheduleResult> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("IS_DELETE", "0");
        queryWrapper.and(wrapper -> {
            boolean first = true;
            for (Long lhScheduleId : queryIds) {
                // LH_SCHEDULE_IDS 主要保存为逗号分隔字符串，同时兼容历史数据中的中文逗号、斜杠和分号。
                // 统一转成英文逗号后再使用 FIND_IN_SET，避免 ID=1 误匹配 10、11。
                String findInSetSql = "FIND_IN_SET({0}, REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(LH_SCHEDULE_IDS, '，', ','), '/', ','), '；', ','), ';', ','), ' ', ''))";
                if (first) {
                    wrapper.apply(findInSetSql, String.valueOf(lhScheduleId));
                    first = false;
                } else {
                    wrapper.or().apply(findInSetSql, String.valueOf(lhScheduleId));
                }
            }
        });
        return cxScheduleResultMapper.selectList(queryWrapper);
    }

    @Override
    public AjaxResult importData(List<CxScheduleResult> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<CxScheduleResult> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        String uniqueMsg = I18nUtil.getMessage("import.validated.unique");

        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            CxScheduleResult docEntity = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, docEntity);
            ImportExcelValidatedUtils.validatedRepeat(list, docEntity, i, 2, importLogId, validated);
            if (CollectionUtils.isNotEmpty(validated)) {
                failureNum++;
                docEntity.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }

        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            CxScheduleResult docEntity = list.get(i);
            if (docEntity.getId() != null && docEntity.getId() == -999L) {
                continue;
            }

            if (StringUtils.isBlank(docEntity.getCxMachineCode())) {
                failureNum++;
                String message = I18nUtil.getMessage("ui.data.alert.cxScheduleResult.machineCodeRequired");
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                        errorNum, String.format(message, errorNum), importErrorLogs);
                continue;
            }

            if (docEntity.getScheduleDate() == null) {
                failureNum++;
                String message = I18nUtil.getMessage("ui.data.alert.cxScheduleResult.scheduleDateRequired");
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                        errorNum, String.format(message, errorNum), importErrorLogs);
                continue;
            }

            if (checkUnique(docEntity).equals(UserConstants.UNIQUE)) {
                importList.add(docEntity);
                successNum++;
            } else {
                if (updateSupport) {
                    QueryWrapper<CxScheduleResult> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("CX_MACHINE_CODE", docEntity.getCxMachineCode());
                    queryWrapper.eq("SCHEDULE_DATE", docEntity.getScheduleDate());
                    queryWrapper.eq("ORDER_NO", docEntity.getOrderNo());
                    CxScheduleResult existEntity = cxScheduleResultMapper.selectOne(queryWrapper);
                    if (existEntity != null) {
                        docEntity.setId(existEntity.getId());
                        importList.add(docEntity);
                        successNum++;
                    }
                } else {
                    failureNum++;
                    ImportExcelValidatedUtils.addImportErrorLog(importLogId, errorNum,
                            String.format(uniqueMsg, errorNum), importErrorLogs);
                }
            }
        }

        if (CollectionUtils.isEmpty(importList)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        }

        for (CxScheduleResult entity : importList) {
            if (entity.getId() != null) {
                cxScheduleResultMapper.updateById(entity);
            } else {
                cxScheduleResultMapper.insert(entity);
            }
        }

        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

    @Override
    public String checkUnique(CxScheduleResult entity) {
        QueryWrapper<CxScheduleResult> queryWrapper = new QueryWrapper<>();
        queryWrapper.ne(PubUtil.isNotEmpty(entity.getFieldValueByFieldName("id")), "ID", entity.getFieldValueByFieldName("id"));
        queryWrapper.eq("CX_MACHINE_CODE", entity.getCxMachineCode());
        queryWrapper.eq("SCHEDULE_DATE", entity.getScheduleDate());
        queryWrapper.eq("ORDER_NO", entity.getOrderNo());

        if (cxScheduleResultMapper.selectCount(queryWrapper) > 0) {
            return UserConstants.NOT_UNIQUE;
        } else {
            return UserConstants.UNIQUE;
        }
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("cxMachineCode", "scheduleDate", "orderNo");
    }

    @Override
    protected String getDocTypeCode() {
        return "CX_SCHEDULE_RESULT";
    }

    @Override
    public byte[] exportData(List<CxScheduleResult> list, Date scheduleDate) {
        java.io.InputStream inputStream = this.getClass().getClassLoader()
                .getResourceAsStream("excelModel/cxjhtemplate.xls");
        if (Objects.isNull(inputStream)) {
            throw new ServiceException("成型计划导入模板不存在");
        }
        List<CxScheduleResult> exportList = Objects.isNull(list) ? Collections.emptyList() : list;
        Map<String, Object> tableMap = buildExportTableMap(exportList, scheduleDate);
        List<List<Map<String, Object>>> excelDataList = new ArrayList<>();
        excelDataList.add(buildExportDataList(exportList));
        return ExcelUtils.writeMultiList(inputStream, 1, tableMap, excelDataList);
    }

    /**
     * 导出成型余量数据（含多个Sheet页：成型余量 + 成型日计划 + 硫化产量 + 成型结构切换等）。
     * 填充模式与 LH 模块一致：使用 CxExport.xlsx 统一多Sheet模板，
     * 依次调用 writeMultiList 写入各 sheetIndex，保留模板原始样式。
     *
     * @param queryVO 查询条件，按成型排程结果列表查询口径筛选数据
     * @param fileName 导出文件名，保留用于对齐远程调用契约
     * @return 成型余量Excel文件字节数组
     */
    @Override
    public byte[] exportCxRemainQty(CxScheduleResult queryVO, String fileName) {
        List<CxScheduleResult> list = cxScheduleResultMapper.selectList(buildCxRemainQtyQueryWrapper(queryVO));
        List<CxScheduleResult> exportList = Objects.isNull(list) ? Collections.emptyList() : list;

        InputStream inputStream = this.getClass().getClassLoader()
                .getResourceAsStream("excelModel/CxExport.xlsx");
        if (Objects.isNull(inputStream)) {
            throw new ServiceException("成型导出模板不存在");
        }

        Map<String, String> recipeTypeMap = loadRecipeTypeDictMap();

        Date scheduleDate = exportList.stream()
                .map(CxScheduleResult::getScheduleDate)
                .filter(Objects::nonNull)
                .max(Date::compareTo)
                .orElse(DateUtil.date());

        Map<String, BigDecimal> totalDailyPlanQtyMap = buildTotalDailyPlanQtyMap(exportList);
        Map<String, BigDecimal> todayNightFinishQtyMap = buildTodayNightFinishQtyMap(exportList, scheduleDate);
        Map<String, Map<String, String>> smallGlueMaps = buildSmallGlueMaps(exportList);
        Map<String, String> smallGlueMap = smallGlueMaps.getOrDefault("smallGlue", Collections.emptyMap());
        Map<String, String> placeholderMap = smallGlueMaps.getOrDefault("placeholder", Collections.emptyMap());

        Map<String, String> shiftCapacitiesMap = buildShiftCapacitiesMap();

        Set<String> keyProductEmbryoCodes = loadKeyProductEmbryoCodes();

        // Sheet 0: 成型余量-按机台
        Map<String, Object> remainQtyTableMap = new HashMap<>(16);
        List<List<Map<String, Object>>> remainQtyDataList = new ArrayList<>();
        remainQtyDataList.add(buildCxRemainQtyExportDataList(exportList));
        byte[] exportBytes = ExcelUtils.writeMultiList(inputStream, 0, remainQtyTableMap, remainQtyDataList);

        // Sheet 1: 成型日计划
        Map<String, Object> planTableMap = buildCxTemplateTableMap(exportList);
        List<List<Map<String, Object>>> planDataList = new ArrayList<>();
        List<Map<String, Object>> planRows = buildCxTemplateDataList(exportList, recipeTypeMap, totalDailyPlanQtyMap, todayNightFinishQtyMap, smallGlueMap, placeholderMap, shiftCapacitiesMap, keyProductEmbryoCodes);
        planDataList.add(planRows);

        // 为小计行添加 DAEEF3 背景色标识 + 胎胚余量<400 红色背景
        List<CellStyle> cellStyleList = new ArrayList<>();
        int templateListStartRow = 4;
        for (int i = 0; i < planRows.size(); i++) {
            Map<String, Object> rowMap = planRows.get(i);
            int rowNum = templateListStartRow + i;

            if ("小计".equals(rowMap.get("cxMachineCode"))) {
                cellStyleList.add(new CellStyle(
                        rowNum, rowNum,
                        0, 59,
                        "#DAEEF3", true, true, null));
            } else {
                Object cxRemainVal = rowMap.get("cxRemainQty");
                if (cxRemainVal instanceof Number) {
                    BigDecimal remainQty = new BigDecimal(cxRemainVal.toString());
                    if (remainQty.compareTo(new BigDecimal("400")) < 0) {
                        cellStyleList.add(new CellStyle(
                                rowNum, rowNum,
                                8, 8,
                                null, true, false, null, "#FF0000"));
                    }
                }
            }
        }
        if (!cellStyleList.isEmpty()) {
            planTableMap.put("CELL_STYLE", cellStyleList);
        }

        inputStream = new ByteArrayInputStream(exportBytes);
        exportBytes = ExcelUtils.writeMultiList(inputStream, 1, planTableMap, planDataList);

        // Sheet 7: 成型结构切换
        Map<String, Object> structureTableMap = new HashMap<>();
        // 结构切换Sheet需要查询当月更大范围的排程数据，确保前后结构的班次记录都能覆盖
        LocalDate scheduleLocalDate = cn.hutool.core.date.DateUtil.toLocalDateTime(scheduleDate).toLocalDate();
        List<CxScheduleResult> monthExportList = queryMonthScheduleResultsForStructure(scheduleLocalDate);
        List<List<Map<String, Object>>> structureDataList = buildStructureChangeSheetData(queryVO, structureTableMap, monthExportList);
        inputStream = new ByteArrayInputStream(exportBytes);
        exportBytes = ExcelUtils.writeMultiList(inputStream, 7, structureTableMap, structureDataList);

        return exportBytes;
    }

    /**
     * 构建成型结构切换Sheet的数据列表（用于写入 CxExport.xlsx 的 Sheet 7）。
     * 同时将颜色标识（CELL_STYLE）写入 tableMap，供 writeMultiList 渲染单元格背景色。
     *
     * @param queryVO 查询条件
     * @param tableMap 表头/样式数据容器，方法会向其中写入 CELL_STYLE
     * @param exportList 成型排程结果列表，用于计算余量和班次日期
     * @return 结构切换数据列表
     */
    private List<List<Map<String, Object>>> buildStructureChangeSheetData(CxScheduleResult queryVO,
                                                                          Map<String, Object> tableMap,
                                                                          List<CxScheduleResult> exportList) {
        MpStructureAllocation structureQuery = buildStructureAllocationQuery(queryVO);
        TableDataInfo structureDataInfo = mpStructureAllocationRemoteService.list(structureQuery);
        List<MpStructureAllocation> structureList = structureDataInfo != null
                ? convertToMpStructureAllocationList(structureDataInfo.getRows())
                : Collections.emptyList();

        if (CollectionUtils.isEmpty(structureList)) {
            List<List<Map<String, Object>>> emptyList = new ArrayList<>();
            emptyList.add(new ArrayList<>());
            return emptyList;
        }

        Map<String, List<MpStructureAllocation>> machineGroupMap = structureList.stream()
                .filter(Objects::nonNull)
                .filter(s -> StringUtils.isNotBlank(s.getCxMachineCode()))
                .collect(Collectors.groupingBy(
                        s -> s.getCxMachineCode().trim(),
                        LinkedHashMap::new,
                        Collectors.toList()));
        machineGroupMap.entrySet().removeIf(entry -> entry.getValue().size() < 2);

        LocalDate scheduleDate = queryVO != null && queryVO.getScheduleDate() != null
                ? cn.hutool.core.date.DateUtil.toLocalDateTime(queryVO.getScheduleDate()).toLocalDate()
                : LocalDate.now();

        List<Map<String, Object>> dataList = buildStructureChangeDataListV2(
                machineGroupMap, scheduleDate, exportList);

        List<CellStyle> cellStyleList = buildCellStyleListForStructureChange(dataList);
        if (PubUtil.isNotEmpty(cellStyleList)) {
            tableMap.put("CELL_STYLE", cellStyleList);
        }

        List<List<Map<String, Object>>> excelDataList = new ArrayList<>();
        excelDataList.add(dataList);
        return excelDataList;
    }

    /**
     * 加载示方类型字典，构建 code → label 映射。
     */
    private Map<String, String> loadRecipeTypeDictMap() {
        List<SysDictData> dictList = sysDictDataCacheService.getType("biz_construction_stage");
        if (CollectionUtils.isEmpty(dictList)) {
            return Collections.emptyMap();
        }
        return dictList.stream()
                .filter(d -> StringUtils.isNotEmpty(d.getDictValue()))
                .collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel,
                        (a, b) -> a, LinkedHashMap::new));
    }

    /**
     * 构建表头占位符数据。
     */
    private Map<String, Object> buildCxTemplateTableMap(List<CxScheduleResult> list) {
        Map<String, Object> tableMap = new LinkedHashMap<>();

        Date scheduleDate = list.stream()
                .map(CxScheduleResult::getScheduleDate)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        if (scheduleDate != null) {
            java.time.LocalDate baseDate = cn.hutool.core.date.DateUtil.toLocalDateTime(scheduleDate).toLocalDate();
            java.time.LocalDate d1 = baseDate.minusDays(2);
            java.time.LocalDate d2 = baseDate.minusDays(1);
            java.time.LocalDate d3 = baseDate;
            java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("MM/dd");

            tableMap.put("shiftDate1", "早班 Ca sáng " + d1.format(fmt));
            tableMap.put("shiftDate2", "中班 Ca chiều " + d1.format(fmt));
            tableMap.put("shiftDate3", "夜班 Ca đêm " + d2.format(fmt));
            tableMap.put("shiftDate4", "早班 Ca sáng " + d2.format(fmt));
            tableMap.put("shiftDate5", "中班 Ca chiều " + d2.format(fmt));
            tableMap.put("shiftDate6", "夜班 Ca đêm " + d3.format(fmt));
            tableMap.put("shiftDate7", "早班 Ca sáng " + d3.format(fmt));
            tableMap.put("shiftDate8", "中班 Ca chiều " + d3.format(fmt));

            tableMap.put("yearmonthday", cn.hutool.core.date.DateUtil.format(scheduleDate, "yyyy年MM月dd日"));
        }

        BigDecimal[] planTotals = new BigDecimal[9];
        for (int i = 1; i <= 8; i++) {
            planTotals[i] = BigDecimal.ZERO;
        }
        for (CxScheduleResult item : list) {
            planTotals[1] = safeAdd(planTotals[1], item.getClass1PlanQty());
            planTotals[2] = safeAdd(planTotals[2], item.getClass2PlanQty());
            planTotals[3] = safeAdd(planTotals[3], item.getClass3PlanQty());
            planTotals[4] = safeAdd(planTotals[4], item.getClass4PlanQty());
            planTotals[5] = safeAdd(planTotals[5], item.getClass5PlanQty());
            planTotals[6] = safeAdd(planTotals[6], item.getClass6PlanQty());
            planTotals[7] = safeAdd(planTotals[7], item.getClass7PlanQty());
            planTotals[8] = safeAdd(planTotals[8], item.getClass8PlanQty());
        }
        for (int i = 1; i <= 8; i++) {
            tableMap.put("class" + i + "PlanQtyTotal", zeroToEmpty(planTotals[i]));
        }

        return tableMap;
    }

    /**
     * 构建列表数据，按机台分组并在每组末尾插入小计行。
     */
    private List<Map<String, Object>> buildCxTemplateDataList(List<CxScheduleResult> list, Map<String, String> recipeTypeMap,
                                                               Map<String, BigDecimal> totalDailyPlanQtyMap,
                                                               Map<String, BigDecimal> todayNightFinishQtyMap,
                                                               Map<String, String> smallGlueMap,
                                                               Map<String, String> placeholderMap,
                                                               Map<String, String> shiftCapacitiesMap,
                                                               Set<String> keyProductEmbryoCodes) {
        List<CxScheduleResult> exportList = Objects.isNull(list) ? Collections.emptyList() : list;

        Map<String, List<CxScheduleResult>> groupMap = exportList.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(
                        item -> PubUtil.isNotEmpty(item.getCxMachineCode()) ? item.getCxMachineCode() : "",
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<Map<String, Object>> dataList = new ArrayList<>();
        for (Map.Entry<String, List<CxScheduleResult>> entry : groupMap.entrySet()) {
            List<CxScheduleResult> groupList = entry.getValue();
            groupList.sort(Comparator.comparing(
                    item -> PubUtil.isNotEmpty(item.getMaterialCode()) ? item.getMaterialCode() : "",
                    String::compareTo));

            for (CxScheduleResult item : groupList) {
                dataList.add(buildCxTemplateRow(item, recipeTypeMap, totalDailyPlanQtyMap, todayNightFinishQtyMap, smallGlueMap, placeholderMap, shiftCapacitiesMap, keyProductEmbryoCodes));
            }
            dataList.add(buildCxTemplateSubtotalRow(groupList));
        }

        return dataList;
    }

    /**
     * 构建一行明细数据。
     */
    private Map<String, Object> buildCxTemplateRow(CxScheduleResult item, Map<String, String> recipeTypeMap,
                                                      Map<String, BigDecimal> totalDailyPlanQtyMap,
                                                      Map<String, BigDecimal> todayNightFinishQtyMap,
                                                      Map<String, String> smallGlueMap,
                                                      Map<String, String> placeholderMap,
                                                      Map<String, String> shiftCapacitiesMap,
                                                      Set<String> keyProductEmbryoCodes) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("cxMachineCode", item.getCxMachineCode());
        row.put("structureName", item.getStructureName());
        row.put("embryoCode", item.getEmbryoCode());
        row.put("materialDesc", item.getMaterialDesc());
        row.put("mainMaterialDesc", item.getMainMaterialDesc());
        row.put("materialCode", item.getMaterialCode());
        row.put("cxRemainQty", item.getCxRemainQty());
        row.put("lhRemainQty", item.getLhRemainQty());
        row.put("totalStock", item.getTotalStock());
        row.put("lhClassQty", item.getLhClassQty());

        boolean keyProduct = keyProductEmbryoCodes != null
                && keyProductEmbryoCodes.contains(StringUtils.defaultString(item.getEmbryoCode()).trim());

        row.put("class1PlanQty", zeroToEmpty(item.getClass1PlanQty()));
        row.put("class1FinishQty", zeroToEmpty(item.getClass1FinishQty()));
        row.put("class1Analysis", item.getClass1Analysis());
        row.put("class1RecipeType", dictLabel(recipeTypeMap, item.getClass1RecipeType()));
        row.put("class1RecipeNo", item.getClass1RecipeNo());
        row.put("class1Remark", keyProduct && isPositivePlan(item.getClass1PlanQty()) ? "SPQT" : "");

        row.put("class2PlanQty", zeroToEmpty(item.getClass2PlanQty()));
        row.put("class2FinishQty", zeroToEmpty(item.getClass2FinishQty()));
        row.put("class2Analysis", item.getClass2Analysis());
        row.put("class2RecipeType", dictLabel(recipeTypeMap, item.getClass2RecipeType()));
        row.put("class2RecipeNo", item.getClass2RecipeNo());
        row.put("class2Remark", keyProduct && isPositivePlan(item.getClass2PlanQty()) ? "SPQT" : "");

        row.put("class3PlanQty", zeroToEmpty(item.getClass3PlanQty()));
        row.put("class3FinishQty", zeroToEmpty(item.getClass3FinishQty()));
        row.put("class3Analysis", item.getClass3Analysis());
        row.put("class3RecipeType", dictLabel(recipeTypeMap, item.getClass3RecipeType()));
        row.put("class3RecipeNo", item.getClass3RecipeNo());
        row.put("class3Remark", keyProduct && isPositivePlan(item.getClass3PlanQty()) ? "SPQT" : "");

        row.put("class4PlanQty", zeroToEmpty(item.getClass4PlanQty()));
        row.put("class4FinishQty", zeroToEmpty(item.getClass4FinishQty()));
        row.put("class4Analysis", item.getClass4Analysis());
        row.put("class4RecipeType", dictLabel(recipeTypeMap, item.getClass4RecipeType()));
        row.put("class4RecipeNo", item.getClass4RecipeNo());
        row.put("class4Remark", keyProduct && isPositivePlan(item.getClass4PlanQty()) ? "SPQT" : "");

        row.put("class5PlanQty", zeroToEmpty(item.getClass5PlanQty()));
        row.put("class5FinishQty", zeroToEmpty(item.getClass5FinishQty()));
        row.put("class5Analysis", item.getClass5Analysis());
        row.put("class5RecipeType", dictLabel(recipeTypeMap, item.getClass5RecipeType()));
        row.put("class5RecipeNo", item.getClass5RecipeNo());
        row.put("class5Remark", keyProduct && isPositivePlan(item.getClass5PlanQty()) ? "SPQT" : "");

        row.put("class6PlanQty", zeroToEmpty(item.getClass6PlanQty()));
        row.put("class6FinishQty", zeroToEmpty(item.getClass6FinishQty()));
        row.put("class6Analysis", item.getClass6Analysis());
        row.put("class6RecipeType", dictLabel(recipeTypeMap, item.getClass6RecipeType()));
        row.put("class6RecipeNo", item.getClass6RecipeNo());
        row.put("class6Remark", keyProduct && isPositivePlan(item.getClass6PlanQty()) ? "SPQT" : "");

        row.put("class7PlanQty", zeroToEmpty(item.getClass7PlanQty()));
        row.put("class7FinishQty", zeroToEmpty(item.getClass7FinishQty()));
        row.put("class7Analysis", item.getClass7Analysis());
        row.put("class7RecipeType", dictLabel(recipeTypeMap, item.getClass7RecipeType()));
        row.put("class7RecipeNo", item.getClass7RecipeNo());
        row.put("class7Remark", keyProduct && isPositivePlan(item.getClass7PlanQty()) ? "SPQT" : "");

        row.put("class8PlanQty", zeroToEmpty(item.getClass8PlanQty()));
        row.put("class8FinishQty", zeroToEmpty(item.getClass8FinishQty()));
        row.put("class8Analysis", item.getClass8Analysis());
        row.put("class8RecipeType", dictLabel(recipeTypeMap, item.getClass8RecipeType()));
        row.put("class8RecipeNo", item.getClass8RecipeNo());
        row.put("class8Remark", keyProduct && isPositivePlan(item.getClass8PlanQty()) ? "SPQT" : "");

        BigDecimal totalPlan = sumPlan(item);
        BigDecimal totalFinish = sumFinish(item);
        row.put("totalPlanQty", zeroToEmpty(sumLast3ShiftsPlan(item)));
        row.put("totalFinishQty", zeroToEmpty(totalFinish));
        row.put("dailyPlanQty", zeroToEmpty(totalPlan));
        row.put("remark", item.getRemark());
        row.put("lhMachineQty", countLhScheduleIds(item.getLhScheduleIds()));

        BigDecimal tdpq = totalDailyPlanQtyMap.get(item.getLhScheduleIds());
        row.put("totalDailyPlanQty", zeroToEmpty(tdpq));

        String tnfKey = StringUtils.defaultString(item.getFactoryCode()).trim() + "|"
                + StringUtils.defaultString(item.getMaterialCode()).trim();
        BigDecimal tnfq = todayNightFinishQtyMap.get(tnfKey);
        row.put("todayNightFinishQty", zeroToEmpty(tnfq));

        BigDecimal ylSum = (tnfq != null ? tnfq : BigDecimal.ZERO)
                .subtract(tdpq != null ? tdpq : BigDecimal.ZERO);
        row.put("ylSum", zeroToEmpty(ylSum));

        String embryoCode = item.getEmbryoCode();
        String smallGlueVal = smallGlueMap.getOrDefault(embryoCode, "");
        row.put("smallGlue", smallGlueVal);
        row.put("placeholder", smallGlueVal);

        String shiftCapKey = StringUtils.defaultString(item.getStructureName()).trim() + "|"
                + StringUtils.defaultString(embryoCode).trim();
        row.put("shiftCapacities", shiftCapacitiesMap.getOrDefault(shiftCapKey, ""));

        return row;
    }

    /**
     * 字典转义：根据 code 返回 label，code为空时返回空串。
     */
    private String dictLabel(Map<String, String> dictMap, String code) {
        if (StringUtils.isEmpty(code) || CollectionUtils.sizeIsEmpty(dictMap)) {
            return "";
        }
        return dictMap.getOrDefault(code, code);
    }

    /**
     * 构建小计行。
     */
    private Map<String, Object> buildCxTemplateSubtotalRow(List<CxScheduleResult> groupList) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("cxMachineCode", "小计");

        BigDecimal[] planSums = new BigDecimal[9];
        BigDecimal[] finishSums = new BigDecimal[9];
        for (int i = 1; i <= 8; i++) {
            planSums[i] = BigDecimal.ZERO;
            finishSums[i] = BigDecimal.ZERO;
        }
        for (CxScheduleResult item : groupList) {
            planSums[1] = safeAdd(planSums[1], item.getClass1PlanQty());
            planSums[2] = safeAdd(planSums[2], item.getClass2PlanQty());
            planSums[3] = safeAdd(planSums[3], item.getClass3PlanQty());
            planSums[4] = safeAdd(planSums[4], item.getClass4PlanQty());
            planSums[5] = safeAdd(planSums[5], item.getClass5PlanQty());
            planSums[6] = safeAdd(planSums[6], item.getClass6PlanQty());
            planSums[7] = safeAdd(planSums[7], item.getClass7PlanQty());
            planSums[8] = safeAdd(planSums[8], item.getClass8PlanQty());

            finishSums[1] = safeAdd(finishSums[1], item.getClass1FinishQty());
            finishSums[2] = safeAdd(finishSums[2], item.getClass2FinishQty());
            finishSums[3] = safeAdd(finishSums[3], item.getClass3FinishQty());
            finishSums[4] = safeAdd(finishSums[4], item.getClass4FinishQty());
            finishSums[5] = safeAdd(finishSums[5], item.getClass5FinishQty());
            finishSums[6] = safeAdd(finishSums[6], item.getClass6FinishQty());
            finishSums[7] = safeAdd(finishSums[7], item.getClass7FinishQty());
            finishSums[8] = safeAdd(finishSums[8], item.getClass8FinishQty());
        }

        for (int i = 1; i <= 8; i++) {
            row.put("class" + i + "PlanQty", zeroToEmpty(planSums[i]));
            row.put("class" + i + "FinishQty", zeroToEmpty(finishSums[i]));
        }

        BigDecimal totalPlan = BigDecimal.ZERO;
        BigDecimal totalFinish = BigDecimal.ZERO;
        for (int i = 1; i <= 8; i++) {
            totalPlan = safeAdd(totalPlan, planSums[i]);
            totalFinish = safeAdd(totalFinish, finishSums[i]);
        }
        row.put("totalPlanQty", zeroToEmpty(safeAdd(safeAdd(planSums[6], planSums[7]), planSums[8])));
        row.put("totalFinishQty", zeroToEmpty(totalFinish));
        row.put("dailyPlanQty", zeroToEmpty(totalPlan));

        return row;
    }

    private BigDecimal sumPlan(CxScheduleResult item) {
        return safeAdd(safeAdd(safeAdd(safeAdd(safeAdd(safeAdd(safeAdd(
                item.getClass1PlanQty(), item.getClass2PlanQty()),
                item.getClass3PlanQty()), item.getClass4PlanQty()),
                item.getClass5PlanQty()), item.getClass6PlanQty()),
                item.getClass7PlanQty()), item.getClass8PlanQty());
    }

    /**
     * 汇总最后3个班次（class6、class7、class8）的计划量。
     *
     * @param item 成型排程结果
     * @return 最后3个班次计划量之和
     */
    private BigDecimal sumLast3ShiftsPlan(CxScheduleResult item) {
        return safeAdd(safeAdd(
                item.getClass6PlanQty(), item.getClass7PlanQty()),
                item.getClass8PlanQty());
    }

    private BigDecimal sumFinish(CxScheduleResult item) {
        return safeAdd(safeAdd(safeAdd(safeAdd(safeAdd(safeAdd(safeAdd(
                item.getClass1FinishQty(), item.getClass2FinishQty()),
                item.getClass3FinishQty()), item.getClass4FinishQty()),
                item.getClass5FinishQty()), item.getClass6FinishQty()),
                item.getClass7FinishQty()), item.getClass8FinishQty());
    }

    private BigDecimal safeAdd(BigDecimal a, BigDecimal b) {
        if (a == null && b == null) return BigDecimal.ZERO;
        if (a == null) return b;
        if (b == null) return a;
        return a.add(b);
    }

    /**
     * 计划量/完成量为null或0时返回空字符串，否则返回原值。
     * 用于导出Excel时避免显示无意义的0。
     *
     * @param val 数值
     * @return 空字符串或原值
     */
    private Object zeroToEmpty(BigDecimal val) {
        if (val == null || val.compareTo(BigDecimal.ZERO) == 0) {
            return "";
        }
        return val;
    }

    /**
     * 统计硫化排程任务序号(逗号拼接)中的ID数量。
     * 例如 "5266,5267" 返回 2。
     *
     * @param lhScheduleIds 逗号拼接的ID字符串
     * @return ID数量
     */
    private int countLhScheduleIds(String lhScheduleIds) {
        if (StringUtils.isEmpty(lhScheduleIds)) {
            return 0;
        }
        return (int) Arrays.stream(lhScheduleIds.split("[,，]"))
                .map(String::trim)
                .filter(StringUtils::isNotEmpty)
                .count();
    }

    /**
     * 通过 lhScheduleIds 查询 LH 排程结果，汇总 totalDailyPlanQty。
     * 处理逻辑：解析每个 CX 行的 lhScheduleIds（逗号拼接），
     * 批量查询对应的 LhScheduleResult，按 lhScheduleIds 分组累加 totalDailyPlanQty。
     *
     * @param list 成型排程结果列表
     * @return key=lhScheduleIds逗号串, value=汇总后的总计划数量
     */
    private Map<String, BigDecimal> buildTotalDailyPlanQtyMap(List<CxScheduleResult> list) {
        if (PubUtil.isEmpty(list)) {
            return Collections.emptyMap();
        }
        // 收集所有唯一的 lhScheduleIds，解析为 Long 集合
        Set<Long> allLhIds = list.stream()
                .map(CxScheduleResult::getLhScheduleIds)
                .filter(StringUtils::isNotEmpty)
                .flatMap(ids -> Arrays.stream(ids.split("[,，]")))
                .map(String::trim)
                .filter(StringUtils::isNotEmpty)
                .map(Long::parseLong)
                .collect(Collectors.toSet());

        if (allLhIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // 批量查询 LH 排程结果
        List<LhScheduleResult> lhResults = lhScheduleResultMapper.selectBatchIds(allLhIds);

        // lhScheduleId → totalDailyPlanQty
        Map<Long, Integer> lhIdToPlanQty = new LinkedHashMap<>();
        Set<Integer> usedQtySet = new HashSet<>();
        for (LhScheduleResult r : lhResults) {
            Integer qty = r.getTotalDailyPlanQty() != null ? r.getTotalDailyPlanQty() : 0;
            if (qty == 0 || !usedQtySet.add(qty)) {
                lhIdToPlanQty.put(r.getId(), 0);
            } else {
                lhIdToPlanQty.put(r.getId(), qty);
            }
        }

        // 按 lhScheduleIds 原字符串汇总
        Map<String, BigDecimal> resultMap = new HashMap<>();
        for (CxScheduleResult item : list) {
            String ids = item.getLhScheduleIds();
            if (StringUtils.isEmpty(ids) || resultMap.containsKey(ids)) {
                continue;
            }
            BigDecimal sum = Arrays.stream(ids.split("[,，]"))
                    .map(String::trim)
                    .filter(StringUtils::isNotEmpty)
                    .map(Long::parseLong)
                    .map(lhIdToPlanQty::get)
                    .filter(Objects::nonNull)
                    .map(BigDecimal::valueOf)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            resultMap.put(ids, sum);
        }
        return resultMap;
    }

    /**
     * 构建硫化产量今天夜班Map，复刻 LH 的 buildTodayNightFinishQtyExportMap 计算逻辑。
     * 计算规则：本月1日~排程日当天T_LH_DAY_FINISH_QTY.dayFinishQty 求和
     * + 排程日当天T_LH_SCHE_FINISH_QTY.class1FinishQty 求和，
     * 按工厂+物料聚合。
     *
     * @param list         成型排程结果列表，用于提取工厂和物料查询范围
     * @param scheduleDate 排程日期（T日）
     * @return key=工厂编码|物料编码, value=今天夜班完成量
     */
    private Map<String, BigDecimal> buildTodayNightFinishQtyMap(List<CxScheduleResult> list, Date scheduleDate) {
        if (PubUtil.isEmpty(list)) {
            return Collections.emptyMap();
        }

        List<String> factoryCodes = list.stream()
                .map(CxScheduleResult::getFactoryCode)
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
        List<String> materialCodes = list.stream()
                .map(CxScheduleResult::getMaterialCode)
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());

        log.info("buildTodayNightFinishQtyMap factoryCodes:{}, materialCodes:{}, scheduleDate:{}",
                factoryCodes, materialCodes, scheduleDate);

        if (factoryCodes.isEmpty() || materialCodes.isEmpty()) {
            log.warn("buildTodayNightFinishQtyMap empty factoryCodes or materialCodes, return empty");
            return Collections.emptyMap();
        }

        Date targetScheduleDate = Objects.nonNull(scheduleDate)
                ? DateUtil.beginOfDay(scheduleDate)
                : DateUtil.beginOfDay(DateUtil.date());
        Date monthStart = DateUtil.beginOfMonth(targetScheduleDate);
        Date nextDayStart = DateUtil.offsetDay(targetScheduleDate, 1);

        log.info("buildTodayNightFinishQtyMap monthStart:{}, targetScheduleDate:{}, nextDayStart:{}",
                monthStart, targetScheduleDate, nextDayStart);

        Map<String, BigDecimal> resultMap = new HashMap<>();

        List<Map<String, Object>> dayFinishList = lhFinishQtyMapper.sumDayFinishQty(
                factoryCodes, materialCodes, monthStart, nextDayStart);
        log.info("buildTodayNightFinishQtyMap dayFinishList size:{}", dayFinishList != null ? dayFinishList.size() : 0);
        for (Map<String, Object> row : dayFinishList) {
            String fCode = (String) row.get("FACTORY_CODE");
            String mCode = (String) row.get("MATERIAL_CODE");
            if (StringUtils.isEmpty(fCode) || StringUtils.isEmpty(mCode)) {
                log.warn("buildTodayNightFinishQtyMap dayFinishList row has empty fCode:{}, mCode:{}, row keys:{}",
                        fCode, mCode, row.keySet());
                continue;
            }
            String key = fCode.trim() + "|" + mCode.trim();
            Object totalObj = row.get("TOTAL_FINISH_QTY");
            BigDecimal val = totalObj != null ? new BigDecimal(totalObj.toString()) : BigDecimal.ZERO;
            resultMap.merge(key, val, BigDecimal::add);
        }

        List<Map<String, Object>> scheFinishList = lhFinishQtyMapper.sumScheFinishQty(
                factoryCodes, materialCodes, targetScheduleDate, nextDayStart);
        log.info("buildTodayNightFinishQtyMap scheFinishList size:{}", scheFinishList != null ? scheFinishList.size() : 0);
        for (Map<String, Object> row : scheFinishList) {
            String fCode = (String) row.get("FACTORY_CODE");
            String mCode = (String) row.get("MATERIAL_CODE");
            if (StringUtils.isEmpty(fCode) || StringUtils.isEmpty(mCode)) {
                log.warn("buildTodayNightFinishQtyMap scheFinishList row has empty fCode:{}, mCode:{}, row keys:{}",
                        fCode, mCode, row.keySet());
                continue;
            }
            String key = fCode.trim() + "|" + mCode.trim();
            Object totalObj = row.get("TOTAL_FINISH_QTY");
            BigDecimal val = totalObj != null ? new BigDecimal(totalObj.toString()) : BigDecimal.ZERO;
            resultMap.merge(key, val, BigDecimal::add);
        }

        log.info("buildTodayNightFinishQtyMap resultMap size:{}, keys:{}", resultMap.size(), resultMap.keySet());

        return resultMap;
    }

    /**
     * 构建小胶种和占位符映射。
     * <p>取值逻辑与 Sheet0 成型余量-按机台 的 smallGlue 完全一致：
     * 从 CxParamConfig 读取 SYS04010002，加 AQ 前缀后精确匹配 MdmMaterialConsumeDetail，
     * 再根据胎胚编码查询 MdmMaterialInfo，拼接 规格/花纹 展示胶种。</p>
     *
     * @param exportList 成型排程结果列表
     * @return key=smallGlue/placeholder, value=embryoCode→字符串的映射
     */
    private Map<String, Map<String, String>> buildSmallGlueMaps(List<CxScheduleResult> exportList) {
        Map<String, Map<String, String>> result = new HashMap<>(2);
        result.put("smallGlue", new HashMap<>());
        result.put("placeholder", new HashMap<>());

        if (PubUtil.isEmpty(exportList)) {
            return result;
        }

        // 从参数配置表读取胶种类型编码
        CxParamConfig config = cxParamConfigMapper.selectOne(
                new LambdaQueryWrapper<CxParamConfig>()
                        .eq(CxParamConfig::getParamCode, "SYS04010002")
                        .eq(CxParamConfig::getIsActive, 1));

        if (config == null || StringUtils.isBlank(config.getParamValue())) {
            return result;
        }

        // 加 AQ 前缀后精确匹配 CHILD_MATERIAL_NAME
        List<String> codes = Arrays.stream(config.getParamValue().split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .map(item -> "AQ" + item)
                .collect(Collectors.toList());

        if (codes.isEmpty()) {
            return result;
        }

        String factoryCode = exportList.stream()
                .map(CxScheduleResult::getFactoryCode)
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .orElse(null);

        List<MdmMaterialConsumeDetail> consumeDetails = mdmMaterialConsumeDetailMapper.selectList(
                new LambdaQueryWrapper<MdmMaterialConsumeDetail>()
                        .eq(BaseEntity::getIsDelete, YesOrNoEnum.NO.getCode())
                        .eq(factoryCode != null, MdmMaterialConsumeDetail::getFactoryCode, factoryCode)
                        .in(MdmMaterialConsumeDetail::getChildMaterialName, codes));

        if (CollectionUtils.isEmpty(consumeDetails)) {
            return result;
        }

        // 收集胎胚编码，查询 MdmMaterialInfo 关联规格+花纹
        List<String> embryoCodeList = consumeDetails.stream()
                .map(MdmMaterialConsumeDetail::getEmbryoCode)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());

        Map<String, List<MdmMaterialInfo>> materialInfoMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(embryoCodeList)) {
            LambdaQueryWrapper<MdmMaterialInfo> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(BaseEntity::getIsDelete, YesOrNoEnum.NO.getCode())
                    .eq(factoryCode != null, MdmMaterialInfo::getFactoryCode, factoryCode)
                    .in(MdmMaterialInfo::getEmbryoCode, embryoCodeList);
            List<MdmMaterialInfo> mdmMaterialInfoList = materialInfoEntityMapper.selectList(queryWrapper);
            if (CollectionUtils.isNotEmpty(mdmMaterialInfoList)) {
                materialInfoMap = mdmMaterialInfoList.stream()
                        .filter(i -> StringUtils.isNotBlank(i.getEmbryoCode()))
                        .collect(Collectors.groupingBy(MdmMaterialInfo::getEmbryoCode));
            }
        }

        // 构建 embryoCode → 规格/花纹 映射
        Map<String, List<MdmMaterialInfo>> finalMaterialInfoMap = materialInfoMap;
        Map<String, String> glueMap = consumeDetails.stream()
                .filter(d -> StringUtils.isNotBlank(d.getEmbryoCode()))
                .collect(Collectors.toMap(
                        MdmMaterialConsumeDetail::getEmbryoCode,
                        item -> {
                            List<MdmMaterialInfo> materialInfoList = finalMaterialInfoMap.getOrDefault(item.getEmbryoCode(), new ArrayList<>());
                            List<String> resultList = materialInfoList.stream()
                                    .map(i -> StringUtils.defaultIfBlank(i.getSpecifications(), "") + "/" + StringUtils.defaultIfBlank(i.getPattern(), ""))
                                    .distinct()
                                    .collect(Collectors.toList());
                            return String.join(",", resultList);
                        },
                        (a, b) -> a));

        glueMap.forEach((embryoCode, val) -> {
            // smallGlue 去掉前两个字符（AQ前缀），placeholder 同步
            String displayVal = val.length() > 2 ? val.substring(2) : val;
            result.get("smallGlue").put(embryoCode, displayVal);
            result.get("placeholder").put(embryoCode, displayVal);
        });

        return result;
    }

    /**
     * 构建整车胎面条数映射，依据 structureCode|embryoCode 关联 CxStructureTreadConfig，
     * 取 treadCount 作为整车条数。取数逻辑与 ScheduleServiceImpl.loadStructureTreadConfigs 一致。
     *
     * @return key=结构编码|胎胚编码, value=整车胎面条数
     */
    private Map<String, String> buildShiftCapacitiesMap() {
        List<CxStructureTreadConfig> treadConfigs = cxStructureTreadConfigMapper.selectList(
                new LambdaQueryWrapper<CxStructureTreadConfig>()
                        .eq(CxStructureTreadConfig::getIsDelete, "0"));
        if (CollectionUtils.isEmpty(treadConfigs)) {
            return Collections.emptyMap();
        }
        Map<String, String> result = new HashMap<>();
        for (CxStructureTreadConfig config : treadConfigs) {
            if (config.getStructureCode() != null && config.getTreadCount() != null
                    && StringUtils.isNotBlank(config.getEmbryoCode())) {
                String key = config.getStructureCode().trim() + "|" + config.getEmbryoCode().trim();
                result.put(key, String.valueOf(config.getTreadCount()));
            }
        }
        return result;
    }

    /**
     * 加载关键产品胎胚编码集合。
     * 取数逻辑与 ScheduleServiceImpl.loadKeyProducts 一致：查询 T_CX_KEY_PRODUCT，
     * 过滤 isActive = 1 且 isDelete = 0 的记录，收集胚胎编码。
     *
     * @return 关键产品胎胚编码集合
     */
    private Set<String> loadKeyProductEmbryoCodes() {
        List<CxKeyProduct> keyProducts = cxKeyProductMapper.selectList(
                new LambdaQueryWrapper<CxKeyProduct>()
                        .eq(CxKeyProduct::getIsActive, 1)
                        .eq(CxKeyProduct::getIsDelete, "0"));
        if (CollectionUtils.isEmpty(keyProducts)) {
            return Collections.emptySet();
        }
        return keyProducts.stream()
                .map(CxKeyProduct::getEmbryoCode)
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .collect(Collectors.toSet());
    }

    /**
     * 判断计划量是否为有效值（非 null 且不为 0），
     * 用于决定是否需要标注 SPQT。
     */
    private boolean isPositivePlan(BigDecimal val) {
        return val != null && val.compareTo(BigDecimal.ZERO) != 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult importScheduleTemplate(List<CxScheduleResultTemplateImportVO> list,
                                              CxScheduleResult result, boolean updateSupport, Long logId) {
        if (Objects.isNull(result) || Objects.isNull(result.getScheduleDate())) {
            return AjaxResult.error("导入条件中的排程日期不能为空");
        }
        if (Objects.isNull(list) || list.isEmpty()) {
            return AjaxResult.error("导入文件未读取到有效明细行");
        }

        Date scheduleDate = cn.hutool.core.date.DateUtil.beginOfDay(result.getScheduleDate());
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        int successNum = 0;
        int failureNum = 0;

        for (int i = 0; i < list.size(); i++) {
            int rowNum = i + 2;
            CxScheduleResultTemplateImportVO row = list.get(i);
            if (Objects.isNull(row)) {
                failureNum++;
                ImportExcelValidatedUtils.addImportErrorLog(logId, rowNum,
                        "第" + rowNum + "行数据为空", importErrorLogs);
                continue;
            }
            row.setScheduleDate(scheduleDate);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(logId, rowNum, row);
            ImportExcelValidatedUtils.validatedRepeat(list, row, i, 2, logId, validated,
                    "cxMachineCode", "materialCode");
            if (PubUtil.isNotEmpty(validated)) {
                failureNum++;
                row.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }

        List<String> machineCodes = list.stream()
                .filter(Objects::nonNull)
                .filter(item -> !Objects.equals(item.getId(), -999L))
                .map(CxScheduleResultTemplateImportVO::getCxMachineCode)
                .filter(StringUtils::isNotBlank).map(String::trim).distinct()
                .collect(Collectors.toList());
        List<String> materialCodes = list.stream()
                .filter(Objects::nonNull)
                .filter(item -> !Objects.equals(item.getId(), -999L))
                .map(CxScheduleResultTemplateImportVO::getMaterialCode)
                .filter(StringUtils::isNotBlank).map(String::trim).distinct()
                .collect(Collectors.toList());

        Map<String, CxScheduleResult> existMap = new LinkedHashMap<>();
        if (!machineCodes.isEmpty() && !materialCodes.isEmpty()) {
            List<CxScheduleResult> exists = cxScheduleResultMapper.selectList(
                    new LambdaQueryWrapper<CxScheduleResult>()
                            .eq(CxScheduleResult::getScheduleDate, scheduleDate)
                            .in(CxScheduleResult::getCxMachineCode, machineCodes)
                            .in(CxScheduleResult::getMaterialCode, materialCodes));
            existMap = exists.stream().collect(Collectors.toMap(
                    this::buildImportUniqueKey,
                    item -> item,
                    (oldValue, newValue) -> oldValue,
                    LinkedHashMap::new));
        }

        Set<String> importUniqueKeys = new HashSet<>();

        for (int i = 0; i < list.size(); i++) {
            CxScheduleResultTemplateImportVO row = list.get(i);
            int rowNum = i + 2;
            if (Objects.isNull(row) || Objects.equals(row.getId(), -999L)) {
                continue;
            }

            if (StringUtils.isBlank(row.getCxMachineCode())) {
                failureNum++;
                ImportExcelValidatedUtils.addImportErrorLog(logId, ImportErrorTypeEnums.OTHERS.getCode(),
                        rowNum, "第" + rowNum + "行 成型机台编号不能为空", importErrorLogs);
                continue;
            }
            if (StringUtils.isBlank(row.getMaterialCode())) {
                failureNum++;
                ImportExcelValidatedUtils.addImportErrorLog(logId, ImportErrorTypeEnums.OTHERS.getCode(),
                        rowNum, "第" + rowNum + "行 物料编号不能为空", importErrorLogs);
                continue;
            }

            String uniqueKey = scheduleDate.getTime() + "|"
                    + row.getCxMachineCode().trim() + "|"
                    + row.getMaterialCode().trim();
            if (!importUniqueKeys.add(uniqueKey)) {
                failureNum++;
                ImportExcelValidatedUtils.addImportErrorLog(logId, ImportErrorTypeEnums.OTHERS.getCode(),
                        rowNum, "第" + rowNum + "行 机台+物料在导入文件中重复", importErrorLogs);
                continue;
            }

            String dbUniqueKey = buildImportUniqueKey(row.getCxMachineCode(), scheduleDate, row.getMaterialCode());
            CxScheduleResult target = existMap.get(dbUniqueKey);
            boolean isInsert = Objects.isNull(target);

            if (isInsert) {
                target = new CxScheduleResult();
                target.setDataSource("2");
                target.setIsRelease("0");
                target.setProductionStatus("0");
            }
            target.setScheduleDate(scheduleDate);
            target.setCxMachineCode(row.getCxMachineCode().trim());
            target.setCxMachineName(row.getCxMachineName());
            target.setEmbryoCode(row.getEmbryoCode());
            target.setMaterialCode(row.getMaterialCode().trim());
            target.setMaterialDesc(row.getMaterialDesc());
            target.setMainMaterialDesc(row.getMainMaterialDesc());
            target.setStructureName(row.getStructureName());
            target.setBomDataVersion(row.getBomDataVersion());
            target.setOrderNo(row.getOrderNo());
            target.setCxBatchNo(row.getCxBatchNo());
            target.setIsRelease(row.getIsRelease());
            target.setDataSource(row.getDataSource());

            target.setClass1PlanQty(row.getClass1PlanQty());
            target.setClass1FinishQty(row.getClass1FinishQty());
            target.setClass1Analysis(row.getClass1Analysis());
            target.setClass1RecipeType(row.getClass1RecipeType());
            target.setClass1RecipeNo(row.getClass1RecipeNo());

            target.setClass2PlanQty(row.getClass2PlanQty());
            target.setClass2FinishQty(row.getClass2FinishQty());
            target.setClass2Analysis(row.getClass2Analysis());
            target.setClass2RecipeType(row.getClass2RecipeType());
            target.setClass2RecipeNo(row.getClass2RecipeNo());

            target.setClass3PlanQty(row.getClass3PlanQty());
            target.setClass3FinishQty(row.getClass3FinishQty());
            target.setClass3Analysis(row.getClass3Analysis());
            target.setClass3RecipeType(row.getClass3RecipeType());
            target.setClass3RecipeNo(row.getClass3RecipeNo());

            target.setClass4PlanQty(row.getClass4PlanQty());
            target.setClass4FinishQty(row.getClass4FinishQty());
            target.setClass4Analysis(row.getClass4Analysis());
            target.setClass4RecipeType(row.getClass4RecipeType());

            target.setClass5PlanQty(row.getClass5PlanQty());
            target.setClass5FinishQty(row.getClass5FinishQty());
            target.setClass5Analysis(row.getClass5Analysis());
            target.setClass5RecipeType(row.getClass5RecipeType());

            target.setClass6PlanQty(row.getClass6PlanQty());
            target.setClass6FinishQty(row.getClass6FinishQty());
            target.setClass6Analysis(row.getClass6Analysis());
            target.setClass6RecipeType(row.getClass6RecipeType());

            target.setClass7PlanQty(row.getClass7PlanQty());
            target.setClass7FinishQty(row.getClass7FinishQty());
            target.setClass7Analysis(row.getClass7Analysis());
            target.setClass7RecipeType(row.getClass7RecipeType());

            target.setClass8PlanQty(row.getClass8PlanQty());
            target.setClass8FinishQty(row.getClass8FinishQty());
            target.setClass8Analysis(row.getClass8Analysis());
            target.setClass8RecipeType(row.getClass8RecipeType());

            target.setTotalStock(row.getTotalStock());
            target.setLhMachineCode(row.getLhMachineCode());
            target.setCxRemainQty(row.getCxRemainQty());
            target.setLhRemainQty(row.getLhRemainQty());
            target.setLhClassQty(row.getLhClassQty());

            if (isInsert) {
                cxScheduleResultMapper.insert(target);
                existMap.put(dbUniqueKey, target);
            } else {
                cxScheduleResultMapper.updateById(target);
            }
            successNum++;
        }

        if (failureNum > 0) {
            return AjaxResult.error(
                    I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum,
                    importErrorLogs);
        }
        return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
    }

    private String buildImportUniqueKey(CxScheduleResult entity) {
        return buildImportUniqueKey(entity.getCxMachineCode(), entity.getScheduleDate(), entity.getMaterialCode());
    }

    private String buildImportUniqueKey(String cxMachineCode, Date scheduleDate, String materialCode) {
        return StringUtils.defaultString(cxMachineCode).trim() + "|"
                + cn.hutool.core.date.DateUtil.format(cn.hutool.core.date.DateUtil.beginOfDay(scheduleDate), "yyyy-MM-dd") + "|"
                + StringUtils.defaultString(materialCode).trim();
    }

    /**
     * 构建成型余量导出查询条件。
     *
     * @param queryVO 查询条件，来源于UI导出请求
     * @return 成型排程结果Lambda查询条件
     */
    private LambdaQueryWrapper<CxScheduleResult> buildCxRemainQtyQueryWrapper(CxScheduleResult queryVO) {
        CxScheduleResult query = Objects.isNull(queryVO) ? new CxScheduleResult() : queryVO;
        return new LambdaQueryWrapper<CxScheduleResult>()
                .eq(PubUtil.isNotEmpty(query.getScheduleDate()), CxScheduleResult::getScheduleDate, query.getScheduleDate())
                .like(PubUtil.isNotEmpty(query.getCxMachineCode()), CxScheduleResult::getCxMachineCode, query.getCxMachineCode())
                .like(PubUtil.isNotEmpty(query.getMaterialCode()), CxScheduleResult::getMaterialCode, query.getMaterialCode())
                .like(PubUtil.isNotEmpty(query.getMaterialDesc()), CxScheduleResult::getMaterialDesc, query.getMaterialDesc())
                .like(PubUtil.isNotEmpty(query.getMainMaterialDesc()), CxScheduleResult::getMainMaterialDesc, query.getMainMaterialDesc())
                .eq(PubUtil.isNotEmpty(query.getOrderNo()), CxScheduleResult::getOrderNo, query.getOrderNo())
                .eq(PubUtil.isNotEmpty(query.getProductionStatus()), CxScheduleResult::getProductionStatus, query.getProductionStatus())
                .eq(PubUtil.isNotEmpty(query.getIsRelease()), CxScheduleResult::getIsRelease, query.getIsRelease())
                .orderByAsc(CxScheduleResult::getCxMachineCode)
                .orderByAsc(CxScheduleResult::getMaterialCode);
    }

    /**
     * 查询当月更大范围的成型排程结果，用于结构切换Sheet的前后结构余量和班次日期计算。
     * 不限制scheduleDate精确匹配，而是查询整个月的数据，确保前后结构的所有班次记录都能覆盖。
     *
     * @param scheduleDate 排程日期（取年月范围）
     * @return 当月成型排程结果列表
     */
    private List<CxScheduleResult> queryMonthScheduleResultsForStructure(LocalDate scheduleDate) {
        if (scheduleDate == null) {
            scheduleDate = LocalDate.now();
        }
        // 当月第一天
        LocalDate monthStart = scheduleDate.withDayOfMonth(1);
        // 当月最后一天
        LocalDate monthEnd = scheduleDate.withDayOfMonth(scheduleDate.lengthOfMonth());
        Date startDate = cn.hutool.core.date.DateUtil.parse(monthStart.toString(), "yyyy-MM-dd");
        Date endDate = cn.hutool.core.date.DateUtil.parse(monthEnd.toString(), "yyyy-MM-dd");
        // 结束日期设为当天23:59:59以包含整天
        endDate = cn.hutool.core.date.DateUtil.endOfDay(endDate);

        List<CxScheduleResult> result = cxScheduleResultMapper.selectList(
                new LambdaQueryWrapper<CxScheduleResult>()
                        .between(CxScheduleResult::getScheduleDate, startDate, endDate)
                        .eq(CxScheduleResult::getIsDelete, "0")
                        .orderByAsc(CxScheduleResult::getCxMachineCode)
                        .orderByAsc(CxScheduleResult::getMaterialCode));
        log.info("查询当月成型排程结果用于结构切换: scheduleDate={}, 月份范围={}~{}, 返回{}条",
                scheduleDate, monthStart, monthEnd, result != null ? result.size() : 0);
        return result != null ? result : Collections.emptyList();
    }

    /**
     * 构建成型余量模板列表数据。
     *
     * @param list 成型排程结果明细列表
     * @return 模板列表行数据，字段名与cxyl.xlsx中的列表占位符保持一致
     */
    private List<Map<String, Object>> buildCxRemainQtyExportDataList(List<CxScheduleResult> list) {
        List<CxScheduleResult> exportList = Objects.isNull(list) ? Collections.emptyList() :
                list.stream().sorted(Comparator.comparing(CxScheduleResult::getCxMachineCode)
                                .thenComparing(CxScheduleResult::getMaterialCode))
                        .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }

        Map<String, List<CxScheduleResult>> groupMap = exportList.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(this::buildCxRemainQtyGroupKey, LinkedHashMap::new, Collectors.toList()));

        // 查询胶种，使用胎胚代码关联，取对应的花纹等
        CxParamConfig config = cxParamConfigMapper.selectOne(
                new LambdaQueryWrapper<CxParamConfig>()
                        .eq(CxParamConfig::getParamCode, "SYS04010002")
                        .eq(CxParamConfig::getIsActive, 1));

        Map<String, String> smallGlueMap = new HashMap<>();
        if (config != null && StringUtils.isNotBlank(config.getParamValue())) {
            List<String> codes = Arrays.stream(config.getParamValue().split(","))
                    .map(String::trim)
                    .filter(StringUtils::isNotBlank)
                    .map(item -> "AQ" + item)
                    .collect(Collectors.toList());
            List<MdmMaterialConsumeDetail> mdmMaterialConsumeDetailList = mdmMaterialConsumeDetailMapper.selectList(new LambdaQueryWrapper<MdmMaterialConsumeDetail>()
                    .eq(BaseEntity::getIsDelete, YesOrNoEnum.NO.getCode())
                    .eq(MdmMaterialConsumeDetail::getFactoryCode, list.get(0).getFactoryCode())
                    .in(MdmMaterialConsumeDetail::getChildMaterialName, codes));

            if(CollectionUtils.isNotEmpty(mdmMaterialConsumeDetailList)) {
                List<String> embryoCodeList = mdmMaterialConsumeDetailList.stream()
                        .map(MdmMaterialConsumeDetail::getEmbryoCode)
                        .filter(StringUtils::isNotBlank)
                        .distinct()
                        .collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(embryoCodeList)) {
                    // 根据胎胚编码查询物料信息，关联规格+花纹展示胶种
                    LambdaQueryWrapper<MdmMaterialInfo> queryWrapper = new LambdaQueryWrapper<>();
                    queryWrapper.eq(BaseEntity::getIsDelete, YesOrNoEnum.NO.getCode())
                            .eq(MdmMaterialInfo::getFactoryCode, list.get(0).getFactoryCode())
                            .in(MdmMaterialInfo::getEmbryoCode, embryoCodeList);
                    List<MdmMaterialInfo> mdmMaterialInfoList = materialInfoEntityMapper.selectList(queryWrapper);
                    Map<String, List<MdmMaterialInfo>> materialInfoMap = new HashMap<>();
                    if (CollectionUtils.isNotEmpty(mdmMaterialInfoList)) {
                        materialInfoMap = mdmMaterialInfoList.stream()
                                .filter(item -> StringUtils.isNotBlank(item.getEmbryoCode()))
                                .collect(Collectors.groupingBy(MdmMaterialInfo::getEmbryoCode));
                    }

                    Map<String, List<MdmMaterialInfo>> finalMaterialInfoMap = materialInfoMap;
                    smallGlueMap = mdmMaterialConsumeDetailList.stream().collect(Collectors.toMap(MdmMaterialConsumeDetail::getEmbryoCode,
                            item -> {
                                List<MdmMaterialInfo> materialInfoList = finalMaterialInfoMap.getOrDefault(item.getEmbryoCode(), new ArrayList<>());
                                List<String> resultList = materialInfoList.stream()
                                        .map(i -> StringUtils.defaultIfBlank(i.getSpecifications(), "") + "/" + StringUtils.defaultIfBlank(i.getPattern(), ""))
                                        .distinct()
                                        .collect(Collectors.toList());
                                return String.join(",", resultList);
                            }));
                }
            }
        }


        List<Map<String, Object>> dataList = new ArrayList<>();
        for (List<CxScheduleResult> groupList : groupMap.values()) {
            if (CollectionUtils.isEmpty(groupList)) {
                continue;
            }
            CxScheduleResult first = groupList.get(0);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("cxMachineCode", first.getCxMachineCode());
            row.put("materialCode", first.getMaterialCode());
            String embryoCode = first.getEmbryoCode();
            row.put("embryoCode", embryoCode);
            row.put("mainMaterialDesc", firstNonBlank(groupList, "mainMaterialDesc"));
            // 胶种展示胎胚对应规格+花纹，在t_mdm_material_consume_detail表存在对应的胶种才展示
            if (smallGlueMap.containsKey(embryoCode)) {
                String smallGlue = smallGlueMap.get(embryoCode);
                smallGlue = smallGlue.substring(2);
                row.put("smallGlue", smallGlue);
            } else {
                row.put("smallGlue", "");
            }
            row.put("cxRemainQty", sumCxRemainQty(groupList));
            row.put("remark", buildCxRemainQtyRemark(groupList));
            dataList.add(row);
        }
        return dataList;
    }

    /**
     * 构建成型余量导出分组键。
     *
     * @param item 成型排程结果明细
     * @return 机台编号和物料编码组成的唯一分组键
     */
    private String buildCxRemainQtyGroupKey(CxScheduleResult item) {
        return StringUtils.defaultString(item.getCxMachineCode()).trim() + "|"
                + StringUtils.defaultString(item.getEmbryoCode()).trim();
    }

    /**
     * 获取分组内第一个非空文本字段。
     *
     * @param list 分组明细列表
     * @param fieldName 字段名称，目前用于胎胚描述取值
     * @return 第一个非空字段值，没有则返回空字符串
     */
    private String firstNonBlank(List<CxScheduleResult> list, String fieldName) {
        for (CxScheduleResult item : list) {
            if ("mainMaterialDesc".equals(fieldName) && StringUtils.isNotBlank(item.getMainMaterialDesc())) {
                return item.getMainMaterialDesc();
            }
        }
        return "";
    }

    /**
     * 合计分组内成型余量。
     *
     * @param list 分组明细列表
     * @return 成型余量合计，空值按0处理
     */
    private BigDecimal sumCxRemainQty(List<CxScheduleResult> list) {
        return list.stream()
                .map(CxScheduleResult::getCxRemainQty)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 构建分组备注。
     *
     * @param list 分组明细列表
     * @return 去重后的备注文本，多个备注使用中文分号拼接
     */
    private String buildCxRemainQtyRemark(List<CxScheduleResult> list) {
        return list.stream()
                .map(CxScheduleResult::getRemark)
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .distinct()
                .collect(Collectors.joining("；"));
    }

    /**
     * 导出成型结构切换数据。
     * 主数据源改为T_MP_STRUCTURE_ALLOCATION（通过Feign获取），
     * 只展示有2条以上结构记录的机台（说明有结构切换），
     * 计算收尾预计时间和开产预计时间。
     * 数据需定稿后才能导出：转产表通过PRODUCTION_VERSION关联T_MP_MONTH_PLAN_PROD_FINAL表，
     * 只有在定稿表中存在对应PRODUCTION_VERSION的数据才允许导出。
     *
     * @param queryVO 查询条件，按成型排程结果列表查询口径筛选数据
     * @param fileName 导出文件名，保留用于对齐远程调用契约
     * @return 成型结构切换Excel文件字节数组
     */
    @Override
    public byte[] exportStructureChange(CxScheduleResult queryVO, String fileName) {
        InputStream inputStream = this.getClass().getClassLoader()
                .getResourceAsStream("excelModel/cxStructureChangeExportTemp.xlsx");
        if (Objects.isNull(inputStream)) {
            throw new ServiceException("成型结构切换导出模板不存在");
        }

        MpStructureAllocation structureQuery = buildStructureAllocationQuery(queryVO);
        TableDataInfo structureDataInfo = mpStructureAllocationRemoteService.list(structureQuery);
        List<MpStructureAllocation> structureList = structureDataInfo != null
                ? convertToMpStructureAllocationList(structureDataInfo.getRows())
                : Collections.emptyList();

        if (CollectionUtils.isEmpty(structureList)) {
            List<List<Map<String, Object>>> excelDataList = new ArrayList<>();
            excelDataList.add(new ArrayList<>());
            return ExcelUtils.writeMultiList(inputStream, 0, new HashMap<>(), excelDataList);
        }

        // 通过PRODUCTION_VERSION关联T_MP_MONTH_PLAN_PROD_FINAL定稿表，过滤出已定稿的数据
        Set<String> validProductionVersions = queryValidProductionVersions(structureQuery);
        if (PubUtil.isNotEmpty(validProductionVersions)) {
            structureList = structureList.stream()
                    .filter(s -> s != null && validProductionVersions.contains(s.getProductionVersion()))
                    .collect(Collectors.toList());
        } else {
            // 定稿表中无数据，则无符合条件的数据可导出
            structureList = Collections.emptyList();
        }

        if (CollectionUtils.isEmpty(structureList)) {
            List<List<Map<String, Object>>> excelDataList = new ArrayList<>();
            excelDataList.add(new ArrayList<>());
            return ExcelUtils.writeMultiList(inputStream, 0, new HashMap<>(), excelDataList);
        }

        Map<String, List<MpStructureAllocation>> machineGroupMap = structureList.stream()
                .filter(Objects::nonNull)
                .filter(s -> StringUtils.isNotBlank(s.getCxMachineCode()))
                .collect(Collectors.groupingBy(
                        s -> s.getCxMachineCode().trim(),
                        LinkedHashMap::new,
                        Collectors.toList()));
        machineGroupMap.entrySet().removeIf(entry -> entry.getValue().size() < 2);

        LocalDate scheduleDate = queryVO != null && queryVO.getScheduleDate() != null
                ? cn.hutool.core.date.DateUtil.toLocalDateTime(queryVO.getScheduleDate()).toLocalDate()
                : LocalDate.now();

        // 查询当月更大范围的成型排程结果，用于计算余量和班次日期（不限制scheduleDate精确匹配）
        List<CxScheduleResult> exportList = queryMonthScheduleResultsForStructure(scheduleDate);

        List<Map<String, Object>> dataList = buildStructureChangeDataListV2(
                machineGroupMap, scheduleDate, exportList);

        Map<String, Object> tableMap = new HashMap<>();
        List<CellStyle> cellStyleList = buildCellStyleListForStructureChange(dataList);
        if (PubUtil.isNotEmpty(cellStyleList)) {
            tableMap.put("CELL_STYLE", cellStyleList);
        }

        List<List<Map<String, Object>>> excelDataList = new ArrayList<>();
        excelDataList.add(dataList);
        return ExcelUtils.writeMultiList(inputStream, 0, tableMap, excelDataList);
    }

    /**
     * 将Feign远程调用返回的LinkedHashMap列表转换为MpStructureAllocation实体列表。
     * Feign反序列化泛型丢失，TableDataInfo.getRows()中的元素实际类型为LinkedHashMap，
     * 直接强转会导致ClassCastException，需使用ObjectMapper.convertValue进行类型转换。
     *
     * @param rows Feign远程调用返回的行数据列表
     * @return MpStructureAllocation实体列表
     */
    private List<MpStructureAllocation> convertToMpStructureAllocationList(List<?> rows) {
        List<MpStructureAllocation> entityList = new ArrayList<>();
        if (PubUtil.isEmpty(rows)) {
            return entityList;
        }
        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        for (Object obj : rows) {
            if (obj instanceof MpStructureAllocation) {
                entityList.add((MpStructureAllocation) obj);
            } else if (obj instanceof Map) {
                MpStructureAllocation entity = objectMapper.convertValue(obj, MpStructureAllocation.class);
                entityList.add(entity);
            }
        }
        return entityList;
    }

    /**
     * 构建结构排产查询条件，从排程结果查询VO转换为结构排产查询对象。
     *
     * @param queryVO 排程结果查询条件
     * @return 结构排产查询对象
     */
    private MpStructureAllocation buildStructureAllocationQuery(CxScheduleResult queryVO) {
        MpStructureAllocation structureQuery = new MpStructureAllocation();
        // 分厂为空时赋默认工厂编码
        String factoryCode = (queryVO != null && StringUtils.isNotBlank(queryVO.getFactoryCode()))
                ? queryVO.getFactoryCode() : FactoryConstant.DEFAULT_FACTORY_CODE;
        structureQuery.setFactoryCode(factoryCode);
        if (queryVO != null) {
            structureQuery.setCxMachineCode(queryVO.getCxMachineCode());
        }
        // 年月参数从排程日期的年月拆出，排程日期为空时默认当前日期
        LocalDate ld = (queryVO != null && queryVO.getScheduleDate() != null)
                ? DateUtil.toLocalDateTime(queryVO.getScheduleDate()).toLocalDate()
                : LocalDate.now();
        structureQuery.setYear(ld.getYear());
        structureQuery.setMonth(ld.getMonthValue());
        return structureQuery;
    }

    /**
     * 查询定稿表T_MP_MONTH_PLAN_PROD_FINAL中有效的PRODUCTION_VERSION集合。
     * 转产表数据需通过PRODUCTION_VERSION关联定稿表，只有定稿表中存在的PRODUCTION_VERSION才允许导出。
     *
     * @param structureQuery 结构排产查询条件（含factoryCode、year、month）
     * @return 定稿表中存在的PRODUCTION_VERSION集合
     */
    private Set<String> queryValidProductionVersions(MpStructureAllocation structureQuery) {
        FactoryMonthPlanProductionFinalResult finalQuery = new FactoryMonthPlanProductionFinalResult();
        finalQuery.setFactoryCode(structureQuery.getFactoryCode());
        finalQuery.setYear(structureQuery.getYear());
        finalQuery.setMonth(structureQuery.getMonth());
        try {
            TableDataInfo finalDataInfo = factoryMonthPlanProductionFinalResultRemoteService.list(finalQuery);
            List<?> rows = finalDataInfo != null ? finalDataInfo.getRows() : null;
            if (PubUtil.isEmpty(rows)) {
                return Collections.emptySet();
            }
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Set<String> productionVersions = new HashSet<>();
            for (Object obj : rows) {
                String productionVersion = null;
                if (obj instanceof FactoryMonthPlanProductionFinalResult) {
                    productionVersion = ((FactoryMonthPlanProductionFinalResult) obj).getProductionVersion();
                } else if (obj instanceof Map) {
                    FactoryMonthPlanProductionFinalResult entity = objectMapper.convertValue(obj, FactoryMonthPlanProductionFinalResult.class);
                    productionVersion = entity.getProductionVersion();
                }
                if (StringUtils.isNotBlank(productionVersion)) {
                    productionVersions.add(productionVersion.trim());
                }
            }
            return productionVersions;
        } catch (Exception e) {
            log.error("查询定稿表PRODUCTION_VERSION异常，factoryCode={}, year={}, month={}",
                    structureQuery.getFactoryCode(), structureQuery.getYear(), structureQuery.getMonth(), e);
            return Collections.emptySet();
        }
    }

    /**
     * 构建成型结构切换模板列表数据（V2版本，基于T_MP_STRUCTURE_ALLOCATION）。
     * 按成型机台分组，每个机台按beginDay排序，
     * 根据排程日期找到当前正在运行的结构（scheduleDate的日落在某条结构的beginDay~endDay区间内），
     * 只取当前运行结构作为切换前结构，下一条作为切换后结构，合并为1条导出记录。
     * 即使当天有多次切换，每个机台也只输出1条记录。
     * 排程日期不在任何结构区间内或当前结构为最后一条（无下一个切换结构）时，该机台不展示。
     *
     * @param machineGroupMap 按机台分组的结构排产数据
     * @param scheduleDate 排程日期
     * @return 模板列表行数据
     */
    private List<Map<String, Object>> buildStructureChangeDataListV2(
            Map<String, List<MpStructureAllocation>> machineGroupMap,
            LocalDate scheduleDate,
            List<CxScheduleResult> exportList) {

        List<Map<String, Object>> dataList = new ArrayList<>();
        int scheduleDayOfMonth = scheduleDate.getDayOfMonth();

        for (Map.Entry<String, List<MpStructureAllocation>> entry : machineGroupMap.entrySet()) {
            String machineCode = entry.getKey();
            List<MpStructureAllocation> structures = entry.getValue().stream()
                    .sorted(Comparator.comparing(MpStructureAllocation::getBeginDay, Comparator.nullsLast(Comparator.naturalOrder())))
                    .collect(Collectors.toList());

            // 根据排程日期找到当前正在运行的结构（scheduleDayOfMonth落在beginDay~endDay区间内）
            int currentIndex = -1;
            for (int i = 0; i < structures.size(); i++) {
                MpStructureAllocation s = structures.get(i);
                if (s.getBeginDay() != null && s.getEndDay() != null
                        && scheduleDayOfMonth >= s.getBeginDay() && scheduleDayOfMonth <= s.getEndDay()) {
                    currentIndex = i;
                    break;
                }
            }

            // 排程日期不在任何结构的beginDay~endDay区间内，说明该机台当天无正在运行的结构，跳过
            if (currentIndex == -1) {
                continue;
            }

            // 当前运行结构是最后一条，没有下一个切换结构，不展示
            if (currentIndex >= structures.size() - 1) {
                continue;
            }

            MpStructureAllocation prevStructure = structures.get(currentIndex);
            MpStructureAllocation nextStructure = structures.get(currentIndex + 1);

            Map<String, Object> row = buildStructureChangeRow(
                    machineCode, prevStructure, nextStructure, scheduleDate, exportList);
            dataList.add(row);
        }

        dataList.sort(Comparator.comparing(
                row -> (String) row.get("_sortKey"),
                Comparator.nullsLast(Comparator.naturalOrder())));

        int rowIndex = 1;
        for (Map<String, Object> row : dataList) {
            row.put("stt", rowIndex++);
        }

        return dataList;
    }

    /**
     * 构建单条结构切换导出行数据。
     *
     * 字段取值说明：
     *   remainQty        - 成型余量，依据机台+前结构从CxScheduleResult汇总计算
     *   receiveEstDate   - 预计收尾时间，取机台+前结构分组中最后一个有值班次的日期
     *   startEstDate     - 预计开产时间，取机台+后结构分组中第一个有值班次的日期
     *   receiveMonthPlan - 收尾月计划时间，取转产表第1条（切换前结构）的结束日期
     *   remark           - 收尾备注，取转产表第1条（切换前结构）的REMARK字段
     *   nextStructure    - 后结构，取合并第2条（切换后结构）的结构名称
     *   startMonthPlan   - 开产月计划时间，取合并第2条（切换后结构）的开始日期
     *   remark2          - 开产备注，取合并第2条（切换后结构）的REMARK字段
     *
     * @param machineCode 成型机台编码
     * @param prevStructure 前结构（当前正在执行的结构，即切换前的结构）
     * @param nextStructure 后结构（即将切换到的结构，即切换后的结构）
     * @param scheduleDate 排程日期
     * @param exportList 成型排程结果列表
     * @return 单行导出数据
     */
    private Map<String, Object> buildStructureChangeRow(
            String machineCode,
            MpStructureAllocation prevStructure,
            MpStructureAllocation nextStructure,
            LocalDate scheduleDate,
            List<CxScheduleResult> exportList) {

        String prevStructureName = StringUtils.defaultString(prevStructure.getStructureName()).trim();
        String nextStructureName = StringUtils.defaultString(nextStructure.getStructureName()).trim();

        int year = prevStructure.getYear() != null ? prevStructure.getYear() : scheduleDate.getYear();
        int month = prevStructure.getMonth() != null ? prevStructure.getMonth() : scheduleDate.getMonthValue();

        LocalDate nextBeginDate = nextStructure.getBeginDay() != null
                ? LocalDate.of(year, month, Math.min(nextStructure.getBeginDay(), LocalDate.of(year, month, 1).lengthOfMonth()))
                : scheduleDate;

        // 按机台+结构对成型排程结果分组，用于计算余量和班次日期
        Map<String, List<CxScheduleResult>> machineStructureGroup = Collections.emptyMap();
        if (CollectionUtils.isNotEmpty(exportList)) {
            machineStructureGroup = exportList.stream()
                    .filter(r -> StringUtils.isNotBlank(r.getCxMachineCode()))
                    .collect(Collectors.groupingBy(
                            r -> r.getCxMachineCode().trim() + "|" + StringUtils.defaultString(r.getStructureName()).trim()));
        }

        String prevKey = machineCode + "|" + prevStructureName;
        String nextKey = machineCode + "|" + nextStructureName;
        List<CxScheduleResult> prevGroupList = machineStructureGroup.getOrDefault(prevKey, Collections.emptyList());
        List<CxScheduleResult> nextGroupList = machineStructureGroup.getOrDefault(nextKey, Collections.emptyList());

        log.debug("结构切换行: machineCode={}, prevStructure={}, nextStructure={}, prevKey={}, nextKey={}, prevGroupSize={}, nextGroupSize={}",
                machineCode, prevStructureName, nextStructureName, prevKey, nextKey, prevGroupList.size(), nextGroupList.size());
        if (machineStructureGroup.isEmpty()) {
            log.warn("结构切换: machineStructureGroup为空, exportList.size={}", exportList.size());
        } else {
            log.debug("结构切换: machineStructureGroup keys={}", machineStructureGroup.keySet());
        }

        // 计算机台+前结构的成型余量（按物料去重后汇总）
        BigDecimal remainQtyVal = calculateStructureRemainQty(prevGroupList);
        String remainQty = remainQtyVal.compareTo(BigDecimal.ZERO) > 0 ? remainQtyVal.toString() : "";

        // 预计收尾时间：前结构分组中最后一个有值班次对应的日期
        String receiveEstDate = findLastShiftDateStr(prevGroupList);

        // 预计开产时间：后结构分组中第一个有值班次对应的日期
        String startEstDate = findFirstShiftDateStr(nextGroupList);

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("stt", 0);
        row.put("machineCode", machineCode);
        row.put("structureSpec", prevStructureName);
        row.put("remainQty", remainQty);
        row.put("receiveEstDate", receiveEstDate);
        row.put("receiveMonthPlan", formatDateFromDay(year, month, prevStructure.getEndDay()));
        row.put("remark", prevStructure.getRemark() != null ? prevStructure.getRemark() : "");
        row.put("nextStructure", nextStructureName);
        row.put("startEstDate", startEstDate);
        row.put("startMonthPlan", formatDateFromDay(year, month, nextStructure.getBeginDay()));
        row.put("remark2", nextStructure.getRemark() != null ? nextStructure.getRemark() : "");
        row.put("traceTd", "");
        row.put("traceSw", "");
        row.put("traceIl", "");
        row.put("traceUb", "");
        row.put("traceBd", "");
        row.put("traceCa", "");
        row.put("traceBe", "");
        row.put("traceCh", "");

        String sortKey = String.format("%04d-%02d-%02d", year, month,
                nextStructure.getBeginDay() != null ? nextStructure.getBeginDay() : 99);
        row.put("_sortKey", sortKey);
        row.put("_nextBeginDate", nextBeginDate);

        return row;
    }

    /**
     * 根据年月和日数字格式化日期为"MM.DD"格式。
     *
     * @param year 年份
     * @param month 月份
     * @param day 日（1-31）
     * @return 格式化字符串
     */
    private String formatDateFromDay(int year, int month, Integer day) {
        if (day == null) {
            return "";
        }
        return String.format("%02d.%02d", month, day);
    }

    /**
     * 获取指定班次的计划量。
     *
     * @param result 成型排程结果
     * @param classIndex 班次索引（1-8）
     * @return 计划量
     */
    private BigDecimal getClassPlanQtyByIndex(CxScheduleResult result, int classIndex) {
        switch (classIndex) {
            case 1: return result.getClass1PlanQty();
            case 2: return result.getClass2PlanQty();
            case 3: return result.getClass3PlanQty();
            case 4: return result.getClass4PlanQty();
            case 5: return result.getClass5PlanQty();
            case 6: return result.getClass6PlanQty();
            case 7: return result.getClass7PlanQty();
            case 8: return result.getClass8PlanQty();
            default: return null;
        }
    }

    /**
     * 获取指定班次的完成量。
     *
     * @param result 成型排程结果
     * @param classIndex 班次索引（1-8）
     * @return 完成量
     */
    private BigDecimal getClassFinishQtyByIndex(CxScheduleResult result, int classIndex) {
        switch (classIndex) {
            case 1: return result.getClass1FinishQty();
            case 2: return result.getClass2FinishQty();
            case 3: return result.getClass3FinishQty();
            case 4: return result.getClass4FinishQty();
            case 5: return result.getClass5FinishQty();
            case 6: return result.getClass6FinishQty();
            case 7: return result.getClass7FinishQty();
            case 8: return result.getClass8FinishQty();
            default: return null;
        }
    }

    /**
     * 判断 BigDecimal 是否有值（非null且大于0）。
     *
     * @param val 数值
     * @return true表示有值
     */
    private boolean hasShiftValue(BigDecimal val) {
        return val != null && val.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 计算机台+结构分组下的成型余量总和（按物料去重，取最大余量后汇总）。
     * 逻辑参考 ScheduleServiceImpl.calculateFormingRemainderMap，基于已排程结果中的 cxRemainQty 汇总。
     *
     * @param groupList 该机台+结构下的成型排程结果列表
     * @return 成型余量总和
     */
    private BigDecimal calculateStructureRemainQty(List<CxScheduleResult> groupList) {
        if (CollectionUtils.isEmpty(groupList)) {
            return BigDecimal.ZERO;
        }
        return groupList.stream()
                .filter(r -> StringUtils.isNotBlank(r.getMaterialCode()))
                .collect(Collectors.groupingBy(CxScheduleResult::getMaterialCode))
                .values().stream()
                .map(materialList -> materialList.stream()
                        .map(CxScheduleResult::getCxRemainQty)
                        .filter(Objects::nonNull)
                        .max(Comparator.naturalOrder())
                        .orElse(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 查找分组记录中最后一个有值班次对应的日期。
     * 遍历每条记录的班次（从class8到class1），找到最后一个planQty或finishQty大于0的班次，
     * 取对应记录中日期最靠后的那条。
     *
     * @param groupList 机台+结构分组记录
     * @return 格式化后的日期字符串（yyyy-MM-dd），无数据返回空串
     */
    private String findLastShiftDateStr(List<CxScheduleResult> groupList) {
        if (CollectionUtils.isEmpty(groupList)) {
            return "";
        }
        Date lastDate = null;
        for (CxScheduleResult r : groupList) {
            if (r.getScheduleDate() == null) {
                continue;
            }
            // 从最后一个班次往前找，找到该记录最后一个有值的班次
            for (int i = 8; i >= 1; i--) {
                if (hasShiftValue(getClassPlanQtyByIndex(r, i)) || hasShiftValue(getClassFinishQtyByIndex(r, i))) {
                    if (lastDate == null || r.getScheduleDate().after(lastDate)) {
                        lastDate = r.getScheduleDate();
                    }
                    break;
                }
            }
        }
        return lastDate != null ? cn.hutool.core.date.DateUtil.format(lastDate, "yyyy-MM-dd") : "";
    }

    /**
     * 查找分组记录中第一个有值班次对应的日期。
     * 遍历每条记录的班次（从class1到class8），找到第一个planQty或finishQty大于0的班次，
     * 取对应记录中日期最靠前的那条。
     *
     * @param groupList 机台+结构分组记录
     * @return 格式化后的日期字符串（yyyy-MM-dd），无数据返回空串
     */
    private String findFirstShiftDateStr(List<CxScheduleResult> groupList) {
        if (CollectionUtils.isEmpty(groupList)) {
            return "";
        }
        Date firstDate = null;
        for (CxScheduleResult r : groupList) {
            if (r.getScheduleDate() == null) {
                continue;
            }
            // 从第一个班次往后找，找到该记录第一个有值的班次
            for (int i = 1; i <= 8; i++) {
                if (hasShiftValue(getClassPlanQtyByIndex(r, i)) || hasShiftValue(getClassFinishQtyByIndex(r, i))) {
                    if (firstDate == null || r.getScheduleDate().before(firstDate)) {
                        firstDate = r.getScheduleDate();
                    }
                    break;
                }
            }
        }
        return firstDate != null ? cn.hutool.core.date.DateUtil.format(firstDate, "yyyy-MM-dd") : "";
    }

    /**
     * 构建成型结构切换导出的单元格样式列表（底色间隔区分）。
     * 规则：根据开产时间(月计划)分组，同一天有2条以上记录的标红，
     * 连续多日都有2条以上时按白、红、白、红交替，
     * 一天只有1条的默认白色。
     *
     * @param dataList 导出数据列表
     * @return 单元格样式列表
     */
    private List<CellStyle> buildCellStyleListForStructureChange(List<Map<String, Object>> dataList) {
        List<CellStyle> cellStyleList = new ArrayList<>();
        if (CollectionUtils.isEmpty(dataList)) {
            return cellStyleList;
        }

        Map<String, List<Integer>> dateGroupMap = new LinkedHashMap<>();
        for (int i = 0; i < dataList.size(); i++) {
            Object nextBeginDate = dataList.get(i).get("_nextBeginDate");
            String dateKey;
            if (nextBeginDate instanceof LocalDate) {
                dateKey = ((LocalDate) nextBeginDate).toString();
            } else {
                dateKey = String.valueOf(dataList.get(i).get("startMonthPlan"));
            }
            dateGroupMap.computeIfAbsent(dateKey, k -> new ArrayList<>()).add(i);
        }

        boolean colorToggle = false;
        String redColor = "#FFC7CE";
        for (Map.Entry<String, List<Integer>> dateEntry : dateGroupMap.entrySet()) {
            List<Integer> rowIndexes = dateEntry.getValue();
            if (rowIndexes.size() >= 2) {
                colorToggle = !colorToggle;
                if (colorToggle) {
                    for (Integer rowIdx : rowIndexes) {
                        cellStyleList.add(new CellStyle(
                                rowIdx + 2, rowIdx + 2, 0, 18,
                                redColor, true));
                    }
                }
            } else {
                colorToggle = false;
            }
        }

        return cellStyleList;
    }

    /**
     * 格式化班次开始时间，用于导出显示。
     *
     * @param date 班次开始时间
     * @return 格式化后的时间字符串，空值返回空串
     */
    private String formatStartTime(Date date) {
        return date != null ? cn.hutool.core.date.DateUtil.format(date, "yyyy-MM-dd HH:mm") : "";
    }

    private Map<String, Object> buildExportTableMap(List<CxScheduleResult> list, Date scheduleDate) {
        Map<String, Object> tableMap = new LinkedHashMap<>();
        tableMap.put("scheduleDate", scheduleDate != null
                ? cn.hutool.core.date.DateUtil.format(scheduleDate, "yyyy-MM-dd") : "");
        tableMap.put("totalCount", list != null ? list.size() : 0);
        return tableMap;
    }

    private List<Map<String, Object>> buildExportDataList(List<CxScheduleResult> list) {
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            CxScheduleResult item = list.get(i);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("index", i + 1);
            row.put("cxMachineCode", item.getCxMachineCode());
            row.put("cxMachineName", item.getCxMachineName());
            row.put("embryoCode", item.getEmbryoCode());
            row.put("materialCode", item.getMaterialCode());
            row.put("materialDesc", item.getMaterialDesc());
            row.put("mainMaterialDesc", item.getMainMaterialDesc());
            row.put("structureName", item.getStructureName());
            row.put("bomDataVersion", item.getBomDataVersion());
            row.put("orderNo", item.getOrderNo());
            row.put("cxBatchNo", item.getCxBatchNo());
            row.put("scheduleDate", item.getScheduleDate());

            row.put("class1PlanQty", zeroToEmpty(item.getClass1PlanQty()));
            row.put("class1FinishQty", zeroToEmpty(item.getClass1FinishQty()));
            row.put("class1Analysis", item.getClass1Analysis());
            row.put("class2PlanQty", zeroToEmpty(item.getClass2PlanQty()));
            row.put("class2FinishQty", zeroToEmpty(item.getClass2FinishQty()));
            row.put("class2Analysis", item.getClass2Analysis());
            row.put("class3PlanQty", zeroToEmpty(item.getClass3PlanQty()));
            row.put("class3FinishQty", zeroToEmpty(item.getClass3FinishQty()));
            row.put("class3Analysis", item.getClass3Analysis());
            row.put("class4PlanQty", zeroToEmpty(item.getClass4PlanQty()));
            row.put("class5PlanQty", zeroToEmpty(item.getClass5PlanQty()));
            row.put("class6PlanQty", zeroToEmpty(item.getClass6PlanQty()));
            row.put("class7PlanQty", zeroToEmpty(item.getClass7PlanQty()));
            row.put("class8PlanQty", zeroToEmpty(item.getClass8PlanQty()));

            row.put("totalStock", item.getTotalStock());
            row.put("lhMachineCode", item.getLhMachineCode());
            row.put("cxRemainQty", item.getCxRemainQty());
            row.put("lhRemainQty", item.getLhRemainQty());
            row.put("lhClassQty", item.getLhClassQty());

            dataList.add(row);
        }
        return dataList;
    }

    @Override
    public void updateReleaseStatus(CxScheduleResult item) {
        CxScheduleResult updateEntity = new CxScheduleResult();
        updateEntity.setId(item.getId());
        updateEntity.setIsRelease(item.getIsRelease());
        cxScheduleResultMapper.updateById(updateEntity);
    }

    @Override
    public List<CxScheduleResult> listByScheduleDateAndFactory(Date scheduleDate, String factoryCode) {
        LambdaQueryWrapper<CxScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CxScheduleResult::getScheduleDate, scheduleDate);
        if (StringUtils.isNotBlank(factoryCode)) {
            wrapper.eq(CxScheduleResult::getFactoryCode, factoryCode);
        }
        wrapper.orderByAsc(CxScheduleResult::getCxMachineCode);
        return cxScheduleResultMapper.selectList(wrapper);
    }

    @Override
    public List<CxScheduleResult> listByIds(List<Long> ids) {
        if (org.apache.commons.collections4.CollectionUtils.isEmpty(ids)) {
            return new ArrayList<>();
        }
        LambdaQueryWrapper<CxScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(CxScheduleResult::getId, ids);
        return cxScheduleResultMapper.selectList(wrapper);
    }
}
