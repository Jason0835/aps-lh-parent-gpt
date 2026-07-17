package com.zlt.aps.tc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.tc.api.domain.entity.TcStock;
import com.zlt.aps.tc.mapper.TcStockMapper;
import com.zlt.aps.tc.service.ITcStockService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class TcStockServiceImpl extends AbstractDocService<TcStock> implements ITcStockService {

    private final TcStockMapper stockMapper;

    @Override
    protected String getDocTypeCode() {
        return "TC0912";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("TC0912");
        return sysDocType;
    }

    @Override
    public String checkUnique(TcStock query) {
        String unique = super.checkUnique(query);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tc.stock.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return new ArrayList<>(Arrays.asList("factoryCode", "stockDate", "sidewallCode"));
    }

    /**
     * 逻辑删除并批量保存胎侧库存（事务性操作）
     * 步骤1：逻辑删除当天库存日期的旧数据（IS_DELETE置为1）
     * 步骤2：批量插入MES最新库存数据（新记录，IS_DELETE=0）
     * 历史数据保留，只删当天库存日期的数据
     *
     * @param factoryCode 工厂编码
     * @param stockDate 库存日期
     * @param updateBy  更新者
     * @param list      待插入的胎侧库存列表
     */
    @Override
    public void logicDeleteAndSaveBatch(String factoryCode, Date stockDate, String updateBy, List<TcStock> list) {
        if (factoryCode == null || factoryCode.trim().isEmpty() || stockDate == null) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.mes.stockArgumentsInvalid"));
        }
        List<TcStock> currentStockList = this.stockMapper.selectList(
                new LambdaQueryWrapper<TcStock>()
                        .eq(TcStock::getFactoryCode, factoryCode)
                        .eq(TcStock::getStockDate, stockDate));
        Set<String> currentVersionSet = this.collectDataVersionSet(currentStockList);
        Set<String> incomingVersionSet = this.collectDataVersionSet(list);
        if (!incomingVersionSet.isEmpty() && incomingVersionSet.equals(currentVersionSet)) {
            log.info("胎侧库存同步：工厂={}、库存日期={}的MES数据版本未变化，跳过重复快照写入，dataVersion={}",
                    factoryCode, stockDate, incomingVersionSet);
            return;
        }
        // 仅失效同工厂同日期快照，避免不同工厂之间互相覆盖。
        Date updateTime = new Date();
        int deleteCount = this.stockMapper.update(null, new LambdaUpdateWrapper<TcStock>()
                .eq(TcStock::getFactoryCode, factoryCode)
                .eq(TcStock::getStockDate, stockDate)
                .set(TcStock::getIsDelete, 1)
                .set(TcStock::getUpdateBy, updateBy)
                .set(TcStock::getUpdateTime, updateTime));
        log.info("胎侧库存同步：失效工厂={}、库存日期={}的旧快照，数量={}", factoryCode, stockDate, deleteCount);

        // 批量写入当前MES版本，空结果同样表示该日期快照已清空。
        if (CollectionUtils.isNotEmpty(list)) {
            list.stream().forEach(stock -> {
                stock.setFactoryCode(factoryCode);
                stock.setStockDate(stockDate);
                stock.setCreateBy(updateBy);
            });
            baseDao.saveBatch(list);
            log.info("胎侧库存同步：批量插入完成，插入数量={}", list.size());
        }
    }

    /**
     * 收集库存快照中非空MES数据版本，用于同版本同步幂等判断。
     *
     * @param stockList 库存快照
     * @return 非空数据版本集合
     */
    private Set<String> collectDataVersionSet(List<TcStock> stockList) {
        return CollectionUtils.emptyIfNull(stockList).stream()
                .map(TcStock::getDataVersion)
                .filter(version -> version != null && !version.trim().isEmpty())
                .map(String::trim)
                .collect(Collectors.toSet());
    }
}
