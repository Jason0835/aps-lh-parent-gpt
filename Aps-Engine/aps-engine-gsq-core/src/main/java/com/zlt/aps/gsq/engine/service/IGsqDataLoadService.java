package com.zlt.aps.gsq.engine.service;

import com.zlt.aps.gsq.engine.context.GsqScheduleContext;

/**
 * 钢丝圈排程数据加载Service。
 *
 * <p>负责将S1阶段需要的全部基础数据加载到Context中。</p>
 *
 * @author APS
 */
public interface IGsqDataLoadService {

    /**
     * 加载全部基础数据到Context。
     *
     * <p>加载内容包括：</p>
     * <ul>
     *   <li>排程参数（保鲜期、换盘时长、末班估值等）</li>
     *   <li>胎圈6班次排程结果（用于BOM分解计算需求量）</li>
     *   <li>施工信息（BOM用量、钢丝直径、英寸）</li>
     *   <li>6点MES库存</li>
     *   <li>机台信息（寸口、钢丝直径、产线）</li>
     *   <li>工装车数量及容量</li>
     *   <li>检修计划</li>
     *   <li>胎圈/钢丝圈停产配置</li>
     *   <li>限定/不可作业机台配置</li>
     *   <li>损耗率、月度剩余</li>
     *   <li>前日早班计划量</li>
     * </ul>
     *
     * @param context 排程上下文
     */
    void loadAllData(GsqScheduleContext context);
}
