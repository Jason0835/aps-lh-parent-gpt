package com.zlt.aps.dj.controller;

import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.dj.api.domain.dto.DjLossSettingDto;
import com.zlt.aps.dj.api.domain.entity.DjLossSetting;
import com.zlt.aps.dj.service.DjLossSettingService;

import io.swagger.annotations.ApiOperation;

/**
 * 垫胶损耗率设定Controller
 *
 * @author chen
 * @date 2021-07-13
 */
@RestController
@RequestMapping("/dj/loss")
public class DjLossSettingController extends BaseController {
    @Autowired
    private DjLossSettingService ncLossSettingService;

    /**
     * 查询垫胶损耗率设定列表
     */
    //@PreAuthorize(hasPermi = "dj:loss:list")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody DjLossSettingDto dto) {
        startPage("a.create_time desc");
        DjLossSetting ncLossSetting = new DjLossSetting();
        BeanUtils.copyProperties(dto, ncLossSetting);
        startPage();
        ncLossSetting.setOrderStr(orderStr());
        List<DjLossSettingDto> list = ncLossSettingService.selectNcLossSettingList(ncLossSetting);
        return getDataTable(list);
    }

    /**
     * 获取垫胶损耗率设定详细信息
     */
    //@PreAuthorize(hasPermi = "dj:loss:query")
    @GetMapping(value = "/{id}")
    public DjLossSettingDto getInfo(@PathVariable("id") Long id) {
        return ncLossSettingService.selectNcLossSettingById(id);
    }

    /**
     * 新增垫胶损耗率设定
     */
    //@PreAuthorize(hasPermi = "dj:loss:add")
    @Log(title = "ui.data.column.nc.loss.modelName", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public AjaxResult add(@RequestBody DjLossSettingDto dto) {
        DjLossSetting ncLossSetting = new DjLossSetting();
        BeanUtils.copyProperties(dto, ncLossSetting);
        return toAjax(ncLossSettingService.insertNcLossSetting(ncLossSetting));
    }

    /**
     * 修改垫胶损耗率设定
     */
    //@PreAuthorize(hasPermi = "dj:loss:edit")
    @Log(title = "ui.data.column.nc.loss.modelName", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody DjLossSettingDto dto) {
        DjLossSetting ncLossSetting = new DjLossSetting();
        BeanUtils.copyProperties(dto, ncLossSetting);
        return toAjax(ncLossSettingService.updateNcLossSetting(ncLossSetting));
    }

    /**
     * 删除垫胶损耗率设定
     */
    //@PreAuthorize(hasPermi = "dj:loss:remove")
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
     * 导出垫胶损耗率设定列表
     */
    //@PreAuthorize(hasPermi = "dj:loss:export")
    @Log(title = "ui.data.column.nc.loss.modelName", businessType = BusinessType.EXPORT)
    @PostMapping("/getList")
    public List<DjLossSettingDto> getList(@RequestBody DjLossSettingDto dto) {
        DjLossSetting ncLossSetting = new DjLossSetting();
        BeanUtils.copyProperties(dto, ncLossSetting);
        startPage();
        ncLossSetting.setOrderStr(orderStr());
        return ncLossSettingService.selectNcLossSettingList(ncLossSetting);
    }

    /**
     * 校验垫胶损耗率设定唯一性
     */
    @ApiOperation("校验垫胶损耗率设定唯一性")
    @PostMapping("/checkNcLossSettingUnique")
    public String checkNcLossSettingUnique(@RequestBody DjLossSettingDto dto) {
        DjLossSetting ncLossSetting = new DjLossSetting();
        BeanUtils.copyProperties(dto, ncLossSetting);
        return ncLossSettingService.checkNcLossSettingUnique(ncLossSetting);
    }

    @Log(title = "ui.data.column.nc.loss.modelName", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    @ApiOperation("导入垫胶损耗率信息")
    public AjaxResult importData(@RequestBody List<DjLossSettingDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (StringUtils.isNull(list) || list.size() == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return ncLossSettingService.importData(list, updateSupport, importLogId);
    }
}
