package com.zlt.aps.cd90.engine.service.impl;

import com.zlt.aps.cd90.engine.algorithm.Cd90TimedRollingEngineExecutor;
import com.zlt.aps.cd90.engine.model.Cd90RollingTarget;
import com.zlt.aps.cd90.engine.model.Cd90TimedRollingOutput;
import com.zlt.aps.cd90.engine.service.Cd90ScheduleProgressListener;
import com.zlt.aps.cd90.engine.service.Cd90TimedRollingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** CD90定时滚动排程Engine入口实现。 */
@Service
@RequiredArgsConstructor
public class Cd90TimedRollingServiceImpl implements Cd90TimedRollingService {

    private final Cd90TimedRollingEngineExecutor executor;

    @Override
    public Cd90TimedRollingOutput execute(Cd90RollingTarget target, String inputVersion,
                                          Cd90ScheduleProgressListener listener) {
        return executor.execute(target, inputVersion, listener);
    }
}
