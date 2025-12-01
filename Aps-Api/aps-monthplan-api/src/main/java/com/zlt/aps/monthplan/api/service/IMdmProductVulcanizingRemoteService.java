package com.zlt.aps.monthplan.api.service;


import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.monthplan.api.domain.entity.MdmProductVulcanizing;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@FeignClient(contextId = "IMdmProductVulcanizingRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMdmProductVulcanizingRemoteService {

    /**
     * 查询基础数据-硫化机正在生产品种列表
     */
    @ApiOperation("查询基础数据-硫化机正在生产品种列表")
    @PostMapping("/vulcanization/list")
    TableDataInfo list(@RequestBody MdmProductVulcanizing docProductVulcanization);

    /**
     * 新增基础数据-硫化机正在生产品种
     */
    @ApiOperation("新增基础数据-硫化机正在生产品种")
    @PostMapping("/vulcanization/add")
    AjaxResult add(@RequestBody MdmProductVulcanizing docProductVulcanization);

    /**
     * 修改基础数据-硫化机正在生产品种
     */
    @ApiOperation("修改基础数据-硫化机正在生产品种")
    @PostMapping("/vulcanization/edit")
    AjaxResult edit(@RequestBody MdmProductVulcanizing docProductVulcanization);

    /**
     * 删除基础数据-硫化机正在生产品种
     */
    @ApiOperation("删除基础数据-硫化机正在生产品种")
    @DeleteMapping("/vulcanization/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/vulcanization/{id}")
    MdmProductVulcanizing getInfo(@PathVariable("id") Long id);

    /**
     * 校验基础数据-硫化机正在生产品种唯一性
     */
    @ApiOperation("校验基础数据-硫化机正在生产品种唯一性")
    @PostMapping("/vulcanization/checkDocProductVulcanizationUnique")
    String checkDocProductVulcanizationUnique(@RequestBody MdmProductVulcanizing docProductVulcanization);

    /**
     * 导出基础数据-硫化机正在生产品种列表
     */
    @ApiOperation("导出基础数据-硫化机正在生产品种列表")
    @PostMapping("/vulcanization/getList")
    List<MdmProductVulcanizing> getList(@RequestBody MdmProductVulcanizing docProductVulcanization);

    /**
     * 导入基础数据-硫化机正在生产品种数据
     */
    @ApiOperation("导入基础数据-硫化机正在生产品种")
    @PostMapping("/vulcanization/importData/{updateSupport}/{importLogId}")
    public AjaxResult importData(@RequestBody List<MdmProductVulcanizing> list, @PathVariable("updateSupport") boolean updateSupport, @PathVariable("importLogId") Long importLogId);

    /**
     * 根据物料编码获取物料信息
     */
    @PostMapping("/vulcanization/getProductInfo")
    AjaxResult getProductInfo(@RequestParam("productCode") String productCode);
}
