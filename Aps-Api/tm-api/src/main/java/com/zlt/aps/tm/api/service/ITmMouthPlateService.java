package com.zlt.aps.tm.api.service;


import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tm.api.domain.dto.TmMouthPlateDto;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 胎面口型板信息维护 前端控制器
 * </p>
 *
 * @author chenxueyuan
 * @since 2021-05-27
 */
@FeignClient(contextId = "ITmMouthPlateService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.tm:tm}")
public interface ITmMouthPlateService {

    /**
     * 查询胎面口型板信息维护列表
     */
    @PostMapping("/tm/mouthPlate/list")
    @ApiOperation("查询胎面口型板信息维护列表")
    public TableDataInfo list(@RequestBody TmMouthPlateDto dto);

    /**
     * 根据id获取胎面口型板信息维护详细信息
     */
    @GetMapping(value = "/tm/mouthPlate/{id}")
    @ApiOperation("获取胎面口型板信息详细信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public TmMouthPlateDto getInfo(@PathVariable("id") Long id);

    /**
     * 保存胎面口型板信息维护
     */
    @PostMapping("/tm/mouthPlate/edit")
    @ApiOperation("保存胎面口型板信息（id为空则新增，id不为空则修改）")
    public AjaxResult edit(@RequestBody TmMouthPlateDto dto);

    /**
     * 删除胎面口型板信息维护
     */
    @PostMapping("/tm/mouthPlate/{ids}")
    @ApiOperation("删除胎面口型板信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id数组", paramType = "query")
    })
    public AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 导出胎面口型板信息
     */
    @PostMapping("/tm/mouthPlate/exportData")
    @ApiOperation("导出胎面口型板信息")
    public List<TmMouthPlateDto> exportData(@SpringQueryMap TmMouthPlateDto dto);

    @PostMapping("/tm/mouthPlate/importData")
    @ApiOperation("导入胎面口型板信息")
    public AjaxResult importData(@RequestBody List<TmMouthPlateDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

    /**
     * 删除全部(逻辑删)
     */
    @PostMapping("/tm/mouthPlate/deleteAll")
    AjaxResult deleteAll();
}
