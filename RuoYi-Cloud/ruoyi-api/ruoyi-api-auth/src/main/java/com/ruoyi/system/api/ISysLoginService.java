package com.ruoyi.system.api;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.system.api.domain.LangVo;
import com.ruoyi.system.api.form.LoginBody;
import com.ruoyi.api.gateway.system.model.LoginUser;
import com.zlt.mdm.auth.api.domain.MdmSystemData;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(contextId = "iSysLoginService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.auth:auth}")
public interface ISysLoginService {

    @PostMapping("/login")
    R<?> login(@RequestBody LoginBody form);

    /**
     * 单机主体-退出操作
     * @return
     */
    @DeleteMapping("/logout")
    R<?> logout();

    /**
     * 刷新
     * @return
     */
    @PostMapping("/refresh")
    R<?> refresh();

    /**
     * 切换国际化语言
     * @param lang
     * @return
     */
    @GetMapping("/changeLang/{lang}")
    R<?> changeLang(@NonNull @PathVariable("lang") String lang);

    /**
     * 取得登录的用户数据
     *
     * @return
     */
    @GetMapping("/getLoginUser")
    R<LoginUser> geteUser();

    /***
     * 获取当前用户的语言设定
     * @return LangVo包含时区和语言
     */
    @GetMapping("/getLang")
    R<LangVo> getUserLang();

    /***
     * 取得现有系统所有的连接配置
     * @return
     */
    @PostMapping("systemList")
    R<List<MdmSystemData>> getSystemList();


}
