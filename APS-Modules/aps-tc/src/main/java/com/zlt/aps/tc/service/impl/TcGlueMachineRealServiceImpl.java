package com.zlt.aps.tc.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.tc.api.domain.entity.TcGlueMachineReal;
import com.zlt.aps.tc.api.domain.entity.TcMachineInfo;
import com.zlt.aps.tc.mapper.TcGlueMachineRealMapper;
import com.zlt.aps.tc.mapper.TcMachineInfoMapper;
import com.zlt.aps.tc.service.ITcGlueMachineRealService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.common.utils.PubUtil;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class TcGlueMachineRealServiceImpl extends AbstractDocService<TcGlueMachineReal> implements ITcGlueMachineRealService {

    @Resource
    private TcGlueMachineRealMapper tcGlueMachineRealMapper;

    @Resource
    private TcMachineInfoMapper tcMachineInfoMapper;

    @Override
    protected String getDocTypeCode() {
        return "TC0902";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("TC0902");
        return sysDocType;
    }

    @Override
    public String checkUnique(TcGlueMachineReal query) {
        String unique = super.checkUnique(query);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tc.glueMachineReal.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return new ArrayList<>(Arrays.asList("factoryCode", "glueCode", "machineCode"));
    }

    @Override
    public int removeByIds(List<Long> ids) {
        if (PubUtil.isEmpty(ids)) {
            return 0;
        }
        // 逻辑删除全局配置下 selectBatchIds 仅返回 IS_DELETE=0 的活跃记录
        List<TcGlueMachineReal> list = tcGlueMachineRealMapper.selectBatchIds(ids);
        // 清理同 (FACTORY_CODE, GLUE_CODE, MACHINE_CODE) 的历史墓碑，避免逻辑删除 0->1 时
        // 唯一索引 uk_tc_glue_machine_real_factory_glue_machine 冲突（#23310）
        for (TcGlueMachineReal item : list) {
            tcGlueMachineRealMapper.physicalDeleteTombstones(item.getFactoryCode(), item.getGlueCode(), item.getMachineCode());
        }
        return super.removeByIds(ids);
    }

    @Override
    protected Map<Object, Object> getServiceCheckParams(List<TcGlueMachineReal> list, List<TcGlueMachineReal> importList) {
        Map<Object, Object> serviceCheckParams = super.getServiceCheckParams(list, importList);
        // 提取所有非空、去重的机台编码
        List<String> machineCodeList = list.stream()
                .map(TcGlueMachineReal::getMachineCode)
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
        return serviceCheckParams;
    }

    @Override
    protected Boolean serviceCheckAndDataHandle(TcGlueMachineReal importDocEntity, List<ImportErrorLog> importErrorLogs,
                                                Long importLogId, int errorRowNum, Map<Object, Object> serviceCheckParams) {
        // 校验机台编码是否存在
        if (serviceCheckParams.containsKey("tcMachineCodeList")) {
            List<String> machineCodeList = (List<String>) serviceCheckParams.get("tcMachineCodeList");
            String machineCode = importDocEntity.getMachineCode();
            if (!machineCodeList.contains(machineCode)) {
                String message = I18nUtil.getMessage("ui.data.alert.tc.machineCodeNotExist");
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(), errorRowNum, message, importErrorLogs);
                return Boolean.FALSE;
            }
        }
        return super.serviceCheckAndDataHandle(importDocEntity, importErrorLogs, importLogId, errorRowNum, serviceCheckParams);
    }
}