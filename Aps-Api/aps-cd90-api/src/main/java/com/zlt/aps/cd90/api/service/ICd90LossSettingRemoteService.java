package com.zlt.aps.cd90.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd90.api.domain.entity.Cd90LossSetting;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 直裁损耗率设定 Feign 接口。
 */
@FeignClient(contextId = "ICd90LossSettingRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cd90:/cd90}")
public interface ICd90LossSettingRemoteService {

    @ApiOperation("查询直裁损耗率列表")
    @PostMapping("/loss/list")
    TableDataInfo list(@RequestBody Cd90LossSetting queryVO);

    @ApiOperation("获取直裁损耗率详情")
    @GetMapping("/loss/getInfo/{id}")
    Cd90LossSetting getInfo(@PathVariable("id") Long id);

    @ApiOperation("新增直裁损耗率")
    @PostMapping("/loss/add")
    AjaxResult add(@RequestBody Cd90LossSetting entity);

    @ApiOperation("编辑直裁损耗率")
    @PostMapping("/loss/edit")
    AjaxResult edit(@RequestBody Cd90LossSetting entity);

    @ApiOperation("删除直裁损耗率")
    @PostMapping("/loss/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    @ApiOperation("校验直裁损耗率唯一性")
    @PostMapping("/loss/checkUnique")
    String checkUnique(@RequestBody Cd90LossSetting entity);

    @ApiOperation("导出直裁损耗率")
    @PostMapping("/loss/exportData/{fileName}")
    byte[] exportData(@RequestBody Cd90LossSetting queryVO, @PathVariable("fileName") String fileName);

    @ApiOperation("导入直裁损耗率")
    @PostMapping("/loss/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
}