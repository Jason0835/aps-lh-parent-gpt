package com.zlt.aps.mp.api.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@ApiModel(value = "国际化变更表", description = "国际化变更表")
@Data
public class I18nJsonVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "国际化的语言包，默认zh_CN（zh_CN：中文，en_US：英文，vi_VN：越文）", name = "locale")
    private String locale;


    @ApiModelProperty(value = "资源名称,默认i18n/web（i18n/web：页面国际化包）", name = "basename")
    private String basename;
}
