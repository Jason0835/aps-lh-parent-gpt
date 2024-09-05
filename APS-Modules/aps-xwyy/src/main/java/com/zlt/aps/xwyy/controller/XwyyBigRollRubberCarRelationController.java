package com.zlt.aps.xwyy.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.xwyy.api.domain.entity.XwyyBigRollRubberCarRelation;
import com.zlt.aps.xwyy.service.XwyyBigRollRubberCarRelationService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 帘布大卷原线胶料车数关系Controller
 *
 * @author Joran.Zhang
 * @date 2022-05-10
 */
@RestController
@RequestMapping("/bigRollRubberCarRelation")
public class XwyyBigRollRubberCarRelationController extends BaseController {
    @Autowired
    private XwyyBigRollRubberCarRelationService xwyyBigRollRubberCarRelationService;

    /**
     * 查询帘布大卷原线胶料车数关系列表
     */
    @ApiOperation("查询帘布大卷原线胶料车数关系列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody XwyyBigRollRubberCarRelation xwyyBigRollRubberCarRelation) {
        startPage();
        xwyyBigRollRubberCarRelation.setOrderStr(orderStr());
        List<XwyyBigRollRubberCarRelation> list = xwyyBigRollRubberCarRelationService.selectXwyyBigRollRubberCarRelationList(xwyyBigRollRubberCarRelation);
        return getDataTable(list);
    }

    /**
     * 获取帘布大卷原线胶料车数关系详细信息
     */
    @ApiOperation("获取帘布大卷原线胶料车数关系详细信息")
    @GetMapping(value = "/{id}")
    public XwyyBigRollRubberCarRelation getInfo(@PathVariable("id") Long id) {
        return xwyyBigRollRubberCarRelationService.selectXwyyBigRollRubberCarRelationById(id);
    }

    /**
     * 新增帘布大卷原线胶料车数关系
     */
    @Log(title = "ui.data.column.carRelation.modelName" , businessType = BusinessType.INSERT)
    @ApiOperation("新增帘布大卷原线胶料车数关系")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody XwyyBigRollRubberCarRelation xwyyBigRollRubberCarRelation) {
        return toAjax(xwyyBigRollRubberCarRelationService.insertXwyyBigRollRubberCarRelation(xwyyBigRollRubberCarRelation));
    }

    /**
     * 修改帘布大卷原线胶料车数关系
     */
    @Log(title = "ui.data.column.carRelation.modelName" , businessType = BusinessType.UPDATE)
    @ApiOperation("修改帘布大卷原线胶料车数关系")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody XwyyBigRollRubberCarRelation xwyyBigRollRubberCarRelation) {
        return toAjax(xwyyBigRollRubberCarRelationService.updateXwyyBigRollRubberCarRelation(xwyyBigRollRubberCarRelation));
    }

    /**
     * 删除帘布大卷原线胶料车数关系
     */
    @Log(title = "ui.data.column.carRelation.modelName" , businessType = BusinessType.DELETE)
    @ApiOperation("删除帘布大卷原线胶料车数关系")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(xwyyBigRollRubberCarRelationService.deleteXwyyBigRollRubberCarRelationByIds(ids));
    }

    /**
     * 导出帘布大卷原线胶料车数关系列表
     */
    @Log(title = "ui.data.column.bigRollRemind.modelName" , businessType = BusinessType.EXPORT)
    @ApiOperation("导出帘布大卷原线胶料车数关系列表")
    @PostMapping("/getList")
    public List<XwyyBigRollRubberCarRelation> getList(@RequestBody XwyyBigRollRubberCarRelation xwyyBigRollRubberCarRelation) {
        startPage();
        xwyyBigRollRubberCarRelation.setOrderStr(orderStr());
        return xwyyBigRollRubberCarRelationService.selectXwyyBigRollRubberCarRelationList(xwyyBigRollRubberCarRelation);
    }

    /**
     * 校验帘布大卷原线胶料车数关系唯一性
     */
    @ApiOperation("校验帘布大卷原线胶料车数关系唯一性")
    @PostMapping("/checkXwyyBigRollRubberCarRelationUnique")
    public String checkXwyyBigRollRubberCarRelationUnique(@RequestBody XwyyBigRollRubberCarRelation xwyyBigRollRubberCarRelation) {
        return xwyyBigRollRubberCarRelationService.checkXwyyBigRollRubberCarRelationUnique(xwyyBigRollRubberCarRelation);
    }

    /**
     * 根据集合导入帘布大卷原线胶料车数关系数据
     *
     * @param list          集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     * @return 结果
     */
    @Log(title = "ui.data.column.carRelation.modelName" , businessType = BusinessType.IMPORT)
    @ApiOperation("导入帘布大卷原线胶料车数关系数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<XwyyBigRollRubberCarRelation> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return xwyyBigRollRubberCarRelationService.importData(list, updateSupport, importLogId);
    }

    /**
     * 根据帘布大卷查询对应的关系
     */
    @ApiOperation("根据帘布大卷查询对应的关系")
    @PostMapping("/selectByBigRollCode")
    public XwyyBigRollRubberCarRelation selectByBigRollCode(@RequestBody XwyyBigRollRubberCarRelation xwyyBigRollRubberCarRelation) {
        return xwyyBigRollRubberCarRelationService.selectByBigRollCode(xwyyBigRollRubberCarRelation);
    }
}
