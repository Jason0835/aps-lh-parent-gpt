package com.zlt.mix.schedule.controller;

import javax.annotation.Resource;

import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.*;
import com.zlt.mix.schedule.api.domain.entity.GlueSpanReceive;
import com.zlt.mix.schedule.service.GlueSpanReceiveService;
import com.ruoyi.common.core.web.controller.BaseController;
import io.swagger.annotations.ApiOperation;

/**
 * 胶料跨区接收Controller
 *
 * @author chen
 * @date 2022-08-16
 */
@Api(tags = "胶料跨区接收后端接口")
@RestController
@RequestMapping("/glueSpanReceive")
public class GlueSpanReceiveController extends BaseController {
    @Resource
    private GlueSpanReceiveService glueSpanReceiveService;

    /**
     * 根据排程日期、被委托密炼区查询未被接收的跨区请求总数
     */
    @ApiOperation("根据排程日期、被委托密炼区查询未被接收的跨区请求总数")
    @PostMapping("/selectUnReceiveCount")
    public Integer selectUnReceiveCount(@RequestBody GlueSpanReceive glueSpanReceive) {
        return glueSpanReceiveService.selectUnReceiveCount(glueSpanReceive);
    }

    /**
     * 根据id查询跨区接收信息
     * @param entity id
     * @return 查询到的记录
     */
    @ApiOperation("根据id查询跨区接收信息")
    @PostMapping("/getGlueSpanReceiveInfo")
    public GlueSpanReceive getGlueSpanReceiveInfo(@RequestBody GlueSpanReceive entity) {
        return glueSpanReceiveService.getGlueSpanReceiveInfo(entity);
    }
}
