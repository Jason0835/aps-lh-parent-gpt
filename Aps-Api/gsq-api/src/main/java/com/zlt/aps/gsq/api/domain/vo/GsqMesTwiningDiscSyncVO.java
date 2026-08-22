package com.zlt.aps.gsq.api.domain.vo;

import com.zlt.aps.gsq.api.domain.entity.GsqTwiningDisc;
import com.zlt.aps.gsq.api.domain.entity.GsqTwiningDiscMachine;
import com.zlt.aps.gsq.api.domain.entity.GsqTwiningDiscSpec;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * MES钢丝圈缠绕盘同步聚合VO
 * <p>一次Feign调用传输MES三张缠绕盘表的最新数据，在钢丝圈微服务侧单事务落库，
 * 保证缠绕盘清单/规格关系/机台关系三表数据一致性</p>
 * <p>对应MES中间表：
 * MES_WIRE_DISC_INFO（缠绕盘清单）、
 * MES_WIRE_DISC_SPEC_MAPPING（缠绕盘与规格关系）、
 * MES_WIRE_DISC_MACHINE_MAPPING（缠绕盘与机台关系）</p>
 * <p>三张表均无日期字段，全为字符串/数值字段，无跨时区JDBC偏移问题</p>
 *
 * @author zlt
 * @date 2026-08-20
 */
@Data
@ApiModel(value = "MES钢丝圈缠绕盘同步聚合VO", description = "MES三张缠绕盘表同步数据")
public class GsqMesTwiningDiscSyncVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 缠绕盘清单（来源MES_WIRE_DISC_INFO，各编码按DATA_VERSION最大版本去重） */
    @ApiModelProperty(value = "缠绕盘清单")
    private List<GsqTwiningDisc> discList;

    /** 缠绕盘规格关系（来源MES_WIRE_DISC_SPEC_MAPPING，按twiningDiscCode编码直接关联主表，落库T_GSQ_TWINING_DISC_SPEC） */
    @ApiModelProperty(value = "缠绕盘规格关系")
    private List<GsqTwiningDiscSpec> specList;

    /** 缠绕盘机台关系（来源MES_WIRE_DISC_MACHINE_MAPPING，按twiningDiscCode关联主表） */
    @ApiModelProperty(value = "缠绕盘机台关系")
    private List<GsqTwiningDiscMachine> machineList;
}
