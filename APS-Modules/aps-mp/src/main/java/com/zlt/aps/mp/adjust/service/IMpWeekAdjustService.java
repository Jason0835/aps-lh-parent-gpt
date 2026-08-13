package com.zlt.aps.mp.adjust.service;

import com.zlt.aps.mp.api.domain.dto.MpRollAdjustContextDTO;

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
     * 生产对齐
     * @param contextDTO 周程滚动调整上下文对象
     */
    void productAlign(MpRollAdjustContextDTO contextDTO);

    /**
     * 调整确认
     * @param contextDTO 周程滚动调整上下文对象
     */
    void confirmAdjust(MpRollAdjustContextDTO contextDTO);


    /**
     * 获得初始版本
     * @param contextDTO 周程滚动调整上下文对象
     */
    void initVersion(MpRollAdjustContextDTO contextDTO);

    /**
     * 重新计算
     * @param contextDTO 周程滚动调整上下文对象
     */
    void recalculate(MpRollAdjustContextDTO contextDTO, Boolean isHandleMonthPlanStatistics);
    
    /**
     * 校验胶囊卡盘
     * @param contextDTO
     */
    void checkCapsuleChuckLimit(MpRollAdjustContextDTO contextDTO);
    
    /**
     * 检查模壳标准限制
     * @param contextDTO
     */
    void checkMouldShellLimit(MpRollAdjustContextDTO contextDTO);
    
    /**
     * 初始化上下文
     * @param factoryCode
     * @param productType
     * @return
     */
    MpRollAdjustContextDTO initContextDTO(String factoryCode,String productType);
    
    /**
     * 处理月计划统计结果
     * @param contextDTO
     * @param tempFlag
     */
    void handleMonthPlanStatistics(MpRollAdjustContextDTO contextDTO, String tempFlag);
}

