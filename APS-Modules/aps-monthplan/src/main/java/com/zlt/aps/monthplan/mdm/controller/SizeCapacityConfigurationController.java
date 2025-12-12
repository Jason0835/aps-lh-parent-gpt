package com.zlt.aps.monthplan.mdm.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.PageUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.zlt.aps.maindata.service.ISizeCapacityConfigurationService;
import com.zlt.aps.monthplan.api.domain.entity.SizeCapacityConfiguration;
import com.zlt.aps.monthplan.api.domain.vo.*;
import com.zlt.aps.monthplan.factory.service.IFactoryMonthPlanAdjustPlanBusinessService;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：SizeCapacityConfigurationController.java
 * 描    述：寸口产能配置 控制层类：....
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
@RequestMapping("/sizeCapacity")
@Api(tags = "寸口产能配置业务后端服务接口-->ZLT")
public class SizeCapacityConfigurationController extends AbstractDocBizController<SizeCapacityConfiguration> {

    private final ISizeCapacityConfigurationService sizeCapacityConfigurationService;

    private final IFactoryMonthPlanAdjustPlanBusinessService factoryMonthPlanAdjustPlanBusinessService;

    /**
     * 查询寸口产能配置列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody SizeCapacityConfiguration queryVO) {
        try {
            startPage();
            List<SizeCapacityConfiguration> queryData = sizeCapacityConfigurationService.getConfigurationList(queryVO);
            //处理如果下一寸口与寸口相同，将下一寸口置为空
            if (!CollectionUtils.isEmpty(queryData)) {
                queryData.stream().forEach(rowData -> {
                    if (null == rowData.getNextProSize()) {
                        return;
                    }
                    if (!rowData.getProSize().equals(rowData.getNextProSize())) {
                        return;
                    }
                    rowData.setNextProSize(null);
                });
            }
            return getDataTable(queryData);
        } finally {
            PageUtils.clearPage();
        }
    }

    /**
     * 根据条件获取寸口配置的需求信息
     */
    @PostMapping("/getDemandInfo")
    @ApiOperation("根据条件获取寸口配置的需求信息")
    public SizeCapacityConfigurationVo getSizeCapacityInfo(@RequestBody SizeCapacityConfiguration condition) {
        if (null == condition) {
            return new SizeCapacityConfigurationVo();
        }
        String factoryCode = condition.getFactoryCode();
        String monthPlanVersion = condition.getMonthPlanVersion();
        Integer year = condition.getYear();
        Integer month = condition.getMonth();
        BigDecimal proSize = condition.getProSize();
        if (StringUtils.isBlank(factoryCode) || StringUtils.isBlank(monthPlanVersion) || null == year || null == month || null == proSize) {
            return new SizeCapacityConfigurationVo();
        }
        return sizeCapacityConfigurationService.getDemandInfo(condition);
    }

    /**
     * 根据分厂、年、月、需求版本，生成寸口产能配置
     *
     * @param factoryProductionParam
     * @return
     */
    @PostMapping("/buildSizeCapacityConfiguration")
    @ApiOperation("根据分厂、年、月、需求版本，生成寸口产能配置")
    public AjaxResult autoBuildConfiguration(@RequestBody BuildSizeCapacityParamVo factoryProductionParam) {
        if (null == factoryProductionParam) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.condition.noEmpty"));
        }
        String factoryCode = factoryProductionParam.getFactoryCode();
        Integer year = factoryProductionParam.getYear();
        Integer month = factoryProductionParam.getMonth();
        String monthPlanVersion = factoryProductionParam.getMonthPlanVersion();
        Date formingDate = factoryProductionParam.getFormingDate();
        if (StringUtils.isBlank(factoryCode) || null == year || null == month || StringUtils.isBlank(monthPlanVersion) || null == formingDate) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.sizeCapacityConfiguration.build.errorParam"));
        }
        return AjaxResult.success();
    }

    /**
     * 根据分厂、年、月、查看产能配置详情
     *
     * @param factoryProductionParam
     * @return
     */
    @PostMapping("/getDaySizeCapacityInfo")
    @ApiOperation("根据分厂、年、月、查看产能配置详情")
    public List<DaySizeCapacityConfigurationDetailVo> getDaySizeCapacityInfo(@RequestBody FactoryProductionParamVo factoryProductionParam) {
        String factoryCode = factoryProductionParam.getFactoryCode();
        Integer year = factoryProductionParam.getYear();
        Integer month = factoryProductionParam.getMonth();
        return factoryMonthPlanAdjustPlanBusinessService.getDaySizeCapacityInfo(factoryCode, year, month);
    }

    /**
     * 根据分厂、年、月、查看产能配置详情
     *
     * @param factoryProductionParam
     * @return
     */
    @PostMapping("/getSizeDayCapacityInfo")
    @ApiOperation("根据分厂、年、月、查看寸口产能配置详情")
    public List<DaySizeCapacityConfigurationMouldMethodDetailVo> getSizeDayCapacityInfo(@RequestBody FactoryProductionParamVo factoryProductionParam) {
        String factoryCode = factoryProductionParam.getFactoryCode();
        Integer year = factoryProductionParam.getYear();
        Integer month = factoryProductionParam.getMonth();
        return factoryMonthPlanAdjustPlanBusinessService.getDaySizeCapacityInfoByMouldMethod(factoryCode, year, month);
    }

    /**
     * 根据ID，获取配置信息，包含对应的参考需求信息-总需求量、净需求、备货需求
     */
    @PostMapping("/getSizeCapacityConfiguration")
    @ApiOperation("根据ID，获取配置信息，包含对应的参考需求信息-总需求量、净需求、备货需求")
    public SizeCapacityConfigurationVo getSizeCapacityConfiguration(@RequestBody Long id) {
        if (null == id) {
            return new SizeCapacityConfigurationVo();
        }
        return sizeCapacityConfigurationService.getConfigurationById(id);
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.sizeCapacity.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody SizeCapacityConfiguration billVO) {
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.sizeCapacity.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }


    /**
     * 获取寸口产能配置详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public SizeCapacityConfiguration getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入寸口产能配置数据
     *
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @RequiresPermissions("monthplan:sizeCapacity:import")
    @Log(title = "ui.data.column.sizeCapacity.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData/{updateSupport}")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @PathVariable("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /**
     * 导出列表
     */
    @RequiresPermissions("monthplan:sizeCapacity:export")
    @Log(title = "寸口产能配置", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody SizeCapacityConfiguration queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected IDocService getDocService() {
        return sizeCapacityConfigurationService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<SizeCapacityConfiguration> queryWrapper, SizeCapacityConfiguration queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("year")), "YEAR", queryVO.getFieldValueByFieldName("year"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("month")), "MONTH", queryVO.getFieldValueByFieldName("month"));
//        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("monthPlanVersion")), "MONTH_PLAN_VERSION", queryVO.getFieldValueByFieldName("monthPlanVersion"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("proSize")), "PRO_SIZE", queryVO.getFieldValueByFieldName("proSize"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mouldMethod")), "MOULD_METHOD", queryVO.getFieldValueByFieldName("mouldMethod"));
    }

    @Override
    protected String getTypeCode() {
        return "0203";
    }

}
