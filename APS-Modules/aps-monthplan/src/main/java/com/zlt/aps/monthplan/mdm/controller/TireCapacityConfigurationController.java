package com.zlt.aps.monthplan.mdm.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.PageUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.zlt.aps.maindata.service.ITireCapacityConfigurationService;
import com.zlt.aps.monthplan.api.domain.entity.TireCapacityConfiguration;
import com.zlt.aps.monthplan.api.domain.vo.TireCapacityConfigurationVo;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：TireCapacityConfigurationController.java
 * 描    述：轮胎类型产能配置(特殊情况下配置) 控制层类：....
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-06-04
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/tireCapacity")
@Api(tags = "轮胎类型产能配置(特殊情况下配置)")
public class TireCapacityConfigurationController extends AbstractDocBizController<TireCapacityConfiguration> {

    private final ITireCapacityConfigurationService tireCapacityConfigurationService;

    /**
     * 查询轮胎类型产能配置(特殊情况下配置)列表
     */
    @Override
    @PostMapping("/list")
    @ApiOperation("查询轮胎类型产能配置(特殊情况下配置)列表")
    public TableDataInfo list(@RequestBody TireCapacityConfiguration queryVO) {
        try {
            startPage();
            List<TireCapacityConfigurationVo> queryData = tireCapacityConfigurationService.getConfigurationList(queryVO);
            return getDataTable(queryData);
        } finally {
            PageUtils.clearPage();
        }
    }

    /**
     * 根据条件获取轮胎类型产能配置的需求信息
     *
     * @param condition
     * @return
     */
    @PostMapping("/getDemandInfo")
    @ApiOperation("根据条件获取轮胎类型产能配置的需求信息")
    public TireCapacityConfigurationVo getTireCapacityInfo(@RequestBody TireCapacityConfiguration condition) {
        if (null == condition) {
            return new TireCapacityConfigurationVo();
        }
        String factoryCode = condition.getFactoryCode();
        String monthPlanVersion = condition.getMonthPlanVersion();
        Integer year = condition.getYear();
        Integer month = condition.getMonth();
        if (StringUtils.isBlank(factoryCode) || StringUtils.isBlank(monthPlanVersion) || null == year || null == month) {
            return new TireCapacityConfigurationVo();
        }
        BigDecimal proSize = condition.getProSize();
        String tireType = condition.getTireType();
        if (StringUtils.isBlank(tireType) || null == proSize) {
            return new TireCapacityConfigurationVo();
        }
        return tireCapacityConfigurationService.getDemandInfo(condition);
    }

    /**
     * 根据ID，获取配置信息，包含对应的参考需求信息-总需求量、净需求、备货需求
     *
     * @param id
     * @return
     */
    @PostMapping("/getTireCapacityConfiguration")
    @ApiOperation("根据ID，获取配置信息，包含对应的参考需求信息-总需求量、净需求、备货需求")
    public TireCapacityConfigurationVo getTireCapacityConfiguration(@RequestBody Long id) {
        if (null == id) {
            return new TireCapacityConfigurationVo();
        }
        return tireCapacityConfigurationService.getConfigurationById(id);
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.tireCapacity.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody TireCapacityConfiguration billVO) {
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.tireCapacity.modelName", businessType = BusinessType.DELETE)
    @RequiresPermissions("monthplan:capacity:remove")
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }


    /**
     * 获取轮胎类型产能配置(特殊情况下配置)详细信息
     */
    @RequiresPermissions("monthplan:capacity:query")
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public TireCapacityConfiguration getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入轮胎类型产能配置(特殊情况下配置)数据
     *
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @RequiresPermissions("monthplan:capacity:import")
    @Log(title = "ui.data.column.capacity.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData/{updateSupport}")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @PathVariable("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /**
     * 导出列表
     */
    @RequiresPermissions("monthplan:capacity:export")
    @Log(title = "轮胎类型产能配置(特殊情况下配置)", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody TireCapacityConfiguration queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected IDocService getDocService() {
        return tireCapacityConfigurationService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<TireCapacityConfiguration> queryWrapper, TireCapacityConfiguration queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("year")), "YEAR", queryVO.getFieldValueByFieldName("year"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("month")), "MONTH", queryVO.getFieldValueByFieldName("month"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("monthPlanVersion")), "MONTH_PLAN_VERSION", queryVO.getFieldValueByFieldName("monthPlanVersion"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tireType")), "TIRE_TYPE", queryVO.getFieldValueByFieldName("tireType"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("proSize")), "PRO_SIZE", queryVO.getFieldValueByFieldName("proSize"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("monthCapacity")), "MONTH_CAPACITY", queryVO.getFieldValueByFieldName("monthCapacity"));
    }

    @Override
    protected String getTypeCode() {
        return "0204";
    }

}
