package com.ruoyi.system.api.factory;

import com.ruoyi.api.gateway.system.domain.SysFile;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.system.api.RemoteFileService;
import feign.hystrix.FallbackFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务降级处理
 * 
 * @author ruoyi
 */
@Component
public class RemoteFileFallbackFactory implements FallbackFactory<RemoteFileService>
{
    private static final Logger log = LoggerFactory.getLogger(RemoteFileFallbackFactory.class);

    @Override
    public RemoteFileService create(Throwable throwable)
    {
        log.error(I18nUtil.getMessage("file.error.file.upload.invoke.fail"), throwable.getMessage());
        return new RemoteFileService()
        {
            @Override
            public R<SysFile> upload(MultipartFile file)
            {
                return R.fail(I18nUtil.getMessage("file.error.file.upload.fail") + throwable.getMessage());
            }
        };
    }
}
