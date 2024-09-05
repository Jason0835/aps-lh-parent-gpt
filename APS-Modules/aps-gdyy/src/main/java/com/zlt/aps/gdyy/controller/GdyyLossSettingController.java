package com.zlt.aps.gdyy.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.gdyy.api.domain.dto.GdyyLossSettingDto;
import com.zlt.aps.gdyy.entity.GdyyLossSetting;
import com.zlt.aps.gdyy.service.GdyyLossSettingService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 钢带压延损耗率设定Controller
 *
 * @author chen
 * @date 2021-07-19
 */
@RestController
@RequestMapping("/loss")
public class GdyyLossSettingController extends BaseController {
    @Autowired
    private GdyyLossSettingService gdyyLossSettingService;

    /**
     * 查询钢带压延损耗率设定列表
     */
    @ApiOperation("查询钢带压延损耗率设定列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody GdyyLossSettingDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        GdyyLossSetting gdyyLossSetting = new GdyyLossSetting();
        BeanUtils.copyProperties(dto, gdyyLossSetting);
        List<GdyyLossSettingDto> list = gdyyLossSettingService.selectGdyyLossSettingList(gdyyLossSetting);
        return getDataTable(list);
    }

    /**
     * 获取钢带压延损耗率设定详细信息
     */
    @ApiOperation("获取钢带压延损耗率设定详细信息")
    @GetMapping(value = "/{id}")
    public GdyyLossSettingDto getInfo(@PathVariable("id") Long id) {
        return gdyyLossSettingService.selectGdyyLossSettingById(id);
    }

    /**
     * 新增钢带压延损耗率设定
     */
    @ApiOperation("新增钢带压延损耗率设定")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody GdyyLossSettingDto dto) {
        GdyyLossSetting gdyyLossSetting = new GdyyLossSetting();
        BeanUtils.copyProperties(dto, gdyyLossSetting);
        return toAjax(gdyyLossSettingService.insertGdyyLossSetting(gdyyLossSetting));
    }

    /**
     * 修改钢带压延损耗率设定
     */
    @Log(title = "ui.data.column.gdyy.loss.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("修改钢带压延损耗率设定")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody GdyyLossSettingDto dto) {
        GdyyLossSetting gdyyLossSetting = new GdyyLossSetting();
        BeanUtils.copyProperties(dto, gdyyLossSetting);
        return toAjax(gdyyLossSettingService.updateGdyyLossSetting(gdyyLossSetting));
    }

    /**
     * 删除钢带压延损耗率设定
     */
    @Log(title = "ui.data.column.gdyy.loss.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除钢带压延损耗率设定")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(gdyyLossSettingService.deleteGdyyLossSettingByIds(ids));
    }


    @Log(title = "ui.data.column.gdyy.loss.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除全部(逻辑删)")
    @PostMapping("/deleteAll")
    public AjaxResult deleteAll() {
        gdyyLossSettingService.deleteAll();
        return AjaxResult.success();
    }


    /**
     * 导出钢带压延损耗率设定列表
     */
    @Log(title = "ui.data.column.gdyy.loss.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出钢带压延损耗率设定列表")
    @PostMapping("/getList")
    public List<GdyyLossSettingDto> getList(@RequestBody GdyyLossSettingDto dto) {
        dto.setOrderStr(orderStr());
        GdyyLossSetting gdyyLossSetting = new GdyyLossSetting();
        BeanUtils.copyProperties(dto, gdyyLossSetting);
        return gdyyLossSettingService.selectGdyyLossSettingList(gdyyLossSetting);
    }

    /**
     * 校验钢带压延损耗率设定唯一性
     */
    @ApiOperation("校验钢带压延损耗率设定唯一性")
    @PostMapping("/checkGdyyLossSettingUnique")
    public String checkGdyyLossSettingUnique(@RequestBody GdyyLossSettingDto dto) {
        GdyyLossSetting gdyyLossSetting = new GdyyLossSetting();
        BeanUtils.copyProperties(dto, gdyyLossSetting);
        return gdyyLossSettingService.checkGdyyLossSettingUnique(gdyyLossSetting);
    }

    @Log(title = "ui.data.column.gdyy.loss.modelName", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    @ApiOperation("导入胎面损耗率信息")
    public AjaxResult importData(@RequestBody List<GdyyLossSettingDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (StringUtils.isNull(list) || list.size() == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return gdyyLossSettingService.importData(list, updateSupport, importLogId);
    }
}
