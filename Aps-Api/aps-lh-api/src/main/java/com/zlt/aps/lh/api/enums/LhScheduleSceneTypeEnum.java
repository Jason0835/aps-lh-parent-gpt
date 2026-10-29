package com.zlt.aps.lh.api.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 硫化排程结果的实际业务来源场景，不参与排程策略选择。
 *
 * @author APS
 * @date 2026/10/26
 */
@Getter
@AllArgsConstructor
public enum LhScheduleSceneTypeEnum {

    /**
     * 原在机物料续作或原机原模具恢复续作。
     */
    CONTINUOUS("01", "续作"),
    /**
     * 换活字块流程直接形成的结果。
     */
    TYPE_BLOCK("02", "换活字块"),
    /**
     * 实际成功承接历史交替计划的后物料结果。
     */
    PREVIOUS_ALTERNATE("03", "历史交替计划"),
    /**
     * 实际执行结构切换提案形成的结果。
     */
    STRUCTURE_SWITCH("04", "结构切换"),
    /**
     * 普通新增来源形成的结果。
     */
    NORMAL_NEW_SPEC("05", "普通完全新增"),
    /**
     * 续作派生需求进入新增流程形成的结果。
     */
    CONTINUATION_NEW_SPEC("06", "续作新增"),
    /**
     * 普通候选成功换活字块后的残量进入新增流程形成的结果。
     */
    TYPE_BLOCK_NEW_SPEC("07", "换活字块新增"),
    /**
     * 没有可确认来源的历史或兼容结果。
     */
    OTHER("08", "其他");

    /**
     * 两位场景编码。
     */
    private final String code;
    /**
     * 场景中文说明。
     */
    private final String description;

    /**
     * 按编码读取场景，空值及未知编码统一返回其他。
     *
     * @param code 结果中保存的两位场景编码
     * @return 匹配的场景或其他场景
     */
    public static LhScheduleSceneTypeEnum fromCode(String code) {
        for (LhScheduleSceneTypeEnum sceneType : values()) {
            if (sceneType.getCode().equals(code)) {
                return sceneType;
            }
        }
        return OTHER;
    }
}
