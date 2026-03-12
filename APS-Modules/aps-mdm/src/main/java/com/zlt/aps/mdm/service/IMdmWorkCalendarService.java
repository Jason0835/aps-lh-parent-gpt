package com.zlt.aps.mdm.service;

import com.ruoyi.common.core.domain.SysDictData;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.mdm.api.domain.entity.MdmWorkCalendar;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMdmWorkCalendarService.java
 * 描    述：IMdmWorkCalendarService工作日历后端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-03
 */
public interface IMdmWorkCalendarService extends IDocService<MdmWorkCalendar> {

    /**
     * 根据用户名称过滤出可查看的工序列表
     *
     * @param userName 用户名称
     * @return 结果
     */
    List<SysDictData> selectProcCodeList(String userName);

    /**
     * 生成全年工作日历
     *
     * @param entity 条件
     * @return 结果
     */
    AjaxResult genAnnualPlan(MdmWorkCalendar entity);

    /**
     * 复制工作日历
     *
     * @param entity 条件
     * @return 结果
     */
    AjaxResult copyWorkCalendar(MdmWorkCalendar entity);
}
