package com.zlt.aps.controller.cd90;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd90.api.service.ICd90CommonRemoteService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

/**
 * 直裁共用基础数据 UI 控制层。
 */
@Api(tags = "直裁共用基础数据")
@Controller
@RequestMapping("/cd90/common")
public class Cd90CommonUIController {

    @Resource
    private ICd90CommonRemoteService cd90CommonRemoteService;

    /**
     * 胎体布代号下拉。
     *
     * @return 去重后的胎体布代号列表
     */
    @ApiOperation("胎体布代号下拉")
    @PostMapping("/tireFabricCodes")
    @ResponseBody
    public AjaxResult tireFabricCodes() {
        return cd90CommonRemoteService.tireFabricCodes();
    }

    /**
     * 大卷代码（CORD_SPEC）下拉。
     *
     * @return 去重后的大卷代码列表
     */
    @ApiOperation("大卷代码下拉")
    @PostMapping("/cordSpecs")
    @ResponseBody
    public AjaxResult cordSpecs() {
        return cd90CommonRemoteService.cordSpecs();
    }
}
