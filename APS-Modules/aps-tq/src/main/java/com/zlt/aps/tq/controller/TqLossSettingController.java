package com.zlt.aps.tq.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.tq.api.domain.dto.TqLossSettingDto;
import com.zlt.aps.tq.entity.TqLossSetting;
import com.zlt.aps.tq.service.TqLossSettingService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 胎圈损耗率设定Controller
 *
 * @author chen
 * @date 2021-07-13
 */
@RestController
@RequestMapping("/loss")
public class TqLossSettingController extends BaseController {
    @Autowired
    private TqLossSettingService tqLossSettingService;

    /**
     * 查询胎圈损耗率设定列表
     */
    @ApiOperation("查询胎圈损耗率设定列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody TqLossSettingDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        TqLossSetting tqLossSetting = new TqLossSetting();
        BeanUtils.copyProperties(dto, tqLossSetting);
        List<TqLossSettingDto> list = tqLossSettingService.selectTqLossSettingList(tqLossSetting);
        return getDataTable(list);
    }

    /**
     * 获取胎圈损耗率设定详细信息
     */
    @ApiOperation("获取胎圈损耗率设定详细信息")
    @GetMapping(value = "/{id}")
    public TqLossSettingDto getInfo(@PathVariable("id") Long id) {
        return tqLossSettingService.selectTqLossSettingById(id);
    }

    /**
     * 新增胎圈损耗率设定
     */
    @Log(title = "ui.data.column.tq.loss.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增胎圈损耗率设定")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody TqLossSettingDto dto) {
        TqLossSetting tqLossSetting = new TqLossSetting();
        BeanUtils.copyProperties(dto, tqLossSetting);
        return toAjax(tqLossSettingService.insertTqLossSetting(tqLossSetting));
    }

    /**
     * 修改胎圈损耗率设定
     */
    @Log(title = "ui.data.column.tq.loss.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改胎圈损耗率设定")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody TqLossSettingDto dto) {
        TqLossSetting tqLossSetting = new TqLossSetting();
        BeanUtils.copyProperties(dto, tqLossSetting);
        return toAjax(tqLossSettingService.updateTqLossSetting(tqLossSetting));
    }

    /**
     * 删除胎圈损耗率设定
     */
    @Log(title = "ui.data.column.tq.loss.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除胎圈损耗率设定")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(tqLossSettingService.deleteTqLossSettingByIds(ids));
    }


    @Log(title = "ui.data.column.tq.loss.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除全部(逻辑删)")
    @PostMapping("/deleteAll")
    public AjaxResult deleteAll() {
        tqLossSettingService.deleteAll();
        return AjaxResult.success();
    }


    /**
     * 导出胎圈损耗率设定列表
     */
    @Log(title = "ui.data.column.tq.loss.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出胎圈损耗率设定列表")
    @PostMapping("/getList")
    public List<TqLossSettingDto> getList(@RequestBody TqLossSettingDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        TqLossSetting tqLossSetting = new TqLossSetting();
        BeanUtils.copyProperties(dto, tqLossSetting);
        return tqLossSettingService.selectTqLossSettingList(tqLossSetting);
    }

    /**
     * 校验胎圈损耗率设定唯一性
     */
    @ApiOperation("校验胎圈损耗率设定唯一性")
    @PostMapping("/checkTqLossSettingUnique")
    public String checkTqLossSettingUnique(@RequestBody TqLossSettingDto dto) {
        TqLossSetting tqLossSetting = new TqLossSetting();
        BeanUtils.copyProperties(dto, tqLossSetting);
        return tqLossSettingService.checkTqLossSettingUnique(tqLossSetting);
    }

    @Log(title = "ui.data.column.tq.loss.modelName", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    @ApiOperation("导入胎圈损耗率信息")
    public AjaxResult importData(@RequestBody List<TqLossSettingDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (StringUtils.isNull(list) || list.size() == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return tqLossSettingService.importData(list, updateSupport, importLogId);
    }
}
