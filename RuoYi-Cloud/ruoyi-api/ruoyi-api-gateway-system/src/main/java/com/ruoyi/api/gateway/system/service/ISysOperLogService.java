package com.ruoyi.api.gateway.system.service;

import com.ruoyi.api.gateway.system.domain.SysOperLog;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(contextId = "iSysOperLogService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.system:system}")
public interface ISysOperLogService {

    @PostMapping("/operlog/postList")
    TableDataInfo list(@RequestBody SysOperLog operLog);


    @GetMapping("/operlog/totalList")
    List<SysOperLog> totalList(@SpringQueryMap SysOperLog role);

    /*@PostMapping("/operLog/export")
    void export(HttpServletResponse response, SysOperLog operLog) throws IOException;*/

    @DeleteMapping("/operlog/{operIds}")
    AjaxResult remove(@PathVariable("operIds") Long[] operIds);

    @DeleteMapping("/operlog/clean")
    AjaxResult clean();

    @PostMapping("/operlog")
    AjaxResult add(@RequestBody SysOperLog operLog);

    @PostMapping("/operlog/selectOperLogById")
    SysOperLog selectOperLogById(@RequestParam("operId") Long operId);
}
