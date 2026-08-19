package com.zlt.aps.nc.service.impl;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.engine.service.FactoryService;
import com.zlt.aps.nc.api.domain.entity.NcMachineInfo;
import com.zlt.aps.nc.api.domain.entity.NcSpecifyMachine;
import com.zlt.aps.nc.mapper.NcMachineInfoMapper;
import com.zlt.aps.nc.mapper.NcSpecifyMachineMapper;
import com.zlt.aps.nc.service.NcSpecifyMachineService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.common.utils.PubUtil;

/**
 * <p>
 * 内衬定点机台表 服务实现类
 * </p>
 *
 * @author zlt
 * @since 2026-06-04
 */
@Service
public class NcSpecifyMachineServiceImpl extends AbstractDocService<NcSpecifyMachine>
        implements NcSpecifyMachineService {

    @Resource
    private FactoryService factoryService;

    @Resource
    private NcSpecifyMachineMapper specifyMachineMapper;

    @Resource
    private NcMachineInfoMapper machineInfoMapper;

    @Override
    public String checkUnique(NcSpecifyMachine entity) {
        QueryWrapper<NcSpecifyMachine> queryWrapper = new QueryWrapper<>();
        queryWrapper.ne(PubUtil.isNotEmpty(entity.getFieldValueByFieldName("id")), "ID",
                entity.getFieldValueByFieldName("id"));
        queryWrapper.eq("FACTORY_CODE", entity.getFactoryCode());
        queryWrapper.eq("MACHINE_CODE", entity.getMachineCode());
        queryWrapper.eq("LINING_CODE", entity.getLiningCode());

        if (specifyMachineMapper.selectCount(queryWrapper) > 0) {
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
    public AjaxResult importData(List<NcSpecifyMachine> list, boolean updateSupport, Long importLogId) {
        // 统一填充当前工厂编码（导入模板不含工厂列，取自 sys.factory.code 配置）
        String factoryCode = factoryService.getFactoryCode();
        list.forEach(entity -> entity.setFactoryCode(factoryCode));
        int successNum = 0;
        int failureNum = 0;
        List<NcSpecifyMachine> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        String uniqueMsg = I18nUtil.getMessage("ui.data.alert.ncSpecifyMachine.importUnique");

        // 循环外一次性加载当前工厂的机台主数据编码，用于导入机台存在性校验
        Set<String> machineCodeSet = this.loadMachineCodeSet(factoryCode);

        // 循环外一次性加载当前工厂的全部已有记录，避免在循环内逐笔查询数据库
        Map<String, List<NcSpecifyMachine>> existSpecifyMachineMap = this.loadExistSpecifyMachineMap(factoryCode);

        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            NcSpecifyMachine docEntity = list.get(i);
            // 基础字段校验与重复校验
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, docEntity);
            ImportExcelValidatedUtils.validatedRepeat(list, docEntity, i, 2, importLogId, validated,
                    this.getCheckUniqueFields().toArray(new String[0]));
            // 机台存在性校验：导入的机台不在机台主数据中，该行视为导入失败
            if (StringUtils.isNotEmpty(docEntity.getMachineCode())
                    && !machineCodeSet.contains(docEntity.getMachineCode())) {
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, errorNum,
                        MessageFormat.format(I18nUtil.getMessage("ui.data.alert.ncMachine.machineNotExist"),
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

            if (checkUniqueByCache(docEntity, existSpecifyMachineMap).equals(UserConstants.UNIQUE)) {
                importList.add(docEntity);
                successNum++;
            } else {
                if (updateSupport) {
                    logger.info("updateSupport:{}", docEntity);
                    List<NcSpecifyMachine> existList = existSpecifyMachineMap.get(this.buildSpecifyMachineKey(docEntity));
                    if (CollectionUtils.isNotEmpty(existList) && existList.size() > 1) {
                        failureNum++;
                        String multipleMsg = I18nUtil.getMessage("ui.data.alert.cxStock.multipleRecords");
                        ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                                errorNum, String.format(multipleMsg, errorNum), importErrorLogs);
                        continue;
                    } else if (CollectionUtils.isNotEmpty(existList)) {
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

        for (NcSpecifyMachine entity : importList) {
            if (entity.getId() != null) {
                specifyMachineMapper.updateById(entity);
            } else {
                specifyMachineMapper.insert(entity);
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
     * 基于内存中预先加载的已有记录判断唯一性，
     * 与 checkUnique 使用相同的唯一键口径（工厂编码 + 机台编码 + 内衬代码），
     * 替代导入循环内逐笔调用 checkUnique 查询数据库，提升大数据量导入性能
     *
     * @param entity 待校验的记录
     * @param existSpecifyMachineMap 预先加载的已有记录，按唯一键分组
     * @return 唯一返回 UserConstants.UNIQUE，否则返回 UserConstants.NOT_UNIQUE
     */
    private String checkUniqueByCache(NcSpecifyMachine entity,
            Map<String, List<NcSpecifyMachine>> existSpecifyMachineMap) {
        List<NcSpecifyMachine> existList = existSpecifyMachineMap.get(this.buildSpecifyMachineKey(entity));
        if (CollectionUtils.isNotEmpty(existList)) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
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
                .selectList(new LambdaQueryWrapper<NcMachineInfo>()
                        .eq(NcMachineInfo::getFactoryCode, factoryCode)
                        .select(NcMachineInfo::getMachineCode))
                .stream().map(NcMachineInfo::getMachineCode).filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
    }

    /**
     * 一次性加载当前工厂的全部已有定点机台记录，并按唯一键分组，避免导入时逐笔查询数据库
     *
     * @param factoryCode 工厂编码
     * @return 唯一键 -> 已有记录列表
     */
    private Map<String, List<NcSpecifyMachine>> loadExistSpecifyMachineMap(String factoryCode) {
        if (StringUtils.isEmpty(factoryCode)) {
            return Collections.emptyMap();
        }
        // 一次批量查询该工厂的全部记录，仅取唯一键匹配所需字段
        List<NcSpecifyMachine> existList = specifyMachineMapper.selectList(new LambdaQueryWrapper<NcSpecifyMachine>()
                .eq(NcSpecifyMachine::getFactoryCode, factoryCode)
                .select(NcSpecifyMachine::getId, NcSpecifyMachine::getFactoryCode, NcSpecifyMachine::getMachineCode,
                        NcSpecifyMachine::getLiningCode));
        // 按唯一键分组；排除关键字段为空的记录（与原逐笔 eq 查询口径一致，null 值不会被命中）
        return existList.stream()
                .filter(item -> StringUtils.isNotBlank(item.getFactoryCode())
                        && StringUtils.isNotBlank(item.getMachineCode())
                        && StringUtils.isNotBlank(item.getLiningCode()))
                .collect(Collectors.groupingBy(this::buildSpecifyMachineKey));
    }

    /**
     * 构建定点机台唯一键：工厂编码 + 机台编码 + 内衬代码，用于内存中快速匹配已有记录
     *
     * @param entity 定点机台记录
     * @return 唯一键
     */
    private String buildSpecifyMachineKey(NcSpecifyMachine entity) {
        return StringUtils.defaultString(entity.getFactoryCode()) + "|"
                + StringUtils.defaultString(entity.getMachineCode()) + "|"
                + StringUtils.defaultString(entity.getLiningCode());
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("factoryCode", "machineCode", "liningCode");
    }

    @Override
    protected String getDocTypeCode() {
        return "";
    }
}
