package com.zlt.aps.monthplan.api.domain.vo;

import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 多个区域拼接转换国际化名称用Vo
 *
 * @author Chen
 * @since 2025/12/19
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AreaConvertVo extends BaseEntity {

    /**
     * 区域编码
     */
    private String areaCode;

    /**
     * 区域名称国际化字符串
     */
    private String areaCodeName;

    /**
     * 区域名称国际化
     */
    private String areaCodeNameI18n;
}
