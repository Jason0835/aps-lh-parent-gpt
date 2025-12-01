package com.zlt.aps.cd90.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd90.api.domain.entity.Cd90DayFinishQty;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleResult;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 90度裁断排程结果Service接口
 *
 * @author zlt
 * @date 2021-07-06
 */
@FeignClient(contextId = "ICd90ScheduleResultService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cd90:cd90}")
public interface ICd90ScheduleResultService {


    /**
     * 查询90度裁断排程结果列表
     */
    @PostMapping("/cd90ScheduleResult/list")
    TableDataInfo list(@RequestBody Cd90ScheduleResult cd90ScheduleResult);


    /**
     * 新增90度裁断排程结果
     */
    @PostMapping("/cd90ScheduleResult/add")
    AjaxResult add(@RequestBody Cd90ScheduleResult cd90ScheduleResult);


    /**
     * 修改90度裁断排程结果
     */
    @PostMapping("/cd90ScheduleResult/edit")
    AjaxResult edit(@RequestBody Cd90ScheduleResult cd90ScheduleResult);

    /**
     * 调量
     */
    @PostMapping("/cd90ScheduleResult/changeQty")
    public AjaxResult changeQty(@RequestBody Cd90ScheduleResult scheduleResult);

    /**
     * 转机台
     */
    @PostMapping("/cd90ScheduleResult/changeMachine")
    public AjaxResult changeMachine(@RequestBody Cd90ScheduleResult scheduleResult);

    /**
     * 选机台
     */
    @PostMapping("/cd90ScheduleResult/chooseMachine")
    public AjaxResult chooseMachine(@RequestBody Cd90ScheduleResult scheduleResult);

    /**
     * 删除90度裁断排程结果
     */
    @DeleteMapping("/cd90ScheduleResult/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据IDS获取详细信息
     */
    @PostMapping(value = "/cd90ScheduleResult/getInfos")
    List<Cd90ScheduleResult> getInfos(@RequestBody Cd90ScheduleResult scheduleResult);


    /**
     * 根据ID获取详细信息
     */
    @GetMapping(value = "/cd90ScheduleResult/{id}")
    Cd90ScheduleResult getInfo(@PathVariable("id") Long id);


    /**
     * 校验90度裁断排程结果唯一性
     */
    @PostMapping("/cd90ScheduleResult/checkScheduleResultUnique")
    List<Cd90ScheduleResult> checkScheduleResultUnique(@RequestBody Cd90ScheduleResult cd90ScheduleResult);


    /**
     * 90度裁断排程结果列表
     */
    @PostMapping("/cd90ScheduleResult/getList")
    List<Cd90ScheduleResult> getList(@RequestBody Cd90ScheduleResult cd90ScheduleResult);

    /**
     * 导出列表
     */
    @PostMapping("/cd90ScheduleResult/export")
    byte[] export(@RequestBody Cd90ScheduleResult cd90ScheduleResult);

    /**
     * 自动排程
     */
    @PostMapping("/cd90ScheduleResult/autoPlan")
    AjaxResult autoPlan(@RequestBody Cd90ScheduleResult cd90ScheduleResult);

    /**
     * 自动排程
     */
    @PostMapping("/cd90ScheduleResult/publish")
    AjaxResult publish(@RequestBody Cd90ScheduleResult cd90ScheduleResult);

    /**
     * 查询排程日期是否已发布
     *
     * @param scheduleDate 排程日期
     * @return 是否已经发布
     */
    @PostMapping("/cd90ScheduleResult/isPublish")
    Boolean isPublish(@RequestBody Cd90ScheduleResult entity);

    /**
     * 导入数据
     */
    @PostMapping("/cd90ScheduleResult/importData")
    public AjaxResult importData(@RequestBody List<Cd90ScheduleResult> list, @RequestParam("importLogId") Long importLogId,@RequestParam("scheduleDate")String scheduleDate);

    /**
     * 均衡
     */
    @PostMapping("/cd90ScheduleResult/balance")
    public AjaxResult balance(@RequestBody Cd90ScheduleResult entity);

    /**
     * 根据排程日期查询当前日期发布状态为"发布中"或"超时失败"的记录
     * @param scheduleDate 排程日期
     * @return 查询到的记录数
     */
    @PostMapping("/cd90ScheduleResult/isReleasingOrTimeoutByDate")
    public int isReleasingOrTimeoutByDate(@RequestBody Cd90ScheduleResult entity);

    /**
     * 更改发布状态
     * @param scheduleDate 排程日期
     * @return 结果
     */
    @PostMapping("/cd90ScheduleResult/changeReleaseStatus")
    public AjaxResult changeReleaseStatus(@RequestBody Cd90ScheduleResult entity);

    /**
     * 归并中夜班计划量，合并到同一个班次
     *
     * @param ids             id
     * @param classifiedShift 合并班次
     */
    @PostMapping("/cd90ScheduleResult/combinationMiddleAndNight/{ids}")
    public AjaxResult combinationMiddleAndNight(@PathVariable("ids")Long[] ids, @RequestParam("classifiedShift") String classifiedShift);

    /**
     * 导入完成量
     * @param list 完成量集合
     * @param importLogId 导入记录id
     * @return 结果
     */
    @PostMapping("/cd90ScheduleResult/importFinishQty")
    @ApiOperation("导入完成量")
    AjaxResult importFinishQty(@RequestBody List<Cd90DayFinishQty> list, @RequestParam("importLogId") Long importLogId);

    /**
     * 获取排程日期的昨日早班合计，夜班合计，早班合计，库存合计，理论交班库存合计
     *
     * @param scheduleResult 排程日期
     * @return 结果
     */
    @PostMapping("/cd90ScheduleResult/getSummaryVo")
    @ApiOperation("获取排程日期的排程结果合计")
    public AjaxResult getSummaryVo(@RequestBody Cd90ScheduleResult scheduleResult);
}
