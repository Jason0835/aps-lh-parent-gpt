package com.zlt.aps.gdyy.controller;


import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.exception.CustomException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.gdyy.api.domain.dto.GdyySteelRollColorDto;
import com.zlt.aps.gdyy.entity.GdyySteelRollColor;
import com.zlt.aps.gdyy.service.GdyySteelRollColorService;
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
 * 钢带压延颜色信息维护 前端控制器
 * </p>
 *
 * @author duanjuntao
 * @since 2021-06-10
 */
@Api(tags = {"钢带大卷颜色提示信息接口"})
@RestController
@RequestMapping("/gdyySteelRollColor")
public class GdyySteelRollColorController extends BaseController {

    @Resource
    public GdyySteelRollColorService gdyySteelRollColorService;

    @ApiOperation("根据条件查询钢带大卷颜色提示信息列表")
    @PostMapping("/listGdyySteelRollColor")
    public TableDataInfo listGdyySteelRollColor(@RequestBody GdyySteelRollColorDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        List<GdyySteelRollColorDto> list = gdyySteelRollColorService.listGdyySteelRollColor(dto);
        return getDataTable(list);
    }

    @ApiOperation("根据id查询帘布大卷颜色提示信息")
    @GetMapping("/getGdyySteelRollColor/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public GdyySteelRollColorDto getGdyySteelRollColor(@PathVariable("id") Long id) {
        GdyySteelRollColorDto dto = new GdyySteelRollColorDto();
        BeanUtils.copyProperties(gdyySteelRollColorService.getById(id), dto);
        return dto;
    }

    @Log(title = "ui.steelRollColor.column.modalName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存钢带大卷颜色提示信息（id为空则新增，id不为空则修改）")
    @PostMapping("/saveGdyySteelRollColor")
    public AjaxResult saveGdyySteelRollColor(@RequestBody GdyySteelRollColorDto dto) {
        GdyySteelRollColor entity = new GdyySteelRollColor();
        BeanUtils.copyProperties(dto, entity);
        gdyySteelRollColorService.saveGdyySteelRollColor(entity);
        return AjaxResult.success();
    }

    @ApiOperation("根据code判断主键是否已经存在")
    @PostMapping("/checkGdyySteelRollColor")
    public String checkGdyySteelRollColor(@RequestBody GdyySteelRollColorDto dto) {
        return gdyySteelRollColorService.checkGdyySteelRollColor(dto);
    }

    @Log(title = "ui.steelRollColor.column.modalName", businessType = BusinessType.DELETE)
    @ApiOperation("批量删除钢带大卷颜色提示信息(逻辑删)")
    @PostMapping("/deleteGdyySteelRollColor/{ids}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id數組", paramType = "query")
    })
    public AjaxResult deleteGdyySteelRollColor(@PathVariable("ids") Long[] ids) {
        gdyySteelRollColorService.deleteGdyySteelRollColor(ids);
        return AjaxResult.success();
    }

    @Log(title = "ui.steelRollColor.column.modalName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData")
    public List<GdyySteelRollColorDto> exportData(@RequestBody GdyySteelRollColorDto dto) {
        dto.setOrderStr(orderStr());
        List<GdyySteelRollColorDto> list = gdyySteelRollColorService.listGdyySteelRollColor(dto);
        return list;
    }

    @Log(title = "ui.steelRollColor.column.modalName", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    @ApiOperation("导入钢带大卷颜色提示信息")
    public AjaxResult importData(@RequestBody List<GdyySteelRollColorDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (StringUtils.isNull(list) || list.size() == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return gdyySteelRollColorService.importData(list, updateSupport, importLogId);
    }
}
