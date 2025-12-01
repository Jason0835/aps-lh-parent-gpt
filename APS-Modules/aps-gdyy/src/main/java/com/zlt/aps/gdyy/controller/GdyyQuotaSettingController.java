package com.zlt.aps.gdyy.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.gdyy.api.domain.dto.GdyyQuotaSettingDto;
import com.zlt.aps.gdyy.entity.GdyyQuotaSetting;
import com.zlt.aps.gdyy.service.GdyyQuotaSettingService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 钢带压延定额设定Controller
 *
 * @author chen
 * @date 2021-06-30
 */
@RestController
@RequestMapping("/gdyy/quota")
public class GdyyQuotaSettingController extends BaseController {
    @Autowired
    private GdyyQuotaSettingService gdyyQuotaSettingService;

    /**
     * 查询钢带压延定额设定列表
     */
    @ApiOperation("根据条件查询钢带压延定额设定列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody GdyyQuotaSettingDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        GdyyQuotaSetting setting = new GdyyQuotaSetting();
        BeanUtils.copyProperties(dto, setting);
        List<GdyyQuotaSettingDto> list = gdyyQuotaSettingService.selectQuotaSettingList(setting);
        return getDataTable(list);
    }

    /**
     * 获取钢带压延定额设定详细信息
     */
    @ApiOperation("根据id查询钢带压延定额设定详细信息")
    @GetMapping(value = "/{id}")
    public GdyyQuotaSettingDto getInfo(@PathVariable("id") Long id) {
        GdyyQuotaSetting setting = gdyyQuotaSettingService.selectQuotaSettingById(id);
        GdyyQuotaSettingDto dto = new GdyyQuotaSettingDto();
        BeanUtils.copyProperties(setting, dto);
        return dto;
    }

    /**
     * 修改或新增钢带压延定额设定
     */
    @Log(title = "ui.data.column.gdyy.quota.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("修改或新增钢带压延定额设定")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody GdyyQuotaSettingDto dto) {
        GdyyQuotaSetting setting = new GdyyQuotaSetting();
        BeanUtils.copyProperties(dto, setting);
        return gdyyQuotaSettingService.saveQuotaSetting(setting);
    }

    /**
     * 删除钢带压延定额设定
     */
    @Log(title = "ui.data.column.gdyy.quota.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除钢带压延定额设定（id不为空）")
    @PostMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        gdyyQuotaSettingService.deleteQuotaSettingByIds(ids);
        return AjaxResult.success();
    }

    /**
     * 导出钢带压延定额设定列表
     *
     * @return 结果
     */
    @Log(title = "ui.data.column.gdyy.quota.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出钢带压延定额设定")
    @PostMapping("/export")
    public List<GdyyQuotaSettingDto> export(@RequestBody GdyyQuotaSettingDto dto) {
        dto.setOrderStr(orderStr());
        GdyyQuotaSetting setting = new GdyyQuotaSetting();
        BeanUtils.copyProperties(dto, setting);
        return gdyyQuotaSettingService.selectQuotaSettingList(setting);
    }

    @Log(title = "ui.data.column.gdyy.quota.modelName", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    @ApiOperation("导入钢带压延定额设定信息")
    public AjaxResult importData(@RequestBody List<GdyyQuotaSettingDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (StringUtils.isNull(list) || list.size() == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return gdyyQuotaSettingService.importData(list, updateSupport, importLogId);
    }
}
