package com.zlt.aps.lh.component;

/** 结构专供类型；顺序同时表示匹配专供机时的候选优先层级。 */
public enum DedicatedType {
    /** 全部成型机均有专供配置，只能使用对应专供硫化机。 */
    EXCLUSIVE,
    /** 部分成型机有专供配置，普通硫化机仍可参与。 */
    PREFERRED,
    /** 未命中专供配置，保持原排程规则。 */
    NONE
}
