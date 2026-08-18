package com.zlt.aps.dj.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
import com.zlt.aps.common.engine.service.FactoryService;
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
    private FactoryService factoryService;

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
        // 统一填充当前工厂编码（导入模板不含工厂列，取自 sys.factory.code 配置）
        String factoryCode = factoryService.getFactoryCode();
        list.forEach(entity -> entity.setFactoryCode(factoryCode));
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

        // 循环外一次性加载当前工厂的全部已有记录，避免在循环内逐笔查询数据库
        Map<String, List<DjMachineInfo>> existMachineMap = this.loadExistMachineMap(factoryCode);

        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            DjMachineInfo docEntity = list.get(i);
            if (docEntity.getId() != null && docEntity.getId() == -999L) {
                continue;
            }

            if (checkUniqueByCache(docEntity, existMachineMap).equals(UserConstants.UNIQUE)) {
                importList.add(docEntity);
                successNum++;
            } else {
                if (updateSupport) {
                    List<DjMachineInfo> existList = existMachineMap.get(this.buildMachineKey(docEntity));
                    logger.info("updateSupport:{}", docEntity);
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

    /**
     * 基于内存中预先加载的已有记录判断唯一性，
     * 与 checkUnique 使用相同的唯一键口径（工厂编码 + 机台编码），
     * 替代导入循环内逐笔调用 checkUnique 查询数据库，提升大数据量导入性能
     *
     * @param entity 待校验的记录
     * @param existMachineMap 预先加载的已有记录，按唯一键分组
     * @return 唯一返回 UserConstants.UNIQUE，否则返回 UserConstants.NOT_UNIQUE
     */
    private String checkUniqueByCache(DjMachineInfo entity, Map<String, List<DjMachineInfo>> existMachineMap) {
        List<DjMachineInfo> existList = existMachineMap.get(this.buildMachineKey(entity));
        if (CollectionUtils.isNotEmpty(existList)) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 一次性加载当前工厂的全部已有机台信息，并按唯一键分组，
     * 供导入时在内存中匹配已有记录，避免逐笔查询数据库
     *
     * @param factoryCode 工厂编码
     * @return 唯一键 -> 已有记录列表
     */
    private Map<String, List<DjMachineInfo>> loadExistMachineMap(String factoryCode) {
        if (StringUtils.isEmpty(factoryCode)) {
            return Collections.emptyMap();
        }
        // 一次批量查询该工厂的全部记录，仅取唯一键匹配所需字段
        List<DjMachineInfo> existList = machineMapper.selectList(new LambdaQueryWrapper<DjMachineInfo>()
                .eq(DjMachineInfo::getFactoryCode, factoryCode)
                .select(DjMachineInfo::getId, DjMachineInfo::getFactoryCode, DjMachineInfo::getMachineCode));
        // 按唯一键分组；排除关键字段为空的记录（与原逐笔 eq 查询口径一致，null 值不会被命中）
        return existList.stream()
                .filter(item -> StringUtils.isNotBlank(item.getFactoryCode())
                        && StringUtils.isNotBlank(item.getMachineCode()))
                .collect(Collectors.groupingBy(this::buildMachineKey));
    }

    /**
     * 构建机台信息唯一键：工厂编码 + 机台编码，用于内存中快速匹配已有记录
     *
     * @param entity 机台信息记录
     * @return 唯一键
     */
    private String buildMachineKey(DjMachineInfo entity) {
        return StringUtils.defaultString(entity.getFactoryCode()) + "|"
                + StringUtils.defaultString(entity.getMachineCode());
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
