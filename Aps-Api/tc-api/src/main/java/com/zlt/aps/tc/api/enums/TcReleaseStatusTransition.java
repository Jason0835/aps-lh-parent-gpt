package com.zlt.aps.tc.api.enums;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 胎侧排程发布状态迁移校验工具。
 *
 * <p>集中管理 {@link TcScheduleReleaseStatusEnum} 各状态间的合法迁移路径，作为自动迁移（人工编辑触发、
 * 滚动重排触发等）的统一权威，避免状态流转散落硬编码与非法迁移（如 RELEASE_FAILED 被编辑后误转为 WAIT_RELEASE）。</p>
 *
 * <p>合法迁移矩阵：</p>
 * <ul>
 *   <li>NOT_RELEASED(0)/RELEASE_FAILED(2)/TIMEOUT_FAILED(4)/WAIT_RELEASE(5)
 *   -&gt; RELEASING(3)（开始下发）</li>
 *   <li>RELEASING(3) -&gt; RELEASED(1)/RELEASE_FAILED(2)/TIMEOUT_FAILED(4)（下发结果）</li>
 *   <li>RELEASED(1) -&gt; WAIT_RELEASE(5)（已发布被编辑后回退待发布）</li>
 *   <li>NOT_RELEASED(0)/RELEASE_FAILED(2)/WAIT_RELEASE(5) 人工编辑后保持原状态</li>
 * </ul>
 *
 * <p>所有发布状态写入口均以数据库旧状态为准调用本工具校验，批量更新需先完成全部记录校验。</p>
 */
public final class TcReleaseStatusTransition {

    /** 允许人工编辑的状态集合（编辑后可回退为待发布/未发布）；null/空视为未发布，可编辑 */
    private static final Set<TcScheduleReleaseStatusEnum> EDITABLE_STATUSES = EnumSet.of(
            TcScheduleReleaseStatusEnum.NOT_RELEASED,
            TcScheduleReleaseStatusEnum.RELEASED,
            TcScheduleReleaseStatusEnum.RELEASE_FAILED,
            TcScheduleReleaseStatusEnum.WAIT_RELEASE
    );

    /** 合法迁移矩阵 */
    private static final Map<TcScheduleReleaseStatusEnum, Set<TcScheduleReleaseStatusEnum>> TRANSITIONS = new EnumMap<>(TcScheduleReleaseStatusEnum.class);

    static {
        TRANSITIONS.put(TcScheduleReleaseStatusEnum.NOT_RELEASED,
                EnumSet.of(TcScheduleReleaseStatusEnum.RELEASING));
        TRANSITIONS.put(TcScheduleReleaseStatusEnum.WAIT_RELEASE,
                EnumSet.of(TcScheduleReleaseStatusEnum.RELEASING));
        TRANSITIONS.put(TcScheduleReleaseStatusEnum.RELEASING, EnumSet.of(TcScheduleReleaseStatusEnum.RELEASED, TcScheduleReleaseStatusEnum.RELEASE_FAILED, TcScheduleReleaseStatusEnum.TIMEOUT_FAILED));
        TRANSITIONS.put(TcScheduleReleaseStatusEnum.RELEASED, EnumSet.of(TcScheduleReleaseStatusEnum.WAIT_RELEASE));
        TRANSITIONS.put(TcScheduleReleaseStatusEnum.RELEASE_FAILED,
                EnumSet.of(TcScheduleReleaseStatusEnum.RELEASING));
        TRANSITIONS.put(TcScheduleReleaseStatusEnum.TIMEOUT_FAILED,
                EnumSet.of(TcScheduleReleaseStatusEnum.RELEASING));
    }

    private TcReleaseStatusTransition() {
    }

    /**
     * 判断从 from 迁移到 to 是否合法。
     *
     * @param from 原状态编码；null/空视为 NOT_RELEASED
     * @param to   目标状态编码
     * @return true 表示合法；from/to 任一无法识别返回 false
     */
    public static boolean canTransit(String from, String to) {
        TcScheduleReleaseStatusEnum fromEnum = normalize(from);
        TcScheduleReleaseStatusEnum toEnum = parse(to);
        if (fromEnum == null || toEnum == null) {
            return false;
        }
        if (fromEnum == toEnum) {
            return true;
        }
        Set<TcScheduleReleaseStatusEnum> targets = TRANSITIONS.get(fromEnum);
        return targets != null && targets.contains(toEnum);
    }

    /**
     * 判断指定状态是否允许人工编辑（编辑后可触发回退）。
     *
     * @param status 状态编码；null/空视为未发布，允许编辑
     * @return true 表示允许编辑
     */
    public static boolean isEditable(String status) {
        TcScheduleReleaseStatusEnum statusEnum = normalize(status);
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
        TcScheduleReleaseStatusEnum statusEnum = normalize(originalStatus);
        if (statusEnum == null) {
            return TcScheduleReleaseStatusEnum.NOT_RELEASED.getCode();
        }
        if (TcScheduleReleaseStatusEnum.RELEASED == statusEnum) {
            return TcScheduleReleaseStatusEnum.WAIT_RELEASE.getCode();
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
    private static TcScheduleReleaseStatusEnum normalize(String code) {
        if (code == null || code.trim().isEmpty()) {
            return TcScheduleReleaseStatusEnum.NOT_RELEASED;
        }
        return parse(code);
    }

    /**
     * 解析状态编码为枚举，无法识别返回 null。
     *
     * @param code 状态编码
     * @return 枚举值；非法返回 null
     */
    private static TcScheduleReleaseStatusEnum parse(String code) {
        if (code == null) {
            return null;
        }
        String trimmed = code.trim();
        for (TcScheduleReleaseStatusEnum e : TcScheduleReleaseStatusEnum.values()) {
            if (e.getCode().equals(trimmed)) {
                return e;
            }
        }
        return null;
    }
}
