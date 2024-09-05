package com.zlt.aps.cx.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cx.api.domain.dto.CxQuotaSettingDto;
import com.zlt.aps.cx.entity.CxQuotaSetting;
import com.zlt.aps.cx.service.CxQuotaSettingService;
import io.swagger.annotations.Api;
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
 * 成型定额设定Controller
 *
 * @author chen
 * @date 2021-06-16
 */
@RestController
@RequestMapping("/cx/quota")
@Api(tags = "成型定额设定信息维护接口")
public class CxQuotaSettingController extends BaseController {
    @Autowired
    private CxQuotaSettingService cxQuotaSettingService;

    /**
     * 查询成型定额设定列表
     */
    //@PreAuthorize(hasPermi = "cx:setting:list")
    @PostMapping("/list")
    @ApiOperation("查询成型定额设定列表")
    public TableDataInfo list(@RequestBody CxQuotaSettingDto dto) {
        CxQuotaSetting setting = new CxQuotaSetting();
        BeanUtils.copyProperties(dto, setting);
        startPage();
        String orderStr = orderStr();
        if (orderStr != null && orderStr.contains("machine_type")) {
            orderStr = orderStr.replace("machine_type", "type");
        }
        setting.setOrderStr(orderStr);
        List<CxQuotaSettingDto> list = cxQuotaSettingService.selectCxQuotaSettingList(setting);
        return getDataTable(list);
    }

    /**
     * 获取成型定额设定详细信息
     */
    @GetMapping(value = "/{id}")
    @ApiOperation("获取成型定额设定详细信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public CxQuotaSettingDto getInfo(@PathVariable("id") Long id) {
        CxQuotaSetting setting = cxQuotaSettingService.selectCxQuotaSettingById(id);
        CxQuotaSettingDto dto = new CxQuotaSettingDto();
        BeanUtils.copyProperties(setting, dto);
        return dto;
    }

    /**
     * 修改成型定额设定
     */
    //@PreAuthorize(hasPermi = "cx:setting:edit")
    @Log(title = "ui.data.column.cx.setting.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @PostMapping("/edit")
    @ApiOperation("修改成型定额设定（id为空则新增，id不为空则修改）")
    public AjaxResult edit(@RequestBody CxQuotaSettingDto dto) {
        CxQuotaSetting setting = new CxQuotaSetting();
        BeanUtils.copyProperties(dto, setting);
        cxQuotaSettingService.saveCxQuotaSetting(setting);
        return AjaxResult.success();
    }

    /**
     * 删除成型定额设定
     */
    //@PreAuthorize(hasPermi = "cx:setting:remove")
    @Log(title = "ui.data.column.cx.setting.modelName", businessType = BusinessType.DELETE)
    @PostMapping("/{ids}")
    @ApiOperation("删除成型定额设定")
    public AjaxResult remove(@PathVariable("ids") Long[] ids) {
        cxQuotaSettingService.deleteCxQuotaSettingByIds(ids);
        return AjaxResult.success();
    }

    /**
     * 导出成型定额设定列表
     */
    //@PreAuthorize(hasPermi = "cx:setting:export")
    @Log(title = "ui.data.column.cx.setting.modelName", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    @ApiOperation("导出成型定额设定列表")
    public List<CxQuotaSettingDto> export(@SpringQueryMap CxQuotaSetting dto) {
        CxQuotaSetting setting = new CxQuotaSetting();
        BeanUtils.copyProperties(dto, setting);
        startPage();
        String orderStr = orderStr();
        if (orderStr != null && orderStr.contains("machine_type")) {
            orderStr = orderStr.replace("machine_type", "type");
        }
        setting.setOrderStr(orderStr);
        return cxQuotaSettingService.selectCxQuotaSettingList(setting);
    }

    @Log(title = "ui.data.column.cx.setting.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<CxQuotaSettingDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return cxQuotaSettingService.importData(list, updateSupport, importLogId);
    }
}
