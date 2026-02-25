package com.zlt.aps.mp.api.domain.entity;

import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Data;
import com.ruoyi.common.core.annotation.Excel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmMsgTemplateUserRel.java
 * 描    述：消息模板关联用户对象 t_mdm_msg_template_user_rel
 *@author hc
 *@date 2026-01-28
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：hc
 *     修改内容：...
 */

@ApiModel(value = "消息模板关联用户对象", description = "消息模板关联用户对象 ")
@Data
@TableName(value = "T_MDM_MSG_TEMPLATE_USER_REL")
public class MdmMsgTemplateUserRel extends BaseEntity {

    private static final long serialVersionUID = 1L;

     /** 消息模板编号 */
    @Excel(name = "ui.data.column.msgTemplateUserRel.templateCode")
    @ApiModelProperty(value = "消息模板编号", name = "templateCode")
    @TableField(value = "TEMPLATE_CODE")
    private String templateCode;

    /** 用户名 */
    @Excel(name = "ui.data.column.msgTemplateUserRel.userName")
    @ApiModelProperty(value = "用户名", name = "userName")
    @TableField(value = "USER_NAME")
    private String userName;


}