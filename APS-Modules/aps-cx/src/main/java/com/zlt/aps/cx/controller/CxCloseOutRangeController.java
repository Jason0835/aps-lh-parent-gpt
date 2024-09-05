package com.zlt.aps.cx.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cx.api.domain.entity.CxCloseOutRange;
import com.zlt.aps.cx.service.CxCloseOutRangeService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 成型收尾范围系数Controller
 *
 * @author zlt
 * @date 2021-12-28
 */
@RestController
@RequestMapping("/closeOutRange")
public class CxCloseOutRangeController extends BaseController
{
    @Autowired
    private CxCloseOutRangeService cxCloseOutRangeService;

    /**
     * 查询成型收尾范围系数列表
     */
    @ApiOperation("查询成型收尾范围系数列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody CxCloseOutRange cxCloseOutRange)
    {
        startPage();
        cxCloseOutRange.setOrderStr(orderStr());
        List<CxCloseOutRange> list = cxCloseOutRangeService.selectCxCloseOutRangeList(cxCloseOutRange);
        return getDataTable(list);
    }

    /**
     * 获取成型收尾范围系数详细信息
     */
    @ApiOperation("获取成型收尾范围系数详细信息")
    @GetMapping(value = "/{id}")
    public CxCloseOutRange getInfo(@PathVariable("id") Long id){
        return cxCloseOutRangeService.selectCxCloseOutRangeById(id);
    }

    /**
     * 新增成型收尾范围系数
     */
    @Log(title = "ui.data.column.closeOutRange.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增成型收尾范围系数")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody CxCloseOutRange cxCloseOutRange){
        return toAjax(cxCloseOutRangeService.insertCxCloseOutRange(cxCloseOutRange));
    }

    /**
     * 修改成型收尾范围系数
     */
    @Log(title = "ui.data.column.closeOutRange.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改成型收尾范围系数")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody CxCloseOutRange cxCloseOutRange){
        return toAjax(cxCloseOutRangeService.updateCxCloseOutRange(cxCloseOutRange));
    }

    /**
     * 删除成型收尾范围系数
     */
    @Log(title = "ui.data.column.closeOutRange.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除成型收尾范围系数")
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids){
        return toAjax(cxCloseOutRangeService.deleteCxCloseOutRangeByIds(ids));
    }

    /**
     * 导出成型收尾范围系数列表
     */
    @Log(title = "ui.data.column.closeOutRange.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出成型收尾范围系数列表")
    @PostMapping("/getList")
    public List<CxCloseOutRange> getList(@RequestBody CxCloseOutRange cxCloseOutRange){
        startPage();
        cxCloseOutRange.setOrderStr(orderStr());
        return  cxCloseOutRangeService.selectCxCloseOutRangeList(cxCloseOutRange);
    }

    /**
     * 校验成型收尾范围系数唯一性
     */
    @ApiOperation("校验成型收尾范围系数唯一性")
    @PostMapping("/checkCxCloseOutRangeUnique")
    public String checkCxCloseOutRangeUnique(@RequestBody CxCloseOutRange cxCloseOutRange){
        return cxCloseOutRangeService.checkCxCloseOutRangeUnique(cxCloseOutRange);
    }

    /**
     * 根据集合导入成型收尾范围系数数据
     * @param list 集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId 导入日志id
     * @return 结果
     */
    @Log(title = "ui.data.column.closeOutRange.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入成型收尾范围系数数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<CxCloseOutRange> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return cxCloseOutRangeService.importData(list, updateSupport, importLogId);
    }
}
