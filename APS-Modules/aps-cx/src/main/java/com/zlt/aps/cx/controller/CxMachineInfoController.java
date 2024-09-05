package com.zlt.aps.cx.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cx.api.domain.dto.LhMachineInfoDto;
import com.zlt.aps.cx.api.domain.entity.CxMachineInfo;
import com.zlt.aps.cx.service.CxMachineInfoService;
import com.zlt.aps.cx.service.LhMachineInfoEmbyroStockService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 成型机台信息Controller
 *
 * @author zlt
 * @date 2021-05-28
 */
@Api(tags = "成型机台信息维护接口")
@RestController
@RequestMapping("/cx/machine")
public class CxMachineInfoController extends BaseController {
    @Autowired
    private CxMachineInfoService cxMachineInfoService;

    @Autowired
    private LhMachineInfoEmbyroStockService lhMachineInfoEmbyroStockService;

    /**
     * 查询成型机台信息列表
     */
    @ApiOperation("根据条件查询成型机台信息")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody CxMachineInfo cxMachineInfo) {
        startPage();
        String orderStr = orderStr();
        // 字段下划线和数据库不一致替换
        if (orderStr != null && orderStr.contains("dimension_mini_mum")) {
            orderStr = orderStr.replace("dimension_mini_mum","dimension_minimum");
        }else if (orderStr != null && orderStr.contains("dimension_maxi_mum")){
            orderStr = orderStr.replace("dimension_maxi_mum","dimension_maximum");
        }
        cxMachineInfo.setOrderStr(orderStr);
        List<CxMachineInfo> list = cxMachineInfoService.selectCxMachineInfoList(cxMachineInfo);
        return getDataTable(list);
    }

    /**
     * 查询成型机台信息列表
     */
    @ApiOperation("根据条件查询成型机台信息")
    @PostMapping("/listOrderByName")
    public List<CxMachineInfo> listOrderByName(@RequestBody CxMachineInfo cxMachineInfo) {
        startPage("machine_name");
        List<CxMachineInfo> list = cxMachineInfoService.listOrderByName(cxMachineInfo);
        return list;
    }


    /**
     * 获取成型机台信息详细信息
     */
    @GetMapping(value = "/{id}")
    @ApiOperation("根据id查询成型机台信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public CxMachineInfo getInfo(@PathVariable("id") Long id) {
        return cxMachineInfoService.selectCxMachineInfoById(id);
    }

    /**
     * 新增成型机台信息
     */
    //@PreAuthorize(hasPermi = "cx:machine:add")
    @Log(title = "ui.cx.machine.export.sheetName", businessType = BusinessType.INSERT)
    @ApiOperation("新增成型机台信息（id不为空）")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody CxMachineInfo cxMachineInfo) {
        return toAjax(cxMachineInfoService.insertCxMachineInfo(cxMachineInfo));
    }

    /**
     * 修改成型机台信息
     */
    //@PreAuthorize(hasPermi = "cx:machine:edit")
    @Log(title = "ui.cx.machine.export.sheetName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改成型机台信息（id不为空）")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody CxMachineInfo cxMachineInfo) {
        return toAjax(cxMachineInfoService.updateCxMachineInfo(cxMachineInfo));
    }

    /**
     * 删除成型机台信息
     */
    //@PreAuthorize(hasPermi = "cx:machine:remove")
    @Log(title = "ui.cx.machine.export.sheetName", businessType = BusinessType.DELETE)
    @ApiOperation("删除成型机台信息（id不为空）")
    @DeleteMapping("/remove/{ids}")
    public AjaxResult remove(@PathVariable("ids") Long[] ids) {
        return toAjax(cxMachineInfoService.deleteCxMachineInfoByIds(ids));
    }

    /**
     * 校验机台编号唯一性
     */
    @ApiOperation("校验机台编号唯一性")
    @PostMapping("/checkMachineCodeUnique")
    public String checkMachineCodeUnique(@RequestBody CxMachineInfo cxMachineInfo) {
        return cxMachineInfoService.checkMachineCodeUnique(cxMachineInfo);
    }

    /**
     * 查询列表
     */
    @Log(title = "ui.cx.machine.export.sheetName", businessType = BusinessType.EXPORT)
    @PostMapping("/exportList")
    public List<CxMachineInfo> exportList(@RequestBody CxMachineInfo cxMachineInfo) {
        startPage();
        String orderStr = orderStr();
        // 字段下划线和数据库不一致替换
        if (orderStr != null && orderStr.contains("dimension_mini_mum")) {
            orderStr = orderStr.replace("dimension_mini_mum","dimension_minimum");
        }else if (orderStr != null && orderStr.contains("dimension_maxi_mum")){
            orderStr = orderStr.replace("dimension_maxi_mum","dimension_maximum");
        }
        cxMachineInfo.setOrderStr(orderStr);
        List<CxMachineInfo> list = cxMachineInfoService.selectCxMachineInfoList(cxMachineInfo);
        return list;
    }

    @PostMapping("/list2")
    public List<CxMachineInfo> list2(@RequestBody CxMachineInfo cxMachineInfo) {
        List<CxMachineInfo> list = cxMachineInfoService.selectCxMachineInfoList2(cxMachineInfo);
        return list;
    }

    /**
     * 获取其他半部件机台列表
     */
    @PostMapping("/getOrtherMachineInfo")
    public List<CxMachineInfo> getOrtherMachineInfo(@RequestBody CxMachineInfo cxMachineInfo) {
        List<CxMachineInfo> list = cxMachineInfoService.getOrtherMachineInfo(cxMachineInfo);
        return list;
    }

    /**
     * 硫化机台下拉列表
     */
    @PostMapping("/getLhMachineForQty")
    public TableDataInfo getLhMachineForQty(@RequestBody LhMachineInfoDto lhMachineInfoDto) {
        List<LhMachineInfoDto> list = lhMachineInfoEmbyroStockService.getList(lhMachineInfoDto.getMachineName());
        return getDataTable(list);
    }


    @Log(title = "ui.cx.machine.export.sheetName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<CxMachineInfo> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return cxMachineInfoService.importData(list, updateSupport, importLogId);
    }
}
