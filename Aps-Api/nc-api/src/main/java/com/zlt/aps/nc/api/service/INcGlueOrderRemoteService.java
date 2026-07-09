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
import com.zlt.aps.nc.api.domain.dto.NcGlueOrderDto;
import com.zlt.aps.nc.api.domain.entity.NcGlueOrder;

/**
 * 内衬胶料顺序对外暴露接口
 */
@FeignClient(contextId = "INcGlueOrderRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.nc:nc}")
public interface INcGlueOrderRemoteService {

    /**
     * 根据条件查询胶料顺序列表
     */
    @PostMapping("/nc/glueOrder/list")
    TableDataInfo list(@RequestBody NcGlueOrder glueOrder);

    /**
     * 保存胶料顺序信息（id为空则新增，id不为空则修改）
     */
    @PostMapping("/nc/glueOrder/save")
    AjaxResult save(@RequestBody NcGlueOrder glueOrder);

    /**
     * 根据id查询胶料顺序信息
     */
    @GetMapping(value = "/nc/glueOrder/{id}")
    AjaxResult getInfo(@PathVariable("id") Long id);

    /**
     * 根据code判断胶料是否已经存在
     */
    @PostMapping("/nc/glueOrder/checkGlueCodeUnique")
    String checkGlueCodeUnique(@RequestBody NcGlueOrderDto dto);

    /**
     * 校验唯一性
     */
    @PostMapping("/nc/glueOrder/checkUnique")
    String checkUnique(@RequestBody NcGlueOrder glueOrder);

    /**
     * 批量删除胶料顺序信息(逻辑删)
     */
    @PostMapping("/nc/glueOrder/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 导出接口
     */
    @PostMapping("/nc/glueOrder/exportData/{fileName}")
    byte[] exportData(@RequestBody NcGlueOrder queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入信息
     */
    @PostMapping("/nc/glueOrder/importData")
    AjaxResult importData(@RequestBody ImportContext importContext,
            @RequestParam("updateSupport") boolean updateSupport);
}
