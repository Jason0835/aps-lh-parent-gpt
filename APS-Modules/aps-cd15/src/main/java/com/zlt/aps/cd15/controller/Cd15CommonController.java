package com.zlt.aps.cd15.controller;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.maindata.service.IMdmConstructionInfoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 斜裁公用基础数据控制层。
 */
@Api(tags = "斜裁公用基础数据")
@RestController
@RequestMapping("/common")
public class Cd15CommonController {

    @Resource
    private IMdmConstructionInfoService mdmConstructionInfoService;

    /**
     * 钢带代码下拉。
     *
     * @return 去重后的钢带代码列表
     */
    @ApiOperation("钢带代码下拉")
    @PostMapping("/steelStripCodes")
    public AjaxResult steelStripCodes() {
        return AjaxResult.success(mdmConstructionInfoService.listSteelStripCodes());
    }

    /**
     * 钢压大卷规格下拉（ARTICLE_CROWN_SPEC）。
     *
     * @return 去重后的钢压大卷规格列表
     */
    @ApiOperation("钢压大卷规格下拉")
    @PostMapping("/articleCrownSpecs")
    public AjaxResult articleCrownSpecs() {
        return AjaxResult.success(mdmConstructionInfoService.listArticleCrownSpecs());
    }
}