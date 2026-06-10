package com.zlt.aps.dj.controller;


import java.util.List;

import javax.annotation.Resource;

import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.dj.api.domain.entity.DjAssistSpec;
import com.zlt.aps.dj.service.DjAssistSpecService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

@Api(tags = {"垫胶外协规格管理接口"})
@RestController
@RequestMapping("/dj/assistSpec")
public class DjAssistSpecController extends BaseController {

    @Resource
    private DjAssistSpecService ncAssistSpecService;

    @ApiOperation("根据条件查询外协规格管理列表")
    @PostMapping("/listAssistSpec")
    public TableDataInfo listAssistSpec(@RequestBody DjAssistSpec dto) {
        startPage();
        dto.setOrderStr(orderStr());
        List<DjAssistSpec> list = ncAssistSpecService.listAssistSpec(dto);
        return getDataTable(list);
    }

    @ApiOperation("根据id查询外协规格管理信息")
    @GetMapping("/getAssistSpec/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public DjAssistSpec getAssistSpec(@PathVariable("id") Long id) {
        DjAssistSpec dto = new DjAssistSpec();
        BeanUtils.copyProperties(ncAssistSpecService.getById(id), dto);
        return dto;
    }

    @Log(title = "ui.nc.assistSpec.column.modalName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存外协规格管理信息（id为空则新增，id不为空则修改）")
    @PostMapping("/saveAssistSpec")
    public AjaxResult saveAssistSpec(@RequestBody DjAssistSpec dto) {
        DjAssistSpec entity = new DjAssistSpec();
        BeanUtils.copyProperties(dto, entity);
        ncAssistSpecService.saveAssistSpec(entity);
        return AjaxResult.success();
    }

    @ApiOperation("根据code判断代号是否已经存在")
    @PostMapping("/checkAssistSpecCodeUnique")
    public String checkAssistSpecCodeUnique(@RequestBody DjAssistSpec dto) {
        return ncAssistSpecService.checkAssistSpecCodeUnique(dto);
    }

    @Log(title = "ui.nc.assistSpec.column.modalName", businessType = BusinessType.DELETE)
    @ApiOperation("批量删除外协规格管理信息(逻辑删)")
    @PostMapping("/deleteAssistSpec/{ids}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id數組", paramType = "query")
    })
    public AjaxResult deleteAssistSpec(@PathVariable("ids") Long[] ids) {
        ncAssistSpecService.deleteAssistSpec(ids);
        return AjaxResult.success();
    }

    @Log(title = "ui.nc.assistSpec.column.modalName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData")
    public List<DjAssistSpec> exportData(@RequestBody DjAssistSpec dto) {
        dto.setOrderStr(orderStr());
        List<DjAssistSpec> list = ncAssistSpecService.listAssistSpec(dto);
        return list;
    }

    @Log(title = "ui.nc.assistSpec.column.modalName", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    @ApiOperation("导入垫胶外协规格管理")
    public AjaxResult importData(@RequestBody List<DjAssistSpec> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (StringUtils.isNull(list) || list.size() == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return ncAssistSpecService.importData(list, updateSupport, importLogId);
    }
}
