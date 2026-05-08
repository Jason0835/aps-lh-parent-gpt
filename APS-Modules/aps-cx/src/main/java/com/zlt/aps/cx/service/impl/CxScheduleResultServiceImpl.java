package com.zlt.aps.cx.service.impl;

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
import com.zlt.aps.cx.entity.CxMachineStructureCapacity;
import com.zlt.aps.cx.entity.config.CxParamConfig;
import com.zlt.aps.cx.entity.config.CxShiftConfig;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.cx.mapper.CxMachineStructureCapacityMapper;
import com.zlt.aps.cx.mapper.CxParamConfigMapper;
import com.zlt.aps.cx.mapper.CxScheduleResultMapper;
import com.zlt.aps.cx.mapper.CxShiftConfigMapper;
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

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
    private CxShiftConfigMapper cxShiftConfigMapper;

    @Autowired
    private CxMachineStructureCapacityMapper cxMachineStructureCapacityMapper;

    @Autowired
    private CxParamConfigMapper cxParamConfigMapper;

    private static final int DEFAULT_STRUCTURE_SWITCH_HOURS = 8;
    private static final int END_TIME_CALCULATION_WINDOW_DAYS = 4;
    private static final int DEFAULT_SHIFT_CAPACITY = 400;

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
     * 导出成型余量数据。
     *
     * @param queryVO 查询条件，按成型排程结果列表查询口径筛选数据
     * @param fileName 导出文件名，保留用于对齐远程调用契约
     * @return 成型余量Excel文件字节数组
     */
    @Override
    public byte[] exportCxRemainQty(CxScheduleResult queryVO, String fileName) {
        InputStream inputStream = this.getClass().getClassLoader()
                .getResourceAsStream("excelModel/cxyl.xlsx");
        if (Objects.isNull(inputStream)) {
            throw new ServiceException("成型余量导出模板不存在");
        }

        // 按成型排程结果列表的查询口径查询明细数据，再按机台+物料合并余量。
        List<CxScheduleResult> list = cxScheduleResultMapper.selectList(buildCxRemainQtyQueryWrapper(queryVO));
        Map<String, Object> tableMap = new HashMap<>(16);
        List<List<Map<String, Object>>> excelDataList = new ArrayList<>();
        excelDataList.add(buildCxRemainQtyExportDataList(list));
        return ExcelUtils.writeMultiList(inputStream, 0, tableMap, excelDataList);
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
                ? (List<MpStructureAllocation>) structureDataInfo.getRows()
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

        List<CxShiftConfig> shiftConfigs = cxShiftConfigMapper.selectList(
                new LambdaQueryWrapper<CxShiftConfig>()
                        .eq(CxShiftConfig::getIsActive, 1)
                        .orderByAsc(CxShiftConfig::getScheduleDay)
                        .orderByAsc(CxShiftConfig::getDayShiftOrder));

        List<CxMachineStructureCapacity> capacityList = cxMachineStructureCapacityMapper.selectList(
                new LambdaQueryWrapper<CxMachineStructureCapacity>()
                        .eq(CxMachineStructureCapacity::getIsActive, 1));
        Map<String, CxMachineStructureCapacity> capacityMap = capacityList.stream()
                .collect(Collectors.toMap(
                        c -> c.getCxMachineCode().trim() + "|" + c.getStructureName().trim(),
                        c -> c,
                        (oldVal, newVal) -> oldVal,
                        LinkedHashMap::new));

        List<CxScheduleResult> scheduleResults = cxScheduleResultMapper.selectList(
                buildStructureChangeQueryWrapper(queryVO));
        Map<String, BigDecimal> remainQtyMap = scheduleResults.stream()
                .filter(r -> StringUtils.isNotBlank(r.getCxMachineCode()) && StringUtils.isNotBlank(r.getStructureName()))
                .filter(r -> r.getCxRemainQty() != null)
                .collect(Collectors.groupingBy(
                        r -> r.getCxMachineCode().trim() + "|" + r.getStructureName().trim(),
                        Collectors.reducing(BigDecimal.ZERO, CxScheduleResult::getCxRemainQty, BigDecimal::add)));

        int structureSwitchHours = getStructureSwitchHours();

        LocalDate scheduleDate = queryVO != null && queryVO.getScheduleDate() != null
                ? cn.hutool.core.date.DateUtil.toLocalDateTime(queryVO.getScheduleDate()).toLocalDate()
                : LocalDate.now();

        List<Map<String, Object>> dataList = buildStructureChangeDataListV2(
                machineGroupMap, shiftConfigs, capacityMap, remainQtyMap,
                structureSwitchHours, scheduleDate);

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
     * 构建结构排产查询条件，从排程结果查询VO转换为结构排产查询对象。
     *
     * @param queryVO 排程结果查询条件
     * @return 结构排产查询对象
     */
    private MpStructureAllocation buildStructureAllocationQuery(CxScheduleResult queryVO) {
        MpStructureAllocation structureQuery = new MpStructureAllocation();
        if (queryVO != null) {
            structureQuery.setFactoryCode(queryVO.getFactoryCode());
            structureQuery.setCxMachineCode(queryVO.getCxMachineCode());
            if (queryVO.getScheduleDate() != null) {
                LocalDate ld = cn.hutool.core.date.DateUtil.toLocalDateTime(queryVO.getScheduleDate()).toLocalDate();
                structureQuery.setYear(ld.getYear());
                structureQuery.setMonth(ld.getMonthValue());
            }
        }
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
     * 获取结构切换时间参数（小时），从T_CX_PARAM_CONFIG读取，
     * 参数编码为STRUCTURE_SWITCH_HOURS，未配置时默认8小时。
     *
     * @return 结构切换时间（小时）
     */
    private int getStructureSwitchHours() {
        CxParamConfig paramConfig = cxParamConfigMapper.selectOne(
                new LambdaQueryWrapper<CxParamConfig>()
                        .eq(CxParamConfig::getParamCode, "STRUCTURE_SWITCH_HOURS")
                        .eq(CxParamConfig::getIsActive, 1)
                        .last("LIMIT 1"));
        if (paramConfig != null && StringUtils.isNotBlank(paramConfig.getParamValue())) {
            try {
                return Integer.parseInt(paramConfig.getParamValue().trim());
            } catch (NumberFormatException e) {
                log.warn("结构切换时间参数值格式错误，使用默认值{}小时: {}", DEFAULT_STRUCTURE_SWITCH_HOURS, paramConfig.getParamValue());
            }
        }
        return DEFAULT_STRUCTURE_SWITCH_HOURS;
    }

    /**
     * 构建成型结构切换模板列表数据（V2版本，基于T_MP_STRUCTURE_ALLOCATION）。
     * 按成型机台分组，每个机台按beginDay排序，
     * 相邻结构之间生成一条切换记录，
     * 计算收尾预计时间和开产预计时间。
     *
     * @param machineGroupMap 按机台分组的结构排产数据
     * @param shiftConfigs 班次配置列表
     * @param capacityMap 机台结构产能配置映射（key: machineCode|structureName）
     * @param remainQtyMap 余量映射（key: machineCode|structureName）
     * @param structureSwitchHours 结构切换时间（小时）
     * @param scheduleDate 排程日期
     * @return 模板列表行数据
     */
    private List<Map<String, Object>> buildStructureChangeDataListV2(
            Map<String, List<MpStructureAllocation>> machineGroupMap,
            List<CxShiftConfig> shiftConfigs,
            Map<String, CxMachineStructureCapacity> capacityMap,
            Map<String, BigDecimal> remainQtyMap,
            int structureSwitchHours,
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
                        shiftConfigs, capacityMap, remainQtyMap,
                        structureSwitchHours, scheduleDate);
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
     * @param machineCode 成型机台编码
     * @param prevStructure 前结构（当前正在执行的结构）
     * @param nextStructure 后结构（即将切换到的结构）
     * @param shiftConfigs 班次配置列表
     * @param capacityMap 机台结构产能配置映射
     * @param remainQtyMap 余量映射
     * @param structureSwitchHours 结构切换时间（小时）
     * @param scheduleDate 排程日期
     * @return 单行导出数据
     */
    private Map<String, Object> buildStructureChangeRow(
            String machineCode,
            MpStructureAllocation prevStructure,
            MpStructureAllocation nextStructure,
            List<CxShiftConfig> shiftConfigs,
            Map<String, CxMachineStructureCapacity> capacityMap,
            Map<String, BigDecimal> remainQtyMap,
            int structureSwitchHours,
            LocalDate scheduleDate) {

        String prevStructureName = StringUtils.defaultString(prevStructure.getStructureName()).trim();
        String nextStructureName = StringUtils.defaultString(nextStructure.getStructureName()).trim();
        String alternatingType = StringUtils.defaultString(nextStructure.getAlternatingType()).trim();
        boolean isInchChange = AlternativeTypeEnum.PRO_SIZE_ALTERNATIVE.getCode().equals(alternatingType);

        String capacityKey = machineCode + "|" + prevStructureName;
        CxMachineStructureCapacity capacity = capacityMap.get(capacityKey);
        int shiftCapacity = capacity != null ? capacity.getDailyCapacity() : DEFAULT_SHIFT_CAPACITY;

        String remainKey = machineCode + "|" + prevStructureName;
        BigDecimal remainQty = remainQtyMap.getOrDefault(remainKey, BigDecimal.ZERO);
        if (remainQty.compareTo(BigDecimal.ZERO) == 0 && prevStructure.getNetQty() != null) {
            remainQty = new BigDecimal(prevStructure.getNetQty());
        }

        int year = prevStructure.getYear() != null ? prevStructure.getYear() : scheduleDate.getYear();
        int month = prevStructure.getMonth() != null ? prevStructure.getMonth() : scheduleDate.getMonthValue();

        LocalDate prevEndDate = prevStructure.getEndDay() != null
                ? LocalDate.of(year, month, Math.min(prevStructure.getEndDay(), LocalDate.of(year, month, 1).lengthOfMonth()))
                : scheduleDate;
        LocalDate nextBeginDate = nextStructure.getBeginDay() != null
                ? LocalDate.of(year, month, Math.min(nextStructure.getBeginDay(), LocalDate.of(year, month, 1).lengthOfMonth()))
                : scheduleDate;

        String estimatedEndTime = calculateEstimatedEndTime(
                remainQty, shiftCapacity, shiftConfigs, scheduleDate, year, month);

        String estimatedStartTime = calculateEstimatedStartTime(
                estimatedEndTime, structureSwitchHours, isInchChange,
                shiftConfigs, year, month);

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
     * 计算收尾预计时间。
     * 根据余量和班产推算4天内的收尾时间，格式为"MM.DD日早班/中班/夜班"。
     *
     * @param remainQty 余量
     * @param dailyCapacity 日产能
     * @param shiftConfigs 班次配置列表
     * @param scheduleDate 排程日期（计算起点）
     * @param year 年份
     * @param month 月份
     * @return 格式化的收尾预计时间字符串
     */
    private String calculateEstimatedEndTime(BigDecimal remainQty, int dailyCapacity,
                                             List<CxShiftConfig> shiftConfigs,
                                             LocalDate scheduleDate, int year, int month) {
        if (remainQty == null || remainQty.compareTo(BigDecimal.ZERO) <= 0) {
            return "";
        }
        if (dailyCapacity <= 0) {
            dailyCapacity = DEFAULT_SHIFT_CAPACITY;
        }

        int remainInt = remainQty.intValue();
        int shiftsPerDay = shiftConfigs.isEmpty() ? 3 : (int) shiftConfigs.stream()
                .filter(s -> s.getScheduleDay() != null && s.getScheduleDay() == 1)
                .count();
        if (shiftsPerDay <= 0) {
            shiftsPerDay = 3;
        }
        int shiftCapacity = dailyCapacity / shiftsPerDay;
        if (shiftCapacity <= 0) {
            shiftCapacity = DEFAULT_SHIFT_CAPACITY / 3;
        }

        int shiftsNeeded = remainInt / shiftCapacity + (remainInt % shiftCapacity > 0 ? 1 : 0);

        LocalDate calcDate = scheduleDate;
        int shiftIndex = 0;

        for (int dayOffset = 0; dayOffset < END_TIME_CALCULATION_WINDOW_DAYS; dayOffset++) {
            LocalDate currentDate = calcDate.plusDays(dayOffset);
            int dayShifts = getShiftCountForDate(shiftConfigs, currentDate, year, month);

            for (int s = 0; s < dayShifts; s++) {
                shiftIndex++;
                if (shiftIndex >= shiftsNeeded) {
                    String shiftName = getShiftNameByOrder(shiftConfigs, currentDate, year, month, s);
                    return formatDateShift(currentDate, shiftName);
                }
            }
        }

        LocalDate lastDate = calcDate.plusDays(END_TIME_CALCULATION_WINDOW_DAYS - 1);
        String lastShiftName = getLastShiftName(shiftConfigs, lastDate, year, month);
        return formatDateShift(lastDate, lastShiftName);
    }

    /**
     * 计算开产预计时间。
     * 开产预计时间 = 收尾预计时间 + 结构切换时间，
     * 如果是"换英寸"则强制安排在早班。
     *
     * @param estimatedEndTime 收尾预计时间（格式：MM.DD日X班）
     * @param switchHours 结构切换时间（小时）
     * @param isInchChange 是否为换英寸
     * @param shiftConfigs 班次配置列表
     * @param year 年份
     * @param month 月份
     * @return 格式化的开产预计时间字符串
     */
    private String calculateEstimatedStartTime(String estimatedEndTime, int switchHours,
                                               boolean isInchChange,
                                               List<CxShiftConfig> shiftConfigs,
                                               int year, int month) {
        if (StringUtils.isBlank(estimatedEndTime)) {
            return "";
        }

        LocalDateTime endDateTime = parseDateShift(estimatedEndTime, year, month);
        if (endDateTime == null) {
            return "";
        }

        LocalDateTime startDateTime = endDateTime.plusHours(switchHours);

        if (isInchChange) {
            startDateTime = adjustToMorningShift(startDateTime, shiftConfigs, year, month);
        }

        String shiftName = determineShiftName(startDateTime, shiftConfigs, year, month);
        return formatDateShift(startDateTime.toLocalDate(), shiftName);
    }

    /**
     * 将"换英寸"的开产时间调整到早班。
     * 如果计算出的开产时间不在早班时间段内，则调整到下一个早班的开始时间。
     *
     * @param dateTime 原始开产时间
     * @param shiftConfigs 班次配置列表
     * @param year 年份
     * @param month 月份
     * @return 调整后的开产时间
     */
    private LocalDateTime adjustToMorningShift(LocalDateTime dateTime,
                                                List<CxShiftConfig> shiftConfigs,
                                                int year, int month) {
        CxShiftConfig morningShift = shiftConfigs.stream()
                .filter(s -> s.getScheduleDay() != null && s.getScheduleDay() == 1)
                .filter(s -> s.getDayShiftOrder() != null && s.getDayShiftOrder() == 1)
                .findFirst()
                .orElse(null);

        if (morningShift == null) {
            return dateTime;
        }

        LocalTime morningStart = morningShift.getShiftStartTime();
        LocalTime morningEnd = morningShift.getShiftEndTime();

        LocalTime time = dateTime.toLocalTime();
        boolean isCrossDay = morningShift.getIsCrossDay() != null && morningShift.getIsCrossDay() == 1;

        boolean inMorningShift;
        if (isCrossDay) {
            inMorningShift = !time.isBefore(morningStart) || !time.isAfter(morningEnd);
        } else {
            inMorningShift = !time.isBefore(morningStart) && !time.isAfter(morningEnd);
        }

        if (!inMorningShift) {
            if (time.isAfter(morningEnd) && !isCrossDay) {
                dateTime = dateTime.plusDays(1);
            }
            dateTime = LocalDateTime.of(dateTime.toLocalDate(), morningStart);
        }

        return dateTime;
    }

    /**
     * 解析"MM.DD日X班"格式的时间字符串为LocalDateTime。
     *
     * @param dateShiftStr 格式化的日期班次字符串
     * @param year 年份
     * @param month 月份
     * @return LocalDateTime对象
     */
    private LocalDateTime parseDateShift(String dateShiftStr, int year, int month) {
        try {
            String cleaned = dateShiftStr.replace("日", "|").replace("月", "|");
            String[] parts = cleaned.split("\\|");
            if (parts.length < 2) {
                return null;
            }
            int m = Integer.parseInt(parts[0].trim());
            int d = Integer.parseInt(parts[1].trim());
            LocalDate date = LocalDate.of(year, m, d);

            String shiftName = parts.length >= 3 ? parts[2].trim() : "早班";
            LocalTime shiftTime = getShiftStartTimeByName(shiftName);
            return LocalDateTime.of(date, shiftTime);
        } catch (Exception e) {
            log.warn("解析日期班次字符串失败: {}", dateShiftStr, e);
            return null;
        }
    }

    /**
     * 根据班次名称获取班次开始时间。
     *
     * @param shiftName 班次名称（早班/中班/夜班）
     * @return 班次开始时间
     */
    private LocalTime getShiftStartTimeByName(String shiftName) {
        if (shiftName.contains("夜")) {
            return LocalTime.of(0, 0);
        } else if (shiftName.contains("中")) {
            return LocalTime.of(16, 0);
        } else {
            return LocalTime.of(8, 0);
        }
    }

    /**
     * 根据时间确定所在班次名称。
     *
     * @param dateTime 时间
     * @param shiftConfigs 班次配置列表
     * @param year 年份
     * @param month 月份
     * @return 班次名称
     */
    private String determineShiftName(LocalDateTime dateTime, List<CxShiftConfig> shiftConfigs,
                                       int year, int month) {
        LocalTime time = dateTime.toLocalTime();
        for (CxShiftConfig config : shiftConfigs) {
            if (config.getScheduleDay() == null || config.getScheduleDay() != 1) {
                continue;
            }
            LocalTime start = config.getShiftStartTime();
            LocalTime end = config.getShiftEndTime();
            boolean isCrossDay = config.getIsCrossDay() != null && config.getIsCrossDay() == 1;

            boolean inRange;
            if (isCrossDay) {
                inRange = !time.isBefore(start) || !time.isAfter(end);
            } else {
                inRange = !time.isBefore(start) && !time.isAfter(end);
            }
            if (inRange) {
                return config.getShiftName() != null ? config.getShiftName() : "早班";
            }
        }
        if (time.isBefore(LocalTime.of(8, 0))) {
            return "夜班";
        } else if (time.isBefore(LocalTime.of(16, 0))) {
            return "早班";
        } else {
            return "中班";
        }
    }

    /**
     * 获取指定日期的班次数。
     *
     * @param shiftConfigs 班次配置列表
     * @param date 日期
     * @param year 年份
     * @param month 月份
     * @return 班次数
     */
    private int getShiftCountForDate(List<CxShiftConfig> shiftConfigs, LocalDate date, int year, int month) {
        int scheduleDay = getScheduleDay(shiftConfigs, date, year, month);
        return (int) shiftConfigs.stream()
                .filter(s -> s.getScheduleDay() != null && s.getScheduleDay() == scheduleDay)
                .count();
    }

    /**
     * 获取日期对应的排程天数（1/2/3表示排程周期中的第几天）。
     *
     * @param shiftConfigs 班次配置列表
     * @param date 日期
     * @param year 年份
     * @param month 月份
     * @return 排程天数
     */
    private int getScheduleDay(List<CxShiftConfig> shiftConfigs, LocalDate date, int year, int month) {
        if (!shiftConfigs.isEmpty()) {
            Integer maxDay = shiftConfigs.stream()
                    .map(CxShiftConfig::getScheduleDay)
                    .filter(Objects::nonNull)
                    .max(Integer::compareTo)
                    .orElse(1);
            if (maxDay > 0) {
                int dayOfMonth = date.getDayOfMonth();
                return ((dayOfMonth - 1) % maxDay) + 1;
            }
        }
        return 1;
    }

    /**
     * 根据班次序号获取班次名称。
     *
     * @param shiftConfigs 班次配置列表
     * @param date 日期
     * @param year 年份
     * @param month 月份
     * @param order 班次序号（0开始）
     * @return 班次名称
     */
    private String getShiftNameByOrder(List<CxShiftConfig> shiftConfigs, LocalDate date,
                                        int year, int month, int order) {
        int scheduleDay = getScheduleDay(shiftConfigs, date, year, month);
        List<CxShiftConfig> dayShifts = shiftConfigs.stream()
                .filter(s -> s.getScheduleDay() != null && s.getScheduleDay() == scheduleDay)
                .sorted(Comparator.comparing(CxShiftConfig::getDayShiftOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
        if (order < dayShifts.size()) {
            String name = dayShifts.get(order).getShiftName();
            return name != null ? name : "早班";
        }
        return "早班";
    }

    /**
     * 获取指定日期最后一个班次的名称。
     *
     * @param shiftConfigs 班次配置列表
     * @param date 日期
     * @param year 年份
     * @param month 月份
     * @return 最后班次名称
     */
    private String getLastShiftName(List<CxShiftConfig> shiftConfigs, LocalDate date, int year, int month) {
        int scheduleDay = getScheduleDay(shiftConfigs, date, year, month);
        return shiftConfigs.stream()
                .filter(s -> s.getScheduleDay() != null && s.getScheduleDay() == scheduleDay)
                .sorted(Comparator.comparing(CxShiftConfig::getDayShiftOrder, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(CxShiftConfig::getShiftName)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("夜班");
    }

    /**
     * 格式化日期和班次为"MM.DD日X班"格式。
     *
     * @param date 日期
     * @param shiftName 班次名称
     * @return 格式化字符串
     */
    private String formatDateShift(LocalDate date, String shiftName) {
        if (date == null) {
            return "";
        }
        return String.format("%02d.%02d日%s", date.getMonthValue(), date.getDayOfMonth(),
                StringUtils.defaultString(shiftName));
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
