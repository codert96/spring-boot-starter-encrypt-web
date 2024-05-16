package com.github.codert96.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.codert96.web.bean.WebEncryptProperties;
import com.github.codert96.web.core.EncryptorKeyExtractor;
import com.github.codert96.web.handler.EncryptRequestResponseResolverHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.method.support.HandlerMethodArgumentResolverComposite;
import org.springframework.web.method.support.HandlerMethodReturnValueHandlerComposite;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.handler.HandlerExceptionResolverComposite;
import org.springframework.web.servlet.mvc.method.annotation.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

@RequiredArgsConstructor
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(WebEncryptProperties.class)
public class WebEncryptConfig implements ApplicationContextAware, InitializingBean {
    private final WebEncryptProperties webEncryptProperties;
    private ApplicationContext applicationContext;

    @Override
    public void afterPropertiesSet() {

        Map<String, RequestBodyAdvice> requestBodyAdviceMap = applicationContext.getBeansOfType(RequestBodyAdvice.class);
        //noinspection rawtypes
        Map<String, ResponseBodyAdvice> responseBodyAdviceMap = applicationContext.getBeansOfType(ResponseBodyAdvice.class);

        List<Object> requestResponseBodyAdvice = new ArrayList<>();

        if (!CollectionUtils.isEmpty(requestBodyAdviceMap)) {
            requestResponseBodyAdvice.addAll(requestBodyAdviceMap.values());
        }
        if (!CollectionUtils.isEmpty(responseBodyAdviceMap)) {
            requestResponseBodyAdvice.addAll(responseBodyAdviceMap.values());
        }
        ObjectMapper objectMapper = applicationContext.getBean(ObjectMapper.class);
        EncryptorKeyExtractor encryptorKeyExtractor = applicationContext.getBean(EncryptorKeyExtractor.class);

        ContentNegotiationManager negotiationManager = applicationContext.getBean(ContentNegotiationManager.class);

        {
            RequestMappingHandlerAdapter requestMappingHandlerAdapter = applicationContext.getBean(RequestMappingHandlerAdapter.class);
            List<Object> bodyAdvice = newList(requestResponseBodyAdvice, findRequestResponseBodyAdvice(requestMappingHandlerAdapter, "requestResponseBodyAdvice"));

            EncryptRequestResponseResolverHandler encryptRequestResponseResolverHandler = new EncryptRequestResponseResolverHandler(
                    requestMappingHandlerAdapter.getMessageConverters(),
                    negotiationManager,
                    bodyAdvice,
                    objectMapper,
                    webEncryptProperties,
                    encryptorKeyExtractor
            );
            set(requestMappingHandlerAdapter::getArgumentResolvers, encryptRequestResponseResolverHandler, requestMappingHandlerAdapter::setArgumentResolvers);
            set(requestMappingHandlerAdapter::getReturnValueHandlers, encryptRequestResponseResolverHandler, requestMappingHandlerAdapter::setReturnValueHandlers);
        }

        {
            HandlerExceptionResolverComposite handlerExceptionResolverComposite = applicationContext.getBean(HandlerExceptionResolverComposite.class);
            List<HandlerExceptionResolver> exceptionResolvers = handlerExceptionResolverComposite.getExceptionResolvers();

            for (HandlerExceptionResolver exceptionResolver : exceptionResolvers) {
                if (exceptionResolver instanceof ExceptionHandlerExceptionResolver) {
                    ExceptionHandlerExceptionResolver exceptionHandlerExceptionResolver = (ExceptionHandlerExceptionResolver) exceptionResolver;
                    List<Object> bodyAdvice = newList(requestResponseBodyAdvice, findRequestResponseBodyAdvice(exceptionHandlerExceptionResolver, "responseBodyAdvice"));

                    EncryptRequestResponseResolverHandler encryptRequestResponseResolverHandler = new EncryptRequestResponseResolverHandler(
                            exceptionHandlerExceptionResolver.getMessageConverters(),
                            negotiationManager,
                            bodyAdvice,
                            objectMapper,
                            webEncryptProperties,
                            encryptorKeyExtractor
                    );
                    set(() -> Optional.ofNullable(exceptionHandlerExceptionResolver.getArgumentResolvers())
                            .map(HandlerMethodArgumentResolverComposite::getResolvers)
                            .orElseGet(ArrayList::new), encryptRequestResponseResolverHandler, exceptionHandlerExceptionResolver::setArgumentResolvers);
                    set(() -> Optional.ofNullable(exceptionHandlerExceptionResolver.getReturnValueHandlers())
                            .map(HandlerMethodReturnValueHandlerComposite::getHandlers)
                            .orElseGet(ArrayList::new), encryptRequestResponseResolverHandler, exceptionHandlerExceptionResolver::setReturnValueHandlers);
                }
            }
        }
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }


    private <T> void set(Supplier<List<T>> supplier, T t, Consumer<List<T>> consumer) {
        List<T> arrayList = new ArrayList<>();
        supplier.get().forEach(o -> {
            if (o instanceof RequestResponseBodyMethodProcessor) {
                arrayList.add(t);
            }
            arrayList.add(o);
        });
        consumer.accept(Collections.unmodifiableList(arrayList));
    }

    private List<Object> newList(List<Object> first, List<Object> second) {
        HashSet<Object> objects = new HashSet<>(first);
        objects.addAll(second);
        return new ArrayList<>(objects);
    }

    private <T> List<Object> findRequestResponseBodyAdvice(T t, String fieldName) {
        List<Object> list = new ArrayList<>();
        Field field = ReflectionUtils.findField(t.getClass(), fieldName);
        if (Objects.nonNull(field)) {
            ReflectionUtils.makeAccessible(field);
            Object obj = ReflectionUtils.getField(field, t);
            if (obj instanceof List) {
                list.addAll((List<?>) obj);
            }
        }
        return list;
    }
}
