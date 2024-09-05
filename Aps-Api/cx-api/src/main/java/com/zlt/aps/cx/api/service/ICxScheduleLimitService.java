package com.zlt.aps.cx.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cx.api.domain.dto.CxScheduleLimitDto;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 成型排产限制Service接口
 *
 * @author zlt
 * @date 2021-06-11
 */
@FeignClient(contextId = "ICxScheduleLimitService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cx:cx}")
public interface ICxScheduleLimitService {
    /**
     * 查询成型排产限制列表
     *
     * @param dto 成型排产限制
     * @return 成型排产限制集合
     */
    @PostMapping("/cx/limit/list")
    @ApiOperation("查询成型排产限制信息维护列表")
    public TableDataInfo list(@RequestBody CxScheduleLimitDto dto);

    /**
     * 查询成型排产限制
     *
     * @param id 成型排产限制ID
     * @return 成型排产限制
     */
    @GetMapping("/cx/limit/{id}")
    @ApiOperation("查询成型排产限制信息维护列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public CxScheduleLimitDto getInfo(@PathVariable("id") Long id);

    /**
     * 修改成型排产限制
     *
     * @param dto 成型排产限制
     * @return 结果
     */
    @PostMapping("/cx/limit/edit")
    @ApiOperation("修改成型排产限制（id为空则新增，id不为空则修改）")
    public AjaxResult edit(@RequestBody CxScheduleLimitDto dto);

    /**
     * 删除成型排产限制
     *
     * @param ids 需要删除的成型排产限制ID
     * @return 结果
     */
    @PostMapping("/cx/limit/{ids}")
    @ApiOperation("删除成型排产限制信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id数组", paramType = "query")
    })
    public AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 导出成型排产限制信息
     */
    @PostMapping("/cx/limit/export")
    @ApiOperation("导出成型排产限制信息")
    public List<CxScheduleLimitDto> exportData(@SpringQueryMap CxScheduleLimitDto dto);

    /**
     * 导入数据
     */
    @PostMapping("/cx/limit/importData")
    public AjaxResult importData(@RequestBody List<CxScheduleLimitDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
