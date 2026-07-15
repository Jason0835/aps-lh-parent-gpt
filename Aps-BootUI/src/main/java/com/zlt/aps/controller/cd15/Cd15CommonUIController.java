package com.zlt.aps.controller.cd15;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd15.api.service.ICd15CommonRemoteService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

/**
 * 斜裁公用基础数据 UI 控制层。
 */
@Api(tags = "斜裁公用基础数据")
@Controller
@RequestMapping("/cd15/common")
public class Cd15CommonUIController {

    @Resource
    private ICd15CommonRemoteService cd15CommonRemoteService;

    /**
     * 钢带代码下拉。
     *
     * @return 去重后的钢带代码列表
     */
    @ApiOperation("钢带代码下拉")
    @PostMapping("/steelStripCodes")
    @ResponseBody
    public AjaxResult steelStripCodes() {
        return cd15CommonRemoteService.steelStripCodes();
    }

    /**
     * 钢压大卷规格下拉（ARTICLE_CROWN_SPEC）。
     *
     * @return 去重后的钢压大卷规格列表
     */
    @ApiOperation("钢压大卷规格下拉")
    @PostMapping("/articleCrownSpecs")
    @ResponseBody
    public AjaxResult articleCrownSpecs() {
        return cd15CommonRemoteService.articleCrownSpecs();
    }
}