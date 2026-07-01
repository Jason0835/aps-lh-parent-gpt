package com.zlt.aps.cd90.engine.service;

import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleContext;
import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleInput;
import com.zlt.aps.cd90.engine.model.Cd90RollingScheduleContext;
import com.zlt.aps.cd90.engine.model.Cd90ScheduleCandidate;

import java.util.List;

/**
 * 当前直裁班次候选规格的内存准备服务。
 */
public interface Cd90ScheduleCandidatePreparationService {

    /**
     * 根据排程上下文、输入快照、直裁班次字段和滚动上下文生成有序候选规格。
     *
     * @param context 自动排程上下文
     * @param input 自动排程输入快照
     * @param classField 当前直裁班次字段，取CLASS1至CLASS8
     * @param rolling 多班滚动排程共享的内存上下文，提供累计成型消耗用于续作判定
     * @return 已排序候选规格
     */
    List<Cd90ScheduleCandidate> prepare(Cd90AutoScheduleContext context,
                                        Cd90AutoScheduleInput input,
                                        String classField,
                                        Cd90RollingScheduleContext rolling);
}
