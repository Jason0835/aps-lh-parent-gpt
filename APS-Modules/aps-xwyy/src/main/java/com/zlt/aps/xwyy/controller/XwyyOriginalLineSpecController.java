package com.zlt.aps.xwyy.controller;


import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.xwyy.api.domain.entity.XwyyOriginalLineSpec;
import com.zlt.aps.xwyy.service.XwyyOriginalLineSpecService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@Api(tags = {"原线规格管理接口"})
@RestController
@RequestMapping("/originalLineSpec")
public class XwyyOriginalLineSpecController extends BaseController {

    @Resource
    private XwyyOriginalLineSpecService xwyyOriginalLineSpecService;

    @ApiOperation("根据条件查询原线规格管理列表")
    @GetMapping("/listOriginalLineSpec")
    public TableDataInfo listOriginalLineSpec(XwyyOriginalLineSpec dto) {
        startPage();
        dto.setOrderStr(orderStr());
        List<XwyyOriginalLineSpec> list = xwyyOriginalLineSpecService.listOriginalLineSpec(dto);
        return getDataTable(list);
    }

    @ApiOperation("根据id查询原线规格管理信息")
    @GetMapping("/getOriginalLineSpec/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public XwyyOriginalLineSpec getOriginalLineSpec(@PathVariable("id") Long id) {
        XwyyOriginalLineSpec dto = new XwyyOriginalLineSpec();
        BeanUtils.copyProperties(xwyyOriginalLineSpecService.getById(id), dto);
        return dto;
    }

    @Log(title = "ui.xwyy.originalLineSpec.column.modalName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存原线规格管理信息（id为空则新增，id不为空则修改）")
    @PostMapping("/saveOriginalLineSpec")
    public AjaxResult saveOriginalLineSpec(@RequestBody XwyyOriginalLineSpec dto) {
        if (UserConstants.NOT_UNIQUE.equals(xwyyOriginalLineSpecService.checkOriginalLineSpecCodeUnique(dto))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.xwyy.originalLineSpec.alter.isAssistSpecExist"));
        }
        xwyyOriginalLineSpecService.saveOriginalLineSpec(dto);
        return AjaxResult.success();
    }

    @ApiOperation("根据code判断代号是否已经存在")
    @PostMapping("/checkOriginalLineSpecCodeUnique")
    public String checkOriginalLineSpecCodeUnique(@RequestBody XwyyOriginalLineSpec dto) {
        return xwyyOriginalLineSpecService.checkOriginalLineSpecCodeUnique(dto);
    }

    @Log(title = "ui.xwyy.originalLineSpec.column.modalName", businessType = BusinessType.DELETE)
    @ApiOperation("批量删除原线规格管理信息(逻辑删)")
    @PostMapping("/deleteOriginalLineSpec/{ids}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id數組", paramType = "query")
    })
    public AjaxResult deleteOriginalLineSpec(@PathVariable("ids") Long[] ids) {
        xwyyOriginalLineSpecService.deleteOriginalLineSpec(ids);
        return AjaxResult.success();
    }

    @Log(title = "ui.xwyy.originalLineSpec.column.modalName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @GetMapping("/exportData")
    public List<XwyyOriginalLineSpec> exportData(XwyyOriginalLineSpec dto) {
        dto.setOrderStr(orderStr());
        List<XwyyOriginalLineSpec> list = xwyyOriginalLineSpecService.listOriginalLineSpec(dto);
        return list;
    }

    @Log(title = "ui.xwyy.originalLineSpec.column.modalName", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    @ApiOperation("导入原线规格管理")
    public AjaxResult importData(@RequestBody List<XwyyOriginalLineSpec> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (StringUtils.isNull(list) || list.size() == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return xwyyOriginalLineSpecService.importData(list, updateSupport, importLogId);
    }
}
