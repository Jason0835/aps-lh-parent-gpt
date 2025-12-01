package com.zlt.aps.tm.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.tm.api.domain.dto.TmLossSettingDto;
import com.zlt.aps.tm.entity.TmLossSetting;
import com.zlt.aps.tm.service.TmLossSettingService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 胎面损耗率设定Controller
 *
 * @author chen
 * @date 2021-07-12
 */
@RestController
@RequestMapping("/tm/loss")
public class TmLossSettingController extends BaseController {
    @Autowired
    private TmLossSettingService tmLossSettingService;

    /**
     * 查询胎面损耗率设定列表
     */
    //@PreAuthorize(hasPermi = "tm:loss:list")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody TmLossSettingDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        TmLossSetting tmLossSetting = new TmLossSetting();
        BeanUtils.copyProperties(dto, tmLossSetting);
        List<TmLossSettingDto> list = tmLossSettingService.selectTmLossSettingList(tmLossSetting);
        return getDataTable(list);
    }

    /**
     * 获取胎面损耗率设定详细信息
     */
    //@PreAuthorize(hasPermi = "tm:loss:query")
    @GetMapping(value = "/{id}")
    public TmLossSettingDto getInfo(@PathVariable("id") Long id) {
        return tmLossSettingService.selectTmLossSettingById(id);
    }

    /**
     * 新增胎面损耗率设定
     */
    @Log(title = "ui.data.column.tm.loss.modelName", businessType = BusinessType.INSERT)
    //@PreAuthorize(hasPermi = "tm:loss:add")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody TmLossSettingDto dto) {
        TmLossSetting tmLossSetting = new TmLossSetting();
        BeanUtils.copyProperties(dto, tmLossSetting);
        return toAjax(tmLossSettingService.insertTmLossSetting(tmLossSetting));
    }

    /**
     * 修改胎面损耗率设定
     */
    @Log(title = "ui.data.column.tm.loss.modelName", businessType = BusinessType.UPDATE)
    //@PreAuthorize(hasPermi = "tm:loss:edit")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody TmLossSettingDto dto) {
        TmLossSetting tmLossSetting = new TmLossSetting();
        BeanUtils.copyProperties(dto, tmLossSetting);
        return toAjax(tmLossSettingService.updateTmLossSetting(tmLossSetting));
    }

    /**
     * 删除胎面损耗率设定
     */
    @Log(title = "ui.data.column.tm.loss.modelName", businessType = BusinessType.DELETE)
    //@PreAuthorize(hasPermi = "tm:loss:remove")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(tmLossSettingService.deleteTmLossSettingByIds(ids));
    }


    @Log(title = "ui.data.column.tm.loss.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除全部(逻辑删)")
    @PostMapping("/deleteAll")
    public AjaxResult deleteAll() {
        tmLossSettingService.deleteAll();
        return AjaxResult.success();
    }


    /**
     * 导出胎面损耗率设定列表
     */
    @Log(title = "ui.data.column.tm.loss.modelName", businessType = BusinessType.EXPORT)
    //@PreAuthorize(hasPermi = "tm:loss:export")
    @PostMapping("/getList")
    public List<TmLossSettingDto> getList(@RequestBody TmLossSettingDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        TmLossSetting tmLossSetting = new TmLossSetting();
        BeanUtils.copyProperties(dto, tmLossSetting);
        return tmLossSettingService.selectTmLossSettingList(tmLossSetting);
    }

    /**
     * 校验胎面损耗率设定唯一性
     */
    @ApiOperation("校验胎面损耗率设定唯一性")
    @PostMapping("/checkTmLossSettingUnique")
    public String checkTmLossSettingUnique(@RequestBody TmLossSettingDto dto) {
        TmLossSetting tmLossSetting = new TmLossSetting();
        BeanUtils.copyProperties(dto, tmLossSetting);
        return tmLossSettingService.checkTmLossSettingUnique(tmLossSetting);
    }

    @Log(title = "ui.data.column.tm.loss.modelName", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    @ApiOperation("导入胎面损耗率信息")
    public AjaxResult importData(@RequestBody List<TmLossSettingDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (StringUtils.isNull(list) || list.size() == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return tmLossSettingService.importData(list, updateSupport, importLogId);
    }
}
