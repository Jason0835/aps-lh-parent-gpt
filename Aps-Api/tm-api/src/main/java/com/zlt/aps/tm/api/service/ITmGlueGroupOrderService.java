package com.zlt.aps.tm.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tm.api.domain.dto.TmGlueGroupOrderDto;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 胎面胶料组别顺序对外暴露接口
 */
@FeignClient(contextId = "iTmGlueGroupOrderService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.tm:tm}")
public interface ITmGlueGroupOrderService {

    /**
     * 根据条件查询胶料组别顺序列表
     */
    @GetMapping("/glueGroupOrder/listGlueGroupOrder")
    TableDataInfo listGlueGroupOrder(@SpringQueryMap TmGlueGroupOrderDto dto);

    /**
     * 根据id查询胶料组别顺序信息
     */
    @GetMapping("/glueGroupOrder/getGlueGroupOrder/{id}")
    TmGlueGroupOrderDto getGlueGroupOrder(@PathVariable("id") Long id);

    /**
     * 保存胶料组别顺序信息（id为空则新增，id不为空则修改）
     */
    @PostMapping("/glueGroupOrder/saveGlueGroupOrder")
    AjaxResult saveGlueGroupOrder(@RequestBody TmGlueGroupOrderDto dto);

    /**
     * 根据code判断胶料组号是否已经存在
     */
    @PostMapping("/glueGroupOrder/checkGlueGroupCodeUnique")
    String checkGlueGroupCodeUnique(@RequestBody TmGlueGroupOrderDto dto);

    /**
     * 批量删除胶料组别顺序信息(逻辑删)
     * @param ids 多个id逗号分割
     */
    @PostMapping("/glueGroupOrder/deleteGlueGroupOrder/{ids}")
    AjaxResult deleteGlueGroupOrder(@PathVariable("ids") Long[] ids);

    /**
     * 导出接口
     * @param dto
     */
    @GetMapping("/glueGroupOrder/exportData")
    List<TmGlueGroupOrderDto> exportData(@SpringQueryMap TmGlueGroupOrderDto dto);

    @PostMapping("/glueGroupOrder/importData")
    @ApiOperation("导入胎面胶料组别顺序信息")
    public AjaxResult importData(@RequestBody List<TmGlueGroupOrderDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
