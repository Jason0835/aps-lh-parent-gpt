package com.zlt.aps.nc.controller;

import java.io.IOException;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.nc.api.domain.entity.NcMachineMaintenance;
import com.zlt.aps.nc.mapper.NcMachineMaintenanceMapper;
import com.zlt.aps.nc.service.NcMachineMaintenanceService;
import com.zlt.aps.utils.AppUtils;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

/**
 * 内衬机台维修计划Controller
 *
 * @author zlt
 * @date 2026-08-06
 */
@Api(tags = "内衬机台维修计划维护接口")
@RestController
@RequestMapping("/nc/machineMaintenance")
public class NcMachineMaintenanceController extends AbstractDocBizController<NcMachineMaintenance> {
    @Autowired
    private NcMachineMaintenanceService machineMaintenanceService;

    @Resource
    private NcMachineMaintenanceMapper maintenanceMapper;

    /**
     * 查询机台维修计划列表
     */
    @PostMapping("/list")
    @ApiOperation("根据条件查询列表信息")
    public TableDataInfo list(@RequestBody NcMachineMaintenance queryVO) {
        return super.list(queryVO);
    }

    /**
     * 新增/修改机台维修计划
     */
    @Log(title = "ui.data.column.nc.machineMaintenance.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存机台维修计划（id不为空）")
    @Override
    public AjaxResult save(@RequestBody NcMachineMaintenance billVO) {
        return super.save(billVO);
    }

    /**
     * 删除机台维修计划
     */
    @Log(title = "ui.data.column.nc.machineMaintenance.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("根据id批量删除信息")
    @ApiImplicitParams({ @ApiImplicitParam(name = "ids", dataType = "Long[]", value = "主键ids") })
    @PostMapping("/remove")
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    /**
     * 获取机台维修计划详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public NcMachineMaintenance getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }

    /**
     * 导入机台维修计划数据
     *
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 导入结果
     */
    @Log(title = "ui.data.column.nc.machineMaintenance.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入信息")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext,
            @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /**
     * 导出机台维修计划列表
     */
    @Log(title = "ui.data.column.nc.machineMaintenance.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody NcMachineMaintenance queryVO, @PathVariable("fileName") String fileName,
            HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<NcMachineMaintenance> listExportData(NcMachineMaintenance obj) {
        QueryWrapper<NcMachineMaintenance> wrapper = new QueryWrapper<>();
        startPage("update_time desc");
        this.builderCondition(wrapper, obj);
        List<NcMachineMaintenance> list = maintenanceMapper.selectList(wrapper);
        AppUtils.formatData(list, getQueryFormulas());
        return list;
    }

    @Override
    @SuppressWarnings("rawtypes")
    protected IDocService getDocService() {
        return machineMaintenanceService;
    }

    @Override
    protected void builderCondition(QueryWrapper<NcMachineMaintenance> queryWrapper, NcMachineMaintenance queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFactoryCode()), "FACTORY_CODE", queryVO.getFactoryCode());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getMachineCode()), "MACHINE_CODE", queryVO.getMachineCode());
    }

    @Override
    protected String getTypeCode() {
        return "0";
    }

    @Override
    protected String getOrderBy() {
        return "update_time desc";
    }

    @Override
    protected String[] getQueryFormulas() {
        return new String[] {
                "machineName->getcolvalue(t_nc_machine_info, machine_name, machine_code, machineCode)",
        };
    }
}
