package com.zlt.aps.cx.controller;

import com.ruoyi.api.gateway.system.domain.Ztree;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.PreAuthorize;
import com.zlt.aps.cx.api.domain.dto.BomInfoDto;
import com.zlt.aps.cx.entity.BomInfo;
import com.zlt.aps.cx.service.BomInfoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

/**
 * BOM信息Controller
 *
 * @author Chen
 * @date 2021-06-11
 */
@RestController
@RequestMapping("/cx/bom")
@Api(tags = "Bom信息维护接口")
public class BomInfoController extends BaseController {
    @Autowired
    private BomInfoService bomInfoService;

    /**
     * 查询BOM信息列表
     *
     * @return 查询到的bom信息列表
     */
    @ApiOperation("查询BOM信息列表")
    @PostMapping("/list")
    public List<BomInfoDto> list(@RequestBody BomInfoDto dto) {
        BomInfo bomInfo = new BomInfo();
        BeanUtils.copyProperties(dto, bomInfo);
        return bomInfoService.selectBomInfoList(bomInfo);
    }

}
