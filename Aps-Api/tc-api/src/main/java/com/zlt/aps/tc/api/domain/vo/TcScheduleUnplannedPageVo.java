package com.zlt.aps.tc.api.domain.vo;

import com.zlt.aps.tc.api.domain.entity.TcScheduleUnplanned;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 胎侧未排任务分页结果。
 *
 * <p>用于 Feign 反序列化后端 mybatis-plus {@code Page} 的 JSON 结构（records/total），
 * 避免 tc-api 模块强依赖 mybatis-plus-extension 的 {@code Page} 类。
 * 字段命名与 mybatis-plus {@code Page} 序列化结果保持一致，多余字段由 Jackson 忽略。</p>
 */
@Data
@ApiModel(value = "胎侧未排任务分页结果", description = "胎侧未排任务分页结果")
public class TcScheduleUnplannedPageVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前页数据。 */
    @ApiModelProperty(value = "当前页数据")
    private List<TcScheduleUnplanned> records = new ArrayList<>();

    /** 总记录数。 */
    @ApiModelProperty(value = "总记录数")
    private long total;
}
