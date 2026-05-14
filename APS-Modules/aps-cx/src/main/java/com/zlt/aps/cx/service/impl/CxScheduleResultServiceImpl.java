package com.zlt.aps.cx.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.api.gateway.system.service.ISysDictDataCacheService;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.SysDictData;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.domain.CellStyle;
import com.zlt.aps.common.core.utils.ExcelUtils;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.cx.entity.schedule.LhScheduleResult;
import com.zlt.aps.cx.mapper.CxScheduleResultMapper;
import com.zlt.aps.cx.mapper.LhFinishQtyMapper;
import com.zlt.aps.cx.mapper.LhScheduleResultMapper;
import com.zlt.aps.cx.service.CxScheduleDetailService;
import com.zlt.aps.cx.service.CxScheduleResultService;
import com.zlt.aps.cx.vo.CxScheduleResultTemplateImportVO;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import com.zlt.aps.mp.api.domain.entity.MpStructureAllocation;
import com.zlt.aps.mp.api.service.IFactoryMonthPlanProductionFinalResultRemoteService;
import com.zlt.aps.mp.api.service.IMpStructureAllocationRemoteService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.common.utils.PubUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
     * 导出成型余量数据（含三个Sheet页：成型余量 + 成型计划明细 + 成型结构切换）。
     * 填充模式：
     *   1. 以成型计划主表模板(cxjhtemplate.xlsx)为基体，writeMultiList填充后作为最终工作簿
     *   2. 成型余量模板(cxyl.xlsx)独立填充后，通过copySheet合并到基体，setSheetOrder移到第一位
     *   3. 成型结构切换模板(cxStructureChangeExportTemp.xlsx)独立填充后，通过copySheet追加到基体末尾
     *
     * @param queryVO 查询条件，按成型排程结果列表查询口径筛选数据
     * @param fileName 导出文件名，保留用于对齐远程调用契约
     * @return 成型余量Excel文件字节数组
     */
    @Override
    public byte[] exportCxRemainQty(CxScheduleResult queryVO, String fileName) {
        List<CxScheduleResult> list = cxScheduleResultMapper.selectList(buildCxRemainQtyQueryWrapper(queryVO));

        // 第一步：以成型计划主表模板为基体，writeMultiList填充后作为最终工作簿
        XSSFWorkbook finalWorkbook = buildFinalWorkbookFromTemplate(list);

        // 第二步：成型余量模板独立填充后，copySheet合并到基体，setSheetOrder移到第一位
        byte[] firstSheetBytes = buildFirstSheetBytes(list);
        try {
            XSSFWorkbook firstWorkbook = new XSSFWorkbook(new ByteArrayInputStream(firstSheetBytes));
            ExcelUtils.copySheet(firstWorkbook, 0, finalWorkbook);
            finalWorkbook.setSheetOrder(finalWorkbook.getSheetName(finalWorkbook.getNumberOfSheets() - 1), 0);
            firstWorkbook.close();
        } catch (Exception e) {
            throw new ServiceException("合并成型余量Sheet失败", e);
        }

        // 第三步：成型结构切换模板独立填充后，copySheet追加到基体末尾（成型计划主表后面）
        byte[] thirdSheetBytes = buildStructureChangeSheetBytes(queryVO);
        try {
            XSSFWorkbook thirdWorkbook = new XSSFWorkbook(new ByteArrayInputStream(thirdSheetBytes));
            ExcelUtils.copySheet(thirdWorkbook, 0, finalWorkbook);
            // 设置第三个sheet名称为"成型结构切换"
            int thirdSheetIndex = finalWorkbook.getNumberOfSheets() - 1;
            finalWorkbook.setSheetName(thirdSheetIndex, "成型结构切换");
            thirdWorkbook.close();
        } catch (Exception e) {
            throw new ServiceException("合并成型结构切换Sheet失败", e);
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            finalWorkbook.write(out);
            finalWorkbook.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new ServiceException("导出Excel失败", e);
        }
    }

    /**
     * 构建第一页（成型余量）数据。
     * 填充模式：独立加载模板 → writeMultiList填充 → 返回byte[]
     *
     * @param list 成型排程结果明细列表
     * @return 成型余量Sheet的字节数组
     */
    private byte[] buildFirstSheetBytes(List<CxScheduleResult> list) {
        InputStream inputStream = this.getClass().getClassLoader()
                .getResourceAsStream("excelModel/cxyl.xlsx");
        if (Objects.isNull(inputStream)) {
            throw new ServiceException("成型余量导出模板不存在");
        }
        Map<String, Object> tableMap = new HashMap<>(16);
        List<List<Map<String, Object>>> excelDataList = new ArrayList<>();
        excelDataList.add(buildCxRemainQtyExportDataList(list));
        return ExcelUtils.writeMultiList(inputStream, 0, tableMap, excelDataList);
    }

    /**
     * 构建第三页（成型结构切换）数据。
     * 填充模式与buildFirstSheetBytes完全一致：独立加载模板 → writeMultiList填充 → 返回byte[]
     * 逻辑来源于exportStructureChange方法，将结构切换数据填充到cxStructureChangeExportTemp.xlsx模板后返回字节数组，
     * 供exportCxRemainQty方法通过copySheet合并到最终工作簿的第3个sheet。
     *
     * @param queryVO 查询条件，按成型排程结果列表查询口径筛选数据
     * @return 成型结构切换Sheet的字节数组
     */
    private byte[] buildStructureChangeSheetBytes(CxScheduleResult queryVO) {
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
                machineGroupMap, scheduleDate);

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
     * 加载模板并通过占位符替换填充数据，返回最终工作簿。
     * 参考 LH 导出模式：writeMultiList 会保留模板中的图片、隐藏列等属性。
     */
    private XSSFWorkbook buildFinalWorkbookFromTemplate(List<CxScheduleResult> list) {
        InputStream templateInput = this.getClass().getClassLoader()
                .getResourceAsStream("excelModel/cxjhtemplate.xlsx");
        if (Objects.isNull(templateInput)) {
            throw new ServiceException("成型计划模板不存在");
        }
        try {
            List<CxScheduleResult> exportList = Objects.isNull(list) ? Collections.emptyList() : list;

            // 加载示方类型字典（biz_construction_stage），用于导出时 code → label 转义
            Map<String, String> recipeTypeMap = loadRecipeTypeDictMap();

            // 提取排程日期，用于 todayNightFinishQty 计算
            Date scheduleDate = exportList.stream()
                    .map(CxScheduleResult::getScheduleDate)
                    .filter(Objects::nonNull)
                    .max(Date::compareTo)
                    .orElse(DateUtil.date());

            // 查询 LH 排程结果，按 lhScheduleIds 汇总 totalDailyPlanQty
            // key=lhScheduleIds逗号串, value=汇总后的总计划数量
            Map<String, BigDecimal> totalDailyPlanQtyMap = buildTotalDailyPlanQtyMap(exportList);

            // 查询 LH 日完成量和班次完成量表，按工厂+物料聚合 todayNightFinishQty
            // key=工厂编码|物料编码, value=今天夜班完成量
            Map<String, BigDecimal> todayNightFinishQtyMap = buildTodayNightFinishQtyMap(exportList, scheduleDate);

            // 构建表头占位符数据（{shiftDate1}~{shiftDate8}、{yearmonthday}等）
            Map<String, Object> tableMap = buildCxTemplateTableMap(exportList);

            // 构建列表数据（按机台分组 + 小计行），key 对应模板中的 {.xxx} 占位符
            List<List<Map<String, Object>>> excelDataList = new ArrayList<>();
            excelDataList.add(buildCxTemplateDataList(exportList, recipeTypeMap, totalDailyPlanQtyMap, todayNightFinishQtyMap));

            // writeMultiList 读取模板 → 替换占位符 → 输出字节
            byte[] bytes = ExcelUtils.writeMultiList(templateInput, 0, tableMap, excelDataList);
            return new XSSFWorkbook(new ByteArrayInputStream(bytes));
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("生成成型计划Sheet失败", e);
        }
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

        return tableMap;
    }

    /**
     * 构建列表数据，按机台分组并在每组末尾插入小计行。
     */
    private List<Map<String, Object>> buildCxTemplateDataList(List<CxScheduleResult> list, Map<String, String> recipeTypeMap,
                                                               Map<String, BigDecimal> totalDailyPlanQtyMap,
                                                               Map<String, BigDecimal> todayNightFinishQtyMap) {
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
                dataList.add(buildCxTemplateRow(item, recipeTypeMap, totalDailyPlanQtyMap, todayNightFinishQtyMap));
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
                                                      Map<String, BigDecimal> todayNightFinishQtyMap) {
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

        row.put("class1PlanQty", zeroToEmpty(item.getClass1PlanQty()));
        row.put("class1FinishQty", zeroToEmpty(item.getClass1FinishQty()));
        row.put("class1Analysis", item.getClass1Analysis());
        row.put("class1RecipeType", dictLabel(recipeTypeMap, item.getClass1RecipeType()));
        row.put("class1RecipeNo", item.getClass1RecipeNo());

        row.put("class2PlanQty", zeroToEmpty(item.getClass2PlanQty()));
        row.put("class2FinishQty", zeroToEmpty(item.getClass2FinishQty()));
        row.put("class2Analysis", item.getClass2Analysis());
        row.put("class2RecipeType", dictLabel(recipeTypeMap, item.getClass2RecipeType()));
        row.put("class2RecipeNo", item.getClass2RecipeNo());

        row.put("class3PlanQty", zeroToEmpty(item.getClass3PlanQty()));
        row.put("class3FinishQty", zeroToEmpty(item.getClass3FinishQty()));
        row.put("class3Analysis", item.getClass3Analysis());
        row.put("class3RecipeType", dictLabel(recipeTypeMap, item.getClass3RecipeType()));
        row.put("class3RecipeNo", item.getClass3RecipeNo());

        row.put("class4PlanQty", zeroToEmpty(item.getClass4PlanQty()));
        row.put("class4FinishQty", zeroToEmpty(item.getClass4FinishQty()));
        row.put("class4Analysis", item.getClass4Analysis());
        row.put("class4RecipeType", dictLabel(recipeTypeMap, item.getClass4RecipeType()));
        row.put("class4RecipeNo", item.getClass4RecipeNo());

        row.put("class5PlanQty", zeroToEmpty(item.getClass5PlanQty()));
        row.put("class5FinishQty", zeroToEmpty(item.getClass5FinishQty()));
        row.put("class5Analysis", item.getClass5Analysis());
        row.put("class5RecipeType", dictLabel(recipeTypeMap, item.getClass5RecipeType()));
        row.put("class5RecipeNo", item.getClass5RecipeNo());

        row.put("class6PlanQty", zeroToEmpty(item.getClass6PlanQty()));
        row.put("class6FinishQty", zeroToEmpty(item.getClass6FinishQty()));
        row.put("class6Analysis", item.getClass6Analysis());
        row.put("class6RecipeType", dictLabel(recipeTypeMap, item.getClass6RecipeType()));
        row.put("class6RecipeNo", item.getClass6RecipeNo());

        row.put("class7PlanQty", zeroToEmpty(item.getClass7PlanQty()));
        row.put("class7FinishQty", zeroToEmpty(item.getClass7FinishQty()));
        row.put("class7Analysis", item.getClass7Analysis());
        row.put("class7RecipeType", dictLabel(recipeTypeMap, item.getClass7RecipeType()));
        row.put("class7RecipeNo", item.getClass7RecipeNo());

        row.put("class8PlanQty", zeroToEmpty(item.getClass8PlanQty()));
        row.put("class8FinishQty", zeroToEmpty(item.getClass8FinishQty()));
        row.put("class8Analysis", item.getClass8Analysis());
        row.put("class8RecipeType", dictLabel(recipeTypeMap, item.getClass8RecipeType()));
        row.put("class8RecipeNo", item.getClass8RecipeNo());

        BigDecimal totalPlan = sumPlan(item);
        BigDecimal totalFinish = sumFinish(item);
        row.put("totalPlanQty", zeroToEmpty(totalPlan));
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
        row.put("totalPlanQty", zeroToEmpty(totalPlan));
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
        Map<Long, Integer> lhIdToPlanQty = lhResults.stream()
                .collect(Collectors.toMap(
                        LhScheduleResult::getId,
                        r -> r.getTotalDailyPlanQty() != null ? r.getTotalDailyPlanQty() : 0,
                        (a, b) -> a));

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
     * 构建成型余量模板列表数据。
     *
     * @param list 成型排程结果明细列表
     * @return 模板列表行数据，字段名与cxyl.xlsx中的列表占位符保持一致
     */
    private List<Map<String, Object>> buildCxRemainQtyExportDataList(List<CxScheduleResult> list) {
        List<CxScheduleResult> exportList = Objects.isNull(list) ? Collections.emptyList() : list;
        Map<String, List<CxScheduleResult>> groupMap = exportList.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(this::buildCxRemainQtyGroupKey, LinkedHashMap::new, Collectors.toList()));

        List<Map<String, Object>> dataList = new ArrayList<>();
        for (List<CxScheduleResult> groupList : groupMap.values()) {
            if (CollectionUtils.isEmpty(groupList)) {
                continue;
            }
            CxScheduleResult first = groupList.get(0);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("cxMachineCode", first.getCxMachineCode());
            row.put("materialCode", first.getMaterialCode());
            row.put("mainMaterialDesc", firstNonBlank(groupList, "mainMaterialDesc"));
            // 小胶种暂未明确来源，按需求先导出空值，避免误用其他业务字段。
            row.put("smallGlue", "");
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
                + StringUtils.defaultString(item.getMaterialCode()).trim();
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

        List<Map<String, Object>> dataList = buildStructureChangeDataListV2(
                machineGroupMap, scheduleDate);

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
            LocalDate scheduleDate) {

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
                    machineCode, prevStructure, nextStructure, scheduleDate);
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
     *   remainQty        - 余量，需成型排程接口提供（包含在提供预计收尾/开产时间的接口中，暂输出空串，待接口对接）
     *   receiveEstDate   - 预计收尾时间，需成型排程接口提供（暂输出空串，待接口对接）
     *   startEstDate     - 预计开产时间，需成型排程接口提供（暂输出空串，待接口对接）
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
     * @return 单行导出数据
     */
    private Map<String, Object> buildStructureChangeRow(
            String machineCode,
            MpStructureAllocation prevStructure,
            MpStructureAllocation nextStructure,
            LocalDate scheduleDate) {

        String prevStructureName = StringUtils.defaultString(prevStructure.getStructureName()).trim();
        String nextStructureName = StringUtils.defaultString(nextStructure.getStructureName()).trim();

        int year = prevStructure.getYear() != null ? prevStructure.getYear() : scheduleDate.getYear();
        int month = prevStructure.getMonth() != null ? prevStructure.getMonth() : scheduleDate.getMonthValue();

        LocalDate nextBeginDate = nextStructure.getBeginDay() != null
                ? LocalDate.of(year, month, Math.min(nextStructure.getBeginDay(), LocalDate.of(year, month, 1).lengthOfMonth()))
                : scheduleDate;

        // TODO: 余量、预计收尾时间和预计开产时间需成型排程接口提供，当前暂输出空串，待接口对接后替换
        String remainQty = "";
        String receiveEstDate = "";
        String startEstDate = "";

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
