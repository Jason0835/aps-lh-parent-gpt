package com.zlt.mix.schedule.controller;

import javax.annotation.Resource;

import org.springframework.web.bind.annotation.*;
import com.zlt.mix.schedule.api.domain.entity.MaterialSpanReceive;
import com.zlt.mix.schedule.service.MaterialSpanReceiveService;
import com.ruoyi.common.core.web.controller.BaseController;
import io.swagger.annotations.ApiOperation;

/**
 * 硫磺辅料跨区接收Controller
 *
 * @author cxy
 * @date 2022-08-30
 */
@RestController
@RequestMapping("/materialSpanReceive")
public class MaterialSpanReceiveController extends BaseController {
    @Resource
    private MaterialSpanReceiveService materialSpanReceiveService;

    /**
     * 根据排程日期、被委托密炼区查询未被接收的跨区请求总数
     */
    @ApiOperation("根据排程日期、被委托密炼区查询未被接收的跨区请求总数")
    @PostMapping("/selectUnReceiveCount")
    public Integer selectUnReceiveCount(@RequestBody MaterialSpanReceive materialSpanReceive) {
        return materialSpanReceiveService.selectUnReceiveCount(materialSpanReceive);
    }

    /**
     * 根据id查询跨区接收信息
     *
     * @param entity id
     * @return 查询到的记录
     */
    @ApiOperation("根据id查询跨区接收信息")
    @PostMapping("/getMaterialSpanReceiveInfo")
    public MaterialSpanReceive getMaterialSpanReceiveInfo(@RequestBody MaterialSpanReceive entity) {
        return materialSpanReceiveService.getMaterialSpanReceiveInfo(entity);
    }
}
