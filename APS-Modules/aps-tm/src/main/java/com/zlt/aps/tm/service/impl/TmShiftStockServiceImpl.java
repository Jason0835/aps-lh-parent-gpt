package com.zlt.aps.tm.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.tm.api.constant.TmScheduleConstants;
import com.zlt.aps.tm.api.domain.entity.TmShiftStock;
import com.zlt.aps.tm.mapper.TmShiftStockMapper;
import com.zlt.aps.tm.service.ITmShiftStockService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * 胎面自动滚动班次库存服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class TmShiftStockServiceImpl extends AbstractDocService<TmShiftStock>
        implements ITmShiftStockService {

    private final TmShiftStockMapper shiftStockMapper;

    @Override
    protected String getDocTypeCode() {
        return "TM0813";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode(this.getDocTypeCode());
        return sysDocType;
    }

    /**
     * 替换指定班次库存快照，空集合也会先失效旧快照。
     *
     * @param factoryCode 工厂编码
     * @param stockDate MES库存物理日期
     * @param shiftOrder 班次顺序
     * @param updateBy 更新人
     * @param stockList 新库存快照
     * @throws ServiceException 工厂、日期或班次非法时抛出
     */
    @Override
    public void replaceShiftStock(String factoryCode, Date stockDate, Integer shiftOrder,
                                  String updateBy, List<TmShiftStock> stockList) {
        if (StrUtil.isBlank(factoryCode) || stockDate == null || shiftOrder == null
                || shiftOrder < 1 || shiftOrder > TmScheduleConstants.TM_MAX_SHIFT_ORDER) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.rollingRequestInvalid"));
        }
        Date normalizedStockDate = DateUtil.beginOfDay(stockDate);
        String operator = StrUtil.blankToDefault(StrUtil.trim(updateBy), "MES");
        Date updateTime = new Date();
        int deleteCount = this.shiftStockMapper.update(null, new LambdaUpdateWrapper<TmShiftStock>()
                .eq(TmShiftStock::getFactoryCode, StrUtil.trim(factoryCode))
                .eq(TmShiftStock::getStockDate, normalizedStockDate)
                .eq(TmShiftStock::getShiftOrder, shiftOrder)
                .set(TmShiftStock::getIsDelete, 1)
                .set(TmShiftStock::getUpdateBy, operator)
                .set(TmShiftStock::getUpdateTime, updateTime));
        List<TmShiftStock> normalizedList = stockList == null ? Collections.emptyList() : stockList;
        normalizedList.forEach(stock -> {
            stock.setFactoryCode(StrUtil.trim(factoryCode));
            stock.setStockDate(normalizedStockDate);
            stock.setShiftOrder(shiftOrder);
            stock.setCreateBy(operator);
            stock.setUpdateBy(operator);
            stock.setIsDelete(0);
        });
        if (CollectionUtils.isNotEmpty(normalizedList)) {
            this.baseDao.saveBatch(normalizedList);
        }
        log.info("胎面班次库存快照替换完成，factoryCode={}，stockDate={}，shiftOrder={}，失效数量={}，新增数量={}",
                factoryCode, DateUtil.formatDate(normalizedStockDate), shiftOrder, deleteCount, normalizedList.size());
    }
}
