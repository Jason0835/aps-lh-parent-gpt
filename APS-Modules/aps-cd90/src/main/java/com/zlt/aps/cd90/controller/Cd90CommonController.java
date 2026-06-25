package com.zlt.aps.cd90.controller;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.maindata.service.IMdmConstructionInfoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 直裁共用基础数据控制层。
 */
@Api(tags = "直裁共用基础数据")
@RestController
@RequestMapping("/common")
public class Cd90CommonController {

    @Resource
    private IMdmConstructionInfoService mdmConstructionInfoService;

    /**
     * 胎体布代号下拉。
     *
     * @return 去重后的胎体布代号列表
     */
    @ApiOperation("胎体布代号下拉")
    @PostMapping("/tireFabricCodes")
    public AjaxResult tireFabricCodes() {
        return AjaxResult.success(mdmConstructionInfoService.listTireFabricCodes());
    }

    /**
     * 大卷代码（CORD_SPEC）下拉。
     *
     * @return 去重后的大卷代码列表
     */
    @ApiOperation("大卷代码下拉")
    @PostMapping("/cordSpecs")
    public AjaxResult cordSpecs() {
        return AjaxResult.success(mdmConstructionInfoService.listCordSpecs());
    }
}
