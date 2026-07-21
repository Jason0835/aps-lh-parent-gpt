package com.zlt.aps.tc.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.tc.api.domain.entity.TcScheFinishQty;
import com.zlt.bill.common.service.IDocService;

import java.util.Date;
import java.util.List;

/**
 * 胎侧班次完成量MES快照服务。
 */
public interface ITcScheFinishQtyService extends IDocService<TcScheFinishQty> {

    /**
     * 失效旧快照并批量保存MES最新完成量。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate MES业务日期
     * @param updateBy 更新人
     * @param insertList 完成量列表
     */
    void logicDeleteAndSaveBatch(String factoryCode, Date scheduleDate, String updateBy,
                                 List<TcScheFinishQty> insertList);

    /**
     * 将MES三班完成量回写胎侧六班排程结果。
     *
     * @param finishQtyList MES完成量列表
     * @return 回写摘要
     */
    AjaxResult writeBackScheduleResultFinishQty(List<TcScheFinishQty> finishQtyList);
}
