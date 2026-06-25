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
import com.zlt.aps.dj.api.domain.entity.DjDepthConfig;

/**
 * 垫胶备库班数与供成型机数配置对外暴露接口
 *
 * @author zlt
 */
@FeignClient(contextId = "IDjDepthConfigRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.dj:dj}")
public interface IDjDepthConfigRemoteService {

    /**
     * 根据条件查询列表
     */
    @PostMapping("/dj/depthConfig/list")
    TableDataInfo list(@RequestBody DjDepthConfig depthConfig);

    /**
     * 保存（id为空则新增，id不为空则修改）
     */
    @PostMapping("/dj/depthConfig/save")
    AjaxResult save(@RequestBody DjDepthConfig depthConfig);

    /**
     * 根据id查询
     */
    @GetMapping(value = "/dj/depthConfig/{id}")
    AjaxResult getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @PostMapping("/dj/depthConfig/checkUnique")
    String checkUnique(@RequestBody DjDepthConfig depthConfig);

    /**
     * 校验配置规则交叉（确保新增/修改的规则不与现有规则有范围交叉）
     */
    @PostMapping("/dj/depthConfig/checkRangeCross")
    String checkRangeCross(@RequestBody DjDepthConfig depthConfig);

    /**
     * 批量删除(逻辑删)
     */
    @DeleteMapping("/dj/depthConfig/{ids}")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 导出
     */
    @PostMapping("/dj/depthConfig/exportData/{fileName}")
    byte[] exportData(@RequestBody DjDepthConfig queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入
     */
    @PostMapping("/dj/depthConfig/importData")
    AjaxResult importData(@RequestBody ImportContext importContext,
            @RequestParam("updateSupport") boolean updateSupport);
}
