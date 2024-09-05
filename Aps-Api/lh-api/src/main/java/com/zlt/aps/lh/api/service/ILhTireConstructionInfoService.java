package com.zlt.aps.lh.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.lh.api.domain.entity.LhTireConstructionInfo;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 硫化外胎施工信息Service接口
 * @author zlt
 * @date 2021-11-15
 */
@FeignClient(contextId = "ILhTireConstructionInfoService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.lh:lh}")
public interface ILhTireConstructionInfoService {

    /**
     * 查询硫化外胎施工信息列表
     */
    @ApiOperation("查询硫化外胎施工信息列表")
    @PostMapping("/lhTireConstructionInfo/list")
    TableDataInfo list(@RequestBody LhTireConstructionInfo lhTireConstructionInfo);

    /**
    * 新增硫化外胎施工信息
    */
    @ApiOperation("新增硫化外胎施工信息")
    @PostMapping("/lhTireConstructionInfo/add")
    AjaxResult add(@RequestBody LhTireConstructionInfo lhTireConstructionInfo);

    /**
     * 修改硫化外胎施工信息
     */
    @ApiOperation("修改硫化外胎施工信息")
    @PostMapping("/lhTireConstructionInfo/edit")
    AjaxResult edit(@RequestBody LhTireConstructionInfo lhTireConstructionInfo);

    /**
     * 删除硫化外胎施工信息
     */
    @ApiOperation("删除硫化外胎施工信息")
    @DeleteMapping("/lhTireConstructionInfo/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/lhTireConstructionInfo/{id}")
    LhTireConstructionInfo getInfo(@PathVariable("id") Long id);

    /**
     * 校验硫化外胎施工信息唯一性
     */
    @ApiOperation("校验硫化外胎施工信息唯一性")
    @PostMapping("/lhTireConstructionInfo/checkLhTireConstructionInfoUnique")
    String checkLhTireConstructionInfoUnique(@RequestBody LhTireConstructionInfo lhTireConstructionInfo);

    /**
     * 导出硫化外胎施工信息列表
     */
    @ApiOperation("导出硫化外胎施工信息列表")
    @PostMapping("/lhTireConstructionInfo/getList")
    List<LhTireConstructionInfo> getList(@RequestBody LhTireConstructionInfo lhTireConstructionInfo);

    /**
     * 导入硫化外胎施工信息数据
     */
    @ApiOperation("导入硫化外胎施工信息")
    @PostMapping("/lhTireConstructionInfo/importData")
    public AjaxResult importData(@RequestBody List<LhTireConstructionInfo> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

    /**
     * 根据sap查询对应的胎胚代码
     * @param lhTireConstructionInfo sap品号
     * @return 查询到的胎胚代码
     */
    @ApiOperation("根据sap查询对应的胎胚代码")
    @PostMapping("/lhTireConstructionInfo/getEmbryoCodeListBySapCode")
    public List<LhTireConstructionInfo> getEmbryoCodeListBySapCode(@RequestBody LhTireConstructionInfo lhTireConstructionInfo);
}
