package com.zlt.aps.nc.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.nc.api.domain.dto.NcMouthPlateDto;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 内衬口型板信息维护 前端控制器
 * </p>
 *
 * @author chenxueyuan
 * @since 2021-06-01
 */
@FeignClient(contextId = "INcMouthPlateService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.nc:nc}")
public interface INcMouthPlateService {

    /**
     * 查询内衬口型板信息维护列表
     */
    @PostMapping("/nc/mouthPlate/list")
    @ApiOperation("查询内衬口型板信息维护列表")
    public TableDataInfo list(@RequestBody NcMouthPlateDto dto);

    /**
     * 根据id获取内衬口型板信息维护详细信息
     */
    @GetMapping(value = "/nc/mouthPlate/{id}")
    @ApiOperation("获取内衬口型板信息详细信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public NcMouthPlateDto getInfo(@PathVariable("id") Long id);

    /**
     * 保存内衬口型板信息维护
     */
    @PostMapping("/nc/mouthPlate/edit")
    @ApiOperation("保存内衬口型板信息（id为空则新增，id不为空则修改）")
    public AjaxResult edit(@RequestBody NcMouthPlateDto dto);

    /**
     * 删除内衬口型板信息维护
     */
    @PostMapping("/nc/mouthPlate/{ids}")
    @ApiOperation("删除内衬口型板信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id数组", paramType = "query")
    })
    public AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 导出内衬口型板信息
     */
    @PostMapping("/nc/mouthPlate/exportData")
    @ApiOperation("导出内衬口型板信息")
    public List<NcMouthPlateDto> exportData(@RequestBody NcMouthPlateDto dto);

    @PostMapping("/nc/mouthPlate/importData")
    @ApiOperation("导入内衬口型板信息")
    public AjaxResult importData(@RequestBody List<NcMouthPlateDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
