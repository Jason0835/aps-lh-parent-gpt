package com.zlt.aps.cx.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cx.api.domain.entity.CxMachineInfo;
import com.zlt.aps.cx.api.domain.entity.CxPrecisionPlan;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(contextId = "ICxPrecisionPlanRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cx:/cx}")
public interface ICxPrecisionPlanRemoteService {

    @ApiOperation("查询列表")
    @PostMapping("/cxPrecisionPlan/list")
    TableDataInfo list(@RequestBody CxPrecisionPlan queryVO);

    @ApiOperation("保存")
    @PostMapping("/cxPrecisionPlan/save")
    AjaxResult save(@RequestBody CxPrecisionPlan entity);

    @ApiOperation("删除")
    @DeleteMapping("/cxPrecisionPlan/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/cxPrecisionPlan/{id}")
    CxPrecisionPlan getInfo(@PathVariable("id") Long id);

    @ApiOperation("校验唯一性")
    @PostMapping("/cxPrecisionPlan/checkUnique")
    String checkUnique(@RequestBody CxPrecisionPlan entity);

    @ApiOperation("导出列表")
    @PostMapping("/cxPrecisionPlan/exportData/{fileName}")
    byte[] exportData(@RequestBody CxPrecisionPlan queryVO, @PathVariable("fileName") String fileName);

    @ApiOperation("导入数据")
    @PostMapping("/cxPrecisionPlan/importData")
    AjaxResult importData(@RequestBody ImportContext context, @RequestParam("updateSupport") boolean updateSupport);

    @ApiOperation("从MES同步数据生成成型精度初版计划")
    @PostMapping("/cxPrecisionPlan/generateFromMes")
    AjaxResult generatePlansFromMes(@RequestParam(value = "year", required = false) Integer year);

    @ApiOperation("自动生成年度成型精度计划")
    @PostMapping("/cxPrecisionPlan/autoGenerateYearly")
    AjaxResult autoGenerateYearlyPlans(@RequestParam("year") Integer year);

    @ApiOperation("执行30天预警检查")
    @PostMapping("/cxPrecisionPlan/checkWarning")
    AjaxResult checkWarning();

    @ApiOperation("批量更新到期天数")
    @PostMapping("/cxPrecisionPlan/batchUpdateDaysToDue")
    AjaxResult batchUpdateDaysToDue();

    @ApiOperation("MES回传实际完成时间")
    @PostMapping("/cxPrecisionPlan/updateActualDate")
    AjaxResult updateActualDate(@RequestParam("mesSourceId") Long mesSourceId,
                                @RequestParam("actualDate") String actualDate);

    /**
     * 自动推算成型精度计划（15天周期）
     *
     * @param year 年度
     * @return 推算结果
     */
    @ApiOperation("自动推算成型精度计划（15天周期）")
    @PostMapping("/cxPrecisionPlan/autoCalculateCx15Days")
    AjaxResult autoCalculateCxPrecisionPlan15Days(@RequestParam("year") Integer year);

    /**
     * 自动推算成型精度计划（60天周期）
     *
     * @param year 年度
     * @return 推算结果
     */
    @ApiOperation("自动推算成型精度计划（60天周期）")
    @PostMapping("/cxPrecisionPlan/autoCalculateCx60Days")
    AjaxResult autoCalculateCxPrecisionPlan60Days(@RequestParam("year") Integer year);

    /**
     * 根据设备保养计划生成并推算成型精度计划
     *
     * @param maintenancePlanIds 设备保养计划ID列表
     * @param cycleDays 周期天数（15/60）
     * @return 生成结果
     */
    @ApiOperation("根据设备保养计划生成并推算成型精度计划")
    @PostMapping("/cxPrecisionPlan/generateFromMaintenance")
    AjaxResult generateFromMaintenancePlan(@RequestBody List<Long> maintenancePlanIds,
                                           @RequestParam("cycleDays") Integer cycleDays);

    /**
     * 按设备保养计划(MES同步数据)分发写入成型精度计划表
     * 现逻辑：MES全权决定计划时间和实际完成时间，APS侧不再回填实际日期、不再生成下一次精度计划。
     * 本方法根据MES字段值直接计算派生字段并upsert到T_CX_PRECISION_PLAN。
     * 精度类型映射：MES "成型精度15天"→APS PRECISION_TYPE='成型精度', PRECISION_CYCLE='15'
     *
     * @param maintenancePlanIds 设备保养计划ID列表（处理PRECISION_TYPE以'成型精度'开头的数据）
     * @return 分发写入的记录数
     */
    @ApiOperation("按设备保养计划分发写入成型精度计划表")
    @PostMapping("/cxPrecisionPlan/dispatchFromMaintenance")
    AjaxResult dispatchFromMaintenancePlan(@RequestBody List<Long> maintenancePlanIds);
}
