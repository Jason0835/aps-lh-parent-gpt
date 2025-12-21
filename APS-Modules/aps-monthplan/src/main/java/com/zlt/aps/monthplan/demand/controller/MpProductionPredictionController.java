package com.zlt.aps.monthplan.demand.controller;


import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.tlt.aps.redissonLock.annotation.RedissonLockAnno;
import com.zlt.aps.monthplan.api.domain.entity.MpProductionPrediction;
import com.zlt.aps.monthplan.demand.service.IMpProductionPredictionService;
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
* 文件名称：MpProductionPredictionController.java
* 描    述：S2-1002.未来产量预测 控制层类：....
*@author yelq
*@date 2025-12-21
*@version 1.0
*
 *  修改记录：
*     修改时间：...
*     修 改 人：yelq
*     修改内容：...
*/
@Slf4j
@Api(tags = "S2-1002.未来产量预测")
@RestController
@RequestMapping("/productionPrediction")
public class MpProductionPredictionController extends BusiController<MpProductionPrediction>
{
    @Autowired
    private IMpProductionPredictionService mpProductionPredictionService;

    /**
     * 查询S2-1002.未来产量预测列表
     */
    @RequiresPermissions( "monthplan:productionPrediction:list")
    @ApiOperation("查询S2-1002.未来产量预测列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody MpProductionPrediction mpProductionPrediction)
    {
        startPage("create_time desc");
        List<MpProductionPrediction> list = mpProductionPredictionService.selectMpProductionPredictionList(mpProductionPrediction);
        return getDataTable(list);
    }


    /**
     * 导出S2-1002.未来产量预测列表
     */
    @RequiresPermissions( "monthplan:productionPrediction:export")
    @Log(title = "S2-1002.未来产量预测", businessType = BusinessType.EXPORT)
    @PostMapping("/exportData/{fileName}")
    public byte[] exportData(@RequestBody MpProductionPrediction mpProductionPrediction,@PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return commonExport(mpProductionPrediction,fileName,response);
    }

    @Override
    public List<MpProductionPrediction> listExportData(MpProductionPrediction mpProductionPrediction) {
        startPage("create_time desc");
        return  mpProductionPredictionService.selectMpProductionPredictionList(mpProductionPrediction);
    }

    /**
     * 获取S2-1002.未来产量预测详细信息
     */
    @RequiresPermissions( "monthplan:productionPrediction:query")
    @ApiOperation("获取S2-1002.未来产量预测详细信息")
    @GetMapping(value = "/{id}")
    public MpProductionPrediction getInfo(@PathVariable("id") Long id)
    {
        return mpProductionPredictionService.selectMpProductionPredictionById(id);
    }

    /**
     * 新增S2-1002.未来产量预测
     */
    @Log(title = "ui.data.column.productionPrediction.modelName", businessType = BusinessType.INSERT)
    @RequiresPermissions( "monthplan:productionPrediction:add")
    @ApiOperation("新增S2-1002.未来产量预测")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody MpProductionPrediction mpProductionPrediction){
        return toAjax(mpProductionPredictionService.insertMpProductionPrediction(mpProductionPrediction));
    }

    /**
     * 修改S2-1002.未来产量预测
     */
    @Log(title = "ui.data.column.productionPrediction.modelName", businessType = BusinessType.UPDATE)
    @RequiresPermissions( "monthplan:productionPrediction:edit")
    @ApiOperation("修改S2-1002.未来产量预测")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody MpProductionPrediction mpProductionPrediction){
        return toAjax(mpProductionPredictionService.updateMpProductionPrediction(mpProductionPrediction));
    }

    /**
     * 删除S2-1002.未来产量预测
     */
    @Log(title = "ui.data.column.productionPrediction.modelName", businessType = BusinessType.DELETE)
    @RequiresPermissions( "monthplan:productionPrediction:remove")
    @ApiOperation("删除S2-1002.未来产量预测")
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids){
        return toAjax(mpProductionPredictionService.deleteMpProductionPredictionByIds(ids));
    }

    /**
     * 校验S2-1002.未来产量预测唯一性
     */
    @ApiOperation("校验S2-1002.未来产量预测唯一性")
    @PostMapping("/checkMpProductionPredictionUnique")
    public String checkMpProductionPredictionUnique(@RequestBody MpProductionPrediction mpProductionPrediction){
        return mpProductionPredictionService.checkMpProductionPredictionUnique(mpProductionPrediction);
    }

    /**
     * 根据集合导入S2-1002.未来产量预测数据
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.productionPrediction.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入S2-1002.未来产量预测数据")
    @PostMapping("/importData/{updateSupport}")
    public AjaxResult importData(@RequestBody ImportContext importContext, @PathVariable("updateSupport") boolean updateSupport) throws Exception {
        return commonImport(importContext,updateSupport);
    }

    @Override
    public AjaxResult doImportData(List<MpProductionPrediction> list, boolean updateSupport, long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return mpProductionPredictionService.importData(list, updateSupport, importLogId);
    }

    @ApiOperation("生成订单预测")
    @RedissonLockAnno(uniqueMark = "redissonLock:productionPrediction:createMonthPrediction:",
        expressions = {"#createCondition.factoryCode", "#createCondition.year", "#createCondition.month"},
        msgKey = "ui.data.alert.createMonthPrediction.run",
        waitTime = 5,
        leaseTime = 300
    )
    @PostMapping("/createMonthPrediction")
    public AjaxResult createMonthPrediction(@RequestBody MpProductionPrediction createCondition){
        return mpProductionPredictionService.createMonthPrediction(createCondition);
    }
}
