package com.ruoyi.api.gateway.system.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.api.gateway.system.domain.SysDictData;
import com.ruoyi.common.core.web.domain.AjaxResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 系统字典缓存控制接口
 */
@FeignClient(contextId = "iSysDictDataCacheService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.system:system}")
public interface ISysDictDataCacheService {

    @PostMapping("/dict/cache/getType")
    public List<SysDictData> getType(@RequestParam("dictType") String dictType);

    @PostMapping("/dict/cache/getLabel")
    public String getLabel(@RequestParam("dictType") String dictType, @RequestParam("dictValue") String dictValue);

    @GetMapping("/dict/cache/cleanCache")
    public AjaxResult cleanCache();

    @GetMapping("/dict/cache/reloadCache")
    public AjaxResult reloadCache();
}
