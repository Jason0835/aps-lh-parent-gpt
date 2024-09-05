package com.zlt.aps.lh.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.lh.api.domain.entity.LhInProductionSpec;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 硫化机台当前生产规格Service接口
 *
 * @author chen
 * @date 2022-03-23
 */
@FeignClient(contextId = "ILhInProductionSpecService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.lh:lh}")
public interface ILhInProductionSpecService {

    /**
     * 查询硫化机台当前生产规格列表
     */
    @ApiOperation("查询硫化机台当前生产规格列表")
    @PostMapping("/inProductionSpec/list")
    TableDataInfo list(@RequestBody LhInProductionSpec lhInProductionSpec);

    /**
     * 新增硫化机台当前生产规格
     */
    @ApiOperation("新增硫化机台当前生产规格")
    @PostMapping("/inProductionSpec/add")
    AjaxResult add(@RequestBody LhInProductionSpec lhInProductionSpec);

    /**
     * 修改硫化机台当前生产规格
     */
    @ApiOperation("修改硫化机台当前生产规格")
    @PostMapping("/inProductionSpec/edit")
    AjaxResult edit(@RequestBody LhInProductionSpec lhInProductionSpec);

    /**
     * 删除硫化机台当前生产规格
     */
    @ApiOperation("删除硫化机台当前生产规格")
    @DeleteMapping("/inProductionSpec/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/inProductionSpec/{id}")
    LhInProductionSpec getInfo(@PathVariable("id") Long id);

    /**
     * 校验硫化机台当前生产规格唯一性
     */
    @ApiOperation("校验硫化机台当前生产规格唯一性")
    @PostMapping("/inProductionSpec/checkLhInProductionSpecUnique")
    String checkLhInProductionSpecUnique(@RequestBody LhInProductionSpec lhInProductionSpec);

    /**
     * 导出硫化机台当前生产规格列表
     */
    @ApiOperation("导出硫化机台当前生产规格列表")
    @PostMapping("/inProductionSpec/getList")
    List<LhInProductionSpec> getList(@RequestBody LhInProductionSpec lhInProductionSpec);

    /**
     * 导入硫化机台当前生产规格数据
     */
    @ApiOperation("导入硫化机台当前生产规格")
    @PostMapping("/inProductionSpec/importData")
    public AjaxResult importData(@RequestBody List<LhInProductionSpec> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
