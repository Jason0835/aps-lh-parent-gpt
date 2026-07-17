package com.zlt.aps.itf.mes.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.zlt.aps.itf.constant.DataSource;
import com.zlt.aps.tc.api.domain.entity.TcScheduleResultIssue;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 胎侧排程结果下发MES中间表Mapper。
 */
@DS(DataSource.MES)
@Mapper
public interface TcScheduleResultIssueMapper {

    /**
     * 批量新增胎侧排程结果。
     *
     * @param issueList 下发记录
     * @return 影响行数
     */
    int batchInsert(@Param("list") List<TcScheduleResultIssue> issueList);

    /**
     * 批量更新已存在的胎侧排程结果。
     *
     * @param issueList 下发记录
     * @return 影响行数
     */
    int batchUpdate(@Param("list") List<TcScheduleResultIssue> issueList);

    /**
     * 查询已存在记录的业务键。
     *
     * @param issueList 下发记录
     * @return 已存在记录
     */
    List<TcScheduleResultIssue> selectExisting(@Param("list") List<TcScheduleResultIssue> issueList);

    /**
     * 删除指定日期和数据版本的胎侧排程结果。
     *
     * @param scheduleDate 排程日期
     * @param dataVersion 数据版本
     * @return 影响行数
     */
    int deleteByScheduleDate(@Param("scheduleDate") String scheduleDate,
                             @Param("dataVersion") String dataVersion);

    /**
     * 按数据版本查询MES幂等键。
     *
     * @param dataVersion 数据版本
     * @return 幂等键集合
     */
    List<String> selectIdempotencyKeys(@Param("dataVersion") String dataVersion);
}
