package com.zlt.aps.cd15.engine.service;

import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleContext;
import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleInput;
import com.zlt.aps.cd15.engine.model.Cd15RollingScheduleContext;
import com.zlt.aps.cd15.engine.model.Cd15ScheduleCandidate;

import java.util.List;

/**
 * 当前斜裁班次候选规格的内存准备服务。
 */
public interface Cd15ScheduleCandidatePreparationService {

    /**
     * 根据排程上下文、输入快照、斜裁班次字段和滚动上下文生成有序候选规格。
     *
     * @param context 自动排程上下文
     * @param input 自动排程输入快照
     * @param classField 当前斜裁班次字段，取CLASS1至CLASS8
     * @param rolling 多班滚动排程共享的内存上下文，提供累计成型消耗用于续作判定
     * @return 已排序候选规格
     */
    List<Cd15ScheduleCandidate> prepare(Cd15AutoScheduleContext context,
                                        Cd15AutoScheduleInput input,
                                        String classField,
                                        Cd15RollingScheduleContext rolling);
}
