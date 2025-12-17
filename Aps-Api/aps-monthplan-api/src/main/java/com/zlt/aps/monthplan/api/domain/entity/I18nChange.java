package com.zlt.aps.monthplan.api.domain.entity;


import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.ruoyi.common.utils.StringUtils;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@ApiModel(value = "国际化变更表", description = "国际化变更表")
@Data
@TableName(value = "T_I18N_CHANGE")
@KeySequence(value = "SEQ_I18N_CHANGE")
public class I18nChange extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "国际化关系表ID", name = "relId")
    @TableField(value = "REL_ID")
    private Long relId;

    @ApiModelProperty(value = "国际化对应key", name = "changeKey")
    @TableField(value = "CHANGE_KEY")
    private String changeKey;

    @ApiModelProperty(value = "国际化对应value", name = "changeValue")
    @TableField(value = "CHANGE_VALUE")
    private String changeValue;

    @ApiModelProperty(value = "是否修改过（0：未修改 1：已修改）", name = "isChange")
    @TableField(value = "IS_CHANGE")
    private Integer isChange;

    // ---- 关联字段 ----
    @ApiModelProperty(value = "所属模块国际化", name = "modeName")
    @TableField(exist = false)
    private String modeName;

    @ApiModelProperty(value = "所属文件名", name = "fileName")
    @TableField(exist = false)
    private String fileName;

    @ApiModelProperty(value = "是否页面加载（0：否 1：是）", name = "isPage")
    @TableField(exist = false)
    private Integer isPage;

    // ---- 国际化字段 ----
    @ApiModelProperty(value = "国际化对应valueI18n", name = "changeValueI18n")
    private String changeValueI18n;

    @ApiModelProperty(value = "国际化对应value（中）300 必填", notes = "必填", name = "changeValueI18n_zh_CN")
    private String changeValueI18n_zh_CN;

    @ApiModelProperty(value = "国际化对应value（英）300 必填", notes = "必填", name = "changeValueI18n_en_US")
    private String changeValueI18n_en_US;

    @ApiModelProperty(value = "国际化对应value（越）300 必填", notes = "必填", name = "changeValueI18n_vi_VN")
    private String changeValueI18n_vi_VN;

    @ApiModelProperty(value = "所属模块国际化I18n", name = "modeNameI18n")
    private String modeNameI18n;

    @ApiModelProperty(value = "所属模块国际化（中）", name = "modeNameI18n_zh_CN")
    private String modeNameI18n_zh_CN;

    @ApiModelProperty(value = "所属模块国际化（英）", name = "modeNameI18n_en_US")
    private String modeNameI18n_en_US;

    @ApiModelProperty(value = "所属模块国际化（越）", name = "modeNameI18n_vi_VN")
    private String modeNameI18n_vi_VN;

    /**
     * 国际化回显具体字段
     */
    public void buildChangeValue() {
        String zhCN = StringUtils.isEmpty(this.getChangeValueI18n_zh_CN()) ? "" : this.getChangeValueI18n_zh_CN();
        String enUS = StringUtils.isEmpty(this.getChangeValueI18n_en_US()) ? "" : this.getChangeValueI18n_en_US();
        String viVN = StringUtils.isEmpty(this.getChangeValueI18n_vi_VN()) ? "" : this.getChangeValueI18n_vi_VN();
        this.setChangeValue(StringUtils.format("[{\"zh_CN\":\"{}\",\"en_US\":\"{}\",\"vi_VN\":\"{}\"}]", zhCN, enUS, viVN));
    }
}
