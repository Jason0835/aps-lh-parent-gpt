package com.zlt.aps.tm.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.tm.api.domain.entity.TmMachineInfo;
import com.zlt.aps.tm.service.TmMachineInfoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 胎面机台信息Controller
 *
 * @author zlt
 * @date 2021-05-28
 */
@Api(tags = "胎面机台信息维护接口")
@RestController
@RequestMapping("/machine")
public class TmMachineInfoController extends BaseController {
    @Autowired
    private TmMachineInfoService tTmMachineInfoService;

    /**
     * 查询胎面机台信息列表
     */
    @ApiOperation("根据条件查询胎面机台信息")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody TmMachineInfo machineInfo) {
        startPage();
        machineInfo.setOrderStr(orderStr());
        List<TmMachineInfo> list = tTmMachineInfoService.selectMachineInfoList(machineInfo);
        return getDataTable(list);
    }

    /**
     * 获取胎面机台信息详细信息
     */
    @GetMapping(value = "/{id}")
    @ApiOperation("根据id查询胎面机台信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public TmMachineInfo getInfo(@PathVariable("id") Long id) {
        return tTmMachineInfoService.selectMachineInfoById(id);
    }

    /**
     * 新增胎面机台信息
     */
    @Log(title = "ui.data.column.machine.info", businessType = BusinessType.INSERT)
    @ApiOperation("新增胎面机台信息（id不为空）")
    @PostMapping
    public AjaxResult add(@RequestBody TmMachineInfo machineInfo) {
        return toAjax(tTmMachineInfoService.insertMachineInfo(machineInfo));
    }

    /**
     * 修改胎面机台信息
     */
    @Log(title = "ui.data.column.machine.info", businessType = BusinessType.UPDATE)
    @ApiOperation("修改胎面机台信息（id不为空）")
    @PutMapping
    public AjaxResult edit(@RequestBody TmMachineInfo machineInfo) {
        return toAjax(tTmMachineInfoService.updateMachineInfo(machineInfo));
    }

    /**
     * 删除胎面机台信息
     */
    @Log(title = "ui.data.column.machine.info", businessType = BusinessType.DELETE)
    @ApiOperation("删除胎面机台信息（id不为空）")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(tTmMachineInfoService.deleteMachineInfoByIds(ids));
    }

    /**
     * 校验机台编号唯一性
     */
    @ApiOperation("校验机台编号唯一性")
    @PostMapping("/checkMachineCodeUnique")
    public String checkMachineCodeUnique(@RequestBody TmMachineInfo machineInfo) {
        return tTmMachineInfoService.checkMachineCodeUnique(machineInfo);
    }

    /**
     * 查询存信息列表
     */
    @Log(title = "ui.data.column.machine.info", businessType = BusinessType.EXPORT)
    @PostMapping("/exportList")
    public List<TmMachineInfo> exportList(@RequestBody TmMachineInfo machineInfo) {
        startPage();
        machineInfo.setOrderStr(orderStr());
        List<TmMachineInfo> list = tTmMachineInfoService.selectMachineInfoList(machineInfo);
        return list;
    }

    /**
     * 根据胎面和口型板获取对应机台信息
     */
    @PostMapping("/list2")
    public List<TmMachineInfo> list2(@RequestBody TmMachineInfo machineInfo) {
        List<TmMachineInfo> list = tTmMachineInfoService.selectMachineInfoList2(machineInfo);
        return list;
    }

    @Log(title = "ui.data.column.machine.info", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    @ApiOperation("导入胎面机台信息")
    public AjaxResult importData(@RequestBody List<TmMachineInfo> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (StringUtils.isNull(list) || list.size() == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return tTmMachineInfoService.importData(list, updateSupport, importLogId);
    }
}
