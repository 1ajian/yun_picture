package com.jianzhao.picturebackend;

import com.jianzhao.picturebackend.service.PictureService;
import com.jianzhao.picturebackend.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class PictureBackendApplicationTests {

    @Resource
    private PictureService pictureService;

    @Resource
    private UserService userService;
    @Test
    void contextLoads() {
//        String urlTemplate = "https://image.baidu.com/search/acjson?tn=resultjson_com&word=%s&pn=%s&rn=%s";
//        String url = String.format(urlTemplate,"萧熏儿",10,30);
//        System.out.println(url);



        //JSONObject jsonObject = JSONUtil.parseObj(response);
//        JSONArray list = jsonObject.getJSONArray("data");
//        System.out.println(list.size());
//        for (int i = 0; i < list.size(); i++) {
//            JSONObject res = list.get(i, JSONObject.class);
//            JSONArray replaceUrl = res.getJSONArray("replaceUrl");
//            JSONObject aim = replaceUrl.get(0, JSONObject.class);
//            String objURL = (String) aim.get("ObjURL");
//            System.out.println(objURL);
//        }


    }

    @Test
    public void test(){
        String s = "https://cloud-picture-1349091286.cos.ap-guangzhou.myqcloud.com/public/1915029936313888769/2025-04-26_a9a2c0f5.jpg".replaceAll("https://cloud-picture-1349091286.cos.ap-guangzhou.myqcloud.com", "");
        System.out.println(s);
    }

    @Test
    public void test3(){

    }

}
