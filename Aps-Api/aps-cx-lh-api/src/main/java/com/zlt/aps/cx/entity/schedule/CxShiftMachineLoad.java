package com.zlt.aps.cx.entity.schedule;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDate;
import java.util.Date;

/**
 * 班次级机台胎胚负荷映射（供续作预留参考前班次台数）
 *
 * <p>排程保存时按班次维度写入，下次排程开始时从昨日最后一个班次加载，
 * 构建 {@code previousShiftMachineEmbryoLoadMap} 供 ContinueTaskProcessor 保底预留参考。
 *
 * <p>对应表：T_CX_SHIFT_MACHINE_LOAD
 *
 * @author APS Team
 */
@Data
@TableName("T_CX_SHIFT_MACHINE_LOAD")
@ApiModel(value = "班次级机台胎胚负荷映射", description = "供续作预留参考前班次硫化机台数")
public class CxShiftMachineLoad {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    /** 排程日期 */
    private LocalDate scheduleDate;

    /** 班次编码 */
    private String shiftCode;

    /** 班次序号(1-8) */
    private Integer shiftOrder;

    /** 成型机台编码 */
    private String cxMachineCode;

    /** 胎胚编码 */
    private String embryoCode;

    /** 硫化机台数 */
    private Integer lhMachineCount;

    /** 工厂编码 */
    private String factoryCode;

    /** 创建时间 */
    private Date createTime;
}
