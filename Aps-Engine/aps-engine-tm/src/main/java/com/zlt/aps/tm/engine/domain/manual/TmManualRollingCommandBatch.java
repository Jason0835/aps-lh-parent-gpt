package com.zlt.aps.tm.engine.domain.manual;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 胎面人工滚动批量命令。
 */
@Data
public class TmManualRollingCommandBatch {

    /** 同一事务内需要统一计算的命令 */
    private List<TmManualRollingCommand> commandList = new ArrayList<>();

    /**
     * 追加有效命令。
     *
     * @param command 滚动命令
     */
    public void addCommand(TmManualRollingCommand command) {
        if (command != null) {
            commandList.add(command);
        }
    }
}
