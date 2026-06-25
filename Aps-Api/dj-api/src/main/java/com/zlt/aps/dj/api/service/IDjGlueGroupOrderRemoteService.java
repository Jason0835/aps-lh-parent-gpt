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
import com.zlt.aps.dj.api.domain.dto.DjGlueGroupOrderDto;
import com.zlt.aps.dj.api.domain.entity.DjGlueGroupOrder;

/**
 * 垫胶胶料组别顺序对外暴露接口
 */
@FeignClient(contextId = "IDjGlueGroupOrderRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.dj:dj}")
public interface IDjGlueGroupOrderRemoteService {

    /**
     * 根据条件查询胶料组别顺序列表
     */
    @PostMapping("/dj/glueGroupOrder/list")
    TableDataInfo list(@RequestBody DjGlueGroupOrder glueGroupOrder);

    /**
     * 保存胶料组别顺序信息（id为空则新增，id不为空则修改）
     */
    @PostMapping("/dj/glueGroupOrder/save")
    AjaxResult save(@RequestBody DjGlueGroupOrder glueGroupOrder);

    /**
     * 根据id查询胶料组别顺序信息
     */
    @GetMapping(value = "/dj/glueGroupOrder/{id}")
    AjaxResult getInfo(@PathVariable("id") Long id);

    /**
     * 根据code判断胶料组号是否已经存在
     */
    @PostMapping("/dj/glueGroupOrder/checkGlueGroupCodeUnique")
    String checkGlueGroupCodeUnique(@RequestBody DjGlueGroupOrderDto dto);

    /**
     * 校验唯一性
     */
    @PostMapping("/dj/glueGroupOrder/checkUnique")
    String checkUnique(@RequestBody DjGlueGroupOrder glueGroupOrder);

    /**
     * 批量删除胶料组别顺序信息(逻辑删)
     */
    @PostMapping("/dj/glueGroupOrder/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 导出接口
     */
    @PostMapping("/dj/glueGroupOrder/exportData/{fileName}")
    byte[] exportData(@RequestBody DjGlueGroupOrder queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入信息
     */
    @PostMapping("/dj/glueGroupOrder/importData")
    AjaxResult importData(@RequestBody ImportContext importContext,
            @RequestParam("updateSupport") boolean updateSupport);
}
