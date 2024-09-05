package com.zlt.aps.cx.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cx.api.domain.dto.CxLossSettingDto;
import com.zlt.aps.cx.entity.CxLossSetting;
import com.zlt.aps.cx.service.CxLossSettingService;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 成型损耗率设定Controller
 *
 * @author chen
 * @date 2021-07-19
 */
@RestController
@RequestMapping("/loss")
public class CxLossSettingController extends BaseController {
    @Autowired
    private CxLossSettingService cxLossSettingService;

    /**
     * 查询成型损耗率设定列表
     */
    //@PreAuthorize(hasPermi = "cx:loss:list")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody CxLossSettingDto dto) {
        CxLossSetting cxLossSetting = new CxLossSetting();
        BeanUtils.copyProperties(dto, cxLossSetting);
        startPage();
        cxLossSetting.setOrderStr(orderStr());
        List<CxLossSettingDto> list = cxLossSettingService.selectCxLossSettingList(cxLossSetting);
        return getDataTable(list);
    }

    /**
     * 获取成型损耗率设定详细信息
     */
    //@PreAuthorize(hasPermi = "cx:loss:query")
    @GetMapping(value = "/{id}")
    public CxLossSettingDto getInfo(@PathVariable("id") Long id) {
        return cxLossSettingService.selectCxLossSettingById(id);
    }

    /**
     * 新增成型损耗率设定
     */
    //@PreAuthorize(hasPermi = "cx:loss:add")
    @Log(title = "ui.data.column.cx.loss.modelName", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public AjaxResult add(@RequestBody CxLossSettingDto dto) {
        CxLossSetting cxLossSetting = new CxLossSetting();
        BeanUtils.copyProperties(dto, cxLossSetting);
        return toAjax(cxLossSettingService.insertCxLossSetting(cxLossSetting));
    }

    /**
     * 修改成型损耗率设定
     */
    //@PreAuthorize(hasPermi = "cx:loss:edit")
    @Log(title = "ui.data.column.cx.loss.modelName", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody CxLossSettingDto dto) {
        CxLossSetting cxLossSetting = new CxLossSetting();
        BeanUtils.copyProperties(dto, cxLossSetting);
        return toAjax(cxLossSettingService.updateCxLossSetting(cxLossSetting));
    }

    /**
     * 删除成型损耗率设定
     */
    //@PreAuthorize(hasPermi = "cx:loss:remove")
    @Log(title = "ui.data.column.cx.loss.modelName", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(cxLossSettingService.deleteCxLossSettingByIds(ids));
    }

    /**
     * 导出成型损耗率设定列表
     */
    //@PreAuthorize(hasPermi = "cx:loss:export")
    @Log(title = "ui.data.column.cx.loss.modelName", businessType = BusinessType.EXPORT)
    @PostMapping("/getList")
    public List<CxLossSettingDto> getList(@RequestBody CxLossSettingDto dto) {
        CxLossSetting cxLossSetting = new CxLossSetting();
        BeanUtils.copyProperties(dto, cxLossSetting);
        startPage();
        cxLossSetting.setOrderStr(orderStr());
        return cxLossSettingService.selectCxLossSettingList(cxLossSetting);
    }

    /**
     * 校验成型损耗率设定唯一性
     */
    @ApiOperation("校验成型损耗率设定唯一性")
    @PostMapping("/checkCxLossSettingUnique")
    public String checkCxLossSettingUnique(@RequestBody CxLossSettingDto dto) {
        CxLossSetting cxLossSetting = new CxLossSetting();
        BeanUtils.copyProperties(dto, cxLossSetting);
        return cxLossSettingService.checkCxLossSettingUnique(cxLossSetting);
    }

    @Log(title = "ui.data.column.cx.loss.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<CxLossSettingDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return cxLossSettingService.importData(list, updateSupport, importLogId);
    }
}
