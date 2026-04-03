package com.zlt.aps.maindata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.RowStateEnum;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.maindata.mapper.LhMachineInfoEntityMapper;
import com.zlt.aps.maindata.mapper.MdmMouldCleanPlanEntityMapper;
import com.zlt.aps.maindata.service.IMdmMouldCleanPlanService;
import com.zlt.aps.mp.api.domain.entity.LhMachineInfo;
import com.zlt.aps.mdm.api.domain.entity.MdmMouldCleanPlan;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.common.utils.PubUtil;
import jodd.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * APS模具清洗预警计划Service实现
 *
 * @author zlt
 * @since 2025/12/25
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class MdmMouldCleanPlanServiceImpl extends AbstractDocService<MdmMouldCleanPlan> implements IMdmMouldCleanPlanService {

    @Resource
    private MdmMouldCleanPlanEntityMapper mdmMouldCleanPlanEntityMapper;

    @Autowired
    private LhMachineInfoEntityMapper lhMachineInfoEntityMapper;

    @Override
    protected String getDocTypeCode() {
        return "0";
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
    public AjaxResult importData(List<MdmMouldCleanPlan> list, boolean updateSupport, Long importLogId) {
        // 0.初始化
        int successNum = 0;
        int failureNum = 0;
        List<MdmMouldCleanPlan> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        String uniqueMsg = I18nUtil.getMessage("import.validated.unique");

        // 1.进行非空校验,Excel中数据重复校验
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            MdmMouldCleanPlan docEntity = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, docEntity);
            ImportExcelValidatedUtils.validatedRepeat(list, docEntity, i, 2, importLogId, validated);
            if (CollectionUtils.isNotEmpty(validated)) {
                failureNum++;
                docEntity.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }

        // 2.进行数据库唯一性校验 + 硫化机台存在性校验
        // 先批量查询，提升性能
        Map<String, List<MdmMouldCleanPlan>> factoryCodeMap = list.stream()
                .collect(Collectors.groupingBy(MdmMouldCleanPlan::getFactoryCode));

        // 查询硫化机台
        Map<String, LhMachineInfo> machineInfoMap = new HashMap<>(16);
        if (!factoryCodeMap.isEmpty()) {
            for (String factoryCode : factoryCodeMap.keySet()) {
                List<MdmMouldCleanPlan> itemList = factoryCodeMap.get(factoryCode);
                List<String> machineCodeList = itemList.stream()
                        .map(MdmMouldCleanPlan::getLhCode)
                        .distinct()
                        .collect(Collectors.toList());
                List<List<String>> splitList = com.zlt.aps.maindata.utils.CollectionUtils.splitList(machineCodeList, 900);
                List<LhMachineInfo> machineInfoList = new ArrayList<>();
                for (List<String> codeList : splitList) {
                    LambdaQueryWrapper<LhMachineInfo> wrapper = new LambdaQueryWrapper<LhMachineInfo>();
                    wrapper.in(LhMachineInfo::getMachineCode, codeList);
                    wrapper.eq(LhMachineInfo::getFactoryCode, factoryCode);
                    machineInfoList.addAll(lhMachineInfoEntityMapper.selectList(wrapper));
                }
                if (CollectionUtils.isNotEmpty(machineInfoList)) {
                    machineInfoMap = machineInfoList.stream().collect(Collectors
                            .toMap(x -> x.getFactoryCode() + "," + x.getMachineCode(), machine -> machine));
                }
            }
        }

        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            MdmMouldCleanPlan docEntity = list.get(i);
            if (docEntity.getId() != null && docEntity.getId() == -999L) {
                continue;
            }

            // 检查硫化机台是否存在
            if (!machineInfoMap.containsKey(docEntity.getFactoryCode() + "," + docEntity.getLhCode())) {
                failureNum++;
                String message = I18nUtil.getMessage("ui.data.alert.mouldCleanPlan.lhCodeNotExist");
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                        errorNum, String.format(message, errorNum), importErrorLogs);
                continue;
            }

            if (checkUnique(docEntity).equals(UserConstants.UNIQUE)) {
                docEntity.setRowState(RowStateEnum.ADDED);
                if (StringUtil.isBlank(docEntity.getFactoryCode())) {
                    docEntity.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
                }
                importList.add(docEntity);
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
    public String checkUnique(MdmMouldCleanPlan docEntityVO) {
        // 唯一性判断依据: 根据业务修改
        QueryWrapper<MdmMouldCleanPlan> queryWrapper = new QueryWrapper<>();
        queryWrapper.ne(PubUtil.isNotEmpty(docEntityVO.getFieldValueByFieldName("id")), "ID", docEntityVO.getFieldValueByFieldName("id"));
        // 校验维度: factoryCode + lhCode + operTime
        queryWrapper.eq("FACTORY_CODE", docEntityVO.getFactoryCode());
        queryWrapper.eq("LH_CODE", docEntityVO.getLhCode());
        queryWrapper.eq("OPER_TIME", docEntityVO.getOperTime());
//        queryWrapper.eq("DEL_FLAG", ApsConstant.DEL_FLAG_NORMAL);

        if (mdmMouldCleanPlanEntityMapper.selectCount(queryWrapper) > 0) {
            return UserConstants.NOT_UNIQUE;
        } else {
            return UserConstants.UNIQUE;
        }
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("factoryCode", "lhCode", "operTime");
    }

}
