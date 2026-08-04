package com.zlt.aps.tm.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 胎面自动排程过程日志实体。
 *
 * <p>一条记录对应一个成功自动排程批次，用于保存中文计算过程追溯信息。</p>
 */
@ApiModel(value = "胎面自动排程过程日志对象", description = "胎面自动排程过程日志表实体对象")
@Data
@TableName(value = "T_TM_SCHEDULE_PROCESS_LOG")
public class TmScheduleProcessLog extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 自动排程批次号。 */
    @ApiModelProperty(value = "自动排程批次号", name = "batchNo")
    @TableField(value = "BATCH_NO")
    private String batchNo;

    /** 自动排程中文过程日志明细。 */
    @ApiModelProperty(value = "自动排程中文过程日志明细", name = "logDetail")
    @TableField(value = "LOG_DETAIL")
    private String logDetail;
}
