package com.zlt.mix.common.core.domain;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.zlt.mix.common.core.constant.ZltConstant;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * entity基类
 */
@Data
public class ZltBaseDto implements Serializable {

    @ApiModelProperty(value = "备注", position = 500)
    private String remark;

    @ApiModelProperty(value = "删除标识：0--正常，1-删除", position = 600)
    private String delFlag;

    @ApiModelProperty(value = "创建者", position = 700)
    private String createBy;

    @ApiModelProperty(value = "创建时间", position = 800)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone="GMT+8")
    private Date createTime;

    @ApiModelProperty(value = "更新者", position = 900)
    private String updateBy;

    @ApiModelProperty(value = "更新时间", position = 1000)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone="GMT+8")
    private Date updateTime;

    /**
     * 排序字段：字段名+排列方式
     */
    private String orderStr;

    /**
     * 根据id是否为空给创建时间，创建人，更新时间，更新人赋值
     */
    public void setBaseValue(Long id) {
        if(id == null) {
            //id为空，表示为新增操作
            this.setDelFlag(ZltConstant.DEL_FLAG_NORMAL);
            this.setCreateBy(SecurityUtils.getUsername());
            this.setCreateTime(new Date());
        } else {
            //更新操作
            this.setUpdateBy(SecurityUtils.getUsername());
            this.setUpdateTime(new Date());
        }
    }

}
