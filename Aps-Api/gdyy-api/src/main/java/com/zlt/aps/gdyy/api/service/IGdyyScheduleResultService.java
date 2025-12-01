package com.zlt.aps.gdyy.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.gdyy.api.domain.dto.GdyyScheduleResultDto;
import com.zlt.aps.gdyy.api.domain.entity.GdyyDayFinishQty;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 钢带压延排程结果Service接口
 * @author chen
 * @date 2021-07-05
 */
@FeignClient(contextId = "IGdyyScheduleResultService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.gdyy:gdyy}")
public interface IGdyyScheduleResultService {
    
    /**
     * 查询钢带压延排程结果列表
     *
     * @param dto 钢带压延排程结果
     * @return 钢带压延排程结果集合
     */
    @PostMapping("/gdyy/scheduleResult/list")
    @ApiOperation("查询钢带压延排程结果信息维护列表")
    public TableDataInfo list(@RequestBody GdyyScheduleResultDto dto);

    /**
     * 查询钢带压延排程结果
     *
     * @param id 钢带压延排程结果ID
     * @return 钢带压延排程结果
     */
    @GetMapping("/gdyy/scheduleResult/{id}")
    @ApiOperation("查询钢带压延排程结果信息维护列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public GdyyScheduleResultDto getInfo(@PathVariable("id") Long id);

    /**
     * 修改钢带压延排程结果
     *
     * @param dto 钢带压延排程结果
     * @return 结果
     */
    @PostMapping("/gdyy/scheduleResult/edit")
    @ApiOperation("修改钢带压延排程结果（id为空则新增，id不为空则修改）")
    public AjaxResult edit(@RequestBody GdyyScheduleResultDto dto);

    /**
     * 调量
     *
     * @param dto 钢带压延排程结果
     * @return 结果
     */
    @PostMapping("/gdyy/scheduleResult/changeQty")
    @ApiOperation("调量")
    public AjaxResult changeQty(@RequestBody GdyyScheduleResultDto dto);

    /**
     * 转机台
     *
     * @param dto 胎圈排程结果
     * @return 结果
     */
    @PostMapping("/gdyy/scheduleResult/changeMachine")
    @ApiOperation("转机台")
    public AjaxResult changeMachine(@RequestBody GdyyScheduleResultDto dto);

    /**
     * 删除钢带压延排程结果
     *
     * @param ids 需要删除的钢带压延排程结果ID
     * @return 结果
     */
    @PostMapping("/gdyy/scheduleResult/{ids}")
    @ApiOperation("删除钢带压延排程结果信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id数组", paramType = "query")
    })
    public AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 导出钢带压延排程结果信息
     */
    @PostMapping("/gdyy/scheduleResult/export")
    @ApiOperation("导出钢带压延排程结果信息")
    public byte[] exportData(@RequestBody GdyyScheduleResultDto dto);

    /**
     * 查询钢带压延排程结果列表
     *
     * @param dto 钢带压延排程结果
     * @return 钢带压延排程结果集合
     */
    @PostMapping("/gdyy/scheduleResult/getList")
    @ApiOperation("查询钢带压延排程结果信息维护列表")
    public List<GdyyScheduleResultDto> getList(@RequestBody GdyyScheduleResultDto dto);

    /**
     * 发布所有排程结果
     * @param dto 查询条件
     */
    @PostMapping("/gdyy/scheduleResult/publish")
    @ApiOperation("发布所有排程结果")
    public AjaxResult publish(@RequestBody GdyyScheduleResultDto dto);

    /**
     * 自动排程
     */
    @PostMapping("/gdyy/scheduleResult/autoPlan")
    public AjaxResult autoPlan(@RequestBody GdyyScheduleResultDto dto);

    /**
     * 查询排程日期是否已发布
     * @param scheduleDate 排程日期
     * @return 是否已经发布
     */
    @PostMapping("/gdyy/scheduleResult/isPublish")
    Boolean isPublish(@RequestBody GdyyScheduleResultDto dto);

    /**
     * 根据排程日期、物料编号、机台id校验唯一性
     * @param scheduleResult 要校验记录
     * @return 查询到的记录数
     */
    @PostMapping("/gdyy/scheduleResult/checkUnique")
    public Boolean checkUnique(@RequestBody GdyyScheduleResultDto dto);

    @PostMapping("/gdyy/scheduleResult/importData")
    @ApiOperation("导入钢带压延排程结果信息")
    public AjaxResult importData(@RequestBody List<GdyyScheduleResultDto> list, @RequestParam("importLogId") Long importLogId, @RequestParam("scheduleDate")String scheduleDate);


    /**
     * 根据排程日期查询当前日期发布状态为"发布中"或"超时失败"的记录
     * @param scheduleDate 排程日期
     * @return 查询到的记录数
     */
    @PostMapping("/gdyy/scheduleResult/isReleasingOrTimeoutByDate")
    public int isReleasingOrTimeoutByDate(@RequestBody GdyyScheduleResultDto entity);

    /**
     * 更改发布状态
     * @param scheduleDate 排程日期
     * @return 结果
     */
    @PostMapping("/gdyy/scheduleResult/changeReleaseStatus")
    public AjaxResult changeReleaseStatus(@RequestBody GdyyScheduleResultDto entity);

    /**
     * 导入钢带压延完成量
     * @param list 完成量集合
     * @param importLogId 导入记录id
     * @return 结果
     */
    @PostMapping("/gdyy/scheduleResult/importFinishQty")
    @ApiOperation("导入钢带压延完成量")
    AjaxResult importFinishQty(@RequestBody List<GdyyDayFinishQty> list, @RequestParam("importLogId") Long importLogId);

    /**
     * 获取排程日期的昨日早班合计，夜班合计，早班合计，库存合计，理论交班库存合计
     *
     * @param scheduleResult 排程日期
     * @return 结果
     */
    @PostMapping("/gdyy/scheduleResult/getSummaryVo")
    @ApiOperation("获取排程日期的排程结果合计")
    public AjaxResult getSummaryVo(@RequestBody GdyyScheduleResultDto scheduleResult);
}
