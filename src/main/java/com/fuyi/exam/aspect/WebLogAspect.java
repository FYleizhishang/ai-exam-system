package com.fuyi.exam.aspect;

import com.google.gson.Gson;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * 全局接口日志切面 (商用级性能监控)
 * 作用：非侵入式地记录所有接口的访问信息和耗时，完全不需要修改原有 Controller 的代码
 */
@Aspect
@Component
public class WebLogAspect {

    private static final Logger log = LoggerFactory.getLogger(WebLogAspect.class);
    // 复用你项目里已经有的 Gson 库来做 JSON 序列化
    private final Gson gson = new Gson();

    /**
     * 定义切点：拦截 com.fuyi.exam.controller 包及其所有子包下的类的所有方法
     */
    @Pointcut("execution(* com.fuyi.exam.controller..*.*(..))")
    public void webLog() {
    }

    /**
     * 环绕通知：在方法执行前后动态植入逻辑
     */
    @Around("webLog()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        // 记录接口调用的开始时间
        long startTime = System.currentTimeMillis();

        // 获取当前 HTTP 请求对象
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

        if (request != null) {
            // 打印请求头部和入参的详细信息
            log.info("========================================== Start ==========================================");
            log.info("URL            : {}", request.getRequestURL().toString());
            log.info("HTTP Method    : {}", request.getMethod());
            log.info("IP             : {}", request.getRemoteAddr());
            log.info("Class Method   : {}.{}", joinPoint.getSignature().getDeclaringTypeName(), joinPoint.getSignature().getName());

            // 打印请求参数 (加 try-catch 防止某些特殊对象无法序列化导致业务报错)
            try {
                log.info("Request Args   : {}", gson.toJson(joinPoint.getArgs()));
            } catch (Exception e) {
                log.info("Request Args   : [无法序列化的参数]");
            }
        }

        // 🔥 执行原有的 Controller 核心业务逻辑 (绝对不影响你的原有代码)
        Object result = joinPoint.proceed();

        if (request != null) {
            // 打印接口返回的数据
            try {
                log.info("Response       : {}", gson.toJson(result));
            } catch (Exception e) {
                log.info("Response       : [无法序列化的返回值]");
            }

            // 计算并打印接口实际耗时
            long timeTaken = System.currentTimeMillis() - startTime;
            log.info("Time Taken     : {} ms", timeTaken);
            log.info("=========================================== End ===========================================");
        }

        return result;
    }
}