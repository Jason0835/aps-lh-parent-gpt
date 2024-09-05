package com.zlt.aps.nc.controller;


import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.collections4.CollectionUtils;
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
import com.zlt.aps.nc.api.domain.dto.NcCurlRollDto;
import com.zlt.aps.nc.api.domain.entity.NcCurlRoll;
import com.zlt.aps.nc.service.NcCurlRollService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

@Api(tags = {"内衬卷曲信息接口"})
@RestController
@RequestMapping("/curlRoll")
public class NcCurlRollController extends BaseController {

    @Resource
    private NcCurlRollService ncCurlRollService;

    @ApiOperation("根据条件查询内衬卷曲信息列表")
    @GetMapping("/listCurlRoll")
    public TableDataInfo listCurlRoll(NcCurlRoll dto) {
        startPage();
        dto.setOrderStr(orderStr());
        List<NcCurlRoll> list = ncCurlRollService.listCurlRoll(dto);
        return getDataTable(list);
    }

    @ApiOperation("根据id查询内衬卷曲信息信息")
    @GetMapping("/getCurlRoll/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public NcCurlRoll getCurlRoll(@PathVariable("id") Long id) {
        NcCurlRoll dto = new NcCurlRoll();
        BeanUtils.copyProperties(ncCurlRollService.getById(id), dto);
        return dto;
    }

    @Log(title = "ui.nc.curlRoll.column.modalName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存内衬卷曲信息信息（id为空则新增，id不为空则修改）")
    @PostMapping("/saveCurlRoll")
    public AjaxResult saveCurlRoll(@RequestBody NcCurlRoll dto) {
        NcCurlRoll entity = new NcCurlRoll();
        BeanUtils.copyProperties(dto, entity);
        ncCurlRollService.saveCurlRoll(entity);
        return AjaxResult.success();
    }

    @ApiOperation("根据code判断内衬卷曲代号是否已经存在")
    @PostMapping("/checkCurlRollCodeUnique")
    public String checkCurlRollCodeUnique(@RequestBody NcCurlRoll dto) {
        return ncCurlRollService.checkCurlRollCodeUnique(dto);
    }

    @Log(title = "ui.nc.curlRoll.column.modalName", businessType = BusinessType.DELETE)
    @ApiOperation("批量删除内衬卷曲信息信息(逻辑删)")
    @PostMapping("/deleteCurlRoll/{ids}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id數組", paramType = "query")
    })
    public AjaxResult deleteCurlRoll(@PathVariable("ids") Long[] ids) {
        ncCurlRollService.deleteCurlRoll(ids);
        return AjaxResult.success();
    }

    @Log(title = "ui.nc.curlRoll.column.modalName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @GetMapping("/exportData")
    public List<NcCurlRoll> exportData(NcCurlRoll dto) {
        dto.setOrderStr(orderStr());
        List<NcCurlRoll> list = ncCurlRollService.listCurlRoll(dto);
        return list;
    }

    @Log(title = "ui.nc.curlRoll.column.modalName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<NcCurlRollDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return ncCurlRollService.importData(list, updateSupport, importLogId);
    }
}
