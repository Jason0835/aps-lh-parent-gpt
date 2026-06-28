package com.zlt.aps.tm.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tm.api.domain.entity.TmDepthConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 备库班数配置对外暴露接口
 *
 * @author zlt
 */
@FeignClient(contextId = "ITmDepthConfigRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tm:/tm}")
public interface ITmDepthConfigRemoteService {

    /**
     * 根据条件查询列表
     */
    @PostMapping("/depthConfig/list")
    TableDataInfo list(@RequestBody TmDepthConfig depthConfig);

    /**
     * 保存（id为空则新增，id不为空则修改）
     */
    @PostMapping("/depthConfig/save")
    AjaxResult save(@RequestBody TmDepthConfig depthConfig);

    /**
     * 根据id查询
     */
    @GetMapping(value = "/depthConfig/{id}")
    AjaxResult getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @PostMapping("/depthConfig/checkUnique")
    String checkUnique(@RequestBody TmDepthConfig depthConfig);

    /**
     * 校验配置规则交叉（确保新增/修改的规则不与现有规则有范围交叉）
     */
    @PostMapping("/depthConfig/checkRangeCross")
    String checkRangeCross(@RequestBody TmDepthConfig depthConfig);

    /**
     * 批量删除(逻辑删)
     */
    @PostMapping("/depthConfig/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 导出
     */
    @PostMapping("/depthConfig/exportData/{fileName}")
    byte[] exportData(@RequestBody TmDepthConfig queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入
     */
    @PostMapping("/depthConfig/importData")
    AjaxResult importData(@RequestBody ImportContext importContext,
            @RequestParam("updateSupport") boolean updateSupport);
}
