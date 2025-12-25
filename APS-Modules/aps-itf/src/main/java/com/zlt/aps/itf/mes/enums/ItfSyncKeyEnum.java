package com.zlt.aps.itf.mes.enums;

import lombok.Getter;

/**
 * 接口码枚举类
 *
 * @author zlt
 * @since 2025/12/25
 */
@Getter
public enum ItfSyncKeyEnum {
    /**
     * XXX接口
     */
    SYNC_XXX("XXX", "MES", "APS", "XXX接口"),
    ;
	/**
	 * 接口码
	 */
    private final String code;
    /**
     * 数据提供系统
     */
    private final String dataSys;
    /**
     * 数据接收系统
     */
    private final String dockSys;
    /**
     * 接口描述
     */
    private final String desc;

    ItfSyncKeyEnum(String code, String dataSys, String dockSys, String desc) {
        this.code = code;
        this.dataSys = dataSys;
        this.dockSys = dockSys;
        this.desc = desc;
    }

    public static ItfSyncKeyEnum getByCode(String code) {
        for (ItfSyncKeyEnum value : ItfSyncKeyEnum.values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }
}
