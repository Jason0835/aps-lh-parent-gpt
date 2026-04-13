package com.zlt.aps.cx.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cx.api.domain.entity.CxPrecisionPlan;
import com.zlt.aps.cx.mapper.CxPrecisionPlanMapper;
import com.zlt.aps.cx.service.ICxPrecisionPlanAutoCalculateService;
import com.zlt.aps.cx.service.ICxPrecisionPlanService;
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
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：CxPrecisionPlanController.java
 * 描    述：成型精度计划管理 控制层类
 *
 * @author APS Team
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：...
 * 修改内容：...
 * @date 2026-04-03
 */
@Slf4j
@Api(tags = "成型精度计划管理")
@RestController
@RequestMapping("/cxPrecisionPlan")
public class CxPrecisionPlanController extends AbstractDocBizController<CxPrecisionPlan> {

    @Autowired
    private ICxPrecisionPlanService cxPrecisionPlanService;

    @Autowired
    private ICxPrecisionPlanAutoCalculateService cxPrecisionPlanAutoCalculateService;

    @Resource
    private CxPrecisionPlanMapper cxPrecisionPlanMapper;

    /**
     * 查询成型精度计划列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody CxPrecisionPlan queryVO) {
        return super.list(queryVO);
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.cxPrecisionPlan.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody CxPrecisionPlan entity) {
        return super.save(entity);
    }

    /**
     * 删除成型精度计划
     */
    @Log(title = "ui.data.column.cxPrecisionPlan.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    /**
     * 获取成型精度计划详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public CxPrecisionPlan getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入成型库存数据
     *
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.cxStock.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /**
     * 导出来型精度计划列表
     */
    @Log(title = "ui.data.column.cxPrecisionPlan.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody CxPrecisionPlan queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody CxPrecisionPlan entity) {
        return cxPrecisionPlanService.checkUnique(entity);
    }

    @Override
    protected List<CxPrecisionPlan> listExportData(CxPrecisionPlan obj) {
        QueryWrapper<CxPrecisionPlan> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return cxPrecisionPlanMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService() {
        return cxPrecisionPlanService;
    }

    @Override
    protected void builderCondition(QueryWrapper<CxPrecisionPlan> queryWrapper, CxPrecisionPlan queryVO) {
        // 工厂编码精确查询
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFactoryCode()), "FACTORY_CODE", queryVO.getFactoryCode());
        // 机台名称模糊查询
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getMachineName()), "MACHINE_NAME", queryVO.getMachineName());
        // 计划日期区间查询
        if (PubUtil.isNotEmpty(queryVO.getPlanDateBegin()) && PubUtil.isNotEmpty(queryVO.getPlanDateEnd())) {
            queryWrapper.between("PLAN_DATE", queryVO.getPlanDateBegin(), queryVO.getPlanDateEnd());
        }
        // 其他精确查询条件
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getMachineCode()), "MACHINE_CODE", queryVO.getMachineCode());
    }

    @Override
    protected String getTypeCode() {
        return "0";
    }

    @Override
    protected String getOrderBy() {
        return "plan_date desc, factory_code asc, machine_code asc";
    }

    /**
     * 从MES同步数据生成成型精度初版计划
     */
    @ApiOperation("从MES同步数据生成成型精度初版计划")
    @PostMapping("/generateFromMes")
    public AjaxResult generatePlansFromMes() {
        try {
            int count = cxPrecisionPlanService.generatePlansFromMes();
            return AjaxResult.success("生成成功", count);
        } catch (Exception e) {
            log.error("从MES同步数据生成成型精度计划失败", e);
            return AjaxResult.error("生成失败：" + e.getMessage());
        }
    }

    /**
     * 自动生成年度成型精度计划
     */
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

    /**
     * 批量更新到期天数
     */
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

    /**
     * MES回传实际完成时间
     */
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

    /**
     * 自动推算成型精度计划（15天周期）
     */
    @ApiOperation("自动推算成型精度计划（15天周期）")
    @PostMapping("/autoCalculateCx15Days")
    public AjaxResult autoCalculateCxPrecisionPlan15Days(@RequestParam("year") Integer year) {
        return cxPrecisionPlanAutoCalculateService.autoCalculateCxPrecisionPlan15Days(year);
    }

    /**
     * 自动推算成型精度计划（60天周期）
     */
    @ApiOperation("自动推算成型精度计划（60天周期）")
    @PostMapping("/autoCalculateCx60Days")
    public AjaxResult autoCalculateCxPrecisionPlan60Days(@RequestParam("year") Integer year) {
        return cxPrecisionPlanAutoCalculateService.autoCalculateCxPrecisionPlan60Days(year);
    }

    /**
     * 根据设备保养计划生成并推算成型精度计划
     */
    @ApiOperation("根据设备保养计划生成并推算成型精度计划")
    @PostMapping("/generateFromMaintenance")
    public AjaxResult generateFromMaintenancePlan(@RequestBody List<Long> maintenancePlanIds,
                                                   @RequestParam("cycleDays") Integer cycleDays) {
        return cxPrecisionPlanAutoCalculateService.generateFromMaintenancePlanByIds(maintenancePlanIds, cycleDays);
    }
}
