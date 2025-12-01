package com.zlt.aps.tm.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tm.api.domain.entity.TmDayFinishQty;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;


/**
 * 胎面排程结果Service接口
 * @author zlt
 * @date 2021-06-17
 */
@FeignClient(contextId = "iTmScheduleResultService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tm:tm}")
public interface ITmScheduleResultService{


    /**
     * 查询胎面排程结果列表
     */
    @PostMapping("/tmScheduleResult/list")
    TableDataInfo list(@RequestBody TmScheduleResult tmScheduleResult);

    /**
    * 新增胎面排程结果
    */
    @PostMapping("/tmScheduleResult")
    AjaxResult add(@RequestBody TmScheduleResult tmScheduleResult);


    /**
     * 修改胎面排程结果
     */
    @PostMapping("/tmScheduleResult/edit")
    AjaxResult edit(@RequestBody TmScheduleResult tmScheduleResult);

    /**
     * 调量
     */
    @PostMapping("/tmScheduleResult/changeQty")
    public AjaxResult changeQty(@RequestBody TmScheduleResult tmScheduleResult);

    /**
     * 转机台
     */
    @PostMapping("/tmScheduleResult/changeMachine")
    public AjaxResult changeMachine(@RequestBody TmScheduleResult tmScheduleResult);

    /**
     * 选机台
     */
    @PostMapping("/tmScheduleResult/chooseMachine")
    public AjaxResult chooseMachine(@RequestBody TmScheduleResult tmScheduleResult);

    /**
     * 删除胎面排程结果
     */
    @DeleteMapping("/tmScheduleResult/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);


    /**
     * 根据ID获取详细信息
     */
    @GetMapping(value = "/tmScheduleResult/{id}")
    TmScheduleResult getInfo(@PathVariable("id") Long id);


    /**
     * 根据IDS获取详细信息
     */
    @PostMapping(value = "/tmScheduleResult/getInfos")
    List<TmScheduleResult> getInfos(@RequestBody TmScheduleResult scheduleResult);


    /**
     * 校验胎面排程结果唯一性
     */
    @PostMapping("/tmScheduleResult/checktmScheduleResultUnique")
    String checktmScheduleResultUnique(@RequestBody TmScheduleResult tmScheduleResult);


    /**
     * 导出列表
     */
    @PostMapping("/tmScheduleResult/export")
    byte[] export(@RequestBody TmScheduleResult tmScheduleResult);

    /**
     * 获取排程结果
     * @param tmScheduleResult
     * @return
     */
    @PostMapping("/tmScheduleResult/getlist")
    List<TmScheduleResult> getlist(@RequestBody TmScheduleResult tmScheduleResult);

    /**
     * 批量更新发布状态
     */
    @GetMapping("/tmScheduleResult/batchUpdate/{ids}")
    int batchUpdate(@PathVariable("ids") long[] ids,Date scheduleDate);

    /**
     * 自动排程
     * @param tmScheduleResult
     * @return
     */
    @PostMapping("/tmScheduleResult/autoPlan")
    AjaxResult autoPlan(@RequestBody TmScheduleResult tmScheduleResult);

    /**
     * 自动排程
     * @param tmScheduleResult
     * @return
     */
    @PostMapping("/tmScheduleResult/publish")
    AjaxResult publish(@RequestBody TmScheduleResult tmScheduleResult);

    /**
     * 查询排程日期是否已发布
     * @param scheduleDate 排程日期
     * @return 是否已经发布
     */
    @PostMapping("/tmScheduleResult/isPublish")
    Boolean isPublish(@RequestBody TmScheduleResult entity);

    /**
     * 唯一性校验
     */
    @PostMapping("/tmScheduleResult/checkUnique")
    List<TmScheduleResult> checkUnique(@RequestBody TmScheduleResult tmScheduleResult);

    @PostMapping("/tmScheduleResult/importData")
    public AjaxResult importData(@RequestBody List<TmScheduleResult> list, @RequestParam("importLogId") Long importLogId,@RequestParam("scheduleDate")String scheduleDate);

    /**
     * 均衡
     */
    @PostMapping("/tmScheduleResult/balance")
    public AjaxResult balance(@RequestBody TmScheduleResult tmScheduleResult);

    /**
     * 同胶料归并生产
     */
    @PostMapping("/tmScheduleResult/mergeProduct")
    public AjaxResult mergeProduct(@RequestBody TmScheduleResult tmScheduleResult);

    /**
     * 根据排程日期查询当前日期发布状态为"发布中"或"超时失败"的记录
     * @param scheduleDate 排程日期
     * @return 查询到的记录数
     */
    @PostMapping("/tmScheduleResult/isReleasingOrTimeoutByDate")
    public int isReleasingOrTimeoutByDate(@RequestBody TmScheduleResult tmScheduleResult);

    /**
     * 更改发布状态
     * @param scheduleDate 排程日期
     * @return 结果
     */
    @PostMapping("/tmScheduleResult/changeReleaseStatus")
    public AjaxResult changeReleaseStatus(@RequestBody TmScheduleResult entity);

    /**
     * 归并中夜班计划量，合并到同一个班次
     *
     * @param ids             id
     * @param classifiedShift 合并班次
     */
    @PostMapping("/tmScheduleResult/combinationMiddleAndNight/{ids}")
    public AjaxResult combinationMiddleAndNight(@PathVariable("ids")Long[] ids, @RequestParam("classifiedShift") String classifiedShift);

    /**
     * 导入完成量
     * @param list 完成量集合
     * @param importLogId 导入记录id
     * @return 结果
     */
    @PostMapping("/tmScheduleResult/importFinishQty")
    @ApiOperation("导入完成量")
    AjaxResult importFinishQty(@RequestBody List<TmDayFinishQty> list, @RequestParam("importLogId") Long importLogId);

    /**
     * 获取排程日期的昨日早班合计，夜班合计，早班合计，库存合计，理论交班库存合计
     *
     * @param tmScheduleResult 排程日期
     * @return 结果
     */
    @PostMapping("/tmScheduleResult/getSummaryVo")
    @ApiOperation("获取排程日期的排程结果合计")
    public AjaxResult getSummaryVo(@RequestBody TmScheduleResult tmScheduleResult);

    /**
     * 批量转机台
     *
     * @param scheduleResult 排程结果
     * @return 结果
     */
    @PostMapping("/tmScheduleResult/batchChangeMachine")
    @ApiOperation("批量转机台")
    AjaxResult batchChangeMachine(@RequestBody TmScheduleResult scheduleResult);
}
