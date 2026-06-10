package com.zlt.aps.cd90.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cd90.api.domain.entity.Cd90MachineMaintenancePlan;
import com.zlt.aps.cd90.mapper.Cd90MachineMaintenancePlanMapper;
import com.zlt.aps.cd90.service.ICd90MachineMaintenancePlanService;
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

@Api(tags = "直裁机台检修计划")
@RestController
@RequestMapping("/cd90MachineMaintenance")
public class Cd90MachineMaintenancePlanController extends AbstractDocBizController<Cd90MachineMaintenancePlan> {

    @Resource
    private ICd90MachineMaintenancePlanService cd90MachineMaintenancePlanService;

    @Resource
    private Cd90MachineMaintenancePlanMapper cd90MachineMaintenancePlanMapper;

    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody Cd90MachineMaintenancePlan queryVO) {
        return super.list(queryVO);
    }

    @Log(title = "ui.data.column.machineMaintenancePlan.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody Cd90MachineMaintenancePlan entity) {
        return super.save(entity);
    }

    @Log(title = "ui.data.column.machineMaintenancePlan.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("编辑")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody Cd90MachineMaintenancePlan entity) {
        return super.save(entity);
    }

    @Log(title = "ui.data.column.machineMaintenancePlan.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @PostMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    @ApiOperation("获取详情")
    @GetMapping("/getInfo/{id}")
    @Override
    public Cd90MachineMaintenancePlan getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody Cd90MachineMaintenancePlan entity) {
        return cd90MachineMaintenancePlanService.checkUnique(entity);
    }

    @Log(title = "ui.data.column.machineMaintenancePlan.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    @Log(title = "ui.data.column.machineMaintenancePlan.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody Cd90MachineMaintenancePlan queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<Cd90MachineMaintenancePlan> listExportData(Cd90MachineMaintenancePlan obj) {
        QueryWrapper<Cd90MachineMaintenancePlan> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        List<Cd90MachineMaintenancePlan> list = cd90MachineMaintenancePlanMapper.selectList(wrapper);
        AppUtils.formatData(list, getQueryFormulas());
        return list;
    }

    @Override
    protected IDocService getDocService() {
        return cd90MachineMaintenancePlanService;
    }

    @Override
    protected void builderCondition(QueryWrapper<Cd90MachineMaintenancePlan> queryWrapper, Cd90MachineMaintenancePlan queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFactoryCode()), "FACTORY_CODE", queryVO.getFactoryCode());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getMachineCode()), "MACHINE_CODE", queryVO.getMachineCode());
        queryWrapper.eq(queryVO.getDowntimeDate() != null, "DOWNTIME_DATE", queryVO.getDowntimeDate());
    }

    @Override
    protected String getTypeCode() {
        return "CD90_MACHINE_MAINTENANCE_PLAN";
    }

    @Override
    protected String getOrderBy() {
        return "MACHINE_CODE asc, DOWNTIME_DATE desc, DOWNTIME_START_TIME asc";
    }
}