package com.zlt.aps.cx.vo;

import lombok.Data;

/**
 * 提前收尾信息（Advance Ending Info）
 *
 * <p>触发条件（NEVER_CONFIGURED）：结构当日无可配置机台（BEGIN_DAY/END_DAY 不覆盖排程日或结构名口径不一致），
 * 且未来3个月排产配置（T_MP_STRUCTURE_ALLOCATION）中从未配置过该结构 -> 成型机已切换/将切换走且不会回来，
 * 该结构下仍有成型余量的胎胚将永远无法排产，需生成提前收尾记录提醒计划员。
 *
 * <p>数据流：TaskGroupService.collectAdvanceEndingInfos 收集（每班次刷新，末次为准）
 * -> CoreScheduleAlgorithmServiceImpl.appendAdvanceEndingResults 追加主表 0 产量行（对应班次 CLASS 分析）
 * -> processScenario3AdvanceEnding 写 T_CX_EMBRYO_LH_TIME（结构切换最早可供硫化时间，场景3）。
 *
 * @author APS Team
 */
@Data
public class AdvanceEndingInfo {

    /** 分组结构名（如 265/70R19.5，无花纹后缀） */
    private String structureName;

    /** 当月配置表结构名（如 265/70R19.5-12PR708F，前缀匹配命中的配置行名称，无配置时为 null） */
    private String configStructureName;

    /** 胎胚编码 */
    private String embryoCode;

    /** 物料编码 */
    private String materialCode;

    /** 物料描述 */
    private String materialDesc;

    /** 产品状态 */
    private String productStatus;

    /** 成型余量（收集时点 = 总成型余量 - 已使用成型余量） */
    private Integer formingRemainder;

    /** 硫化余量（收集时点，来自 monthSurplusMap） */
    private Integer vulcanizeRemainder;

    /** 收尾日（当月配置 max END_DAY，当月无配置时为 null） */
    private Integer endingDay;

    /** 切换日（= 收尾日 + 1，当月无配置时为 null） */
    private Integer switchDay;

    /** 当月配置的成型机台编码（当月无配置时为 null） */
    private String cxMachineCode;

    /** 关联硫化任务ID（逗号分隔） */
    private String lhScheduleIds;

    /** 提前收尾备注（写入主表对应班次 CLASS 分析 + T_CX_EMBRYO_LH_TIME.REMARK） */
    private String remark;
}
