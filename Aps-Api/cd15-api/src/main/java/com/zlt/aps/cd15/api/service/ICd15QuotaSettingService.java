package com.zlt.aps.cd15.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd15.api.domain.dto.Cd15QuotaSettingDto;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 15度裁断定额设定Service接口
 * @author chen
 * @date 2021-06-28
 */
@FeignClient(contextId = "ICd15QuotaSettingService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cd15:cd15}")
public interface ICd15QuotaSettingService {

    /**
     * 查询15度裁断定额设定列表
     *
     * @param dto 15度裁断定额设定
     * @return 15度裁断定额设定集合
     */
    @PostMapping("/cd15/quota/list")
    @ApiOperation("查询15度裁断定额设定信息维护列表")
    public TableDataInfo list(@RequestBody Cd15QuotaSettingDto dto);

    /**
     * 查询15度裁断定额设定
     *
     * @param id 15度裁断定额设定ID
     * @return 15度裁断定额设定
     */
    @GetMapping("/cd15/quota/{id}")
    @ApiOperation("查询15度裁断定额设定信息维护列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public Cd15QuotaSettingDto getInfo(@PathVariable("id") Long id);

    /**
     * 修改15度裁断定额设定
     *
     * @param dto 15度裁断定额设定
     * @return 结果
     */
    @PostMapping("/cd15/quota/edit")
    @ApiOperation("修改15度裁断定额设定（id为空则新增，id不为空则修改）")
    public AjaxResult edit(@RequestBody Cd15QuotaSettingDto dto);

    /**
     * 删除15度裁断定额设定
     *
     * @param ids 需要删除的15度裁断定额设定ID
     * @return 结果
     */
    @PostMapping("/cd15/quota/{ids}")
    @ApiOperation("删除15度裁断定额设定信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id数组", paramType = "query")
    })
    public AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 导出15度裁断定额设定信息
     */
    @PostMapping("/cd15/quota/export")
    @ApiOperation("导出15度裁断定额设定信息")
    public List<Cd15QuotaSettingDto> exportData(@SpringQueryMap Cd15QuotaSettingDto dto);

    @PostMapping("/cd15/quota/importData")
    @ApiOperation("导入15度裁断定额设定信息")
    public AjaxResult importData(@RequestBody List<Cd15QuotaSettingDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

}
