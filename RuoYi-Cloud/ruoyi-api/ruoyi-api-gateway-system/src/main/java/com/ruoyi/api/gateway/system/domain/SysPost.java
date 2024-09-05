package com.ruoyi.api.gateway.system.domain;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.annotation.Excel.ColumnType;
import com.ruoyi.common.core.web.domain.BaseEntity;

/**
 * 岗位表 sys_post
 * 
 * @author ruoyi
 */
public class SysPost extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 岗位序号 */
    @Excel(name = "api.syspost.columnname.orderno", cellType = ColumnType.NUMERIC)
    private Long postId;

    /** 岗位编码 */
    @Excel(name = "api.syspost.columnname.code")
    private String postCode;

    /** 岗位名称 */
    @Excel(name = "api.syspost.columnname.name")
    private String postName;

    /** 岗位排序 */
    @Excel(name = "api.syspost.columnname.sort")
    private String postSort;

    /** 状态（0正常 1停用） */
    @Excel(name = "api.syspost.columnname.status", readConverterExp = "api.syspost.status.desc")
    private String status;

    /** 用户是否存在此岗位标识 默认不存在 */
    private boolean flag = false;

    public Long getPostId()
    {
        return postId;
    }

    public void setPostId(Long postId)
    {
        this.postId = postId;
    }

    @NotBlank(message = "api.syspost.error.code.isnull")
    @Size(min = 0, max = 64, message = "api.syspost.error.code.overlength")
    public String getPostCode()
    {
        return postCode;
    }

    public void setPostCode(String postCode)
    {
        this.postCode = postCode;
    }

    @NotBlank(message = "api.syspost.error.name.isnull")
    @Size(min = 0, max = 50, message = "api.syspost.error.name.overlength")
    public String getPostName()
    {
        return postName;
    }

    public void setPostName(String postName)
    {
        this.postName = postName;
    }

    @NotBlank(message = "api.syspost.error.sort.isnull")
    public String getPostSort()
    {
        return postSort;
    }

    public void setPostSort(String postSort)
    {
        this.postSort = postSort;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public boolean isFlag()
    {
        return flag;
    }

    public void setFlag(boolean flag)
    {
        this.flag = flag;
    }
    
    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("postId", getPostId())
            .append("postCode", getPostCode())
            .append("postName", getPostName())
            .append("postSort", getPostSort())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
