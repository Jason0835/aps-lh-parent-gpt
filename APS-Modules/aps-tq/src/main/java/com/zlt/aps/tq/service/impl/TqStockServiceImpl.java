package com.zlt.aps.tq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.tq.api.domain.entity.TqStock;
import com.zlt.aps.tq.mapper.TqStockMapper;
import com.zlt.aps.tq.service.ITqStockService;
import com.zlt.bill.common.service.AbstractDocService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

@Slf4j
@Service
public class TqStockServiceImpl extends AbstractDocService<TqStock> implements ITqStockService {

    @Resource
    private TqStockMapper tqStockMapper;

    @Override
    protected String getDocTypeCode() {
        return "TQ_STOCK";
    }

    @Override
    public String checkUnique(TqStock entity) {
        QueryWrapper<TqStock> wrapper = new QueryWrapper<>();
        wrapper.ne(entity.getId() != null, "ID", entity.getId());
        wrapper.eq("STOCK_DATE", entity.getStockDate());
        wrapper.eq("MATERIAL_CODE", entity.getMaterialCode());
        wrapper.eq("IS_DELETE", 0);
        if (tqStockMapper.selectCount(wrapper) > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 逻辑删除并批量保存胎圈库存（事务性操作）
     * 步骤1：逻辑删除指定库存日期的旧数据（IS_DELETE置为1）
     * 步骤2：批量插入MES最新库存数据（新记录，IS_DELETE=0）
     * 历史数据保留，只删当天库存日期的数据
     *
     * @param stockDate 库存日期
     * @param updateBy  更新者
     * @param list      待插入的胎圈库存列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void logicDeleteAndSaveBatch(Date stockDate, String updateBy, List<TqStock> list) {
        if (stockDate == null) {
            throw new IllegalArgumentException("库存日期不能为空");
        }
        // 步骤1：逻辑删除当天库存日期的旧数据
        Date updateTime = new Date();
        int deleteCount = tqStockMapper.logicDeleteByStockDate(stockDate, updateBy, updateTime);
        log.info("胎圈库存同步：逻辑删除库存日期={}的旧数据，删除数量={}", stockDate, deleteCount);

        // 步骤2：批量插入MES最新库存数据
        if (CollectionUtils.isNotEmpty(list)) {
            // 分批插入，每批1000条
            int batchSize = 1000;
            for (int i = 0; i < list.size(); i += batchSize) {
                int end = Math.min(i + batchSize, list.size());
                List<TqStock> batch = list.subList(i, end);
                for (TqStock stock : batch) {
                    baseDao.save(stock);
                }
            }
            log.info("胎圈库存同步：批量插入完成，插入数量={}", list.size());
        }
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("stockDate", "materialCode");
    }

    @Override
    public AjaxResult importData(List<TqStock> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<TqStock> importList = new ArrayList<>();

        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> (a.getStockDate() + a.getMaterialCode()), Collectors.counting()));

        for (int i = 0; i < list.size(); i++) {
            TqStock stock = list.get(i);

            Long hasValue = groupMap.get(stock.getStockDate() + stock.getMaterialCode());
            if (hasValue != null && hasValue > 1) {
                failureNum++;
                stock.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.data.column.stock.stockDate");
                String columnName2 = I18nUtil.getMessage("ui.data.column.tq.scheduleResult.beadCode");
                message = String.format(message, columnName + "+" + columnName2);
                addImportErrorLog(importLogId, i + 2, message, importErrorLogs);
                continue;
            }

            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, stock);
            if (CollectionUtils.isEmpty(validated)) {
                BigDecimal stockNum = stock.getStockNum() == null ? BigDecimal.ZERO : stock.getStockNum();
                BigDecimal modifyNum = stock.getModifyNum() == null ? BigDecimal.ZERO : stock.getModifyNum();
                BigDecimal badNum = stock.getBadNum() == null ? BigDecimal.ZERO : stock.getBadNum();
                BigDecimal dd = stockNum.add(modifyNum).subtract(badNum);
                if (dd.compareTo(BigDecimal.ZERO) < 0) {
                    failureNum++;
                    stock.setId(-999L);
                    addImportErrorLog(importLogId, i + 2,
                            I18nUtil.getMessage("ui.data.column.stock.stockNumValidate"), importErrorLogs);
                    continue;
                }
                importList.add(stock);
            } else {
                failureNum++;
                stock.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }
        try {
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                tqStockMapper.mergeSql(importList);
            } else {
                for (int i = 0; i < list.size(); i++) {
                    TqStock excelItem = list.get(i);
                    if (excelItem.getId() != null && excelItem.getId().equals(-999L)) {
                        continue;
                    }
                    List<TqStock> unic = tqStockMapper.checkStockListUnic(excelItem);
                    if (CollectionUtils.isEmpty(unic)) {
                        successNum++;
                        baseDao.save(excelItem);
                    } else {
                        failureNum++;
                        addImportErrorLog(importLogId, i + 2,
                                I18nUtil.getMessage("ui.stock.message.unique"), importErrorLogs);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            successNum = 0;
            failureNum = list.size();
            importErrorLogs.clear();
            addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
        }
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }
}
