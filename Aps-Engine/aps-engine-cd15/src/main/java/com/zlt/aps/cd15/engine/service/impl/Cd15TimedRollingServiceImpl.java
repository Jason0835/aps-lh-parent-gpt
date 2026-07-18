package com.zlt.aps.cd15.engine.service.impl;

import com.zlt.aps.cd15.engine.algorithm.Cd15TimedRollingEngineExecutor;
import com.zlt.aps.cd15.engine.model.Cd15RollingTarget;
import com.zlt.aps.cd15.engine.model.Cd15TimedRollingOutput;
import com.zlt.aps.cd15.engine.service.Cd15ScheduleProgressListener;
import com.zlt.aps.cd15.engine.service.Cd15TimedRollingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** CD15定时滚动排程Engine入口实现。 */
@Service
@RequiredArgsConstructor
public class Cd15TimedRollingServiceImpl implements Cd15TimedRollingService {

    private final Cd15TimedRollingEngineExecutor executor;

    @Override
    public Cd15TimedRollingOutput execute(Cd15RollingTarget target, String inputVersion,
                                          Cd15ScheduleProgressListener listener) {
        return executor.execute(target, inputVersion, listener);
    }
}
