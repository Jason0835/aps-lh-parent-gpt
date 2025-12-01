package com.zlt.aps.tm.controller;


import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.tm.api.domain.dto.TmMouthPlateDto;
import com.zlt.aps.tm.entity.TmMouthPlate;
import com.zlt.aps.tm.service.TmMouthPlateService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 胎面口型板信息维护 前端控制器
 * </p>
 *
 * @author chenxueyuan
 * @since 2021-05-27
 */
@RestController
@RequestMapping("/tm/mouthPlate")
public class TmMouthPlateController extends BaseController {

    @Autowired
    private TmMouthPlateService tmMouthPlateService;

    /**
     * 查询胎面口型板信息维护列表
     */
    @PostMapping("/list")
    @ApiOperation("查询胎面口型板信息维护列表")
    public TableDataInfo list(@RequestBody TmMouthPlateDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        TmMouthPlate tmMouthPlate = new TmMouthPlate();
        BeanUtils.copyProperties(dto, tmMouthPlate);
        List<TmMouthPlateDto> list = tmMouthPlateService.selectMouthPlateList(tmMouthPlate);
        return getDataTable(list);
    }

    /**
     * 根据id获取胎面口型板信息维护详细信息
     *
     * @return 查询到的口型板信息
     */
    @GetMapping(value = "/{id}")
    @ApiOperation("获取胎面口型板信息详细信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public TmMouthPlateDto getInfo(@PathVariable("id") Long id) {
        TmMouthPlate tmMouthPlate = tmMouthPlateService.selectTmMouthPlateById(id);
        TmMouthPlateDto dto = new TmMouthPlateDto();
        BeanUtils.copyProperties(tmMouthPlate, dto);
        return dto;
    }

    /**
     * 保存胎面口型板信息维护
     */
    @Log(title = "ui.data.column.tm.mouthPlate.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @PostMapping("/edit")
    @ApiOperation("保存胎面口型板信息（id为空则新增，id不为空则修改）")
    public AjaxResult edit(@Validated @RequestBody TmMouthPlateDto dto) {
        TmMouthPlate tmMouthPlate = new TmMouthPlate();
        BeanUtils.copyProperties(dto, tmMouthPlate);
        tmMouthPlateService.saveTmMouthPlate(tmMouthPlate);
        return AjaxResult.success();
    }

    /**
     * 删除胎面口型板信息维护
     */
    @Log(title = "ui.data.column.tm.mouthPlate.modelName", businessType = BusinessType.DELETE)
    @PostMapping("/{ids}")
    @ApiOperation("删除胎面口型板信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id数组", paramType = "query")
    })
    public AjaxResult remove(@PathVariable("ids") Long[] ids) {
        tmMouthPlateService.deleteTmMouthPlateByIds(ids);
        return AjaxResult.success();
    }


    @Log(title = "ui.data.column.tm.mouthPlate.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除全部(逻辑删)")
    @PostMapping("/deleteAll")
    public AjaxResult deleteAll() {
        tmMouthPlateService.deleteAll();
        return AjaxResult.success();
    }


    /**
     * 导出胎面口型板信息
     */
    @Log(title = "ui.data.column.tm.mouthPlate.modelName", businessType = BusinessType.EXPORT)
    @PostMapping("/exportData")
    @ApiOperation("导出胎面口型板信息")
    public List<TmMouthPlateDto> exportData(@RequestBody TmMouthPlateDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        TmMouthPlate tmMouthPlate = new TmMouthPlate();
        BeanUtils.copyProperties(dto, tmMouthPlate);
        return tmMouthPlateService.selectMouthPlateList(tmMouthPlate);
    }

    @Log(title = "ui.data.column.tm.mouthPlate.modelName", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    @ApiOperation("导入胎面口型板信息")
    public AjaxResult importData(@RequestBody List<TmMouthPlateDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (StringUtils.isNull(list) || list.size() == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return tmMouthPlateService.importData(list, updateSupport, importLogId);
    }
}
