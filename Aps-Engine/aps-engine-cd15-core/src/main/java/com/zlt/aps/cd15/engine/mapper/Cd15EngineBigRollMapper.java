package com.zlt.aps.cd15.engine.mapper;

import java.util.List;

import com.zlt.aps.cd15.engine.vo.Cd15BigRollVo;

/**
 * 15度裁断钢带大卷信息数据Mapper
 * 
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-12 11:37:24
 * @Version 1.0
 */
public interface Cd15EngineBigRollMapper {
	/**
	 * 获取钢带大卷信息列表
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-12 11:38:42
	 * @return
	 */
	List<Cd15BigRollVo> listCd15BigRoll();
}
