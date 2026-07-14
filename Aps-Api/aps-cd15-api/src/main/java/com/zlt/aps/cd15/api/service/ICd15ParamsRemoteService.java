package com.zlt.aps.cd15.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd15.api.domain.entity.Cd15Params;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 15度裁断参数设置远程服务
 */
@FeignClient(contextId = "ICd15ParamsRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cd15:/cd15}")
public interface ICd15ParamsRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/cd15Params/list")
    TableDataInfo list(@RequestBody Cd15Params queryVO);

    /**
     * 获取详情
     */
    @ApiOperation("获取详情")
    @GetMapping("/cd15Params/getInfo/{id}")
    Cd15Params getInfo(@PathVariable("id") Long id);

    /**
     * Get parameter value by factory code and parameter code.
     */
    @ApiOperation("get param value")
    @GetMapping("/cd15Params/getParamValue/{factoryCode}/{paramCode}")
    AjaxResult getParamValue(@PathVariable("factoryCode") String factoryCode, @PathVariable("paramCode") String paramCode);

    /**
     * 新增
     */
    @ApiOperation("新增")
    @PostMapping("/cd15Params/add")
    AjaxResult add(@RequestBody Cd15Params entity);

    /**
     * 编辑
     */
    @ApiOperation("编辑")
    @PostMapping("/cd15Params/edit")
    AjaxResult edit(@RequestBody Cd15Params entity);

    /**
     * 删除
     */
    @ApiOperation("删除")
    @PostMapping("/cd15Params/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/cd15Params/checkUnique")
    String checkUnique(@RequestBody Cd15Params entity);

    /**
     * 导出
     */
    @ApiOperation("导出")
    @PostMapping("/cd15Params/exportData/{fileName}")
    byte[] exportData(@RequestBody Cd15Params queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入
     */
    @ApiOperation("导入")
    @PostMapping("/cd15Params/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
}
