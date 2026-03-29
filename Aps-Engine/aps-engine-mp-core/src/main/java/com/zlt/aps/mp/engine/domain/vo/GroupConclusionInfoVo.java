package com.zlt.aps.mp.engine.domain.vo;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * 分组收尾信息对象-辅助，值传递
 *
 * @author ZLT
 * @date 20260329
 */
@Slf4j
@Getter
public class GroupConclusionInfoVo implements Serializable {
    /**
     * 业务处理成功标记
     * true 表示在conclusionDay日前一日要收尾
     * false 表示无需收尾
     */
    private boolean successFlag;
    /**
     * 此日的前一日为分组收尾日
     */
    private Integer conclusionDay;
    /**
     * 收尾天数
     */
    private Integer deductionDay;
    /**
     * 收尾条件：实单最小硫化机台数
     */
    private Integer minLhMachineCount;
    /**
     * 需要释放产能的天集合，包含conclusionDay
     */
    private Set<Integer> deductionDaySet;
    /**
     * 收尾机台
     */
    private CxMachineBaseInfoVo conclusionCxMachine;

    /**
     * 构造函数
     *
     * @param minLhMachineCount 实单最小硫化机台数
     * @param conclusionDay     最早释放天
     * @param deductionDaySet   需要释放的天集合
     */
    public GroupConclusionInfoVo(Integer minLhMachineCount, Integer conclusionDay, Set<Integer> deductionDaySet) {
        this.successFlag = true;
        this.minLhMachineCount = minLhMachineCount;
        this.conclusionDay = conclusionDay;
        if (null == deductionDaySet) {
            deductionDaySet = new HashSet<>();
        }
        deductionDaySet.add(conclusionDay);
        this.deductionDaySet = deductionDaySet;
        this.deductionDay = deductionDaySet.size();
    }

    /**
     * 创建无需收尾的业务结果对象
     *
     * @param minLhMachineCount 最低硫化配比
     * @return
     */
    public static GroupConclusionInfoVo buildNoConclusionInfo(Integer minLhMachineCount) {
        GroupConclusionInfoVo noHandler = new GroupConclusionInfoVo(minLhMachineCount, null, null);
        noHandler.successFlag = false;
        return noHandler;
    }

    /**
     * 加入收尾机台信息
     *
     * @param selectedCxMachine 选择收尾的机台
     */
    public void addSelectedConclusionCxMachine(CxMachineBaseInfoVo selectedCxMachine) {
        if (null == selectedCxMachine) {
            return;
        }
        this.conclusionCxMachine = selectedCxMachine;
    }
}
