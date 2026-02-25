package com.zlt.aps.mp.mdm.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.zlt.aps.maindata.service.IMdmMsgTemplateUserRelService;
import com.zlt.aps.mp.api.domain.entity.MdmMsgTemplateUserRel;
import lombok.extern.slf4j.Slf4j;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import com.ruoyi.common.core.web.page.TableDataInfo;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmMsgTemplateUserRelController.java
 * 描    述：消息模板关联用户 控制层类：....
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
@RestController
@RequestMapping("/msgTemplateUserRel")
public class MdmMsgTemplateUserRelController extends BaseController<MdmMsgTemplateUserRel> {
    @Autowired
    private IMdmMsgTemplateUserRelService mdmMsgTemplateUserRelService;

    /**
     * 查询消息模板关联用户列表
     */
    @ApiOperation("查询消息模板关联用户列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody MdmMsgTemplateUserRel mdmMsgTemplateUserRel) {
        startPage("create_time desc");
        List<MdmMsgTemplateUserRel> list = mdmMsgTemplateUserRelService.selectMdmMsgTemplateUserRelList(mdmMsgTemplateUserRel);
        return getDataTable(list);
    }

    /**
     * 消息模板绑定用户
     */
    @ApiOperation("消息模板绑定用户")
    @PostMapping("/bindUsers")
    AjaxResult bindUsers(@RequestBody MdmMsgTemplateUserRel mdmMsgTemplateUserRel) {
        return toAjax(mdmMsgTemplateUserRelService.bindUsers(mdmMsgTemplateUserRel));
    }

    @PostMapping("/batchGetAssociatedUsers")
    public Map<String, String> batchGetAssociatedUsers(@RequestBody List<String> templateCodes) {
        return  mdmMsgTemplateUserRelService.batchGetAssociatedUsers(templateCodes);
    }


    /**
     * 获取消息模板关联用户详细信息
     */
    @ApiOperation("获取消息模板关联用户详细信息")
    @GetMapping(value = "/{id}")
    public MdmMsgTemplateUserRel getInfo(@PathVariable("id") Long id) {
        return mdmMsgTemplateUserRelService.selectMdmMsgTemplateUserRelById(id);
    }

    /**
     * 新增消息模板关联用户
     */
    @Log(title = "ui.data.column.msgTemplateUserRel.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增消息模板关联用户")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody MdmMsgTemplateUserRel mdmMsgTemplateUserRel) {
        return toAjax(mdmMsgTemplateUserRelService.insertMdmMsgTemplateUserRel(mdmMsgTemplateUserRel));
    }

    /**
     * 修改消息模板关联用户
     */
    @Log(title = "ui.data.column.msgTemplateUserRel.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改消息模板关联用户")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody MdmMsgTemplateUserRel mdmMsgTemplateUserRel) {
        return toAjax(mdmMsgTemplateUserRelService.updateMdmMsgTemplateUserRel(mdmMsgTemplateUserRel));
    }

    /**
     * 删除消息模板关联用户
     */
    @Log(title = "ui.data.column.msgTemplateUserRel.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除消息模板关联用户")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(mdmMsgTemplateUserRelService.deleteMdmMsgTemplateUserRelByIds(ids));
    }

    /**
     * 校验消息模板关联用户唯一性
     */
    @ApiOperation("校验消息模板关联用户唯一性")
    @PostMapping("/checkMdmMsgTemplateUserRelUnique")
    public String checkMdmMsgTemplateUserRelUnique(@RequestBody MdmMsgTemplateUserRel mdmMsgTemplateUserRel) {
        return mdmMsgTemplateUserRelService.checkMdmMsgTemplateUserRelUnique(mdmMsgTemplateUserRel);
    }
}
