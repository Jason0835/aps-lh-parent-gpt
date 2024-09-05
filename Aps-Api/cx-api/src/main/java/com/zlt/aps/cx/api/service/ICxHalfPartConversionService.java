package com.zlt.aps.cx.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cx.api.domain.entity.CxHalfPartConversion;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 半部件规格换算Service接口
 * @author zlt
 * @date 2022-01-20
 */
@FeignClient(contextId = "ICxHalfPartConversionService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cx:cx}")
public interface ICxHalfPartConversionService {

    /**
     * 查询半部件规格换算列表
     */
    @ApiOperation("查询半部件规格换算列表")
    @PostMapping("/conversion/list")
    TableDataInfo list(@RequestBody CxHalfPartConversion cxHalfPartConversion);

    /**
    * 新增半部件规格换算
    */
    @ApiOperation("新增半部件规格换算")
    @PostMapping("/conversion/add")
    AjaxResult add(@RequestBody CxHalfPartConversion cxHalfPartConversion);

    /**
     * 修改半部件规格换算
     */
    @ApiOperation("修改半部件规格换算")
    @PostMapping("/conversion/edit")
    AjaxResult edit(@RequestBody CxHalfPartConversion cxHalfPartConversion);

    /**
     * 删除半部件规格换算
     */
    @ApiOperation("删除半部件规格换算")
    @DeleteMapping("/conversion/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/conversion/{id}")
    CxHalfPartConversion getInfo(@PathVariable("id") Long id);

    /**
     * 校验半部件规格换算唯一性
     */
    @ApiOperation("校验半部件规格换算唯一性")
    @PostMapping("/conversion/checkCxHalfPartConversionUnique")
    String checkCxHalfPartConversionUnique(@RequestBody CxHalfPartConversion cxHalfPartConversion);

    /**
     * 导出半部件规格换算列表
     */
    @ApiOperation("导出半部件规格换算列表")
    @PostMapping("/conversion/getList")
    List<CxHalfPartConversion> getList(@RequestBody CxHalfPartConversion cxHalfPartConversion);

    /**
     * 导入半部件规格换算数据
     */
    @ApiOperation("导入半部件规格换算")
    @PostMapping("/conversion/importData")
    public AjaxResult importData(@RequestBody List<CxHalfPartConversion> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

    /**
     * 根据排程日期、半部件类型、半部件编码，查询排程表是否有对应排程，有则返回排程id
     * @param queryParams 查询参数
     * @return 查询到的排程id
     */
    @PostMapping("/conversion/getScheduleResultByParams")
    public Long getScheduleResultByParams(@RequestBody CxHalfPartConversion queryParams);

    /**
     * 根据半部件类型代号查询对应的机台信息
     * @param queryParams 半部件类型代号
     * @return 机台id和机台名称
     */
    @PostMapping("/conversion/getMachineInfoListByHalfPartType")
    public List<CxHalfPartConversion> getMachineInfoListByHalfPartType(@RequestBody CxHalfPartConversion queryParams);
}
