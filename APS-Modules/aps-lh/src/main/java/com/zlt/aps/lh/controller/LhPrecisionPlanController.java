package com.zlt.aps.lh.controller;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.lh.api.domain.entity.LhPrecisionPlan;
import com.zlt.aps.lh.api.domain.vo.LhPrecisionPlanVo;
import com.zlt.aps.lh.service.ILhPrecisionPlanService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 硫化精度计划Controller
 *
 * @author APS Team
 */
@Slf4j
@Api(tags = "硫化精度计划管理")
@RestController
@RequestMapping("/lhPrecisionPlan")
public class LhPrecisionPlanController {

    @Autowired
    private ILhPrecisionPlanService lhPrecisionPlanService;

    /**
     * 查询硫化精度计划列表
     */
    @ApiOperation("查询硫化精度计划列表")
    @PostMapping("/list")
    public AjaxResult list(@RequestBody LhPrecisionPlanVo vo) {
        List<LhPrecisionPlan> list = lhPrecisionPlanService.selectLhPrecisionPlanList(vo);
        return AjaxResult.success(list);
    }

    /**
     * 从MES同步数据生成硫化精度初版计划
     */
    @ApiOperation("从MES同步数据生成硫化精度初版计划")
    @PostMapping("/generateFromMes")
    public AjaxResult generatePlansFromMes() {
        try {
            int count = lhPrecisionPlanService.generatePlansFromMes();
            return AjaxResult.success("生成成功", count);
        } catch (Exception e) {
            log.error("从MES同步数据生成硫化精度计划失败", e);
            return AjaxResult.error("生成失败：" + e.getMessage());
        }
    }

    /**
     * 自动生成年度硫化精度计划
     */
    @ApiOperation("自动生成年度硫化精度计划")
    @PostMapping("/autoGenerateYearly")
    public AjaxResult autoGenerateYearlyPlans(@RequestParam("year") Integer year) {
        try {
            int count = lhPrecisionPlanService.autoGenerateYearlyPlans(year);
            return AjaxResult.success("生成成功", count);
        } catch (Exception e) {
            log.error("自动生成{}年度硫化精度计划失败", year, e);
            return AjaxResult.error("生成失败：" + e.getMessage());
        }
    }

    /**
     * 执行30天预警检查
     */
    @ApiOperation("执行30天预警检查")
    @PostMapping("/checkWarning")
    public AjaxResult checkWarning() {
        try {
            int count = lhPrecisionPlanService.checkWarning();
            return AjaxResult.success("预警检查完成", count);
        } catch (Exception e) {
            log.error("执行30天预警检查失败", e);
            return AjaxResult.error("预警检查失败：" + e.getMessage());
        }
    }

    /**
     * 批量更新到期天数
     */
    @ApiOperation("批量更新到期天数")
    @PostMapping("/batchUpdateDaysToDue")
    public AjaxResult batchUpdateDaysToDue() {
        try {
            int count = lhPrecisionPlanService.batchUpdateDaysToDue();
            return AjaxResult.success("更新成功", count);
        } catch (Exception e) {
            log.error("批量更新到期天数失败", e);
            return AjaxResult.error("更新失败：" + e.getMessage());
        }
    }

    /**
     * MES回传实际完成时间
     */
    @ApiOperation("MES回传实际完成时间")
    @PostMapping("/updateActualDate")
    public AjaxResult updateActualDate(@RequestParam("mesSourceId") Long mesSourceId,
                                       @RequestParam("actualDate") String actualDate) {
        try {
            boolean result = lhPrecisionPlanService.updateActualDate(mesSourceId, actualDate);
            return result ? AjaxResult.success("更新成功") : AjaxResult.error("更新失败");
        } catch (Exception e) {
            log.error("MES回传实际完成时间失败", e);
            return AjaxResult.error("更新失败：" + e.getMessage());
        }
    }
}
