package com.zlt.aps.cd90.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.RowStateEnum;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cd90.api.domain.entity.Cd90MachineInfo;
import com.zlt.aps.cd90.api.domain.entity.Cd90SpecifyMachine;
import com.zlt.aps.cd90.mapper.Cd90MachineInfoMapper;
import com.zlt.aps.cd90.mapper.Cd90SpecifyMachineMapper;
import com.zlt.aps.cd90.service.ICd90SpecifyMachineService;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.common.utils.PubUtil;
import com.zlt.sysdef.domain.SysDocType;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 直裁定点机台业务实现。
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class Cd90SpecifyMachineServiceImpl extends AbstractDocService<Cd90SpecifyMachine> implements ICd90SpecifyMachineService {

    @Resource
    private Cd90SpecifyMachineMapper cd90SpecifyMachineMapper;

    @Resource
    private Cd90MachineInfoMapper cd90MachineInfoMapper;

    @Override
    protected String getDocTypeCode() {
        return "CD90_SPECIFY_MACHINE";
    }

    @Override
    public int save(Cd90SpecifyMachine entity) {
        normalize(entity);
        return super.save(entity);
    }

    /**
     * 校验同一工厂、帘布、机台下是否已存在配置（作业类型不纳入唯一性校验）。
     *
     * @param specifyMachine 定点机台配置
     * @return 唯一性标识
     */
    @Override
    public String checkUnique(Cd90SpecifyMachine specifyMachine) {
        normalize(specifyMachine);
        LambdaQueryWrapper<Cd90SpecifyMachine> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cd90SpecifyMachine::getFactoryCode, specifyMachine.getFactoryCode());
        wrapper.eq(Cd90SpecifyMachine::getClothCode, specifyMachine.getClothCode());
        wrapper.eq(Cd90SpecifyMachine::getMachineCode, specifyMachine.getMachineCode());
        wrapper.ne(specifyMachine.getId() != null, Cd90SpecifyMachine::getId, specifyMachine.getId());
        return cd90SpecifyMachineMapper.selectCount(wrapper) > 0 ? UserConstants.NOT_UNIQUE : UserConstants.UNIQUE;
    }

    @Override
    public AjaxResult importData(List<Cd90SpecifyMachine> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<Cd90SpecifyMachine> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        String uniqueMsg = I18nUtil.getMessage("import.validated.unique");

        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            Cd90SpecifyMachine docEntity = list.get(i);
            normalize(docEntity);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, docEntity);
            ImportExcelValidatedUtils.validatedRepeat(list, docEntity, i, 2, importLogId, validated,
                    getCheckUniqueFields().toArray(new String[0]));
            if (!isMachineEnabled(docEntity)) {
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                        errorNum, I18nUtil.getMessage("ui.data.column.cd90SpecifyMachine.machineInvalid"), validated);
            }
            if (CollectionUtils.isNotEmpty(validated)) {
                failureNum++;
                docEntity.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }

        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            Cd90SpecifyMachine docEntity = list.get(i);
            if (docEntity.getId() != null && docEntity.getId() == -999L) {
                continue;
            }
            Cd90SpecifyMachine exist = getExistSpecifyMachine(docEntity);
            if (exist == null) {
                docEntity.setRowState(RowStateEnum.ADDED);
                importList.add(docEntity);
            } else if (updateSupport) {
                exist.setRemark(docEntity.getRemark());
                cd90SpecifyMachineMapper.updateById(exist);
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

    private Cd90SpecifyMachine getExistSpecifyMachine(Cd90SpecifyMachine specifyMachine) {
        LambdaQueryWrapper<Cd90SpecifyMachine> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cd90SpecifyMachine::getFactoryCode, specifyMachine.getFactoryCode());
        wrapper.eq(Cd90SpecifyMachine::getClothCode, specifyMachine.getClothCode());
        wrapper.eq(Cd90SpecifyMachine::getMachineCode, specifyMachine.getMachineCode());
        return cd90SpecifyMachineMapper.selectOne(wrapper);
    }

    private boolean isMachineEnabled(Cd90SpecifyMachine specifyMachine) {
        if (StringUtils.isBlank(specifyMachine.getFactoryCode()) || StringUtils.isBlank(specifyMachine.getMachineCode())) {
            return false;
        }
        LambdaQueryWrapper<Cd90MachineInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cd90MachineInfo::getFactoryCode, specifyMachine.getFactoryCode());
        wrapper.eq(Cd90MachineInfo::getMachineCode, specifyMachine.getMachineCode());
        wrapper.eq(Cd90MachineInfo::getStatus, ApsConstant.APS_STRING_1);
        return cd90MachineInfoMapper.selectCount(wrapper) > 0;
    }

    private void normalize(Cd90SpecifyMachine specifyMachine) {
        if (specifyMachine == null) {
            return;
        }
        specifyMachine.setLineType(StringUtils.defaultString(specifyMachine.getLineType()));
        specifyMachine.setJobType(StringUtils.defaultString(specifyMachine.getJobType()));
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("CD90_SPECIFY_MACHINE");
        return sysDocType;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("factoryCode", "clothCode", "machineCode");
    }
}
