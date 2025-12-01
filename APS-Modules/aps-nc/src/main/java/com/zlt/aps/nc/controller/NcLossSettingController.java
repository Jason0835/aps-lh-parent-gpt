package com.zlt.aps.nc.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.nc.api.domain.dto.NcLossSettingDto;
import com.zlt.aps.nc.entity.NcLossSetting;
import com.zlt.aps.nc.service.NcLossSettingService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 内衬损耗率设定Controller
 *
 * @author chen
 * @date 2021-07-13
 */
@RestController
@RequestMapping("/nc/loss")
public class NcLossSettingController extends BaseController {
    @Autowired
    private NcLossSettingService ncLossSettingService;

    /**
     * 查询内衬损耗率设定列表
     */
    //@PreAuthorize(hasPermi = "nc:loss:list")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody NcLossSettingDto dto) {
        startPage("a.create_time desc");
        NcLossSetting ncLossSetting = new NcLossSetting();
        BeanUtils.copyProperties(dto, ncLossSetting);
        startPage();
        ncLossSetting.setOrderStr(orderStr());
        List<NcLossSettingDto> list = ncLossSettingService.selectNcLossSettingList(ncLossSetting);
        return getDataTable(list);
    }

    /**
     * 获取内衬损耗率设定详细信息
     */
    //@PreAuthorize(hasPermi = "nc:loss:query")
    @GetMapping(value = "/{id}")
    public NcLossSettingDto getInfo(@PathVariable("id") Long id) {
        return ncLossSettingService.selectNcLossSettingById(id);
    }

    /**
     * 新增内衬损耗率设定
     */
    //@PreAuthorize(hasPermi = "nc:loss:add")
    @Log(title = "ui.data.column.nc.loss.modelName", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public AjaxResult add(@RequestBody NcLossSettingDto dto) {
        NcLossSetting ncLossSetting = new NcLossSetting();
        BeanUtils.copyProperties(dto, ncLossSetting);
        return toAjax(ncLossSettingService.insertNcLossSetting(ncLossSetting));
    }

    /**
     * 修改内衬损耗率设定
     */
    //@PreAuthorize(hasPermi = "nc:loss:edit")
    @Log(title = "ui.data.column.nc.loss.modelName", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody NcLossSettingDto dto) {
        NcLossSetting ncLossSetting = new NcLossSetting();
        BeanUtils.copyProperties(dto, ncLossSetting);
        return toAjax(ncLossSettingService.updateNcLossSetting(ncLossSetting));
    }

    /**
     * 删除内衬损耗率设定
     */
    //@PreAuthorize(hasPermi = "nc:loss:remove")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(ncLossSettingService.deleteNcLossSettingByIds(ids));
    }


    @Log(title = "ui.data.column.nc.loss.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除全部(逻辑删)")
    @PostMapping("/deleteAll")
    public AjaxResult deleteAll() {
        ncLossSettingService.deleteAll();
        return AjaxResult.success();
    }

    /**
     * 导出内衬损耗率设定列表
     */
    //@PreAuthorize(hasPermi = "nc:loss:export")
    @Log(title = "ui.data.column.nc.loss.modelName", businessType = BusinessType.EXPORT)
    @PostMapping("/getList")
    public List<NcLossSettingDto> getList(@RequestBody NcLossSettingDto dto) {
        NcLossSetting ncLossSetting = new NcLossSetting();
        BeanUtils.copyProperties(dto, ncLossSetting);
        startPage();
        ncLossSetting.setOrderStr(orderStr());
        return ncLossSettingService.selectNcLossSettingList(ncLossSetting);
    }

    /**
     * 校验内衬损耗率设定唯一性
     */
    @ApiOperation("校验内衬损耗率设定唯一性")
    @PostMapping("/checkNcLossSettingUnique")
    public String checkNcLossSettingUnique(@RequestBody NcLossSettingDto dto) {
        NcLossSetting ncLossSetting = new NcLossSetting();
        BeanUtils.copyProperties(dto, ncLossSetting);
        return ncLossSettingService.checkNcLossSettingUnique(ncLossSetting);
    }

    @Log(title = "ui.data.column.nc.loss.modelName", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    @ApiOperation("导入内衬损耗率信息")
    public AjaxResult importData(@RequestBody List<NcLossSettingDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (StringUtils.isNull(list) || list.size() == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return ncLossSettingService.importData(list, updateSupport, importLogId);
    }
}
