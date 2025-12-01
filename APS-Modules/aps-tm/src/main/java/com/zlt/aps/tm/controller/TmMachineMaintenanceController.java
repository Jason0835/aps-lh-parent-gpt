package com.zlt.aps.tm.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.zlt.aps.common.engine.constants.EngineConstants;
import com.zlt.aps.tm.api.domain.entity.TmMachineMaintenance;
import com.zlt.aps.tm.mapper.TmMachineMaintenanceEntityMapper;
import com.zlt.aps.tm.service.ITmMachineMaintenanceService;
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
 * 文件名称：TmMachineMaintenanceController.java
 * 描    述：胎面机台维修计划 控制层类：....
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
@Api(tags = "胎面机台维修计划")
@RestController
@RequestMapping("/tmMachineMaintenance")
public class TmMachineMaintenanceController extends AbstractDocBizController<TmMachineMaintenance> {

    @Autowired
    private ITmMachineMaintenanceService tmMachineMaintenanceService;

    @Autowired
    private TmMachineMaintenanceEntityMapper entityMapper;

    /**
     * 查询胎面机台维修计划列表
     */
    @RequiresPermissions("tm:tmMachineMaintenance:list")
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody TmMachineMaintenance queryVO) {
        return super.list(queryVO);
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.tmMachineMaintenance.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @RequiresPermissions("tm:tmMachineMaintenance:save")
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody TmMachineMaintenance billVO) {
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
    @Log(title = "ui.data.column.tmMachineMaintenance.modelName", businessType = BusinessType.DELETE)
    @RequiresPermissions("tm:tmMachineMaintenance:remove")
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }


    /**
     * 获取胎面机台维修计划详细信息
     */
    @RequiresPermissions("tm:tmMachineMaintenance:query")
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public TmMachineMaintenance getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入胎面机台维修计划数据
     *
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @RequiresPermissions("tm:tmMachineMaintenance:import")
    @Log(title = "ui.data.column.tmMachineMaintenance.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /**
     * 导出列表
     */
    @RequiresPermissions("tm:tmMachineMaintenance:export")
    @Log(title = "胎面机台维修计划", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody TmMachineMaintenance queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<TmMachineMaintenance> listExportData(TmMachineMaintenance obj) {
        QueryWrapper<TmMachineMaintenance> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        List<TmMachineMaintenance> list = entityMapper.selectList(wrapper);
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
        return tmMachineMaintenanceService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<TmMachineMaintenance> queryWrapper, TmMachineMaintenance queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("machineId")), "MACHINE_ID", queryVO.getFieldValueByFieldName("machineId"));
    }

    @Override
    protected String getTypeCode() {
        return "TM1001";
    }

    @Override
    protected String[] getQueryFormulas() {
        return new String[]{
                "machineName->getcolvalue(t_tm_machine_info, machine_name, id, machineId)",
        };
    }

}
