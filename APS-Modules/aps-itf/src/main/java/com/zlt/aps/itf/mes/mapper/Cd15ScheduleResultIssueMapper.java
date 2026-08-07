package com.zlt.aps.itf.mes.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResultIssue;
import com.zlt.aps.itf.constant.DataSource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 斜裁排程结果下发 MES 中间表 Mapper。
 */
@DS(DataSource.MES)
@Mapper
public interface Cd15ScheduleResultIssueMapper {

    /**
     * 按业务键删除历史下发记录。
     *
     * @param issueList 下发记录
     * @param factoryCode 工厂编码
     * @return 删除数量
     */
    int batchDeleteByBusinessKey(
            @Param("list") List<Cd15ScheduleResultIssue> issueList,
            @Param("factoryCode") String factoryCode);

    /**
     * 批量写入斜裁排程结果。
     *
     * @param issueList 下发记录
     * @param dataVersion 数据版本
     * @param companyCode 公司编码
     * @param factoryCode 工厂编码
     * @return 写入数量
     */
    int batchInsert(
            @Param("list") List<Cd15ScheduleResultIssue> issueList,
            @Param("dataVersion") String dataVersion,
            @Param("companyCode") String companyCode,
            @Param("factoryCode") String factoryCode);
}
