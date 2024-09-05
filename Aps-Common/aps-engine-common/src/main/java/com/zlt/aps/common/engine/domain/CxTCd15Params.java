package com.zlt.aps.common.engine.domain;

import java.io.Serializable;
import java.util.Date;

import com.zlt.aps.common.core.domain.ApsBaseEntity;
import lombok.Data;

/**
 * 15度裁断参数信息
 * @TableName T_CD15_PARAMS
 */
@Data
public class CxTCd15Params extends ApsBaseEntity {
    /**
     * 主键ID，对应自增序列为：SEQ_PUBLIC
     */
    private Long id;

    /**
     * 参数code
     */
    private String paramCode;

    /**
     * 参数名称
     */
    private String paramName;

    /**
     * 参数值
     */
    private String paramValue;

    /**
     * 参数值对应的正则表达式
     */
    private String regularExpression;

    /**
     * 参数值根据正则表达式校验是失败后的错误提示
     */
    private String errorTips;

    private static final long serialVersionUID = 1L;
}