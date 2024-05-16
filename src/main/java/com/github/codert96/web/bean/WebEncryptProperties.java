package com.github.codert96.web.bean;

import com.github.codert96.web.core.Encryptor;
import com.github.codert96.web.core.impl.SM4Encryptor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.beans.Transient;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ConfigurationProperties(prefix = WebEncryptProperties.PREFIX)
public class WebEncryptProperties {
    public static final String PREFIX = "spring.web.encrypt";

    private final transient Map<Class<? extends Encryptor>, Encryptor> ENCRYPTOR_MAP = new ConcurrentHashMap<>();

    @Getter
    @Setter
    private boolean enable;

    @Getter
    @Setter
    private Class<? extends Encryptor> encryptor = SM4Encryptor.class;


    @Transient
    public Encryptor encryptor() {
        return ENCRYPTOR_MAP.computeIfAbsent(encryptor, BeanUtils::instantiateClass);
    }
}
