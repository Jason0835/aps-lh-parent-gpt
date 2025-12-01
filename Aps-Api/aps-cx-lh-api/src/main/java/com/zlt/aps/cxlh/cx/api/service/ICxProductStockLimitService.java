package com.zlt.aps.cxlh.cx.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cxlh.cx.api.domain.entity.CxProductStockLimit;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 成型投产班次库存限定设置Service接口
 * @author zlt
 * @date 2022-01-07
 */
@FeignClient(contextId = "ICxProductStockLimitService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cxlh:cxlh}")
public interface ICxProductStockLimitService {

    /**
     * 查询成型投产班次库存限定设置列表
     */
    @ApiOperation("查询成型投产班次库存限定设置列表")
    @PostMapping("/shiftLimit/list")
    TableDataInfo list(@RequestBody CxProductStockLimit cxProductStockLimit);

    /**
    * 新增成型投产班次库存限定设置
    */
    @ApiOperation("新增成型投产班次库存限定设置")
    @PostMapping("/shiftLimit/add")
    AjaxResult add(@RequestBody CxProductStockLimit cxProductStockLimit);

    /**
     * 修改成型投产班次库存限定设置
     */
    @ApiOperation("修改成型投产班次库存限定设置")
    @PostMapping("/shiftLimit/edit")
    AjaxResult edit(@RequestBody CxProductStockLimit cxProductStockLimit);

    /**
     * 删除成型投产班次库存限定设置
     */
    @ApiOperation("删除成型投产班次库存限定设置")
    @DeleteMapping("/shiftLimit/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/shiftLimit/{id}")
    CxProductStockLimit getInfo(@PathVariable("id") Long id);

    /**
     * 校验成型投产班次库存限定设置唯一性
     */
    @ApiOperation("校验成型投产班次库存限定设置唯一性")
    @PostMapping("/shiftLimit/checkCxProductStockLimitUnique")
    List<CxProductStockLimit> checkCxProductStockLimitUnique(@RequestBody CxProductStockLimit cxProductStockLimit);

    /**
     * 导出成型投产班次库存限定设置列表
     */
    @ApiOperation("导出成型投产班次库存限定设置列表")
    @PostMapping("/shiftLimit/getList")
    List<CxProductStockLimit> getList(@RequestBody CxProductStockLimit cxProductStockLimit);

    /**
     * 导入成型投产班次库存限定设置数据
     */
    @ApiOperation("导入成型投产班次库存限定设置")
    @PostMapping("/shiftLimit/importData")
    public AjaxResult importData(@RequestBody List<CxProductStockLimit> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
