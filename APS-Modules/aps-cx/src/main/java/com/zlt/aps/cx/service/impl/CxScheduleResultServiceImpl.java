package com.zlt.aps.cx.service.impl;

import com.ruoyi.common.utils.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cx.entity.schedule.CxScheduleDetail;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.cx.mapper.CxScheduleResultMapper;
import com.zlt.aps.cx.service.CxScheduleDetailService;
import com.zlt.aps.cx.service.CxScheduleResultService;
import com.zlt.aps.cx.vo.CxScheduleResultTemplateImportVO;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.aps.common.core.utils.ExcelUtils;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.ruoyi.common.exception.ServiceException;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.common.utils.PubUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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

    @Override
    public List<CxScheduleResult> listByScheduleDate(LocalDate scheduleDate) {
        return cxScheduleResultMapper.selectList(new LambdaQueryWrapper<CxScheduleResult>()
                .eq(CxScheduleResult::getScheduleDate, scheduleDate.atStartOfDay())
                .orderByAsc(CxScheduleResult::getCxMachineCode));
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
                .filter(StringUtil::isNotBlank).map(String::trim).distinct()
                .collect(Collectors.toList());
        List<String> materialCodes = list.stream()
                .filter(Objects::nonNull)
                .filter(item -> !Objects.equals(item.getId(), -999L))
                .map(CxScheduleResultTemplateImportVO::getMaterialCode)
                .filter(StringUtil::isNotBlank).map(String::trim).distinct()
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

            if (StringUtil.isBlank(row.getCxMachineCode())) {
                failureNum++;
                ImportExcelValidatedUtils.addImportErrorLog(logId, ImportErrorTypeEnums.OTHERS.getCode(),
                        rowNum, "第" + rowNum + "行 成型机台编号不能为空", importErrorLogs);
                continue;
            }
            if (StringUtil.isBlank(row.getMaterialCode())) {
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
