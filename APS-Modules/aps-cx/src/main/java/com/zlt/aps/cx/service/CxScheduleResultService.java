package com.zlt.aps.cx.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zlt.aps.cx.api.domain.entity.CxStock;
import com.zlt.aps.cx.vo.ScheduleQueryVo;
import com.zlt.aps.cx.vo.ScheduleResultVo;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.bill.common.service.IDocService;

import java.time.LocalDate;
import java.util.List;

/**
 * 成型排程结果服务接口
 *
 * @author APS Team
 */
public interface CxScheduleResultService extends IDocService<CxScheduleResult> {

    /**
     * 根据排程日期查询排程结果
     *
     * @param scheduleDate 排程日期
     * @return 排程结果列表
     */
    List<CxScheduleResult> listByScheduleDate(LocalDate scheduleDate);


}
