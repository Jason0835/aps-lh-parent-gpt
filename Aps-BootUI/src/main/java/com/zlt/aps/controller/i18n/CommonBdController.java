package com.zlt.aps.controller.i18n;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.mp.api.domain.vo.I18nJsonVo;
import com.zlt.aps.mp.api.service.I18nChangeRemoteService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 根据shiro配置，在URL前增加/common，走非鉴权公用入口
 */
@Slf4j
@Api(tags = "非鉴权公用入口")
@Controller
@RequestMapping("/common/bd/i18nChange")
public class CommonBdController {

    @Autowired
    private I18nChangeRemoteService i18nChangeRemoteService;

    /**
     * 查询语言包JSON
     */
    @ApiOperation(value = "查询语言包JSON", notes = "查询语言包JSON")
    @PostMapping("/pageJson")
    @ResponseBody
    public AjaxResult pageJson(I18nJsonVo jsonVo) {
        return i18nChangeRemoteService.pageJson(jsonVo);
    }
}
