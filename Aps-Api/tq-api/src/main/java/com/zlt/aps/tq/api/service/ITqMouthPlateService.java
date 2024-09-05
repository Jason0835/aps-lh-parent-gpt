package com.zlt.aps.tq.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tq.api.domain.dto.TqMouthPlateDto;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 胎圈口型板信息维护 前端控制器
 * </p>
 *
 * @author chenxueyuan
 * @since 2021-06-08
 */
@FeignClient(contextId = "ITqMouthPlateService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.tq:tq}")
public interface ITqMouthPlateService {

    /**
     * 查询胎圈口型板信息维护列表
     */
    @PostMapping("/tq/mouthPlate/list")
    @ApiOperation("查询胎圈口型板信息维护列表")
    public TableDataInfo list(@RequestBody TqMouthPlateDto dto);

    /**
     * 根据id获取胎圈口型板信息维护详细信息
     */
    @GetMapping(value = "/tq/mouthPlate/{id}")
    @ApiOperation("获取胎圈口型板信息详细信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public TqMouthPlateDto getInfo(@PathVariable("id") Long id);

    /**
     * 保存胎圈口型板信息维护
     */
    @PostMapping("/tq/mouthPlate/edit")
    @ApiOperation("保存胎圈口型板信息（id为空则新增，id不为空则修改）")
    public AjaxResult edit(@RequestBody TqMouthPlateDto dto);

    /**
     * 删除胎圈口型板信息维护
     */
    @PostMapping("/tq/mouthPlate/{ids}")
    @ApiOperation("删除胎圈口型板信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id数组", paramType = "query")
    })
    public AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 导出胎圈口型板信息
     */
    @PostMapping("/tq/mouthPlate/exportData")
    @ApiOperation("导出胎圈口型板信息")
    public List<TqMouthPlateDto> exportData(@SpringQueryMap TqMouthPlateDto dto);

    @PostMapping("/tq/mouthPlate/importData")
    @ApiOperation("导入胎圈口型板信息")
    public AjaxResult importData(@RequestBody List<TqMouthPlateDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

    /**
     * 删除全部(逻辑删)
     */
    @PostMapping("/tq/mouthPlate/deleteAll")
    AjaxResult deleteAll();
}
