package com.ruoyi.api.gateway.system.service;

import com.ruoyi.api.gateway.system.domain.SysNotice;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@FeignClient(contextId = "ISysNoticeService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.system:system}")
public interface ISysNoticeService {

    /**
     * 获取通知公告列表
     * @param notice
     * @return
     */
    @GetMapping("/notice/list")
    TableDataInfo list(@SpringQueryMap SysNotice notice);

    /**
     * 根据通知公告编号获取详细信息
     * @param noticeId
     * @return
     */
    @GetMapping(value = "/notice/{noticeId}")
    AjaxResult getInfo(@PathVariable("noticeId") Long noticeId);

    /**
     * 新增通知公告
     * @param notice
     * @return
     */
    @PostMapping("/notice")
    AjaxResult add(@Validated @RequestBody SysNotice notice);

    /**
     * 修改通知公告
     * @param notice
     * @return
     */
    @PutMapping("/notice")
    AjaxResult edit(@Validated @RequestBody SysNotice notice);

    /**
     * 删除通知公告
     * @param noticeIds
     * @return
     */
    @DeleteMapping("/notice/{noticeIds}")
    AjaxResult remove(@PathVariable("noticeIds") Long[] noticeIds);

    /**
     * 根据通知获取详情
     * @param noticeId
     * @return
     */
    @PostMapping("/notice/selectNoticeById")
    SysNotice selectNoticeById(@RequestParam("noticeId") Long noticeId);
}
