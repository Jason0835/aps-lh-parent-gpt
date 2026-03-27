package com.zlt.aps.cx.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cxlh.cx.api.domain.entity.CxHalfPartConversion;
import com.zlt.aps.cxlh.cx.api.domain.entity.CxProductConstructionInfo;
import com.zlt.aps.cx.service.CxProductConstructionInfoService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 半部件规格换算Controller
 *
 * @author zlt
 * @date 2022-01-20
 */
@RestController
@RequestMapping("/conversion")
public class CxHalfPartConversionController extends BaseController {
	@Autowired
	private CxProductConstructionInfoService cxProductConstructionInfoService;

    /**
     * 查询半部件规格换算列表
     */
    @ApiOperation("查询半部件规格换算列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody CxHalfPartConversion cxHalfPartConversion) {
        // 调用接口
        List<CxHalfPartConversion> list = cxProductConstructionInfoService.conversionHalfPartPlan(cxHalfPartConversion);
        return getDataTable(list);
    }

    /**
     * 根据排程日期、半部件类型、半部件编码，查询排程表是否有对应排程，有则返回排程id
     * @param queryParams 查询参数
     * @return 查询到的排程id
     */
    @ApiOperation("根据排程日期、半部件类型、半部件编码，查询排程表是否有对应排程，有则返回排程id")
    @PostMapping("/getScheduleResultByParams")
    public Long getScheduleResultByParams(@RequestBody CxHalfPartConversion queryParams) {
        return cxProductConstructionInfoService.getScheduleResultByParams(queryParams);
    }

    /**
     * 根据半部件类型代号查询对应的机台信息
     * @param queryParams 半部件类型代号
     * @return 机台id和机台名称
     */
    @ApiOperation("根据半部件类型代号查询对应的机台信息")
    @PostMapping("/getMachineInfoListByHalfPartType")
    public List<CxHalfPartConversion> getMachineInfoListByHalfPartType(@RequestBody CxHalfPartConversion queryParams) {
        return cxProductConstructionInfoService.getMachineInfoListByHalfPartType(queryParams);
    }


    /**
     * 导出投产施工信息列表
     */
    @Log(title = "ui.data.column.productConstruction.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出投产施工信息列表")
    @PostMapping("/productConstruction/getList")
    public List<CxProductConstructionInfo> getList(@RequestBody CxProductConstructionInfo cxProductConstructionInfo) {
        startPage();
        cxProductConstructionInfo.setOrderStr(orderStr());
        return cxProductConstructionInfoService.selectCxProductConstructionInfoList(cxProductConstructionInfo);
    }
}
