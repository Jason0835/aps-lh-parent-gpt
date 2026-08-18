package com.zlt.aps.itf.mes.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.zlt.aps.itf.constant.DataSource;
import com.zlt.aps.itf.mes.domain.MesCd90ScheduleResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 直裁排程结果 MES 中间表 Mapper。
 */
@DS(DataSource.MES)
@Mapper
public interface Cd90ScheduleResultIssueMapper {

    /**
     * 按业务键删除历史下发记录。
     *
     * @param rows 中间表宽表记录
     * @param factoryCode 工厂编码
     * @return 删除数量
     */
    int batchDeleteByBusinessKey(
            @Param("list") List<MesCd90ScheduleResult> rows,
            @Param("factoryCode") String factoryCode);

    /**
     * 批量写入直裁排程结果。
     *
     * @param rows 中间表宽表记录
     * @return 写入数量
     */
    int batchInsert(@Param("list") List<MesCd90ScheduleResult> rows);
}
