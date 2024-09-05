package com.zlt.aps.cd90.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.PreAuthorize;
import com.zlt.aps.cd90.api.domain.dto.Cd90QuotaSettingDto;
import com.zlt.aps.cd90.entity.Cd90QuotaSetting;
import com.zlt.aps.cd90.service.Cd90QuotaSettingService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 90度裁断定额设定Controller
 *
 * @author chen
 * @date 2021-06-29
 */
@Api(tags = "90°裁断定额设定维护接口")
@RestController
@RequestMapping("/cd90/quota")
public class Cd90QuotaSettingController extends BaseController {
    @Autowired
    private Cd90QuotaSettingService cd90QuotaSettingService;

    /**
     * 查询90度裁断定额设定列表
     */
    @ApiOperation("根据条件查询90度裁断定额设定列表")
    //@PreAuthorize(hasPermi = "cd90:quota:list")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody Cd90QuotaSettingDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        Cd90QuotaSetting setting = new Cd90QuotaSetting();
        BeanUtils.copyProperties(dto, setting);
        List<Cd90QuotaSettingDto> list = cd90QuotaSettingService.selectQuotaSettingList(setting);
        return getDataTable(list);
    }

    /**
     * 获取90度裁断定额设定详细信息
     */
    @ApiOperation("根据id查询90度裁断定额设定详细信息")
    //@PreAuthorize(hasPermi = "cd90:quota:query")
    @GetMapping(value = "/{id}")
    public Cd90QuotaSettingDto getInfo(@PathVariable("id") Long id) {
        Cd90QuotaSetting setting = cd90QuotaSettingService.selectQuotaSettingById(id);
        Cd90QuotaSettingDto dto = new Cd90QuotaSettingDto();
        BeanUtils.copyProperties(setting, dto);
        return dto;
    }

    /**
     * 修改或新增90度裁断定额设定
     */
    @Log(title = "ui.data.column.cd90.quota.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("修改或新增90度裁断定额设定")
    //@PreAuthorize(hasPermi = "cd90:quota:edit")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody Cd90QuotaSettingDto dto) {
        Cd90QuotaSetting setting = new Cd90QuotaSetting();
        BeanUtils.copyProperties(dto, setting);
        return cd90QuotaSettingService.saveQuotaSetting(setting);
    }

    /**
     * 删除90度裁断定额设定
     */
    @Log(title = "ui.data.column.cd90.quota.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除90度裁断定额设定（id不为空）")
    //@PreAuthorize(hasPermi = "cd90:quota:remove")
    @PostMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        cd90QuotaSettingService.deleteQuotaSettingByIds(ids);
        return AjaxResult.success();
    }

    /**
     * 导出90度裁断定额设定列表
     *
     * @return 结果
     */
    @Log(title = "ui.data.column.cd90.quota.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出90度裁断定额设定")
    //@PreAuthorize(hasPermi = "cd90:quota:export")
    @PostMapping("/export")
    public List<Cd90QuotaSettingDto> export(Cd90QuotaSettingDto dto) {
        dto.setOrderStr(orderStr());
        Cd90QuotaSetting setting = new Cd90QuotaSetting();
        BeanUtils.copyProperties(dto, setting);
        return cd90QuotaSettingService.selectQuotaSettingList(setting);
    }

    @Log(title = "ui.data.column.cd90.quota.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<Cd90QuotaSettingDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return cd90QuotaSettingService.importData(list, updateSupport, importLogId);
    }

}
