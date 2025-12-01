package com.zlt.aps.cd90.controller;


import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cd90.api.domain.dto.Cd90BigRollDto;
import com.zlt.aps.cd90.entity.Cd90BigRoll;
import com.zlt.aps.cd90.service.Cd90BigRollService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@Api(tags = {"90度裁断帘布大卷信息接口"})
@RestController
@RequestMapping("/cd90/bigRoll")
public class Cd90BigRollController extends BaseController {

    @Resource
    private Cd90BigRollService Cd90BigRollService;

    @ApiOperation("根据条件查询帘布大卷信息列表")
    @PostMapping("/listBigRoll")
    public TableDataInfo listBigRoll(@RequestBody Cd90BigRollDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        List<Cd90BigRollDto> list = Cd90BigRollService.listBigRoll(dto);
        return getDataTable(list);
    }

    @ApiOperation("根据id查询帘布大卷信息信息")
    @GetMapping("/getBigRoll/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public Cd90BigRollDto getBigRoll(@PathVariable("id") Long id) {
        Cd90BigRollDto dto = new Cd90BigRollDto();
        BeanUtils.copyProperties(Cd90BigRollService.getById(id), dto);
        return dto;
    }

    @Log(title = "ui.cd90.bigRoll.column.modalName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存帘布大卷信息信息（id为空则新增，id不为空则修改）")
    @PostMapping("/saveBigRoll")
    public AjaxResult saveBigRoll(@RequestBody Cd90BigRollDto dto) {
        Cd90BigRoll entity = new Cd90BigRoll();
        BeanUtils.copyProperties(dto, entity);
        Cd90BigRollService.saveBigRoll(entity);
        return AjaxResult.success();
    }

    @ApiOperation("根据code判断帘布大卷代号是否已经存在")
    @PostMapping("/checkBigRollCodeUnique")
    public String checkBigRollCodeUnique(@RequestBody Cd90BigRollDto dto) {
        return Cd90BigRollService.checkBigRollCodeUnique(dto);
    }

    @Log(title = "ui.cd90.bigRoll.column.modalName", businessType = BusinessType.DELETE)
    @ApiOperation("批量删除帘布大卷信息信息(逻辑删)")
    @PostMapping("/deleteBigRoll/{ids}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id數組", paramType = "query")
    })
    public AjaxResult deleteBigRoll(@PathVariable("ids") Long[] ids) {
        Cd90BigRollService.deleteBigRoll(ids);
        return AjaxResult.success();
    }

    @Log(title = "ui.cd90.bigRoll.column.modalName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData")
    public List<Cd90BigRollDto> exportData(@RequestBody Cd90BigRollDto dto) {
        dto.setOrderStr(orderStr());
        List<Cd90BigRollDto> list = Cd90BigRollService.listBigRoll(dto);
        return list;
    }

    @Log(title = "ui.cd90.bigRoll.column.modalName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<Cd90BigRollDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return Cd90BigRollService.importData(list, updateSupport, importLogId);
    }
}
