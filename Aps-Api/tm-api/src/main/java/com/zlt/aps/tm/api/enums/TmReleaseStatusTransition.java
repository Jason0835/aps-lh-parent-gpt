package com.zlt.aps.tm.api.enums;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 胎面排程发布状态迁移校验工具。
 *
 * <p>集中管理 {@link TmScheduleReleaseStatusEnum} 各状态间的合法迁移路径，作为自动迁移（人工编辑触发、
 * 滚动重排触发等）的统一权威，避免状态流转散落硬编码与非法迁移（如 RELEASE_FAILED 被编辑后误转为 WAIT_RELEASE）。</p>
 *
 * <p>合法迁移矩阵（与胎侧 TcReleaseStatusTransition 对齐）：</p>
 * <ul>
 *   <li>NOT_RELEASED(0)/RELEASE_FAILED(2)/TIMEOUT_FAILED(4)/WAIT_RELEASE(5)
 *   -&gt; RELEASING(3)（开始下发）</li>
 *   <li>RELEASING(3) -&gt; RELEASED(1)/RELEASE_FAILED(2)/TIMEOUT_FAILED(4)（下发结果）</li>
 *   <li>RELEASED(1) -&gt; WAIT_RELEASE(5)（已发布被编辑后回退待发布，唯一进入待发布的路径）</li>
 *   <li>NOT_RELEASED(0)/RELEASE_FAILED(2)/WAIT_RELEASE(5) 人工编辑后保持原状态</li>
 * </ul>
 *
 * <p>所有发布状态写入口均以数据库旧状态为准调用本工具校验，批量更新需先完成全部记录校验。</p>
 */
public final class TmReleaseStatusTransition {

    /** 允许人工编辑的状态集合（编辑后仅已发布回退待发布，其余保持原状）；null/空视为未发布，可编辑 */
    private static final Set<TmScheduleReleaseStatusEnum> EDITABLE_STATUSES = EnumSet.of(
            TmScheduleReleaseStatusEnum.NOT_RELEASED,
            TmScheduleReleaseStatusEnum.RELEASED,
            TmScheduleReleaseStatusEnum.RELEASE_FAILED,
            TmScheduleReleaseStatusEnum.WAIT_RELEASE
    );

    /** 合法迁移矩阵 */
    private static final Map<TmScheduleReleaseStatusEnum, Set<TmScheduleReleaseStatusEnum>> TRANSITIONS = new EnumMap<>(TmScheduleReleaseStatusEnum.class);

    static {
        TRANSITIONS.put(TmScheduleReleaseStatusEnum.NOT_RELEASED,
                EnumSet.of(TmScheduleReleaseStatusEnum.RELEASING));
        TRANSITIONS.put(TmScheduleReleaseStatusEnum.WAIT_RELEASE,
                EnumSet.of(TmScheduleReleaseStatusEnum.RELEASING));
        TRANSITIONS.put(TmScheduleReleaseStatusEnum.RELEASING, EnumSet.of(TmScheduleReleaseStatusEnum.RELEASED,
                TmScheduleReleaseStatusEnum.RELEASE_FAILED, TmScheduleReleaseStatusEnum.TIMEOUT_FAILED));
        TRANSITIONS.put(TmScheduleReleaseStatusEnum.RELEASED, EnumSet.of(TmScheduleReleaseStatusEnum.WAIT_RELEASE));
        TRANSITIONS.put(TmScheduleReleaseStatusEnum.RELEASE_FAILED,
                EnumSet.of(TmScheduleReleaseStatusEnum.RELEASING));
        TRANSITIONS.put(TmScheduleReleaseStatusEnum.TIMEOUT_FAILED,
                EnumSet.of(TmScheduleReleaseStatusEnum.RELEASING));
    }

    private TmReleaseStatusTransition() {
    }

    /**
     * 判断从 from 迁移到 to 是否合法。
     *
     * @param from 原状态编码；null/空视为 NOT_RELEASED
     * @param to   目标状态编码
     * @return true 表示合法；from/to 任一无法识别返回 false
     */
    public static boolean canTransit(String from, String to) {
        TmScheduleReleaseStatusEnum fromEnum = normalize(from);
        TmScheduleReleaseStatusEnum toEnum = parse(to);
        if (fromEnum == null || toEnum == null) {
            return false;
        }
        if (fromEnum == toEnum) {
            return true;
        }
        Set<TmScheduleReleaseStatusEnum> targets = TRANSITIONS.get(fromEnum);
        return targets != null && targets.contains(toEnum);
    }

    /**
     * 判断指定状态是否允许人工编辑（编辑后可触发回退）。
     *
     * @param status 状态编码；null/空视为未发布，允许编辑
     * @return true 表示允许编辑
     */
    public static boolean isEditable(String status) {
        TmScheduleReleaseStatusEnum statusEnum = normalize(status);
        return statusEnum != null && EDITABLE_STATUSES.contains(statusEnum);
    }

    /**
     * 人工编辑后应回退到的状态。
     *
     * <p>仅已发布状态回退 WAIT_RELEASE(5)，未发布、发布失败和待发布状态保持不变。</p>
     *
     * @param originalStatus 原状态编码
     * @return 回退后的状态编码
     */
    public static String resolveEditedStatus(String originalStatus) {
        TmScheduleReleaseStatusEnum statusEnum = normalize(originalStatus);
        if (statusEnum == null) {
            return TmScheduleReleaseStatusEnum.NOT_RELEASED.getCode();
        }
        if (TmScheduleReleaseStatusEnum.RELEASED == statusEnum) {
            return TmScheduleReleaseStatusEnum.WAIT_RELEASE.getCode();
        }
        return statusEnum.getCode();
    }

    /**
     * 判断状态编码是否为合法的发布状态枚举值。
     *
     * @param code 状态编码
     * @return true 表示合法
     */
    public static boolean isValidCode(String code) {
        return parse(code) != null;
    }

    /**
     * 解析状态编码为枚举；null/空返回 NOT_RELEASED，无法识别返回 null。
     *
     * @param code 状态编码
     * @return 枚举值
     */
    private static TmScheduleReleaseStatusEnum normalize(String code) {
        if (code == null || code.trim().isEmpty()) {
            return TmScheduleReleaseStatusEnum.NOT_RELEASED;
        }
        return parse(code);
    }

    /**
     * 解析状态编码为枚举，无法识别返回 null。
     *
     * @param code 状态编码
     * @return 枚举值；非法返回 null
     */
    private static TmScheduleReleaseStatusEnum parse(String code) {
        if (code == null) {
            return null;
        }
        String trimmed = code.trim();
        for (TmScheduleReleaseStatusEnum e : TmScheduleReleaseStatusEnum.values()) {
            if (e.getCode().equals(trimmed)) {
                return e;
            }
        }
        return null;
    }
}
