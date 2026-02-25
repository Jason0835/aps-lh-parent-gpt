package com.zlt.aps.mp.api.domain.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@ApiModel(value = "国际化变更表", description = "国际化变更表")
@Data
@TableName(value = "T_I18N_RELATION")
public class I18nRelation implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID", name = "id")
    @TableId(value = "ID", type = IdType.INPUT)
    private Long id;

    @ApiModelProperty(value = "所属模块国际化", name = "modeName")
    @TableField(value = "MODE_NAME")
    private String modeName;

    @ApiModelProperty(value = "所属文件名", name = "fileName")
    @TableField(value = "FILE_NAME")
    private String fileName;

    @ApiModelProperty(value = "是否页面加载（0：否 1：是）", name = "isPage")
    @TableField(value = "IS_PAGE")
    private Integer isPage;

}
