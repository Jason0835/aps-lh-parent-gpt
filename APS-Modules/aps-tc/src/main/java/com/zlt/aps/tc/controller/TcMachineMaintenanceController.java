package com.zlt.aps.tc.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.zlt.aps.common.engine.constants.EngineConstants;
import com.zlt.aps.tc.api.domain.entity.TcMachineMaintenance;
import com.zlt.aps.tc.mapper.TcMachineMaintenanceEntityMapper;
import com.zlt.aps.tc.service.ITcMachineMaintenanceService;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.exception.QueryExprException;
import com.zlt.common.utils.PubUtil;
import com.zlt.core.queryformulas.QueryFormulaUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：TcMachineMaintenanceController.java
 * 描    述：胎侧机台维修计划 控制层类：....
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-09-15
 */
@Slf4j
@Api(tags = "胎侧机台维修计划")
@RestController
@RequestMapping("/tcMachineMaintenance")
public class TcMachineMaintenanceController extends AbstractDocBizController<TcMachineMaintenance> {

    @Autowired
    private ITcMachineMaintenanceService tcMachineMaintenanceService;

    @Autowired
    private TcMachineMaintenanceEntityMapper entityMapper;

    /**
     * 查询胎侧机台维修计划列表
     */
    @RequiresPermissions("tc:tcMachineMaintenance:list")
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody TcMachineMaintenance queryVO) {
        return super.list(queryVO);
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.tcMachineMaintenance.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @RequiresPermissions("tc:tcMachineMaintenance:save")
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody TcMachineMaintenance billVO) {
        // 如果停机开始时间在7点到19点之间，则停机班次为早班，否则为夜班
        Date stopStartTime = billVO.getStopStartTime();
        if (stopStartTime != null) {
            Calendar instance = Calendar.getInstance();
            instance.setTime(stopStartTime);
            int hour = instance.get(Calendar.HOUR_OF_DAY);
            if (hour >= 7 && hour <= 19) {
                billVO.setStopShift(EngineConstants.DAY_CLASS_SHIFT);
            } else {
                billVO.setStopShift(EngineConstants.NIGHT_CLASS_SHIFT);
            }
        }
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.tcMachineMaintenance.modelName", businessType = BusinessType.DELETE)
    @RequiresPermissions("tc:tcMachineMaintenance:remove")
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }


    /**
     * 获取胎侧机台维修计划详细信息
     */
    @RequiresPermissions("tc:tcMachineMaintenance:query")
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public TcMachineMaintenance getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入胎侧机台维修计划数据
     *
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @RequiresPermissions("tc:tcMachineMaintenance:import")
    @Log(title = "ui.data.column.tcMachineMaintenance.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /**
     * 导出列表
     */
    @RequiresPermissions("tc:tcMachineMaintenance:export")
    @Log(title = "胎侧机台维修计划", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody TcMachineMaintenance queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<TcMachineMaintenance> listExportData(TcMachineMaintenance obj) {
        QueryWrapper<TcMachineMaintenance> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        List<TcMachineMaintenance> list = entityMapper.selectList(wrapper);
        //执行公式
        try {
            QueryFormulaUtil.execFormula(list, this.getQueryFormulas());
        } catch (QueryExprException e) {
            throw new ServiceException("执行查询公式时发生错误.");
        }
        return list;
    }

    @Override
    protected IDocService getDocService() {
        return tcMachineMaintenanceService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<TcMachineMaintenance> queryWrapper, TcMachineMaintenance queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("machineId")), "MACHINE_ID", queryVO.getFieldValueByFieldName("machineId"));
    }

    @Override
    protected String getTypeCode() {
        return "TC1001";
    }

    @Override
    protected String[] getQueryFormulas() {
        return new String[]{
                "machineName->getcolvalue(t_tc_machine_info, machine_name, id, machineId)",
        };
    }
}
