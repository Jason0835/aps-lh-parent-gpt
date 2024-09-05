package com.zlt.aps.tc.api.service;


import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tc.api.domain.dto.TcMouthPlateDto;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 胎侧口型板信息维护 前端控制器
 * </p>
 *
 * @author chenxueyuan
 * @since 2021-06-01
 */
@FeignClient(contextId = "ITcMouthPlateService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tc:tc}")
public interface ITcMouthPlateService {

    /**
     * 查询胎侧口型板信息维护列表
     */
    @PostMapping("/tc/mouthPlate/list")
    @ApiOperation("查询胎侧口型板信息维护列表")
    public TableDataInfo list(@RequestBody TcMouthPlateDto dto);

    /**
     * 根据id获取胎侧口型板信息维护详细信息
     */
    @GetMapping(value = "/tc/mouthPlate/{id}")
    @ApiOperation("获取胎侧口型板信息详细信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public TcMouthPlateDto getInfo(@PathVariable("id") Long id);

    /**
     * 保存胎侧口型板信息维护
     */
    @PostMapping("/tc/mouthPlate/edit")
    @ApiOperation("保存胎侧口型板信息（id为空则新增，id不为空则修改）")
    public AjaxResult edit(@RequestBody TcMouthPlateDto dto);

    /**
     * 删除胎侧口型板信息维护
     */
    @PostMapping("/tc/mouthPlate/{ids}")
    @ApiOperation("删除胎侧口型板信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id数组", paramType = "query")
    })
    public AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 导出胎侧口型板信息
     */
    @PostMapping("/tc/mouthPlate/exportData")
    @ApiOperation("导出胎侧口型板信息")
    public List<TcMouthPlateDto> exportData(@SpringQueryMap TcMouthPlateDto dto);

    /**
     * 数据导入
     */
    @PostMapping("/tc/mouthPlate/importData")
    AjaxResult importData(@RequestBody List<TcMouthPlateDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

    /**
     * 删除全部(逻辑删)
     */
    @PostMapping("/tc/mouthPlate/deleteAll")
    AjaxResult deleteAll();
}
