package com.tlt.aps.enums;

/**
 * 顺序层级枚举定义类
 *
 * @author ZLT
 * 20250217
 */
public enum SortHierarchyEnum {
    /**
     * 1 第一层
     */
    FIRST_HIERARCHY(1, "第一层"),
    /**
     * 2 第二层
     */
    SECOND_HIERARCHY(2, "第二层"),
    /**
     * 3 第三层
     */
    THIRD_HIERARCHY(3, "第三层");

    private Integer code;
    private String remark;

    SortHierarchyEnum(Integer code, String remark) {
        this.code = code;
        this.remark = remark;
    }

    /**
     * 根据业务编码，获取对应的业务排序枚举实例对象
     *
     * @param code
     * @return
     */
    public static SortHierarchyEnum getInstance(Integer code) {
        if (null == code) {
            return null;
        }
        for (SortHierarchyEnum hierarchy : SortHierarchyEnum.values()) {
            if (hierarchy.getCode().equals(code)) {
                return hierarchy;
            }
        }
        return null;
    }

    /**
     * 编码
     *
     * @return
     */
    public Integer getCode() {
        return code;
    }

    /**
     * 备注说明
     *
     * @return
     */
    public String getRemark() {
        return remark;
    }
}
