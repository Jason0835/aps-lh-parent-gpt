package com.zlt.aps.cx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cx.api.domain.entity.CxStock;
import com.zlt.aps.cx.mapper.CxStockMapper;
import com.zlt.aps.cx.service.CxStockService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.common.utils.PubUtil;
import jodd.util.StringUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 库存Service实现类
 *
 * @author APS Team
 */
@Service
public class CxStockServiceImpl extends AbstractDocService<CxStock> implements CxStockService {

    @Autowired
    CxStockMapper cxStockMapper;

    @Override
    public CxStock getByMaterialCode(String materialCode) {
        LambdaQueryWrapper<CxStock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CxStock::getEmbryoCode, materialCode);
        return cxStockMapper.selectOne(wrapper);
    }

    @Override
    public List<CxStock> listLowStock() {
        LambdaQueryWrapper<CxStock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CxStock::getAlertStatus, "LOW");
        return cxStockMapper.selectList(wrapper);
    }

    @Override
    public List<CxStock> listHighStock() {
        LambdaQueryWrapper<CxStock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CxStock::getAlertStatus, "HIGH");
        return cxStockMapper.selectList(wrapper);
    }

    @Override
    public List<CxStock> listEndingStock() {
        LambdaQueryWrapper<CxStock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CxStock::getIsEndingSku, "1");
        return cxStockMapper.selectList(wrapper);
    }

    @Override
    public BigDecimal calculateStockHours(String materialCode) {
        CxStock stock = getByMaterialCode(materialCode);
        if (stock == null || stock.getStockNum() == null || stock.getStockNum() <= 0) {
            return BigDecimal.ZERO;
        }
        // 使用有效库存计算可供时长
        Integer effectiveStock = stock.getEffectiveStock();
        if (effectiveStock <= 0) {
            return BigDecimal.ZERO;
        }
        // 简化计算：库存 / (硫化机台数 * 模数 * 每小时产能)
        // 实际应根据硫化时间和模数计算
        return new BigDecimal(effectiveStock).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshAllAlertStatus() {
        List<CxStock> stocks = cxStockMapper.selectList(null);
        Date now = new Date();

        for (CxStock stock : stocks) {
            String alertStatus = calculateAlertStatus(stock);

            if (!alertStatus.equals(stock.getAlertStatus())) {
                stock.setAlertStatus(alertStatus);
                stock.setAlertTime(now);
                stock.setUpdateTime(now);
                cxStockMapper.updateById(stock);
            }
        }
    }

    @Override
    public Page<CxStock> pageList(Page<CxStock> page, String alertStatus) {
        LambdaQueryWrapper<CxStock> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(alertStatus)) {
            wrapper.eq(CxStock::getAlertStatus, alertStatus);
        }

        wrapper.orderByDesc(CxStock::getAlertTime)
                .orderByAsc(CxStock::getEmbryoCode);

        return cxStockMapper.selectPage(page, wrapper);
    }

    /**
     * 计算预警状态
     */
    private String calculateAlertStatus(CxStock stock) {
        if (stock == null || stock.getStockNum() == null) {
            return "NORMAL";
        }

        // 根据库存可供硫化时长判断
        BigDecimal stockHours = stock.getStockHours();
        if (stockHours == null) {
            stockHours = calculateStockHours(stock.getEmbryoCode());
        }

        // 预警阈值（可配置）
        BigDecimal lowThreshold = new BigDecimal("4");   // 低于4小时预警
        BigDecimal highThreshold = new BigDecimal("48"); // 高于48小时预警

        if (stockHours.compareTo(lowThreshold) < 0) {
            return "LOW";
        } else if (stockHours.compareTo(highThreshold) > 0) {
            return "HIGH";
        }

        return "NORMAL";
    }


    @Override
    public AjaxResult importData(List<CxStock> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<CxStock> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        String uniqueMsg = I18nUtil.getMessage("import.validated.unique");

        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            CxStock docEntity = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, docEntity);
            ImportExcelValidatedUtils.validatedRepeat(list, docEntity, i, 2, importLogId, validated);
            if (CollectionUtils.isNotEmpty(validated)) {
                failureNum++;
                docEntity.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }

        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            CxStock docEntity = list.get(i);
            if (docEntity.getId() != null && docEntity.getId() == -999L) {
                continue;
            }

            if (StringUtil.isBlank(docEntity.getEmbryoCode())) {
                failureNum++;
                String message = I18nUtil.getMessage("ui.data.alert.cxStock.embryoCodeRequired");
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                        errorNum, String.format(message, errorNum), importErrorLogs);
                continue;
            }

            if (docEntity.getStockDate() == null) {
                failureNum++;
                String message = I18nUtil.getMessage("ui.data.alert.cxStock.stockDateRequired");
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                        errorNum, String.format(message, errorNum), importErrorLogs);
                continue;
            }

            if (checkUnique(docEntity).equals(UserConstants.UNIQUE)) {
                importList.add(docEntity);
                successNum++;
            } else {
                if (updateSupport) {
                    QueryWrapper<CxStock> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("FACTORY_CODE", docEntity.getFactoryCode());
                    queryWrapper.eq("STOCK_DATE", docEntity.getStockDate());
                    queryWrapper.eq("EMBRYO_CODE", docEntity.getEmbryoCode());
                    CxStock existEntity = cxStockMapper.selectOne(queryWrapper);
                    if (existEntity != null) {
                        docEntity.setId(existEntity.getId());
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
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        }

        for (CxStock entity : importList) {
            if (entity.getId() != null) {
                cxStockMapper.updateById(entity);
            } else {
                cxStockMapper.insert(entity);
            }
        }

        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

    @Override
    public String checkUnique(CxStock entity) {
        QueryWrapper<CxStock> queryWrapper = new QueryWrapper<>();
        queryWrapper.ne(PubUtil.isNotEmpty(entity.getFieldValueByFieldName("id")), "ID", entity.getFieldValueByFieldName("id"));
        queryWrapper.eq("FACTORY_CODE", entity.getFactoryCode());
        queryWrapper.eq("STOCK_DATE", entity.getStockDate());
        queryWrapper.eq("EMBRYO_CODE", entity.getEmbryoCode());

        if (cxStockMapper.selectCount(queryWrapper) > 0) {
            return UserConstants.NOT_UNIQUE;
        } else {
            return UserConstants.UNIQUE;
        }
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("factoryCode", "stockDate", "embryoCode");
    }

    @Override
    protected String getDocTypeCode() {
        return "";
    }
}
