package com.zlt.aps.cd15.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd15.api.domain.entity.Cd15CurlLength;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 斜裁卷曲长度 Feign 接口。
 */
@FeignClient(contextId = "ICd15CurlLengthRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cd15:/cd15}")
public interface ICd15CurlLengthRemoteService {

    /** 查询列表 */
    @ApiOperation("查询斜裁卷曲长度列表")
    @PostMapping("/curlLength/list")
    TableDataInfo list(@RequestBody Cd15CurlLength queryVO);

    /** 获取详情 */
    @ApiOperation("获取斜裁卷曲长度详情")
    @GetMapping("/curlLength/getInfo/{id}")
    Cd15CurlLength getInfo(@PathVariable("id") Long id);

    /** 新增 */
    @ApiOperation("新增斜裁卷曲长度")
    @PostMapping("/curlLength/add")
    AjaxResult add(@RequestBody Cd15CurlLength entity);

    /** 编辑 */
    @ApiOperation("编辑斜裁卷曲长度")
    @PostMapping("/curlLength/edit")
    AjaxResult edit(@RequestBody Cd15CurlLength entity);

    /** 删除 */
    @ApiOperation("删除斜裁卷曲长度")
    @PostMapping("/curlLength/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /** 校验唯一性 */
    @ApiOperation("校验斜裁卷曲长度唯一性")
    @PostMapping("/curlLength/checkUnique")
    String checkUnique(@RequestBody Cd15CurlLength entity);

    /** 导出数据 */
    @ApiOperation("导出斜裁卷曲长度")
    @PostMapping("/curlLength/exportData/{fileName}")
    byte[] exportData(@RequestBody Cd15CurlLength queryVO, @PathVariable("fileName") String fileName);

    /** 导入数据 */
    @ApiOperation("导入斜裁卷曲长度")
    @PostMapping("/curlLength/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
}
