package com.zlt.aps.cx.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cx.api.domain.entity.CxDayFinishQty;
import com.zlt.aps.cx.service.ICxDayFinishQtyService;
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
 * 成型排程日完成量Controller
 *
 * @author APS Team
 * @since 2026/05/12
 */
@Slf4j
@Api(tags = "成型排程日完成量管理")
@RestController
@RequestMapping("/cxDayFinishQty")
public class CxDayFinishQtyController extends AbstractDocBizController<CxDayFinishQty> {

    @Autowired
    private ICxDayFinishQtyService cxDayFinishQtyService;

    /**
     * 查询成型排程日完成量列表
     */
    @ApiOperation("查询成型排程日完成量列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody CxDayFinishQty queryVO) {
        return super.list(queryVO);
    }

    /**
     * 获取详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{id}")
    @Override
    public CxDayFinishQty getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    /**
     * 导出数据
     */
    @Log(title = "ui.data.column.cxDayFinishQty.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody CxDayFinishQty queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    /**
     * 条件拼接
     */
    @Override
    protected void builderCondition(QueryWrapper<CxDayFinishQty> queryWrapper, CxDayFinishQty queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.ge(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("finishDateStart")), "FINISH_DATE", queryVO.getFieldValueByFieldName("finishDateStart"));
        queryWrapper.le(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("finishDateEnd")), "FINISH_DATE", queryVO.getFieldValueByFieldName("finishDateEnd"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("embryoCode")), "EMBRYO_CODE", queryVO.getFieldValueByFieldName("embryoCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("exampleType")), "EXAMPLE_TYPE", queryVO.getFieldValueByFieldName("exampleType"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("bomDataVersion")), "BOM_DATA_VERSION", queryVO.getFieldValueByFieldName("bomDataVersion"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("dataVersion")), "DATA_VERSION", queryVO.getFieldValueByFieldName("dataVersion"));
    }

    @Override
    protected IDocService getDocService() {
        return cxDayFinishQtyService;
    }

    @Override
    protected String getTypeCode() {
        return "0";
    }

    @Override
    protected String getOrderBy() {
        return "FINISH_DATE desc, ID desc";
    }
}
