package com.zlt.aps.cd15.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.cd15.api.domain.dto.Cd15LossSettingDto;
import com.zlt.aps.cd15.entity.Cd15LossSetting;
import com.zlt.aps.cd15.service.Cd15LossSettingService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 15度裁断损耗率设定Controller
 *
 * @author chen
 * @date 2021-07-19
 */
@RestController
@RequestMapping("/loss")
public class Cd15LossSettingController extends BaseController {
    @Autowired
    private Cd15LossSettingService cd15LossSettingService;

    /**
     * 查询15度裁断损耗率设定列表
     */
    //@PreAuthorize(hasPermi = "cd15:loss:list")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody Cd15LossSettingDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        Cd15LossSetting cd15LossSetting = new Cd15LossSetting();
        BeanUtils.copyProperties(dto, cd15LossSetting);
        List<Cd15LossSettingDto> list = cd15LossSettingService.selectCd15LossSettingList(cd15LossSetting);
        return getDataTable(list);
    }

    /**
     * 获取15度裁断损耗率设定详细信息
     */
    //@PreAuthorize(hasPermi = "cd15:loss:query")
    @GetMapping(value = "/{id}")
    public Cd15LossSettingDto getInfo(@PathVariable("id") Long id) {
        return cd15LossSettingService.selectCd15LossSettingById(id);
    }

    /**
     * 新增15度裁断损耗率设定
     */
    @Log(title = "ui.data.column.cd15.loss.modelName", businessType = BusinessType.INSERT)
    //@PreAuthorize(hasPermi = "cd15:loss:add")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody Cd15LossSettingDto dto) {
        Cd15LossSetting cd15LossSetting = new Cd15LossSetting();
        BeanUtils.copyProperties(dto, cd15LossSetting);
        return toAjax(cd15LossSettingService.insertCd15LossSetting(cd15LossSetting));
    }

    /**
     * 修改15度裁断损耗率设定
     */
    @Log(title = "ui.data.column.cd15.loss.modelName", businessType = BusinessType.UPDATE)
    //@PreAuthorize(hasPermi = "cd15:loss:edit")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody Cd15LossSettingDto dto) {
        Cd15LossSetting cd15LossSetting = new Cd15LossSetting();
        BeanUtils.copyProperties(dto, cd15LossSetting);
        return toAjax(cd15LossSettingService.updateCd15LossSetting(cd15LossSetting));
    }

    /**
     * 删除15度裁断损耗率设定
     */
    @Log(title = "ui.data.column.cd15.loss.modelName", businessType = BusinessType.DELETE)
    //@PreAuthorize(hasPermi = "cd15:loss:remove")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(cd15LossSettingService.deleteCd15LossSettingByIds(ids));
    }

    @Log(title = "ui.data.column.cd15.loss.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除全部(逻辑删)")
    @PostMapping("/deleteAll")
    public AjaxResult deleteAll() {
        cd15LossSettingService.deleteAll();
        return AjaxResult.success();
    }

    /**
     * 导出15度裁断损耗率设定列表
     */
    @Log(title = "ui.data.column.cd15.loss.modelName", businessType = BusinessType.EXPORT)
    //@PreAuthorize(hasPermi = "cd15:loss:export")
    @PostMapping("/getList")
    public List<Cd15LossSettingDto> getList(@RequestBody Cd15LossSettingDto dto) {
        dto.setOrderStr(orderStr());
        Cd15LossSetting cd15LossSetting = new Cd15LossSetting();
        BeanUtils.copyProperties(dto, cd15LossSetting);
        return cd15LossSettingService.selectCd15LossSettingList(cd15LossSetting);
    }

    /**
     * 校验15度裁断损耗率设定唯一性
     */
    @ApiOperation("校验15度裁断损耗率设定唯一性")
    @PostMapping("/checkCd15LossSettingUnique")
    public String checkCd15LossSettingUnique(@RequestBody Cd15LossSettingDto dto) {
        Cd15LossSetting cd15LossSetting = new Cd15LossSetting();
        BeanUtils.copyProperties(dto, cd15LossSetting);
        return cd15LossSettingService.checkCd15LossSettingUnique(cd15LossSetting);
    }

    @Log(title = "ui.data.column.cd15.loss.modelName", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    @ApiOperation("导入15度裁断损耗率信息")
    public AjaxResult importData(@RequestBody List<Cd15LossSettingDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (StringUtils.isNull(list) || list.size() == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return cd15LossSettingService.importData(list, updateSupport, importLogId);
    }
}
