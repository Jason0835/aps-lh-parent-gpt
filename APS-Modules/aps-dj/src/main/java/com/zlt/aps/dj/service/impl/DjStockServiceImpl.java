package com.zlt.aps.dj.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.engine.service.FactoryService;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.dj.api.domain.entity.DjStock;
import com.zlt.aps.dj.engine.mapper.DjEngineConstructionInfoMapper;
import com.zlt.aps.dj.mapper.DjStockMapper;
import com.zlt.aps.dj.service.DjStockService;
import com.zlt.aps.mdm.api.domain.entity.MdmConstructionInfo;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.common.utils.PubUtil;

/**
 * 垫胶库存信息Service业务层处理
 *
 * @author zlt
 * @date 2026-05-31
 */
@Service
public class DjStockServiceImpl extends AbstractDocService<DjStock> implements DjStockService {
    @Autowired
    private FactoryService factoryService;

    @Autowired
    private DjStockMapper stockMapper;

    @Autowired
    private DjEngineConstructionInfoMapper djEngineConstructionInfoMapper;

    @Override
    public String checkUnique(DjStock entity) {
        QueryWrapper<DjStock> queryWrapper = new QueryWrapper<>();
        queryWrapper.ne(PubUtil.isNotEmpty(entity.getFieldValueByFieldName("id")), "ID",
                entity.getFieldValueByFieldName("id"));
        queryWrapper.eq("FACTORY_CODE", entity.getFactoryCode());
        queryWrapper.eq("STOCK_DATE", entity.getStockDate());
        queryWrapper.eq("MATERIAL_CODE", entity.getMaterialCode());

        if (stockMapper.selectCount(queryWrapper) > 0) {
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
    public AjaxResult importData(List<DjStock> list, boolean updateSupport, Long importLogId) {
        // 统一填充当前工厂编码（导入模板不含工厂列，取自 sys.factory.code 配置）
        String factoryCode = factoryService.getFactoryCode();
        list.forEach(entity -> entity.setFactoryCode(factoryCode));
        int successNum = 0;
        int failureNum = 0;
        List<DjStock> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        String uniqueMsg = I18nUtil.getMessage("ui.data.alert.cxStock.embryoCodeNotUnique");

        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            DjStock docEntity = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, docEntity);
            ImportExcelValidatedUtils.validatedRepeat(list, docEntity, i, 2, importLogId, validated,
                    this.getCheckUniqueFields().toArray(new String[0]));
            if (CollectionUtils.isNotEmpty(validated)) {
                failureNum++;
                docEntity.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }

        // 循环外一次性加载导入数据涉及日期的全部已有库存，避免在循环内逐笔查询数据库
        Map<String, List<DjStock>> existStockMap = this.loadExistStockMap(factoryCode, list);

        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            DjStock docEntity = list.get(i);
            if (docEntity.getId() != null && docEntity.getId() == -999L) {
                continue;
            }

            if (checkUnique(docEntity).equals(UserConstants.UNIQUE)) {
                importList.add(docEntity);
                successNum++;
            } else {
                if (updateSupport) {
                    logger.info("updateSupport:{}", docEntity);
                    List<DjStock> existList = existStockMap.get(this.buildStockKey(docEntity));
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
        // 通过物料编码批量关联垫胶主数据，补全物料名称（避免逐条查询，兼容大数据量导入）
        Map<String, String> paddingNameMap = this.loadPaddingNameMap(importList);
        for (DjStock entity : importList) {
            entity.setDataSource(ApsConstant.DATA_SOURCE_SYSTEM);
            String paddingName = paddingNameMap.get(entity.getMaterialCode());
            if (StringUtils.isNotBlank(paddingName)) {
                entity.setMaterialName(paddingName);
            }
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
     * 一次性加载导入数据涉及日期的全部已有库存，并按唯一键分组，
     * 供导入时在内存中匹配已有记录，避免逐笔查询数据库
     *
     * @param factoryCode 工厂编码
     * @param list 导入数据
     * @return 唯一键 -> 已有库存列表
     */
    private Map<String, List<DjStock>> loadExistStockMap(String factoryCode, List<DjStock> list) {
        // 收集导入数据中的所有库存日期并去重
        Set<Date> stockDates = list.stream()
                .map(DjStock::getStockDate)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(stockDates)) {
            return Collections.emptyMap();
        }
        // 一次批量查询这些日期的全部库存，仅取唯一键匹配所需字段
        List<DjStock> existList = stockMapper.selectList(new LambdaQueryWrapper<DjStock>()
                .eq(DjStock::getFactoryCode, factoryCode)
                .in(DjStock::getStockDate, stockDates)
                .select(DjStock::getId, DjStock::getFactoryCode, DjStock::getStockDate,
                        DjStock::getMaterialCode));
        // 按唯一键分组；排除关键字段为空的记录（与原逐笔 eq 查询口径一致，null 值不会被命中）
        return existList.stream()
                .filter(item -> StringUtils.isNotBlank(item.getFactoryCode())
                        && item.getStockDate() != null
                        && StringUtils.isNotBlank(item.getMaterialCode()))
                .collect(Collectors.groupingBy(this::buildStockKey));
    }

    /**
     * 构建库存唯一键：工厂编码 + 库存日期 + 物料编码，用于内存中快速匹配已有库存
     *
     * @param stock 库存记录
     * @return 唯一键
     */
    private String buildStockKey(DjStock stock) {
        String stockDate = stock.getStockDate() == null ? "" : String.valueOf(stock.getStockDate().getTime());
        return StringUtils.defaultString(stock.getFactoryCode()) + "|" + stockDate + "|"
                + StringUtils.defaultString(stock.getMaterialCode());
    }

    /**
     * 批量查询垫胶主数据，构建 垫胶编码 -> 垫胶名称 映射，用于导入时补全库存物料名称。
     * 采用一次 IN 查询避免逐条查库，提升大数据量导入性能。
     *
     * @param stockList 库存导入数据
     * @return 垫胶编码与垫胶名称的映射
     */
    private Map<String, String> loadPaddingNameMap(List<DjStock> stockList) {
        // 收集待导入记录中的所有垫胶编码并去重
        Set<String> paddingCodes = stockList.stream()
                .map(DjStock::getMaterialCode)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(paddingCodes)) {
            return Collections.emptyMap();
        }
        // 一次批量查询垫胶主数据，仅取编码与名称两列
        List<MdmConstructionInfo> constructionList = djEngineConstructionInfoMapper.selectList(
                new LambdaQueryWrapper<MdmConstructionInfo>()
                        .in(MdmConstructionInfo::getPaddingCode, paddingCodes)
                        .select(MdmConstructionInfo::getPaddingCode, MdmConstructionInfo::getPaddingName));
        // 构建映射，同名编码重复时取第一条
        return constructionList.stream()
                .filter(item -> StringUtils.isNotBlank(item.getPaddingName()))
                .collect(Collectors.toMap(MdmConstructionInfo::getPaddingCode, MdmConstructionInfo::getPaddingName,
                        (firstName, secondName) -> firstName));
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("factoryCode", "stockDate", "materialCode");
    }

    @Override
    protected String getDocTypeCode() {
        return "";
    }
}
