package com.zlt.aps.common.core.domain;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.Date;

/**
 * entity基类
 */
public class ApsBaseDto implements Serializable {

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
    public void setBaseVale(Long id) {
        if(id == null) {
            //id为空，表示为新增操作
            this.setDelFlag(ApsConstant.DEL_FLAG_NORMAL);
            this.setCreateBy(SecurityUtils.getUsername());
            this.setCreateTime(new Date());
        } else {
            //更新操作
            this.setUpdateBy(SecurityUtils.getUsername());
            this.setUpdateTime(new Date());
        }
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(String delFlag) {
        this.delFlag = delFlag;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getOrderStr() {
        return orderStr;
    }

    public void setOrderStr(String orderStr) {
        this.orderStr = orderStr;
    }
}
