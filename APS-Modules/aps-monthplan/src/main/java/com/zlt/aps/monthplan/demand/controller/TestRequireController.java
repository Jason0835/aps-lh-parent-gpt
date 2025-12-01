package com.zlt.aps.monthplan.demand.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.monthplan.api.domain.entity.SaleMonthPlanRequire;
import com.zlt.aps.monthplan.demand.mapper.SaleMonthPlanRequireMapper;
import com.zlt.core.dao.basedao.BaseDao;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：SaleMonthPlanRequireController.java
 * 描    述：月度生产需求计划 控制层类：....
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-02-14
 */
@Slf4j
@Api(tags = "生产需求计划处理")
@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestRequireController extends BaseController<SaleMonthPlanRequire> {

    private final SaleMonthPlanRequireMapper saleMonthPlanRequireMapper;

    private final BaseDao baseDao;

    /**
     * 对需求计划进行处理
     */
    @ApiOperation("对需求计划进行处理")
    @PostMapping("/handler")
    public AjaxResult handlerRequire(@RequestBody SaleMonthPlanRequire queryCondition) {
        QueryWrapper<SaleMonthPlanRequire> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("MONTH_PLAN_VERSION", queryCondition.getMonthPlanVersion());
        List<SaleMonthPlanRequire> versionDataList = saleMonthPlanRequireMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(versionDataList)) {
            return AjaxResult.success();
        }
        List<TempProductionDto> productionList = saleMonthPlanRequireMapper.getProductionQty();
        if (CollectionUtils.isEmpty(productionList)) {
            return AjaxResult.success();
        }
        List<SaleMonthPlanRequire> updateList = new ArrayList<>();
        Map<String, List<SaleMonthPlanRequire>> groupDataMap = versionDataList.stream().collect(Collectors.groupingBy(SaleMonthPlanRequire::getProductCode));
        Map<String, TempProductionDto> productionMap = productionList.stream().collect(Collectors.toMap(TempProductionDto::getProductCode, Function.identity()));
        groupDataMap.entrySet().forEach(entry -> {
            String productCode = entry.getKey();
            List<SaleMonthPlanRequire> requireList = entry.getValue();
            if (CollectionUtils.isEmpty(requireList)) {
                return;
            }
            requireList.sort(Comparator.comparing(SaleMonthPlanRequire::getIsStockUp));
            TempProductionDto productionInfo = productionMap.get(productCode);
            if (null == productionInfo) {
                return;
            }
            Long productionQty = productionInfo.getProductionQty();
            for (SaleMonthPlanRequire require : requireList) {
                Long planQty = require.getPlanQty();
                if (planQty >= productionQty) {
                    require.setPlanQty(planQty - productionQty);
                } else {
                    require.setPlanQty(BigDecimal.ZERO.longValue());
                    productionQty = productionQty - planQty;
                }
                updateList.add(require);
            }
        });
        if (!CollectionUtils.isEmpty(updateList)) {
            baseDao.updateBatch(updateList);
        }
        return AjaxResult.success();
    }


}
