package com.zlt.aps.common.core.enums;

import com.ruoyi.common.utils.StringUtils;
import lombok.Getter;

/**
 * 操作业务枚举
 *
 * @author Chen
 * @since 2025/12/9
 */
@Getter
public enum OperationBusinessEnums {

    /**
     * 生成月周期排产结构配置
     */
    CREATE_MONTH_CYCLE_STRUCTURE("CREATE_MONTH_CYCLE_STRUCTRUE", "生成月周期排产结构配置"),

    /**
     * 生成月均销量
     */
    CREATE_MONTH_AVERAGE_SALE("CREATE_MONTH_AVERAGE_SALE", "生成月均销量"),

    ;

    /**
     * 操作业务编码
     */
    private final String code;

    /**
     * 操作业务名称
     */
    private final String name;

    private OperationBusinessEnums(String code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * 根据编码获取对应的枚举
     *
     * @param code 编码
     * @return 结果
     */
    public static OperationBusinessEnums getOperationBusinessByCode(String code) {
        if (StringUtils.isEmpty(code)) {
            return null;
        }
        for (OperationBusinessEnums enums : OperationBusinessEnums.values()) {
            if (enums.getCode().equals(code)) {
                return enums;
            }
        }
        return null;
    }
}
