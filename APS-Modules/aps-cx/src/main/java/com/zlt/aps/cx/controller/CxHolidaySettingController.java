package com.zlt.aps.cx.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cx.api.domain.dto.CxHolidaySettingDto;
import com.zlt.aps.cx.entity.CxHolidaySetting;
import com.zlt.aps.cx.service.CxHolidaySettingService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 假日设定Controller
 *
 * @author chen
 * @date 2021-06-30
 */
@RestController
@RequestMapping("/cx/holiday")
public class CxHolidaySettingController extends BaseController {
    @Autowired
    private CxHolidaySettingService cxHolidaySettingService;

    /**
     * 查询成型假日设定列表
     */
    //@PreAuthorize(hasPermi = "cx:holiday:list")
    @PostMapping("/list")
    @ApiOperation("查询成型假日设定列表")
    public TableDataInfo list(@RequestBody CxHolidaySettingDto dto) {
        CxHolidaySetting setting = new CxHolidaySetting();
        BeanUtils.copyProperties(dto, setting);
        startPage();
        setting.setOrderStr(orderStr());
        List<CxHolidaySettingDto> list = cxHolidaySettingService.selectCxHolidaySettingList(setting);
        return getDataTable(list);
    }

    /**
     * 获取成型假日设定详细信息
     */
    @GetMapping(value = "/{id}")
    @ApiOperation("获取成型假日设定详细信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public CxHolidaySettingDto getInfo(@PathVariable("id") Long id) {
        CxHolidaySetting setting = cxHolidaySettingService.selectCxHolidaySettingById(id);
        CxHolidaySettingDto dto = new CxHolidaySettingDto();
        BeanUtils.copyProperties(setting, dto);
        return dto;
    }

    /**
     * 修改成型假日设定
     */
    //@PreAuthorize(hasPermi = "cx:holiday:edit")
    @Log(title = "ui.data.column.cx.holiday.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @PostMapping("/edit")
    @ApiOperation("修改成型假日设定（id为空则新增，id不为空则修改）")
    public AjaxResult edit(@RequestBody CxHolidaySettingDto dto) {
        cxHolidaySettingService.saveCxHolidaySetting(dto);
        return AjaxResult.success();
    }

    /**
     * 删除成型假日设定
     */
    //@PreAuthorize(hasPermi = "cx:holiday:remove")
    @Log(title = "ui.data.column.cx.holiday.modelName", businessType = BusinessType.DELETE)
    @PostMapping("/{ids}")
    @ApiOperation("删除成型假日设定")
    public AjaxResult remove(@PathVariable("ids") Long[] ids) {
        cxHolidaySettingService.deleteCxHolidaySettingByIds(ids);
        return AjaxResult.success();
    }

    /**
     * 导出成型假日设定列表
     */
    //@PreAuthorize(hasPermi = "cx:holiday:export")
    @Log(title = "ui.data.column.cx.holiday.modelName", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    @ApiOperation("导出成型假日设定列表")
    public List<CxHolidaySettingDto> export(@SpringQueryMap CxHolidaySettingDto dto) {
        CxHolidaySetting setting = new CxHolidaySetting();
        BeanUtils.copyProperties(dto, setting);
        startPage();
        setting.setOrderStr(orderStr());
        return cxHolidaySettingService.selectCxHolidaySettingList(setting);
    }

    /**
     * 校验记录唯一性
     *
     * @param dto 要校验记录
     * @return 查询到的结果
     */
    //@PreAuthorize(hasPermi = "cx:holiday:edit")
    @PostMapping("/checkUnique")
    @ApiOperation("校验记录唯一性")
    public List<CxHolidaySettingDto> checkUnique(@SpringQueryMap CxHolidaySettingDto dto) {
        CxHolidaySetting setting = new CxHolidaySetting();
        BeanUtils.copyProperties(dto, setting);
        return cxHolidaySettingService.checkUnique(setting);
    }

    @Log(title = "ui.data.column.cx.holiday.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<CxHolidaySettingDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return cxHolidaySettingService.importData(list, updateSupport, importLogId);
    }
}
