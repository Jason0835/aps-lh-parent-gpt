package com.zlt.aps.tq.controller;


import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.exception.CustomException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.tq.api.domain.dto.TqMouthPlateDto;
import com.zlt.aps.tq.entity.TqMouthPlate;
import com.zlt.aps.tq.service.TqMouthPlateService;
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
 * 胎圈口型板信息维护 前端控制器
 * </p>
 *
 * @author chen
 * @since 2021-06-08
 */
@RestController
@RequestMapping("/tq/mouthPlate")
public class TqMouthPlateController extends BaseController {

    @Autowired
    private TqMouthPlateService tqMouthPlateService;

    /**
     * 查询胎圈口型板信息维护列表
     */
    @PostMapping("/list")
    @ApiOperation("查询胎圈口型板信息维护列表")
    public TableDataInfo list(@RequestBody TqMouthPlateDto dto) {
        //多表联查指定排序
        startPage();
        dto.setOrderStr(orderStr());
        TqMouthPlate mouthPlate = new TqMouthPlate();
        BeanUtils.copyProperties(dto, mouthPlate);
        List<TqMouthPlateDto> list = tqMouthPlateService.selectMouthPlateList(mouthPlate);
        return getDataTable(list);
    }

    /**
     * 根据id获取胎圈口型板信息维护详细信息
     *
     * @return 查询到的口型板信息
     */
    @GetMapping(value = "/{id}")
    @ApiOperation("获取胎圈口型板信息详细信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public TqMouthPlateDto getInfo(@PathVariable("id") Long id) {
        TqMouthPlate mouthPlate = tqMouthPlateService.selectMouthPlateById(id);
        TqMouthPlateDto dto = new TqMouthPlateDto();
        BeanUtils.copyProperties(mouthPlate, dto);
        return dto;
    }

    /**
     * 保存胎圈口型板信息维护
     */
    @Log(title = "ui.data.column.tq.mouthPlate.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @PostMapping("/edit")
    @ApiOperation("保存胎圈口型板信息（id为空则新增，id不为空则修改）")
    public AjaxResult edit(@Validated @RequestBody TqMouthPlateDto dto) {
        TqMouthPlate mouthPlate = new TqMouthPlate();
        BeanUtils.copyProperties(dto, mouthPlate);
        tqMouthPlateService.saveMouthPlate(mouthPlate);
        return AjaxResult.success();
    }

    /**
     * 删除胎圈口型板信息维护
     */
    @Log(title = "ui.data.column.tq.mouthPlate.modelName", businessType = BusinessType.DELETE)
    @PostMapping("/{ids}")
    @ApiOperation("删除胎圈口型板信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id数组", paramType = "query")
    })
    public AjaxResult remove(@PathVariable("ids") Long[] ids) {
        tqMouthPlateService.deleteMouthPlateByIds(ids);
        return AjaxResult.success();
    }


    @Log(title = "ui.data.column.tq.mouthPlate.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除全部(逻辑删)")
    @PostMapping("/deleteAll")
    public AjaxResult deleteAll() {
        tqMouthPlateService.deleteAll();
        return AjaxResult.success();
    }


    /**
     * 导出胎面口型板信息
     */
    @Log(title = "ui.data.column.tq.mouthPlate.modelName", businessType = BusinessType.EXPORT)
    @PostMapping("/exportData")
    @ApiOperation("导出胎面口型板信息")
    public List<TqMouthPlateDto> exportData(@SpringQueryMap TqMouthPlateDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        TqMouthPlate mouthPlate = new TqMouthPlate();
        BeanUtils.copyProperties(dto, mouthPlate);
        return tqMouthPlateService.selectMouthPlateList(mouthPlate);
    }

    @Log(title = "ui.data.column.tq.mouthPlate.modelName", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    @ApiOperation("导入胎面口型板信息")
    public AjaxResult importData(@RequestBody List<TqMouthPlateDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (StringUtils.isNull(list) || list.size() == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return tqMouthPlateService.importData(list, updateSupport, importLogId);
    }
}
