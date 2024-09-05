package com.zlt.mdm.auth.service.impl;

import com.zlt.mdm.auth.api.domain.MdmSystemData;
import com.zlt.mdm.auth.api.domain.vo.UserSystemVo;
import com.zlt.mdm.auth.mapper.MdmSystemAuthMapper;
import com.zlt.mdm.auth.service.IMdmSystemAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MdmSystemAuthServiceImpl implements IMdmSystemAuthService {

    @Autowired
    MdmSystemAuthMapper mdmSystemAuthMapper;

    @Override
    public UserSystemVo selectSystemDataByUserId(Long userId) {
        return mdmSystemAuthMapper.selectSystemDatasByUserId(userId);
    }

    @Override
    public List<MdmSystemData> selectSystemDataList() {
        return mdmSystemAuthMapper.selectSystemDataList();
    }
}
