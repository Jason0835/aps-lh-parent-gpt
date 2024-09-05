package com.zlt.aps.cd15.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.cd15.api.domain.dto.Cd15QuotaSettingDto;
import com.zlt.aps.cd15.entity.Cd15QuotaSetting;
import com.zlt.aps.cd15.service.Cd15QuotaSettingService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 15度裁断定额设定Controller
 *
 * @author chen
 * @date 2021-06-28
 */
@RestController
@RequestMapping("/cd15/quota")
public class Cd15QuotaSettingController extends BaseController {

    @Autowired
    private Cd15QuotaSettingService cd15QuotaSettingService;

    /**
     * 查询15度裁断定额设定列表
     */
    //@PreAuthorize(hasPermi = "cd15:setting:list")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody Cd15QuotaSettingDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        Cd15QuotaSetting setting = new Cd15QuotaSetting();
        BeanUtils.copyProperties(dto, setting);
        List<Cd15QuotaSettingDto> list = cd15QuotaSettingService.selectQuotaSettingList(setting);
        return getDataTable(list);
    }

    /**
     * 获取15度裁断定额设定详细信息
     */
    //@PreAuthorize(hasPermi = "cd15:setting:query")
    @GetMapping(value = "/{id}")
    public Cd15QuotaSettingDto getInfo(@PathVariable("id") Long id) {
        Cd15QuotaSetting setting = cd15QuotaSettingService.selectQuotaSettingById(id);
        Cd15QuotaSettingDto dto = new Cd15QuotaSettingDto();
        BeanUtils.copyProperties(setting, dto);
        return dto;
    }

    /**
     * 修改或新增15度裁断定额设定
     */
    @Log(title = "ui.data.column.cd15.setting.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    //@PreAuthorize(hasPermi = "cd15:setting:edit")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody Cd15QuotaSettingDto dto) {
        Cd15QuotaSetting setting = new Cd15QuotaSetting();
        BeanUtils.copyProperties(dto, setting);
        return cd15QuotaSettingService.saveQuotaSetting(setting);
    }

    /**
     * 删除15度裁断定额设定
     */
    @Log(title = "ui.data.column.cd15.setting.modelName", businessType = BusinessType.DELETE)
    //@PreAuthorize(hasPermi = "cd15:setting:remove")
    @PostMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        cd15QuotaSettingService.deleteQuotaSettingByIds(ids);
        return AjaxResult.success();
    }

    /**
     * 导出15度裁断定额设定列表
     *
     * @return 结果
     */
    @Log(title = "ui.data.column.cd15.setting.modelName", businessType = BusinessType.EXPORT)
    //@PreAuthorize(hasPermi = "cd15:setting:export")
    @PostMapping("/export")
    public List<Cd15QuotaSettingDto> export(Cd15QuotaSettingDto dto) {
        dto.setOrderStr(orderStr());
        Cd15QuotaSetting setting = new Cd15QuotaSetting();
        BeanUtils.copyProperties(dto, setting);
        return cd15QuotaSettingService.selectQuotaSettingList(setting);
    }

    @Log(title = "ui.data.column.cd15.setting.modelName", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    @ApiOperation("导入15度裁断定额设定信息")
    public AjaxResult importData(@RequestBody List<Cd15QuotaSettingDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (StringUtils.isNull(list) || list.size() == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return cd15QuotaSettingService.importData(list, updateSupport, importLogId);
    }
}
