package com.zlt.aps.nc.api.service;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.nc.api.domain.dto.NcGlueGroupOrderDto;
import com.zlt.aps.nc.api.domain.entity.NcGlueGroupOrder;

/**
 * 内衬胶料组别顺序对外暴露接口
 */
@FeignClient(contextId = "INcGlueGroupOrderRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.nc:nc}")
public interface INcGlueGroupOrderRemoteService {

    /**
     * 根据条件查询胶料组别顺序列表
     */
    @PostMapping("/nc/glueGroupOrder/list")
    TableDataInfo list(@RequestBody NcGlueGroupOrder glueGroupOrder);

    /**
     * 保存胶料组别顺序信息（id为空则新增，id不为空则修改）
     */
    @PostMapping("/nc/glueGroupOrder/save")
    AjaxResult save(@RequestBody NcGlueGroupOrder glueGroupOrder);

    /**
     * 根据id查询胶料组别顺序信息
     */
    @GetMapping(value = "/nc/glueGroupOrder/{id}")
    AjaxResult getInfo(@PathVariable("id") Long id);

    /**
     * 根据code判断胶料组号是否已经存在
     */
    @PostMapping("/nc/glueGroupOrder/checkGlueGroupCodeUnique")
    String checkGlueGroupCodeUnique(@RequestBody NcGlueGroupOrderDto dto);

    /**
     * 校验唯一性
     */
    @PostMapping("/nc/glueGroupOrder/checkUnique")
    String checkUnique(@RequestBody NcGlueGroupOrder glueGroupOrder);

    /**
     * 批量删除胶料组别顺序信息(逻辑删)
     */
    @PostMapping("/nc/glueGroupOrder/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 导出接口
     */
    @PostMapping("/nc/glueGroupOrder/exportData/{fileName}")
    byte[] exportData(@RequestBody NcGlueGroupOrder queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入信息
     */
    @PostMapping("/nc/glueGroupOrder/importData")
    AjaxResult importData(@RequestBody ImportContext importContext,
            @RequestParam("updateSupport") boolean updateSupport);
}
