package com.mix.sync;

import javax.annotation.Resource;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.zlt.mix.sync.MixSyncApplication;
import com.zlt.mix.sync.controller.RequestMesController;

import lombok.extern.slf4j.Slf4j;

@SpringBootTest(classes = MixSyncApplication.class)
@Slf4j
public class SyncTest {

    @Resource
    RequestMesController mes;
    
//    @Test
    public void test() {
    	mes.syncBasMaterial();
    }
}
