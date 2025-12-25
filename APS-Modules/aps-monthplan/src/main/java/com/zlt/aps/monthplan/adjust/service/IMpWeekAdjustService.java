package com.zlt.aps.monthplan.adjust.service;

import com.zlt.aps.monthplan.api.domain.dto.MpRollAdjustContextDTO;

/**
 * 周程滚动调整接口
 * @author wengpc
 */
public interface IMpWeekAdjustService {

    /**
     * 生成调整明细
     * @param contextDTO 周程滚动调整上下文对象
     */
    void generateAdjust(MpRollAdjustContextDTO contextDTO);

    /**
     * 自动调整
     * @param contextDTO 周程滚动调整上下文对象
     */
    void autoAdjust(MpRollAdjustContextDTO contextDTO);

    /**
     * 调整确认
     * @param contextDTO 周程滚动调整上下文对象
     */
    void confirmAdjust(MpRollAdjustContextDTO contextDTO);

}

