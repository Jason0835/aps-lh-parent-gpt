package com.zlt.aps.tm.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.tm.api.domain.entity.TmStock;
import com.zlt.aps.tm.mapper.TmStockMapper;
import com.zlt.aps.tm.service.ITmStockService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;

@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class TmStockServiceImpl extends AbstractDocService<TmStock> implements ITmStockService {

    @Resource
    private TmStockMapper tmStockMapper;

    @Override
    protected String getDocTypeCode() {
        return "TM0812";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("TM0812");
        return sysDocType;
    }

    @Override
    public String checkUnique(TmStock query) {
        String unique = super.checkUnique(query);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.stock.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return new ArrayList<>(Arrays.asList("factoryCode", "stockDate", "treadCode"));
    }

    /**
     * 逻辑删除并批量保存胎面库存（事务性操作）
     * 步骤1：逻辑删除当天库存日期的旧数据（IS_DELETE置为1）
     * 步骤2：批量插入MES最新库存数据（新记录，IS_DELETE=0）
     * 历史数据保留，只删当天库存日期的数据
     *
     * @param stockDate 库存日期
     * @param updateBy  更新者
     * @param list      待插入的胎面库存列表
     */
    @Override
    public void logicDeleteAndSaveBatch(Date stockDate, String updateBy, List<TmStock> list) {
        if (stockDate == null) {
            throw new IllegalArgumentException("库存日期不能为空");
        }
        // 步骤1：逻辑删除当天库存日期的旧数据
        Date updateTime = new Date();
        int deleteCount = tmStockMapper.logicDeleteByStockDate(stockDate, updateBy, updateTime);
        log.info("胎面库存同步：逻辑删除库存日期={}的旧数据，删除数量={}", stockDate, deleteCount);

        // 步骤2：批量插入MES最新库存数据
        if (CollectionUtils.isNotEmpty(list)) {
            // 分批插入，每批1000条
            int batchSize = 1000;
            for (int i = 0; i < list.size(); i += batchSize) {
                int end = Math.min(i + batchSize, list.size());
                List<TmStock> batch = list.subList(i, end);
                for (TmStock stock : batch) {
                    baseDao.save(stock);
                }
            }
            log.info("胎面库存同步：批量插入完成，插入数量={}", list.size());
        }
    }

    /**
     * 替换指定工厂和库存日期的胎面库存快照。
     *
     * @param factoryCode 工厂编码
     * @param stockDate 库存日期
     * @param updateBy 更新人
     * @param stockList MES库存列表，空集合表示清空快照
     * @throws ServiceException 工厂或库存日期为空时抛出
     */
    @Override
    public void replaceStock(String factoryCode, Date stockDate, String updateBy, List<TmStock> stockList) {
        if (StrUtil.isBlank(factoryCode) || stockDate == null) {
            throw new ServiceException(I18nUtil.getMessage("ui.itf.mes.stockArgumentsInvalid"));
        }
        String normalizedFactoryCode = StrUtil.trim(factoryCode);
        Date normalizedStockDate = DateUtil.beginOfDay(stockDate);
        String operator = StrUtil.blankToDefault(StrUtil.trim(updateBy), "MES");
        Date now = new Date();
        int deleteCount = this.tmStockMapper.update(null, new LambdaUpdateWrapper<TmStock>()
                .eq(TmStock::getFactoryCode, normalizedFactoryCode)
                .eq(TmStock::getStockDate, normalizedStockDate)
                .set(TmStock::getIsDelete, 1)
                .set(TmStock::getUpdateBy, operator)
                .set(TmStock::getUpdateTime, now));
        List<TmStock> normalizedList = stockList == null ? Collections.emptyList() : stockList;
        normalizedList.forEach(stock -> {
            stock.setFactoryCode(normalizedFactoryCode);
            stock.setStockDate(normalizedStockDate);
            stock.setCreateBy(operator);
            stock.setUpdateBy(operator);
            stock.setCreateTime(now);
            stock.setUpdateTime(now);
            stock.setIsDelete(0);
        });
        if (CollectionUtils.isNotEmpty(normalizedList)) {
            this.baseDao.saveBatch(normalizedList);
        }
        log.info("胎面库存快照替换完成，factoryCode={}，stockDate={}，失效数量={}，新增数量={}",
                normalizedFactoryCode, DateUtil.formatDate(normalizedStockDate), deleteCount, normalizedList.size());
    }
}
