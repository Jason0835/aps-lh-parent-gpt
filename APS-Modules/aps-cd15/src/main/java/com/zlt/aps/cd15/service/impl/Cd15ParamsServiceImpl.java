package com.zlt.aps.cd15.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.RowStateEnum;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cd15.api.domain.entity.Cd15Params;
import com.zlt.aps.cd15.mapper.Cd15ParamsMapper;
import com.zlt.aps.cd15.service.ICd15ParamsService;
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
 * 15度裁断参数设置 服务实现类
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class Cd15ParamsServiceImpl extends AbstractDocService<Cd15Params> implements ICd15ParamsService {

    @Resource
    private Cd15ParamsMapper cd15ParamsMapper;

    @Override
    protected String getDocTypeCode() {
        return "CD15_PARAMS";
    }

    @Override
    public String checkUnique(Cd15Params entity) {
        LambdaQueryWrapper<Cd15Params> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cd15Params::getFactoryCode, entity.getFactoryCode());
        wrapper.eq(Cd15Params::getParamCode, entity.getParamCode());
        wrapper.ne(entity.getId() != null, Cd15Params::getId, entity.getId());
        return cd15ParamsMapper.selectCount(wrapper) > 0 ? UserConstants.NOT_UNIQUE : UserConstants.UNIQUE;
    }

    @Override
    public AjaxResult importData(List<Cd15Params> list, boolean updateSupport, Long importLogId) {
        int successNum = 0, failureNum = 0;
        List<Cd15Params> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        String uniqueMsg = I18nUtil.getMessage("import.validated.unique");

        // 第一遍校验：字段验证和重复行检查
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            Cd15Params docEntity = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, docEntity);
            ImportExcelValidatedUtils.validatedRepeat(list, docEntity, i, 2, importLogId, validated);
            if (CollectionUtils.isNotEmpty(validated)) {
                failureNum++;
                docEntity.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }
        // 第二遍处理：数据库重复检查和插入/更新
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            Cd15Params docEntity = list.get(i);
            // 跳过第一遍校验失败的记录
            if (docEntity.getId() != null && docEntity.getId() == -999L) {
                continue;
            }
            Cd15Params exist = getExistParams(docEntity);
            if (exist == null) {
                // 不存在，新增
                docEntity.setRowState(RowStateEnum.ADDED);
                importList.add(docEntity);
            } else if (updateSupport) {
                // 存在且支持更新，则更新
                exist.setParamName(docEntity.getParamName());
                exist.setParamValue(docEntity.getParamValue());
                exist.setRegularExpression(docEntity.getRegularExpression());
                exist.setErrorTips(docEntity.getErrorTips());
                exist.setRemark(docEntity.getRemark());
                cd15ParamsMapper.updateById(exist);
                successNum++;
            } else {
                // 存在但不支持更新，报错
                failureNum++;
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, errorNum, String.format(uniqueMsg, errorNum), importErrorLogs);
            }
        }

        // 保存新增数据
        if (PubUtil.isEmpty(importList) && successNum == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        }
        if (CollectionUtils.isNotEmpty(importList)) {
            successNum += baseDao.saveBatch(importList);
        }

        // 返回结果
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        }
        return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
    }

    /**
     * 查询是否存在相同工厂和参数编码的记录
     */
    private Cd15Params getExistParams(Cd15Params entity) {
        LambdaQueryWrapper<Cd15Params> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cd15Params::getFactoryCode, entity.getFactoryCode());
        wrapper.eq(Cd15Params::getParamCode, entity.getParamCode());
        return cd15ParamsMapper.selectOne(wrapper);
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("CD15_PARAMS");
        return sysDocType;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("factoryCode", "paramCode");
    }
}