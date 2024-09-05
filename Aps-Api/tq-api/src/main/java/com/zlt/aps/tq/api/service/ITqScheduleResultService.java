package com.zlt.aps.tq.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tq.api.domain.dto.TqScheduleResultDto;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 胎圈排程结果Service接口
 * @author chen
 * @date 2021-06-21
 */
@FeignClient(contextId = "ITqScheduleResultService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tq:tq}")
public interface ITqScheduleResultService {
    
    /**
     * 查询胎圈排程结果列表
     *
     * @param dto 胎圈排程结果
     * @return 胎圈排程结果集合
     */
    @PostMapping("/tq/scheduleResult/list")
    @ApiOperation("查询胎圈排程结果信息维护列表")
    public TableDataInfo list(@RequestBody TqScheduleResultDto dto);

    /**
     * 查询胎圈排程结果
     *
     * @param id 胎圈排程结果ID
     * @return 胎圈排程结果
     */
    @GetMapping("/tq/scheduleResult/{id}")
    @ApiOperation("查询胎圈排程结果信息维护列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public TqScheduleResultDto getInfo(@PathVariable("id") Long id);

    /**
     * 修改胎圈排程结果
     *
     * @param dto 胎圈排程结果
     * @return 结果
     */
    @PostMapping("/tq/scheduleResult/edit")
    @ApiOperation("修改胎圈排程结果（id为空则新增，id不为空则修改）")
    public AjaxResult edit(@RequestBody TqScheduleResultDto dto);

    /**
     * 转机台
     *
     * @param dto 胎圈排程结果
     * @return 结果
     */
    @PostMapping("/tq/scheduleResult/changeMachine")
    @ApiOperation("转机台")
    public AjaxResult changeMachine(@RequestBody TqScheduleResultDto dto);

    /**
     * 调量
     *
     * @param dto 胎圈排程结果
     * @return 结果
     */
    @PostMapping("/tq/scheduleResult/changeQty")
    @ApiOperation("调量")
    public AjaxResult changeQty(@RequestBody TqScheduleResultDto dto);


    /**
     * 根据IDS获取详细信息
     */
    @PostMapping(value = "/tq/scheduleResult/getInfos")
    List<TqScheduleResultDto> getInfos(@RequestBody TqScheduleResultDto scheduleResult);

    /**
     * 删除胎圈排程结果
     *
     * @param ids 需要删除的胎圈排程结果ID
     * @return 结果
     */
    @PostMapping("/tq/scheduleResult/{ids}")
    @ApiOperation("删除胎圈排程结果信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id数组", paramType = "query")
    })
    public AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 导出胎圈排程结果信息
     */
    @PostMapping("/tq/scheduleResult/export")
    @ApiOperation("导出胎圈排程结果信息")
    public byte[] exportData(@RequestBody TqScheduleResultDto dto);

    /**
     * 查询胎圈排程结果列表
     *
     * @param dto 胎圈排程结果
     * @return 胎圈排程结果集合
     */
    @PostMapping("/tq/scheduleResult/getList")
    @ApiOperation("查询胎圈排程结果信息维护列表")
    public List<TqScheduleResultDto> getList(@RequestBody TqScheduleResultDto dto);

    /**
     * 发布所有排程结果
     * @param dto 查询条件
     */
    @PostMapping("/tq/scheduleResult/publish")
    @ApiOperation("发布所有排程结果")
    public AjaxResult publish(@RequestBody TqScheduleResultDto dto);

    /**
     * 自动排程
     */
    @PostMapping("/tq/scheduleResult/autoPlan")
    public AjaxResult autoPlan(@RequestBody TqScheduleResultDto dto);

    /**
     * 查询排程日期是否已发布
     * @param scheduleDate 排程日期
     * @return 是否已经发布
     */
    @PostMapping("/tq/scheduleResult/isPublish")
    Boolean isPublish(@RequestBody TqScheduleResultDto dto);

    /**
     * 根据排程日期、物料编号、机台id校验唯一性
     * @param scheduleResult 要校验记录
     * @return 查询到的记录数
     */
    @PostMapping("/tq/scheduleResult/checkUnique")
    public Boolean checkUnique(@RequestBody TqScheduleResultDto dto);

    @PostMapping("/tq/scheduleResult/importData")
    @ApiOperation("导入胎圈排程结果信息")
    public AjaxResult importData(@RequestBody List<TqScheduleResultDto> list, @RequestParam("importLogId") Long importLogId, @RequestParam("scheduleDate")String scheduleDate);

    /**
     * 选机台
     */
    @PostMapping("/tq/scheduleResult/chooseMachine")
    public AjaxResult chooseMachine(@RequestBody TqScheduleResultDto scheduleResult);

    /**
     * 根据排程日期查询当前日期发布状态为"发布中"或"超时失败"的记录
     * @param scheduleDate 排程日期
     * @return 查询到的记录数
     */
    @PostMapping("/tq/scheduleResult/isReleasingOrTimeoutByDate")
    public int isReleasingOrTimeoutByDate(@RequestBody TqScheduleResultDto scheduleResult);

    /**
     * 更改发布状态
     * @param scheduleDate 排程日期
     * @return 结果
     */
    @PostMapping("/tq/scheduleResult/changeReleaseStatus")
    public AjaxResult changeReleaseStatus(@RequestBody TqScheduleResultDto entity);
}
