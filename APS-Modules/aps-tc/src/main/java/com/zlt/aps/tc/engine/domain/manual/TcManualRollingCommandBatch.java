package com.zlt.aps.tc.engine.domain.manual;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 同一事务内连续执行的胎侧人工滚动命令集。
 */
@Data
public class TcManualRollingCommandBatch {
    /** 命令集合。 */
    private List<TcManualRollingCommand> commandList = new ArrayList<>();

    /**
     * 追加有效命令。
     *
     * @param command 滚动命令
     */
    public void addCommand(TcManualRollingCommand command) {
        if (command != null) {
            this.commandList.add(command);
        }
    }
}
