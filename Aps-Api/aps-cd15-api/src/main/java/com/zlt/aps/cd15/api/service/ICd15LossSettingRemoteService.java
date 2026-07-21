package com.zlt.aps.cd15.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd15.api.domain.entity.Cd15LossSetting;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 斜裁损耗率设定 Feign 接口。
 */
@FeignClient(contextId = "ICd15LossSettingRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cd15:/cd15}")
public interface ICd15LossSettingRemoteService {

    @ApiOperation("查询斜裁损耗率列表")
    @PostMapping("/cd15LossSetting/list")
    TableDataInfo list(@RequestBody Cd15LossSetting queryVO);

    @ApiOperation("获取斜裁损耗率详情")
    @GetMapping("/cd15LossSetting/getInfo/{id}")
    Cd15LossSetting getInfo(@PathVariable("id") Long id);

    @ApiOperation("新增斜裁损耗率")
    @PostMapping("/cd15LossSetting/add")
    AjaxResult add(@RequestBody Cd15LossSetting entity);

    @ApiOperation("编辑斜裁损耗率")
    @PostMapping("/cd15LossSetting/edit")
    AjaxResult edit(@RequestBody Cd15LossSetting entity);

    @ApiOperation("删除斜裁损耗率")
    @PostMapping("/cd15LossSetting/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    @ApiOperation("校验斜裁损耗率唯一性")
    @PostMapping("/cd15LossSetting/checkUnique")
    String checkUnique(@RequestBody Cd15LossSetting entity);

    @ApiOperation("导出斜裁损耗率")
    @PostMapping("/cd15LossSetting/exportData/{fileName}")
    byte[] exportData(@RequestBody Cd15LossSetting queryVO, @PathVariable("fileName") String fileName);

    @ApiOperation("导入斜裁损耗率")
    @PostMapping("/cd15LossSetting/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
}
