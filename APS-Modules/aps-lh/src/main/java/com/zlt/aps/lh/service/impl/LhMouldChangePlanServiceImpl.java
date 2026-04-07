package com.zlt.aps.lh.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.RowStateEnum;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.redis.service.RedisService;
import com.zlt.aps.common.SyncDataLogsService;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.itf.mes.IMesItfService;
import com.zlt.aps.lh.api.domain.entity.LhMouldChangePlan;
import com.zlt.aps.lh.mapper.LhMouldChangePlanEntityMapper;
import com.zlt.aps.lh.service.ILhMouldChangePlanService;
import com.zlt.aps.maindata.mapper.LhMachineInfoEntityMapper;
import com.zlt.aps.maindata.mapper.MdmMaterialInfoEntityMapper;
import com.zlt.aps.mdm.api.domain.entity.MdmMoldAlterPlan;
import com.zlt.aps.mp.api.domain.entity.LhMachineInfo;
import com.zlt.aps.mp.api.domain.entity.MdmMaterialInfo;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.common.utils.PubUtil;
import jodd.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 模具交替计划Service实现
 *
 * @author APS Team
 * @since 2026/04/01
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class LhMouldChangePlanServiceImpl extends AbstractDocService<LhMouldChangePlan> implements ILhMouldChangePlanService {

    @Resource
    private LhMouldChangePlanEntityMapper lhMouldChangePlanMapper;

    @Autowired
    private LhMachineInfoEntityMapper lhMachineInfoEntityMapper;

    @Autowired
    private MdmMaterialInfoEntityMapper mdmMaterialInfoEntityMapper;

    @Autowired
    private SyncDataLogsService syncDataLogsService;

    @Autowired
    private RedisService redisService;

    @Autowired
    private IMesItfService mesItfService;

    @Override
    public String[] getQueryFormulas() {
        return new String[0];
    }

    @Override
    protected String getDocTypeCode() {
        return "";
    }

    /**
     * 导入数据
     *
     * @param list
     * @param updateSupport
     * @param importLogId
     * @return
     */
    @Override
    public AjaxResult importData(List<LhMouldChangePlan> list, boolean updateSupport, Long importLogId) {
        // 0.初始化
        int successNum = 0;
        int failureNum = 0;
        List<LhMouldChangePlan> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        String uniqueMsg = I18nUtil.getMessage("import.validated.unique");

        // 1.进行非空校验,Excel中数据重复校验
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            LhMouldChangePlan docEntity = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, docEntity);
            ImportExcelValidatedUtils.validatedRepeat(list, docEntity, i, 2, importLogId, validated);
            if (CollectionUtils.isNotEmpty(validated)) {
                failureNum++;
                docEntity.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }

        // 2.进行数据库唯一性校验 + 硫化机台/物料存在性校验
        // 先批量查询，提升性能
        Map<String, List<LhMouldChangePlan>> factoryCodeMap = list.stream()
                .collect(Collectors.groupingBy(LhMouldChangePlan::getFactoryCode));

        // 查询硫化机台
        Map<String, LhMachineInfo> machineInfoMap = new HashMap<>(16);
        if (!factoryCodeMap.isEmpty()) {
            for (String factoryCode : factoryCodeMap.keySet()) {
                List<LhMouldChangePlan> itemList = factoryCodeMap.get(factoryCode);
                List<String> machineCodeList = itemList.stream()
                        .map(LhMouldChangePlan::getLhMachineCode)
                        .filter(StringUtil::isNotBlank)
                        .distinct()
                        .collect(Collectors.toList());
                if (!machineCodeList.isEmpty()) {
                    List<List<String>> splitList = com.zlt.aps.maindata.utils.CollectionUtils.splitList(machineCodeList, 900);
                    List<LhMachineInfo> machineInfoList = new ArrayList<>();
                    for (List<String> codeList : splitList) {
                        LambdaQueryWrapper<LhMachineInfo> wrapper = new LambdaQueryWrapper<>();
                        wrapper.in(LhMachineInfo::getMachineCode, codeList);
                        wrapper.eq(LhMachineInfo::getFactoryCode, factoryCode);
                        machineInfoList.addAll(lhMachineInfoEntityMapper.selectList(wrapper));
                    }
                    if (CollectionUtils.isNotEmpty(machineInfoList)) {
                        Map<String, LhMachineInfo> partMap = machineInfoList.stream().collect(Collectors
                                .toMap(x -> x.getFactoryCode() + "," + x.getMachineCode(), machine -> machine));
                        machineInfoMap.putAll(partMap);
                    }
                }
            }
        }

        // 查询物料 (前规格 + 后规格)
        Map<String, MdmMaterialInfo> materialInfoMap = new HashMap<>(16);
        if (!factoryCodeMap.isEmpty()) {
            for (String factoryCode : factoryCodeMap.keySet()) {
                List<LhMouldChangePlan> itemList = factoryCodeMap.get(factoryCode);
                List<String> materialCodeList = new ArrayList<>();
                for (LhMouldChangePlan item : itemList) {
                    if (StringUtil.isNotBlank(item.getBeforeMaterialCode())) {
                        materialCodeList.add(item.getBeforeMaterialCode());
                    }
                    if (StringUtil.isNotBlank(item.getAfterMaterialCode())) {
                        materialCodeList.add(item.getAfterMaterialCode());
                    }
                }
                materialCodeList = materialCodeList.stream().distinct().collect(Collectors.toList());
                if (!materialCodeList.isEmpty()) {
                    List<List<String>> splitList = com.zlt.aps.maindata.utils.CollectionUtils.splitList(materialCodeList, 900);
                    List<MdmMaterialInfo> materialInfoList = new ArrayList<>();
                    for (List<String> codeList : splitList) {
                        LambdaQueryWrapper<MdmMaterialInfo> wrapper = new LambdaQueryWrapper<>();
                        wrapper.in(MdmMaterialInfo::getMaterialCode, codeList);
                        wrapper.eq(MdmMaterialInfo::getFactoryCode, factoryCode);
                        materialInfoList.addAll(mdmMaterialInfoEntityMapper.selectList(wrapper));
                    }
                    if (CollectionUtils.isNotEmpty(materialInfoList)) {
                        Map<String, MdmMaterialInfo> partMap = materialInfoList.stream().collect(Collectors
                                .toMap(x -> x.getFactoryCode() + "," + x.getMaterialCode(), material -> material));
                        materialInfoMap.putAll(partMap);
                    }
                }
            }
        }

        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            LhMouldChangePlan docEntity = list.get(i);
            if (docEntity.getId() != null && docEntity.getId() == -999L) {
                continue;
            }

            // 检查硫化机台是否存在
            if (StringUtil.isBlank(docEntity.getLhMachineCode())) {
                failureNum++;
                String message = I18nUtil.getMessage("ui.data.alert.lhMouldChangePlan.lhMachineCodeRequired");
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                        errorNum, String.format(message, errorNum), importErrorLogs);
                continue;
            }
            if (!machineInfoMap.containsKey(docEntity.getFactoryCode() + "," + docEntity.getLhMachineCode())) {
                failureNum++;
                String message = I18nUtil.getMessage("ui.data.alert.lhMouldChangePlan.lhMachineCodeNotExist");
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                        errorNum, String.format(message, errorNum, docEntity.getLhMachineCode()), importErrorLogs);
                continue;
            }

            // 检查前规格物料是否存在
            if (StringUtil.isNotBlank(docEntity.getBeforeMaterialCode()) &&
                    !materialInfoMap.containsKey(docEntity.getFactoryCode() + "," + docEntity.getBeforeMaterialCode())) {
                failureNum++;
                String message = I18nUtil.getMessage("ui.data.alert.lhMouldChangePlan.beforeMaterialCodeNotExist");
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                        errorNum, String.format(message, errorNum, docEntity.getBeforeMaterialCode()), importErrorLogs);
                continue;
            }

            // 检查后规格物料是否存在
            if (StringUtil.isNotBlank(docEntity.getAfterMaterialCode()) &&
                    !materialInfoMap.containsKey(docEntity.getFactoryCode() + "," + docEntity.getAfterMaterialCode())) {
                failureNum++;
                String message = I18nUtil.getMessage("ui.data.alert.lhMouldChangePlan.afterMaterialCodeNotExist");
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                        errorNum, String.format(message, errorNum, docEntity.getAfterMaterialCode()), importErrorLogs);
                continue;
            }

            // 填充机台名称和物料描述
            if (machineInfoMap.containsKey(docEntity.getFactoryCode() + "," + docEntity.getLhMachineCode())) {
                LhMachineInfo machine = machineInfoMap.get(docEntity.getFactoryCode() + "," + docEntity.getLhMachineCode());
                docEntity.setLhMachineName(machine.getMachineName());
            }
            if (StringUtil.isNotBlank(docEntity.getBeforeMaterialCode()) &&
                    materialInfoMap.containsKey(docEntity.getFactoryCode() + "," + docEntity.getBeforeMaterialCode())) {
                MdmMaterialInfo material = materialInfoMap.get(docEntity.getFactoryCode() + "," + docEntity.getBeforeMaterialCode());
                docEntity.setBeforeMaterialDesc(material.getMaterialDesc());
            }
            if (StringUtil.isNotBlank(docEntity.getAfterMaterialCode()) &&
                    materialInfoMap.containsKey(docEntity.getFactoryCode() + "," + docEntity.getAfterMaterialCode())) {
                MdmMaterialInfo material = materialInfoMap.get(docEntity.getFactoryCode() + "," + docEntity.getAfterMaterialCode());
                docEntity.setAfterMaterialDesc(material.getMaterialDesc());
            }

            if (checkUnique(docEntity).equals(UserConstants.UNIQUE)) {
                docEntity.setRowState(RowStateEnum.ADDED);
                if (StringUtil.isBlank(docEntity.getFactoryCode())) {
                    docEntity.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
                }
                importList.add(docEntity);
                successNum++;
            } else {
                failureNum++;
                // 数据库已经存在,不允许插入
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, errorNum,
                        String.format(uniqueMsg, errorNum), importErrorLogs);
            }
        }

        if (CollectionUtils.isEmpty(importList)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        }

        successNum = baseDao.saveBatch(importList);

        // 返回提示信息及错误集合
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

    /**
     * 校验唯一性
     */
    @Override
    public String checkUnique(LhMouldChangePlan docEntityVO) {
        // 唯一性判断依据: 根据业务修改
        QueryWrapper<LhMouldChangePlan> queryWrapper = new QueryWrapper<>();
        queryWrapper.ne(PubUtil.isNotEmpty(docEntityVO.getFieldValueByFieldName("id")), "ID", docEntityVO.getFieldValueByFieldName("id"));
        // 校验维度: factoryCode + lhResultBatchNo + orderNo + planDate + lhMachineCode + beforeMaterialCode + afterMaterialCode
        queryWrapper.eq("FACTORY_CODE", docEntityVO.getFactoryCode());
        queryWrapper.eq("LH_RESULT_BATCH_NO", docEntityVO.getLhResultBatchNo());
        queryWrapper.eq("ORDER_NO", docEntityVO.getOrderNo());
        queryWrapper.eq("PLAN_DATE", docEntityVO.getPlanDate());
        queryWrapper.eq("LH_MACHINE_CODE", docEntityVO.getLhMachineCode());
        queryWrapper.eq("BEFORE_MATERIAL_CODE", docEntityVO.getBeforeMaterialCode());
        queryWrapper.eq("AFTER_MATERIAL_CODE", docEntityVO.getAfterMaterialCode());

        if (lhMouldChangePlanMapper.selectCount(queryWrapper) > 0) {
            return UserConstants.NOT_UNIQUE;
        } else {
            return UserConstants.UNIQUE;
        }
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("factoryCode", "lhResultBatchNo", "orderNo", "planDate", "lhMachineCode", "beforeMaterialCode", "afterMaterialCode");
    }

    /**
     * 排程发布
     */
    @Override
    public AjaxResult issueSchedule(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.param.error"));
        }

        // 1. 加锁防止多次下发
        Long[] idArray = ids.toArray(new Long[0]);
        String lockKey = "lhMouldChangePlan:issue:lock" + Arrays.toString(idArray);
        if (redisService.getCacheObject(lockKey)) {
            return AjaxResult.success();
        }
        try {
            redisService.setCacheObject(lockKey,"1");
            // 2. 查询选中记录
            QueryWrapper<LhMouldChangePlan> wrapper = new QueryWrapper<>();
            wrapper.in("ID", ids);
            List<LhMouldChangePlan> planList = lhMouldChangePlanMapper.selectList(wrapper);
            if (CollectionUtils.isEmpty(planList)) {
                return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.lhMouldChangePlan.noData"));
            }

            // 3. 校验是否存在已发布的数据
            List<LhMouldChangePlan> releasedList = planList.stream()
                    .filter(item -> ApsConstant.IS_RELEASE.equals(item.getIsRelease()))
                    .collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(releasedList)) {
                return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.lhMouldChangePlan.hasReleasedData"));
            }

            // 4. 转换为MdmMoldAlterPlan
            List<MdmMoldAlterPlan> moldAlterPlanList = new ArrayList<>();
            for (LhMouldChangePlan plan : planList) {
                MdmMoldAlterPlan moldAlterPlan = new MdmMoldAlterPlan();
                BeanUtils.copyProperties(plan, moldAlterPlan);
                moldAlterPlan.setLhBatchNo(plan.getLhResultBatchNo());
                moldAlterPlan.setLeftRightMold(plan.getLeftRightMould());
                moldAlterPlan.setMaterialCode(plan.getBeforeMaterialCode());
                moldAlterPlan.setSpecDesc(plan.getBeforeMaterialDesc());
                moldAlterPlan.setPlanMaterialCode(plan.getAfterMaterialCode());
                moldAlterPlan.setPlanSpecDesc(plan.getAfterMaterialDesc());
                moldAlterPlan.setChangeMoldType(plan.getChangeMouldType());
                moldAlterPlan.setMoldNo(plan.getMouldCode());
                moldAlterPlan.setScheduleDate(plan.getScheduleDate());
                moldAlterPlanList.add(moldAlterPlan);
            }

            // 5. 调用MES接口下发
            try {
                AjaxResult result = mesItfService.issueMoldAlterPlan(moldAlterPlanList);
                if (AjaxResult.Type.ERROR.value() != Integer.parseInt(result.get(AjaxResult.CODE_TAG).toString())) {
                    // 6. 更新发布状态为已发布
                    for (LhMouldChangePlan plan : planList) {
                        plan.setIsRelease(ApsConstant.IS_RELEASE);
                    }
                    this.baseDao.updateBatch(planList);
                    return AjaxResult.success(I18nUtil.getMessage("ui.data.alert.lhMouldChangePlan.issueSuccess"));
                } else {
                    return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.lhMouldChangePlan.issueFail"));
                }
            } catch (Exception e) {
                log.error("排程发布失败", e);
                return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.lhMouldChangePlan.issueFail"));
            }
        }catch (Exception e){
            redisService.deleteObject(lockKey);
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.lhMouldChangePlan.issueFail"));
        } finally {
            redisService.deleteObject(lockKey);
        }
    }
}
