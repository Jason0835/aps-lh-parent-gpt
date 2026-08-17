package com.zlt.aps.cd15.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.RowStateEnum;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cd15.api.domain.entity.Cd15Stock;
import com.zlt.aps.cd15.mapper.Cd15StockMapper;
import com.zlt.aps.cd15.service.ICd15StockService;
import com.zlt.aps.maindata.service.IMdmConstructionInfoService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.common.utils.PubUtil;
import com.zlt.sysdef.domain.SysDocType;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 斜裁库存管理 Service 实现。
 */
@Service
@Slf4j
@Transactional(rollbackFor = Exception.class)
public class Cd15StockServiceImpl extends AbstractDocService<Cd15Stock> implements ICd15StockService {

    @Resource
    private Cd15StockMapper cd15StockMapper;

    @Resource
    private IMdmConstructionInfoService mdmConstructionInfoService;

    @Override
    protected String getDocTypeCode() {
        return "CD15_STOCK";
    }

    @Override
    public String checkUnique(Cd15Stock entity) {
        LambdaQueryWrapper<Cd15Stock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cd15Stock::getFactoryCode, entity.getFactoryCode());
        wrapper.eq(Cd15Stock::getStockDate, entity.getStockDate());
        wrapper.eq(Cd15Stock::getShiftCode, entity.getShiftCode());
        wrapper.eq(Cd15Stock::getMaterialCode, entity.getMaterialCode());
        wrapper.ne(entity.getId() != null, Cd15Stock::getId, entity.getId());
        return cd15StockMapper.selectCount(wrapper) > 0 ? UserConstants.NOT_UNIQUE : UserConstants.UNIQUE;
    }

    @Override
    public String validateBusiness(Cd15Stock entity) {
        if (entity != null && StringUtils.isNotBlank(entity.getMaterialCode())
                && !this.isSteelStripCodeExists(entity.getMaterialCode())) {
            return "ui.data.column.cd15Stock.materialCodeInvalid";
        }
        return null;
    }

    @Override
    public AjaxResult importData(List<Cd15Stock> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<Cd15Stock> insertList = new ArrayList<>();
        List<ImportErrorLog> errorList = new ArrayList<>();
        Set<String> steelStripCodes = this.loadSteelStripCodes();
        String uniqueMessage = I18nUtil.getMessage("import.validated.unique");

        for (int index = 0; index < list.size(); index++) {
            int rowNum = index + 2;
            Cd15Stock importEntity = list.get(index);
            List<ImportErrorLog> validateList = ImportExcelValidatedUtils.validated(importLogId, rowNum, importEntity);
            ImportExcelValidatedUtils.validatedRepeat(list, importEntity, index, 2, importLogId, validateList,
                    this.getCheckUniqueFields().toArray(new String[0]));
            if (StringUtils.isNotBlank(importEntity.getMaterialCode()) && !steelStripCodes.contains(importEntity.getMaterialCode())) {
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                        rowNum, I18nUtil.getMessage("ui.data.column.cd15Stock.materialCodeInvalid"), validateList);
            }
            if (CollectionUtils.isNotEmpty(validateList)) {
                failureNum++;
                importEntity.setId(-999L);
                errorList.addAll(validateList);
            }
        }

        for (int index = 0; index < list.size(); index++) {
            int rowNum = index + 2;
            Cd15Stock importEntity = list.get(index);
            if (importEntity.getId() != null && importEntity.getId() == -999L) {
                continue;
            }
            Cd15Stock existEntity = this.getExist(importEntity);
            if (existEntity == null) {
                importEntity.setRowState(RowStateEnum.ADDED);
                insertList.add(importEntity);
            } else if (updateSupport) {
                this.copyImportValues(existEntity, importEntity);
                cd15StockMapper.updateById(existEntity);
                successNum++;
            } else {
                failureNum++;
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, rowNum,
                        MessageFormat.format(uniqueMessage, rowNum), errorList);
            }
        }

        if (PubUtil.isEmpty(insertList) && successNum == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, errorList);
        }
        if (CollectionUtils.isNotEmpty(insertList)) {
            successNum += baseDao.saveBatch(insertList);
        }
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, errorList);
        }
        return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
    }

    @Override
    public void logicDeleteAndSaveBatch(String factoryCode, Date stockDate, String shiftCode,
                                        String updateBy, List<Cd15Stock> stockList) {
        Date normalizedStockDate = DateUtil.beginOfDay(stockDate);
        List<Cd15Stock> normalizedList = stockList == null
                ? new ArrayList<>() : new ArrayList<>(stockList);
        normalizedList.sort(Comparator.comparing(Cd15Stock::getMaterialCode));
        List<Cd15Stock> existingList = cd15StockMapper.selectList(
                new LambdaQueryWrapper<Cd15Stock>()
                        .eq(Cd15Stock::getFactoryCode, factoryCode)
                        .eq(Cd15Stock::getStockDate, normalizedStockDate)
                        .eq(Cd15Stock::getShiftCode, shiftCode)
                        .orderByAsc(Cd15Stock::getMaterialCode));
        if (isSameMesSnapshot(existingList, normalizedList)) {
            log.info("斜裁MES库存快照未变化，跳过替换：factoryCode={}，stockDate={}，shiftCode={}，数量={}",
                    factoryCode, DateUtil.formatDate(normalizedStockDate), shiftCode, normalizedList.size());
            return;
        }
        Date now = new Date();
        cd15StockMapper.logicDeleteByScope(factoryCode, normalizedStockDate, shiftCode, updateBy, now);
        normalizedList.forEach(stock -> {
            stock.setFactoryCode(factoryCode);
            stock.setStockDate(normalizedStockDate);
            stock.setShiftCode(shiftCode);
            stock.setCreateBy(updateBy);
            stock.setUpdateBy(updateBy);
            stock.setCreateTime(now);
            stock.setUpdateTime(now);
            stock.setIsDelete(0);
        });
        if (CollectionUtils.isNotEmpty(normalizedList)) {
            baseDao.saveBatch(normalizedList);
        }
    }

    private boolean isSameMesSnapshot(List<Cd15Stock> existingList, List<Cd15Stock> incomingList) {
        if (existingList == null || existingList.size() != incomingList.size()) {
            return false;
        }
        for (int index = 0; index < existingList.size(); index++) {
            Cd15Stock existing = existingList.get(index);
            Cd15Stock incoming = incomingList.get(index);
            if (!Objects.equals(existing.getMaterialCode(), incoming.getMaterialCode())
                    || !Objects.equals(existing.getStockNum(), incoming.getStockNum())
                    || !Objects.equals(existing.getModifyNum(), incoming.getModifyNum())
                    || !Objects.equals(existing.getBadNum(), incoming.getBadNum())
                    || !Objects.equals(existing.getRollStockNum(), incoming.getRollStockNum())
                    || !Objects.equals(existing.getRollModifyNum(), incoming.getRollModifyNum())
                    || !Objects.equals(existing.getRollBadNum(), incoming.getRollBadNum())) {
                return false;
            }
        }
        return true;
    }

    /**
     * 查询同工厂、同库存日期、同班次、同物料编号的已有库存。
     *
     * @param entity 导入数据
     * @return 已有库存
     */
    private Cd15Stock getExist(Cd15Stock entity) {
        LambdaQueryWrapper<Cd15Stock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cd15Stock::getFactoryCode, entity.getFactoryCode());
        wrapper.eq(Cd15Stock::getStockDate, entity.getStockDate());
        wrapper.eq(Cd15Stock::getShiftCode, entity.getShiftCode());
        wrapper.eq(Cd15Stock::getMaterialCode, entity.getMaterialCode());
        return cd15StockMapper.selectOne(wrapper);
    }

    /**
     * 导入更新时只覆盖页面维护字段，保留主键和审计信息。
     *
     * @param target 已有库存
     * @param source 导入库存
     */
    private void copyImportValues(Cd15Stock target, Cd15Stock source) {
        target.setStockNum(source.getStockNum());
        target.setModifyNum(source.getModifyNum());
        target.setBadNum(source.getBadNum());
        target.setRollStockNum(source.getRollStockNum());
        target.setRollModifyNum(source.getRollModifyNum());
        target.setRollBadNum(source.getRollBadNum());
        target.setRemark(source.getRemark());
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("CD15_STOCK");
        return sysDocType;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("factoryCode", "stockDate", "shiftCode", "materialCode");
    }

    private boolean isSteelStripCodeExists(String materialCode) {
        if (StringUtils.isBlank(materialCode)) {
            return true;
        }
        return this.loadSteelStripCodes().contains(materialCode);
    }

    private Set<String> loadSteelStripCodes() {
        List<String> steelStripCodeList = mdmConstructionInfoService.listSteelStripCodes();
        return CollectionUtils.isEmpty(steelStripCodeList) ? new HashSet<>() : new HashSet<>(steelStripCodeList);
    }
}
