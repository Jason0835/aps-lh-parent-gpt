package com.zlt.aps.dj.controller;


import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
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
import com.zlt.aps.dj.api.domain.dto.DjMouthPlateDto;
import com.zlt.aps.dj.api.domain.entity.DjMouthPlate;
import com.zlt.aps.dj.service.DjMouthPlateService;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

/**
 * <p>
 * 垫胶口型板信息维护 前端控制器
 * </p>
 *
 * @author chenxueyuan
 * @since 2021-05-27
 */
@RestController
@RequestMapping("/dj/mouthPlate")
public class DjMouthPlateController extends BaseController {

    @Autowired
    private DjMouthPlateService ncMouthPlateService;

    /**
     * 查询垫胶口型板信息维护列表
     */
    @PostMapping("/list")
    @ApiOperation("查询垫胶口型板信息维护列表")
    public TableDataInfo list(@RequestBody DjMouthPlateDto dto) {
        //多表联查指定排序
        startPage("");
        DjMouthPlate mouthPlate = new DjMouthPlate();
        BeanUtils.copyProperties(dto, mouthPlate);
        startPage();
        mouthPlate.setOrderStr(orderStr());
        List<DjMouthPlateDto> list = ncMouthPlateService.selectMouthPlateList(mouthPlate);
        return getDataTable(list);
    }

    /**
     * 根据id获取垫胶口型板信息维护详细信息
     *
     * @return 查询到的口型板信息
     */
    @GetMapping(value = "/{id}")
    @ApiOperation("获取垫胶口型板信息详细信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public DjMouthPlateDto getInfo(@PathVariable("id") Long id) {
        DjMouthPlate mouthPlate = ncMouthPlateService.selectMouthPlateById(id);
        DjMouthPlateDto dto = new DjMouthPlateDto();
        BeanUtils.copyProperties(mouthPlate, dto);
        return dto;
    }

    /**
     * 保存垫胶口型板信息维护
     */
    @Log(title = "ui.data.column.nc.mouthPlate.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @PostMapping("/edit")
    @ApiOperation("保存垫胶口型板信息（id为空则新增，id不为空则修改）")
    public AjaxResult edit(@Validated @RequestBody DjMouthPlateDto dto) {
        DjMouthPlate mouthPlate = new DjMouthPlate();
        BeanUtils.copyProperties(dto, mouthPlate);
        ncMouthPlateService.saveMouthPlate(mouthPlate);
        return AjaxResult.success();
    }

    /**
     * 删除垫胶口型板信息维护
     */
    @Log(title = "ui.data.column.nc.mouthPlate.modelName", businessType = BusinessType.DELETE)
    @PostMapping("/{ids}")
    @ApiOperation("删除垫胶口型板信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id数组", paramType = "query")
    })
    public AjaxResult remove(@PathVariable("ids") Long[] ids) {
        ncMouthPlateService.deleteMouthPlateByIds(ids);
        return AjaxResult.success();
    }

    /**
     * 导出胎面口型板信息
     */
    @Log(title = "ui.data.column.nc.mouthPlate.modelName", businessType = BusinessType.EXPORT)
    @PostMapping("/exportData")
    @ApiOperation("导出胎面口型板信息")
    public List<DjMouthPlateDto> exportData(@RequestBody DjMouthPlateDto dto) {
        DjMouthPlate mouthPlate = new DjMouthPlate();
        BeanUtils.copyProperties(dto, mouthPlate);
        startPage();
        mouthPlate.setOrderStr(orderStr());
        return ncMouthPlateService.selectMouthPlateList(mouthPlate);
    }

    @Log(title = "ui.data.column.nc.mouthPlate.modelName", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    @ApiOperation("导入胎面口型板信息")
    public AjaxResult importData(@RequestBody List<DjMouthPlateDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (StringUtils.isNull(list) || list.size() == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return ncMouthPlateService.importData(list, updateSupport, importLogId);
    }
}
