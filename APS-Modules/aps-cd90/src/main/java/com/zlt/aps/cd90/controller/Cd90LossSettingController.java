package com.zlt.aps.cd90.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cd90.api.domain.dto.Cd90LossSettingDto;
import com.zlt.aps.cd90.entity.Cd90LossSetting;
import com.zlt.aps.cd90.service.Cd90LossSettingService;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 90度裁断损耗率设定Controller
 *
 * @author chen
 * @date 2021-07-19
 */
@RestController
@RequestMapping("/cd90/loss")
public class Cd90LossSettingController extends BaseController {
    @Autowired
    private Cd90LossSettingService cd90LossSettingService;

    /**
     * 查询90度裁断损耗率设定列表
     */
    //@PreAuthorize(hasPermi = "cd90:loss:list")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody Cd90LossSettingDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        Cd90LossSetting cd90LossSetting = new Cd90LossSetting();
        BeanUtils.copyProperties(dto, cd90LossSetting);
        List<Cd90LossSettingDto> list = cd90LossSettingService.selectCd90LossSettingList(cd90LossSetting);
        return getDataTable(list);
    }

    /**
     * 获取90度裁断损耗率设定详细信息
     */
    //@PreAuthorize(hasPermi = "cd90:loss:query")
    @GetMapping(value = "/{id}")
    public Cd90LossSettingDto getInfo(@PathVariable("id") Long id) {
        return cd90LossSettingService.selectCd90LossSettingById(id);
    }

    /**
     * 新增90度裁断损耗率设定
     */
    //@PreAuthorize(hasPermi = "cd90:loss:add")
    @Log(title = "ui.data.column.cd90.loss.modelName", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public AjaxResult add(@RequestBody Cd90LossSettingDto dto) {
        Cd90LossSetting cd90LossSetting = new Cd90LossSetting();
        BeanUtils.copyProperties(dto, cd90LossSetting);
        return toAjax(cd90LossSettingService.insertCd90LossSetting(cd90LossSetting));
    }

    /**
     * 修改90度裁断损耗率设定
     */
    //@PreAuthorize(hasPermi = "cd90:loss:edit")
    @Log(title = "ui.data.column.cd90.loss.modelName", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody Cd90LossSettingDto dto) {
        Cd90LossSetting cd90LossSetting = new Cd90LossSetting();
        BeanUtils.copyProperties(dto, cd90LossSetting);
        return toAjax(cd90LossSettingService.updateCd90LossSetting(cd90LossSetting));
    }

    /**
     * 删除90度裁断损耗率设定
     */
    //@PreAuthorize(hasPermi = "cd90:loss:remove")
    @Log(title = "ui.data.column.cd90.loss.modelName", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(cd90LossSettingService.deleteCd90LossSettingByIds(ids));
    }


    @Log(title = "ui.data.column.cd90.loss.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除全部(逻辑删)")
    @PostMapping("/deleteAll")
    public AjaxResult deleteAll() {
        cd90LossSettingService.deleteAll();
        return AjaxResult.success();
    }

    /**
     * 导出90度裁断损耗率设定列表
     */
    //@PreAuthorize(hasPermi = "cd90:loss:export")
    @Log(title = "ui.data.column.cd90.loss.modelName", businessType = BusinessType.EXPORT)
    @PostMapping("/getList")
    public List<Cd90LossSettingDto> getList(@RequestBody Cd90LossSettingDto dto) {
        dto.setOrderStr(orderStr());
        Cd90LossSetting cd90LossSetting = new Cd90LossSetting();
        BeanUtils.copyProperties(dto, cd90LossSetting);
        return cd90LossSettingService.selectCd90LossSettingList(cd90LossSetting);
    }

    /**
     * 校验90度裁断损耗率设定唯一性
     */
    @ApiOperation("校验90度裁断损耗率设定唯一性")
    @PostMapping("/checkCd90LossSettingUnique")
    public String checkCd90LossSettingUnique(@RequestBody Cd90LossSettingDto dto) {
        Cd90LossSetting cd90LossSetting = new Cd90LossSetting();
        BeanUtils.copyProperties(dto, cd90LossSetting);
        return cd90LossSettingService.checkCd90LossSettingUnique(cd90LossSetting);
    }

    @Log(title = "ui.data.column.cd90.loss.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<Cd90LossSettingDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return cd90LossSettingService.importData(list, updateSupport, importLogId);
    }

}
