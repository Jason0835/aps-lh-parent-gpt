package com.zlt.aps.gsq.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import com.zlt.aps.gsq.api.domain.dto.GsqLossSettingDto;
import com.zlt.aps.gsq.entity.GsqLossSetting;
import com.zlt.aps.gsq.service.GsqLossSettingService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 钢丝圈损耗率设定Controller
 *
 * @author chen
 * @date 2021-07-13
 */
@RestController
@RequestMapping("/gsq/loss")
public class GsqLossSettingController extends BaseController {
    @Autowired
    private GsqLossSettingService gsqLossSettingService;

    /**
     * 查询钢丝圈损耗率设定列表
     */
    @ApiOperation("查询钢丝圈损耗率设定列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody GsqLossSettingDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        GsqLossSetting gsqLossSetting = new GsqLossSetting();
        BeanUtils.copyProperties(dto, gsqLossSetting);
        List<GsqLossSettingDto> list = gsqLossSettingService.selectGsqLossSettingList(gsqLossSetting);
        return getDataTable(list);
    }

    /**
     * 获取钢丝圈损耗率设定详细信息
     */
    @ApiOperation("获取钢丝圈损耗率设定详细信息")
    @GetMapping(value = "/{id}")
    public GsqLossSettingDto getInfo(@PathVariable("id") Long id) {
        return gsqLossSettingService.selectGsqLossSettingById(id);
    }

    /**
     * 新增钢丝圈损耗率设定
     */
    @Log(title = "ui.data.column.gsq.loss.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增钢丝圈损耗率设定")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody GsqLossSettingDto dto) {
        GsqLossSetting gsqLossSetting = new GsqLossSetting();
        BeanUtils.copyProperties(dto, gsqLossSetting);
        return toAjax(gsqLossSettingService.insertGsqLossSetting(gsqLossSetting));
    }

    /**
     * 修改钢丝圈损耗率设定
     */
    @Log(title = "ui.data.column.gsq.loss.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改钢丝圈损耗率设定")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody GsqLossSettingDto dto) {
        GsqLossSetting gsqLossSetting = new GsqLossSetting();
        BeanUtils.copyProperties(dto, gsqLossSetting);
        return toAjax(gsqLossSettingService.updateGsqLossSetting(gsqLossSetting));
    }

    /**
     * 删除钢丝圈损耗率设定
     */
    @Log(title = "ui.data.column.gsq.loss.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除钢丝圈损耗率设定")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(gsqLossSettingService.deleteGsqLossSettingByIds(ids));
    }


    @Log(title = "ui.data.column.gsq.loss.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除全部(逻辑删)")
    @PostMapping("/deleteAll")
    public AjaxResult deleteAll() {
        gsqLossSettingService.deleteAll();
        return AjaxResult.success();
    }


    /**
     * 导出钢丝圈损耗率设定列表
     */
    @Log(title = "ui.data.column.gsq.loss.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出钢丝圈损耗率设定列表")
    @PostMapping("/getList")
    public List<GsqLossSettingDto> getList(@RequestBody GsqLossSettingDto dto) {
        dto.setOrderStr(orderStr());
        GsqLossSetting gsqLossSetting = new GsqLossSetting();
        BeanUtils.copyProperties(dto, gsqLossSetting);
        return gsqLossSettingService.selectGsqLossSettingList(gsqLossSetting);
    }

    /**
     * 校验钢丝圈损耗率设定唯一性
     */
    @ApiOperation("校验钢丝圈损耗率设定唯一性")
    @PostMapping("/checkGsqLossSettingUnique")
    public String checkGsqLossSettingUnique(@RequestBody GsqLossSettingDto dto) {
        GsqLossSetting gsqLossSetting = new GsqLossSetting();
        BeanUtils.copyProperties(dto, gsqLossSetting);
        return gsqLossSettingService.checkGsqLossSettingUnique(gsqLossSetting);
    }

    @Log(title = "ui.data.column.gsq.loss.modelName", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    @ApiOperation("导入钢丝圈损耗率信息")
    public AjaxResult importData(@RequestBody List<GsqLossSettingDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtil.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return gsqLossSettingService.importData(list, updateSupport, importLogId);
    }
}
