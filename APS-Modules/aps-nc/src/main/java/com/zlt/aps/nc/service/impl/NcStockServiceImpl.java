package com.zlt.aps.nc.service.impl;

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
import com.zlt.aps.nc.api.domain.entity.NcStock;
import com.zlt.aps.nc.mapper.NcStockMapper;
import com.zlt.aps.nc.service.NcStockService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.common.utils.PubUtil;

/**
 * 内衬库存信息Service业务层处理
 *
 * @author zlt
 * @date 2026-06-31
 */
@Service
public class NcStockServiceImpl extends AbstractDocService<NcStock> implements NcStockService {
    @Autowired
    private FactoryService factoryService;

    @Autowired
    private NcStockMapper stockMapper;

    @Override
    public String checkUnique(NcStock entity) {
        QueryWrapper<NcStock> queryWrapper = new QueryWrapper<>();
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
    public AjaxResult importData(List<NcStock> list, boolean updateSupport, Long importLogId) {
        // 统一填充当前工厂编码（导入模板不含工厂列，取自 sys.factory.code 配置）
        String factoryCode = factoryService.getFactoryCode();
        list.forEach(entity -> entity.setFactoryCode(factoryCode));
        int successNum = 0;
        int failureNum = 0;
        List<NcStock> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        String uniqueMsg = I18nUtil.getMessage("ui.data.alert.ncStock.importUnique");

        // 循环外一次性加载导入数据涉及日期的全部已有库存，避免在循环内逐笔查询数据库
        Map<String, List<NcStock>> existStockMap = this.loadExistStockMap(factoryCode, list);

        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            NcStock docEntity = list.get(i);
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

            if (checkUniqueByCache(docEntity, existStockMap).equals(UserConstants.UNIQUE)) {
                importList.add(docEntity);
                successNum++;
            } else {
                if (updateSupport) {
                    logger.info("updateSupport:{}", docEntity);
                    List<NcStock> existList = existStockMap.get(this.buildStockKey(docEntity));
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
        baseDao.saveBatch(importList);

        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum,
                    importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

    /**
     * 基于内存中预先加载的已有库存数据判断唯一性，
     * 与 checkUnique 使用相同的唯一键口径（工厂编码 + 库存日期 + 物料编码），
     * 替代导入循环内逐笔调用 checkUnique 查询数据库，提升大数据量导入性能
     *
     * @param entity 待校验的库存记录
     * @param existStockMap 预先加载的已有库存数据，按唯一键分组
     * @return 唯一返回 UserConstants.UNIQUE，否则返回 UserConstants.NOT_UNIQUE
     */
    private String checkUniqueByCache(NcStock entity, Map<String, List<NcStock>> existStockMap) {
        List<NcStock> existList = existStockMap.get(this.buildStockKey(entity));
        if (CollectionUtils.isNotEmpty(existList)) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 一次性加载导入数据涉及日期的全部已有库存，并按唯一键分组，
     * 供导入时在内存中匹配已有记录，避免逐笔查询数据库
     *
     * @param factoryCode 工厂编码
     * @param list 导入数据
     * @return 唯一键 -> 已有库存列表
     */
    private Map<String, List<NcStock>> loadExistStockMap(String factoryCode, List<NcStock> list) {
        // 收集导入数据中的所有库存日期并去重
        Set<Date> stockDates = list.stream()
                .map(NcStock::getStockDate)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(stockDates)) {
            return Collections.emptyMap();
        }
        // 一次批量查询这些日期的全部库存，仅取唯一键匹配所需字段
        List<NcStock> existList = stockMapper.selectList(new LambdaQueryWrapper<NcStock>()
                .eq(NcStock::getFactoryCode, factoryCode)
                .in(NcStock::getStockDate, stockDates)
                .select(NcStock::getId, NcStock::getFactoryCode, NcStock::getStockDate,
                        NcStock::getMaterialCode));
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
    private String buildStockKey(NcStock stock) {
        String stockDate = stock.getStockDate() == null ? "" : String.valueOf(stock.getStockDate().getTime());
        return StringUtils.defaultString(stock.getFactoryCode()) + "|" + stockDate + "|"
                + StringUtils.defaultString(stock.getMaterialCode());
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
