package com.zlt.aps.xwyy.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.PreAuthorize;
import com.zlt.aps.xwyy.api.domain.dto.XwyyQuotaSettingDto;
import com.zlt.aps.xwyy.entity.XwyyQuotaSetting;
import com.zlt.aps.xwyy.service.XwyyQuotaSettingService;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 纤维压延定额设定Controller
 *
 * @author chen
 * @date 2021-06-29
 */
@RestController
@RequestMapping("/xwyy/quota")
public class XwyyQuotaSettingController extends BaseController {
    @Autowired
    private XwyyQuotaSettingService xwyyQuotaSettingService;

    /**
     * 查询纤维压延定额设定列表
     */
    @ApiOperation("根据条件查询纤维压延定额设定列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody XwyyQuotaSettingDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        XwyyQuotaSetting setting = new XwyyQuotaSetting();
        BeanUtils.copyProperties(dto, setting);
        List<XwyyQuotaSettingDto> list = xwyyQuotaSettingService.selectQuotaSettingList(setting);
        return getDataTable(list);
    }

    /**
     * 获取纤维压延定额设定详细信息
     */
    @ApiOperation("根据id查询纤维压延定额设定详细信息")
    @GetMapping(value = "/{id}")
    public XwyyQuotaSettingDto getInfo(@PathVariable("id") Long id) {
        XwyyQuotaSetting setting = xwyyQuotaSettingService.selectQuotaSettingById(id);
        XwyyQuotaSettingDto dto = new XwyyQuotaSettingDto();
        BeanUtils.copyProperties(setting, dto);
        return dto;
    }

    /**
     * 修改或新增纤维压延定额设定
     */
    @Log(title = "ui.data.column.xwyy.quota.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("修改或新增纤维压延定额设定")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody XwyyQuotaSettingDto dto) {
        XwyyQuotaSetting setting = new XwyyQuotaSetting();
        BeanUtils.copyProperties(dto, setting);
        return xwyyQuotaSettingService.saveQuotaSetting(setting);
    }

    /**
     * 删除纤维压延定额设定
     */
    @Log(title = "ui.data.column.xwyy.quota.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除纤维压延定额设定（id不为空）")
    @PostMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        xwyyQuotaSettingService.deleteQuotaSettingByIds(ids);
        return AjaxResult.success();
    }

    /**
     * 导出纤维压延定额设定列表
     *
     * @return 结果
     */
    @Log(title = "ui.data.column.xwyy.quota.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出纤维压延定额设定")
    @PostMapping("/export")
    public List<XwyyQuotaSettingDto> export(@RequestBody XwyyQuotaSettingDto dto) {
        dto.setOrderStr(orderStr());
        XwyyQuotaSetting setting = new XwyyQuotaSetting();
        BeanUtils.copyProperties(dto, setting);
        return xwyyQuotaSettingService.selectQuotaSettingList(setting);
    }

    @Log(title = "ui.data.column.xwyy.quota.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<XwyyQuotaSettingDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return xwyyQuotaSettingService.importData(list, updateSupport, importLogId);
    }
}
