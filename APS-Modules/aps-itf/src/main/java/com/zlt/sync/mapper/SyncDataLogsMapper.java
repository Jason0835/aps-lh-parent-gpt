package com.zlt.sync.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.zlt.aps.itf.constant.DataSource;
import com.zlt.aps.itf.vo.SyncDataLogs;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 *
 * @Description 同步日志数据接口
 * @Author zlt
 * @Date 2022-3-9 14:09:16
 */
@DS(DataSource.MASTER)
@Mapper
public interface SyncDataLogsMapper {

	/**
	 * 查询同步日志，通过数据版本查询只会查询到一条记录
	 *
	 * @param dataVersion
	 * @return
	 */
	SyncDataLogs getSyncDataLogs(@Param("dataVersion") String dataVersion);

	/**
	 * 查询请求日志，通过数据版本查询只会查询到一条记录
	 *
	 * @param dataVersion
	 * @return
	 */
	SyncDataLogs getReqDataLogs(@Param("dataVersion") String dataVersion);

}
