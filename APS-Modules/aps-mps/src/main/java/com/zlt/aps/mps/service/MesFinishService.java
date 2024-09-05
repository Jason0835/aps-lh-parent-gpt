package com.zlt.aps.mps.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Gim
 */
public interface MesFinishService {
    // 成型排程完成量回报
    @Transactional
    AjaxResult mergeCxFinish(String dataVersion);

    // 硫化排程完成量回报
    @Transactional
    AjaxResult mergeLhFinish(String dataVersion);

    // 成型排程日完成量回报
    @Transactional
    AjaxResult mergeCxDayFinish(String dataVersion);

    // 硫化排程日完成量回报
    @Transactional
    AjaxResult mergeLhDayFinish(String dataVersion);

    // 成型8-12点的完成量
    @Transactional
    AjaxResult mergeCxPartFinish(String dataVersion);

    // 胎面完成量回报
    @Transactional
    AjaxResult mergeTmFinish(String dataVersion);

    // 胎面日完成量回报
    @Transactional
    AjaxResult mergeTmDayFinish(String dataVersion);

    // 胎侧完成量回报
    @Transactional
    AjaxResult mergeTcFinish(String dataVersion);

    // 胎侧日完成量回报
    @Transactional
    AjaxResult mergeTcDayFinish(String dataVersion);

    // 内衬完成量回报
    @Transactional
    AjaxResult mergeNcFinish(String dataVersion);

    // 内衬日完成量回报
    @Transactional
    AjaxResult mergeNcDayFinish(String dataVersion);

    // 15度裁断完成量回报
    @Transactional
    AjaxResult mergeCd15Finish(String dataVersion);

    // 15度裁断日完成量回报
    @Transactional
    AjaxResult mergeCd15DayFinish(String dataVersion);

    // 90度裁断完成量回报
    @Transactional
    AjaxResult mergeCd90Finish(String dataVersion);

    // 90度裁断日完成量回报
    @Transactional
    AjaxResult mergeCd90DayFinish(String dataVersion);

    // *钢带压延没有完成量回报
    // 纤维压延完成量回报
    @Transactional
    AjaxResult mergeXwyyFinish(String dataVersion);

    // 纤维压延日完成量回报
    @Transactional
    AjaxResult mergeXwyyDayFinish(String dataVersion);

    // 胎圈完成量回报
    @Transactional
    AjaxResult mergeTqFinish(String dataVersion);

    // 胎圈日完成量回报
    @Transactional
    AjaxResult mergeTqDayFinish(String dataVersion);

    // 钢丝圈完成量回报
    @Transactional
    AjaxResult mergeGsqFinish(String dataVersion);

    // 钢丝圈日完成量回报
    @Transactional
    AjaxResult mergeGsqDayFinish(String dataVersion);

    // 各工序班次完成量同步
    @Transactional
    AjaxResult mergeClassFinishQty(String dataVersion);
}
