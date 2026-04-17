package com.zlt.aps.cx.controller;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cx.api.domain.entity.CxMachineInfo;
import com.zlt.aps.cx.api.domain.entity.CxPrecisionPlan;
import com.zlt.aps.cx.mapper.CxPrecisionPlanMapper;
import com.zlt.aps.cx.service.ICxPrecisionPlanService;
import com.zlt.aps.lh.api.domain.entity.LhChipStock;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Slf4j
@Api(tags = "成型精度计划管理")
@RestController
@RequestMapping("/cxPrecisionPlan")
public class CxPrecisionPlanController extends AbstractDocBizController<CxPrecisionPlan> {

    @Autowired
    private ICxPrecisionPlanService cxPrecisionPlanService;

    @Resource
    private CxPrecisionPlanMapper cxPrecisionPlanMapper;


    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody CxPrecisionPlan queryVO) {
        TableDataInfo tableDataInfo =  super.list(queryVO);
        List<CxPrecisionPlan> list = (List<CxPrecisionPlan>) tableDataInfo.getRows();
        formateCxPrecisionPlanList(list);
        return tableDataInfo;
    }

    private void formateCxPrecisionPlanList(List<CxPrecisionPlan> list) {
        for (CxPrecisionPlan cxPrecisionPlan : list) {
            Long diffDay = DateUtil.betweenDay(DateUtil.date(), cxPrecisionPlan.getPlanDate(), true);
            cxPrecisionPlan.setDaysToDue(diffDay);
        }
    }

    @Log(title = "ui.data.column.cxPrecisionPlan.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody CxPrecisionPlan entity) {
        return super.save(entity);
    }

    @Log(title = "ui.data.column.cxPrecisionPlan.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    @ApiOperation("获取详情信息")
    @GetMapping(value = "/{billId}")
    @Override
    public CxPrecisionPlan getInfo(@PathVariable("billId") Long billId) {
        CxPrecisionPlan plan = super.getInfo(billId);
        if(plan != null){
            formateCxPrecisionPlanList(Collections.singletonList(plan));
        }
        return plan;

    }

    @Log(title = "ui.data.column.cxStock.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    @Log(title = "ui.data.column.cxPrecisionPlan.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody CxPrecisionPlan queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody CxPrecisionPlan entity) {
        return cxPrecisionPlanService.checkUnique(entity);
    }

    @Override
    protected List<CxPrecisionPlan> listExportData(CxPrecisionPlan obj) {
        QueryWrapper<CxPrecisionPlan> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        List<CxPrecisionPlan> planList = cxPrecisionPlanMapper.selectList(wrapper);
        formateCxPrecisionPlanList(planList);
        return planList;
    }

    @Override
    protected IDocService getDocService() {
        return cxPrecisionPlanService;
    }

    @Override
    protected void builderCondition(QueryWrapper<CxPrecisionPlan> queryWrapper, CxPrecisionPlan queryVO) {
        queryWrapper.eq("IS_DELETE", 0);
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFactoryCode()), "FACTORY_CODE", queryVO.getFactoryCode());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getMachineCode()), "MACHINE_CODE", queryVO.getMachineCode());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getPrecisionType()), "PRECISION_TYPE", queryVO.getPrecisionType());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getCompletionStatus()), "COMPLETION_STATUS", queryVO.getCompletionStatus());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getWarningStatus()), "WARNING_STATUS", queryVO.getWarningStatus());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getDataSource()), "DATA_SOURCE", queryVO.getDataSource());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getCompanyCode()), "COMPANY_CODE", queryVO.getCompanyCode());

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

    @Override
    protected String getTypeCode() {
        return "0";
    }

    @Override
    protected String getOrderBy() {
        return "PLAN_DATE desc, ID desc";
    }

    @ApiOperation("从MES同步数据生成成型精度初版计划")
    @PostMapping("/generateFromMes")
    public AjaxResult generatePlansFromMes(@RequestParam(value = "year", required = false) Integer year) {
        try {
            int count = cxPrecisionPlanService.generatePlansFromMes(year);
            return AjaxResult.success("生成成功", count);
        } catch (Exception e) {
            log.error("从MES同步数据生成成型精度计划失败", e);
            return AjaxResult.error("生成失败：" + e.getMessage());
        }
    }

    @ApiOperation("自动生成年度成型精度计划")
    @PostMapping("/autoGenerateYearly")
    public AjaxResult autoGenerateYearlyPlans(@RequestParam("year") Integer year) {
        try {
            int count = cxPrecisionPlanService.autoGenerateYearlyPlans(year);
            return AjaxResult.success("生成成功", count);
        } catch (Exception e) {
            log.error("自动生成{}年度成型精度计划失败", year, e);
            return AjaxResult.error("生成失败：" + e.getMessage());
        }
    }

    @ApiOperation("执行30天预警检查")
    @PostMapping("/checkWarning")
    public AjaxResult checkWarning() {
        try {
            int count = cxPrecisionPlanService.checkWarning();
            return AjaxResult.success("预警检查完成", count);
        } catch (Exception e) {
            log.error("执行30天预警检查失败", e);
            return AjaxResult.error("预警检查失败：" + e.getMessage());
        }
    }

    @ApiOperation("批量更新到期天数")
    @PostMapping("/batchUpdateDaysToDue")
    public AjaxResult batchUpdateDaysToDue() {
        try {
            int count = cxPrecisionPlanService.batchUpdateDaysToDue();
            return AjaxResult.success("更新成功", count);
        } catch (Exception e) {
            log.error("批量更新到期天数失败", e);
            return AjaxResult.error("更新失败：" + e.getMessage());
        }
    }

    @ApiOperation("MES回传实际完成时间")
    @PostMapping("/updateActualDate")
    public AjaxResult updateActualDate(@RequestParam("mesSourceId") Long mesSourceId,
                                       @RequestParam("actualDate") String actualDate) {
        try {
            boolean result = cxPrecisionPlanService.updateActualDate(mesSourceId, actualDate);
            return result ? AjaxResult.success("更新成功") : AjaxResult.error("更新失败");
        } catch (Exception e) {
            log.error("MES回传实际完成时间失败", e);
            return AjaxResult.error("更新失败：" + e.getMessage());
        }
    }

}
