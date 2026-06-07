package com.zlt.aps.cd90.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.RowStateEnum;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cd90.api.domain.entity.Cd90Params;
import com.zlt.aps.cd90.mapper.Cd90ParamsMapper;
import com.zlt.aps.cd90.service.ICd90ParamsService;
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

@Service
@Transactional(rollbackFor = Exception.class)
public class Cd90ParamsServiceImpl extends AbstractDocService<Cd90Params> implements ICd90ParamsService {

    @Resource
    private Cd90ParamsMapper cd90ParamsMapper;

    @Override
    protected String getDocTypeCode() { return "CD90_PARAMS"; }

    @Override
    public String checkUnique(Cd90Params entity) {
        LambdaQueryWrapper<Cd90Params> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cd90Params::getFactoryCode, entity.getFactoryCode());
        wrapper.eq(Cd90Params::getParamCode, entity.getParamCode());
        wrapper.ne(entity.getId() != null, Cd90Params::getId, entity.getId());
        return cd90ParamsMapper.selectCount(wrapper) > 0 ? UserConstants.NOT_UNIQUE : UserConstants.UNIQUE;
    }

    @Override
    public AjaxResult importData(List<Cd90Params> list, boolean updateSupport, Long importLogId) {
        int successNum = 0, failureNum = 0;
        List<Cd90Params> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        String uniqueMsg = I18nUtil.getMessage("import.validated.unique");

        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            Cd90Params docEntity = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, docEntity);
            ImportExcelValidatedUtils.validatedRepeat(list, docEntity, i, 2, importLogId, validated);
            if (CollectionUtils.isNotEmpty(validated)) { failureNum++; docEntity.setId(-999L); importErrorLogs.addAll(validated); }
        }
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            Cd90Params docEntity = list.get(i);
            if (docEntity.getId() != null && docEntity.getId() == -999L) continue;
            Cd90Params exist = getExistParams(docEntity);
            if (exist == null) { docEntity.setRowState(RowStateEnum.ADDED); importList.add(docEntity); }
            else if (updateSupport) { exist.setParamName(docEntity.getParamName()); exist.setParamValue(docEntity.getParamValue()); exist.setRegularExpression(docEntity.getRegularExpression()); exist.setErrorTips(docEntity.getErrorTips()); exist.setRemark(docEntity.getRemark()); cd90ParamsMapper.updateById(exist); successNum++; }
            else { failureNum++; ImportExcelValidatedUtils.addImportErrorLog(importLogId, errorNum, String.format(uniqueMsg, errorNum), importErrorLogs); }
        }
        if (PubUtil.isEmpty(importList) && successNum == 0) return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        if (CollectionUtils.isNotEmpty(importList)) successNum += baseDao.saveBatch(importList);
        if (failureNum > 0) return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
    }

    private Cd90Params getExistParams(Cd90Params entity) {
        LambdaQueryWrapper<Cd90Params> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cd90Params::getFactoryCode, entity.getFactoryCode());
        wrapper.eq(Cd90Params::getParamCode, entity.getParamCode());
        return cd90ParamsMapper.selectOne(wrapper);
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("CD90_PARAMS");
        return sysDocType;
    }

    @Override
    protected List<String> getCheckUniqueFields() { return Arrays.asList("factoryCode", "paramCode"); }
}