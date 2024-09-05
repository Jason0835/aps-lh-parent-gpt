package com.zlt.aps.nc.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.nc.api.domain.entity.NcScheduleResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 内衬胶排程结果Service接口
 *
 * @author zlt
 * @date 2021-06-24
 */
@FeignClient(contextId = "INcScheduleResultService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.nc:nc}")
public interface INcScheduleResultService {

    /**
     * 查询内衬排程结果列表
     */
    @PostMapping("/ncScheduleResult/list")
    TableDataInfo list(@RequestBody NcScheduleResult ncScheduleResult);


    /**
     * 新增内衬排程结果
     */
    @PostMapping("/ncScheduleResult")
    AjaxResult add(@RequestBody NcScheduleResult ncScheduleResult);


    /**
     * 修改内衬排程结果
     */
    @PostMapping("/ncScheduleResult/edit")
    AjaxResult edit(@RequestBody NcScheduleResult ncScheduleResult);

    /**
     * 调量
     */
    @PostMapping("/ncScheduleResult/changeQty")
    public AjaxResult changeQty(@RequestBody NcScheduleResult scheduleResult);

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
    List<NcScheduleResult> getList(@RequestBody NcScheduleResult ncScheduleResult);

    /**
     * 唯一性校验
     */
    @PostMapping("/ncScheduleResult/checkUnique")
    List<NcScheduleResult> checkUnique(@RequestBody NcScheduleResult entity);

    /**
     * 导出列表
     */
    @PostMapping("/ncScheduleResult/export")
    byte[] export(@RequestBody NcScheduleResult ncScheduleResult);

    /**
     * 自动排程
     * @param ncScheduleResult
     * @return
     */
    @PostMapping("/ncScheduleResult/autoPlan")
    AjaxResult autoPlan(@RequestBody NcScheduleResult ncScheduleResult);

    /**
     * 自动排程
     * @param ncScheduleResult
     * @return
     */
    @PostMapping("/ncScheduleResult/publish")
    AjaxResult publish(@RequestBody NcScheduleResult ncScheduleResult);

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

}
