package com.zlt.aps.monthplan.demand.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.maindata.mapper.MpHistorySaleRecordEntityMapper;
import com.zlt.aps.maindata.mapper.MpMonthlySaleQtyEntityMapper;
import com.zlt.aps.maindata.service.IMpMonthlySaleQtyService;
import com.zlt.aps.monthplan.api.domain.entity.MpHistorySaleRecord;
import com.zlt.aps.monthplan.api.domain.entity.MpMonthlySaleQty;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpMonthlySaleQtyController.java
 * 描    述：月均销量 控制层类：....
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-11
 */
@Slf4j
@Api(tags = "月均销量")
@RestController
@RequestMapping("/mpMonthlySaleQty")
public class MpMonthlySaleQtyController extends AbstractDocBizController<MpMonthlySaleQty> {

    @Autowired
    private IMpMonthlySaleQtyService mpMonthlySaleQtyService;

    @Autowired
    private MpMonthlySaleQtyEntityMapper entityMapper;

    @Autowired
    private MpHistorySaleRecordEntityMapper mpHistorySaleRecordEntityMapper;

    /**
     * 查询月均销量列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody MpMonthlySaleQty queryVO) {
        TableDataInfo tableDataInfo = super.list(queryVO);
        List<MpMonthlySaleQty> list = (List<MpMonthlySaleQty>) tableDataInfo.getRows();
        List<String> codeList = list.stream().map(MpMonthlySaleQty::getMaterialCode).collect(Collectors.toList());
        // 查询历史销售记录
        int passTwelveMonth = 12;
        Calendar instance = Calendar.getInstance();
        instance.setTime(new Date());
        int year = instance.get(Calendar.YEAR);
        String month = String.format("%02d", instance.get(Calendar.MONTH) + 1);
        String maxYearMonth = year + month;

        instance.add(Calendar.MONTH, -passTwelveMonth);
        int last12Year = instance.get(Calendar.YEAR);
        String last12Month = String.format("%02d", instance.get(Calendar.MONTH) + 1);
        String last12YearMonth = last12Year + last12Month;

        String factoryCode = queryVO.getFactoryCode();
        List<MpHistorySaleRecord> sumQtyGroupByAreaList = mpHistorySaleRecordEntityMapper.selectSumQtyGroupByArea(factoryCode, last12YearMonth, maxYearMonth, codeList);

        Map<String, List<MpHistorySaleRecord>> sumQtyGroupByAreaMap = new HashMap<>(16);
        if (CollectionUtils.isNotEmpty(sumQtyGroupByAreaList)) {
            sumQtyGroupByAreaMap = sumQtyGroupByAreaList.stream().collect(Collectors.groupingBy(MpHistorySaleRecord::getMaterialCode));
        }
        List<MpHistorySaleRecord> sumQtyGroupByMonthList = mpHistorySaleRecordEntityMapper.selectSumQtyGroupByMonth(factoryCode, last12YearMonth, maxYearMonth, codeList);
        Map<String, List<MpHistorySaleRecord>> sumQtyGroupByMonthMap = new HashMap<>(16);
        if (CollectionUtils.isNotEmpty(sumQtyGroupByMonthList)) {
            sumQtyGroupByMonthMap = sumQtyGroupByMonthList.stream().collect(Collectors.groupingBy(MpHistorySaleRecord::getMaterialCode));
        }

        for (MpMonthlySaleQty monthlySaleQty : list) {
            String materialCode = monthlySaleQty.getMaterialCode();
            if (sumQtyGroupByAreaMap.containsKey(materialCode)) {
                List<MpHistorySaleRecord> areaGroupList = sumQtyGroupByAreaMap.get(materialCode);
                monthlySaleQty.setAreaGroupList(areaGroupList);
            }
            if (sumQtyGroupByMonthMap.containsKey(materialCode)) {
                List<MpHistorySaleRecord> monthGroupList = sumQtyGroupByMonthMap.get(materialCode);
                monthlySaleQty.setMonthGroupList(monthGroupList);
            }
        }
        return tableDataInfo;
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.mpMonthlySaleQty.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody MpMonthlySaleQty billVO) {
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.mpMonthlySaleQty.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }


    /**
     * 获取月均销量详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public MpMonthlySaleQty getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入月均销量数据
     *
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.mpMonthlySaleQty.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /**
     * 导出列表
     */
    @Log(title = "月均销量", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody MpMonthlySaleQty queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<MpMonthlySaleQty> listExportData(MpMonthlySaleQty obj) {
        QueryWrapper<MpMonthlySaleQty> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return entityMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService() {
        return mpMonthlySaleQtyService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<MpMonthlySaleQty> queryWrapper, MpMonthlySaleQty queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productTypeCode")), "PRODUCT_TYPE_CODE", queryVO.getFieldValueByFieldName("productTypeCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("locationType")), "LOCATION_TYPE", queryVO.getFieldValueByFieldName("locationType"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("brand")), "BRAND", queryVO.getFieldValueByFieldName("brand"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialCode")), "MATERIAL_CODE", queryVO.getFieldValueByFieldName("materialCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialDesc")), "MATERIAL_DESC", queryVO.getFieldValueByFieldName("materialDesc"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("rollMonthSaleQty")), "ROLL_MONTH_SALE_QTY", queryVO.getFieldValueByFieldName("rollMonthSaleQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("averageSaleQty")), "AVERAGE_SALE_QTY", queryVO.getFieldValueByFieldName("averageSaleQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("passThreeMonthSaleQty")), "PASS_THREE_MONTH_SALE_QTY", queryVO.getFieldValueByFieldName("passThreeMonthSaleQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("saleArea")), "SALE_AREA", queryVO.getFieldValueByFieldName("saleArea"));
    }

    @Override
    protected String getTypeCode() {
        return "MP1209";
    }

    /**
     * 生成月均销量
     *
     * @param mpMonthlySaleQty 参数
     * @return 结果
     */
    @ApiOperation("生成月均销量")
    @PostMapping("/genMonthlySaleQty")
    public AjaxResult genMonthlySaleQty(@RequestBody MpMonthlySaleQty mpMonthlySaleQty) {
        return mpMonthlySaleQtyService.genMonthlySaleQty(mpMonthlySaleQty);
    }

}
