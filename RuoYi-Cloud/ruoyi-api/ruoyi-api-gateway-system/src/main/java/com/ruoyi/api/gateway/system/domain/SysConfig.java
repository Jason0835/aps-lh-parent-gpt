package com.ruoyi.api.gateway.system.domain;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.annotation.Excel.ColumnType;
import com.ruoyi.common.core.web.domain.BaseEntity;

/**
 * 参数配置表 sys_config
 * 
 * @author ruoyi
 */
public class SysConfig extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 参数主键 */
    @Excel(name = "api.config.columnname.orderno", cellType = ColumnType.NUMERIC)
    private Long configId;

    /** 参数名称 */
    @Excel(name = "api.config.columnname.name")
    private String configName;

    /** 参数键名 */
    @Excel(name = "api.config.columnname.keyName")
    private String configKey;

    /** 参数键值 */
    @Excel(name = "api.config.columnname.value")
    private String configValue;

    /** 系统内置（Y是 N否） */
    @Excel(name = "api.config.columnname.configType", readConverterExp = "api.config.configType.desc")
    private String configType;

    public Long getConfigId()
    {
        return configId;
    }

    public void setConfigId(Long configId)
    {
        this.configId = configId;
    }

    @NotBlank(message = "api.config.error.name.isnull")
    @Size(min = 0, max = 100, message = "api.config.error.name.overlength")
    public String getConfigName()
    {
        return configName;
    }

    public void setConfigName(String configName)
    {
        this.configName = configName;
    }

    @NotBlank(message = "api.config.error.keyName.isnull")
    @Size(min = 0, max = 100, message = "api.config.error.keyName.overlength")
    public String getConfigKey()
    {
        return configKey;
    }

    public void setConfigKey(String configKey)
    {
        this.configKey = configKey;
    }

    @NotBlank(message = "api.config.error.value.isnull")
    @Size(min = 0, max = 500, message = "api.config.error.value.overlength")
    public String getConfigValue()
    {
        return configValue;
    }

    public void setConfigValue(String configValue)
    {
        this.configValue = configValue;
    }

    public String getConfigType()
    {
        return configType;
    }

    public void setConfigType(String configType)
    {
        this.configType = configType;
    }
    
    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("configId", getConfigId())
            .append("configName", getConfigName())
            .append("configKey", getConfigKey())
            .append("configValue", getConfigValue())
            .append("configType", getConfigType())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
