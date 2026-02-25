package com.zlt.aps.mp.api.service;

import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(contextId = "IRemoteImportErrorLogService", name = "${remoteApi.value.system:ruoyi-system}")
public interface IRemoteImportErrorLogService {
    
    @PostMapping({"/importErrorLog/insertImportErrorLogList"})
    int insertImportErrorLogList(@RequestBody List<ImportErrorLog> var1);
}
