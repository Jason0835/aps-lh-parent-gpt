package com.zlt.aps.lh.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.lh.api.domain.entity.LhMachineInfo;
import com.zlt.aps.lh.service.LhMachineInfoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 硫化机台信息Controller
 *
 * @author zlt
 * @date 2021-05-28
 */
@Api(tags = "硫化机台信息维护接口")
@RestController
@RequestMapping("/machine")
public class LhMachineInfoController extends BaseController {
    @Autowired
    private LhMachineInfoService machineInfoService;

    /**
     * 查询硫化机台信息列表
     */
    @ApiOperation("根据条件查询硫化机台信息")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody LhMachineInfo machineInfo) {
        startPage();
        String orderStr = orderStr();
        if (orderStr != null && orderStr.contains("dimension_minmum")) {
            orderStr = orderStr.replace("dimension_minmum", "dimension_minimum");
        }
        machineInfo.setOrderStr(orderStr);
        List<LhMachineInfo> list = machineInfoService.selectMachineInfoList(machineInfo);
        return getDataTable(list);
    }

    /**
     * 获取硫化机台信息详细信息
     */
    @GetMapping(value = "/{id}")
    @ApiOperation("根据id查询硫化机台信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public LhMachineInfo getInfo(@PathVariable("id") Long id) {
        return machineInfoService.selectMachineInfoById(id);
    }

    /**
     * 新增硫化机台信息
     */
    @Log(title = "ui.data.lh.column.machine.info", businessType = BusinessType.INSERT)
    @ApiOperation("新增硫化机台信息（id不为空）")
    @PostMapping
    public AjaxResult add(@RequestBody LhMachineInfo machineInfo) {
        return toAjax(machineInfoService.insertMachineInfo(machineInfo));
    }

    /**
     * 修改硫化机台信息
     */
    @Log(title = "ui.data.lh.column.machine.info", businessType = BusinessType.UPDATE)
    @ApiOperation("修改硫化机台信息（id不为空）")
    @PutMapping
    public AjaxResult edit(@RequestBody LhMachineInfo machineInfo) {
        return toAjax(machineInfoService.updateMachineInfo(machineInfo));
    }

    /**
     * 删除硫化机台信息
     */
    @Log(title = "ui.data.lh.column.machine.info", businessType = BusinessType.DELETE)
    @ApiOperation("删除硫化机台信息（id不为空）")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(machineInfoService.deleteMachineInfoByIds(ids));
    }

    /**
     * 校验机台编号唯一性
     */
    @ApiOperation("校验机台编号唯一性")
    @PostMapping("/checkMachineCodeUnique")
    public String checkMachineCodeUnique(@RequestBody LhMachineInfo machineInfo) {
        return machineInfoService.checkMachineCodeUnique(machineInfo);
    }

    /**
     * 查询列表
     */
    @Log(title = "ui.data.lh.column.machine.info", businessType = BusinessType.EXPORT)
    @ApiOperation("查询列表")
    @PostMapping("/exportList")
    public List<LhMachineInfo> exportList(@RequestBody LhMachineInfo machineInfo) {
        startPage();
        String orderStr = orderStr();
        if (orderStr != null && orderStr.contains("dimension_minmum")) {
            orderStr = orderStr.replace("dimension_minmum", "dimension_minimum");
        }
        machineInfo.setOrderStr(orderStr);
        List<LhMachineInfo> list = machineInfoService.selectMachineInfoList(machineInfo);
        return list;
    }

    /**
     * 根据条件查询硫化机台信息
     */
    @ApiOperation("根据条件查询硫化机台信息")
    @PostMapping("/listMachineInfo")
    public List<LhMachineInfo> listMachineInfo(@RequestBody LhMachineInfo machineInfo) {
        return machineInfoService.listMachineInfo(machineInfo);
    }

    @Log(title = "ui.data.lh.column.machine.info", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<LhMachineInfo> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return machineInfoService.importData(list, updateSupport, importLogId);
    }
}
