package com.yupi.yupicturebackend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class YuPictureBackendApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    public void test(){
        String s = "https://cloud-picture-1349091286.cos.ap-guangzhou.myqcloud.com/public/1915029936313888769/2025-04-26_a9a2c0f5.jpg".replaceAll("https://cloud-picture-1349091286.cos.ap-guangzhou.myqcloud.com", "");
        System.out.println(s);
    }

}
