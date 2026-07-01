package com.zlt.aps.dj.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.dj.api.domain.entity.DjDayFinishQty;
import com.zlt.aps.dj.api.domain.entity.DjScheduleResult;

import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 垫胶胶排程结果Service接口
 *
 * @author zlt
 * @date 2021-06-24
 */
@FeignClient(contextId = "IDjScheduleResultRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.dj:dj}")
public interface IDjScheduleResultRemoteService {

    /**
     * 查询垫胶排程结果列表
     */
    @PostMapping("/djScheduleResult/list")
    TableDataInfo list(@RequestBody DjScheduleResult djScheduleResult);


    /**
     * 垫胶排程插单
     */
    @PostMapping("/djScheduleResult/add")
    AjaxResult add(@RequestBody DjScheduleResult djScheduleResult);


    /**
     * 修改垫胶排程结果
     */
    @PostMapping("/djScheduleResult/edit")
    AjaxResult edit(@RequestBody DjScheduleResult djScheduleResult);

    /**
     * 调量
     */
    @PostMapping("/djScheduleResult/changeQty")
    public AjaxResult changeQty(@RequestBody DjScheduleResult scheduleResult);

    /**
     * 转机台
     */
    @PostMapping("/djScheduleResult/changeMachine")
    public AjaxResult changeMachine(@RequestBody DjScheduleResult scheduleResult);


    /**
     * 删除垫胶排程结果
     */
    @DeleteMapping("/djScheduleResult/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);


    /**
     * 根据ID获取详细信息
     */
    @GetMapping(value = "/djScheduleResult/{id}")
    DjScheduleResult getInfo(@PathVariable("id") Long id);


    /**
     * 获取排程结果
     */
    @PostMapping("/djScheduleResult/getList")
    List<DjScheduleResult> getList(@RequestBody DjScheduleResult djScheduleResult);

    /**
     * 唯一性校验
     */
    @PostMapping("/djScheduleResult/checkUnique")
    Boolean checkUnique(@RequestBody DjScheduleResult entity);

    /**
     * 导出列表
     */
    @PostMapping("/djScheduleResult/export")
    byte[] export(@RequestBody DjScheduleResult djScheduleResult);

    /**
     * 自动排程
     * @param djScheduleResult
     * @return
     */
    @PostMapping("/djScheduleResult/autoPlan")
    AjaxResult autoPlan(@RequestBody DjScheduleResult djScheduleResult);

    /**
     * 自动排程
     * @param djScheduleResult
     * @return
     */
    @PostMapping("/djScheduleResult/publish")
    AjaxResult publish(@RequestBody DjScheduleResult djScheduleResult);

    /**
     * 查询排程日期是否已发布
     * @param scheduleDate 排程日期
     * @return 是否已经发布
     */
    @PostMapping("/djScheduleResult/isPublish")
    Boolean isPublish(@RequestBody DjScheduleResult entity);

    /**
     * 导入数据
     */
    @PostMapping("/djScheduleResult/importData")
    public AjaxResult importData(@RequestBody List<DjScheduleResult> list, @RequestParam("importLogId") Long importLogId,@RequestParam("scheduleDate")String scheduleDate);


    /**
     * 根据IDS获取详细信息
     */
    @PostMapping(value = "/djScheduleResult/getInfos")
    List<DjScheduleResult> getInfos(@RequestBody DjScheduleResult scheduleResult);

    /**
     * 选机台
     */
    @PostMapping("/djScheduleResult/chooseMachine")
    public AjaxResult chooseMachine(@RequestBody DjScheduleResult scheduleResult);

    /**
     * 均衡
     */
    @PostMapping("/djScheduleResult/balance")
    public AjaxResult balance(@RequestBody DjScheduleResult entity);

    /**
     * 同胶料归并生产
     */
    @PostMapping("/djScheduleResult/mergeProduct")
    public AjaxResult mergeProduct(@RequestBody DjScheduleResult entity);


    /**
     * 根据排程日期查询当前日期发布状态为"发布中"或"超时失败"的记录
     * @param scheduleDate 排程日期
     * @return 查询到的记录数
     */
    @PostMapping("/djScheduleResult/isReleasingOrTimeoutByDate")
    public int isReleasingOrTimeoutByDate(@RequestBody DjScheduleResult scheduleResult);

    /**
     * 更改发布状态
     * @param scheduleDate 排程日期
     * @return 结果
     */
    @PostMapping("/djScheduleResult/changeReleaseStatus")
    public AjaxResult changeReleaseStatus(@RequestBody DjScheduleResult entity);

    /**
     * 归并中夜班计划量，合并到同一个班次
     *
     * @param ids             id
     * @param classifiedShift 合并班次
     */
    @PostMapping("/djScheduleResult/combinationMiddleAndNight/{ids}")
    public AjaxResult combinationMiddleAndNight(@PathVariable("ids") Long[] ids, @RequestParam("classifiedShift") String classifiedShift);

    /**
     * 导入完成量
     * @param list 完成量集合
     * @param importLogId 导入记录id
     * @return 结果
     */
    @PostMapping("/djScheduleResult/importFinishQty")
    @ApiOperation("导入完成量")
    AjaxResult importFinishQty(@RequestBody List<DjDayFinishQty> list, @RequestParam("importLogId") Long importLogId);

    /**
     * 获取排程日期的昨日早班合计，夜班合计，早班合计，库存合计，理论交班库存合计
     *
     * @param scheduleResult 排程日期
     * @return 结果
     */
    @PostMapping("/djScheduleResult/getSummaryVo")
    @ApiOperation("获取排程日期的排程结果合计")
    public AjaxResult getSummaryVo(@RequestBody DjScheduleResult scheduleResult);

    /**
     * 获取连续6个班次的表头
     */
    @GetMapping("/djScheduleResult/getWorkClass")
    @ApiOperation("获取连续6个班次的表头")
    public AjaxResult getWorkClass(@RequestParam(value = "scheduleDate", required = false) String scheduleDate);

    /**
     * 获取垫胶下拉列表
     */
    @GetMapping("/djScheduleResult/getPaddingDistList")
    @ApiOperation("获取垫胶下拉列表")
    AjaxResult getPaddingDistList();
}
