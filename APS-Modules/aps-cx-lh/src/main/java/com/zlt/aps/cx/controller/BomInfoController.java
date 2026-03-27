package com.zlt.aps.cx.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.zlt.aps.cx.service.BomInfoService;
import com.zlt.aps.cxlh.cx.api.domain.entity.BomInfo;
import com.zlt.aps.cxlh.cx.api.domain.vo.BomInfoDto;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
