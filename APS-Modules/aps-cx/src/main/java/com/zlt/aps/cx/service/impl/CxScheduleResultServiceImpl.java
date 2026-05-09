package com.zlt.aps.cx.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.domain.CellStyle;
import com.zlt.aps.common.core.utils.ExcelUtils;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.cx.mapper.CxScheduleResultMapper;
import com.zlt.aps.cx.service.CxScheduleDetailService;
import com.zlt.aps.cx.service.CxScheduleResultService;
import com.zlt.aps.cx.vo.CxScheduleResultTemplateImportVO;
import com.zlt.aps.mp.api.domain.entity.MpStructureAllocation;
import com.zlt.aps.mp.api.enums.AlternativeTypeEnum;
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
     * 导出成型余量数据（含两个Sheet页：成型余量 + 成型计划明细）。
     *
     * @param queryVO 查询条件，按成型排程结果列表查询口径筛选数据
     * @param fileName 导出文件名，保留用于对齐远程调用契约
     * @return 成型余量Excel文件字节数组
     */
    @Override
    public byte[] exportCxRemainQty(CxScheduleResult queryVO, String fileName) {
        // 按成型排程结果列表的查询口径查询明细数据
        List<CxScheduleResult> list = cxScheduleResultMapper.selectList(buildCxRemainQtyQueryWrapper(queryVO));

        // 构建第一页（成型余量）数据
        byte[] firstSheetBytes = buildFirstSheetBytes(list);

        // 加载第一页结果工作簿
        XSSFWorkbook finalWorkbook;
        try {
            finalWorkbook = new XSSFWorkbook(new ByteArrayInputStream(firstSheetBytes));
        } catch (Exception e) {
            throw new ServiceException("读取成型余量导出结果失败", e);
        }

        // 构建第二页（成型计划明细），写入最终工作簿
        buildSecondSheet(finalWorkbook, list);

        // 输出最终工作簿字节数组
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

        // 按机台+物料合并余量后填充模板
        Map<String, Object> tableMap = new HashMap<>(16);
        List<List<Map<String, Object>>> excelDataList = new ArrayList<>();
        excelDataList.add(buildCxRemainQtyExportDataList(list));
        return ExcelUtils.writeMultiList(inputStream, 0, tableMap, excelDataList);
    }

    /**
     * 构建第二页（成型计划明细），使用cxjhtemplate.xlsx模板，
     * 通过占位符替换方式填充成型排程结果明细数据。
     *
     * @param finalWorkbook 最终输出工作簿，第二页将追加到此工作簿
     * @param list 成型排程结果明细列表
     */
    private void buildSecondSheet(XSSFWorkbook finalWorkbook, List<CxScheduleResult> list) {
        InputStream templateInput = this.getClass().getClassLoader()
                .getResourceAsStream("excelModel/cxjhtemplate.xlsx");
        if (Objects.isNull(templateInput)) {
            throw new ServiceException("成型计划模板不存在");
        }

        try {
            List<CxScheduleResult> exportList = Objects.isNull(list) ? Collections.emptyList() : list;

            // 构建表头占位符数据（{shiftDate1}~{shiftDate8}、{yearmonthday}等）
            Map<String, Object> tableMap = buildCxTemplateTableMap(exportList);

            // 构建列表数据（含按工厂分组的小计行）
            List<List<Map<String, Object>>> excelDataList = new ArrayList<>();
            excelDataList.add(buildCxTemplateDataList(exportList));

            // 使用占位符替换方式生成第二页Excel字节
            byte[] secondSheetBytes = ExcelUtils.writeMultiList(templateInput, 0, tableMap, excelDataList);

            // 加载第二页并复制到最终工作簿
            XSSFWorkbook secondWorkbook = new XSSFWorkbook(new ByteArrayInputStream(secondSheetBytes));
            ExcelUtils.copySheet(secondWorkbook, 0, finalWorkbook);
            secondWorkbook.close();
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("生成成型计划Sheet失败", e);
        }
    }

    /**
     * 构建成型计划模板表头占位符数据。
     *
     * @param list 排程结果列表
     * @return 表头占位符Map，key为模板中的占位符名
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
            // D1 = scheduleDate - 2, D2 = scheduleDate - 1, D3 = scheduleDate
            java.time.LocalDate d1 = baseDate.minusDays(2);
            java.time.LocalDate d2 = baseDate.minusDays(1);
            java.time.LocalDate d3 = baseDate;
            java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("MM/dd");

            tableMap.put("shiftDate1", d1.format(fmt));
            tableMap.put("shiftDate2", d1.format(fmt));
            tableMap.put("shiftDate3", d2.format(fmt));
            tableMap.put("shiftDate4", d2.format(fmt));
            tableMap.put("shiftDate5", d2.format(fmt));
            tableMap.put("shiftDate6", d3.format(fmt));
            tableMap.put("shiftDate7", d3.format(fmt));
            tableMap.put("shiftDate8", d3.format(fmt));

            tableMap.put("yearmonthday", cn.hutool.core.date.DateUtil.format(scheduleDate, "yyyy年MM月dd日"));
        }

        return tableMap;
    }

    /**
     * 构建成型计划模板列表数据，按工厂分组并在每组末尾插入小计行。
     *
     * @param list 排程结果列表
     * @return 列表行数据，每行为一个Map，key对应模板中的{.xxx}占位符
     */
    private List<Map<String, Object>> buildCxTemplateDataList(List<CxScheduleResult> list) {
        List<CxScheduleResult> exportList = Objects.isNull(list) ? Collections.emptyList() : list;

        // 按工厂编码分组，保持顺序
        Map<String, List<CxScheduleResult>> groupMap = exportList.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(
                        item -> PubUtil.isNotEmpty(item.getFactoryCode()) ? item.getFactoryCode() : "",
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<Map<String, Object>> dataList = new ArrayList<>();
        for (Map.Entry<String, List<CxScheduleResult>> entry : groupMap.entrySet()) {
            List<CxScheduleResult> groupList = entry.getValue();

            // 按机台排序
            groupList.sort(Comparator.comparing(
                    item -> PubUtil.isNotEmpty(item.getCxMachineCode()) ? item.getCxMachineCode() : "",
                    String::compareTo));

            // 添加明细行
            for (CxScheduleResult item : groupList) {
                dataList.add(buildCxTemplateRow(item));
            }

            // 添加小计行
            dataList.add(buildCxTemplateSubtotalRow(entry.getKey(), groupList));
        }

        return dataList;
    }

    /**
     * 构建成型计划模板的一行明细数据。
     * 列映射参照模板表头：
     * C4=机台→cxMachineCode, C5=结构→structureName, C6=胎胚编码→embryoCode,
     * C7=胎胚描述→materialDesc, C8=物料描述→mainMaterialDesc, C9=物料编码→materialCode,
     * C10=TD胶种(缺失), C11=TD整车条数(缺失)
     */
    private Map<String, Object> buildCxTemplateRow(CxScheduleResult item) {
        Map<String, Object> row = new LinkedHashMap<>();
        // C4-C15: 基础列
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

        row.put("class1PlanQty", item.getClass1PlanQty());
        row.put("class1FinishQty", item.getClass1FinishQty());
        row.put("class1Analysis", item.getClass1Analysis());
        row.put("class1RecipeType", item.getClass1RecipeType());
        row.put("class1RecipeNo", item.getClass1RecipeNo());

        row.put("class2PlanQty", item.getClass2PlanQty());
        row.put("class2FinishQty", item.getClass2FinishQty());
        row.put("class2Analysis", item.getClass2Analysis());
        row.put("class2RecipeType", item.getClass2RecipeType());
        row.put("class2RecipeNo", item.getClass2RecipeNo());

        row.put("class3PlanQty", item.getClass3PlanQty());
        row.put("class3FinishQty", item.getClass3FinishQty());
        row.put("class3Analysis", item.getClass3Analysis());
        row.put("class3RecipeType", item.getClass3RecipeType());
        row.put("class3RecipeNo", item.getClass3RecipeNo());

        row.put("class4PlanQty", item.getClass4PlanQty());
        row.put("class4FinishQty", item.getClass4FinishQty());
        row.put("class4Analysis", item.getClass4Analysis());
        row.put("class4RecipeType", item.getClass4RecipeType());
        row.put("class4RecipeNo", item.getClass4RecipeNo());

        row.put("class5PlanQty", item.getClass5PlanQty());
        row.put("class5FinishQty", item.getClass5FinishQty());
        row.put("class5Analysis", item.getClass5Analysis());
        row.put("class5RecipeType", item.getClass5RecipeType());
        row.put("class5RecipeNo", item.getClass5RecipeNo());

        row.put("class6PlanQty", item.getClass6PlanQty());
        row.put("class6FinishQty", item.getClass6FinishQty());
        row.put("class6Analysis", item.getClass6Analysis());
        row.put("class6RecipeType", item.getClass6RecipeType());
        row.put("class6RecipeNo", item.getClass6RecipeNo());

        row.put("class7PlanQty", item.getClass7PlanQty());
        row.put("class7FinishQty", item.getClass7FinishQty());
        row.put("class7Analysis", item.getClass7Analysis());
        row.put("class7RecipeType", item.getClass7RecipeType());
        row.put("class7RecipeNo", item.getClass7RecipeNo());

        row.put("class8PlanQty", item.getClass8PlanQty());
        row.put("class8FinishQty", item.getClass8FinishQty());
        row.put("class8Analysis", item.getClass8Analysis());
        row.put("class8RecipeType", item.getClass8RecipeType());
        row.put("class8RecipeNo", item.getClass8RecipeNo());

        // 合计计划量、完成量
        BigDecimal totalPlan = sumClassPlanQtys(item);
        BigDecimal totalFinish = sumClassFinishQtys(item);
        row.put("totalPlanQty", totalPlan);
        row.put("totalFinishQty", totalFinish);
        row.put("dailyPlanQty", totalPlan);
        row.put("remark", item.getRemark());
        row.put("lhMachineQty", item.getLhMachineQty());

        return row;
    }

    /**
     * 构建按工厂分组的小计行。
     * 小计标识放在 cxMachineCode(C4) 列，便于区分。
     */
    private Map<String, Object> buildCxTemplateSubtotalRow(String factoryCode, List<CxScheduleResult> groupList) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("cxMachineCode", "小计");
        row.put("structureName", "");

        // 汇总各班计划量、完成量
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

        row.put("class1PlanQty", planSums[1]);
        row.put("class1FinishQty", finishSums[1]);
        row.put("class2PlanQty", planSums[2]);
        row.put("class2FinishQty", finishSums[2]);
        row.put("class3PlanQty", planSums[3]);
        row.put("class3FinishQty", finishSums[3]);
        row.put("class4PlanQty", planSums[4]);
        row.put("class4FinishQty", finishSums[4]);
        row.put("class5PlanQty", planSums[5]);
        row.put("class5FinishQty", finishSums[5]);
        row.put("class6PlanQty", planSums[6]);
        row.put("class6FinishQty", finishSums[6]);
        row.put("class7PlanQty", planSums[7]);
        row.put("class7FinishQty", finishSums[7]);
        row.put("class8PlanQty", planSums[8]);
        row.put("class8FinishQty", finishSums[8]);

        // 合计
        BigDecimal totalPlan = BigDecimal.ZERO;
        BigDecimal totalFinish = BigDecimal.ZERO;
        for (int i = 1; i <= 8; i++) {
            totalPlan = safeAdd(totalPlan, planSums[i]);
            totalFinish = safeAdd(totalFinish, finishSums[i]);
        }
        row.put("totalPlanQty", totalPlan);
        row.put("totalFinishQty", totalFinish);
        row.put("dailyPlanQty", totalPlan);

        return row;
    }

    /**
     * 汇总各班计划量。
     */
    private BigDecimal sumClassPlanQtys(CxScheduleResult item) {
        BigDecimal sum = BigDecimal.ZERO;
        sum = safeAdd(sum, item.getClass1PlanQty());
        sum = safeAdd(sum, item.getClass2PlanQty());
        sum = safeAdd(sum, item.getClass3PlanQty());
        sum = safeAdd(sum, item.getClass4PlanQty());
        sum = safeAdd(sum, item.getClass5PlanQty());
        sum = safeAdd(sum, item.getClass6PlanQty());
        sum = safeAdd(sum, item.getClass7PlanQty());
        sum = safeAdd(sum, item.getClass8PlanQty());
        return sum;
    }

    /**
     * 汇总各班完成量。
     */
    private BigDecimal sumClassFinishQtys(CxScheduleResult item) {
        BigDecimal sum = BigDecimal.ZERO;
        sum = safeAdd(sum, item.getClass1FinishQty());
        sum = safeAdd(sum, item.getClass2FinishQty());
        sum = safeAdd(sum, item.getClass3FinishQty());
        sum = safeAdd(sum, item.getClass4FinishQty());
        sum = safeAdd(sum, item.getClass5FinishQty());
        sum = safeAdd(sum, item.getClass6FinishQty());
        sum = safeAdd(sum, item.getClass7FinishQty());
        sum = safeAdd(sum, item.getClass8FinishQty());
        return sum;
    }

    /**
     * 安全加法，null视为0。
     */
    private BigDecimal safeAdd(BigDecimal a, BigDecimal b) {
        if (a == null && b == null) return BigDecimal.ZERO;
        if (a == null) return b;
        if (b == null) return a;
        return a.add(b);
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

        Map<String, List<MpStructureAllocation>> machineGroupMap = structureList.stream()
                .filter(Objects::nonNull)
                .filter(s -> StringUtils.isNotBlank(s.getCxMachineCode()))
                .collect(Collectors.groupingBy(
                        s -> s.getCxMachineCode().trim(),
                        LinkedHashMap::new,
                        Collectors.toList()));
        machineGroupMap.entrySet().removeIf(entry -> entry.getValue().size() < 2);

        List<CxScheduleResult> scheduleResults = cxScheduleResultMapper.selectList(
                buildStructureChangeQueryWrapper(queryVO));
        // 按物料编码+胎胚代码分组，取成型余量最大值（同结构下不同机台可能存在共用数据）
        Map<String, BigDecimal> remainQtyMap = scheduleResults.stream()
                .filter(r -> StringUtils.isNotBlank(r.getMaterialCode()) && StringUtils.isNotBlank(r.getEmbryoCode()))
                .filter(r -> r.getCxRemainQty() != null)
                .collect(Collectors.groupingBy(
                        r -> r.getMaterialCode().trim() + "|" + r.getEmbryoCode().trim(),
                        Collectors.reducing(BigDecimal.ZERO, CxScheduleResult::getCxRemainQty, BigDecimal::max)));

        // 构建结构名到物料编码+胎胚代码的映射，用于通过结构名查找成型余量
        Map<String, String> structureToRemainKeyMap = scheduleResults.stream()
                .filter(r -> StringUtils.isNotBlank(r.getStructureName()))
                .filter(r -> StringUtils.isNotBlank(r.getMaterialCode()) && StringUtils.isNotBlank(r.getEmbryoCode()))
                .collect(Collectors.toMap(
                        CxScheduleResult::getStructureName,
                        r -> r.getMaterialCode().trim() + "|" + r.getEmbryoCode().trim(),
                        (existing, replacement) -> existing,
                        LinkedHashMap::new));

        LocalDate scheduleDate = queryVO != null && queryVO.getScheduleDate() != null
                ? cn.hutool.core.date.DateUtil.toLocalDateTime(queryVO.getScheduleDate()).toLocalDate()
                : LocalDate.now();

        List<Map<String, Object>> dataList = buildStructureChangeDataListV2(
                machineGroupMap, remainQtyMap, structureToRemainKeyMap, scheduleDate);

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
     * 构建成型结构切换导出查询条件（用于查询排程结果余量数据）。
     *
     * @param queryVO 查询条件，来源于UI导出请求
     * @return 成型排程结果Lambda查询条件
     */
    private LambdaQueryWrapper<CxScheduleResult> buildStructureChangeQueryWrapper(CxScheduleResult queryVO) {
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
                .orderByAsc(CxScheduleResult::getScheduleDate)
                .orderByAsc(CxScheduleResult::getMaterialCode);
    }

    /**
     * 构建成型结构切换模板列表数据（V2版本，基于T_MP_STRUCTURE_ALLOCATION）。
     * 按成型机台分组，每个机台按beginDay排序，
     * 相邻结构之间生成一条切换记录。
     *
     * 班产和收尾预计时间由成型排程同事提供接口获取（TODO），
     * 当前仅填充结构和成型余量，收尾/开产预计时间字段暂输出空串。
     *
     * @param machineGroupMap 按机台分组的结构排产数据
     * @param remainQtyMap 余量映射（key: materialCode|embryoCode）
     * @param structureToRemainKeyMap 结构名到余量映射key的映射（key: structureName, value: materialCode|embryoCode）
     * @param scheduleDate 排程日期
     * @return 模板列表行数据
     */
    private List<Map<String, Object>> buildStructureChangeDataListV2(
            Map<String, List<MpStructureAllocation>> machineGroupMap,
            Map<String, BigDecimal> remainQtyMap,
            Map<String, String> structureToRemainKeyMap,
            LocalDate scheduleDate) {

        List<Map<String, Object>> dataList = new ArrayList<>();

        for (Map.Entry<String, List<MpStructureAllocation>> entry : machineGroupMap.entrySet()) {
            String machineCode = entry.getKey();
            List<MpStructureAllocation> structures = entry.getValue().stream()
                    .sorted(Comparator.comparing(MpStructureAllocation::getBeginDay, Comparator.nullsLast(Comparator.naturalOrder())))
                    .collect(Collectors.toList());

            for (int i = 0; i < structures.size() - 1; i++) {
                MpStructureAllocation prevStructure = structures.get(i);
                MpStructureAllocation nextStructure = structures.get(i + 1);

                Map<String, Object> row = buildStructureChangeRow(
                        machineCode, prevStructure, nextStructure,
                        remainQtyMap, structureToRemainKeyMap, scheduleDate);
                dataList.add(row);
            }
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
     * 成型余量通过结构名关联到物料编码+胎胚代码，再从remainQtyMap取最大值。
     * 班产和收尾/开产预计时间由成型排程同事提供接口获取（TODO），
     * 当前receiveChangeDate和vulcanizeChangeDate暂输出空串。
     *
     * @param machineCode 成型机台编码
     * @param prevStructure 前结构（当前正在执行的结构）
     * @param nextStructure 后结构（即将切换到的结构）
     * @param remainQtyMap 余量映射（key: materialCode|embryoCode）
     * @param structureToRemainKeyMap 结构名到余量映射key的映射
     * @param scheduleDate 排程日期
     * @return 单行导出数据
     */
    private Map<String, Object> buildStructureChangeRow(
            String machineCode,
            MpStructureAllocation prevStructure,
            MpStructureAllocation nextStructure,
            Map<String, BigDecimal> remainQtyMap,
            Map<String, String> structureToRemainKeyMap,
            LocalDate scheduleDate) {

        String prevStructureName = StringUtils.defaultString(prevStructure.getStructureName()).trim();
        String nextStructureName = StringUtils.defaultString(nextStructure.getStructureName()).trim();
        String alternatingType = StringUtils.defaultString(nextStructure.getAlternatingType()).trim();
        boolean isInchChange = AlternativeTypeEnum.PRO_SIZE_ALTERNATIVE.getCode().equals(alternatingType);

        // 通过结构名查找对应的物料编码+胎胚代码key，再从remainQtyMap取成型余量
        String remainKey = structureToRemainKeyMap.getOrDefault(prevStructureName, "");
        BigDecimal remainQty = StringUtils.isNotBlank(remainKey)
                ? remainQtyMap.getOrDefault(remainKey, BigDecimal.ZERO)
                : BigDecimal.ZERO;
        if (remainQty.compareTo(BigDecimal.ZERO) == 0 && prevStructure.getNetQty() != null) {
            remainQty = new BigDecimal(prevStructure.getNetQty());
        }

        int year = prevStructure.getYear() != null ? prevStructure.getYear() : scheduleDate.getYear();
        int month = prevStructure.getMonth() != null ? prevStructure.getMonth() : scheduleDate.getMonthValue();

        LocalDate nextBeginDate = nextStructure.getBeginDay() != null
                ? LocalDate.of(year, month, Math.min(nextStructure.getBeginDay(), LocalDate.of(year, month, 1).lengthOfMonth()))
                : scheduleDate;

        // TODO: 班产和收尾预计时间由成型排程提供接口获取，传入结构和成型余量，
        //  当前收尾预计时间和开产预计时间暂输出空串，待接口对接后替换
        String estimatedEndTime = "";
        String estimatedStartTime = "";

        String remark = isInchChange ? "换英寸" : "换结构";

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("stt", 0);
        row.put("cxMachineCode", machineCode);
        row.put("materialSpec", prevStructureName + "→" + nextStructureName);
        row.put("qty", remainQty.intValue());
        row.put("receivePlanDate", formatDateFromDay(year, month, prevStructure.getEndDay()));
        row.put("receiveChangeDate", estimatedEndTime);
        row.put("remark", remark);
        row.put("orderNo", "");
        row.put("vulcanizePlanDate", formatDateFromDay(year, month, nextStructure.getBeginDay()));
        row.put("vulcanizeChangeDate", estimatedStartTime);
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
                dateKey = String.valueOf(dataList.get(i).get("vulcanizePlanDate"));
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
                                rowIdx + 1, rowIdx + 1, 0, 11,
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

            row.put("class1PlanQty", item.getClass1PlanQty());
            row.put("class1FinishQty", item.getClass1FinishQty());
            row.put("class1Analysis", item.getClass1Analysis());
            row.put("class2PlanQty", item.getClass2PlanQty());
            row.put("class2FinishQty", item.getClass2FinishQty());
            row.put("class2Analysis", item.getClass2Analysis());
            row.put("class3PlanQty", item.getClass3PlanQty());
            row.put("class3FinishQty", item.getClass3FinishQty());
            row.put("class3Analysis", item.getClass3Analysis());
            row.put("class4PlanQty", item.getClass4PlanQty());
            row.put("class5PlanQty", item.getClass5PlanQty());
            row.put("class6PlanQty", item.getClass6PlanQty());
            row.put("class7PlanQty", item.getClass7PlanQty());
            row.put("class8PlanQty", item.getClass8PlanQty());

            row.put("totalStock", item.getTotalStock());
            row.put("lhMachineCode", item.getLhMachineCode());
            row.put("cxRemainQty", item.getCxRemainQty());
            row.put("lhRemainQty", item.getLhRemainQty());
            row.put("lhClassQty", item.getLhClassQty());

            dataList.add(row);
        }
        return dataList;
    }
}
