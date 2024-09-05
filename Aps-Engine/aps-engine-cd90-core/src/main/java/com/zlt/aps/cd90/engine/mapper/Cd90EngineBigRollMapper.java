package com.zlt.aps.cd90.engine.mapper;

import java.util.List;

import com.zlt.aps.cd90.engine.vo.Cd90BigRollVo;

/**
 * 15度裁断钢带大卷信息数据Mapper
 * 
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-14 11:37:24
 * @Version 1.0
 */
public interface Cd90EngineBigRollMapper {
	/**
	 * 获取指定月份的15度裁断月度计划
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-14 11:38:42
	 * @param year  年
	 * @param month 月
	 * @return 符合条件的月度计划列表
	 */
	List<Cd90BigRollVo> listCd90BigRoll();
}
