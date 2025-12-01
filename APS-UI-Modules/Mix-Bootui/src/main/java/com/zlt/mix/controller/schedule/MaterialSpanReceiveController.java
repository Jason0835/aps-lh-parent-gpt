package com.zlt.mix.controller.schedule;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.annotation.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import com.zlt.mix.schedule.api.domain.entity.MaterialSpanReceive;
import com.zlt.mix.schedule.api.service.IMaterialSpanReceiveService;

/**
 * 硫磺辅料跨区接收Controller
 * @author cxy
 * @date 2022-08-30
 */
@Api(tags = "硫磺辅料跨区接收")
@Controller
@RequestMapping("/schedule/materialSpanReceive")
public class MaterialSpanReceiveController extends BaseController {

    @Resource
    private IMaterialSpanReceiveService iMaterialSpanReceiveService;

    /**
     * 根据排程日期、被委托密炼区查询未被接收的跨区请求总数
     */
    @ApiOperation("根据排程日期、被委托密炼区查询未被接收的跨区请求总数")
    @PostMapping("/selectUnReceiveCount")
    @ResponseBody
    public AjaxResult selectUnReceiveCount(MaterialSpanReceive materialSpanReceive) {
        return AjaxResult.success(iMaterialSpanReceiveService.selectUnReceiveCount(materialSpanReceive));
    }

}
