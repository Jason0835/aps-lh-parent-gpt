package com.zlt.aps.lh.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.lh.api.domain.entity.LhScheduleCheckResult;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 硫化排程校验结果 Mapper（MyBatis-Plus）。
 *
 * <p>校验执行前需要按「工厂 + 业务排程日」清理旧记录，此处使用物理删除
 * 保证同日重跑不会残留历史异常（逻辑删除会累积脏数据）。</p>
 *
 * @author APS
 * @date 2026/10/27
 */
@Mapper
public interface LhScheduleCheckResultMapper extends BaseMapper<LhScheduleCheckResult> {

    /**
     * 按工厂 + 业务排程日物理删除旧校验记录（保证重跑幂等）。
     *
     * @param factoryCode 工厂编号
     * @param scheduleDate 业务排程日（yyyy-MM-dd）
     * @return 删除行数
     */
    @Delete("DELETE FROM T_LH_SCHEDULE_CHECK_RESULT "
            + "WHERE FACTORY_CODE = #{factoryCode} AND SCHEDULE_DATE = #{scheduleDate}")
    int physicalDeleteByFactoryAndDate(@Param("factoryCode") String factoryCode,
                                       @Param("scheduleDate") String scheduleDate);
}
