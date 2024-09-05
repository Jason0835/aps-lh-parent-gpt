package com.zlt.aps.tc.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.tc.api.domain.entity.TcSidewallCodeColor;
import com.zlt.aps.tc.service.TcSidewallCodeColorService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 胎侧代码前缀颜色设定Controller
 *
 * @author zlt
 * @date 2022-01-14
 */
@RestController
@RequestMapping("/sidewallCodeColor")
public class TcSidewallCodeColorController extends BaseController
{
    @Autowired
    private TcSidewallCodeColorService tcSidewallCodeColorService;

    /**
     * 查询胎侧代码前缀颜色设定列表
     */
    @ApiOperation("查询胎侧代码前缀颜色设定列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody TcSidewallCodeColor tcSidewallCodeColor)
    {
        startPage("create_time desc");
        startPage();
        tcSidewallCodeColor.setOrderStr(orderStr());
        List<TcSidewallCodeColor> list = tcSidewallCodeColorService.selectTcSidewallCodeColorList(tcSidewallCodeColor);
        return getDataTable(list);
    }

    /**
     * 获取胎侧代码前缀颜色设定详细信息
     */
    @ApiOperation("获取胎侧代码前缀颜色设定详细信息")
    @GetMapping(value = "/{id}")
    public TcSidewallCodeColor getInfo(@PathVariable("id") Long id){
        return tcSidewallCodeColorService.selectTcSidewallCodeColorById(id);
    }

    /**
     * 新增胎侧代码前缀颜色设定
     */
    @Log(title = "ui.data.column.sidewallCodeColor.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增胎侧代码前缀颜色设定")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody TcSidewallCodeColor tcSidewallCodeColor){
        return toAjax(tcSidewallCodeColorService.insertTcSidewallCodeColor(tcSidewallCodeColor));
    }

    /**
     * 修改胎侧代码前缀颜色设定
     */
    @Log(title = "ui.data.column.sidewallCodeColor.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改胎侧代码前缀颜色设定")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody TcSidewallCodeColor tcSidewallCodeColor){
        return toAjax(tcSidewallCodeColorService.updateTcSidewallCodeColor(tcSidewallCodeColor));
    }

    /**
     * 删除胎侧代码前缀颜色设定
     */
    @Log(title = "ui.data.column.sidewallCodeColor.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除胎侧代码前缀颜色设定")
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids){
        return toAjax(tcSidewallCodeColorService.deleteTcSidewallCodeColorByIds(ids));
    }

    /**
     * 导出胎侧代码前缀颜色设定列表
     */
    @Log(title = "ui.data.column.sidewallCodeColor.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出胎侧代码前缀颜色设定列表")
    @PostMapping("/getList")
    public List<TcSidewallCodeColor> getList(@RequestBody TcSidewallCodeColor tcSidewallCodeColor){
        startPage();
        tcSidewallCodeColor.setOrderStr(orderStr());
        return  tcSidewallCodeColorService.selectTcSidewallCodeColorList(tcSidewallCodeColor);
    }

    /**
     * 校验胎侧代码前缀颜色设定唯一性
     */
    @ApiOperation("校验胎侧代码前缀颜色设定唯一性")
    @PostMapping("/checkTcSidewallCodeColorUnique")
    public String checkTcSidewallCodeColorUnique(@RequestBody TcSidewallCodeColor tcSidewallCodeColor){
        return tcSidewallCodeColorService.checkTcSidewallCodeColorUnique(tcSidewallCodeColor);
    }

    /**
     * 根据集合导入胎侧代码前缀颜色设定数据
     * @param list 集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId 导入日志id
     * @return 结果
     */
    @Log(title = "ui.data.column.sidewallCodeColor.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入胎侧代码前缀颜色设定数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<TcSidewallCodeColor> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return tcSidewallCodeColorService.importData(list, updateSupport, importLogId);
    }
}
