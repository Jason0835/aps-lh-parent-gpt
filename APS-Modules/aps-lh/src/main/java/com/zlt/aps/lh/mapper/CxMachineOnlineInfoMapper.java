package com.zlt.aps.lh.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.lh.api.domain.entity.CxMachineOnlineInfo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 成型在机信息Mapper（硫化侧只读引用）
 * <p>
 * 读取成型侧 {@code T_CX_MACHINE_ONLINE_INFO} 表，供胎胚收尾标识加载时的
 * 「成型在机双结构 → 结构切换完成」判定使用；与成型侧 {@code CxMachineOnlineInfoMapper}
 * 读取同一张表、同窗口口径（T 日与 T-1 日）。
 * 本 Mapper 仅查询，不参与 MES 同步写入。
 * </p>
 *
 * @author APS Team
 */
@Mapper
public interface CxMachineOnlineInfoMapper extends BaseMapper<CxMachineOnlineInfo> {
}
