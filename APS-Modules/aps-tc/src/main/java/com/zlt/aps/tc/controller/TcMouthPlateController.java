package com.zlt.aps.tc.controller;


import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.tc.api.domain.dto.TcMouthPlateDto;
import com.zlt.aps.tc.entity.TcMouthPlate;
import com.zlt.aps.tc.service.TcMouthPlateService;
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
 * 胎侧口型板信息维护 前端控制器
 * </p>
 *
 * @author chenxueyuan
 * @since 2021-06-02
 */
@RestController
@RequestMapping("/tc/mouthPlate")
public class TcMouthPlateController extends BaseController {

    @Autowired
    private TcMouthPlateService tcMouthPlateService;

    /**
     * 查询胎侧口型板信息维护列表
     */
    @PostMapping("/list")
    @ApiOperation("查询胎侧口型板信息维护列表")
    public TableDataInfo list(@RequestBody TcMouthPlateDto dto) {
        TcMouthPlate mouthPlate = new TcMouthPlate();
        BeanUtils.copyProperties(dto, mouthPlate);
        startPage();
        mouthPlate.setOrderStr(orderStr());
        List<TcMouthPlateDto> list = tcMouthPlateService.selectMouthPlateList(mouthPlate);
        return getDataTable(list);
    }

    /**
     * 根据id获取胎侧口型板信息维护详细信息
     *
     * @return 查询到的口型板信息
     */
    @GetMapping(value = "/{id}")
    @ApiOperation("获取胎侧口型板信息详细信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public TcMouthPlateDto getInfo(@PathVariable("id") Long id) {
        TcMouthPlate mouthPlate = tcMouthPlateService.selectTmMouthPlateById(id);
        TcMouthPlateDto dto = new TcMouthPlateDto();
        BeanUtils.copyProperties(mouthPlate, dto);
        return dto;
    }

    /**
     * 保存胎侧口型板信息维护
     */
    @Log(title = "ui.data.column.tc.mouthPlate.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @PostMapping("/edit")
    @ApiOperation("保存胎侧口型板信息（id为空则新增，id不为空则修改）")
    public AjaxResult edit(@Validated @RequestBody TcMouthPlateDto dto) {
        TcMouthPlate mouthPlate = new TcMouthPlate();
        BeanUtils.copyProperties(dto, mouthPlate);
        tcMouthPlateService.saveTmMouthPlate(mouthPlate);
        return AjaxResult.success();
    }

    /**
     * 删除胎侧口型板信息维护
     */
    @Log(title = "ui.data.column.tc.mouthPlate.modelName", businessType = BusinessType.DELETE)
    @PostMapping("/{ids}")
    @ApiOperation("删除胎侧口型板信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id数组", paramType = "query")
    })
    public AjaxResult remove(@PathVariable("ids") Long[] ids) {
        tcMouthPlateService.deleteTmMouthPlateByIds(ids);
        return AjaxResult.success();
    }


    @Log(title = "ui.data.column.tc.mouthPlate.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除全部(逻辑删)")
    @PostMapping("/deleteAll")
    public AjaxResult deleteAll() {
        tcMouthPlateService.deleteAll();
        return AjaxResult.success();
    }


    /**
     * 导出胎面口型板信息
     */
    @Log(title = "ui.data.column.tc.mouthPlate.modelName", businessType = BusinessType.EXPORT)
    @PostMapping("/exportData")
    @ApiOperation("导出胎面口型板信息")
    public List<TcMouthPlateDto> exportData(@SpringQueryMap TcMouthPlateDto dto) {
        TcMouthPlate mouthPlate = new TcMouthPlate();
        BeanUtils.copyProperties(dto, mouthPlate);
        startPage();
        mouthPlate.setOrderStr(orderStr());
        return tcMouthPlateService.selectMouthPlateList(mouthPlate);
    }

    @Log(title = "ui.data.column.tc.mouthPlate.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<TcMouthPlateDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (StringUtils.isNull(list) || list.size() == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return tcMouthPlateService.importData(list, updateSupport, importLogId);
    }
}
