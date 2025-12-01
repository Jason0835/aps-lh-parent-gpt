package com.zlt.aps.mps.mapper;

import java.util.Date;
import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.zlt.aps.mps.api.domain.ProductionReminderUser;
import com.zlt.aps.mps.api.domain.UnfinishedScheduleResult;

/**
 * 成型月结库存Mapper接口
 *
 * @author zlt
 * @date 2025-02-21
 */
public interface NoticeMapper {

    /**
     * 查询生产结果通知人
     *
     * @param dto 成型定额设定
     * @return 成型定额设定集合
     */
    List<ProductionReminderUser> queryProductionReminder();

    /**
     * 查询有未完成量的排程信息
     * 
     * @param scheduleDate 查询排产日期
     * @return
     */
    List<UnfinishedScheduleResult> queryUnfinishedSchedule(@Param("scheduleDate") Date scheduleDate);
}
