package com.zlt.aps.tc.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tc.api.domain.dto.TcGlueGroupOrderDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 胎侧胶料组别顺序对外暴露接口
 */
@FeignClient(contextId = "iTcGlueGroupOrderService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tc:tc}")
public interface ITcGlueGroupOrderService {

    /**
     * 根据条件查询胶料组别顺序列表
     */
    @PostMapping("/tc/glueGroupOrder/listGlueGroupOrder")
    TableDataInfo listGlueGroupOrder(@RequestBody TcGlueGroupOrderDto dto);

    /**
     * 根据id查询胶料组别顺序信息
     */
    @GetMapping("/tc/glueGroupOrder/getGlueGroupOrder/{id}")
    TcGlueGroupOrderDto getGlueGroupOrder(@PathVariable("id") Long id);

    /**
     * 保存胶料组别顺序信息（id为空则新增，id不为空则修改）
     */
    @PostMapping("/tc/glueGroupOrder/saveGlueGroupOrder")
    AjaxResult saveGlueGroupOrder(@RequestBody TcGlueGroupOrderDto dto);

    /**
     * 根据code判断胶料组号是否已经存在
     */
    @PostMapping("/tc/glueGroupOrder/checkGlueGroupCodeUnique")
    String checkGlueGroupCodeUnique(@RequestBody TcGlueGroupOrderDto dto);

    /**
     * 批量删除胶料组别顺序信息(逻辑删)
     *
     * @param ids 多个id逗号分割
     */
    @PostMapping("/tc/glueGroupOrder/deleteGlueGroupOrder/{ids}")
    AjaxResult deleteGlueGroupOrder(@PathVariable("ids") Long[] ids);

    /**
     * 导出接口
     *
     * @param dto
     */
    @PostMapping("/tc/glueGroupOrder/exportData")
    List<TcGlueGroupOrderDto> exportData(@RequestBody TcGlueGroupOrderDto dto);

    /**
     * 数据导入
     */
    @PostMapping("/tc/glueGroupOrder/importData")
    AjaxResult importData(@RequestBody List<TcGlueGroupOrderDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

}
