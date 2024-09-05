package com.zlt.aps.controller.cx;

import com.ruoyi.common.core.web.controller.BaseController;
import com.zlt.aps.cx.api.domain.dto.BomInfoDto;
import com.zlt.aps.cx.api.service.ICxBomInfoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * BOM信息Controller
 *
 * @author Chen
 * @date 2021-06-11
 */
@Controller
@RequestMapping("/cx/bom")
@Api(tags = "Bom信息维护接口")
public class BomInfoController extends BaseController {
    private final String prefix = "cx/bom";
    @Autowired
    private ICxBomInfoService iCxBomInfoService;

    @RequiresPermissions("cx:bom:view")
    @GetMapping()
    @ApiOperation("跳转到bom信息管理首页")
    public String toIndex() {

        return prefix + "/bom";
    }

    /**
     * 查询BOM信息列表
     */
    @RequiresPermissions("cx:bom:list")
    @PostMapping("/list")
    @ResponseBody
    @ApiOperation("查询bom信息维护列表")
    public List<BomInfoDto> list(BomInfoDto dto) {
        return iCxBomInfoService.list(dto);
    }


}
