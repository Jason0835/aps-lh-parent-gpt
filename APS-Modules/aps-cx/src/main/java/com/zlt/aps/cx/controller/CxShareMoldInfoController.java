package com.zlt.aps.cx.controller;

import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cx.api.domain.entity.CxShareMoldInfo;
import com.zlt.aps.cx.service.CxShareMoldInfoService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 成型胎胚共用模具信息Controller
 *
 * @author chen
 * @date 2022-03-22
 */
@RestController
@RequestMapping("/shareMoldInfo")
public class CxShareMoldInfoController extends BaseController {
    @Autowired
    private CxShareMoldInfoService cxShareMoldInfoService;

    /**
     * 查询成型胎胚共用模具信息列表
     */
    @ApiOperation("查询成型胎胚共用模具信息列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody CxShareMoldInfo cxShareMoldInfo) {
        startPage();
        cxShareMoldInfo.setOrderStr(orderStr());
        List<CxShareMoldInfo> list = cxShareMoldInfoService.selectCxShareMoldInfoList(cxShareMoldInfo);
        return getDataTable(list);
    }

    /**
     * 获取成型胎胚共用模具信息详细信息
     */
    @ApiOperation("获取成型胎胚共用模具信息详细信息")
    @GetMapping(value = "/{id}")
    public CxShareMoldInfo getInfo(@PathVariable("id") Long id) {
        return cxShareMoldInfoService.selectCxShareMoldInfoById(id);
    }

    /**
     * 新增成型胎胚共用模具信息
     */
    @Log(title = "ui.data.column.shareMoldInfo.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增成型胎胚共用模具信息")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody CxShareMoldInfo cxShareMoldInfo) {
        if (UserConstants.NOT_UNIQUE.equals(cxShareMoldInfoService.checkCxShareMoldInfoUnique(cxShareMoldInfo))) {
            return AjaxResult.error(I18nUtil.getMessage("mes.error.message.cxShareMoldInfo.exist"));
        }
        return toAjax(cxShareMoldInfoService.insertCxShareMoldInfo(cxShareMoldInfo));
    }

    /**
     * 修改成型胎胚共用模具信息
     */
    @Log(title = "ui.data.column.shareMoldInfo.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改成型胎胚共用模具信息")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody CxShareMoldInfo cxShareMoldInfo) {
        if (UserConstants.NOT_UNIQUE.equals(cxShareMoldInfoService.checkCxShareMoldInfoUnique(cxShareMoldInfo))) {
            return AjaxResult.error(I18nUtil.getMessage("mes.error.message.cxShareMoldInfo.exist"));
        }
        return toAjax(cxShareMoldInfoService.updateCxShareMoldInfo(cxShareMoldInfo));
    }

    /**
     * 删除成型胎胚共用模具信息
     */
    @Log(title = "ui.data.column.shareMoldInfo.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除成型胎胚共用模具信息")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(cxShareMoldInfoService.deleteCxShareMoldInfoByIds(ids));
    }

    /**
     * 导出成型胎胚共用模具信息列表
     */
    @Log(title = "ui.data.column.shareMoldInfo.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出成型胎胚共用模具信息列表")
    @PostMapping("/getList")
    public List<CxShareMoldInfo> getList(@RequestBody CxShareMoldInfo cxShareMoldInfo) {
        cxShareMoldInfo.setOrderStr(orderStr());
        return cxShareMoldInfoService.selectCxShareMoldInfoList(cxShareMoldInfo);
    }

    /**
     * 校验成型胎胚共用模具信息唯一性
     */
    @ApiOperation("校验成型胎胚共用模具信息唯一性")
    @PostMapping("/checkCxShareMoldInfoUnique")
    public String checkCxShareMoldInfoUnique(@RequestBody CxShareMoldInfo cxShareMoldInfo) {
        return cxShareMoldInfoService.checkCxShareMoldInfoUnique(cxShareMoldInfo);
    }

    /**
     * 根据集合导入成型胎胚共用模具信息数据
     *
     * @param list          集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     * @return 结果
     */
    @Log(title = "ui.data.column.shareMoldInfo.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入成型胎胚共用模具信息数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<CxShareMoldInfo> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return cxShareMoldInfoService.importData(list, updateSupport, importLogId);
    }
}
