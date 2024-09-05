package com.zlt.aps.tm.controller;


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
import com.zlt.aps.tm.api.domain.dto.TmCurlRollDto;
import com.zlt.aps.tm.api.domain.entity.TmCurlRoll;
import com.zlt.aps.tm.service.TmCurlRollService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

@Api(tags = {"胎面卷曲信息接口"})
@RestController
@RequestMapping("/curlRoll")
public class TmCurlRollController extends BaseController {

    @Resource
    private TmCurlRollService tmCurlRollService;

    @ApiOperation("根据条件查询胎面卷曲信息列表")
    @GetMapping("/listCurlRoll")
    public TableDataInfo listCurlRoll(TmCurlRoll dto) {
        startPage();
        dto.setOrderStr(orderStr());
        List<TmCurlRoll> list = tmCurlRollService.listCurlRoll(dto);
        return getDataTable(list);
    }

    @ApiOperation("根据id查询胎面卷曲信息信息")
    @GetMapping("/getCurlRoll/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public TmCurlRoll getCurlRoll(@PathVariable("id") Long id) {
        TmCurlRoll dto = new TmCurlRoll();
        BeanUtils.copyProperties(tmCurlRollService.getById(id), dto);
        return dto;
    }

    @Log(title = "ui.tm.curlRoll.column.modalName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存胎面卷曲信息信息（id为空则新增，id不为空则修改）")
    @PostMapping("/saveCurlRoll")
    public AjaxResult saveCurlRoll(@RequestBody TmCurlRoll dto) {
        TmCurlRoll entity = new TmCurlRoll();
        BeanUtils.copyProperties(dto, entity);
        tmCurlRollService.saveCurlRoll(entity);
        return AjaxResult.success();
    }

    @ApiOperation("根据code判断胎面卷曲代号是否已经存在")
    @PostMapping("/checkCurlRollCodeUnique")
    public String checkCurlRollCodeUnique(@RequestBody TmCurlRoll dto) {
        return tmCurlRollService.checkCurlRollCodeUnique(dto);
    }

    @Log(title = "ui.tm.curlRoll.column.modalName", businessType = BusinessType.DELETE)
    @ApiOperation("批量删除胎面卷曲信息信息(逻辑删)")
    @PostMapping("/deleteCurlRoll/{ids}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id數組", paramType = "query")
    })
    public AjaxResult deleteCurlRoll(@PathVariable("ids") Long[] ids) {
        tmCurlRollService.deleteCurlRoll(ids);
        return AjaxResult.success();
    }

    @Log(title = "ui.tm.curlRoll.column.modalName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @GetMapping("/exportData")
    public List<TmCurlRoll> exportData(TmCurlRoll dto) {
        dto.setOrderStr(orderStr());
        List<TmCurlRoll> list = tmCurlRollService.listCurlRoll(dto);
        return list;
    }

    @Log(title = "ui.tm.curlRoll.column.modalName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<TmCurlRollDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return tmCurlRollService.importData(list, updateSupport, importLogId);
    }
}
