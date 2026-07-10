package com.zlt.aps.cd15.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cd15.api.domain.entity.Cd15MachineMaintenancePlan;
import com.zlt.aps.cd15.mapper.Cd15MachineMaintenancePlanMapper;
import com.zlt.aps.cd15.service.ICd15MachineMaintenancePlanService;
import com.zlt.aps.utils.AppUtils;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 斜裁机台检修计划控制层。
 */
@Api(tags = "斜裁机台检修计划")
@RestController
@RequestMapping("/cd15MachineMaintenance")
public class Cd15MachineMaintenancePlanController extends AbstractDocBizController<Cd15MachineMaintenancePlan> {

    @Resource
    private ICd15MachineMaintenancePlanService cd15MachineMaintenancePlanService;

    @Resource
    private Cd15MachineMaintenancePlanMapper cd15MachineMaintenancePlanMapper;

    /** 查询斜裁机台检修计划列表 */
    @ApiOperation("查询斜裁机台检修计划列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody Cd15MachineMaintenancePlan queryVO) {
        return super.list(queryVO);
    }

    /** 新增斜裁机台检修计划 */
    @Log(title = "ui.data.column.cd15MachineMaintenancePlan.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增斜裁机台检修计划")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody Cd15MachineMaintenancePlan entity) {
        AjaxResult validateResult = cd15MachineMaintenancePlanService.validateForSave(entity);
        if (validateResult != null) {
            return validateResult;
        }
        return super.save(entity);
    }

    /** 编辑斜裁机台检修计划 */
    @Log(title = "ui.data.column.cd15MachineMaintenancePlan.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("编辑斜裁机台检修计划")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody Cd15MachineMaintenancePlan entity) {
        AjaxResult validateResult = cd15MachineMaintenancePlanService.validateForSave(entity);
        if (validateResult != null) {
            return validateResult;
        }
        return super.save(entity);
    }

    /** 删除斜裁机台检修计划 */
    @Log(title = "ui.data.column.cd15MachineMaintenancePlan.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除斜裁机台检修计划")
    @PostMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    /** 获取斜裁机台检修计划详情 */
    @ApiOperation("获取斜裁机台检修计划详情")
    @GetMapping("/getInfo/{id}")
    @Override
    public Cd15MachineMaintenancePlan getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    /** 校验斜裁机台检修计划唯一性 */
    @ApiOperation("校验斜裁机台检修计划唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody Cd15MachineMaintenancePlan entity) {
        return cd15MachineMaintenancePlanService.checkUnique(entity);
    }

    /** 校验斜裁机台检修计划时间段重叠 */
    @ApiOperation("校验斜裁机台检修计划时间段重叠")
    @PostMapping("/checkOverlap")
    public String checkOverlap(@RequestBody Cd15MachineMaintenancePlan entity) {
        return cd15MachineMaintenancePlanService.checkOverlap(entity);
    }

    /** 导入斜裁机台检修计划 */
    @Log(title = "ui.data.column.cd15MachineMaintenancePlan.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入斜裁机台检修计划")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /** 导出斜裁机台检修计划 */
    @Log(title = "ui.data.column.cd15MachineMaintenancePlan.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出斜裁机台检修计划")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody Cd15MachineMaintenancePlan queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<Cd15MachineMaintenancePlan> listExportData(Cd15MachineMaintenancePlan obj) {
        QueryWrapper<Cd15MachineMaintenancePlan> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        List<Cd15MachineMaintenancePlan> list = cd15MachineMaintenancePlanMapper.selectList(wrapper);
        AppUtils.formatData(list, getQueryFormulas());
        return list;
    }

    @Override
    protected IDocService getDocService() {
        return cd15MachineMaintenancePlanService;
    }

    @Override
    protected void builderCondition(QueryWrapper<Cd15MachineMaintenancePlan> queryWrapper, Cd15MachineMaintenancePlan queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFactoryCode()), "FACTORY_CODE", queryVO.getFactoryCode());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getMachineCode()), "MACHINE_CODE", queryVO.getMachineCode());
        queryWrapper.eq(queryVO.getDowntimeDate() != null, "DOWNTIME_DATE", queryVO.getDowntimeDate());
    }

    @Override
    protected String getTypeCode() {
        return "CD15_MACHINE_MAINTENANCE_PLAN";
    }

    @Override
    protected String getOrderBy() {
        return "MACHINE_CODE asc, DOWNTIME_DATE desc, DOWNTIME_START_TIME asc";
    }
}