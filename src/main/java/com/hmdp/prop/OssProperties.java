package com.hmdp.prop;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aliyun.oss.file")
@Data
public class OssProperties {
    private String bucketname;
    private String endpoint;
    private String keyid;
    private String keysecret;
}
