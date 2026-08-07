package com.zlt.aps.cd90.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cd90.api.domain.entity.Cd90ShiftStock;
import com.zlt.aps.cd90.mapper.Cd90ShiftStockMapper;
import com.zlt.aps.cd90.service.ICd90ShiftStockService;
import com.zlt.core.dao.basedao.BaseDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 直裁自动滚动班次库存写入服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class Cd90ShiftStockServiceImpl implements ICd90ShiftStockService {

    private final BaseDao baseDao;
    private final Cd90ShiftStockMapper shiftStockMapper;

    @Override
    public void replaceShiftStock(String factoryCode, Date stockDate, String shiftCode,
                                  Date shiftStartTime, String updateBy,
                                  List<Cd90ShiftStock> stockList) {
        if (StringUtils.isBlank(factoryCode) || stockDate == null || StringUtils.isBlank(shiftCode)
                || shiftStartTime == null) {
            throw new ServiceException(I18nUtil.getMessage("ui.cd90.shiftStock.syncArgumentsInvalid"));
        }
        Date normalizedStockDate = DateUtil.beginOfDay(stockDate);
        String normalizedFactoryCode = factoryCode.trim();
        String normalizedShiftCode = shiftCode.trim();
        String operator = StringUtils.defaultIfBlank(StringUtils.trim(updateBy), "MES");
        List<Cd90ShiftStock> normalizedList = stockList == null
                ? new ArrayList<>() : new ArrayList<>(stockList);
        this.validateSource(normalizedList);
        normalizedList.sort(Comparator.comparing(Cd90ShiftStock::getMaterialCode));

        List<Cd90ShiftStock> existingList = this.shiftStockMapper.selectList(
                new LambdaQueryWrapper<Cd90ShiftStock>()
                        .eq(Cd90ShiftStock::getFactoryCode, normalizedFactoryCode)
                        .eq(Cd90ShiftStock::getShiftCode, normalizedShiftCode)
                        .eq(Cd90ShiftStock::getShiftStartTime, shiftStartTime)
                        .orderByAsc(Cd90ShiftStock::getMaterialCode));
        if (this.isSameSnapshot(existingList, normalizedList)) {
            log.info("直裁班次库存快照未变化，跳过替换：factoryCode={}，shiftCode={}，shiftStartTime={}，数量={}",
                    normalizedFactoryCode, normalizedShiftCode,
                    DateUtil.formatDateTime(shiftStartTime), normalizedList.size());
            return;
        }

        Date now = new Date();
        this.shiftStockMapper.deleteInvalidByScope(normalizedFactoryCode,
                shiftStartTime, normalizedShiftCode);
        int deleteCount = this.shiftStockMapper.logicDeleteByScope(normalizedFactoryCode,
                shiftStartTime, normalizedShiftCode, operator, now);
        normalizedList.forEach(stock -> {
            stock.setFactoryCode(normalizedFactoryCode);
            stock.setStockDate(normalizedStockDate);
            stock.setShiftCode(normalizedShiftCode);
            stock.setShiftStartTime(shiftStartTime);
            stock.setCreateBy(operator);
            stock.setUpdateBy(operator);
            stock.setCreateTime(now);
            stock.setUpdateTime(now);
            stock.setIsDelete(0);
        });
        if (CollectionUtils.isNotEmpty(normalizedList)) {
            this.baseDao.saveBatch(normalizedList);
        }
        log.info("直裁班次库存快照替换完成：factoryCode={}，shiftCode={}，shiftStartTime={}，失效数量={}，新增数量={}",
                normalizedFactoryCode, normalizedShiftCode, DateUtil.formatDateTime(shiftStartTime),
                deleteCount, normalizedList.size());
    }

    private void validateSource(List<Cd90ShiftStock> stockList) {
        Set<String> materialCodes = new HashSet<>();
        for (Cd90ShiftStock stock : stockList) {
            if (stock == null || StringUtils.isBlank(stock.getMaterialCode())
                    || stock.getStockNum() == null || !materialCodes.add(stock.getMaterialCode().trim())) {
                throw new ServiceException(I18nUtil.getMessage("ui.cd90.shiftStock.syncSourceInvalid"));
            }
            stock.setMaterialCode(stock.getMaterialCode().trim());
        }
    }

    private boolean isSameSnapshot(List<Cd90ShiftStock> existingList,
                                   List<Cd90ShiftStock> incomingList) {
        if (existingList == null || existingList.size() != incomingList.size()) {
            return false;
        }
        for (int index = 0; index < existingList.size(); index++) {
            Cd90ShiftStock existing = existingList.get(index);
            Cd90ShiftStock incoming = incomingList.get(index);
            if (!Objects.equals(existing.getMaterialCode(), incoming.getMaterialCode())
                    || !this.sameNumber(existing.getStockNum(), incoming.getStockNum())
                    || !this.sameNumber(existing.getModifyNum(), incoming.getModifyNum())
                    || !this.sameNumber(existing.getBadNum(), incoming.getBadNum())
                    || !Objects.equals(existing.getSnapshotTime(), incoming.getSnapshotTime())) {
                return false;
            }
        }
        return true;
    }

    private boolean sameNumber(Double first, Double second) {
        BigDecimal firstValue = BigDecimal.valueOf(first == null ? 0D : first);
        BigDecimal secondValue = BigDecimal.valueOf(second == null ? 0D : second);
        return firstValue.compareTo(secondValue) == 0;
    }
}
