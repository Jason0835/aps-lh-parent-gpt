package com.zlt.aps.common.engine.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zlt.aps.common.engine.domain.ParamsVo;
import com.zlt.aps.common.engine.mapper.ParamsMapper;
import com.zlt.aps.common.engine.service.ParamsService;

@Service
public class ParamsServiceImpl implements ParamsService {
	@Autowired
	private ParamsMapper paramsMapper;

	/**
	 * 成型参数
	 * 
	 * @param code
	 * @return
	 */
	@Override
	public String getCxParam(String code) {
		return paramsMapper.listCxParams().stream().filter(p -> p.getParamCode().equals(code)).findAny()
				.map(ParamsVo::getParamValue).orElse(null);
	};

	/**
	 * 硫化参数
	 * 
	 * @param code
	 * @return
	 */
	@Override
	public String getLhParam(String code) {
		return paramsMapper.listLhParams().stream().filter(p -> p.getParamCode().equals(code)).findAny()
				.map(ParamsVo::getParamValue).orElse(null);
	};

	/**
	 * 内衬参数
	 * 
	 * @param code
	 * @return
	 */
	@Override
	public String getNcParam(String code) {
		return paramsMapper.listNcParams().stream().filter(p -> p.getParamCode().equals(code)).findAny()
				.map(ParamsVo::getParamValue).orElse(null);
	};

	/**
	 * 胎圈参数
	 * 
	 * @param code
	 * @return
	 */
	@Override
	public String getTqParam(String code) {
		return paramsMapper.listTqParams().stream().filter(p -> p.getParamCode().equals(code)).findAny()
				.map(ParamsVo::getParamValue).orElse(null);
	};

	/**
	 * 胎侧参数
	 * 
	 * @param code
	 * @return
	 */
	@Override
	public String getTcParam(String code) {
		return paramsMapper.listTcParams().stream().filter(p -> p.getParamCode().equals(code)).findAny()
				.map(ParamsVo::getParamValue).orElse(null);
	};

	/**
	 * 钢丝圈参数
	 * 
	 * @param code
	 * @return
	 */
	@Override
	public String getGsqParam(String code) {
		return paramsMapper.listGsqParams().stream().filter(p -> p.getParamCode().equals(code)).findAny()
				.map(ParamsVo::getParamValue).orElse(null);
	};

	/**
	 * 胎面参数
	 * 
	 * @param code
	 * @return
	 */
	@Override
	public String getTmParam(String code) {
		return paramsMapper.listTmParams().stream().filter(p -> p.getParamCode().equals(code)).findAny()
				.map(ParamsVo::getParamValue).orElse(null);
	};

	/**
	 * 15度裁断参数
	 * 
	 * @param code
	 * @return
	 */
	@Override
	public String getCd15Param(String code) {
		return paramsMapper.listCd15Params().stream().filter(p -> p.getParamCode().equals(code)).findAny()
				.map(ParamsVo::getParamValue).orElse(null);
	};

	/**
	 * 90度裁断参数
	 * 
	 * @param code
	 * @return
	 */
	@Override
	public String getCd90Param(String code) {
		return paramsMapper.listCd90Params().stream().filter(p -> p.getParamCode().equals(code)).findAny()
				.map(ParamsVo::getParamValue).orElse(null);
	};

	/**
	 * 钢带压延参数
	 * 
	 * @param code
	 * @return
	 */
	@Override
	public String getGdyyParam(String code) {
		return paramsMapper.listGdyyParams().stream().filter(p -> p.getParamCode().equals(code)).findAny()
				.map(ParamsVo::getParamValue).orElse(null);
	};

	/**
	 * 纤维压延参数
	 * 
	 * @param code
	 * @return
	 */
	@Override
	public String getXwyyParam(String code) {
		return paramsMapper.listXwyyParams().stream().filter(p -> p.getParamCode().equals(code)).findAny()
				.map(ParamsVo::getParamValue).orElse(null);
	};
}
