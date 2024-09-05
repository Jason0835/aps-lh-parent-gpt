package com.zlt.aps.gdyy.controller;


import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.exception.CustomException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.gdyy.api.domain.dto.GdyyMattersAttentionDto;
import com.zlt.aps.gdyy.entity.GdyyMattersAttention;
import com.zlt.aps.gdyy.service.GdyyMattersAttentionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 钢带压延注意事项维护 前端控制器
 * </p>
 *
 * @author duanjuntao
 * @since 2021-06-10
 */
@Api(tags = {"钢带大卷注意事项信息接口"})
@RestController
@RequestMapping("/gdyyMattersAttention")
public class GdyyMattersAttentionController extends BaseController {

    @Resource
    public GdyyMattersAttentionService gdyyMattersAttentionService;

    @ApiOperation("根据条件查询钢带大卷注意事项信息列表")
    @PostMapping("/listGdyyMattersAttention")
    public TableDataInfo listGdyyMattersAttention(@RequestBody GdyyMattersAttentionDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        List<GdyyMattersAttentionDto> list = gdyyMattersAttentionService.listGdyyMattersAttention(dto);
        return getDataTable(list);
    }

    @ApiOperation("根据id查询帘布大卷注意事项信息")
    @GetMapping("/getGdyyMattersAttention/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public GdyyMattersAttentionDto getGdyyMattersAttention(@PathVariable("id") Long id) {
        GdyyMattersAttentionDto dto = new GdyyMattersAttentionDto();
        BeanUtils.copyProperties(gdyyMattersAttentionService.getById(id), dto);
        return dto;
    }

    @Log(title = "ui.data.column.gdyy.params.mattersAttentionName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存钢带大卷注意事项信息（id为空则新增，id不为空则修改）")
    @PostMapping("/saveGdyyMattersAttention")
    public AjaxResult saveGdyyMattersAttention(@RequestBody GdyyMattersAttentionDto dto) {
        GdyyMattersAttention entity = new GdyyMattersAttention();
        BeanUtils.copyProperties(dto, entity);
        gdyyMattersAttentionService.saveGdyyMattersAttention(entity);
        return AjaxResult.success();
    }

    @Log(title = "ui.data.column.gdyy.params.mattersAttentionName", businessType = BusinessType.DELETE)
    @ApiOperation("批量删除钢带大卷注意事项信息(逻辑删)")
    @PostMapping("/deleteGdyyMattersAttention/{ids}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id數組", paramType = "query")
    })
    public AjaxResult deleteGdyyMattersAttention(@PathVariable("ids") Long[] ids) {
        gdyyMattersAttentionService.deleteGdyyMattersAttention(ids);
        return AjaxResult.success();
    }

    @Log(title = "ui.data.column.gdyy.params.mattersAttentionName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData")
    public List<GdyyMattersAttentionDto> exportData(@RequestBody GdyyMattersAttentionDto dto) {
        dto.setOrderStr(orderStr());
        List<GdyyMattersAttentionDto> list = gdyyMattersAttentionService.listGdyyMattersAttention(dto);
        return list;
    }

    @Log(title = "ui.data.column.gdyy.params.mattersAttentionName", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    @ApiOperation("导入钢带压延注意事项信息")
    public AjaxResult importData(@RequestBody List<GdyyMattersAttentionDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (StringUtils.isNull(list) || list.size() == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        List<GdyyMattersAttention> settingList = new ArrayList<>();
        for (GdyyMattersAttentionDto dto : list) {
            GdyyMattersAttention setting = new GdyyMattersAttention();
            BeanUtils.copyProperties(dto, setting);
            settingList.add(setting);
        }
        return gdyyMattersAttentionService.importData(settingList, updateSupport, importLogId);
    }
}
