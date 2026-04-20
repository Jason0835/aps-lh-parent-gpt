package com.zlt.aps.lh.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.lh.api.domain.entity.LhPrecisionPlan;
import com.zlt.aps.lh.api.domain.vo.LhPrecisionPlanExportVO;
import com.zlt.aps.lh.api.domain.vo.LhPrecisionPlanImportVO;
import com.zlt.aps.lh.api.domain.vo.LhPrecisionPlanVo;
import com.zlt.aps.lh.mapper.LhPrecisionPlanMapper;
import com.zlt.aps.lh.service.ILhPrecisionPlanService;
import com.zlt.aps.lh.service.IPrecisionPlanAutoCalculateService;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
public class LhPrecisionPlanController extends AbstractDocBizController<LhPrecisionPlan> {

    @Autowired
    private ILhPrecisionPlanService lhPrecisionPlanService;

    @Autowired
    private IPrecisionPlanAutoCalculateService precisionPlanAutoCalculateService;

    @Resource
    private LhPrecisionPlanMapper lhPrecisionPlanMapper;

    /**
     * 查询硫化精度计划列表
     */
    @ApiOperation("查询硫化精度计划列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody LhPrecisionPlan queryVO) {
        return super.list(queryVO);
    }

    /**
     * 获取详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{id}")
    @Override
    public LhPrecisionPlan getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    /**
     * 保存
     */
    @Log(title = "ui.lh.precision.plan.model.name", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody LhPrecisionPlan billVO) {
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.lh.precision.plan.model.name", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @PostMapping("/delete/{ids}")
    public AjaxResult deleteByIds(@PathVariable("ids") Long[] ids) {
        return super.removeByIds(Arrays.asList(ids));
    }

    /**
     * 导入数据
     */
    @Log(title = "ui.lh.precision.plan.model.name", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /**
     * 导入硫化精度计划数据（Feign接口）
     */
    @Log(title = "ui.lh.precision.plan.model.name", businessType = BusinessType.IMPORT)
    @ApiOperation("导入硫化精度计划数据")
    @PostMapping("/importDataFeign")
    public AjaxResult importDataFeign(@RequestBody List<LhPrecisionPlanImportVO> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        return lhPrecisionPlanService.importDataFeign(list, updateSupport, importLogId);
    }

//    @Override
//    public AjaxResult doImportData(List list, boolean updateSupport, long importLogId) {
//        return lhPrecisionPlanService.importData(list, updateSupport, importLogId);
//    }

    /**
     * 导出硫化精度计划列表（Feign接口）
     */
    @ApiOperation("导出硫化精度计划列表")
    @PostMapping("/exportData")
    public List<LhPrecisionPlanExportVO> exportDataList(@RequestBody LhPrecisionPlan queryVO) {
        LhPrecisionPlanVo vo = new LhPrecisionPlanVo();
        BeanUtils.copyProperties(queryVO, vo);
        List<LhPrecisionPlan> list = lhPrecisionPlanService.selectLhPrecisionPlanList(vo);
        
        // 转换为导出VO
        List<LhPrecisionPlanExportVO> exportList = new ArrayList<>();
        for (LhPrecisionPlan plan : list) {
            LhPrecisionPlanExportVO exportVO = new LhPrecisionPlanExportVO();
            BeanUtils.copyProperties(plan, exportVO);
            // 到期日为负数时显示为0
            if (exportVO.getDaysToDue() != null && exportVO.getDaysToDue() < 0) {
                exportVO.setDaysToDue(0);
            }
            exportList.add(exportVO);
        }
        return exportList;
    }

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody LhPrecisionPlan entity) {
        return lhPrecisionPlanService.checkUnique(entity);
    }

    @Override
    protected IDocService getDocService() {
        return lhPrecisionPlanService;
    }

    @Override
    protected String getTypeCode() {
        return "0";
    }

    @Override
    protected String getOrderBy() {
        return "PLAN_DATE desc, ID desc";
    }

    @Override
    protected List<LhPrecisionPlan> listExportData(LhPrecisionPlan obj) {
        QueryWrapper<LhPrecisionPlan> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        wrapper.last("ORDER BY " + getOrderBy());
        return lhPrecisionPlanMapper.selectList(wrapper);
    }

    @Override
    protected void builderCondition(QueryWrapper<LhPrecisionPlan> queryWrapper, LhPrecisionPlan queryVO) {
        queryWrapper.eq("IS_DELETE", 0);
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getMachineCode()), "MACHINE_CODE", queryVO.getMachineCode());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getPrecisionType()), "PRECISION_TYPE", queryVO.getPrecisionType());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getCompletionStatus()), "COMPLETION_STATUS", queryVO.getCompletionStatus());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getWarningStatus()), "WARNING_STATUS", queryVO.getWarningStatus());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getDataSource()), "DATA_SOURCE", queryVO.getDataSource());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getCompanyCode()), "COMPANY_CODE", queryVO.getCompanyCode());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFactoryCode()), "FACTORY_CODE", queryVO.getFactoryCode());

        if (queryVO.getPlanDateStart() != null) {
            queryWrapper.ge("PLAN_DATE", queryVO.getPlanDateStart());
        }
        if (queryVO.getPlanDateEnd() != null) {
            queryWrapper.le("PLAN_DATE", queryVO.getPlanDateEnd());
        }
        if (queryVO.getActualDateStart() != null) {
            queryWrapper.ge("ACTUAL_DATE", queryVO.getActualDateStart());
        }
        if (queryVO.getActualDateEnd() != null) {
            queryWrapper.le("ACTUAL_DATE", queryVO.getActualDateEnd());
        }
    }

    /**
     * 从MES同步数据生成硫化精度初版计划
     */
    @ApiOperation("从MES同步数据生成硫化精度初版计划")
    @PostMapping("/generateFromMes")
    public AjaxResult generatePlansFromMes(@RequestParam("year") Integer year) {
        try {
            int count = lhPrecisionPlanService.generatePlansFromMes(year);
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

    /**
     * 自动推算硫化精度计划（年度）
     */
    @ApiOperation("自动推算硫化精度计划（年度）")
    @PostMapping("/autoCalculateLh")
    public AjaxResult autoCalculateLhPrecisionPlan(@RequestParam("year") Integer year) {
        return precisionPlanAutoCalculateService.autoCalculateLhPrecisionPlan(year);
    }

    /**
     * 根据设备保养计划生成并推算硫化精度计划
     */
    @ApiOperation("根据设备保养计划生成并推算硫化精度计划")
    @PostMapping("/generateFromMaintenance")
    public AjaxResult generateFromMaintenancePlan(@RequestBody List<Long> maintenancePlanIds) {
        return precisionPlanAutoCalculateService.generateFromMaintenancePlanByIds(maintenancePlanIds, "硫化精度");
    }
}
