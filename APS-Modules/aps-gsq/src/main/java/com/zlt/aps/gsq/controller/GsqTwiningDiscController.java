package com.zlt.aps.gsq.controller;


import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import com.zlt.aps.gsq.api.domain.dto.GsqTwiningDiscDto;
import com.zlt.aps.gsq.entity.GsqTwiningDisc;
import com.zlt.aps.gsq.service.GsqTwiningDiscService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@Api(tags = {"钢丝圈缠绕盘接口"})
@RestController
@RequestMapping("/twiningDisc")
public class GsqTwiningDiscController extends BaseController {

    @Resource
    private GsqTwiningDiscService gsqTwiningDiscService;

    @ApiOperation("根据条件查询缠绕盘列表")
    @GetMapping("/listTwiningDisc")
    public TableDataInfo listTwiningDisc(GsqTwiningDiscDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        List<GsqTwiningDiscDto> list = gsqTwiningDiscService.listTwiningDisc(dto);
        return getDataTable(list);
    }

    @ApiOperation("根据id查询缠绕盘信息")
    @GetMapping("/getTwiningDisc/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public GsqTwiningDiscDto getTwiningDisc(@PathVariable("id") Long id) {
        GsqTwiningDiscDto dto = new GsqTwiningDiscDto();
        BeanUtils.copyProperties(gsqTwiningDiscService.getById(id), dto);
        return dto;
    }

    @Log(title = "ui.gsq.twiningDisc.column.modalName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存缠绕盘信息（id为空则新增，id不为空则修改）")
    @PostMapping("/saveTwiningDisc")
    public AjaxResult saveTwiningDisc(@RequestBody GsqTwiningDiscDto dto) {
        GsqTwiningDisc entity = new GsqTwiningDisc();
        BeanUtils.copyProperties(dto, entity);
        gsqTwiningDiscService.saveTwiningDisc(entity);
        return AjaxResult.success();
    }

    @ApiOperation("根据code判断缠绕盘代号是否已经存在")
    @PostMapping("/checkSerialNumberUnique")
    public String checkSerialNumberUnique(@RequestBody GsqTwiningDiscDto dto) {
        return gsqTwiningDiscService.checkSerialNumberUnique(dto);
    }

    @Log(title = "ui.gsq.twiningDisc.column.modalName", businessType = BusinessType.DELETE)
    @ApiOperation("批量删除缠绕盘信 息(逻辑删)")
    @PostMapping("/deleteTwiningDisc/{ids}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id數組", paramType = "query")
    })
    public AjaxResult deleteTwiningDisc(@PathVariable("ids") Long[] ids) {
        gsqTwiningDiscService.deleteTwiningDisc(ids);
        return AjaxResult.success();
    }


    @Log(title = "ui.gsq.twiningDisc.column.modalName", businessType = BusinessType.DELETE)
    @ApiOperation("删除全部(逻辑删)")
    @PostMapping("/deleteAll")
    public AjaxResult deleteAll() {
        gsqTwiningDiscService.deleteAll();
        return AjaxResult.success();
    }


    @Log(title = "ui.gsq.twiningDisc.column.modalName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @GetMapping("/exportData")
    public List<GsqTwiningDiscDto> exportData(GsqTwiningDiscDto dto) {
        dto.setOrderStr(orderStr());
        List<GsqTwiningDiscDto> list = gsqTwiningDiscService.listTwiningDisc(dto);
        return list;
    }

    @Log(title = "ui.gsq.twiningDisc.column.modalName", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    @ApiOperation("导入钢丝圈缠绕盘信息")
    public AjaxResult importData(@RequestBody List<GsqTwiningDiscDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtil.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return gsqTwiningDiscService.importData(list, updateSupport, importLogId);
    }
}
