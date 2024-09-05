package com.zlt.aps.xwyy.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.xwyy.api.domain.entity.XwyyBigRollOriginalBrand;
import com.zlt.aps.xwyy.service.XwyyBigRollOriginalBrandService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 帘布大卷原线品牌Controller
 *
 * @author chen
 * @date 2022-05-11
 */
@RestController
@RequestMapping("/bigRollOriginalBrand")
public class XwyyBigRollOriginalBrandController extends BaseController {
    @Autowired
    private XwyyBigRollOriginalBrandService xwyyBigRollOriginalBrandService;

    /**
     * 查询帘布大卷原线品牌列表
     */
    @ApiOperation("查询帘布大卷原线品牌列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody XwyyBigRollOriginalBrand xwyyBigRollOriginalBrand) {
        startPage();
        xwyyBigRollOriginalBrand.setOrderStr(orderStr());
        List<XwyyBigRollOriginalBrand> list = xwyyBigRollOriginalBrandService.selectXwyyBigRollOriginalBrandList(xwyyBigRollOriginalBrand);
        return getDataTable(list);
    }

    /**
     * 获取帘布大卷原线品牌详细信息
     */
    @ApiOperation("获取帘布大卷原线品牌详细信息")
    @GetMapping(value = "/{id}")
    public XwyyBigRollOriginalBrand getInfo(@PathVariable("id") Long id) {
        return xwyyBigRollOriginalBrandService.selectXwyyBigRollOriginalBrandById(id);
    }

    /**
     * 新增帘布大卷原线品牌
     */
    @Log(title = "ui.data.column.bigRollOriginalBrand.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增帘布大卷原线品牌")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody XwyyBigRollOriginalBrand xwyyBigRollOriginalBrand) {
        return toAjax(xwyyBigRollOriginalBrandService.insertXwyyBigRollOriginalBrand(xwyyBigRollOriginalBrand));
    }

    /**
     * 修改帘布大卷原线品牌
     */
    @Log(title = "ui.data.column.bigRollOriginalBrand.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改帘布大卷原线品牌")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody XwyyBigRollOriginalBrand xwyyBigRollOriginalBrand) {
        return toAjax(xwyyBigRollOriginalBrandService.updateXwyyBigRollOriginalBrand(xwyyBigRollOriginalBrand));
    }

    /**
     * 删除帘布大卷原线品牌
     */
    @Log(title = "ui.data.column.bigRollOriginalBrand.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除帘布大卷原线品牌")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(xwyyBigRollOriginalBrandService.deleteXwyyBigRollOriginalBrandByIds(ids));
    }

    /**
     * 导出帘布大卷原线品牌列表
     */
    @Log(title = "ui.data.column.bigRollOriginalBrand.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出帘布大卷原线品牌列表")
    @PostMapping("/getList")
    public List<XwyyBigRollOriginalBrand> getList(@RequestBody XwyyBigRollOriginalBrand xwyyBigRollOriginalBrand) {
        startPage();
        xwyyBigRollOriginalBrand.setOrderStr(orderStr());
        return xwyyBigRollOriginalBrandService.selectXwyyBigRollOriginalBrandList(xwyyBigRollOriginalBrand);
    }

    /**
     * 校验帘布大卷原线品牌唯一性
     */
    @ApiOperation("校验帘布大卷原线品牌唯一性")
    @PostMapping("/checkXwyyBigRollOriginalBrandUnique")
    public String checkXwyyBigRollOriginalBrandUnique(@RequestBody XwyyBigRollOriginalBrand xwyyBigRollOriginalBrand) {
        return xwyyBigRollOriginalBrandService.checkXwyyBigRollOriginalBrandUnique(xwyyBigRollOriginalBrand);
    }

    /**
     * 根据集合导入帘布大卷原线品牌数据
     *
     * @param list          集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     * @return 结果
     */
    @Log(title = "ui.data.column.bigRollOriginalBrand.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入帘布大卷原线品牌数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<XwyyBigRollOriginalBrand> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return xwyyBigRollOriginalBrandService.importData(list, updateSupport, importLogId);
    }
}
