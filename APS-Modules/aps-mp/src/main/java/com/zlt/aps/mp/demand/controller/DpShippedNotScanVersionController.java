package com.zlt.aps.mp.demand.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.mp.api.domain.entity.DpShippedNotScanVersion;
import com.zlt.aps.mp.demand.mapper.DpShippedNotScanVersionEntityMapper;
import com.zlt.aps.mp.demand.service.IDpShippedNotScanVersionService;
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

@Slf4j
@Api(tags = "已出库未扫描版本")
@RestController
@RequestMapping("/dpShippedNotScanVersion")
public class DpShippedNotScanVersionController extends AbstractDocBizController<DpShippedNotScanVersion> {

    @Autowired
    private IDpShippedNotScanVersionService dpShippedNotScanVersionService;

    @Autowired
    private DpShippedNotScanVersionEntityMapper entityMapper;

    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody DpShippedNotScanVersion queryVO) {
        return super.list(queryVO);
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }

    @Log(title = "ui.data.column.dpShippedNotScanVersion.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody DpShippedNotScanVersion billVO) {
        return super.save(billVO);
    }

    @Log(title = "ui.data.column.dpShippedNotScanVersion.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public DpShippedNotScanVersion getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }

    @Log(title = "ui.data.column.dpShippedNotScanVersion.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    @Log(title = "已出库未扫描版本", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody DpShippedNotScanVersion queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<DpShippedNotScanVersion> listExportData(DpShippedNotScanVersion obj) {
        QueryWrapper<DpShippedNotScanVersion> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return entityMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService() {
        return dpShippedNotScanVersionService;
    }

    @Override
    protected void builderCondition(QueryWrapper<DpShippedNotScanVersion> queryWrapper, DpShippedNotScanVersion queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("year")), "YEAR", queryVO.getFieldValueByFieldName("year"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("month")), "MONTH", queryVO.getFieldValueByFieldName("month"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("requireVersion")), "REQUIRE_VERSION", queryVO.getFieldValueByFieldName("requireVersion"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("saleBillNo")), "SALE_BILL_NO", queryVO.getFieldValueByFieldName("saleBillNo"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("saleOrderNo")), "SALE_ORDER_NO", queryVO.getFieldValueByFieldName("saleOrderNo"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("saleOrgName")), "SALE_ORG_NAME", queryVO.getFieldValueByFieldName("saleOrgName"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("sellTo")), "SELL_TO", queryVO.getFieldValueByFieldName("sellTo"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("billId")), "BILL_ID", queryVO.getFieldValueByFieldName("billId"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("sapCode")), "SAP_CODE", queryVO.getFieldValueByFieldName("sapCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialName")), "MATERIAL_NAME", queryVO.getFieldValueByFieldName("materialName"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("dot")), "DOT", queryVO.getFieldValueByFieldName("dot"));
    }

    @Override
    protected String getTypeCode() {
        return "2026033101";
    }

    @ApiOperation("查询需求计划版本号")
    @PostMapping("/findMonthPlanVersion")
    public AjaxResult findMonthPlanVersion(@RequestBody DpShippedNotScanVersion queryCondition) {
        return AjaxResult.success(dpShippedNotScanVersionService.findMonthPlanVersion(queryCondition));
    }

    @Log(title = "ui.data.column.dpShippedNotScanVersion.modelName", businessType = BusinessType.OTHER)
    @ApiOperation("生成已出库未扫描版本")
    @PostMapping("/generate")
    public AjaxResult generate(@RequestBody DpShippedNotScanVersion queryCondition) {
        dpShippedNotScanVersionService.generateShippedNotScanVersion(queryCondition);
        return AjaxResult.success();
    }
}
