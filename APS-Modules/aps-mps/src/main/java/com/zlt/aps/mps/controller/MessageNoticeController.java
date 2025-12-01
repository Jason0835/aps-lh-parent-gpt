package com.zlt.aps.mps.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.mps.service.INoticeService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * 通知接口
 * @author zlt
 *
 */
@Api(tags = "消息通知接口")
@RestController
@RequestMapping("/messageNotice")
public class MessageNoticeController {
    @Autowired
    private INoticeService iNoticeService;

    @ApiOperation("未完成生产结果通知")
    @PostMapping("/unfinishedSchedule")
    public AjaxResult unfinishedSchedule() {
        return iNoticeService.unfinishedSchedule();
    }
}
