package com.zlt.mix.controller.schedule;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.mix.schedule.api.domain.entity.GlueSpanReceive;
import com.zlt.mix.schedule.api.service.IGlueSpanReceiveService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

/**
 * 胶料跨区接收
 * @author: Chen
 * @since: 2022/8/18 13:34
 */
@Api(tags = "胶料跨区接收")
@Controller
@RequestMapping("/schedule/glueSpanReceive")
public class GlueSpanReceiveController {

    @Resource
    private IGlueSpanReceiveService iGlueSpanReceiveService;

    /**
     * 根据排程日期、被委托密炼区查询未被接收的跨区请求总数
     */
    @ApiOperation("根据排程日期、被委托密炼区查询未被接收的跨区请求总数")
    @PostMapping("/selectUnReceiveCount")
    @ResponseBody
    public AjaxResult selectUnReceiveCount(GlueSpanReceive glueSpanReceive) {
        return AjaxResult.success(iGlueSpanReceiveService.selectUnReceiveCount(glueSpanReceive));
    }
}
