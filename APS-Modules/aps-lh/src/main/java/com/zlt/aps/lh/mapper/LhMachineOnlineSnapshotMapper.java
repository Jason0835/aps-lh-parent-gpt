package com.zlt.aps.lh.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.lh.api.domain.dto.LhMachineOnlineSnapshotDTO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 前段欠产准入的MES历史快照查询映射。
 * 业务仅调用框架查询，不提供自定义SQL或MES数据维护入口。
 *
 * @author APS
 * @since 2026/11/15
 */
@Mapper
public interface LhMachineOnlineSnapshotMapper extends BaseMapper<LhMachineOnlineSnapshotDTO> {
}
