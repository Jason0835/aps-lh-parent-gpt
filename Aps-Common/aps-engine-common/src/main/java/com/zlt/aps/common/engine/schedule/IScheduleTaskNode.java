package com.zlt.aps.common.engine.schedule;

/**
 * 通用排程任务节点读取接口。
 *
 * <p>该接口用于屏蔽胎面、胎侧等业务任务对象差异，让通用任务链只读取任务、机台、
 * 班次和顺序等基础信息，不反向依赖具体业务模块。</p>
 *
 * @param <T> 节点承载的业务任务对象类型
 */
public interface IScheduleTaskNode<T> {

    /**
     * 获取节点承载的业务任务对象。
     *
     * @return 业务任务对象；节点未绑定任务时由实现类决定是否返回空
     */
    T getTask();

    /**
     * 获取节点所属机台编码。
     *
     * @return 机台编码；未分配任务允许为空
     */
    String getMachineCode();

    /**
     * 获取节点所属班次顺序。
     *
     * @return 六班横向模型中的班次序号
     */
    Integer getShiftOrder();

    /**
     * 获取当前节点在链内的任务顺序。
     *
     * @return 任务顺序；节点尚未入链时允许为空
     */
    Integer getSequence();
}
