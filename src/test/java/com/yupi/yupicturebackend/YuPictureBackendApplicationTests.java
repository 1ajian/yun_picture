package com.yupi.yupicturebackend;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.*;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.exception.ThrowUtils;
import com.yupi.yupicturebackend.model.dto.picture.PictureUploadRequest;
import com.yupi.yupicturebackend.model.entity.User;
import com.yupi.yupicturebackend.model.enums.ImageFormatEnum;
import com.yupi.yupicturebackend.model.vo.PictureVO;
import com.yupi.yupicturebackend.service.PictureService;
import com.yupi.yupicturebackend.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@SpringBootTest
class YuPictureBackendApplicationTests {

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
