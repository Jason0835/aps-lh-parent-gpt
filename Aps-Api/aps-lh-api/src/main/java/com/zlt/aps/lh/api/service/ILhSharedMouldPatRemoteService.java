package com.zlt.aps.lh.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.lh.api.domain.entity.LhSharedMouldPat;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 共用模具花纹配置前端接口
 *
 * @author zlt
 * @date 2026-05-14
 */
@FeignClient(contextId = "ILhSharedMouldPatRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.lh:/lh}")
public interface ILhSharedMouldPatRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/lhSharedMouldPat/list")
    TableDataInfo list(@RequestBody LhSharedMouldPat queryVO);

    /**
     * 保存
     */
    @ApiOperation("保存")
    @PostMapping("/lhSharedMouldPat/save")
    AjaxResult save(@RequestBody LhSharedMouldPat lhSharedMouldPat);

    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/lhSharedMouldPat/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/lhSharedMouldPat/{id}")
    LhSharedMouldPat getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/lhSharedMouldPat/checkUnique")
    String checkUnique(@RequestBody LhSharedMouldPat lhSharedMouldPatVO);

    /**
     * 导出共用模具花纹配置列表
     */
    @ApiOperation("导出列表")
    @PostMapping("/lhSharedMouldPat/exportData/{fileName}")
    byte[] exportData(@RequestBody LhSharedMouldPat queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入共用模具花纹配置数据
     */
    @ApiOperation("导入共用模具花纹配置")
    @PostMapping("/lhSharedMouldPat/importData/{updateSupport}")
    AjaxResult importData(@RequestBody ImportContext importContext, @PathVariable("updateSupport") boolean updateSupport);
}
