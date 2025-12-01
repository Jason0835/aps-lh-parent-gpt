package com.zlt.aps.mps.api.domain;

import com.zlt.aps.common.core.domain.ApsBaseEntity;

import lombok.Data;

/**
 *生产提醒人配置
 * @TableName t_production_reminder_user
 */
@Data
public class ProductionReminderUser extends ApsBaseEntity {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 工序
     */
    private String processes;

    /**
     * 人员
     */
    private String userName;
    
    /**
     * 邮箱
     */
    private String email;


    private static final long serialVersionUID = 1L;

}