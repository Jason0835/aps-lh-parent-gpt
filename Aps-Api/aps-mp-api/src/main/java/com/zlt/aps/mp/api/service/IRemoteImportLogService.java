package com.zlt.aps.mp.api.service;

import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.common.core.web.domain.AjaxResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;



@FeignClient(contextId = "IRemoteImportLogService", name = "${remoteApi.value.system:ruoyi-system}")
public interface IRemoteImportLogService {

    @PostMapping({"/importLog/edit"})
    AjaxResult edit(@RequestBody ImportLog var1);
}