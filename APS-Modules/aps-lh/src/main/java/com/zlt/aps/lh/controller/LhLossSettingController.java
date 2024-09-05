package com.zlt.aps.lh.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.lh.api.domain.dto.LhLossSettingDto;
import com.zlt.aps.lh.entity.LhLossSetting;
import com.zlt.aps.lh.service.LhLossSettingService;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 硫化损耗率设定Controller
 *
 * @author chen
 * @date 2021-07-19
 */
@RestController
@RequestMapping("/loss")
public class LhLossSettingController extends BaseController {
    @Autowired
    private LhLossSettingService lhLossSettingService;

    /**
     * 查询硫化损耗率设定列表
     */
    @ApiOperation("查询硫化损耗率设定列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody LhLossSettingDto dto) {
        LhLossSetting lhLossSetting = new LhLossSetting();
        BeanUtils.copyProperties(dto, lhLossSetting);
        startPage();
        lhLossSetting.setOrderStr(orderStr());
        List<LhLossSettingDto> list = lhLossSettingService.selectLhLossSettingList(lhLossSetting);
        return getDataTable(list);
    }

    /**
     * 获取硫化损耗率设定详细信息
     */
    @ApiOperation("获取硫化损耗率设定详细信息")
    @GetMapping(value = "/{id}")
    public LhLossSettingDto getInfo(@PathVariable("id") Long id) {
        return lhLossSettingService.selectLhLossSettingById(id);
    }

    /**
     * 新增硫化损耗率设定
     */
    @Log(title = "ui.data.column.lh.loss.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增硫化损耗率设定")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody LhLossSettingDto dto) {
        LhLossSetting lhLossSetting = new LhLossSetting();
        BeanUtils.copyProperties(dto, lhLossSetting);
        return toAjax(lhLossSettingService.insertLhLossSetting(lhLossSetting));
    }

    /**
     * 修改硫化损耗率设定
     */
    @Log(title = "ui.data.column.lh.loss.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改硫化损耗率设定")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody LhLossSettingDto dto) {
        LhLossSetting lhLossSetting = new LhLossSetting();
        BeanUtils.copyProperties(dto, lhLossSetting);
        return toAjax(lhLossSettingService.updateLhLossSetting(lhLossSetting));
    }

    /**
     * 删除硫化损耗率设定
     */
    @Log(title = "ui.data.column.lh.loss.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除硫化损耗率设定")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(lhLossSettingService.deleteLhLossSettingByIds(ids));
    }

    /**
     * 导出硫化损耗率设定列表
     */
    @Log(title = "ui.data.column.lh.loss.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出硫化损耗率设定列表")
    @PostMapping("/getList")
    public List<LhLossSettingDto> getList(@RequestBody LhLossSettingDto dto) {
        LhLossSetting lhLossSetting = new LhLossSetting();
        BeanUtils.copyProperties(dto, lhLossSetting);
        startPage();
        lhLossSetting.setOrderStr(orderStr());
        return lhLossSettingService.selectLhLossSettingList(lhLossSetting);
    }

    /**
     * 校验硫化损耗率设定唯一性
     */
    @ApiOperation("校验硫化损耗率设定唯一性")
    @PostMapping("/checkLhLossSettingUnique")
    public String checkLhLossSettingUnique(@RequestBody LhLossSettingDto dto) {
        LhLossSetting lhLossSetting = new LhLossSetting();
        BeanUtils.copyProperties(dto, lhLossSetting);
        return lhLossSettingService.checkLhLossSettingUnique(lhLossSetting);
    }

    @Log(title = "ui.data.column.lh.loss.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<LhLossSettingDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return lhLossSettingService.importData(list, updateSupport, importLogId);
    }

}
