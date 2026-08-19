package com.zlt.aps.dj.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
import com.zlt.aps.dj.api.domain.entity.DjCurlRoll;
import com.zlt.aps.dj.mapper.DjCurlRollMapper;
import com.zlt.aps.dj.service.DjCurlRollService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.common.utils.PubUtil;

/**
 * 垫胶卷曲信息维护Service业务层处理
 *
 * @author zlt
 * @date 2026-06-10
 */
@Service
public class DjCurlRollServiceImpl extends AbstractDocService<DjCurlRoll> implements DjCurlRollService {
    @Resource
    private FactoryService factoryService;
    
    @Resource
    private DjCurlRollMapper curlRollMapper;

    @Override
    public String checkUnique(DjCurlRoll entity) {
        if (StringUtils.isEmpty(entity.getPaddingCode())) {
            throw new RuntimeException(I18nUtil.getMessage("ui.error.message.curlRoll.paddingCodeNull"));
        }
        QueryWrapper<DjCurlRoll> queryWrapper = new QueryWrapper<>();
        queryWrapper.ne(PubUtil.isNotEmpty(entity.getFieldValueByFieldName("id")), "ID",
                entity.getFieldValueByFieldName("id"));
        queryWrapper.eq("FACTORY_CODE", entity.getFactoryCode());
        queryWrapper.eq("PADDING_CODE", entity.getPaddingCode());
        if (curlRollMapper.exists(queryWrapper)) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 唯一校验字段：工厂编码 + 填充物料号
     *
     * @return 唯一校验字段名列表
     */
    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("factoryCode", "paddingCode");
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
    public AjaxResult importData(List<DjCurlRoll> list, boolean updateSupport, Long importLogId) {
        // 统一填充当前工厂编码（导入模板不含工厂列，取自 sys.factory.code 配置）
        String factoryCode = factoryService.getFactoryCode();
        list.forEach(entity -> entity.setFactoryCode(factoryCode));
        int successNum = 0;
        int failureNum = 0;
        List<DjCurlRoll> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        String uniqueMsg = I18nUtil.getMessage("ui.data.alert.djCurlRoll.importUnique");

        // 循环外一次性加载当前工厂的全部已有记录，避免在循环内逐笔查询数据库
        Map<String, List<DjCurlRoll>> existCurlRollMap = this.loadExistCurlRollMap(factoryCode);

        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            DjCurlRoll docEntity = list.get(i);
            // 基础字段校验与重复校验
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, docEntity);
            ImportExcelValidatedUtils.validatedRepeat(list, docEntity, i, 2, importLogId, validated,
                    this.getCheckUniqueFields().toArray(new String[0]));
            if (CollectionUtils.isNotEmpty(validated)) {
                // 校验不通过，该行直接跳过，不再进行唯一性判断
                failureNum++;
                docEntity.setId(-999L);
                importErrorLogs.addAll(validated);
                continue;
            }

            if (checkUniqueByCache(docEntity, existCurlRollMap).equals(UserConstants.UNIQUE)) {
                importList.add(docEntity);
                successNum++;
            } else {
                if (updateSupport) {
                    logger.info("updateSupport:{}", docEntity);
                    List<DjCurlRoll> existList = existCurlRollMap.get(this.buildCurlRollKey(docEntity));
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

        for (DjCurlRoll entity : importList) {
            if (entity.getId() != null) {
                curlRollMapper.updateById(entity);
            } else {
                curlRollMapper.insert(entity);
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
     * 与 checkUnique 使用相同的唯一键口径（工厂编码 + 填充码），
     * 替代导入循环内逐笔调用 checkUnique 查询数据库，提升大数据量导入性能
     *
     * @param entity 待校验的记录
     * @param existCurlRollMap 预先加载的已有记录，按唯一键分组
     * @return 唯一返回 UserConstants.UNIQUE，否则返回 UserConstants.NOT_UNIQUE
     */
    private String checkUniqueByCache(DjCurlRoll entity, Map<String, List<DjCurlRoll>> existCurlRollMap) {
        List<DjCurlRoll> existList = existCurlRollMap.get(this.buildCurlRollKey(entity));
        if (CollectionUtils.isNotEmpty(existList)) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 一次性加载当前工厂的全部已有卷曲信息，并按唯一键分组，避免导入时逐笔查询数据库
     *
     * @param factoryCode 工厂编码
     * @return 唯一键 -> 已有记录列表
     */
    private Map<String, List<DjCurlRoll>> loadExistCurlRollMap(String factoryCode) {
        if (StringUtils.isEmpty(factoryCode)) {
            return Collections.emptyMap();
        }
        // 一次批量查询该工厂的全部记录，仅取唯一键匹配所需字段
        List<DjCurlRoll> existList = curlRollMapper.selectList(new LambdaQueryWrapper<DjCurlRoll>()
                .eq(DjCurlRoll::getFactoryCode, factoryCode)
                .select(DjCurlRoll::getId, DjCurlRoll::getFactoryCode, DjCurlRoll::getPaddingCode));
        // 按唯一键分组；排除关键字段为空的记录（与原逐笔 eq 查询口径一致，null 值不会被命中）
        return existList.stream()
                .filter(item -> StringUtils.isNotBlank(item.getFactoryCode())
                        && StringUtils.isNotBlank(item.getPaddingCode()))
                .collect(Collectors.groupingBy(this::buildCurlRollKey));
    }

    /**
     * 构建卷曲信息唯一键：工厂编码 + 填充码，用于内存中快速匹配已有记录
     *
     * @param entity 卷曲信息记录
     * @return 唯一键
     */
    private String buildCurlRollKey(DjCurlRoll entity) {
        return StringUtils.defaultString(entity.getFactoryCode()) + "|"
                + StringUtils.defaultString(entity.getPaddingCode());
    }

    @Override
    protected String getDocTypeCode() {
        return "";
    }
}