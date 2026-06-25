package com.zlt.aps.common.core.enums;

import com.ruoyi.common.utils.StringUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 模具交替完成状态枚举
 * <p>MES回报的完成状态可能为中文（未完成/已完成）或数值（0/1），本枚举统一处理两种格式的转换。</p>
 *
 * @author APS Team
 */
@Getter
@AllArgsConstructor
public enum MouldFinishStatusEnum {

    /**
     * 未完成
     */
    NOT_COMPLETED("0", "未完成"),

    /**
     * 已完成
     */
    COMPLETED("1", "已完成");

    /**
     * 状态编码（数据库存储值）
     */
    private final String code;

    /**
     * 中文描述（MES回报可能传此值）
     */
    private final String zhName;

    /**
     * 根据编码获取枚举
     *
     * @param code 状态编码
     * @return 枚举对象，未匹配返回null
     */
    public static MouldFinishStatusEnum getByCode(String code) {
        if (StringUtils.isEmpty(code)) {
            return null;
        }
        for (MouldFinishStatusEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }

    /**
     * 根据中文名称获取枚举
     *
     * @param zhName 中文名称
     * @return 枚举对象，未匹配返回null
     */
    public static MouldFinishStatusEnum getByZhName(String zhName) {
        if (StringUtils.isEmpty(zhName)) {
            return null;
        }
        for (MouldFinishStatusEnum e : values()) {
            if (e.getZhName().equals(zhName)) {
                return e;
            }
        }
        return null;
    }

    /**
     * 将MES回报的完成状态（可能是中文或数值）统一转换为标准编码
     *
     * @param value MES回报的完成状态值
     * @return 标准编码，无法识别时原值返回
     */
    public static String convertToCode(String value) {
        if (StringUtils.isEmpty(value)) {
            return value;
        }
        // 先尝试按编码匹配
        MouldFinishStatusEnum byCode = getByCode(value);
        if (byCode != null) {
            return byCode.getCode();
        }
        // 再尝试按中文名称匹配
        MouldFinishStatusEnum byZhName = getByZhName(value);
        if (byZhName != null) {
            return byZhName.getCode();
        }
        // 无法识别，原值返回
        return value;
    }

    /**
     * 判断是否为已完成状态
     *
     * @param value 状态值（编码或中文）
     * @return true-已完成
     */
    public static boolean isCompleted(String value) {
        return COMPLETED.getCode().equals(convertToCode(value));
    }
}
