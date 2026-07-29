package com.zlt.aps.tm.api.domain.vo;

import com.zlt.aps.tm.api.domain.entity.TmScheduleUnplanned;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 胎面未排任务分页结果。
 *
 * <p>用于 Feign 反序列化后端 mybatis-plus {@code Page} 的 records、total 结构，
 * 避免 tm-api 和 BootUI 依赖 mybatis-plus-extension。</p>
 */
@Data
@ApiModel(value = "胎面未排任务分页结果", description = "胎面未排任务分页结果")
public class TmScheduleUnplannedPageVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前页数据。 */
    @ApiModelProperty(value = "当前页数据")
    private List<TmScheduleUnplanned> records = new ArrayList<>();

    /** 总记录数。 */
    @ApiModelProperty(value = "总记录数")
    private long total;
}
