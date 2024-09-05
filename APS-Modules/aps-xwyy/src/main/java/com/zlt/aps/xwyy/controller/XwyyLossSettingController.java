package com.zlt.aps.xwyy.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.xwyy.api.domain.dto.XwyyLossSettingDto;
import com.zlt.aps.xwyy.entity.XwyyLossSetting;
import com.zlt.aps.xwyy.service.XwyyLossSettingService;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 纤维压延损耗率设定Controller
 *
 * @author chen
 * @date 2021-07-19
 */
@RestController
@RequestMapping("/loss")
public class XwyyLossSettingController extends BaseController {
    @Autowired
    private XwyyLossSettingService xwyyLossSettingService;

    /**
     * 查询纤维压延损耗率设定列表
     */
    @ApiOperation("查询纤维压延损耗率设定列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody XwyyLossSettingDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        XwyyLossSetting xwyyLossSetting = new XwyyLossSetting();
        BeanUtils.copyProperties(dto, xwyyLossSetting);
        List<XwyyLossSettingDto> list = xwyyLossSettingService.selectXwyyLossSettingList(xwyyLossSetting);
        return getDataTable(list);
    }

    /**
     * 获取纤维压延损耗率设定详细信息
     */
    @ApiOperation("获取纤维压延损耗率设定详细信息")
    @GetMapping(value = "/{id}")
    public XwyyLossSettingDto getInfo(@PathVariable("id") Long id) {
        return xwyyLossSettingService.selectXwyyLossSettingById(id);
    }

    /**
     * 新增纤维压延损耗率设定
     */
    @Log(title = "ui.data.column.xwyy.loss.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("新增纤维压延损耗率设定")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody XwyyLossSettingDto dto) {
        XwyyLossSetting xwyyLossSetting = new XwyyLossSetting();
        BeanUtils.copyProperties(dto, xwyyLossSetting);
        return toAjax(xwyyLossSettingService.insertXwyyLossSetting(xwyyLossSetting));
    }

    /**
     * 修改纤维压延损耗率设定
     */
    @Log(title = "ui.data.column.xwyy.loss.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("修改纤维压延损耗率设定")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody XwyyLossSettingDto dto) {
        XwyyLossSetting xwyyLossSetting = new XwyyLossSetting();
        BeanUtils.copyProperties(dto, xwyyLossSetting);
        return toAjax(xwyyLossSettingService.updateXwyyLossSetting(xwyyLossSetting));
    }

    /**
     * 删除纤维压延损耗率设定
     */
    @Log(title = "ui.data.column.xwyy.loss.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除纤维压延损耗率设定")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(xwyyLossSettingService.deleteXwyyLossSettingByIds(ids));
    }


    @Log(title = "ui.data.column.xwyy.loss.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除全部(逻辑删)")
    @PostMapping("/deleteAll")
    public AjaxResult deleteAll() {
        xwyyLossSettingService.deleteAll();
        return AjaxResult.success();
    }


    /**
     * 导出纤维压延损耗率设定列表
     */
    @Log(title = "ui.data.column.xwyy.loss.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出纤维压延损耗率设定列表")
    @PostMapping("/getList")
    public List<XwyyLossSettingDto> getList(@RequestBody XwyyLossSettingDto dto) {
        dto.setOrderStr(orderStr());
        XwyyLossSetting xwyyLossSetting = new XwyyLossSetting();
        BeanUtils.copyProperties(dto, xwyyLossSetting);
        return xwyyLossSettingService.selectXwyyLossSettingList(xwyyLossSetting);
    }

    /**
     * 校验纤维压延损耗率设定唯一性
     */
    @ApiOperation("校验纤维压延损耗率设定唯一性")
    @PostMapping("/checkXwyyLossSettingUnique")
    public String checkXwyyLossSettingUnique(@RequestBody XwyyLossSettingDto dto) {
        XwyyLossSetting xwyyLossSetting = new XwyyLossSetting();
        BeanUtils.copyProperties(dto, xwyyLossSetting);
        return xwyyLossSettingService.checkXwyyLossSettingUnique(xwyyLossSetting);
    }

    @Log(title = "ui.data.column.xwyy.loss.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<XwyyLossSettingDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return xwyyLossSettingService.importData(list, updateSupport, importLogId);
    }
}
