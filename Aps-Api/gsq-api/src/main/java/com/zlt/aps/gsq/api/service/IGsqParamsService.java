package com.zlt.aps.gsq.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.gsq.api.domain.dto.GsqParamsDto;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 钢丝圈排程参数配置 前端接口
 *
 * @author zlt
 * @date 2021-05-25
 */
@FeignClient(contextId = "IGsqParamsService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.gsq:gsq}")
public interface IGsqParamsService {

    /**
     * 查询钢丝圈排程参数配置列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/gsq/params/list")
    TableDataInfo list(@RequestBody GsqParamsDto queryVO);

    /**
     * 保存钢丝圈排程参数配置
     */
    @ApiOperation("保存")
    @PostMapping("/gsq/params/save")
    AjaxResult save(GsqParamsDto gsqParamsDto);

    /**
     * 删除钢丝圈排程参数配置
     */
    @ApiOperation("删除")
    @DeleteMapping("/gsq/params/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取钢丝圈排程参数配置详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/gsq/params/{id}")
    GsqParamsDto getInfo(@PathVariable("id") Long id);

    /**
     * 修改钢丝圈排程参数配置
     */
    @ApiOperation("修改")
    @PostMapping("/gsq/params/edit")
    AjaxResult edit(@RequestBody GsqParamsDto dto);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/gsq/params/checkUnique")
    String checkUnique(@RequestBody GsqParamsDto gsqParamsDto);

    /**
     * 导出列表
     */
    @ApiOperation("导出列表")
    @PostMapping("/gsq/params/exportData/{fileName}")
    byte[] exportData(@RequestBody GsqParamsDto queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入数据
     */
    @ApiOperation("导入数据")
    @PostMapping("/gsq/params/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
}
