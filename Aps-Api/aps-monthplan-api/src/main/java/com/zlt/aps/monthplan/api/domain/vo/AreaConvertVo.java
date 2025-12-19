package com.zlt.aps.monthplan.api.domain.vo;

import lombok.Data;

/**
 * 多个区域拼接转换国际化名称用Vo
 *
 * @author Chen
 * @since 2025/12/19
 */
@Data
public class AreaConvertVo {

    /**
     * 区域编码
     */
    private String areaCode;

    /**
     * 区域名称
     */
    private String areaCodeName;
}
