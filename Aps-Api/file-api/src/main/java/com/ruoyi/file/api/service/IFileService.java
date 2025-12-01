package com.ruoyi.file.api.service;


import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.api.gateway.system.domain.SysFile;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author lbn
 */
@FeignClient(contextId = "iFileService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.file:file}")
public interface IFileService {

    @PostMapping("/upload")
    public R<SysFile> upload(MultipartFile file);
}
