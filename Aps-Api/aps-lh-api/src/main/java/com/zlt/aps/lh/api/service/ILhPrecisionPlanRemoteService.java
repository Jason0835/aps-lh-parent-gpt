package com.zlt.aps.lh.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.lh.api.domain.entity.LhPrecisionPlan;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 硫化精度计划远程服务接口
 *
 * @author APS Team
 */
@FeignClient(contextId = "ILhPrecisionPlanRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.lh:/lh}")
public interface ILhPrecisionPlanRemoteService {

    /**
     * 查询硫化精度计划列表
     */
    @ApiOperation("查询硫化精度计划列表")
    @PostMapping("/lhPrecisionPlan/list")
    TableDataInfo listLhPrecisionPlan(@RequestBody LhPrecisionPlan lhPrecisionPlan);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/lhPrecisionPlan/{id}")
    LhPrecisionPlan getLhPrecisionPlanInfo(@PathVariable("id") Long id);

    /**
     * 保存硫化精度计划信息（id为空则新增，id不为空则修改）
     */
    @ApiOperation("保存硫化精度计划信息")
    @PostMapping("/lhPrecisionPlan/save")
    AjaxResult saveLhPrecisionPlan(@RequestBody LhPrecisionPlan lhPrecisionPlan);

    /**
     * 批量删除硫化精度计划
     */
    @ApiOperation("批量删除硫化精度计划")
    @PostMapping("/lhPrecisionPlan/delete/{ids}")
    AjaxResult deleteLhPrecisionPlan(@PathVariable("ids") Long[] ids);

    /**
     * 导出硫化精度计划列表
     */
    @ApiOperation("导出硫化精度计划列表")
    @PostMapping("/lhPrecisionPlan/exportData")
    List<LhPrecisionPlan> exportData(@RequestBody LhPrecisionPlan lhPrecisionPlan);

    /**
     * 导入硫化精度计划数据
     */
    @ApiOperation("导入硫化精度计划数据")
    @PostMapping("/lhPrecisionPlan/importDataFeign")
    AjaxResult importData(@RequestBody List<LhPrecisionPlan> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

    /**
     * 从MES同步数据生成硫化精度初版计划
     *
     * @param year 年份
     * @return 生成数量
     */
    @ApiOperation("从MES同步数据生成硫化精度初版计划")
    @PostMapping("/lhPrecisionPlan/generateFromMes")
    AjaxResult generatePlansFromMes(@RequestParam("year") Integer year);

    /**
     * 自动生成年度硫化精度计划
     *
     * @param year 年份
     * @return 生成数量
     */
    @ApiOperation("自动生成年度硫化精度计划")
    @PostMapping("/lhPrecisionPlan/autoGenerateYearly")
    AjaxResult autoGenerateYearlyPlans(@RequestParam("year") Integer year);

    /**
     * 执行30天预警检查
     *
     * @return 预警数量
     */
    @ApiOperation("执行30天预警检查")
    @PostMapping("/lhPrecisionPlan/checkWarning")
    AjaxResult checkWarning();

    /**
     * 批量更新到期天数
     *
     * @return 更新数量
     */
    @ApiOperation("批量更新到期天数")
    @PostMapping("/lhPrecisionPlan/batchUpdateDaysToDue")
    AjaxResult batchUpdateDaysToDue();

    /**
     * MES回传实际完成时间
     *
     * @param mesSourceId MES来源ID
     * @param actualDate 实际日期
     * @return 是否成功
     */
    @ApiOperation("MES回传实际完成时间")
    @PostMapping("/lhPrecisionPlan/updateActualDate")
    AjaxResult updateActualDate(@RequestParam("mesSourceId") Long mesSourceId, 
                                @RequestParam("actualDate") String actualDate);
}
