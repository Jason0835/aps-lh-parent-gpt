package com.zlt.aps.dj.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.dj.api.domain.dto.DjMouthPlateDto;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 垫胶口型板信息维护 前端控制器
 * </p>
 *
 * @author chenxueyuan
 * @since 2021-06-01
 */
@FeignClient(contextId = "INcMouthPlateService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.dj:nc}")
public interface IDjMouthPlateService {

    /**
     * 查询垫胶口型板信息维护列表
     */
    @PostMapping("/dj/mouthPlate/list")
    @ApiOperation("查询垫胶口型板信息维护列表")
    public TableDataInfo list(@RequestBody DjMouthPlateDto dto);

    /**
     * 根据id获取垫胶口型板信息维护详细信息
     */
    @GetMapping(value = "/dj/mouthPlate/{id}")
    @ApiOperation("获取垫胶口型板信息详细信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public DjMouthPlateDto getInfo(@PathVariable("id") Long id);

    /**
     * 保存垫胶口型板信息维护
     */
    @PostMapping("/dj/mouthPlate/edit")
    @ApiOperation("保存垫胶口型板信息（id为空则新增，id不为空则修改）")
    public AjaxResult edit(@RequestBody DjMouthPlateDto dto);

    /**
     * 删除垫胶口型板信息维护
     */
    @PostMapping("/dj/mouthPlate/{ids}")
    @ApiOperation("删除垫胶口型板信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id数组", paramType = "query")
    })
    public AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 导出垫胶口型板信息
     */
    @PostMapping("/dj/mouthPlate/exportData")
    @ApiOperation("导出垫胶口型板信息")
    public List<DjMouthPlateDto> exportData(@RequestBody DjMouthPlateDto dto);

    @PostMapping("/dj/mouthPlate/importData")
    @ApiOperation("导入垫胶口型板信息")
    public AjaxResult importData(@RequestBody List<DjMouthPlateDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
