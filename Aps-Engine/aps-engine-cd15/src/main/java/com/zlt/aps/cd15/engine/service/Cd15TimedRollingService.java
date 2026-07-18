package com.zlt.aps.cd15.engine.service;

import com.zlt.aps.cd15.engine.model.Cd15RollingTarget;
import com.zlt.aps.cd15.engine.model.Cd15TimedRollingOutput;

/** CD15定时滚动排程Engine入口。 */
public interface Cd15TimedRollingService {

    /** 从目标班开始逐班滚动，并返回原批次差异草稿。 */
    Cd15TimedRollingOutput execute(Cd15RollingTarget target, String inputVersion,
                                   Cd15ScheduleProgressListener listener);
}
