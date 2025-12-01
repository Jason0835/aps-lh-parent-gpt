package com.zlt.aps.xwyy.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.xwyy.api.domain.dto.XwyyScheduleResultDto;
import com.zlt.aps.xwyy.api.domain.entity.XwyyDayFinishQty;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 纤维压延排程结果Service接口
 *
 * @author chen
 * @date 2021-07-06
 */
@FeignClient(contextId = "IXwyyScheduleResultService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.xwyy:xwyy}")
public interface IXwyyScheduleResultService {

    /**
     * 查询纤维压延排程结果列表
     *
     * @param dto 纤维压延排程结果
     * @return 纤维压延排程结果集合
     */
    @PostMapping("/xwyy/scheduleResult/list")
    @ApiOperation("查询纤维压延排程结果信息维护列表")
    public TableDataInfo list(@RequestBody XwyyScheduleResultDto dto);

    /**
     * 查询纤维压延排程结果
     *
     * @param id 纤维压延排程结果ID
     * @return 纤维压延排程结果
     */
    @GetMapping("/xwyy/scheduleResult/{id}")
    @ApiOperation("查询纤维压延排程结果信息维护列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public XwyyScheduleResultDto getInfo(@PathVariable("id") Long id);

    /**
     * 修改纤维压延排程结果
     *
     * @param dto 纤维压延排程结果
     * @return 结果
     */
    @PostMapping("/xwyy/scheduleResult/edit")
    @ApiOperation("修改纤维压延排程结果（id为空则新增，id不为空则修改）")
    public AjaxResult edit(@RequestBody XwyyScheduleResultDto dto);

    /**
     * 转机台
     *
     * @param dto 胎圈排程结果
     * @return 结果
     */
    @PostMapping("/xwyy/scheduleResult/changeMachine")
    @ApiOperation("转机台")
    public AjaxResult changeMachine(@RequestBody XwyyScheduleResultDto dto);


    /**
     * 根据IDS获取详细信息
     */
    @PostMapping(value = "/xwyy/scheduleResult/getInfos")
    List<XwyyScheduleResultDto> getInfos(@RequestBody XwyyScheduleResultDto scheduleResult);

    /**
     * 调量
     *
     * @param dto 胎圈排程结果
     * @return 结果
     */
    @PostMapping("/xwyy/scheduleResult/changeQty")
    @ApiOperation("调量")
    public AjaxResult changeQty(@RequestBody XwyyScheduleResultDto dto);

    /**
     * 选机台
     */
    @PostMapping("/xwyy/scheduleResult/chooseMachine")
    public AjaxResult chooseMachine(@RequestBody XwyyScheduleResultDto dto);

    /**
     * 删除纤维压延排程结果
     *
     * @param ids 需要删除的纤维压延排程结果ID
     * @return 结果
     */
    @PostMapping("/xwyy/scheduleResult/remove")
    @ApiOperation("删除纤维压延排程结果信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id数组", paramType = "query")
    })
    public AjaxResult remove(@RequestBody List<XwyyScheduleResultDto> removeList);

    /**
     * 导出纤维压延排程结果信息
     */
    @PostMapping("/xwyy/scheduleResult/export")
    @ApiOperation("导出纤维压延排程结果信息")
    public byte[] exportData(@RequestBody XwyyScheduleResultDto dto);

    /**
     * 查询纤维压延排程结果列表
     *
     * @param dto 纤维压延排程结果
     * @return 纤维压延排程结果集合
     */
    @PostMapping("/xwyy/scheduleResult/getList")
    @ApiOperation("查询纤维压延排程结果信息维护列表")
    public List<XwyyScheduleResultDto> getList(@RequestBody XwyyScheduleResultDto dto);

    /**
     * 发布所有排程结果
     *
     * @param dto 查询条件
     */
    @PostMapping("/xwyy/scheduleResult/publish")
    @ApiOperation("发布所有排程结果")
    public AjaxResult publish(@RequestBody XwyyScheduleResultDto dto);

    /**
     * 自动排程
     */
    @PostMapping("/xwyy/scheduleResult/autoPlan")
    public AjaxResult autoPlan(@RequestBody XwyyScheduleResultDto dto);

    /**
     * 查询排程日期是否已发布
     *
     * @param scheduleDate 排程日期
     * @return 是否已经发布
     */
    @PostMapping("/xwyy/scheduleResult/isPublish")
    Boolean isPublish(@RequestBody XwyyScheduleResultDto dto);

    /**
     * 查询排程日期是否已发布
     *
     * @param scheduleDate 排程日期
     * @return 是否已经发布
     */
    @PostMapping("/xwyy/scheduleResult/checkUnique")
    public Boolean checkUnique(@RequestBody XwyyScheduleResultDto dto);

    @PostMapping("/xwyy/scheduleResult/importData")
    @ApiOperation("导入纤维压延排程结果信息")
    public AjaxResult importData(@RequestBody List<XwyyScheduleResultDto> list, @RequestParam("importLogId") Long importLogId, @RequestParam("scheduleDate")String scheduleDate);


    /**
     * 根据排程日期查询当前日期发布状态为"发布中"或"超时失败"的记录
     * @param scheduleDate 排程日期
     * @return 查询到的记录数
     */
    @PostMapping("/xwyy/scheduleResult/isReleasingOrTimeoutByDate")
    public int isReleasingOrTimeoutByDate(@RequestBody XwyyScheduleResultDto entity);

    /**
     * 更改发布状态
     * @param scheduleDate 排程日期
     * @return 结果
     */
    @PostMapping("/xwyy/scheduleResult/changeReleaseStatus")
    public AjaxResult changeReleaseStatus(@RequestBody XwyyScheduleResultDto entity);

    /**
     * 根据帘布大卷代号获取帘线大卷标准长度
     * @param bigRollCode 帘布大卷代号
     * @return 帘线大卷标准长度
     */
    @PostMapping("/xwyy/scheduleResult/getActClothLength")
    public AjaxResult getActClothLength(@RequestBody String bigRollCode);

    /**
     * 归并中夜班计划量，合并到同一个班次
     *
     * @param ids             id
     * @param classifiedShift 合并班次
     */
    @PostMapping("/xwyy/scheduleResult/combinationMiddleAndNight/{ids}")
    AjaxResult combinationMiddleAndNight(@PathVariable("ids")Long[] ids, @RequestParam("classifiedShift") String classifiedShift);

    /**
     * 导入完成量
     * @param list 完成量集合
     * @param importLogId 导入记录id
     * @return 结果
     */
    @PostMapping("/xwyy/scheduleResult/importFinishQty")
    @ApiOperation("导入完成量")
    AjaxResult importFinishQty(@RequestBody List<XwyyDayFinishQty> list, @RequestParam("importLogId") Long importLogId);

    /**
     * 获取排程日期的昨日早班合计，夜班合计，早班合计，库存合计，理论交班库存合计
     *
     * @param scheduleResult 排程日期
     * @return 结果
     */
    @PostMapping("/xwyy/scheduleResult/getSummaryVo")
    @ApiOperation("获取排程日期的排程结果合计")
    public AjaxResult getSummaryVo(@RequestBody XwyyScheduleResultDto scheduleResult);

    /**
     * 导出线下计划导入列表
     *
     * @param importContext 上下文
     * @return 结果
     */
    @ApiOperation("导出线下计划导入列表")
    @PostMapping("/xwyy/scheduleResult/importExcelToListAndExport")
    byte[] importExcelToListAndExport(@RequestBody ImportContext importContext);

    /**
     * 将线下排程模板的昨日计划、昨日库存，导入到系统
     *
     * @param context       上下文
     * @param updateSupport 是否更新
     * @return 结果
     */
    @ApiOperation("将线下排程模板的昨日计划、昨日库存，导入到系统")
    @PostMapping("/xwyy/scheduleResult/importExcelToLastDayPlanAndStock")
    AjaxResult importExcelToLastDayPlanAndStock(@RequestBody ImportContext context, @RequestParam("updateSupport") boolean updateSupport);
}
