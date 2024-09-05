package com.zlt.aps.cd15.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.zlt.aps.cd15.api.domain.entity.Cd15LineSideStock;

/**
 * 15°裁断库存信息Mapper接口
 *
 * @author hak
 * @date 2022-03-03
 */
public interface Cd15LineSideStockMapper {
	/**
	 * 查询15°裁断库存信息列表
	 *
	 * @param stock 15°裁断库存信息
	 * @return 15°裁断库存信息集合
	 */
	public List<Cd15LineSideStock> selectStockList(Cd15LineSideStock stock);

	/**
	 * 删除线边库库存
	 * 
	 * @param stockDate
	 * @param userName  操作人
	 * @return
	 */
	int deleteCd15LineSideStock(@Param("dataVersion") String dataVersion, @Param("userName") String userName);

	/**
	 * 插入线边库库存
	 * 
	 * @param dataVersion
	 * @param userName    操作人
	 * @return
	 */
	int insertCd15LineSideStock(@Param("dataVersion") String dataVersion, @Param("userName") String userName);
}
