package com.zlt.aps.mp.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.mp.api.domain.dto.ProductMouldConfigurationParam;
import com.zlt.aps.mp.api.domain.dto.ProductMouldRelationConfigurationParam;
import com.zlt.aps.mp.api.domain.entity.MdmSkuMouldRel;
import com.zlt.aps.mp.api.domain.vo.ProductMouldInfoVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMdmProductModelRelationRemoteService.java
 * 描    述：IMdmProductModelRelationRemoteServiceSKU与模具关系前端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-02-24
 */
@FeignClient(contextId = "IMdmProductModelRelationRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMdmProductModelRelationRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/relation/list")
    TableDataInfo list(@RequestBody MdmSkuMouldRel QueryVO);

    /**
     * 保存
     */
    @ApiOperation("保存")
    @PostMapping("/relation/save")
    AjaxResult save(@RequestBody MdmSkuMouldRel MdmSkuMouldRel);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/relation/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/relation/{id}")
    MdmSkuMouldRel getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/relation/checkUnique")
    String checkUnique(@RequestBody MdmSkuMouldRel mdmProductModelRelationVO);

    /**
     * 导出SKU与模具关系列表
     */
    @ApiOperation("导出列表")
    @PostMapping("/relation/exportData/{fileName}")
    byte[] exportData(@RequestBody MdmSkuMouldRel queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入SKU与模具关系数据
     */
    @ApiOperation("导入SKU与模具关系")
    @PostMapping("/relation/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    /**
     * 根据物料获取分厂年月的匹配模具
     *
     * @param queryParam
     * @return
     */
    @ApiOperation("物料匹配的模具")
    @PostMapping("/relation/matchMouldConfiguration")
    ProductMouldInfoVo getProductMouldConfiguration(@RequestBody ProductMouldConfigurationParam queryParam);

    /**
     * 配置物料与模具关系
     *
     * @param configuration
     * @return
     */
    @ApiOperation("配置物料的模具信息")
    @PostMapping("/relation/configurationMouldRelation")
    AjaxResult configurationMouldRelation(@RequestBody ProductMouldRelationConfigurationParam configuration);

    /**
     * 抓取MES数据
     *
     * @return 结果
     */
    @ApiOperation("抓取MES数据")
    @PostMapping("/relation/mesCapture")
    AjaxResult mesCapture();

    /**
     * 更新主花纹到物料表
     *
     * @param queryVO 参数
     * @return 结果
     */
    @ApiOperation("更新主花纹到物料表")
    @PostMapping("/relation/updateMainPatternToMaterial")
    public AjaxResult updateMainPatternToMaterial(@RequestBody MdmSkuMouldRel queryVO);
}
