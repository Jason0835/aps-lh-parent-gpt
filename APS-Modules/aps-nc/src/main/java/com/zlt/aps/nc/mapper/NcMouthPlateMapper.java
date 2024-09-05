package com.zlt.aps.nc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.nc.api.domain.dto.NcMouthPlateDto;
import com.zlt.aps.nc.entity.NcMouthPlate;

import java.util.List;

/**
 * <p>
 * 内衬口型板信息维护 Mapper 接口
 * </p>
 *
 * @author chenxueyuan
 * @since 2021-05-27
 */
public interface NcMouthPlateMapper extends BaseMapper<NcMouthPlate> {

    /**
     * 查询口型板信息及对应的机台名称
     *
     * @param mouthPlate 口型板信息
     * @return 查询到的口型板信息
     */
    public List<NcMouthPlateDto> selectMouthPlateWithMachineInfo(NcMouthPlate mouthPlate);

    /**
     * 校验记录唯一性
     *
     * @param mouthPlate 要校验的记录
     * @return 查询到相同的记录条数
     */
    public int checkUnique(NcMouthPlate mouthPlate);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     *
     * @param list 要合并的集合
     */
    public void mergeSql(List<NcMouthPlateDto> list);
}
