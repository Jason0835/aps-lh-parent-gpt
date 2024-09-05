package com.zlt.aps.gsq.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.gsq.api.domain.dto.GsqScheduleResultDto;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 钢丝圈排程结果Service接口
 * @author chen
 * @date 2021-06-21
 */
@FeignClient(contextId = "IGsqScheduleResultService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.gsq:gsq}")
public interface IGsqScheduleResultService {
    
    /**
     * 查询钢丝圈排程结果列表
     *
     * @param dto 钢丝圈排程结果
     * @return 钢丝圈排程结果集合
     */
    @PostMapping("/gsq/scheduleResult/list")
    @ApiOperation("查询钢丝圈排程结果信息维护列表")
    public TableDataInfo list(@RequestBody GsqScheduleResultDto dto);

    /**
     * 查询钢丝圈排程结果
     *
     * @param id 钢丝圈排程结果ID
     * @return 钢丝圈排程结果
     */
    @GetMapping("/gsq/scheduleResult/{id}")
    @ApiOperation("查询钢丝圈排程结果信息维护列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public GsqScheduleResultDto getInfo(@PathVariable("id") Long id);

    /**
     * 修改钢丝圈排程结果
     *
     * @param dto 钢丝圈排程结果
     * @return 结果
     */
    @PostMapping("/gsq/scheduleResult/edit")
    @ApiOperation("修改钢丝圈排程结果")
    public AjaxResult edit(@RequestBody GsqScheduleResultDto dto);

    /**
     * 插单
     *
     * @param dto 钢丝圈排程结果
     * @return 结果
     */
    @PostMapping("/gsq/scheduleResult/add")
    @ApiOperation("插单")
    public AjaxResult add(@RequestBody GsqScheduleResultDto dto);

    /**
     * 调量
     *
     * @param dto 钢丝圈排程结果
     * @return 结果
     */
    @PostMapping("/gsq/scheduleResult/changeQty")
    @ApiOperation("调量")
    public AjaxResult changeQty(@RequestBody GsqScheduleResultDto dto);

    /**
     * 转机台
     *
     * @param dto 钢丝圈排程结果
     * @return 结果
     */
    @PostMapping("/gsq/scheduleResult/changeMachine")
    @ApiOperation("转机台")
    public AjaxResult changeMachine(@RequestBody GsqScheduleResultDto dto);

    /**
     * 选机台
     */
    @PostMapping("/gsq/scheduleResult/chooseMachine")
    public AjaxResult chooseMachine(@RequestBody GsqScheduleResultDto dto);

    /**
     * 删除钢丝圈排程结果
     *
     * @param ids 需要删除的钢丝圈排程结果ID
     * @return 结果
     */
    @PostMapping("/gsq/scheduleResult/{ids}")
    @ApiOperation("删除钢丝圈排程结果信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id数组", paramType = "query")
    })
    public AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 导出钢丝圈排程结果信息
     */
    @PostMapping("/gsq/scheduleResult/export")
    @ApiOperation("导出钢丝圈排程结果信息")
    public byte[] exportData(@RequestBody GsqScheduleResultDto dto);

    /**
     * 查询钢丝圈排程结果列表
     *
     * @param dto 钢丝圈排程结果
     * @return 钢丝圈排程结果集合
     */
    @PostMapping("/gsq/scheduleResult/getList")
    @ApiOperation("查询钢丝圈排程结果信息维护列表")
    public List<GsqScheduleResultDto> getList(@RequestBody GsqScheduleResultDto dto);

    /**
     * 发布所有排程结果
     * @param dto 查询条件
     */
    @PostMapping("/gsq/scheduleResult/publish")
    public AjaxResult publish(@RequestBody GsqScheduleResultDto dto);

    /**
     * 自动排程
     */
    @PostMapping("/gsq/scheduleResult/autoPlan")
    public AjaxResult autoPlan(@RequestBody GsqScheduleResultDto dto);

    /**
     * 根据IDS获取详细信息
     */
    @PostMapping(value = "/gsq/scheduleResult/getInfos")
    List<GsqScheduleResultDto> getInfos(@RequestBody GsqScheduleResultDto scheduleResult);

    /**
     * 查询排程日期是否已发布
     * @param scheduleDate 排程日期
     * @return 是否已经发布
     */
    @PostMapping("/gsq/scheduleResult/isPublish")
    Boolean isPublish(@RequestBody GsqScheduleResultDto dto);

    /**
     * 根据排程日期、物料编号、机台id校验唯一性
     * @param scheduleResult 要校验记录
     * @return 查询到的记录数
     */
    @PostMapping("/gsq/scheduleResult/checkUnique")
    public Boolean checkUnique(@RequestBody GsqScheduleResultDto dto);

    @PostMapping("/gsq/scheduleResult/importData")
    @ApiOperation("导入钢丝圈排程结果信息")
    public AjaxResult importData(@RequestBody List<GsqScheduleResultDto> list, @RequestParam("importLogId") Long importLogId, @RequestParam("scheduleDate") String scheduleDate);

    /**
     * 根据排程日期查询当前日期发布状态为"发布中"或"超时失败"的记录
     * @param scheduleDate 排程日期
     * @return 查询到的记录数
     */
    @PostMapping("/gsq/scheduleResult/isReleasingOrTimeoutByDate")
    public int isReleasingOrTimeoutByDate(@RequestBody GsqScheduleResultDto scheduleResult);

    /**
     * 更改发布状态
     * @param scheduleDate 排程日期
     * @return 结果
     */
    @PostMapping("/gsq/scheduleResult/changeReleaseStatus")
    public AjaxResult changeReleaseStatus(@RequestBody GsqScheduleResultDto entity);
}
