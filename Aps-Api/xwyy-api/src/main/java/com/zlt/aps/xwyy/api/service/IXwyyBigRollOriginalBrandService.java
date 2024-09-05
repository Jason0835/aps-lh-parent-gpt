package com.zlt.aps.xwyy.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.xwyy.api.domain.entity.XwyyBigRollOriginalBrand;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 帘布大卷原线品牌Service接口
 *
 * @author chen
 * @date 2022-05-11
 */
@FeignClient(contextId = "IXwyyBigRollOriginalBrandService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.xwyy:xwyy}")
public interface IXwyyBigRollOriginalBrandService {

    /**
     * 查询帘布大卷原线品牌列表
     */
    @ApiOperation("查询帘布大卷原线品牌列表")
    @PostMapping("/bigRollOriginalBrand/list")
    TableDataInfo list(@RequestBody XwyyBigRollOriginalBrand xwyyBigRollOriginalBrand);

    /**
     * 新增帘布大卷原线品牌
     */
    @ApiOperation("新增帘布大卷原线品牌")
    @PostMapping("/bigRollOriginalBrand/add")
    AjaxResult add(@RequestBody XwyyBigRollOriginalBrand xwyyBigRollOriginalBrand);

    /**
     * 修改帘布大卷原线品牌
     */
    @ApiOperation("修改帘布大卷原线品牌")
    @PostMapping("/bigRollOriginalBrand/edit")
    AjaxResult edit(@RequestBody XwyyBigRollOriginalBrand xwyyBigRollOriginalBrand);

    /**
     * 删除帘布大卷原线品牌
     */
    @ApiOperation("删除帘布大卷原线品牌")
    @DeleteMapping("/bigRollOriginalBrand/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/bigRollOriginalBrand/{id}")
    XwyyBigRollOriginalBrand getInfo(@PathVariable("id") Long id);

    /**
     * 校验帘布大卷原线品牌唯一性
     */
    @ApiOperation("校验帘布大卷原线品牌唯一性")
    @PostMapping("/bigRollOriginalBrand/checkXwyyBigRollOriginalBrandUnique")
    String checkXwyyBigRollOriginalBrandUnique(@RequestBody XwyyBigRollOriginalBrand xwyyBigRollOriginalBrand);

    /**
     * 导出帘布大卷原线品牌列表
     */
    @ApiOperation("导出帘布大卷原线品牌列表")
    @PostMapping("/bigRollOriginalBrand/getList")
    List<XwyyBigRollOriginalBrand> getList(@RequestBody XwyyBigRollOriginalBrand xwyyBigRollOriginalBrand);

    /**
     * 导入帘布大卷原线品牌数据
     */
    @ApiOperation("导入帘布大卷原线品牌")
    @PostMapping("/bigRollOriginalBrand/importData")
    public AjaxResult importData(@RequestBody List<XwyyBigRollOriginalBrand> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
