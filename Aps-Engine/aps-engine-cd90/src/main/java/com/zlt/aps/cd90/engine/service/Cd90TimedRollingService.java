package com.zlt.aps.cd90.engine.service;

import com.zlt.aps.cd90.engine.model.Cd90RollingTarget;
import com.zlt.aps.cd90.engine.model.Cd90TimedRollingOutput;

/** CD90定时滚动排程Engine入口。 */
public interface Cd90TimedRollingService {

    /** 从目标班开始逐班滚动，并返回原批次差异草稿。 */
    Cd90TimedRollingOutput execute(Cd90RollingTarget target, String inputVersion,
                                   Cd90ScheduleProgressListener listener);
}
