package com.zlt.aps.dj.api.service;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.dj.api.domain.dto.DjGlueOrderDto;
import com.zlt.aps.dj.api.domain.entity.DjGlueOrder;

/**
 * 垫胶胶料顺序对外暴露接口
 */
@FeignClient(contextId = "IDjGlueOrderRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.dj:dj}")
public interface IDjGlueOrderRemoteService {

    /**
     * 根据条件查询胶料顺序列表
     */
    @PostMapping("/dj/glueOrder/list")
    TableDataInfo list(@RequestBody DjGlueOrder glueOrder);

    /**
     * 保存胶料顺序信息（id为空则新增，id不为空则修改）
     */
    @PostMapping("/dj/glueOrder/save")
    AjaxResult save(@RequestBody DjGlueOrder glueOrder);

    /**
     * 根据id查询胶料顺序信息
     */
    @GetMapping(value = "/dj/glueOrder/{id}")
    AjaxResult getInfo(@PathVariable("id") Long id);

    /**
     * 根据code判断胶料是否已经存在
     */
    @PostMapping("/dj/glueOrder/checkGlueCodeUnique")
    String checkGlueCodeUnique(@RequestBody DjGlueOrderDto dto);

    /**
     * 校验唯一性
     */
    @PostMapping("/dj/glueOrder/checkUnique")
    String checkUnique(@RequestBody DjGlueOrder glueOrder);

    /**
     * 批量删除胶料顺序信息(逻辑删)
     */
    @PostMapping("/dj/glueOrder/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 导出接口
     */
    @PostMapping("/dj/glueOrder/exportData/{fileName}")
    byte[] exportData(@RequestBody DjGlueOrder queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入信息
     */
    @PostMapping("/dj/glueOrder/importData")
    AjaxResult importData(@RequestBody ImportContext importContext,
            @RequestParam("updateSupport") boolean updateSupport);
}
