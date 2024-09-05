package com.zlt.aps.cx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cx.api.domain.dto.CxScheduleLimitDto;
import com.zlt.aps.cx.entity.CxScheduleLimit;

import java.util.List;

/**
 * <p>
 * 成型排产限制信息维护 Mapper 接口
 * </p>
 *
 * @author chenxueyuan
 * @since 2021-06-16
 */
public interface CxScheduleLimitMapper extends BaseMapper<CxScheduleLimit> {

    /**
     * 根据条件查询排产限制信息
     *
     * @param limit 查询条件
     * @return 查询到的结果
     */
    public List<CxScheduleLimitDto> selectLimitList(CxScheduleLimit limit);

    /**
     * 根据条件查询排产限制信息校验唯一性
     *
     * @param limit 查询条件
     * @return 查询到的结果
     */
    public List<CxScheduleLimitDto> checkUnique(CxScheduleLimit limit);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     */
    public void mergeSql(List<CxScheduleLimit> list);

}
