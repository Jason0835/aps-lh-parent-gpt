package com.zlt.aps.cx.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cx.api.domain.entity.InProductionSpec;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 成型机台当前生产规格Service接口
 *
 * @author chen
 * @date 2022-02-25
 */
@FeignClient(contextId = "IInProductionSpecService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cx:cx}")
public interface IInProductionSpecService {

    /**
     * 查询成型机台当前生产规格列表
     */
    @ApiOperation("查询成型机台当前生产规格列表")
    @PostMapping("/inProductionSpec/list")
    TableDataInfo list(@RequestBody InProductionSpec inProductionSpec);

    /**
     * 新增成型机台当前生产规格
     */
    @ApiOperation("新增成型机台当前生产规格")
    @PostMapping("/inProductionSpec/add")
    AjaxResult add(@RequestBody InProductionSpec inProductionSpec);

    /**
     * 修改成型机台当前生产规格
     */
    @ApiOperation("修改成型机台当前生产规格")
    @PostMapping("/inProductionSpec/edit")
    AjaxResult edit(@RequestBody InProductionSpec inProductionSpec);

    /**
     * 删除成型机台当前生产规格
     */
    @ApiOperation("删除成型机台当前生产规格")
    @DeleteMapping("/inProductionSpec/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/inProductionSpec/{id}")
    InProductionSpec getInfo(@PathVariable("id") Long id);

    /**
     * 校验成型机台当前生产规格唯一性
     */
    @ApiOperation("校验成型机台当前生产规格唯一性")
    @PostMapping("/inProductionSpec/checkInProductionSpecUnique")
    String checkInProductionSpecUnique(@RequestBody InProductionSpec inProductionSpec);

    /**
     * 导出成型机台当前生产规格列表
     */
    @ApiOperation("导出成型机台当前生产规格列表")
    @PostMapping("/inProductionSpec/getList")
    List<InProductionSpec> getList(@RequestBody InProductionSpec inProductionSpec);

    /**
     * 导入成型机台当前生产规格数据
     */
    @ApiOperation("导入成型机台当前生产规格")
    @PostMapping("/inProductionSpec/importData")
    public AjaxResult importData(@RequestBody List<InProductionSpec> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
