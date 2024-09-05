package com.zlt.aps.tc.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import io.swagger.annotations.ApiOperation;

import com.zlt.aps.tc.api.domain.entity.TcSidewallCodeColor;


/**
 * 胎侧代码前缀颜色设定Service接口
 * @author zlt
 * @date 2022-01-14
 */
@FeignClient(contextId = "ITcSidewallCodeColorService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tc:tc}")
public interface ITcSidewallCodeColorService {

    /**
     * 查询胎侧代码前缀颜色设定列表
     */
    @ApiOperation("查询胎侧代码前缀颜色设定列表")
    @PostMapping("/sidewallCodeColor/list")
    TableDataInfo list(@RequestBody TcSidewallCodeColor tcSidewallCodeColor);

    /**
    * 新增胎侧代码前缀颜色设定
    */
    @ApiOperation("新增胎侧代码前缀颜色设定")
    @PostMapping("/sidewallCodeColor/add")
    AjaxResult add(@RequestBody TcSidewallCodeColor tcSidewallCodeColor);

    /**
     * 修改胎侧代码前缀颜色设定
     */
    @ApiOperation("修改胎侧代码前缀颜色设定")
    @PostMapping("/sidewallCodeColor/edit")
    AjaxResult edit(@RequestBody TcSidewallCodeColor tcSidewallCodeColor);

    /**
     * 删除胎侧代码前缀颜色设定
     */
    @ApiOperation("删除胎侧代码前缀颜色设定")
    @DeleteMapping("/sidewallCodeColor/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/sidewallCodeColor/{id}")
    TcSidewallCodeColor getInfo(@PathVariable("id") Long id);

    /**
     * 校验胎侧代码前缀颜色设定唯一性
     */
    @ApiOperation("校验胎侧代码前缀颜色设定唯一性")
    @PostMapping("/sidewallCodeColor/checkTcSidewallCodeColorUnique")
    String checkTcSidewallCodeColorUnique(@RequestBody TcSidewallCodeColor tcSidewallCodeColor);

    /**
     * 导出胎侧代码前缀颜色设定列表
     */
    @ApiOperation("导出胎侧代码前缀颜色设定列表")
    @PostMapping("/sidewallCodeColor/getList")
    List<TcSidewallCodeColor> getList(@RequestBody TcSidewallCodeColor tcSidewallCodeColor);

    /**
     * 导入胎侧代码前缀颜色设定数据
     */
    @ApiOperation("导入胎侧代码前缀颜色设定")
    @PostMapping("/sidewallCodeColor/importData")
    public AjaxResult importData(@RequestBody List<TcSidewallCodeColor> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
