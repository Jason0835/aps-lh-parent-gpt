package com.zlt.mix.setting.engine;

import com.zlt.mix.common.engine.service.impl.IncrementService;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@MapperScan("com.zlt.**.mapper")
@SpringBootTest
class MixEngineSettingApplicationTests {

    @Resource
    private IncrementService incrementService;

    @Test
    public void test() {
        System.out.println("test start!!!!!!");
        System.out.println("test ending!!!!!!");
        System.out.println(incrementService.getSequence3("TM20210630"));
    }

}
