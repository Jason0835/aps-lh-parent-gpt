package com.zlt.aps.nc.service.impl;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import com.alibaba.nacos.shaded.com.google.common.base.Objects;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.nc.api.domain.entity.NcLossSetting;
import com.zlt.aps.nc.mapper.NcLossSettingMapper;
import com.zlt.aps.nc.service.NcLossSettingService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.common.utils.PubUtil;

/**
 * 内衬损耗率设定Service业务层处理
 *
 * @author chen
 * @date 2021-07-13
 */
@Service
public class NcLossSettingServiceImpl extends AbstractDocService<NcLossSetting> implements NcLossSettingService {

    @Resource
    private NcLossSettingMapper lossSettingMapper;

    @Override
    public String checkUnique(NcLossSetting entity) {
        if (StringUtils.isEmpty(entity.getMachineCode()) && StringUtils.isEmpty(entity.getLiningCode())) {
            throw new RuntimeException(I18nUtil.getMessage("ui.error.message.loss.isAllNull"));
        }
        QueryWrapper<NcLossSetting> queryWrapper = new QueryWrapper<>();
        queryWrapper.ne(PubUtil.isNotEmpty(entity.getFieldValueByFieldName("id")), "ID",
                entity.getFieldValueByFieldName("id"));
        queryWrapper.eq("FACTORY_CODE", entity.getFactoryCode());
        List<NcLossSetting> list = lossSettingMapper.selectList(queryWrapper);

        // 机台、物料号都不为空，看是否有全匹配的
        if (StringUtils.isNotEmpty(entity.getMachineCode()) && StringUtils.isNotEmpty(entity.getLiningCode())
                && list.stream().anyMatch(item -> Objects.equal(entity.getMachineCode(), item.getMachineCode())
                        && Objects.equal(entity.getLiningCode(), item.getLiningCode()))) {
            return UserConstants.NOT_UNIQUE;
        } else if (StringUtils.isNotEmpty(entity.getMachineCode())
                && list.stream().anyMatch(item -> Objects.equal(entity.getMachineCode(), item.getMachineCode())
                        && StringUtils.isEmpty(entity.getLiningCode()))) {
            return UserConstants.NOT_UNIQUE;
        } else if (StringUtils.isNotEmpty(entity.getLiningCode())
                && list.stream().anyMatch(item -> Objects.equal(entity.getLiningCode(), item.getLiningCode())
                        && StringUtils.isEmpty(entity.getLiningCode()))) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    @Override
    public AjaxResult importData(List<NcLossSetting> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<NcLossSetting> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        String uniqueMsg = I18nUtil.getMessage("ui.data.alert.cxStock.embryoCodeNotUnique");

        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            NcLossSetting docEntity = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, docEntity);
            ImportExcelValidatedUtils.validatedRepeat(list, docEntity, i, 2, importLogId, validated,
                    this.getCheckUniqueFields().toArray(new String[0]));
            if (CollectionUtils.isNotEmpty(validated)) {
                failureNum++;
                docEntity.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }

        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            NcLossSetting docEntity = list.get(i);
            if (docEntity.getId() != null && docEntity.getId() == -999L) {
                continue;
            }

            if (checkUnique(docEntity).equals(UserConstants.UNIQUE)) {
                importList.add(docEntity);
                successNum++;
            } else {
                if (updateSupport) {
                    LambdaQueryWrapper<NcLossSetting> queryWrapper = new LambdaQueryWrapper<>();
                    queryWrapper.eq(NcLossSetting::getFactoryCode, docEntity.getFactoryCode());
                    queryWrapper.eq(NcLossSetting::getMachineCode, docEntity.getMachineCode());
                    queryWrapper.eq(NcLossSetting::getLiningCode, docEntity.getLiningCode());
                    logger.info("updateSupport:{}", docEntity);
                    List<NcLossSetting> existList = lossSettingMapper.selectList(queryWrapper);
                    if (existList.size() > 1) {
                        failureNum++;
                        String multipleMsg = I18nUtil.getMessage("ui.data.alert.cxStock.multipleRecords");
                        ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                                errorNum, String.format(multipleMsg, errorNum), importErrorLogs);
                        continue;
                    } else if (existList.size() == 1) {
                        docEntity.setId(existList.get(0).getId());
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
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum,
                    importErrorLogs);
        }

        for (NcLossSetting entity : importList) {
            if (entity.getId() != null) {
                lossSettingMapper.updateById(entity);
            } else {
                lossSettingMapper.insert(entity);
            }
        }

        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum,
                    importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

    @Override
    protected String getDocTypeCode() {
        return "";
    }
}
