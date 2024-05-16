package com.github.codert96.web.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.codert96.web.annotations.Ignored;
import com.github.codert96.web.bean.WebEncryptProperties;
import com.github.codert96.web.core.Encryptor;
import com.github.codert96.web.core.EncryptorKeyExtractor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Base64;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.util.StreamUtils;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;

@Slf4j
public class EncryptRequestResponseResolverHandler extends RequestResponseBodyMethodProcessor {

    private final WebEncryptProperties webEncryptProperties;
    private final EncryptorKeyExtractor encryptorKeyExtractor;
    private final ObjectMapper objectMapper;

    public EncryptRequestResponseResolverHandler(List<HttpMessageConverter<?>> converters, ContentNegotiationManager manager, List<Object> requestResponseBodyAdvice,
                                                 ObjectMapper objectMapper,
                                                 WebEncryptProperties webEncryptProperties,
                                                 EncryptorKeyExtractor encryptorKeyExtractor
    ) {
        super(converters, manager, requestResponseBodyAdvice);
        this.webEncryptProperties = webEncryptProperties;
        this.encryptorKeyExtractor = encryptorKeyExtractor;
        this.objectMapper = objectMapper;
    }


    @Override
    public boolean supportsParameter(@NonNull MethodParameter parameter) {
        return super.supportsParameter(parameter) && webEncryptProperties.isEnable() && !parameter.hasParameterAnnotation(Ignored.class);
    }

    @Override
    public boolean supportsReturnType(@NonNull MethodParameter returnType) {
        return super.supportsReturnType(returnType) && webEncryptProperties.isEnable() && !returnType.hasMethodAnnotation(Ignored.class);
    }

    @Override
    protected Object readWithMessageConverters(@NonNull HttpInputMessage inputMessage, @NonNull MethodParameter parameter, @NonNull Type targetType)
            throws IOException, HttpMediaTypeNotSupportedException, HttpMessageNotReadableException {
        byte[] key = encryptorKeyExtractor.key();
        log.debug("解密的key：{}", Base64.toBase64String(key));
        byte[] body = StreamUtils.copyToByteArray(inputMessage.getBody());
        log.debug("请求的数据：{}", Base64.toBase64String(body));
        Encryptor encryptor = webEncryptProperties.encryptor();
        log.debug("解密的类：{}", encryptor.getClass());
        byte[] decrypt = encryptor.decrypt(key, body);
        HttpHeaders httpHeaders = new HttpHeaders(inputMessage.getHeaders());
        httpHeaders.setContentLength(decrypt.length);
        log.debug("解密后的结果：{}", new String(decrypt));
        HttpInputMessage decryptHttpInputMessage = new HttpInputMessage() {
            @Override
            public @NonNull InputStream getBody() {
                return new ByteArrayInputStream(decrypt);
            }

            @Override
            public @NonNull HttpHeaders getHeaders() {
                return httpHeaders;
            }
        };
        return super.readWithMessageConverters(decryptHttpInputMessage, parameter, targetType);
    }

    @Override
    public void handleReturnValue(Object returnValue, @NonNull MethodParameter returnType, @NonNull ModelAndViewContainer mavContainer, @NonNull NativeWebRequest webRequest) throws IOException, HttpMediaTypeNotAcceptableException, HttpMessageNotWritableException {
        if (!isResourceType(returnValue, returnType) && Objects.nonNull(returnValue)) {
            mavContainer.setRequestHandled(true);
            ServletServerHttpRequest inputMessage = createInputMessage(webRequest);
            ServletServerHttpResponse outputMessage = createOutputMessage(webRequest);

            byte[] key = encryptorKeyExtractor.key();
            log.debug("加密的key：{}", Base64.toBase64String(key));
            byte[] body = objectMapper.writeValueAsBytes(returnValue);
            log.debug("响应的数据：{}", Base64.toBase64String(body));
            Encryptor encryptor = webEncryptProperties.encryptor();
            log.debug("加密的类：{}", encryptor.getClass());
            byte[] encrypt = encryptor.encrypt(key, body);
            HttpHeaders httpHeaders = outputMessage.getHeaders();
            //noinspection UastIncorrectHttpHeaderInspection
            httpHeaders.add("X-XSRF-Body-Encrypt", encryptor.algorithm());
            httpHeaders.setContentLength(encrypt.length);
            log.debug("加密后的结果：{}", Base64.toBase64String(encrypt));

            writeWithMessageConverters(encrypt, returnType, inputMessage, outputMessage);
            return;
        }

        super.handleReturnValue(returnValue, returnType, mavContainer, webRequest);
    }
}
