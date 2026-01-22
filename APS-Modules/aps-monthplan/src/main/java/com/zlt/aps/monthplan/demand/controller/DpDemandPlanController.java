package com.zlt.aps.monthplan.demand.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.redissonLock.annotation.RedissonLockAnno;
import com.zlt.aps.monthplan.api.domain.entity.DpDemandPlan;
import com.zlt.aps.monthplan.common.utils.RequirementVersionService;
import com.zlt.aps.monthplan.common.utils.StringUtil;
import com.zlt.aps.monthplan.demand.mapper.DpDemandPlanEntityMapper;
import com.zlt.aps.monthplan.demand.service.IDpDemandPlanService;
import com.zlt.aps.monthplan.demand.service.impl.DpDemandPlanServiceImpl;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService ;

/**
* Copyright (c) 2022, All rights reserved。
* 文件名称：DpDemandPlanController.java
* 描    述：需求计划 控制层类：....
*@author yelq
*@date 2025-12-25
*@version 1.0
*
 *  修改记录：
*     修改时间：...
*     修 改 人：yelq
*     修改内容：...
*/
@Slf4j
@Api(tags = "需求计划")
@AllArgsConstructor
@RestController
@RequestMapping("/demandPlan")
public class DpDemandPlanController extends AbstractDocBizController<DpDemandPlan> {

    private final IDpDemandPlanService dpDemandPlanService;
    private final RequirementVersionService requirementVersionService;
    private final DpDemandPlanEntityMapper entityMapper;


    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.dpDemandPlan.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody DpDemandPlan billVO){
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.dpDemandPlan.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids){
        return super.removeByIds(ids);
    }


    /**
     * 获取需求计划详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public DpDemandPlan getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入需求计划数据
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.dpDemandPlan.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext,updateSupport);
    }

    @Override
    protected IDocService getDocService(){
        return dpDemandPlanService;
    }



    @Override
    protected String getTypeCode(){
        return "2025122521";
    }

    @ApiOperation("生成需求计划")
    @RedissonLockAnno(uniqueMark = "redissonLock:demandPlan:createMonthRequire:",
        expressions = {"#createCondition.factoryCode", "#createCondition.year", "#createCondition.month"},
        msgKey = "ui.data.alert.createMonthRequire.run",
        waitTime = 5,
        leaseTime = 300
    )
    @PostMapping("/createMonthRequire")
    public AjaxResult createMonthRequire(@RequestBody DpDemandPlan createCondition){
        if (createCondition == null || StringUtil.isEmpty(createCondition.getMonthPlanVersion())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.demandPlan.isnull"));
        }

        QueryWrapper<DpDemandPlan> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("MONTH_PLAN_VERSION", createCondition.getMonthPlanVersion());
        entityMapper.selectCount(queryWrapper);
        if (entityMapper.selectCount(queryWrapper) > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.demandPlan.notUnique"));
        }

        dpDemandPlanService.createMonthRequire(createCondition);
        return AjaxResult.success();
    }

    @ApiOperation("生成需求计划版本")
    @PostMapping("/createMonthRequireVersion")
    public AjaxResult createMonthRequireVersion(){
        return AjaxResult.success( requirementVersionService.generateVersion(DpDemandPlanServiceImpl.PREFIX));
    }

    @ApiOperation("查询需求计划版本号")
    @PostMapping("/findMonthPlanVersion")
    public AjaxResult findMonthPlanVersion(@RequestBody DpDemandPlan queryCondition){
        return AjaxResult.success(dpDemandPlanService.findMonthPlanVersion(queryCondition));
    }

}
