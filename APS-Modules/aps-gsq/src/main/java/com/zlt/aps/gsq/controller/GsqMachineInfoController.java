package com.zlt.aps.gsq.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import com.zlt.aps.gsq.api.domain.entity.GsqMachineInfo;
import com.zlt.aps.gsq.service.GsqMachineInfoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 钢丝圈机台信息Controller
 *
 * @author zlt
 * @date 2021-05-28
 */
@Api(tags = "钢丝圈机台信息维护接口")
@RestController
@RequestMapping("/gsq/machine")
public class GsqMachineInfoController extends BaseController {
    @Autowired
    private GsqMachineInfoService machineInfoService;

    /**
     * 查询钢丝圈机台信息列表
     */
    @ApiOperation("根据条件查询钢丝圈机台信息")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody GsqMachineInfo machineInfo) {
        startPage();
        machineInfo.setOrderStr(orderStr());
        List<GsqMachineInfo> list = machineInfoService.selectMachineInfoList(machineInfo);
        return getDataTable(list);
    }

    /**
     * 获取钢丝圈机台信息详细信息
     */
    @GetMapping(value = "/{id}")
    @ApiOperation("根据id查询钢丝圈机台信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public GsqMachineInfo getInfo(@PathVariable("id") Long id) {
        return machineInfoService.selectMachineInfoById(id);
    }

    /**
     * 新增钢丝圈机台信息
     */
    @Log(title = "ui.data.column.machine.info", businessType = BusinessType.INSERT)
    @ApiOperation("新增钢丝圈机台信息（id不为空）")
    @PostMapping
    public AjaxResult add(@RequestBody GsqMachineInfo machineInfo) {
        return toAjax(machineInfoService.insertMachineInfo(machineInfo));
    }

    /**
     * 修改钢丝圈机台信息
     */
    @Log(title = "ui.data.column.machine.info", businessType = BusinessType.UPDATE)
    @ApiOperation("修改钢丝圈机台信息（id不为空）")
    @PutMapping
    public AjaxResult edit(@RequestBody GsqMachineInfo machineInfo) {
        return toAjax(machineInfoService.updateMachineInfo(machineInfo));
    }

    /**
     * 删除钢丝圈机台信息
     */
    @Log(title = "ui.data.column.machine.info", businessType = BusinessType.DELETE)
    @ApiOperation("删除钢丝圈机台信息（id不为空）")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(machineInfoService.deleteMachineInfoByIds(ids));
    }

    /**
     * 校验机台编号唯一性
     */
    @ApiOperation("校验机台编号唯一性")
    @PostMapping("/checkMachineCodeUnique")
    public String checkMachineCodeUnique(@RequestBody GsqMachineInfo machineInfo) {
        return machineInfoService.checkMachineCodeUnique(machineInfo);
    }

    /**
     * 查询列表
     */
    @Log(title = "ui.data.column.machine.info", businessType = BusinessType.EXPORT)
    @ApiOperation("查询钢丝圈机台信息列表")
    @PostMapping("/exportList")
    public List<GsqMachineInfo> exportList(@RequestBody GsqMachineInfo machineInfo) {
        machineInfo.setOrderStr(orderStr());
        List<GsqMachineInfo> list = machineInfoService.selectMachineInfoList(machineInfo);
        return list;
    }

    /**
     * 根据钢丝圈代码获取对应机台信息
     */
    @ApiOperation("根据钢丝圈代码获取对应机台信息")
    @PostMapping("/listMachineInfo")
    public List<GsqMachineInfo> listMachineInfo(@RequestBody GsqMachineInfo machineInfo) {
        List<GsqMachineInfo> list = machineInfoService.listMachineInfo(machineInfo);
        return list;
    }

    @Log(title = "ui.data.column.machine.info", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    @ApiOperation("导入钢丝圈机台信息")
    public AjaxResult importData(@RequestBody List<GsqMachineInfo> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtil.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return machineInfoService.importData(list, updateSupport, importLogId);
    }
}
