package com.zlt.aps.monthplan.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.monthplan.api.domain.entity.MonthPlanNoticeOrder;
import com.zlt.aps.monthplan.api.domain.vo.MonthPlanAdjustNoticeApplyOperateVo;
import com.zlt.aps.monthplan.api.domain.vo.MonthPlanAdjustNoticeOrderConfirmOperateVo;
import com.zlt.aps.monthplan.api.domain.vo.MonthPlanAdjustNoticeOrderOperateVo;
import com.zlt.aps.monthplan.api.domain.vo.MonthPlanNoticeOrderVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMonthPlanAdjustNoticeOrderRemoteService.java
 * 描    述：IMonthPlanAdjustNoticeOrderRemoteService-月计划调整通知单
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-03-17
 */
@FeignClient(contextId = "IMonthPlanAdjustNoticeOrderRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMonthPlanAdjustNoticeOrderRemoteService {

    /**
     * 查询列表
     *
     * @param QueryVO
     * @return
     */
    @ApiOperation("查询列表")
    @PostMapping("/monthPlanNoticeOrder/list")
    TableDataInfo list(@RequestBody MonthPlanNoticeOrder QueryVO);

    /**
     * 查询列表
     *
     * @param condition
     * @return
     */
    @ApiOperation("查询调整通知单调整明细列表")
    @PostMapping("/monthPlanNoticeOrder/getAdjustDetail")
    TableDataInfo getAdjustDetail(@RequestBody MonthPlanNoticeOrder condition);

    /**
     * 根据ID获取调整通知单编辑信息
     *
     * @param id
     * @return
     */
    @ApiOperation("根据ID获取调整通知单编辑信息")
    @PostMapping("/monthPlanNoticeOrder/getMonthPlanNoticeInfo")
    MonthPlanNoticeOrderVo getMonthPlanNoticeInfo(@RequestBody Long id);

    /**
     * 根据查询条件获取结余库存信息
     *
     * @param noticeOrder
     * @return
     */
    @ApiOperation("根据查询条件获取结余库存信息")
    @PostMapping("/monthPlanNoticeOrder/getStockInfo")
    MonthPlanNoticeOrderVo getMonthPlanNoticeStockInfo(@RequestBody MonthPlanNoticeOrder noticeOrder);

    /**
     * 导入调整通知单
     *
     * @param importContext 导入内容
     * @param updateSupport 是否更新
     * @return
     */
    @ApiOperation("导入分厂月生产计划调整通知单")
    @PostMapping("/monthPlanNoticeOrder/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    /**
     * 保存
     *
     * @param monthPlanNoticeOrder
     * @return
     */
    @ApiOperation("保存")
    @PostMapping("/monthPlanNoticeOrder/save")
    AjaxResult save(@RequestBody MonthPlanNoticeOrder monthPlanNoticeOrder);

    /**
     * 提交
     *
     * @param id
     * @return
     */
    @ApiOperation("提交")
    @PostMapping("/monthPlanNoticeOrder/submit")
    AjaxResult submit(@RequestBody Long id);

    /**
     * 作废取消
     *
     * @param id
     * @return
     */
    @ApiOperation("作废取消")
    @PostMapping("/monthPlanNoticeOrder/cancel")
    AjaxResult cancel(@RequestBody Long id);

    /**
     * 调整通知单进入调整操作接口数据
     *
     * @param noticeOrderOperate
     * @return
     */
    @ApiOperation("调整通知单进入调整操作接口数据")
    @PostMapping("/monthPlanNoticeOrder/getAdjustNoticeAdjustPlan")
    AjaxResult getAdjustNoticeAdjustPlan(@RequestBody MonthPlanNoticeOrder noticeOrderOperate);

    /**
     * 根据调整通知单的调整信息，获取需要调整的计划列表信息
     *
     * @param noticeOrderOperate
     * @return
     */
    @ApiOperation("根据调整通知单的调整信息，获取需要调整的计划列表信息")
    @PostMapping("/monthPlanNoticeOrder/getOperatePlanList")
    AjaxResult getOperatePlanList(@RequestBody MonthPlanAdjustNoticeOrderOperateVo noticeOrderOperate);

    /**
     * 根据调整通知单信息及调减计划，转换对应调增的数量
     *
     * @param param
     * @return
     */
    @PostMapping("/monthPlanNoticeOrder/calculateAddQty")
    @ApiOperation("根据调整通知单信息及调减计划，转换对应调增的数量")
    AjaxResult calculateAddQty(@RequestBody MonthPlanAdjustNoticeApplyOperateVo param);

    /**
     * 对调整通知单执行调整
     *
     * @param noticeOrderOperate
     * @return
     */
    @ApiOperation("对调整通知单执行调整-V3版本")
    @PostMapping("/monthPlanNoticeOrder/executeAdjust")
    AjaxResult executeAdjust(@RequestBody MonthPlanAdjustNoticeOrderOperateVo noticeOrderOperate);

    /**
     * 根据ID获取详细信息
     *
     * @param id
     * @return
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/monthPlanNoticeOrder/{id}")
    MonthPlanNoticeOrder getInfo(@PathVariable("id") Long id);

    /**
     * 对调整通知单进行确认调整操作
     * 可多次操作
     * 使用对月度计划排产计划直接编辑调整方式
     *
     * @param confirmAdjust
     * @return
     */
    @Deprecated
    @ApiOperation("确认调整-V4版本")
    @PostMapping("/monthPlanNoticeOrder/confirmAdjust")
    AjaxResult confirmAdjust(@RequestBody MonthPlanAdjustNoticeOrderConfirmOperateVo confirmAdjust);
}
