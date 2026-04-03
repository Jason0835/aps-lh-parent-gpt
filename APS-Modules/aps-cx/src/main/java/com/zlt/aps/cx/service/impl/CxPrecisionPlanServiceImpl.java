package com.zlt.aps.cx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cx.mapper.CxPrecisionPlanMapper;
import com.zlt.aps.cx.mapper.MdmMoldingMachineMapper;
import com.zlt.aps.cx.service.ICxPrecisionPlanService;
import com.zlt.aps.mdm.api.domain.entity.CxPrecisionPlan;
import com.zlt.aps.mp.api.domain.entity.MdmMoldingMachine;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.PubUtil;
import jodd.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 成型精度计划服务实现类
 *
 * @author APS Team
 */
@Slf4j
@Service
public class CxPrecisionPlanServiceImpl extends AbstractDocService<CxPrecisionPlan> implements ICxPrecisionPlanService {

    @Autowired
    private CxPrecisionPlanMapper cxPrecisionPlanMapper;
    @Autowired
    private MdmMoldingMachineMapper moldingMachineMapper;

    @Override
    public String checkUnique(CxPrecisionPlan entity) {
        Long id = entity.getId();
        String machineCode = entity.getMachineCode();
        Date planDate = entity.getPlanDate();

        LambdaQueryWrapper<CxPrecisionPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CxPrecisionPlan::getMachineCode, machineCode)
              .eq(CxPrecisionPlan::getPlanDate, planDate);

        if (id != null) {
            wrapper.ne(CxPrecisionPlan::getId, id);
        }

        Long count = cxPrecisionPlanMapper.selectCount(wrapper);
        return count > 0 ? UserConstants.NOT_UNIQUE : UserConstants.UNIQUE;
    }


    @Override
    public String getDocTypeCode() {
        return "CX_PRECISION_PLAN";
    }

    @Override
    public AjaxResult importData(List<CxPrecisionPlan> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<CxPrecisionPlan> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        String uniqueMsg = I18nUtil.getMessage("import.validated.unique");
        String machineNotExistMsg = I18nUtil.getMessage("ui.data.alert.cxPrecisionPlan.machineNotExist");

        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            CxPrecisionPlan docEntity = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, docEntity);
            ImportExcelValidatedUtils.validatedRepeat(list, docEntity, i, 2, importLogId, validated);
            if (PubUtil.isNotEmpty(validated)) {
                failureNum++;
                docEntity.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }

        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            CxPrecisionPlan docEntity = list.get(i);
            if (docEntity.getId() != null && docEntity.getId() == -999L) {
                continue;
            }

            if (StringUtil.isBlank(docEntity.getMachineCode())) {
                failureNum++;
                String message = I18nUtil.getMessage("ui.data.alert.cxPrecisionPlan.machineCodeRequired");
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                    errorNum, String.format(message, errorNum), importErrorLogs);
                continue;
            }

            if (docEntity.getPlanDate() == null) {
                failureNum++;
                String message = I18nUtil.getMessage("ui.data.alert.cxPrecisionPlan.planDateRequired");
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                    errorNum, String.format(message, errorNum), importErrorLogs);
                continue;
            }

        LambdaQueryWrapper<MdmMoldingMachine> machineWrapper = new LambdaQueryWrapper<>();
        machineWrapper.eq(MdmMoldingMachine::getCxMachineCode, docEntity.getMachineCode());
        if (StringUtil.isNotBlank(docEntity.getFactoryCode())) {
            machineWrapper.eq(MdmMoldingMachine::getFactoryCode, docEntity.getFactoryCode());
        }
        machineWrapper.last("LIMIT 1");
        MdmMoldingMachine machine = moldingMachineMapper.selectOne(machineWrapper);
        if (machine == null) {
            failureNum++;
            ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                    errorNum, String.format(machineNotExistMsg, errorNum, docEntity.getMachineCode()), importErrorLogs);
            continue;
        }

        if (docEntity.getPlanStartTime() != null && docEntity.getPlanDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String planDateStr = sdf.format(docEntity.getPlanDate());
            String startTimeStr = sdf.format(docEntity.getPlanStartTime());
            if (startTimeStr.compareTo(planDateStr) < 0) {
                failureNum++;
                String message = I18nUtil.getMessage("ui.data.alert.cxPrecisionPlan.startTimeBeforePlanDate");
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                        errorNum, String.format(message, errorNum), importErrorLogs);
                continue;
            }
        }

        if (docEntity.getPlanStartTime() != null && docEntity.getPlanEndTime() != null) {
            if (docEntity.getPlanEndTime().before(docEntity.getPlanStartTime())) {
                failureNum++;
                String message = I18nUtil.getMessage("ui.data.alert.cxPrecisionPlan.endTimeBeforeStartTime");
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                        errorNum, String.format(message, errorNum), importErrorLogs);
                continue;
            }
            long diffMillis = docEntity.getPlanEndTime().getTime() - docEntity.getPlanStartTime().getTime();
            double hours = diffMillis / (1000.0 * 60 * 60);
            BigDecimal estimatedHours = BigDecimal.valueOf(hours).setScale(1, RoundingMode.HALF_UP);
            docEntity.setEstimatedHours(estimatedHours);
        }

        if (docEntity.getPlanDate() != null && docEntity.getLastPrecisionDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String planDateStr = sdf.format(docEntity.getPlanDate());
            String lastPrecisionStr = sdf.format(docEntity.getLastPrecisionDate());
            if (lastPrecisionStr.compareTo(planDateStr) >= 0) {
                failureNum++;
                String message = I18nUtil.getMessage("ui.data.alert.cxPrecisionPlan.lastPrecisionDateAfterStartTime");
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                        errorNum, String.format(message, errorNum), importErrorLogs);
                continue;
            }
        }

        if (docEntity.getPlanDate() != null && docEntity.getDueDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String planDateStr = sdf.format(docEntity.getPlanDate());
            String dueDateStr = sdf.format(docEntity.getDueDate());
            if (dueDateStr.compareTo(planDateStr) <= 0) {
                failureNum++;
                String message = I18nUtil.getMessage("ui.data.alert.cxPrecisionPlan.dueDateBeforePlanDate");
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                        errorNum, String.format(message, errorNum), importErrorLogs);
                continue;
            }
        }

        if (checkUnique(docEntity).equals(UserConstants.UNIQUE)) {
                importList.add(docEntity);
                successNum++;
            } else {
                if (updateSupport) {
                    QueryWrapper<CxPrecisionPlan> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("MACHINE_CODE", docEntity.getMachineCode());
                    queryWrapper.eq("PLAN_DATE", docEntity.getPlanDate());
                    CxPrecisionPlan existEntity = cxPrecisionPlanMapper.selectOne(queryWrapper);
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

        for (CxPrecisionPlan entity : importList) {
            if (entity.getId() != null) {
                cxPrecisionPlanMapper.updateById(entity);
            } else {
                cxPrecisionPlanMapper.insert(entity);
            }
        }

        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum + "," + failureNum);
        }
    }

}
