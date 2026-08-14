package com.zlt.aps.dj.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.dj.api.domain.entity.DjMachineInfo;
import com.zlt.aps.dj.mapper.DjMachineInfoMapper;
import com.zlt.aps.dj.service.DjMachineInfoService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.common.utils.PubUtil;

/**
 * 垫胶机台信息Service业务层处理
 *
 * @author zlt
 * @date 2026-05-28
 */
@Service
public class DjMachineInfoServiceImpl extends AbstractDocService<DjMachineInfo> implements DjMachineInfoService {
    @Autowired
    private DjMachineInfoMapper machineMapper;

    /**
     * 获取机台表
     * 
     * @param queryParams 查询参数
     * @return
     */
    @Override
    public List<DjMachineInfo> selectMachineInfoList(DjMachineInfo queryParams) {
        QueryWrapper<DjMachineInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("FACTORY_CODE", queryParams.getFactoryCode());
        queryWrapper.eq(StringUtils.isNoneEmpty(queryParams.getMachineCode()), "MACHINE_CODE", queryParams.getMachineCode());
        return machineMapper.selectList(queryWrapper);
    }

    @Override
    public String checkUnique(DjMachineInfo entity) {
        QueryWrapper<DjMachineInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.ne(PubUtil.isNotEmpty(entity.getFieldValueByFieldName("id")), "ID",
                entity.getFieldValueByFieldName("id"));
        queryWrapper.eq("FACTORY_CODE", entity.getFactoryCode());
        queryWrapper.eq("MACHINE_CODE", entity.getMachineCode());

        if (machineMapper.selectCount(queryWrapper) > 0) {
            return UserConstants.NOT_UNIQUE;
        } else {
            return UserConstants.UNIQUE;
        }
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
    public AjaxResult importData(List<DjMachineInfo> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<DjMachineInfo> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        String uniqueMsg = I18nUtil.getMessage("ui.data.alert.djMachine.machineCodeExists");

        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            DjMachineInfo docEntity = list.get(i);
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
            DjMachineInfo docEntity = list.get(i);
            if (docEntity.getId() != null && docEntity.getId() == -999L) {
                continue;
            }

            if (checkUnique(docEntity).equals(UserConstants.UNIQUE)) {
                importList.add(docEntity);
                successNum++;
            } else {
                if (updateSupport) {
                    LambdaQueryWrapper<DjMachineInfo> queryWrapper = new LambdaQueryWrapper<>();
                    queryWrapper.eq(DjMachineInfo::getFactoryCode, docEntity.getFactoryCode());
                    queryWrapper.eq(DjMachineInfo::getMachineCode, docEntity.getMachineCode());
                    logger.info("updateSupport:{}", docEntity);
                    List<DjMachineInfo> existList = machineMapper.selectList(queryWrapper);
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
        baseDao.saveBatch(importList);

        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum,
                    importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }
    
    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("factoryCode", "machineCode");
    }

    @Override
    protected String getDocTypeCode() {
        return "";
    }
}
