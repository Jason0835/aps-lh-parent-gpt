package com.zlt.aps.cx.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cx.api.domain.entity.CxShareMoldInfo;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 成型胎胚共用模具信息Service接口
 *
 * @author chen
 * @date 2022-03-22
 */
@FeignClient(contextId = "ICxShareMoldInfoService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cx:cx}")
public interface ICxShareMoldInfoService {

    /**
     * 查询成型胎胚共用模具信息列表
     */
    @ApiOperation("查询成型胎胚共用模具信息列表")
    @PostMapping("/shareMoldInfo/list")
    TableDataInfo list(@RequestBody CxShareMoldInfo cxShareMoldInfo);

    /**
     * 新增成型胎胚共用模具信息
     */
    @ApiOperation("新增成型胎胚共用模具信息")
    @PostMapping("/shareMoldInfo/add")
    AjaxResult add(@RequestBody CxShareMoldInfo cxShareMoldInfo);

    /**
     * 修改成型胎胚共用模具信息
     */
    @ApiOperation("修改成型胎胚共用模具信息")
    @PostMapping("/shareMoldInfo/edit")
    AjaxResult edit(@RequestBody CxShareMoldInfo cxShareMoldInfo);

    /**
     * 删除成型胎胚共用模具信息
     */
    @ApiOperation("删除成型胎胚共用模具信息")
    @DeleteMapping("/shareMoldInfo/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/shareMoldInfo/{id}")
    CxShareMoldInfo getInfo(@PathVariable("id") Long id);

    /**
     * 校验成型胎胚共用模具信息唯一性
     */
    @ApiOperation("校验成型胎胚共用模具信息唯一性")
    @PostMapping("/shareMoldInfo/checkCxShareMoldInfoUnique")
    String checkCxShareMoldInfoUnique(@RequestBody CxShareMoldInfo cxShareMoldInfo);

    /**
     * 导出成型胎胚共用模具信息列表
     */
    @ApiOperation("导出成型胎胚共用模具信息列表")
    @PostMapping("/shareMoldInfo/getList")
    List<CxShareMoldInfo> getList(@RequestBody CxShareMoldInfo cxShareMoldInfo);

    /**
     * 导入成型胎胚共用模具信息数据
     */
    @ApiOperation("导入成型胎胚共用模具信息")
    @PostMapping("/shareMoldInfo/importData")
    public AjaxResult importData(@RequestBody List<CxShareMoldInfo> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
