package com.zlt.aps.dj.controller;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.zlt.aps.common.engine.constants.EngineConstants;
import com.zlt.aps.dj.api.domain.entity.DjMachineMaintenance;
import com.zlt.aps.dj.mapper.DjMachineMaintenanceEntityMapper;
import com.zlt.aps.dj.service.IDjMachineMaintenanceService;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.exception.QueryExprException;
import com.zlt.common.utils.PubUtil;
import com.zlt.core.queryformulas.QueryFormulaUtil;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

/**
 * 垫胶机台维修计划Controller
 *
 * @author zlt
 * @date 2026-06-11
 */
@Slf4j
@Api(tags = "垫胶机台维修计划")
@RestController
@RequestMapping("/dj/machineMaintenance")
public class DjMachineMaintenanceController extends AbstractDocBizController<DjMachineMaintenance> {

    @Autowired
    private IDjMachineMaintenanceService machineMaintenanceService;

    @Autowired
    private DjMachineMaintenanceEntityMapper entityMapper;

    /**
     * 查询垫胶机台维修计划列表
     */
    @RequiresPermissions("dj:machineMaintenance:list")
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody DjMachineMaintenance queryVO) {
        return super.list(queryVO);
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.dj.machineMaintenance.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @RequiresPermissions("dj:machineMaintenance:save")
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody DjMachineMaintenance billVO) {
        Date stopStartTime = billVO.getStopStartTime();
        if (stopStartTime != null) {
            Calendar instance = Calendar.getInstance();
            instance.setTime(stopStartTime);
            int hour = instance.get(Calendar.HOUR_OF_DAY);
            // 按 ClassNumThreePlanEnums 班次定义："01"=夜班(22:00~06:00)、"02"=早班(06:00~14:00)、"03"=中班(14:00~22:00)
            if (hour >= 6 && hour < 14) {
                billVO.setStopShift(EngineConstants.MORNING_CLASS_SHIFT);
            } else if (hour >= 14 && hour < 22) {
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
    @Log(title = "ui.data.column.dj.machineMaintenance.modelName", businessType = BusinessType.DELETE)
    @RequiresPermissions("dj:machineMaintenance:remove")
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    /**
     * 获取垫胶机台维修计划详细信息
     */
    @RequiresPermissions("dj:machineMaintenance:query")
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public DjMachineMaintenance getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }

    /**
     * 根据集合导入垫胶机台维修计划数据
     *
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @RequiresPermissions("dj:machineMaintenance:import")
    @Log(title = "ui.data.column.dj.machineMaintenance.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /**
     * 导出列表
     */
    @RequiresPermissions("dj:machineMaintenance:export")
    @Log(title = "垫胶机台维修计划", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody DjMachineMaintenance queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<DjMachineMaintenance> listExportData(DjMachineMaintenance obj) {
        QueryWrapper<DjMachineMaintenance> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        List<DjMachineMaintenance> list = entityMapper.selectList(wrapper);
        try {
            QueryFormulaUtil.execFormula(list, this.getQueryFormulas());
        } catch (QueryExprException e) {
            throw new ServiceException("执行查询公式时发生错误.");
        }
        return list;
    }

    @Override
    protected IDocService getDocService() {
        return machineMaintenanceService;
    }

    @Override
    protected void builderCondition(QueryWrapper<DjMachineMaintenance> queryWrapper, DjMachineMaintenance queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFactoryCode()), "FACTORY_CODE", queryVO.getFactoryCode());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getMachineCode()), "MACHINE_CODE", queryVO.getMachineCode());
    }

    @Override
    protected String getTypeCode() {
        return "DJ1001";
    }

    @Override
    protected String[] getQueryFormulas() {
        return new String[]{
                "machineName->getcolvalue(t_dj_machine_info, machine_name, machine_code, machineCode)",
        };
    }
}