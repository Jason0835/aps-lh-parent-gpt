package com.zlt.aps.tc.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tc.api.domain.entity.TcDayFinishQty;
import com.zlt.aps.tc.api.domain.entity.TcScheduleResult;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 胎侧排程结果Service接口
 * @author zlt
 * @date 2021-06-21
 */
@FeignClient(contextId = "ITcScheduleResultService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tc:tc}")
public interface ITcScheduleResultService {


    /**
     * 查询胎侧排程结果列表
     */
    @PostMapping("/tcScheduleResult/list")
    TableDataInfo list(@RequestBody TcScheduleResult tcScheduleResult);


    /**
    * 新增胎侧排程结果
    */
    @PostMapping("/tcScheduleResult")
    AjaxResult add(@RequestBody TcScheduleResult tcScheduleResult);


    /**
     * 修改胎侧排程结果
     */
    @PostMapping("/tcScheduleResult/edit")
    AjaxResult edit(@RequestBody TcScheduleResult tcScheduleResult);

    @PostMapping("/tcScheduleResult/changeQty")
    public AjaxResult changeQty(@RequestBody TcScheduleResult tcScheduleResult);

    @PostMapping("/tcScheduleResult/changeMachine")
    public AjaxResult changeMachine(@RequestBody TcScheduleResult tcScheduleResult);

    /**
     * 删除胎侧排程结果
     */
    @DeleteMapping("/tcScheduleResult/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);


    /**
     * 根据ID获取详细信息
     */
    @GetMapping(value = "/tcScheduleResult/{id}")
    TcScheduleResult getInfo(@PathVariable("id") Long id);


    /**
     * 获取排程结果
     */
    @PostMapping("/tcScheduleResult/getList")
    List<TcScheduleResult> getList(@RequestBody TcScheduleResult tcScheduleResult);


    /**
     * 导出列表
     */
    @PostMapping("/tcScheduleResult/export")
    byte[] export(@RequestBody TcScheduleResult tcScheduleResult);


    /**
     * 根据IDS获取详细信息
     */
    @PostMapping(value = "/tcScheduleResult/getInfos")
    List<TcScheduleResult> getInfos(@RequestBody TcScheduleResult scheduleResult);


    /**
     * 批量更新发布状态
     */
    @GetMapping("/tcScheduleResult/batchUpdate/{ids}")
    int batchUpdate(@PathVariable("ids") long[] ids);

    /**
     * 自动排程
     * @param tcScheduleResult
     * @return
     */
    @PostMapping("/tcScheduleResult/autoPlan")
    AjaxResult autoPlan(@RequestBody TcScheduleResult tcScheduleResult);

    /**
     * 自动排程
     * @param tcScheduleResult
     * @return
     */
    @PostMapping("/tcScheduleResult/publish")
    AjaxResult publish(@RequestBody TcScheduleResult tcScheduleResult);

    /**
     * 查询排程日期是否已发布
     * @param scheduleDate 排程日期
     * @return 是否已经发布
     */
    @PostMapping("/tcScheduleResult/isPublish")
    Boolean isPublish(@RequestBody TcScheduleResult entity);

    /**
     * 唯一性校验
     */
    @PostMapping("/tcScheduleResult/checkUnique")
    List<TcScheduleResult> checkUnique(@RequestBody TcScheduleResult entity);

    /**
     * 数据导入
     */
    @PostMapping("/tcScheduleResult/importData")
    public AjaxResult importData(@RequestBody List<TcScheduleResult> list, @RequestParam("importLogId") Long importLogId,@RequestParam("scheduleDate")String scheduleDate);

    /**
     * 选机台
     */
    @PostMapping("/tcScheduleResult/chooseMachine")
    public AjaxResult chooseMachine(@RequestBody TcScheduleResult scheduleResult);

    /**
     * 均衡
     */
    @PostMapping("/tcScheduleResult/balance")
    public AjaxResult balance(@RequestBody TcScheduleResult entity);

    /**
     * 同胶料归并生产
     */
    @PostMapping("/tcScheduleResult/mergeProduct")
    public AjaxResult mergeProduct(@RequestBody TcScheduleResult tcScheduleResult);


    /**
     * 根据排程日期查询当前日期发布状态为"发布中"或"超时失败"的记录
     * @param scheduleDate 排程日期
     * @return 查询到的记录数
     */
    @PostMapping("/tcScheduleResult/isReleasingOrTimeoutByDate")
    public int isReleasingOrTimeoutByDate(@RequestBody TcScheduleResult scheduleResult);

    /**
     * 更改发布状态
     * @param scheduleDate 排程日期
     * @return 结果
     */
    @PostMapping("/tcScheduleResult/changeReleaseStatus")
    public AjaxResult changeReleaseStatus(@RequestBody TcScheduleResult entity);

    /**
     * 归并中夜班计划量，合并到同一个班次
     *
     * @param ids             id
     * @param classifiedShift 合并班次
     */
    @PostMapping("/tcScheduleResult/combinationMiddleAndNight/{ids}")
    public AjaxResult combinationMiddleAndNight(@PathVariable("ids")Long[] ids, @RequestParam("classifiedShift") String classifiedShift);

    /**
     * 导入完成量
     * @param list 完成量集合
     * @param importLogId 导入记录id
     * @return 结果
     */
    @PostMapping("/tcScheduleResult/importFinishQty")
    @ApiOperation("导入完成量")
    AjaxResult importFinishQty(@RequestBody List<TcDayFinishQty> list, @RequestParam("importLogId") Long importLogId);

    /**
     * 获取排程日期的昨日早班合计，夜班合计，早班合计，库存合计，理论交班库存合计
     *
     * @param scheduleResult 排程日期
     * @return 结果
     */
    @PostMapping("/tcScheduleResult/getSummaryVo")
    @ApiOperation("获取排程日期的排程结果合计")
    public AjaxResult getSummaryVo(@RequestBody TcScheduleResult scheduleResult);
}
