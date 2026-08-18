package com.zlt.aps.dj.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
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
import com.zlt.aps.dj.api.domain.entity.DjLossSetting;
import com.zlt.aps.dj.mapper.DjLossSettingMapper;
import com.zlt.aps.dj.service.DjLossSettingService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.common.utils.PubUtil;

/**
 * 垫胶损耗率设定Service业务层处理
 *
 * @author chen
 * @date 2026-06-10
 */
@Service
public class DjLossSettingServiceImpl extends AbstractDocService<DjLossSetting> implements DjLossSettingService {

    @Resource
    private DjLossSettingMapper lossSettingMapper;

    @Override
    public String checkUnique(DjLossSetting entity) {
        if (StringUtils.isEmpty(entity.getMachineCode()) && StringUtils.isEmpty(entity.getPaddingCode())) {
            throw new RuntimeException(I18nUtil.getMessage("ui.error.message.loss.isAllNull"));
        }
        QueryWrapper<DjLossSetting> queryWrapper = new QueryWrapper<>();
        queryWrapper.ne(PubUtil.isNotEmpty(entity.getFieldValueByFieldName("id")), "ID",
                entity.getFieldValueByFieldName("id"));
        queryWrapper.eq("FACTORY_CODE", entity.getFactoryCode());
        List<DjLossSetting> list = lossSettingMapper.selectList(queryWrapper);

        // 机台、物料号都不为空，看是否有全匹配的
        if (StringUtils.isNotEmpty(entity.getMachineCode()) && StringUtils.isNotEmpty(entity.getPaddingCode())
                && list.stream().anyMatch(item -> Objects.equal(entity.getMachineCode(), item.getMachineCode())
                        && Objects.equal(entity.getPaddingCode(), item.getPaddingCode()))) {
            return UserConstants.NOT_UNIQUE;
        } else if (StringUtils.isNotEmpty(entity.getMachineCode())
                && list.stream().anyMatch(item -> Objects.equal(entity.getMachineCode(), item.getMachineCode())
                        && StringUtils.isEmpty(entity.getPaddingCode()))) {
            return UserConstants.NOT_UNIQUE;
        } else if (StringUtils.isNotEmpty(entity.getPaddingCode())
                && list.stream().anyMatch(item -> Objects.equal(entity.getPaddingCode(), item.getPaddingCode())
                        && StringUtils.isEmpty(entity.getPaddingCode()))) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 唯一校验字段：工厂编码 + 机台编码 + 填充物料号
     *
     * @return 唯一校验字段名列表
     */
    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("factoryCode", "machineCode", "paddingCode");
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
    public AjaxResult importData(List<DjLossSetting> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<DjLossSetting> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        String uniqueMsg = I18nUtil.getMessage("ui.data.alert.cxStock.embryoCodeNotUnique");

        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            DjLossSetting docEntity = list.get(i);
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
            DjLossSetting docEntity = list.get(i);
            if (docEntity.getId() != null && docEntity.getId() == -999L) {
                continue;
            }

            if (checkUnique(docEntity).equals(UserConstants.UNIQUE)) {
                importList.add(docEntity);
                successNum++;
            } else {
                if (updateSupport) {
                    LambdaQueryWrapper<DjLossSetting> queryWrapper = new LambdaQueryWrapper<>();
                    queryWrapper.eq(DjLossSetting::getFactoryCode, docEntity.getFactoryCode());
                    queryWrapper.eq(DjLossSetting::getMachineCode, docEntity.getMachineCode());
                    queryWrapper.eq(DjLossSetting::getPaddingCode, docEntity.getPaddingCode());
                    logger.info("updateSupport:{}", docEntity);
                    List<DjLossSetting> existList = lossSettingMapper.selectList(queryWrapper);
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

        for (DjLossSetting entity : importList) {
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
