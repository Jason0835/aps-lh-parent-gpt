package com.zlt.aps.cd90.engine.service;

import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleContext;
import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleInput;
import com.zlt.aps.cd90.engine.model.Cd90ScheduleCandidate;

import java.util.List;

/**
 * 当前直裁班次候选规格的内存准备服务。
 */
public interface Cd90ScheduleCandidatePreparationService {

    /**
     * 根据排程上下文、输入快照和直裁班次字段生成有序候选规格。
     *
     * @param context 自动排程上下文
     * @param input 自动排程输入快照
     * @param classField 当前直裁班次字段，取CLASS1至CLASS8
     * @return 已排序候选规格
     */
    List<Cd90ScheduleCandidate> prepare(Cd90AutoScheduleContext context,
                                        Cd90AutoScheduleInput input,
                                        String classField);
}
