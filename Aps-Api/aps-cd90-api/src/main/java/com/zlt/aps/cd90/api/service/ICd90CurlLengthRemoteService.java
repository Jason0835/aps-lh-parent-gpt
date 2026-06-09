package com.zlt.aps.cd90.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd90.api.domain.entity.Cd90CurlLength;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 直裁卷曲长度 Feign 接口。
 */
@FeignClient(contextId = "ICd90CurlLengthRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cd90:/cd90}")
public interface ICd90CurlLengthRemoteService {

    /** 查询列表 */
    @ApiOperation("查询直裁卷曲长度列表")
    @PostMapping("/cd90CurlLength/list")
    TableDataInfo list(@RequestBody Cd90CurlLength queryVO);

    /** 获取详情 */
    @ApiOperation("获取直裁卷曲长度详情")
    @GetMapping("/cd90CurlLength/getInfo/{id}")
    Cd90CurlLength getInfo(@PathVariable("id") Long id);

    /** 新增 */
    @ApiOperation("新增直裁卷曲长度")
    @PostMapping("/cd90CurlLength/add")
    AjaxResult add(@RequestBody Cd90CurlLength entity);

    /** 编辑 */
    @ApiOperation("编辑直裁卷曲长度")
    @PostMapping("/cd90CurlLength/edit")
    AjaxResult edit(@RequestBody Cd90CurlLength entity);

    /** 删除 */
    @ApiOperation("删除直裁卷曲长度")
    @PostMapping("/cd90CurlLength/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /** 校验唯一性 */
    @ApiOperation("校验直裁卷曲长度唯一性")
    @PostMapping("/cd90CurlLength/checkUnique")
    String checkUnique(@RequestBody Cd90CurlLength entity);

    /** 导出数据 */
    @ApiOperation("导出直裁卷曲长度")
    @PostMapping("/cd90CurlLength/exportData/{fileName}")
    byte[] exportData(@RequestBody Cd90CurlLength queryVO, @PathVariable("fileName") String fileName);

    /** 导入数据 */
    @ApiOperation("导入直裁卷曲长度")
    @PostMapping("/cd90CurlLength/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
}