package com.ruoyi.api.gateway.system.service;

import com.ruoyi.api.gateway.system.domain.SysLogininfor;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 系统访问记录对外暴露接口
 */
@FeignClient(contextId = "iSysLogininforService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.system:system}")
public interface ISysLogininforService {

    @PostMapping("/logininfor/postList")
    TableDataInfo list(@RequestBody SysLogininfor logininfor);


    @GetMapping("/logininfor/totalList")
    List<SysLogininfor> totalList(@SpringQueryMap SysLogininfor role);

    /*@PostMapping("/logininfor/export")
    void export(HttpServletResponse response, SysLogininfor logininfor) throws IOException;*/

    @DeleteMapping("/logininfor/{infoIds}")
    AjaxResult remove(@PathVariable("infoIds") Long[] infoIds);

    @DeleteMapping("/logininfor/clean")
    AjaxResult clean();

    @PostMapping("/logininfor")
    AjaxResult add(@RequestParam("username") String username, @RequestParam("status") String status,
                          @RequestParam("message") String message);
}
