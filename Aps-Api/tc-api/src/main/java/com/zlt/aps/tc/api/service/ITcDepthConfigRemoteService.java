package com.zlt.aps.tc.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tc.api.domain.entity.TcDepthConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 胎侧备库班数配置对外暴露接口
 *
 * @author zlt
 */
@FeignClient(contextId = "ITcDepthConfigRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tc:/tc}")
public interface ITcDepthConfigRemoteService {

    /**
     * 根据条件查询列表
     */
    @PostMapping("/depthConfig/list")
    TableDataInfo list(@RequestBody TcDepthConfig depthConfig);

    /**
     * 保存（id为空则新增，id不为空则修改）
     */
    @PostMapping("/depthConfig/save")
    AjaxResult save(@RequestBody TcDepthConfig depthConfig);

    /**
     * 根据id查询
     */
    @GetMapping(value = "/depthConfig/{id}")
    AjaxResult getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @PostMapping("/depthConfig/checkUnique")
    String checkUnique(@RequestBody TcDepthConfig depthConfig);

    /**
     * 校验配置区间的字段合法性、连续性和完整性
     */
    @PostMapping("/depthConfig/checkRangeCross")
    String checkRangeCross(@RequestBody TcDepthConfig depthConfig);

    /**
     * 批量删除(逻辑删)
     */
    @PostMapping("/depthConfig/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 导出
     */
    @PostMapping("/depthConfig/exportData/{fileName}")
    byte[] exportData(@RequestBody TcDepthConfig queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入
     */
    @PostMapping("/depthConfig/importData")
    AjaxResult importData(@RequestBody ImportContext importContext,
            @RequestParam("updateSupport") boolean updateSupport);
}
