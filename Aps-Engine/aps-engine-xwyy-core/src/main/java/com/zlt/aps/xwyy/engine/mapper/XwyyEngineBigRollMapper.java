package com.zlt.aps.xwyy.engine.mapper;

import java.util.List;

import com.zlt.aps.xwyy.engine.vo.XwyyBigRollVo;

/**
 * 钢压大卷信息数据Mapper
 * 
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-22 11:37:24
 * @Version 1.0
 */
public interface XwyyEngineBigRollMapper {
	/**
	 * 获取帘布大卷配置信息
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-22 10:01:43
	 * @return
	 */
	List<XwyyBigRollVo> listCd90BigRoll();
}
