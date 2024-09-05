package com.zlt.aps.cx.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cx.api.domain.dto.CxMonthStockDto;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 成型月结库存接口
 *
 * @author chen
 * @date 2021-06-17
 */
@FeignClient(contextId = "ICxMonthStockService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cx:cx}")
public interface ICxMonthStockService {
    /**
     * 查询成型月结库存列表
     *
     * @param dto 成型月结库存
     * @return 成型月结库存集合
     */
    @PostMapping("/cx/monthStock/list")
    @ApiOperation("查询成型月结库存信息维护列表")
    public TableDataInfo list(@RequestBody CxMonthStockDto dto);

    /**
     * 查询成型月结库存
     *
     * @param id 成型月结库存ID
     * @return 成型月结库存
     */
    @GetMapping("/cx/monthStock/{id}")
    @ApiOperation("查询成型月结库存信息维护列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public CxMonthStockDto getInfo(@PathVariable("id") Long id);

    /**
     * 修改成型月结库存
     *
     * @param dto 成型月结库存
     * @return 结果
     */
    @PostMapping("/cx/monthStock/edit")
    @ApiOperation("修改成型月结库存（id为空则新增，id不为空则修改）")
    public AjaxResult edit(@RequestBody CxMonthStockDto dto);

    /**
     * 删除成型月结库存
     *
     * @param ids 需要删除的成型月结库存ID
     * @return 结果
     */
    @PostMapping("/cx/monthStock/{ids}")
    @ApiOperation("删除成型月结库存信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id数组", paramType = "query")
    })
    public AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 导出成型月结库存信息
     */
    @PostMapping("/cx/monthStock/export")
    @ApiOperation("导出成型月结库存信息")
    public List<CxMonthStockDto> exportData(@SpringQueryMap CxMonthStockDto dto);

    /**
     * 导入数据
     */
    @PostMapping("/cx/monthStock/importData")
    public AjaxResult importData(@RequestBody List<CxMonthStockDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
