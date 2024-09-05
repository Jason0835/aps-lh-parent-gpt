package com.zlt.aps.tc.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tc.api.domain.dto.TcGlueOrderDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 胎侧胶料顺序对外暴露接口
 */
@FeignClient(contextId = "iTcGlueOrderService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tc:tc}")
public interface ITcGlueOrderService {

    /**
     * 根据条件查询胶料顺序列表
     */
    @GetMapping("/glueOrder/listGlueOrder")
    TableDataInfo listGlueOrder(@SpringQueryMap TcGlueOrderDto dto);

    /**
     * 根据id查询胶料顺序信息
     */
    @GetMapping("/glueOrder/getGlueOrder/{id}")
    TcGlueOrderDto getGlueOrder(@PathVariable("id") Long id);

    /**
     * 保存胶料顺序信息（id为空则新增，id不为空则修改）
     */
    @PostMapping("/glueOrder/saveGlueOrder")
    AjaxResult saveGlueOrder(@RequestBody TcGlueOrderDto dto);

    /**
     * 根据code判断胶料是否已经存在
     */
    @PostMapping("/glueOrder/checkGlueCodeUnique")
    String checkGlueCodeUnique(@RequestBody TcGlueOrderDto dto);

    /**
     * 批量删除胶料顺序信息(逻辑删)
     *
     * @param ids 多个id逗号分割`
     */
    @PostMapping("/glueOrder/deleteGlueOrder/{ids}")
    AjaxResult deleteGlueOrder(@PathVariable("ids") Long[] ids);

    /**
     * 导出接口
     *
     * @param dto
     */
    @GetMapping("/glueOrder/exportData")
    List<TcGlueOrderDto> exportData(@SpringQueryMap TcGlueOrderDto dto);

    /**
     * 数据导入
     */
    @PostMapping("/glueOrder/importData")
    AjaxResult importData(@RequestBody List<TcGlueOrderDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

}
