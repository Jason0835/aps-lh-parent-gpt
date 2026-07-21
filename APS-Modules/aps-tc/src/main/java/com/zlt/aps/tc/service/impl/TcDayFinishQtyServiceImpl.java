package com.zlt.aps.tc.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.tc.api.domain.entity.TcDayFinishQty;
import com.zlt.aps.tc.mapper.TcDayFinishQtyMapper;
import com.zlt.aps.tc.service.ITcDayFinishQtyService;
import com.zlt.bill.common.service.AbstractDocService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 胎侧日完成量MES快照服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class TcDayFinishQtyServiceImpl extends AbstractDocService<TcDayFinishQty>
        implements ITcDayFinishQtyService {

    private final TcDayFinishQtyMapper dayFinishQtyMapper;

    /**
     * 获取内部单据类型编码。
     *
     * @return 单据类型编码
     */
    @Override
    protected String getDocTypeCode() {
        return "TC_DAY_FINISH";
    }

    /**
     * 失效旧快照并批量保存MES最新日完成量。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate MES业务日期
     * @param updateBy 更新人
     * @param insertList 日完成量列表
     * @throws ServiceException 参数无效或持久化失败时抛出
     */
    @Override
    public void logicDeleteAndSaveBatch(String factoryCode, Date scheduleDate, String updateBy,
                                        List<TcDayFinishQty> insertList) {
        if (factoryCode == null || factoryCode.trim().isEmpty() || scheduleDate == null) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.mes.finishArgumentsInvalid"));
        }
        int deleteCount = this.dayFinishQtyMapper.update(null, new LambdaUpdateWrapper<TcDayFinishQty>()
                .eq(TcDayFinishQty::getFactoryCode, factoryCode)
                .eq(TcDayFinishQty::getScheduleDate, scheduleDate)
                .set(TcDayFinishQty::getIsDelete, 1)
                .set(TcDayFinishQty::getUpdateBy, updateBy)
                .set(TcDayFinishQty::getUpdateTime, new Date()));
        if (CollectionUtils.isNotEmpty(insertList)) {
            insertList.stream().forEach(item -> {
                item.setFactoryCode(factoryCode);
                item.setScheduleDate(scheduleDate);
                item.setCreateBy(updateBy);
            });
            baseDao.saveBatch(insertList);
        }
        log.info("胎侧日完成量同步完成, factoryCode={}, scheduleDate={}, invalidated={}, inserted={}",
                factoryCode, scheduleDate, deleteCount, CollectionUtils.size(insertList));
    }
}
