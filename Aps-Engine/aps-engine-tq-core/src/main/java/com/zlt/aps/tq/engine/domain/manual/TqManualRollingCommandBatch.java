package com.zlt.aps.tq.engine.domain.manual;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 胎圈人工滚动批量命令。
 */
@Data
public class TqManualRollingCommandBatch {

    /** 同一事务内需要统一计算的命令 */
    private List<TqManualRollingCommand> commandList = new ArrayList<>();

    /**
     * 追加有效命令。
     *
     * @param command 滚动命令
     */
    public void addCommand(TqManualRollingCommand command) {
        if (command != null) {
            commandList.add(command);
        }
    }
}
