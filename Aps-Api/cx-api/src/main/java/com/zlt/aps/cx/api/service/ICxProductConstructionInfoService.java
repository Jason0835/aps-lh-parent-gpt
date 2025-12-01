package com.zlt.aps.cx.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cx.api.domain.entity.CxProductConstructionInfo;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 投产施工信息Service接口
 * @author zlt
 * @date 2021-12-02
 */
@FeignClient(contextId = "ICxProductConstructionInfoService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cxlh:/cxlh}")
public interface ICxProductConstructionInfoService {

    /**
     * 查询投产施工信息列表
     */
    @ApiOperation("查询投产施工信息列表")
    @PostMapping("/productConstruction/list")
    TableDataInfo list(@RequestBody CxProductConstructionInfo cxProductConstructionInfo);

    /**
    * 新增投产施工信息
    */
    @ApiOperation("新增投产施工信息")
    @PostMapping("/productConstruction/add")
    AjaxResult add(@RequestBody CxProductConstructionInfo cxProductConstructionInfo);

    /**
     * 修改投产施工信息
     */
    @ApiOperation("修改投产施工信息")
    @PostMapping("/productConstruction/edit")
    AjaxResult edit(@RequestBody CxProductConstructionInfo cxProductConstructionInfo);

    /**
     * 修改投产施工信息
     */
    @ApiOperation("修改投产施工信息")
    @PostMapping("/productConstruction/edit1")
    AjaxResult edit1(@RequestBody CxProductConstructionInfo cxProductConstructionInfo);

    @ApiOperation("修改投产施工信息")
    @PostMapping("/productConstruction/updateProductionStage")
    AjaxResult updateProductionStage(@RequestBody CxProductConstructionInfo cxProductConstructionInfo);

    /**
     * 删除投产施工信息
     */
    @ApiOperation("删除投产施工信息")
    @DeleteMapping("/productConstruction/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/productConstruction/{id}")
    CxProductConstructionInfo getInfo(@PathVariable("id") Long id);

    /**
     * 校验投产施工信息唯一性
     */
    @ApiOperation("校验投产施工信息唯一性")
    @PostMapping("/productConstruction/checkCxProductConstructionInfoUnique")
    String checkCxProductConstructionInfoUnique(@RequestBody CxProductConstructionInfo cxProductConstructionInfo);

    /**
     * 导出投产施工信息列表
     */
    @ApiOperation("导出投产施工信息列表")
    @PostMapping("/conversion/productConstruction/getList")
    List<CxProductConstructionInfo> getList(@RequestBody CxProductConstructionInfo cxProductConstructionInfo);

    /**
     * 获取胎胚版本列表
     */
    @ApiOperation("获取胎胚版本列表")
    @PostMapping("/productConstruction/getEmbryoVersions")
    List<CxProductConstructionInfo> getEmbryoVersions(@RequestBody CxProductConstructionInfo pc);

    /**
     * 获取半部件版本列表
     */
    @ApiOperation("获取半部件版本列表")
    @PostMapping("/productConstruction/getPartVersions")
    List<CxProductConstructionInfo> getPartVersions(@RequestBody CxProductConstructionInfo pc);

    /**
     * 导入投产施工信息数据
     */
    @ApiOperation("导入投产施工信息")
    @PostMapping("/productConstruction/importData")
    public AjaxResult importData(@RequestBody List<CxProductConstructionInfo> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
