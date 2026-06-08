package com.zlt.aps.dj.controller;


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
import com.zlt.aps.dj.api.domain.dto.DjCurlRollDto;
import com.zlt.aps.dj.api.domain.entity.DjCurlRoll;
import com.zlt.aps.dj.service.DjCurlRollService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

@Api(tags = {"垫胶卷曲信息接口"})
@RestController
@RequestMapping("/dj/curlRoll")
public class DjCurlRollController extends BaseController {

    @Resource
    private DjCurlRollService ncCurlRollService;

    @ApiOperation("根据条件查询垫胶卷曲信息列表")
    @PostMapping("/listCurlRoll")
    public TableDataInfo listCurlRoll(@RequestBody DjCurlRoll dto) {
        startPage();
        dto.setOrderStr(orderStr());
        List<DjCurlRoll> list = ncCurlRollService.listCurlRoll(dto);
        return getDataTable(list);
    }

    @ApiOperation("根据id查询垫胶卷曲信息信息")
    @GetMapping("/getCurlRoll/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public DjCurlRoll getCurlRoll(@PathVariable("id") Long id) {
        DjCurlRoll dto = new DjCurlRoll();
        BeanUtils.copyProperties(ncCurlRollService.getById(id), dto);
        return dto;
    }

    @Log(title = "ui.nc.curlRoll.column.modalName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存垫胶卷曲信息信息（id为空则新增，id不为空则修改）")
    @PostMapping("/saveCurlRoll")
    public AjaxResult saveCurlRoll(@RequestBody DjCurlRoll dto) {
        DjCurlRoll entity = new DjCurlRoll();
        BeanUtils.copyProperties(dto, entity);
        ncCurlRollService.saveCurlRoll(entity);
        return AjaxResult.success();
    }

    @ApiOperation("根据code判断垫胶卷曲代号是否已经存在")
    @PostMapping("/checkCurlRollCodeUnique")
    public String checkCurlRollCodeUnique(@RequestBody DjCurlRoll dto) {
        return ncCurlRollService.checkCurlRollCodeUnique(dto);
    }

    @Log(title = "ui.nc.curlRoll.column.modalName", businessType = BusinessType.DELETE)
    @ApiOperation("批量删除垫胶卷曲信息信息(逻辑删)")
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
    @PostMapping("/exportData")
    public List<DjCurlRoll> exportData(@RequestBody DjCurlRoll dto) {
        dto.setOrderStr(orderStr());
        List<DjCurlRoll> list = ncCurlRollService.listCurlRoll(dto);
        return list;
    }

    @Log(title = "ui.nc.curlRoll.column.modalName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<DjCurlRollDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return ncCurlRollService.importData(list, updateSupport, importLogId);
    }

    /**
     * 根据编号查询卷曲长度
     *
     * @param curlRoll 查询条件
     * @return 结果
     */
    @ApiOperation("根据编号查询卷曲长度")
    @PostMapping("/selectCurlLengthByCode")
    public AjaxResult selectCurlLengthByCode(@RequestBody DjCurlRoll curlRoll) {
        return ncCurlRollService.selectCurlLengthByCode(curlRoll);
    }
}
