package com.ruoyi.system.api.factory;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.system.api.RemoteUserService;
import com.ruoyi.api.gateway.system.model.LoginUser;
import feign.hystrix.FallbackFactory;

/**
 * 用户服务降级处理
 *
 * @author ruoyi
 */
@Component
public class RemoteUserFallbackFactory implements FallbackFactory<RemoteUserService> {
    private static final Logger log = LoggerFactory.getLogger(RemoteUserFallbackFactory.class);

    @Override
    public RemoteUserService create(Throwable throwable) {
        //log.error("用户服务调用失败:{}", throwable.getMessage());
        String errorMsg = StringUtils.format(I18nUtil.getMessage("api.factory.error.user.server.fail"), throwable.getMessage());
        log.error(errorMsg);
        return new RemoteUserService() {
            @Override
            public R<LoginUser> getUserInfo(String username) {
                //return R.fail("获取用户失败:" + throwable.getMessage());
                String failMsg = StringUtils.format(I18nUtil.getMessage("api.factory.error.get.user.fail"), throwable.getMessage());
                return R.fail(failMsg);
            }

            @Override
            public R<LoginUser> postUserInfo(String username) {
                String failMsg = StringUtils.format(I18nUtil.getMessage("api.factory.error.get.user.fail"), throwable.getMessage());
                return R.fail(failMsg);
            }

            @Override
            public AjaxResult cleanToken(String tokenId) {
                String failMsg = StringUtils.format(I18nUtil.getMessage("api.factory.error.cleancache.user.fail"), throwable.getMessage());
                return AjaxResult.error(failMsg);
            }

        };
    }
}
