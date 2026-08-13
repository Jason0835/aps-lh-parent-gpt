package com.zlt.aps.tc.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.tc.api.domain.entity.TcLossSetting;
import com.zlt.aps.tc.api.domain.entity.TcMachineInfo;
import com.zlt.aps.tc.mapper.TcLossSettingMapper;
import com.zlt.aps.tc.mapper.TcMachineInfoMapper;
import com.zlt.aps.tc.service.ITcLossSettingService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.common.utils.PubUtil;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class TcLossSettingServiceImpl extends AbstractDocService<TcLossSetting> implements ITcLossSettingService {

    @Resource
    private TcLossSettingMapper tcLossSettingMapper;

    @Resource
    private TcMachineInfoMapper tcMachineInfoMapper;

    @Override
    protected String getDocTypeCode() {
        return "TC0910";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("TC0910");
        return sysDocType;
    }

    /**
     * 校验损耗率配置必须填写非空损耗率。
     *
     * @param entity 待校验的胎侧损耗配置
     * @throws ServiceException 损耗率为空时抛出
     */
    @Override
    public void validateLossRate(TcLossSetting entity) {
        if (entity == null || entity.getLossRate() == null) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tc.lossSetting.lossRateRequired"));
        }
    }

    @Override
    public String checkUnique(TcLossSetting query) {
        // 校验胎侧编码与机台编码不能同时为空
        if (StringUtils.isBlank(query.getSidewallCode()) && StringUtils.isBlank(query.getMachineCode())) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tc.lossSetting.bothEmpty"));
        }
        if (StringUtils.isBlank(query.getSidewallCode())) {
            query.setSidewallCode("");
        }
        if (StringUtils.isBlank(query.getMachineCode())) {
            query.setMachineCode("");
        }
        String unique = super.checkUnique(query);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tc.lossSetting.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return new ArrayList<>(Arrays.asList("factoryCode", "sidewallCode", "machineCode"));
    }

    @Override
    public int removeByIds(List<Long> ids) {
        if (PubUtil.isEmpty(ids)) {
            return 0;
        }
        // 逻辑删除全局配置下 selectBatchIds 仅返回 IS_DELETE=0 的活跃记录
        List<TcLossSetting> list = tcLossSettingMapper.selectBatchIds(ids);
        // 清理同 (FACTORY_CODE, SIDEWALL_CODE, MACHINE_CODE) 的历史墓碑，避免逻辑删除 0->1 时
        // 唯一索引 uk_tc_loss_setting_factory_sidewall_machine 冲突（#23294）
        for (TcLossSetting item : list) {
            String sidewallCode = item.getSidewallCode() == null ? "" : item.getSidewallCode();
            String machineCode = item.getMachineCode() == null ? "" : item.getMachineCode();
            tcLossSettingMapper.physicalDeleteTombstones(item.getFactoryCode(), sidewallCode, machineCode);
        }
        return super.removeByIds(ids);
    }

    @Override
    protected Map<Object, Object> getServiceCheckParams(List<TcLossSetting> list, List<TcLossSetting> importList) {
        Map<Object, Object> serviceCheckParams = super.getServiceCheckParams(list, importList);
        // 提取所有非空、去重的机台编码
        List<String> machineCodeList = list.stream()
                .map(TcLossSetting::getMachineCode)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        // 分批查询机台基础数据
        List<List<String>> splitList = ListUtil.split(machineCodeList, 500);
        List<TcMachineInfo> machineInfoList = new ArrayList<>();
        for (List<String> codes : splitList) {
            LambdaQueryWrapper<TcMachineInfo> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(TcMachineInfo::getMachineCode, codes);
            machineInfoList.addAll(tcMachineInfoMapper.selectList(wrapper));
        }
        if (CollUtil.isNotEmpty(machineInfoList)) {
            serviceCheckParams.put("tcMachineCodeList",
                    machineInfoList.stream().map(TcMachineInfo::getMachineCode).collect(Collectors.toList()));
        }
        // 查询已存在的、机台编码为空的活跃记录（#23296）：
        // 框架 checkCodeOrNameExists 对空值用 IS NULL 查询，而库表存的是 ''，
        // 导致这类已存在记录漏判，导入 INSERT 时触发唯一索引冲突。
        List<String> factoryCodes = list.stream()
                .map(TcLossSetting::getFactoryCode)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        if (!factoryCodes.isEmpty()) {
            LambdaQueryWrapper<TcLossSetting> existWrapper = new LambdaQueryWrapper<>();
            existWrapper.in(TcLossSetting::getFactoryCode, factoryCodes)
                    .and(w -> w.eq(TcLossSetting::getMachineCode, "").or().isNull(TcLossSetting::getMachineCode));
            Set<String> existingEmptyMachineKeys = tcLossSettingMapper.selectList(existWrapper).stream()
                    .map(e -> e.getFactoryCode() + "|" + (e.getSidewallCode() == null ? "" : e.getSidewallCode()))
                    .collect(Collectors.toSet());
            serviceCheckParams.put("existingLossEmptyMachineKeys", existingEmptyMachineKeys);
        }
        return serviceCheckParams;
    }

    @Override
    protected Boolean serviceCheckAndDataHandle(TcLossSetting importDocEntity, List<ImportErrorLog> importErrorLogs,
                                                Long importLogId, int errorRowNum, Map<Object, Object> serviceCheckParams) {
        if (importDocEntity.getLossRate() == null) {
            String message = I18nUtil.getMessage("ui.data.alert.tc.lossSetting.lossRateRequired");
            ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(), errorRowNum, message, importErrorLogs);
            return Boolean.FALSE;
        }
        // 校验胎侧编码与机台编码不能同时为空
        String sidewallCode = importDocEntity.getSidewallCode();
        String machineCode = importDocEntity.getMachineCode();
        if (StringUtils.isBlank(sidewallCode) && StringUtils.isBlank(machineCode)) {
            String message = I18nUtil.getMessage("ui.data.alert.tc.lossSetting.bothEmpty");
            ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(), errorRowNum, message, importErrorLogs);
            return Boolean.FALSE;
        }
        // 机台编码为空时，框架 checkCodeOrNameExists 用 IS NULL 查询会漏判库表中 MACHINE_CODE='' 的已存在记录，
        // 导致导入 INSERT 触发唯一索引冲突；此处补判并跳过（#23296）
        if (StringUtils.isBlank(machineCode) && serviceCheckParams.containsKey("existingLossEmptyMachineKeys")) {
            Set<String> existingKeys = (Set<String>) serviceCheckParams.get("existingLossEmptyMachineKeys");
            String key = importDocEntity.getFactoryCode() + "|" + (sidewallCode == null ? "" : sidewallCode);
            if (existingKeys.contains(key)) {
                String message = I18nUtil.getMessage("ui.data.alert.tc.lossSetting.notUnique");
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(), errorRowNum, message, importErrorLogs);
                return Boolean.FALSE;
            }
        }
        // 校验机台编码是否存在（非空时校验）
        if (StringUtils.isNotBlank(machineCode) && serviceCheckParams.containsKey("tcMachineCodeList")) {
            List<String> machineCodeList = (List<String>) serviceCheckParams.get("tcMachineCodeList");
            if (!machineCodeList.contains(machineCode)) {
                String message = I18nUtil.getMessage("ui.data.alert.tc.machineCodeNotExist");
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(), errorRowNum, message, importErrorLogs);
                return Boolean.FALSE;
            }
        }
        return super.serviceCheckAndDataHandle(importDocEntity, importErrorLogs, importLogId, errorRowNum, serviceCheckParams);
    }
}
