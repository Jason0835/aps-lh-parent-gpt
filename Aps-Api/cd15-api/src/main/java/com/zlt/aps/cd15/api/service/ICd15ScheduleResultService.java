package com.zlt.aps.cd15.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;


/**
 * 15度裁断排程结果Service接口
 * @author zlt
 * @date 2021-07-05
 */
@FeignClient(contextId = "ICd15ScheduleResultService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cd15:cd15}")
public interface ICd15ScheduleResultService {


    /**
     * 查询15度裁断排程结果列表
     */
    @PostMapping("/cd15ScheduleResult/list")
    TableDataInfo list(@RequestBody Cd15ScheduleResult cd15ScheduleResult);


    /**
    * 新增15度裁断排程结果
    */
    @PostMapping("/cd15ScheduleResult/add")
    AjaxResult add(@RequestBody Cd15ScheduleResult cd15ScheduleResult);


    /**
     * 修改15度裁断排程结果
     */
    @PostMapping("/cd15ScheduleResult/edit")
    AjaxResult edit(@RequestBody Cd15ScheduleResult cd15ScheduleResult);

    /**
     * 调量
     */
    @PostMapping("/cd15ScheduleResult/changeQty")
    public AjaxResult changeQty(@RequestBody Cd15ScheduleResult scheduleResult);

    /**
     * 转机台
     */
    @PostMapping("/cd15ScheduleResult/changeMachine")
    public AjaxResult changeMachine(@RequestBody Cd15ScheduleResult scheduleResult);

    /**
     * 选机台
     */
    @PostMapping("/cd15ScheduleResult/chooseMachine")
    public AjaxResult chooseMachine(@RequestBody Cd15ScheduleResult scheduleResult);

    /**
     * 删除15度裁断排程结果
     */
    @DeleteMapping("/cd15ScheduleResult/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);


    /**
     * 根据ID获取详细信息
     */
    @GetMapping(value = "/cd15ScheduleResult/{id}")
    Cd15ScheduleResult getInfo(@PathVariable("id") Long id);

    /**
     * 根据IDS获取详细信息
     */
    @PostMapping(value = "/cd15ScheduleResult/getInfos")
    List<Cd15ScheduleResult> getInfos(@RequestBody Cd15ScheduleResult scheduleResult);


    /**
     * 校验15度裁断排程结果唯一性
     */
    @PostMapping("/cd15ScheduleResult/checkScheduleResultUnique")
    List<Cd15ScheduleResult> checkScheduleResultUnique(@RequestBody Cd15ScheduleResult cd15ScheduleResult);


    /**
     * 15度裁断排程结果列表
     */
    @PostMapping("/cd15ScheduleResult/getList")
    List<Cd15ScheduleResult> getList(@RequestBody Cd15ScheduleResult cd15ScheduleResult);

    /**
     * 导出列表
     */
    @PostMapping("/cd15ScheduleResult/export")
    byte[] export(@RequestBody Cd15ScheduleResult cd15ScheduleResult);


    /**
     * 批量更新发布状态
     */
    @GetMapping("/cd15ScheduleResult/batchUpdate/{ids}")
    int batchUpdate(@PathVariable("ids") long[] ids, Date scheduleDate);

    /**
     * 自动排程
     */
    @PostMapping("/cd15ScheduleResult/autoPlan")
    AjaxResult autoPlan(@RequestBody Cd15ScheduleResult cd15ScheduleResult);

    /**
     * 自动排程
     */
    @PostMapping("/cd15ScheduleResult/publish")
    AjaxResult publish(@RequestBody Cd15ScheduleResult cd15ScheduleResult);

    /**
     * 查询排程日期是否已发布
     * @param scheduleDate 排程日期
     * @return 是否已经发布
     */
    @PostMapping("/cd15ScheduleResult/isPublish")
    Boolean isPublish(@RequestBody Cd15ScheduleResult entity);

    /**
     * 导入数据
     */
    @PostMapping("/cd15ScheduleResult/importData")
    public AjaxResult importData(@RequestBody List<Cd15ScheduleResult> list, @RequestParam("importLogId") Long importLogId,@RequestParam("scheduleDate")String scheduleDate);

    /**
     * 均衡
     */
    @PostMapping("/cd15ScheduleResult/balance")
    public AjaxResult balance(@RequestBody Cd15ScheduleResult entity);

    /**
     * 根据排程日期查询当前日期发布状态为"发布中"或"超时失败"的记录
     * @param scheduleDate 排程日期
     * @return 查询到的记录数
     */
    @PostMapping("/cd15ScheduleResult/isReleasingOrTimeoutByDate")
    public int isReleasingOrTimeoutByDate(@RequestBody Cd15ScheduleResult entity);

    /**
     * 更改发布状态
     * @param scheduleDate 排程日期
     * @return 结果
     */
    @PostMapping("/cd15ScheduleResult/changeReleaseStatus")
    public AjaxResult changeReleaseStatus(@RequestBody Cd15ScheduleResult entity);

    /**
     * 归并中夜班计划量，合并到同一个班次
     *
     * @param ids             id
     * @param classifiedShift 合并班次
     */
    @PostMapping("/cd15ScheduleResult/combinationMiddleAndNight/{ids}")
    public AjaxResult combinationMiddleAndNight(@PathVariable("ids")Long[] ids, @RequestParam("classifiedShift") String classifiedShift);
}
