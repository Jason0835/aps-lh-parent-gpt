package com.zlt.aps.dj.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.dj.api.domain.dto.DjGlueGroupOrderDto;

import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 垫胶胶料组别顺序对外暴露接口
 */
@FeignClient(contextId = "iNcGlueGroupOrderService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.dj:nc}")
public interface IDjGlueGroupOrderService {

    /**
     * 根据条件查询胶料组别顺序列表
     */
    @PostMapping("/dj/glueGroupOrder/listGlueGroupOrder")
    TableDataInfo listGlueGroupOrder(@RequestBody DjGlueGroupOrderDto dto);

    /**
     * 根据id查询胶料组别顺序信息
     */
    @GetMapping("/dj/glueGroupOrder/getGlueGroupOrder/{id}")
    DjGlueGroupOrderDto getGlueGroupOrder(@PathVariable("id") Long id);

    /**
     * 保存胶料组别顺序信息（id为空则新增，id不为空则修改）
     */
    @PostMapping("/dj/glueGroupOrder/saveGlueGroupOrder")
    AjaxResult saveGlueGroupOrder(@RequestBody DjGlueGroupOrderDto dto);

    /**
     * 根据code判断胶料组号是否已经存在
     */
    @PostMapping("/dj/glueGroupOrder/checkGlueGroupCodeUnique")
    String checkGlueGroupCodeUnique(@RequestBody DjGlueGroupOrderDto dto);

    /**
     * 批量删除胶料组别顺序信息(逻辑删)
     * @param ids 多个id逗号分割
     */
    @PostMapping("/dj/glueGroupOrder/deleteGlueGroupOrder/{ids}")
    AjaxResult deleteGlueGroupOrder(@PathVariable("ids") Long[] ids);

    /**
     * 导出接口
     * @param dto
     */
    @PostMapping("/dj/glueGroupOrder/exportData")
    List<DjGlueGroupOrderDto> exportData(@RequestBody DjGlueGroupOrderDto dto);

    @PostMapping("/dj/glueGroupOrder/importData")
    @ApiOperation("导入垫胶胶料组别顺序信息")
    public AjaxResult importData(@RequestBody List<DjGlueGroupOrderDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

}
