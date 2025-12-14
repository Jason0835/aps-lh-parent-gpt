package com.zlt.aps.monthplan.demand.controller;


import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.tlt.aps.redissonLock.annotation.RedissonLockAnno;
import com.zlt.aps.monthplan.api.domain.entity.SupplyOrderPool;
import com.zlt.aps.monthplan.demand.service.ISupplyOrderPoolService;
import com.zlt.common.controller.BusiController;
import lombok.extern.slf4j.Slf4j;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

import com.ruoyi.common.core.web.page.TableDataInfo;

/**
* Copyright (c) 2022, All rights reserved。
* 文件名称：SupplyOrderPoolController.java
* 描    述：供应链订单池 控制层类：....
*@author zlt
*@date 2025-12-06
*@version 1.0
*
 *  修改记录：
*     修改时间：...
*     修 改 人：zlt
*     修改内容：...
*/
@Slf4j
@Api(tags = "供应链订单池")
@RestController
@RequestMapping("/supplyOrderPool")
public class SupplyOrderPoolController extends BusiController<SupplyOrderPool>
{
    @Autowired
    private ISupplyOrderPoolService supplyOrderPoolService;

    /**
     * 查询供应链订单池列表
     */
    @RequiresPermissions( "monthplan:supplyOrderPool:list")
    @ApiOperation("查询供应链订单池列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody SupplyOrderPool supplyOrderPool)
    {
        startPage("create_time desc");
        List<SupplyOrderPool> list = supplyOrderPoolService.selectSupplyOrderPoolList(supplyOrderPool);
        return getDataTable(list);
    }


    /**
     * 导出供应链订单池列表
     */
    @RequiresPermissions( "monthplan:supplyOrderPool:export")
    @Log(title = "供应链订单池", businessType = BusinessType.EXPORT)
    @PostMapping("/exportData/{fileName}")
    public byte[] exportData(@RequestBody SupplyOrderPool supplyOrderPool,@PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return commonExport(supplyOrderPool,fileName,response);
    }

    @Override
    public List<SupplyOrderPool> listExportData(SupplyOrderPool supplyOrderPool) {
        startPage("create_time desc");
        return  supplyOrderPoolService.selectSupplyOrderPoolList(supplyOrderPool);
    }

    /**
     * 获取供应链订单池详细信息
     */
    @RequiresPermissions( "monthplan:supplyOrderPool:query")
    @ApiOperation("获取供应链订单池详细信息")
    @GetMapping(value = "/{id}")
    public SupplyOrderPool getInfo(@PathVariable("id") Long id)
    {
        return supplyOrderPoolService.selectSupplyOrderPoolById(id);
    }

    /**
     * 新增供应链订单池
     */
    @Log(title = "ui.data.column.supplyOrderPool.modelName", businessType = BusinessType.INSERT)
    @RequiresPermissions( "monthplan:supplyOrderPool:add")
    @ApiOperation("新增供应链订单池")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody SupplyOrderPool supplyOrderPool){
        return toAjax(supplyOrderPoolService.insertSupplyOrderPool(supplyOrderPool));
    }

    /**
     * 修改供应链订单池
     */
    @Log(title = "ui.data.column.supplyOrderPool.modelName", businessType = BusinessType.UPDATE)
    @RequiresPermissions( "monthplan:supplyOrderPool:edit")
    @ApiOperation("修改供应链订单池")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody SupplyOrderPool supplyOrderPool){
        return toAjax(supplyOrderPoolService.updateSupplyOrderPool(supplyOrderPool));
    }

    /**
     * 删除供应链订单池
     */
    @Log(title = "ui.data.column.supplyOrderPool.modelName", businessType = BusinessType.DELETE)
    @RequiresPermissions( "monthplan:supplyOrderPool:remove")
    @ApiOperation("删除供应链订单池")
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids){
        return toAjax(supplyOrderPoolService.deleteSupplyOrderPoolByIds(ids));
    }

    /**
     * 校验供应链订单池唯一性
     */
    @ApiOperation("校验供应链订单池唯一性")
    @PostMapping("/checkSupplyOrderPoolUnique")
    public String checkSupplyOrderPoolUnique(@RequestBody SupplyOrderPool supplyOrderPool){
        return supplyOrderPoolService.checkSupplyOrderPoolUnique(supplyOrderPool);
    }

    /**
     * 根据集合导入供应链订单池数据
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.supplyOrderPool.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入供应链订单池数据")
    @PostMapping("/importData/{updateSupport}")
    public AjaxResult importData(@RequestBody ImportContext importContext, @PathVariable("updateSupport") boolean updateSupport) throws Exception {
        return commonImport(importContext,updateSupport);
    }

    @Override
    public AjaxResult doImportData(List<SupplyOrderPool> list, boolean updateSupport, long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return supplyOrderPoolService.importData(list, updateSupport, importLogId);
    }

    @ApiOperation("生成周期排产储备")
    @RedissonLockAnno(uniqueMark = "redissonLock:supplyOrderPool:createCycleStockUp:",
        msgKey = "ui.data.alert.createCycleStockUp.run",
        waitTime = 5,
        leaseTime = 300
    )
    @PostMapping("/createCycleStockUp")
    public AjaxResult createCycleStockUp(@RequestBody SupplyOrderPool supplyOrderPool){
        supplyOrderPoolService.createCycleStockUp(supplyOrderPool);
        return AjaxResult.success();
    }

    @ApiOperation("生成常规储备")
    @RedissonLockAnno(uniqueMark = "redissonLock:supplyOrderPool:createPrecedentStockUp:",
        msgKey = "ui.data.alert.createPrecedentStockUp.run",
        waitTime = 5,
        leaseTime = 300
    )
    @PostMapping("/createPrecedentStockUp")
    public AjaxResult createPrecedentStockUp(@RequestBody SupplyOrderPool supplyOrderPool){
        supplyOrderPoolService.createPrecedentStockUp(supplyOrderPool);
        return AjaxResult.success();
    }
}
