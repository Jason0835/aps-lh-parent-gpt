package com.zlt.aps.monthplan.factory.service;


import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.monthplan.api.domain.entity.MonthPlanAdjustDetail;
import com.zlt.aps.monthplan.api.domain.entity.MonthPlanNoticeOrder;
import com.zlt.aps.monthplan.api.domain.vo.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMonthPlanAdjustNoticeOrderService.java
 * 描    述：IMonthPlanAdjustNoticeOrderService-月计划调整通知单后端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-05-21
 */
public interface IMonthPlanAdjustNoticeOrderService {

    /**
     * 列表查询
     *
     * @param queryCondition 查询条件
     * @return
     */
    List<MonthPlanNoticeOrder> selectList(MonthPlanNoticeOrder queryCondition);

    /**
     * 获取调整通知单的调整明细信息
     *
     * @param noticeNo 调整通知单号
     * @return
     */
    List<MonthPlanAdjustDetailVo> getNoticeDetail(String noticeNo);

    /**
     * 根据ID获取调整通知单明细信息
     *
     * @param id
     * @return
     */
    MonthPlanNoticeOrderVo getMonthPlanNoticeInfo(Long id);

    /**
     * 根据分厂，年月及SAP代码获取结余库存
     *
     * @param noticeOrder
     * @return
     */
    MonthPlanNoticeOrderVo getMonthPlanNoticeStockInfo(MonthPlanNoticeOrder noticeOrder);

    /**
     * 保存调整通知单
     *
     * @param noticeOrder
     * @return
     */
    AjaxResult save(MonthPlanNoticeOrder noticeOrder);

    /**
     * 调整通知单提交，需要进行库存对冲
     *
     * @param id
     * @return
     */
    AjaxResult submit(Long id);

    /**
     * 调整通知单作废，直接标记作废
     *
     * @param id
     * @return
     */
    AjaxResult cancel(Long id);

    /**
     * 根据调整通知单，得到调整信息
     * 包含调整通知单，起始的调整日期
     * 及能调整的日期周期
     *
     * @param noticeOrderOperate
     * @return
     */
    AjaxResult getAdjustNoticeAdjustPlan(MonthPlanNoticeOrder noticeOrderOperate);

    /**
     * 根据调整通知单及调整信息，获取需要调整的计划列表
     *
     * @param noticeOrderOperate
     * @return
     */
    AjaxResult getOperatePlanList(MonthPlanAdjustNoticeOrderOperateVo noticeOrderOperate);

    /**
     * 根据调整通知单信息及调减计划，转换对应调增的数量
     *
     * @param param
     * @return
     */
    AjaxResult calculateAddQty(MonthPlanAdjustNoticeApplyOperateVo param);

    /**
     * 对调整通知单确认进行调整(V3版本)
     * 调减时，一定会有调减的计划
     *
     * @param noticeOrderOperate
     * @return
     */
    AjaxResult confirmAdjust(MonthPlanAdjustNoticeOrderOperateVo noticeOrderOperate);


    /**
     * 对调整通知单确认进行调整(V4版本)
     * 采用对原计划进行编辑的方式来调整
     *
     * @param noticeOrderConfirmOperate
     * @return
     */
    @Deprecated
    AjaxResult confirmAdjustByDetail(MonthPlanAdjustNoticeOrderConfirmOperateVo noticeOrderConfirmOperate);

    /**
     * 导入数据处理
     *
     * @param excelDataList excel解析后的数据
     * @param updateSupport 是否需要更新处理 true 更新 false不更新
     * @param importLogId   导入日志ID
     * @return
     */
    AjaxResult importData(List<MonthPlanNoticeOrder> excelDataList, boolean updateSupport, Long importLogId);
}
