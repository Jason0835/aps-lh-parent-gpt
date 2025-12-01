package com.zlt.aps.common.engine.enums;

import com.google.common.base.Objects;

import lombok.Getter;

/**
 * 斜裁机台一出二模式
 *
 */
@Getter
public enum MachineOneOutTwo {
    ONE("1", "一出一"), TWO("0", "一出二");

    /**
     * 下标
     */
    private String index;
    /**
     * 名称
     */
    private String name;

    private MachineOneOutTwo(String index, String name) {
        this.index = index;
        this.name = name;
    }

    /**
     * 根据下标获取
     * 
     * @param index
     * @return
     */
    public static MachineOneOutTwo getClassEnums(String index) {
        for (MachineOneOutTwo cls : MachineOneOutTwo.values()) {
            if (Objects.equal(cls.getIndex(), index)) {
                return cls;
            }
        }
        return null;
    }
}
