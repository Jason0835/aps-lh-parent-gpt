package com.zlt.aps.tc.domain.vo;

import com.zlt.aps.tc.api.domain.vo.TcChangeMachineRequestVo;
import com.zlt.aps.tc.api.domain.vo.TcChangeQtyRequestVo;
import com.zlt.aps.tc.api.domain.vo.TcInsertTaskRequestVo;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 胎侧人工异步任务请求快照。
 */
@Data
public class TcOperationRequestSnapshot implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 插单请求。 */
    private TcInsertTaskRequestVo insertRequest;

    /** 调量请求。 */
    private TcChangeQtyRequestVo changeQtyRequest;

    /** 单条或批量转机台请求。 */
    private TcChangeMachineRequestVo changeMachineRequest;

    /** 删除结果ID。 */
    private List<Long> resultIdList;
}
