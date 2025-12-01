package com.zlt.aps.tc.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.tc.api.domain.dto.TcLossSettingDto;
import com.zlt.aps.tc.entity.TcLossSetting;
import com.zlt.aps.tc.service.TcLossSettingService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 胎侧损耗率设定Controller
 *
 * @author chen
 * @date 2021-07-13
 */
@RestController
@RequestMapping("/tc/loss")
public class TcLossSettingController extends BaseController {
    @Autowired
    private TcLossSettingService tcLossSettingService;

    /**
     * 查询胎侧损耗率设定列表
     */
    //@PreAuthorize(hasPermi = "tc:loss:list")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody TcLossSettingDto dto) {
        TcLossSetting tcLossSetting = new TcLossSetting();
        BeanUtils.copyProperties(dto, tcLossSetting);
        startPage();
        tcLossSetting.setOrderStr(orderStr());
        List<TcLossSettingDto> list = tcLossSettingService.selectTcLossSettingList(tcLossSetting);
        return getDataTable(list);
    }

    /**
     * 获取胎侧损耗率设定详细信息
     */
    //@PreAuthorize(hasPermi = "tc:loss:query")
    @GetMapping(value = "/{id}")
    public TcLossSettingDto getInfo(@PathVariable("id") Long id) {
        return tcLossSettingService.selectTcLossSettingById(id);
    }

    /**
     * 新增胎侧损耗率设定
     */
    @Log(title = "ui.data.column.tc.loss.modelName}", businessType = BusinessType.INSERT)
    //@PreAuthorize(hasPermi = "tc:loss:add")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody TcLossSettingDto dto) {
        TcLossSetting tcLossSetting = new TcLossSetting();
        BeanUtils.copyProperties(dto, tcLossSetting);
        return toAjax(tcLossSettingService.insertTcLossSetting(tcLossSetting));
    }

    /**
     * 修改胎侧损耗率设定
     */
    @Log(title = "ui.data.column.tc.loss.modelName}", businessType = BusinessType.UPDATE)
    //@PreAuthorize(hasPermi = "tc:loss:edit")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody TcLossSettingDto dto) {
        TcLossSetting tcLossSetting = new TcLossSetting();
        BeanUtils.copyProperties(dto, tcLossSetting);
        return toAjax(tcLossSettingService.updateTcLossSetting(tcLossSetting));
    }

    /**
     * 删除胎侧损耗率设定
     */
    @Log(title = "ui.data.column.tc.loss.modelName}", businessType = BusinessType.DELETE)
    //@PreAuthorize(hasPermi = "tc:loss:remove")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(tcLossSettingService.deleteTcLossSettingByIds(ids));
    }

    @Log(title = "ui.data.column.tc.loss.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除全部(逻辑删)")
    @PostMapping("/deleteAll")
    public AjaxResult deleteAll() {
        tcLossSettingService.deleteAll();
        return AjaxResult.success();
    }


    /**
     * 导出胎侧损耗率设定列表
     */
    @Log(title = "ui.data.column.tc.loss.modelName}", businessType = BusinessType.EXPORT)
    //@PreAuthorize(hasPermi = "tc:loss:export")
    @PostMapping("/getList")
    public List<TcLossSettingDto> getList(@RequestBody TcLossSettingDto dto) {
        TcLossSetting tcLossSetting = new TcLossSetting();
        BeanUtils.copyProperties(dto, tcLossSetting);
        startPage();
        tcLossSetting.setOrderStr(orderStr());
        return tcLossSettingService.selectTcLossSettingList(tcLossSetting);
    }

    /**
     * 校验胎侧损耗率设定唯一性
     */
    @ApiOperation("校验胎侧损耗率设定唯一性")
    @PostMapping("/checkTcLossSettingUnique")
    public String checkTcLossSettingUnique(@RequestBody TcLossSettingDto dto) {
        TcLossSetting tcLossSetting = new TcLossSetting();
        BeanUtils.copyProperties(dto, tcLossSetting);
        return tcLossSettingService.checkTcLossSettingUnique(tcLossSetting);
    }

    @Log(title = "ui.data.column.tc.loss.modelName}", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<TcLossSettingDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (StringUtils.isNull(list) || list.size() == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return tcLossSettingService.importData(list, updateSupport, importLogId);
    }

}
