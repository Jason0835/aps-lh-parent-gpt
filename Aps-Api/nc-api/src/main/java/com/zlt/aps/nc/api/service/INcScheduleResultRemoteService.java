package com.zlt.aps.nc.api.service;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.nc.api.domain.entity.NcDayFinishQty;
import com.zlt.aps.nc.api.domain.entity.NcScheduleResult;

import io.swagger.annotations.ApiOperation;


/**
 * 内衬胶排程结果Service接口
 *
 * @author zlt
 * @date 2021-06-24
 */
@FeignClient(contextId = "INcScheduleResultService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.nc:nc}")
public interface INcScheduleResultRemoteService {

    /**
     * 查询内衬排程结果列表
     */
    @PostMapping("/ncScheduleResult/list")
    TableDataInfo list(@RequestBody NcScheduleResult djScheduleResult);


    /**
     * 内衬排程插单
     */
    @PostMapping("/ncScheduleResult/add")
    AjaxResult add(@RequestBody NcScheduleResult djScheduleResult);

    /**
     * 插单前置校验（含跨天日期计算）
     */
    @PostMapping("/ncScheduleResult/validateAdd")
    AjaxResult validateAdd(@RequestBody NcScheduleResult djScheduleResult);


    /**
     * 修改内衬排程结果
     */
    @PostMapping("/ncScheduleResult/edit")
    AjaxResult edit(@RequestBody NcScheduleResult djScheduleResult);

    /**
     * 调量前置校验（产能校验）
     */
    @PostMapping("/ncScheduleResult/changeQtyValidate")
    AjaxResult changeQtyValidate(@RequestBody NcScheduleResult scheduleResult);

    /**
     * 调量
     */
    @PostMapping("/ncScheduleResult/changeQty")
    AjaxResult changeQty(@RequestBody NcScheduleResult scheduleResult);

    /**
     * 转机台
     */
    @PostMapping("/ncScheduleResult/changeMachine")
    public AjaxResult changeMachine(@RequestBody NcScheduleResult scheduleResult);


    /**
     * 删除内衬排程结果
     */
    @DeleteMapping("/ncScheduleResult/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);


    /**
     * 根据ID获取详细信息
     */
    @GetMapping(value = "/ncScheduleResult/{id}")
    NcScheduleResult getInfo(@PathVariable("id") Long id);


    /**
     * 获取排程结果
     */
    @PostMapping("/ncScheduleResult/getList")
    List<NcScheduleResult> getList(@RequestBody NcScheduleResult djScheduleResult);

    /**
     * 唯一性校验
     */
    @PostMapping("/ncScheduleResult/checkUnique")
    Boolean checkUnique(@RequestBody NcScheduleResult entity);

    /**
     * 导出列表
     */
    @PostMapping("/ncScheduleResult/export")
    byte[] export(@RequestBody NcScheduleResult djScheduleResult);

    /**
     * 自动排程
     * @param djScheduleResult
     * @return
     */
    @PostMapping("/ncScheduleResult/autoPlan")
    AjaxResult autoPlan(@RequestBody NcScheduleResult djScheduleResult);

    /**
     * 自动排程
     * @param djScheduleResult
     * @return
     */
    @PostMapping("/ncScheduleResult/publish")
    AjaxResult publish(@RequestBody NcScheduleResult djScheduleResult);

    /**
     * 查询排程日期是否已发布
     * @param scheduleDate 排程日期
     * @return 是否已经发布
     */
    @PostMapping("/ncScheduleResult/isPublish")
    Boolean isPublish(@RequestBody NcScheduleResult entity);

    /**
     * 导入数据
     */
    @PostMapping("/ncScheduleResult/importData")
    public AjaxResult importData(@RequestBody List<NcScheduleResult> list, @RequestParam("importLogId") Long importLogId,@RequestParam("scheduleDate")String scheduleDate);


    /**
     * 根据IDS获取详细信息
     */
    @PostMapping(value = "/ncScheduleResult/getInfos")
    List<NcScheduleResult> getInfos(@RequestBody NcScheduleResult scheduleResult);

    /**
     * 选机台
     */
    @PostMapping("/ncScheduleResult/chooseMachine")
    public AjaxResult chooseMachine(@RequestBody NcScheduleResult scheduleResult);

    /**
     * 均衡
     */
    @PostMapping("/ncScheduleResult/balance")
    public AjaxResult balance(@RequestBody NcScheduleResult entity);

    /**
     * 同胶料归并生产
     */
    @PostMapping("/ncScheduleResult/mergeProduct")
    public AjaxResult mergeProduct(@RequestBody NcScheduleResult entity);


    /**
     * 根据排程日期查询当前日期发布状态为"发布中"或"超时失败"的记录
     * @param scheduleDate 排程日期
     * @return 查询到的记录数
     */
    @PostMapping("/ncScheduleResult/isReleasingOrTimeoutByDate")
    public int isReleasingOrTimeoutByDate(@RequestBody NcScheduleResult scheduleResult);

    /**
     * 更改发布状态
     * @param scheduleDate 排程日期
     * @return 结果
     */
    @PostMapping("/ncScheduleResult/changeReleaseStatus")
    public AjaxResult changeReleaseStatus(@RequestBody NcScheduleResult entity);

    /**
     * 归并中夜班计划量，合并到同一个班次
     *
     * @param ids             id
     * @param classifiedShift 合并班次
     */
    @PostMapping("/ncScheduleResult/combinationMiddleAndNight/{ids}")
    public AjaxResult combinationMiddleAndNight(@PathVariable("ids") Long[] ids, @RequestParam("classifiedShift") String classifiedShift);

    /**
     * 导入完成量
     * @param list 完成量集合
     * @param importLogId 导入记录id
     * @return 结果
     */
    @PostMapping("/ncScheduleResult/importFinishQty")
    @ApiOperation("导入完成量")
    AjaxResult importFinishQty(@RequestBody List<NcDayFinishQty> list, @RequestParam("importLogId") Long importLogId);

    /**
     * 获取排程日期的昨日早班合计，夜班合计，早班合计，库存合计，理论交班库存合计
     *
     * @param scheduleResult 排程日期
     * @return 结果
     */
    @PostMapping("/ncScheduleResult/getSummaryVo")
    @ApiOperation("获取排程日期的排程结果合计")
    public AjaxResult getSummaryVo(@RequestBody NcScheduleResult scheduleResult);

    /**
     * 获取连续6个班次的表头
     */
    @GetMapping("/ncScheduleResult/getWorkClass")
    @ApiOperation("获取连续6个班次的表头")
    public AjaxResult getWorkClass(@RequestParam(value = "scheduleDate", required = false) String scheduleDate);

    /**
     * 获取内衬下拉列表
     */
    @GetMapping("/ncScheduleResult/getPaddingDistList")
    @ApiOperation("获取内衬下拉列表")
    AjaxResult getPaddingDistList();

    /**
     * 获取当前服务器时间对应的班次信息
     */
    @GetMapping("/ncScheduleResult/getCurrentShift")
    @ApiOperation("获取当前班次信息")
    AjaxResult getCurrentShift();
}
