package com.zlt.aps.cd15.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.RowStateEnum;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cd15.api.domain.entity.Cd15MachineInfo;
import com.zlt.aps.cd15.mapper.Cd15MachineInfoMapper;
import com.zlt.aps.cd15.service.ICd15MachineInfoService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.common.utils.PubUtil;
import com.zlt.sysdef.domain.SysDocType;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 斜裁机台基础信息业务实现。
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class Cd15MachineInfoServiceImpl extends AbstractDocService<Cd15MachineInfo> implements ICd15MachineInfoService {

    @Resource
    private Cd15MachineInfoMapper cd15MachineInfoMapper;

    @Override
    protected String getDocTypeCode() {
        return "CD15_MACHINE_INFO";
    }

    /**
     * 校验同一工厂下机台编号是否唯一。
     *
     * @param machineInfo 机台信息
     * @return 唯一性标识
     */
    @Override
    public String checkUnique(Cd15MachineInfo machineInfo) {
        LambdaQueryWrapper<Cd15MachineInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cd15MachineInfo::getFactoryCode, machineInfo.getFactoryCode());
        wrapper.eq(Cd15MachineInfo::getMachineCode, machineInfo.getMachineCode());
        wrapper.ne(machineInfo.getId() != null, Cd15MachineInfo::getId, machineInfo.getId());
        return cd15MachineInfoMapper.selectCount(wrapper) > 0 ? UserConstants.NOT_UNIQUE : UserConstants.UNIQUE;
    }

    /**
     * 导入斜裁机台基础信息。
     *
     * @param list 导入列表
     * @param updateSupport 是否更新已有数据
     * @param importLogId 导入日志 ID
     * @return 导入结果
     */
    @Override
    public AjaxResult importData(List<Cd15MachineInfo> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<Cd15MachineInfo> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        String uniqueMsg = I18nUtil.getMessage("import.validated.unique");

        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            Cd15MachineInfo docEntity = list.get(i);
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
            Cd15MachineInfo docEntity = list.get(i);
            if (docEntity.getId() != null && docEntity.getId() == -999L) {
                continue;
            }
            if (docEntity.getQuota() == null || docEntity.getQuota() <= 0) {
                failureNum++;
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                        errorNum, I18nUtil.getMessage("ui.data.alert.cd15MachineInfo.quotaPositive"), importErrorLogs);
                continue;
            }

            Cd15MachineInfo exist = getExistMachine(docEntity);
            if (exist == null) {
                docEntity.setRowState(RowStateEnum.ADDED);
                importList.add(docEntity);
            } else if (updateSupport) {
                exist.setMachineName(docEntity.getMachineName());
                exist.setClothWidthMax(docEntity.getClothWidthMax());
                exist.setClothWidthMin(docEntity.getClothWidthMin());
                exist.setQuota(docEntity.getQuota());
                exist.setClassShift(docEntity.getClassShift());
                exist.setOpenMachineClass(docEntity.getOpenMachineClass());
                exist.setIsOutTwo(docEntity.getIsOutTwo());
                exist.setStatus(docEntity.getStatus());
                exist.setSteelStripWidth(docEntity.getSteelStripWidth());
                exist.setRemark(docEntity.getRemark());
                cd15MachineInfoMapper.updateById(exist);
                successNum++;
            } else {
                failureNum++;
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, errorNum,
                        String.format(uniqueMsg, errorNum), importErrorLogs);
            }
        }

        if (PubUtil.isEmpty(importList) && successNum == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        }
        if (CollectionUtils.isNotEmpty(importList)) {
            successNum += baseDao.saveBatch(importList);
        }
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        }
        return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
    }

    private Cd15MachineInfo getExistMachine(Cd15MachineInfo machineInfo) {
        LambdaQueryWrapper<Cd15MachineInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cd15MachineInfo::getFactoryCode, machineInfo.getFactoryCode());
        wrapper.eq(Cd15MachineInfo::getMachineCode, machineInfo.getMachineCode());
        return cd15MachineInfoMapper.selectOne(wrapper);
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("CD15_MACHINE_INFO");
        return sysDocType;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("factoryCode", "machineCode");
    }
}
