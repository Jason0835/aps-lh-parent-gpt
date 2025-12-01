package com.zlt.aps.cd15.controller;


import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.cd15.api.domain.dto.Cd15BigRollDto;
import com.zlt.aps.cd15.entity.Cd15BigRoll;
import com.zlt.aps.cd15.service.Cd15BigRollService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@Api(tags = {"15度裁断钢压大卷信息接口"})
@RestController
@RequestMapping("/cd15/bigRoll")
public class Cd15BigRollController extends BaseController {

    @Resource
    private Cd15BigRollService cd15BigRollService;

    @ApiOperation("根据条件查询钢压大卷信息列表")
    @PostMapping("/listBigRoll")
    public TableDataInfo listBigRoll(@RequestBody Cd15BigRollDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        List<Cd15BigRollDto> list = cd15BigRollService.listBigRoll(dto);
        return getDataTable(list);
    }

    @ApiOperation("根据id查询钢压大卷信息信息")
    @GetMapping("/getBigRoll/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public Cd15BigRollDto getBigRoll(@PathVariable("id") Long id) {
        Cd15BigRollDto dto = new Cd15BigRollDto();
        BeanUtils.copyProperties(cd15BigRollService.getById(id), dto);
        return dto;
    }

    @Log(title = "ui.cd15.bigRoll.column.modalName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存钢压大卷信息信息（id为空则新增，id不为空则修改）")
    @PostMapping("/saveBigRoll")
    public AjaxResult saveBigRoll(@RequestBody Cd15BigRollDto dto) {
        Cd15BigRoll entity = new Cd15BigRoll();
        BeanUtils.copyProperties(dto, entity);
        cd15BigRollService.saveBigRoll(entity);
        return AjaxResult.success();
    }

    @ApiOperation("根据code判断钢压大卷代号是否已经存在")
    @PostMapping("/checkBigRollCodeUnique")
    public String checkBigRollCodeUnique(@RequestBody Cd15BigRollDto dto) {
        return cd15BigRollService.checkBigRollCodeUnique(dto);
    }

    @Log(title = "ui.cd15.bigRoll.column.modalName", businessType = BusinessType.DELETE)
    @ApiOperation("批量删除钢压大卷信息信息(逻辑删)")
    @PostMapping("/deleteBigRoll/{ids}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id數組", paramType = "query")
    })
    public AjaxResult deleteBigRoll(@PathVariable("ids") Long[] ids) {
        cd15BigRollService.deleteBigRoll(ids);
        return AjaxResult.success();
    }

    @Log(title = "ui.cd15.bigRoll.column.modalName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData")
    public List<Cd15BigRollDto> exportData(@RequestBody Cd15BigRollDto dto) {
        dto.setOrderStr(orderStr());
        List<Cd15BigRollDto> list = cd15BigRollService.listBigRoll(dto);
        return list;
    }

    @Log(title = "ui.cd15.bigRoll.column.modalName", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    @ApiOperation("导入15度裁断钢压大卷信息")
    public AjaxResult importData(@RequestBody List<Cd15BigRollDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (StringUtils.isNull(list) || list.size() == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return cd15BigRollService.importData(list, updateSupport, importLogId);
    }
}
