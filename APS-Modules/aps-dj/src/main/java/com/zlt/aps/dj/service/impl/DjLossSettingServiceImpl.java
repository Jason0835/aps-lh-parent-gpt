package com.zlt.aps.dj.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.text.MessageFormat;

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
import com.zlt.aps.common.engine.service.FactoryService;
import com.zlt.aps.dj.api.domain.entity.DjLossSetting;
import com.zlt.aps.dj.api.domain.entity.DjMachineInfo;
import com.zlt.aps.dj.mapper.DjLossSettingMapper;
import com.zlt.aps.dj.mapper.DjMachineInfoMapper;
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
    private FactoryService factoryService;

    @Resource
    private DjLossSettingMapper lossSettingMapper;

    @Resource
    private DjMachineInfoMapper machineInfoMapper;

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

        // 唯一判断逻辑抽取至 isUnique，供逐笔校验与导入内存校验共用，保证口径一致
        if (this.isUnique(entity, list)) {
            return UserConstants.UNIQUE;
        }
        return UserConstants.NOT_UNIQUE;
    }

    /**
     * 判断损耗率记录是否与已有记录存在唯一冲突，规则与原 checkUnique 一致：
     * 1. 机台码与物料号均非空：存在两项全匹配的记录则冲突
     * 2. 机台码非空：存在同机台码且物料号为空的记录则冲突
     * 3. 物料号非空：存在同物料号且机台码为空的记录则冲突
     * 抽取为内存判断，供导入时传入预先加载的数据校验，避免逐笔查询数据库
     *
     * @param entity 待校验记录
     * @param existList 已有记录（同一工厂）
     * @return 存在唯一冲突返回 false，唯一返回 true
     */
    private boolean isUnique(DjLossSetting entity, List<DjLossSetting> existList) {
        // 机台码、物料号均非空时，存在两项全匹配的记录则冲突
        if (StringUtils.isNotEmpty(entity.getMachineCode()) && StringUtils.isNotEmpty(entity.getPaddingCode())) {
            if (existList.stream().anyMatch(item -> Objects.equal(entity.getMachineCode(), item.getMachineCode())
                    && Objects.equal(entity.getPaddingCode(), item.getPaddingCode()))) {
                return false;
            }
        }
        // 机台码非空时，存在同机台码且物料号为空的记录则冲突
        if (StringUtils.isNotEmpty(entity.getMachineCode())) {
            if (existList.stream().anyMatch(item -> Objects.equal(entity.getMachineCode(), item.getMachineCode())
                    && StringUtils.isEmpty(item.getPaddingCode()))) {
                return false;
            }
        }
        // 物料号非空时，存在同物料号且机台号为空的记录则冲突（保留原 checkUnique 判断条件，保证抽取前后行为一致）
        if (StringUtils.isNotEmpty(entity.getPaddingCode())) {
            if (existList.stream().anyMatch(item -> Objects.equal(entity.getPaddingCode(), item.getPaddingCode())
                    && StringUtils.isEmpty(item.getMachineCode()))) {
                return false;
            }
        }
        return true;
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
        // 统一填充当前工厂编码（导入模板不含工厂列，取自 sys.factory.code 配置）
        String factoryCode = factoryService.getFactoryCode();
        list.forEach(entity -> entity.setFactoryCode(factoryCode));
        int successNum = 0;
        int failureNum = 0;
        List<DjLossSetting> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        String uniqueMsg = I18nUtil.getMessage("ui.data.alert.djLossSetting.importUnique");

        // 循环外一次性加载当前工厂的机台主数据编码，用于导入机台存在性校验
        Set<String> machineCodeSet = this.loadMachineCodeSet(factoryCode);

        // 循环外一次性加载当前工厂的全部已有记录（含关键字段为空的记录，兼容部分匹配校验），避免在循环内逐笔查询数据库
        List<DjLossSetting> existLossSettingList = this.loadExistLossSettingList(factoryCode);
        Map<String, List<DjLossSetting>> existLossSettingMap = this.loadExistLossSettingMap(factoryCode);

        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            DjLossSetting docEntity = list.get(i);
            // 基础字段校验与重复校验
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, docEntity);
            ImportExcelValidatedUtils.validatedRepeat(list, docEntity, i, 2, importLogId, validated,
                    this.getCheckUniqueFields().toArray(new String[0]));
            // 机台存在性校验：导入的机台不在机台主数据中，该行视为导入失败
            if (StringUtils.isNotEmpty(docEntity.getMachineCode())
                    && !machineCodeSet.contains(docEntity.getMachineCode())) {
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, errorNum,
                        MessageFormat.format(I18nUtil.getMessage("ui.data.alert.djMachine.machineNotExist"),
                                docEntity.getMachineCode()),
                        validated);
            }
            if (CollectionUtils.isNotEmpty(validated)) {
                // 校验不通过，该行直接跳过，不再进行唯一性判断
                failureNum++;
                docEntity.setId(-999L);
                importErrorLogs.addAll(validated);
                continue;
            }

            if (this.isUnique(docEntity, existLossSettingList)) {
                importList.add(docEntity);
                successNum++;
            } else {
                if (updateSupport) {
                    logger.info("updateSupport:{}", docEntity);
                    List<DjLossSetting> existList = existLossSettingMap.get(this.buildLossSettingKey(docEntity));
                    if (CollectionUtils.isNotEmpty(existList) && existList.size() > 1) {
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

    /**
     * 一次性加载当前工厂的全部机台主数据编码，用于导入时校验机台是否存在
     *
     * @param factoryCode 工厂编码
     * @return 该工厂存在的机台编码集合
     */
    private Set<String> loadMachineCodeSet(String factoryCode) {
        if (StringUtils.isEmpty(factoryCode)) {
            return Collections.emptySet();
        }
        return machineInfoMapper
                .selectList(new LambdaQueryWrapper<DjMachineInfo>()
                        .eq(DjMachineInfo::getFactoryCode, factoryCode)
                        .select(DjMachineInfo::getMachineCode))
                .stream().map(DjMachineInfo::getMachineCode).filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
    }

    /**
     * 一次性加载当前工厂的全部已有损耗率记录（含关键字段为空的记录），
     * 供导入时在内存中判断唯一性，兼容机台码/物料号部分匹配的校验口径
     *
     * @param factoryCode 工厂编码
     * @return 当前工厂的全部已有损耗率记录
     */
    private List<DjLossSetting> loadExistLossSettingList(String factoryCode) {
        if (StringUtils.isEmpty(factoryCode)) {
            return Collections.emptyList();
        }
        return lossSettingMapper.selectList(new LambdaQueryWrapper<DjLossSetting>()
                .eq(DjLossSetting::getFactoryCode, factoryCode)
                .select(DjLossSetting::getId, DjLossSetting::getFactoryCode, DjLossSetting::getMachineCode,
                        DjLossSetting::getPaddingCode));
    }

    /**
     * 一次性加载当前工厂的全部已有损耗率记录，并按唯一键分组，避免导入时逐笔查询数据库
     *
     * @param factoryCode 工厂编码
     * @return 唯一键 -> 已有记录列表
     */
    private Map<String, List<DjLossSetting>> loadExistLossSettingMap(String factoryCode) {
        if (StringUtils.isEmpty(factoryCode)) {
            return Collections.emptyMap();
        }
        // 复用全量查询结果，按唯一键分组；排除关键字段为空的记录（与原逐笔 eq 查询口径一致，null 值不会被命中）
        List<DjLossSetting> existList = this.loadExistLossSettingList(factoryCode);
        return existList.stream()
                .filter(item -> StringUtils.isNotBlank(item.getFactoryCode())
                        && StringUtils.isNotBlank(item.getMachineCode())
                        && StringUtils.isNotBlank(item.getPaddingCode()))
                .collect(Collectors.groupingBy(this::buildLossSettingKey));
    }

    /**
     * 构建损耗率唯一键：工厂编码 + 机台编码 + 填充码，用于内存中快速匹配已有记录
     *
     * @param entity 损耗率记录
     * @return 唯一键
     */
    private String buildLossSettingKey(DjLossSetting entity) {
        return StringUtils.defaultString(entity.getFactoryCode()) + "|"
                + StringUtils.defaultString(entity.getMachineCode()) + "|"
                + StringUtils.defaultString(entity.getPaddingCode());
    }

    @Override
    protected String getDocTypeCode() {
        return "";
    }
}
