package com.zlt.mix.schedule.engine.service.basicdata.impl;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.zlt.mix.schedule.engine.mapper.GlueSpanEngineMapper;
import com.zlt.mix.schedule.engine.service.basicdata.GlueSpanEngineService;
import com.zlt.mix.schedule.engine.vo.GlueSpanReceiveVo;

/**
 * 引擎胶料跨区服务
 * 
 * @author hakimryan
 *
 */
@Service
public class GlueSpanEngineServiceImpl implements GlueSpanEngineService {
	@Resource
	private GlueSpanEngineMapper glueSpanEngineMapper;

	/**
	 * 根据查询条件获取胶料跨区接收信息
	 * 
	 * @param glueSpanReceiveVo 查询条件
	 */
	@Override
	public List<GlueSpanReceiveVo> listGlueSpanReceive(GlueSpanReceiveVo glueSpanReceiveVo) {
		return glueSpanEngineMapper.listGlueSpanReceive(glueSpanReceiveVo);
	}
}
