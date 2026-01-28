package com.zlt.aps.monthplan.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.monthplan.api.domain.entity.MdmMsgTemplateUserRel;
import org.springframework.cloud.openfeign.FeignClient;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.core.web.domain.AjaxResult;



/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMdmMsgTemplateUserRelRemoteService.java
 * 描    述：IMdmMsgTemplateUserRelRemoteService消息模板关联用户前端接口
 *@author hc
 *@date 2026-01-28
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：hc
 *     修改内容：...
 */
@FeignClient(contextId = "IMdmMsgTemplateUserRelRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMdmMsgTemplateUserRelRemoteService {

    /**
     * 查询消息模板关联用户列表
     */
    @ApiOperation("查询消息模板关联用户列表")
    @PostMapping("/msgTemplateUserRel/list")
    TableDataInfo list(@RequestBody MdmMsgTemplateUserRel mdmMsgTemplateUserRel);

    /**
     * 消息模板绑定用户
     */
    @ApiOperation("消息模板绑定用户")
    @PostMapping("/msgTemplateUserRel/bindUsers")
    AjaxResult bindUsers(@RequestBody MdmMsgTemplateUserRel mdmMsgTemplateUserRel);

    /**
    * 新增消息模板关联用户
    */
    @ApiOperation("新增消息模板关联用户")
    @PostMapping("/msgTemplateUserRel/add")
    AjaxResult add(@RequestBody MdmMsgTemplateUserRel mdmMsgTemplateUserRel);

    /**
     * 修改消息模板关联用户
     */
    @ApiOperation("修改消息模板关联用户")
    @PostMapping("/msgTemplateUserRel/edit")
    AjaxResult edit(@RequestBody MdmMsgTemplateUserRel mdmMsgTemplateUserRel);

    /**
     * 删除消息模板关联用户
     */
    @ApiOperation("删除消息模板关联用户")
    @DeleteMapping("/msgTemplateUserRel/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/msgTemplateUserRel/{id}")
    MdmMsgTemplateUserRel getInfo(@PathVariable("id") Long id);

    /**
     * 校验消息模板关联用户唯一性
     */
    @ApiOperation("校验消息模板关联用户唯一性")
    @PostMapping("/msgTemplateUserRel/checkMdmMsgTemplateUserRelUnique")
    String checkMdmMsgTemplateUserRelUnique(@RequestBody MdmMsgTemplateUserRel mdmMsgTemplateUserRel);

    /**
     * 导出消息模板关联用户列表
    */
    @ApiOperation("导出消息模板关联用户列表")
    @PostMapping("/msgTemplateUserRel/exportData/{fileName}")
    byte[] exportData(@RequestBody MdmMsgTemplateUserRel mdmMsgTemplateUserRel,@PathVariable("fileName") String fileName);

    /**
     * 导入消息模板关联用户数据
     */
    @ApiOperation("导入消息模板关联用户")
    @PostMapping("/msgTemplateUserRel/importData")
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
