package com.zlt.aps.lh.engine.enums;

import lombok.Getter;

/**
 * 成型任务类型枚举类
 */
@Getter
public enum TaskTypeEnum {

    TODO("1","待投产"),MOLD("2","待换模"),DOING("3","投产中"),CLOSE_OUT("4","已收尾"),CLOSE_OUT_DELIN("5","已收尾欠产");
    private String taskType;
    private String taskTypeName;

    private TaskTypeEnum(String taskType,String taskTypeName){
        this.taskType=taskType;
        this.taskTypeName=taskTypeName;
    }

    /**
     * 根据下标获取
     * @param taskType
     * @return
     */
    public static TaskTypeEnum getTaskTypeEnums(String taskType) {
        for (TaskTypeEnum taskTypeEnum : TaskTypeEnum.values()) {
            if (taskTypeEnum.getTaskType().equals(taskType)) {
                return taskTypeEnum;
            }
        }
        return null;
    }
}
