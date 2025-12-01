package com.zlt.aps.cd15.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.cd15.api.domain.entity.Cd15MachineInfo;
import com.zlt.aps.cd15.service.Cd15MachineInfoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 15°裁断机台信息Controller
 *
 * @author zlt
 * @date 2021-05-28
 */
@Api(tags = "15°裁断机台信息维护接口")
@RestController
@RequestMapping("/cd15/machine")
public class Cd15MachineInfoController extends BaseController {
    @Autowired
    private Cd15MachineInfoService machineInfoService;

    /**
     * 查询15°裁断机台信息列表
     */
    @ApiOperation("根据条件查询15°裁断机台信息")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody Cd15MachineInfo machineInfo) {
        startPage();
        machineInfo.setOrderStr(orderStr());
        List<Cd15MachineInfo> list = machineInfoService.selectMachineInfoList(machineInfo);
        return getDataTable(list);
    }

    /**
     * 获取15°裁断机台信息详细信息
     */
    @GetMapping(value = "/{id}")
    @ApiOperation("根据id查询15°裁断机台信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public Cd15MachineInfo getInfo(@PathVariable("id") Long id) {
        return machineInfoService.selectMachineInfoById(id);
    }

    /**
     * 新增15°裁断机台信息
     */
    @Log(title = "ui.data.column.machine.info", businessType = BusinessType.INSERT)
    //@PreAuthorize(hasPermi = "tc:machine:add")
    @ApiOperation("新增15°裁断机台信息（id不为空）")
    @PostMapping
    public AjaxResult add(@RequestBody Cd15MachineInfo machineInfo) {
        return toAjax(machineInfoService.insertMachineInfo(machineInfo));
    }

    /**
     * 修改15°裁断机台信息
     */
    @Log(title = "ui.data.column.machine.info", businessType = BusinessType.UPDATE)
    //@PreAuthorize(hasPermi = "tc:machine:edit")
    @ApiOperation("修改15°裁断机台信息（id不为空）")
    @PutMapping
    public AjaxResult edit(@RequestBody Cd15MachineInfo machineInfo) {
        return toAjax(machineInfoService.updateMachineInfo(machineInfo));
    }

    /**
     * 删除15°裁断机台信息
     */
    @Log(title = "ui.data.column.machine.info", businessType = BusinessType.DELETE)
    //@PreAuthorize(hasPermi = "tc:machine:remove")
    @ApiOperation("删除15°裁断机台信息（id不为空）")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(machineInfoService.deleteMachineInfoByIds(ids));
    }

    /**
     * 校验机台编号唯一性
     */
    @ApiOperation("校验机台编号唯一性")
    @PostMapping("/checkMachineCodeUnique")
    public String checkMachineCodeUnique(@RequestBody Cd15MachineInfo machineInfo) {
        return machineInfoService.checkMachineCodeUnique(machineInfo);
    }

    /**
     * 查询列表
     */
    @Log(title = "ui.data.column.machine.info", businessType = BusinessType.EXPORT)
    @PostMapping("/exportList")
    public List<Cd15MachineInfo> exportList(@RequestBody Cd15MachineInfo machineInfo) {
        machineInfo.setOrderStr(orderStr());
        List<Cd15MachineInfo> list = machineInfoService.selectMachineInfoList(machineInfo);
        return list;
    }

    @PostMapping("/list2")
    List<Cd15MachineInfo> list2(@RequestBody Cd15MachineInfo machineInfo) {
        startPage("a.create_time desc");
        List<Cd15MachineInfo> list = machineInfoService.selectMachineInfoList2(machineInfo);
        return list;
    }

    @Log(title = "ui.data.column.machine.info", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    @ApiOperation("导入15度裁断机台信息")
    public AjaxResult importData(@RequestBody List<Cd15MachineInfo> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (StringUtils.isNull(list) || list.size() == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return machineInfoService.importData(list, updateSupport, importLogId);
    }
}
