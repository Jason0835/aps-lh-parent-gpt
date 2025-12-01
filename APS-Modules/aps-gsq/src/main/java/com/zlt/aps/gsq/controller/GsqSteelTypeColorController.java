package com.zlt.aps.gsq.controller;


import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.gsq.api.domain.dto.GsqSteelTypeColorDto;
import com.zlt.aps.gsq.entity.GsqSteelTypeColor;
import com.zlt.aps.gsq.service.GsqSteelTypeColorService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 * 钢丝圈颜色信息维护 前端控制器
 * </p>
 */
@Api(tags = {"钢丝圈颜色提示信息接口"})
@RestController
@RequestMapping("/gsqSteelTypeColor")
public class GsqSteelTypeColorController extends BaseController {

    @Resource
    public GsqSteelTypeColorService gsqSteelTypeColorService;

    @ApiOperation("根据条件查询钢丝圈颜色提示信息列表")
    @PostMapping("/listGsqSteelTypeColor")
    public TableDataInfo listGsqSteelTypeColor(@RequestBody GsqSteelTypeColorDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        List<GsqSteelTypeColorDto> list = gsqSteelTypeColorService.listGsqSteelTypeColor(dto);
        return getDataTable(list);
    }

    @ApiOperation("根据id查询钢丝圈颜色提示信息")
    @GetMapping("/getGsqSteelTypeColor/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public GsqSteelTypeColorDto getGsqSteelTypeColor(@PathVariable("id") Long id) {
        GsqSteelTypeColorDto dto = new GsqSteelTypeColorDto();
        BeanUtils.copyProperties(gsqSteelTypeColorService.getById(id), dto);
        return dto;
    }

    @Log(title = "ui.steelRollColor.column.modalName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存钢丝圈颜色提示信息（id为空则新增，id不为空则修改）")
    @PostMapping("/saveGsqSteelTypeColor")
    public AjaxResult saveGsqSteelTypeColor(@RequestBody GsqSteelTypeColorDto dto) {
        GsqSteelTypeColor entity = new GsqSteelTypeColor();
        BeanUtils.copyProperties(dto, entity);
        gsqSteelTypeColorService.saveGsqSteelTypeColor(entity);
        return AjaxResult.success();
    }

    @ApiOperation("根据code判断主键是否已经存在")
    @PostMapping("/checkGsqSteelTypeColor")
    public String checkGsqSteelTypeColor(@RequestBody GsqSteelTypeColorDto dto) {
        return gsqSteelTypeColorService.checkGsqSteelTypeColor(dto);
    }

    @Log(title = "ui.steelRollColor.column.modalName", businessType = BusinessType.DELETE)
    @ApiOperation("批量删除钢丝圈颜色提示信息(逻辑删)")
    @PostMapping("/deleteGsqSteelTypeColor/{ids}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id數組", paramType = "query")
    })
    public AjaxResult deleteGsqSteelTypeColor(@PathVariable("ids") Long[] ids) {
        gsqSteelTypeColorService.deleteGsqSteelTypeColor(ids);
        return AjaxResult.success();
    }

    @Log(title = "ui.steelRollColor.column.modalName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData")
    public List<GsqSteelTypeColorDto> exportData(@RequestBody GsqSteelTypeColorDto dto) {
        dto.setOrderStr(orderStr());
        List<GsqSteelTypeColorDto> list = gsqSteelTypeColorService.listGsqSteelTypeColor(dto);
        return list;
    }

    @Log(title = "ui.steelRollColor.column.modalName", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    @ApiOperation("导入钢丝圈颜色提示信息")
    public AjaxResult importData(@RequestBody List<GsqSteelTypeColorDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (StringUtils.isNull(list) || list.size() == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return gsqSteelTypeColorService.importData(list, updateSupport, importLogId);
    }
}
