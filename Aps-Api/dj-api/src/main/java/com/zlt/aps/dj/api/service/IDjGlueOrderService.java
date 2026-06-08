package com.zlt.aps.dj.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.dj.api.domain.dto.DjGlueOrderDto;

import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 垫胶胶料顺序对外暴露接口
 */
@FeignClient(contextId = "iNcGlueOrderService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.dj:nc}")
public interface IDjGlueOrderService {

    /**
     * 根据条件查询胶料顺序列表
     */
    @PostMapping("/dj/glueOrder/listGlueOrder")
    TableDataInfo listGlueOrder(@RequestBody DjGlueOrderDto dto);

    /**
     * 根据id查询胶料顺序信息
     */
    @GetMapping("/dj/glueOrder/getGlueOrder/{id}")
    DjGlueOrderDto getGlueOrder(@PathVariable("id") Long id);

    /**
     * 保存胶料顺序信息（id为空则新增，id不为空则修改）
     */
    @PostMapping("/dj/glueOrder/saveGlueOrder")
    AjaxResult saveGlueOrder(@RequestBody DjGlueOrderDto dto);

    /**
     * 根据code判断胶料是否已经存在
     */
    @PostMapping("/dj/glueOrder/checkGlueCodeUnique")
    String checkGlueCodeUnique(@RequestBody DjGlueOrderDto dto);

    /**
     * 批量删除胶料顺序信息(逻辑删)
     * @param ids 多个id逗号分割`
     */
    @PostMapping("/dj/glueOrder/deleteGlueOrder/{ids}")
    AjaxResult deleteGlueOrder(@PathVariable("ids") Long[] ids);

    /**
     * 导出接口
     * @param dto
     */
    @PostMapping("/dj/glueOrder/exportData")
    List<DjGlueOrderDto> exportData(@RequestBody DjGlueOrderDto dto);

    @PostMapping("/dj/glueOrder/importData")
    @ApiOperation("导入垫胶胶料顺序信息")
    public AjaxResult importData(@RequestBody List<DjGlueOrderDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

}
