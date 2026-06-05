package com.zlt.aps.cd90.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd90.api.domain.dto.Cd90QuotaSettingDto;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 90度裁断定额设定Service接口
 *
 * @author chen
 * @date 2021-06-29
 */
@FeignClient(contextId = "ICd90QuotaSettingService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cd90:cd90}")
public interface ICd90QuotaSettingService {

    /**
     * 查询90度裁断定额设定列表
     *
     * @param dto 90度裁断定额设定
     * @return 90度裁断定额设定集合
     */
    @PostMapping("/cd90/quota/list")
    @ApiOperation("查询90度裁断定额设定信息维护列表")
    public TableDataInfo list(@RequestBody Cd90QuotaSettingDto dto);

    /**
     * 查询90度裁断定额设定
     *
     * @param id 90度裁断定额设定ID
     * @return 90度裁断定额设定
     */
    @GetMapping("/cd90/quota/{id}")
    @ApiOperation("查询90度裁断定额设定信息维护列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public Cd90QuotaSettingDto getInfo(@PathVariable("id") Long id);

    /**
     * 修改90度裁断定额设定
     *
     * @param dto 90度裁断定额设定
     * @return 结果
     */
    @PostMapping("/cd90/quota/edit")
    @ApiOperation("修改90度裁断定额设定（id为空则新增，id不为空则修改）")
    public AjaxResult edit(@RequestBody Cd90QuotaSettingDto dto);

    /**
     * 删除90度裁断定额设定
     *
     * @param ids 需要删除的90度裁断定额设定ID
     * @return 结果
     */
    @PostMapping("/cd90/quota/{ids}")
    @ApiOperation("删除90度裁断定额设定信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id数组", paramType = "query")
    })
    public AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 导出90度裁断定额设定信息
     */
    @PostMapping("/cd90/quota/export")
    @ApiOperation("导出90度裁断定额设定信息")
    public List<Cd90QuotaSettingDto> exportData(@RequestBody Cd90QuotaSettingDto dto);

    /**
     * 导入数据
     */
    @PostMapping("/cd90/quota/importData")
    public AjaxResult importData(@RequestBody List<Cd90QuotaSettingDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
