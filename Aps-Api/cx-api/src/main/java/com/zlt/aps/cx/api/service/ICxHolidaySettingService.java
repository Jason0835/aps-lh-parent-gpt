package com.zlt.aps.cx.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cx.api.domain.dto.CxHolidaySettingDto;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 假日设定Service接口
 *
 * @author chen
 * @date 2021-06-30
 */
@FeignClient(contextId = "ICxHolidaySettingService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cx:cx}")
public interface ICxHolidaySettingService {

    /**
     * 查询成型假日设定列表
     *
     * @param dto 成型假日设定
     * @return 成型假日设定集合
     */
    @PostMapping("/cx/holiday/list")
    @ApiOperation("查询成型假日设定信息维护列表")
    public TableDataInfo list(@RequestBody CxHolidaySettingDto dto);

    /**
     * 查询成型假日设定
     *
     * @param id 成型假日设定ID
     * @return 成型假日设定
     */
    @GetMapping("/cx/holiday/{id}")
    @ApiOperation("查询成型假日设定信息维护列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public CxHolidaySettingDto getInfo(@PathVariable("id") Long id);

    /**
     * 修改成型假日设定
     *
     * @param dto 成型假日设定
     * @return 结果
     */
    @PostMapping("/cx/holiday/edit")
    @ApiOperation("修改成型假日设定（id为空则新增，id不为空则修改）")
    public AjaxResult edit(@RequestBody CxHolidaySettingDto dto);

    /**
     * 删除成型假日设定
     *
     * @param ids 需要删除的成型假日设定ID
     * @return 结果
     */
    @PostMapping("/cx/holiday/{ids}")
    @ApiOperation("删除成型假日设定信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id数组", paramType = "query")
    })
    public AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 导出成型假日设定信息
     */
    @PostMapping("/cx/holiday/export")
    @ApiOperation("导出成型假日设定信息")
    public List<CxHolidaySettingDto> exportData(@SpringQueryMap CxHolidaySettingDto dto);

    /**
     * 校验记录唯一性
     *
     * @param dto 要校验记录
     * @return 查询到的结果
     */
    @PostMapping("/cx/holiday/checkUnique")
    @ApiOperation("校验记录唯一性")
    public List<CxHolidaySettingDto> checkUnique(@SpringQueryMap CxHolidaySettingDto dto);

    /**
     * 导入数据
     */
    @PostMapping("/cx/holiday/importData")
    public AjaxResult importData(@RequestBody List<CxHolidaySettingDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
