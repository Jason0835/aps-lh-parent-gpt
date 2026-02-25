package com.zlt.aps.controller.maindata;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.mp.api.domain.entity.MdmMsgTemplateUserRel;
import com.zlt.aps.mp.api.service.IMdmMsgTemplateUserRelRemoteService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmMsgTemplateUserRelUIController.java
 * 描    述：消息模板关联用户 UI控制层类：....
 *
 * @author hc
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：hc
 * 修改内容：...
 * @date 2026-01-28
 */
@Slf4j
@Api(tags = "消息模板关联用户")
@Controller
@RequestMapping("/maindata/msgTemplateUserRel")
public class MdmMsgTemplateUserRelUIController extends BaseUIController<MdmMsgTemplateUserRel> {

    @Autowired
    private IMdmMsgTemplateUserRelRemoteService iMdmMsgTemplateUserRelService;

    /**
     * 根据条件查询消息模板关联用户列表
     */
    @ApiOperation("根据条件查询消息模板关联用户列表")
//    @RequiresPermissions("maindata:msgTemplateUserRel:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(MdmMsgTemplateUserRel entity) {
        return iMdmMsgTemplateUserRelService.list(entity);
    }

    /**
     * 消息模板绑定用户
     */
    @ApiOperation("消息模板绑定用户")
    @RequiresPermissions("maindata:msgTemplateUserRel:bindUsers")
    @PostMapping("/bindUsers")
    @ResponseBody
    public AjaxResult bindUsers(MdmMsgTemplateUserRel entity) {
        return iMdmMsgTemplateUserRelService.bindUsers(entity);
    }
}
