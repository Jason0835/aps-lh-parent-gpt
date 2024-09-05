package com.zlt.aps.xwyy.controller;


import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.xwyy.api.domain.dto.XwyyBigRollColorDto;
import com.zlt.aps.xwyy.entity.XwyyBigRollColor;
import com.zlt.aps.xwyy.service.XwyyBigRollColorService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 * 帘布大卷颜色信息维护 前端控制器
 * </p>
 *
 * @author duanjuntao
 * @since 2021-06-07
 */
@Api(tags = {"帘布大卷颜色提示信息接口"})
@RestController
@RequestMapping("/xwyyBigRollColor")
public class XwyyBigRollColorController extends BaseController {

    @Resource
    public XwyyBigRollColorService xwyyBigRollColorService;

    @ApiOperation("根据条件查询帘布大卷颜色提示信息列表")
    @PostMapping("/listXwyyBigRollColor")
    public TableDataInfo listXwyyBigRollColor(@RequestBody XwyyBigRollColorDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        List<XwyyBigRollColorDto> list = xwyyBigRollColorService.listXwyyBigRollColor(dto);
        return getDataTable(list);
    }

    @ApiOperation("根据id查询帘布大卷颜色提示信息")
    @GetMapping("/getXwyyBigRollColor/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public XwyyBigRollColorDto getXwyyBigRollColor(@PathVariable("id") Long id) {
        XwyyBigRollColorDto dto = new XwyyBigRollColorDto();
        BeanUtils.copyProperties(xwyyBigRollColorService.getById(id), dto);
        return dto;
    }

    @Log(title = "ui.bigRollColor.column.modalName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存帘布大卷颜色提示信息（id为空则新增，id不为空则修改）")
    @PostMapping("/saveXwyyBigRollColor")
    public AjaxResult saveXwyyBigRollColor(@RequestBody XwyyBigRollColorDto dto) {
        XwyyBigRollColor entity = new XwyyBigRollColor();
        BeanUtils.copyProperties(dto, entity);
        xwyyBigRollColorService.saveXwyyBigRollColor(entity);
        return AjaxResult.success();
    }

    @ApiOperation("根据code判断主键是否已经存在")
    @PostMapping("/checkXwyyBigRollColor")
    public String checkXwyyBigRollColor(@RequestBody XwyyBigRollColorDto dto) {
        return xwyyBigRollColorService.checkXwyyBigRollColor(dto);
    }

    @Log(title = "ui.bigRollColor.column.modalName", businessType = BusinessType.DELETE)
    @ApiOperation("批量删除帘布大卷颜色提示信息(逻辑删)")
    @PostMapping("/deleteXwyyBigRollColor/{ids}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id數組", paramType = "query")
    })
    public AjaxResult deleteXwyyBigRollColor(@PathVariable("ids") Long[] ids) {
        xwyyBigRollColorService.deleteXwyyBigRollColor(ids);
        return AjaxResult.success();
    }

    @Log(title = "ui.bigRollColor.column.modalName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @GetMapping("/exportData")
    public List<XwyyBigRollColorDto> exportData(XwyyBigRollColorDto dto) {
        dto.setOrderStr(orderStr());
        List<XwyyBigRollColorDto> list = xwyyBigRollColorService.listXwyyBigRollColor(dto);
        return list;
    }

    @Log(title = "ui.bigRollColor.column.modalName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<XwyyBigRollColorDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return xwyyBigRollColorService.importData(list, updateSupport, importLogId);
    }
}
