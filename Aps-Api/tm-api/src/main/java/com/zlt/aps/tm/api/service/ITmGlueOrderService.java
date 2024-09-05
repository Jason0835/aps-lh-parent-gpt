package com.zlt.aps.tm.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tm.api.domain.dto.TmGlueOrderDto;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 胎面胶料顺序对外暴露接口
 */
@FeignClient(contextId = "iTmGlueOrderService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.tm:tm}")
public interface ITmGlueOrderService {

    /**
     * 根据条件查询胶料顺序列表
     */
    @GetMapping("/glueOrder/listGlueOrder")
    TableDataInfo listGlueOrder(@SpringQueryMap TmGlueOrderDto dto);

    /**
     * 根据id查询胶料顺序信息
     */
    @GetMapping("/glueOrder/getGlueOrder/{id}")
    TmGlueOrderDto getGlueOrder(@PathVariable("id") Long id);

    /**
     * 保存胶料顺序信息（id为空则新增，id不为空则修改）
     */
    @PostMapping("/glueOrder/saveGlueOrder")
    AjaxResult saveGlueOrder(@RequestBody TmGlueOrderDto dto);

    /**
     * 根据code判断胶料顺序是否已经存在
     */
    @PostMapping("/glueOrder/checkGlueCodeUnique")
    String checkGlueCodeUnique(@RequestBody TmGlueOrderDto dto);

    /**
     * 批量删除胶料顺序信息(逻辑删)
     * @param ids 多个id逗号分割
     */
    @PostMapping("/glueOrder/deleteGlueOrder/{ids}")
    AjaxResult deleteGlueOrder(@PathVariable("ids") Long[] ids);

    /**
     * 导出接口
     * @param dto
     */
    @GetMapping("/glueOrder/exportData")
    List<TmGlueOrderDto> exportData(@SpringQueryMap TmGlueOrderDto dto);

    @PostMapping("/glueOrder/importData")
    @ApiOperation("导入胎面胶料顺序信息")
    public AjaxResult importData(@RequestBody List<TmGlueOrderDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

}
