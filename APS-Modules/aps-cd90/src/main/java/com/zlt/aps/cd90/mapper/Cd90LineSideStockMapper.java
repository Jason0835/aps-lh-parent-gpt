package com.zlt.aps.cd90.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.zlt.aps.cd90.api.domain.entity.Cd90LineSideStock;

/**
 * 90°裁断库存信息Mapper接口
 *
 * @author hak
 * @date 2022-03-03
 */
public interface Cd90LineSideStockMapper {
	/**
	 * 查询90°裁断库存信息列表
	 *
	 * @param stock 90°裁断库存信息
	 * @return 90°裁断库存信息集合
	 */
	public List<Cd90LineSideStock> selectStockList(Cd90LineSideStock stock);

	/**
	 * 删除线边库库存
	 * 
	 * @param stockDate
	 * @param userName  操作人
	 * @return
	 */
	int deleteCd90LineSideStock(@Param("dataVersion") String dataVersion, @Param("userName") String userName);

	/**
	 * 插入线边库库存
	 * 
	 * @param dataVersion
	 * @param userName    操作人
	 * @return
	 */
	int insertCd90LineSideStock(@Param("dataVersion") String dataVersion, @Param("userName") String userName);
}
