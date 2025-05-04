package com.yupi.yupicturebackend.controller;

import com.yupi.yupicturebackend.common.BaseResponse;
import com.yupi.yupicturebackend.common.ResultUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ClassName: MainController
 * Package: com.yupi.yupicturebackend.controller
 * Description:
 *  健康检查
 * @Author 阿小健
 * @Create 2025/4/22 16:20
 * @Version 1.0
 */
@RestController
@RequestMapping("/")
public class MainController {

    @GetMapping("health")
    public BaseResponse<String> heath() {
        return ResultUtils.success("健康检查");
    }
}
