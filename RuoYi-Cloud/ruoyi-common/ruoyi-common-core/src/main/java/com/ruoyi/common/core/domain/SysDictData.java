package com.ruoyi.common.core.domain;

import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.annotation.Excel.ColumnType;
import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 字典数据表 sys_dict_data
 *
 * @author ruoyi
 */
@Getter
@Setter
public class SysDictData extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 字典编码
     */
    @Excel(name = "api.dictData.columnname.code", cellType = ColumnType.NUMERIC)
    private Long dictCode;

    /**
     * 字典排序
     */
    @Excel(name = "api.dictData.columnname.sort", cellType = ColumnType.NUMERIC)
    private Long dictSort;

    /**
     * 字典标签
     */
    @Excel(name = "api.dictData.columnname.label")
    private String dictLabel;

    /**
     * 字典键值
     */
    @Excel(name = "api.dictData.columnname.value")
    private String dictValue;

    /**
     * 字典类型
     */
    @Excel(name = "api.dictData.columnname.dictType")
    private String dictType;

    /**
     * 样式属性（其他样式扩展）
     */
    private String cssClass;

    /**
     * 表格字典样式
     */
    private String listClass;

    /**
     * 是否默认（Y是 N否）
     */
    @Excel(name = "api.dictData.columnname.isDefault", readConverterExp = "api.dictData.isDefault.desc")
    private String isDefault;

    /**
     * 状态（0正常 1停用）
     */
    @Excel(name = "api.dictData.columnname.status", readConverterExp = "api.dictData.status.desc")
    private String status;

    /**
     * 语言包,默认zh_CN
     */
    @Excel(name = "api.dictData.columnname.lang")
    private String locale;

    public Long getDictCode() {
        return dictCode;
    }

    public void setDictCode(Long dictCode) {
        this.dictCode = dictCode;
    }

    public Long getDictSort() {
        return dictSort;
    }

    public void setDictSort(Long dictSort) {
        this.dictSort = dictSort;
    }

    @NotBlank(message = "api.dictData.error.lablel.isnull")
    @Size(min = 0, max = 100, message = "api.dictData.error.lablel.overlength")
    public String getDictLabel() {
        return dictLabel;
    }

    public void setDictLabel(String dictLabel) {
        this.dictLabel = dictLabel;
    }

    @NotBlank(message = "api.dictData.error.value.isnull")
    @Size(min = 0, max = 100, message = "api.dictData.error.value.overlength")
    public String getDictValue() {
        return dictValue;
    }

    public void setDictValue(String dictValue) {
        this.dictValue = dictValue;
    }

    @NotBlank(message = "api.dictData.error.dictType.isnull")
    @Size(min = 0, max = 100, message = "api.dictData.error.dictType.overlength")
    public String getDictType() {
        return dictType;
    }

    public void setDictType(String dictType) {
        this.dictType = dictType;
    }

    @Size(min = 0, max = 100, message = "api.dictData.error.cssClass.overlength")
    public String getCssClass() {
        return cssClass;
    }

    public void setCssClass(String cssClass) {
        this.cssClass = cssClass;
    }

    public String getListClass() {
        return listClass;
    }

    public void setListClass(String listClass) {
        this.listClass = listClass;
    }

    public boolean getDefault() {
        return UserConstants.YES.equals(this.isDefault) ? true : false;
    }

    public String getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(String isDefault) {
        this.isDefault = isDefault;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("dictCode", getDictCode())
                .append("dictSort", getDictSort())
                .append("dictLabel", getDictLabel())
                .append("dictValue", getDictValue())
                .append("dictType", getDictType())
                .append("cssClass", getCssClass())
                .append("listClass", getListClass())
                .append("isDefault", getIsDefault())
                .append("status", getStatus())
                .append("createBy", getCreateBy())
                .append("createTime", getCreateTime())
                .append("updateBy", getUpdateBy())
                .append("updateTime", getUpdateTime())
                .append("remark", getRemark())
                .append("locale", getLocale())
                .toString();
    }
}
