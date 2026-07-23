package com.zlt.aps.cx.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 任务分组结果（由 {@link com.zlt.aps.cx.service.engine.TaskGroupService#groupTasks} 产出）。
 *
 * @author APS Team
 */
@Data
public class TaskGroupResultVo {

    /** 续作任务：当前机台在产的胎胚 */
    private List<DailyEmbryoTask> continueTasks = new ArrayList<>();
    /** 试制任务：试制/量试任务 */
    private List<DailyEmbryoTask> trialTasks = new ArrayList<>();
    /** 新增任务：非续作、非试制的常规任务 */
    private List<DailyEmbryoTask> newTasks = new ArrayList<>();
}
