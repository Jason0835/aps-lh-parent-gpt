package com.zlt.aps.monthplan.factory.controller;

import cn.hutool.core.collection.CollUtil;
import com.ruoyi.common.core.utils.PageUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import com.zlt.aps.monthplan.api.domain.entity.MpStructureAllocation;
import com.zlt.aps.monthplan.common.utils.PubUtil;
import com.zlt.aps.monthplan.factory.mapper.MpStructureAllocationEntityMapper;
import com.zlt.aps.monthplan.factory.service.IFactoryMonthPlanProductionFinalResultService;
import com.zlt.aps.monthplan.factory.service.IMpStructureAllocationService;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpStructureAllocationController.java
 * 描    述：排产过程_结构排产 控制层类：....
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-29
 */
@Slf4j
@Api(tags = "排产过程_结构排产")
@RestController
@RequiredArgsConstructor
@RequestMapping("/mpStructureAllocation")
public class MpStructureAllocationController extends AbstractDocBizController<MpStructureAllocation> {

    private final IMpStructureAllocationService mpStructureAllocationService;

    private final MpStructureAllocationEntityMapper entityMapper;

    private final IFactoryMonthPlanProductionFinalResultService monthPlanProductionFinalResultService;

    /**
     * 查询排产过程_结构排产列表
     *
     * @param queryCondition 查询条件
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody MpStructureAllocation queryCondition) {
        try {
            startPage();
            List<MpStructureAllocation> list = mpStructureAllocationService.getDataList(queryCondition);
            filterStructureAllocationList(queryCondition, list);
            return getDataTable(list);
        } finally {
            PageUtils.clearPage();
        }
    }


    private void filterStructureAllocationList(MpStructureAllocation queryCondition, List<MpStructureAllocation> list) {
        if (StringUtils.isNotEmpty(queryCondition.getProductionVersion())) {
            return;
        }
        // 排产版本为空，默认查询当前年月最新的排产版本
        FactoryMonthPlanProductionFinalResult param = new FactoryMonthPlanProductionFinalResult();
        param.setFactoryCode(queryCondition.getFactoryCode());
        param.setYear(queryCondition.getYear());
        param.setMonth(queryCondition.getMonth());
        List<FactoryMonthPlanProductionFinalResult> monthPlanResultList = monthPlanProductionFinalResultService.listMonthProdFinalPlans(param);
        if (PubUtil.isEmpty(monthPlanResultList)) {
            list.clear();
            return;
        }
        // 排产版本
        String productionVersion = monthPlanResultList.get(0).getProductionVersion();
        log.info("排产版本为空，默认查询当前年月最新的排产版本:{}", productionVersion);
        CollUtil.filter(list, item -> StringUtils.equals(item.getProductionVersion(), productionVersion));
    }



    @Override
    protected List<MpStructureAllocation> listExportData(MpStructureAllocation condition) {
        if (null == condition) {
            return Collections.emptyList();
        }
        if (null == condition.getYear() || null == condition.getMonth() || StringUtils.isBlank(condition.getFactoryCode())) {
            return Collections.emptyList();
        }
        if (StringUtils.isBlank(condition.getMonthPlanVersion()) || StringUtils.isBlank(condition.getProductionVersion())) {
            return Collections.emptyList();
        }
        return mpStructureAllocationService.getDataList(condition);
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.mpStructureAllocation.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody MpStructureAllocation billVO) {
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.mpStructureAllocation.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    @Override
    protected IDocService getDocService() {
        return mpStructureAllocationService;
    }

    @Override
    protected String getTypeCode() {
        return "MDM0408";
    }


    /**
     * 查询版本列表
     */
    @ApiOperation("查询版本列表")
    @PostMapping("/getVersionList")
    public TableDataInfo getVersionList(@RequestBody MpStructureAllocation queryVO) {
        this.startPage();
        List<MpStructureAllocation> list = entityMapper.getVersionList(queryVO);
        this.clearPage();
        return this.getDataTable(list);
    }

    /**
     * 获取日期最接近的下一个结构
     * @param queryCondition 查询条件
     */
    @ApiOperation("获取日期最接近的下一个结构")
    @PostMapping("/getNextStructure")
    public MpStructureAllocation getNextStructure(@RequestBody MpStructureAllocation queryCondition) {
        return mpStructureAllocationService.getNextStructure(queryCondition);
    }

    /**
     * 获取日期最接近的上一个结构
     * @param queryCondition 查询条件
     */
    @ApiOperation("获取日期最接近的上一个结构")
    @PostMapping("/getPreviousStructure")
    public MpStructureAllocation getPreviousStructure(@RequestBody MpStructureAllocation queryCondition) {
        return mpStructureAllocationService.getPreviousStructure(queryCondition);
    }


}
