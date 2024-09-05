package com.zlt.aps.cx.controller;


import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cx.api.domain.dto.ConstructionInfoDto;
import com.zlt.aps.cx.entity.ConstructionInfo;
import com.zlt.aps.cx.service.ConstructionInfoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@Api(tags = {"施工信息接口"})
@RestController
@RequestMapping("/constructionInfo")
public class ConstructionInfoController extends BaseController {

    @Resource
    private ConstructionInfoService constructionInfoService;

    @ApiOperation("根据条件查询施工信息列表")
    @GetMapping("/listConstructionInfo")
    public TableDataInfo listConstructionInfo(ConstructionInfoDto dto) {
        startPage("CREATE_TIME DESC");
        List<ConstructionInfoDto> list = constructionInfoService.listConstructionInfo(dto);
        return getDataTable(list);
    }

    @ApiOperation("根据id查询施工信息")
    @GetMapping("/getConstructionInfo/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public ConstructionInfoDto getConstructionInfo(@PathVariable("id") Long id) {
        ConstructionInfoDto dto = new ConstructionInfoDto();
        BeanUtils.copyProperties(constructionInfoService.getById(id), dto);
        return dto;
    }

    @Log(title = "ui.construction.modalName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存施工信息（id为空则新增，id不为空则修改）")
    @PostMapping("/saveConstructionInfo")
    public AjaxResult saveConstructionInfo(@RequestBody ConstructionInfoDto dto) {
        ConstructionInfo entity = new ConstructionInfo();
        BeanUtils.copyProperties(dto, entity);
        constructionInfoService.saveConstructionInfo(entity);
        return AjaxResult.success();
    }

    @ApiOperation("验证胚胎代码唯一性")
    @PostMapping("/checkEmbryoCodeUnique")
    public String checkEmbryoCodeUnique(@RequestBody ConstructionInfoDto dto) {
        return constructionInfoService.checkEmbryoCodeUnique(dto);
    }

    @Log(title = "ui.construction.modalName", businessType = BusinessType.DELETE)
    @ApiOperation("批量删除施工信息(逻辑删)")
    @PostMapping("/deleteConstructionInfo/{ids}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id數組", paramType = "query")
    })
    public AjaxResult deleteConstructionInfo(@PathVariable("ids") Long[] ids) {
        constructionInfoService.deleteConstructionInfo(ids);
        return AjaxResult.success();
    }

    @Log(title = "ui.construction.modalName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @GetMapping("/exportData")
    public List<ConstructionInfoDto> exportData(ConstructionInfoDto dto) {
        List<ConstructionInfoDto> list = constructionInfoService.listConstructionInfo(dto);
        return list;
    }

    @Log(title = "ui.construction.modalName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<ConstructionInfoDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return constructionInfoService.importData(list, updateSupport, importLogId);
    }
}
