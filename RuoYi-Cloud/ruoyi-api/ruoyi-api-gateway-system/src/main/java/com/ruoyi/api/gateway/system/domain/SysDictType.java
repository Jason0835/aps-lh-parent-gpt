package com.ruoyi.api.gateway.system.domain;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.annotation.Excel.ColumnType;
import com.ruoyi.common.core.web.domain.BaseEntity;

/**
 * 字典类型表 sys_dict_type
 * 
 * @author ruoyi
 */
public class SysDictType extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 字典主键 */
    @Excel(name = "api.dictType.columnname.orderno", cellType = ColumnType.NUMERIC)
    private Long dictId;

    /** 字典名称 */
    @Excel(name = "api.dictType.columnname.name")
    private String dictName;

    /** 字典类型 */
    @Excel(name = "api.dictType.columnname.dictType")
    private String dictType;

    /** 状态（0正常 1停用） */
    @Excel(name = "api.dictType.columnname.status", readConverterExp = "api.dictType.status.desc")
    private String status;

    public Long getDictId()
    {
        return dictId;
    }

    public void setDictId(Long dictId)
    {
        this.dictId = dictId;
    }

    @NotBlank(message = "api.dictType.error.name.isnull")
    @Size(min = 0, max = 100, message = "api.dictType.error.name.overlength")
    public String getDictName()
    {
        return dictName;
    }

    public void setDictName(String dictName)
    {
        this.dictName = dictName;
    }

    @NotBlank(message = "api.dictType.error.dictType.isnull")
    @Size(min = 0, max = 100, message = "api.dictType.error.dictType.overlength")
    public String getDictType()
    {
        return dictType;
    }

    public void setDictType(String dictType)
    {
        this.dictType = dictType;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }
    
    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("dictId", getDictId())
            .append("dictName", getDictName())
            .append("dictType", getDictType())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
