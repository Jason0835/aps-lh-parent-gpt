package com.zlt.aps.lh.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.maindata.service.ILhMonthPlanSurplusService;
import com.zlt.aps.mp.api.domain.entity.LhMonthPlanSurplus;
import com.zlt.aps.mp.api.domain.entity.LhMonthPlanSurplusDetail;
import com.zlt.aps.mp.api.domain.vo.LhMonthPlanSurplusDetailVo;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
* Copyright (c) 2022, All rights reserved。
* 文件名称：LhMonthPlanSurplusController.java
* 描    述：月度计划外胎汇总 控制层类：....
*@author zlt
*@date 2025-02-21
*@version 1.0
*
 *  修改记录：
*     修改时间：...
*     修 改 人：zlt
*     修改内容：...
*/
@Slf4j
@Api(tags = "月度计划外胎汇总")
@RestController
@RequestMapping("/lhMonthPlanSurplus")
public class LhMonthPlanSurplusController extends AbstractDocBizController<LhMonthPlanSurplus> {

    @Autowired
    private ILhMonthPlanSurplusService lhMonthPlanSurplusService;

    /**
     * 查询月度计划外胎汇总列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody LhMonthPlanSurplus queryVO) {
        return super.list(queryVO);
    }

    /**
     * 查询月度计划外胎汇总列表明细
     */
    @ApiOperation("查询明细列表")
    @PostMapping("/detailList")
    public TableDataInfo detailList(@RequestBody LhMonthPlanSurplusDetail queryVO) {
        try {
            startPage();
            List<LhMonthPlanSurplusDetailVo> list = lhMonthPlanSurplusService.selectDetailList(queryVO);
            return getDataTable(list);
        } finally {
            clearPage();
        }
    }


    /**
     * 保存
     */
    @Log(title = "ui.data.column.lhMonthPlanSurplus.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody LhMonthPlanSurplus billVO){
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.lhMonthPlanSurplus.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids){
        return super.removeByIds(ids);
    }


    /**
     * 获取月度计划外胎汇总详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public LhMonthPlanSurplus getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入月度计划外胎汇总数据
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.lhMonthPlanSurplus.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData/{updateSupport}")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @PathVariable("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext,updateSupport);
    }

    /**
     * 导出列表
     */
    @Log(title = "月度计划外胎汇总", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody LhMonthPlanSurplus queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    /**
     * 更新指定年、月的月度完成量
     */
    @PostMapping("/updateMonthPlanSurplus/{year}/{month}")
    public AjaxResult updateMonthPlanSurplus(@PathVariable("year") int year, @PathVariable("month") int month) {
        return lhMonthPlanSurplusService.updateMonthPlanSurplus(year, month);
    }

    @Override
    protected IDocService getDocService(){
        return lhMonthPlanSurplusService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<LhMonthPlanSurplus> queryWrapper, LhMonthPlanSurplus queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("monthPlanApsVersion")), "MONTH_PLAN_APS_VERSION", queryVO.getFieldValueByFieldName("monthPlanApsVersion"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("monthPlanVersion")), "MONTH_PLAN_VERSION", queryVO.getFieldValueByFieldName("monthPlanVersion"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("year")), "YEAR", queryVO.getFieldValueByFieldName("year"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("month")), "MONTH", queryVO.getFieldValueByFieldName("month"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("specCode")), "SPEC_CODE", queryVO.getFieldValueByFieldName("specCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("monthPlanQty")), "MONTH_PLAN_QTY", queryVO.getFieldValueByFieldName("monthPlanQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lastMonthStock")), "LAST_MONTH_STOCK", queryVO.getFieldValueByFieldName("lastMonthStock"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("specBadQty")), "SPEC_BAD_QTY", queryVO.getFieldValueByFieldName("specBadQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("monthFinishQty")), "MONTH_FINISH_QTY", queryVO.getFieldValueByFieldName("monthFinishQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("monthRemainQty")), "MONTH_REMAIN_QTY", queryVO.getFieldValueByFieldName("monthRemainQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("dataSource")), "DATA_SOURCE", queryVO.getFieldValueByFieldName("dataSource"));
    }


    @Override
    protected String getTypeCode(){
        return "0108";
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }
}
