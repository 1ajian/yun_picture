package com.jianzhao.picturebackend.config;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.region.Region;
import com.qcloud.cos.transfer.TransferManager;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * ClassName: CosClientConfig
 * Package: com.yupi.yupicturebackend.config
 * Description:
 *
 * @Author 阿小健
 * @Create 2025/4/25 12:21
 * @Version 1.0
 */
@Configuration
@ConfigurationProperties(prefix = "cos.client")
@Data
public class CosClientConfig {
    /**
     * 域名
     */
    private String host;

    /**
     * secretId
     */
    private String secretId;

    /**
     * 密钥（注意不要泄露）
     */
    private String secretKey;

    /**
     * 区域
     */
    private String region;

    /**
     * 桶名
     */
    private String bucket;


    /**
     * 创建cosclient实例，这个实例用来后续调用请求
     * @return
     */
    @Bean
    public COSClient createCOSClient() {
        //设置用户身份
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);

        //ClientConfig 中包含了后续请求cos 的客户端设置：
        ClientConfig clientConfig = new ClientConfig(new Region(region));

        //创建并返回COS客户端
        return new COSClient(cred, clientConfig);
    }

}
