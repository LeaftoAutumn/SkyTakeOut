package com.sky.aspect;

import com.sky.annotation.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 自定义切面，用于公共字段自动填充
 */
@Aspect
@Component
@Slf4j
public class AutoFillAspect {

    /**
     * 切入点
     * 匹配 com.sky.mapper 包下的所有类的所有方法并且方法上有 @AutoFill 注解
     */
    @Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)")
    public void autoFillPointcut() {}

    /**
     * 前置通知
     * 在方法执行前执行g=公共字段自动填充
     * @param joinPoint 切入点
     */
    @Before("autoFillPointcut()")
    public void before(JoinPoint joinPoint) {
        log.info("开始进行公共字段自动填充...");

        // 获取当前执行的方法签名对象
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();

        // 通过反射获取方法上的 @AutoFill 注解
        AutoFill autoFill = signature.getMethod().getAnnotation(AutoFill.class);
        // 获取数据库操作类型
        OperationType operationType = autoFill.value();

        // 获取当前方法的参数---实体对象
        Object[] args = joinPoint.getArgs();
        if (args.length == 0 || args[0] == null) {
            log.warn("当前方法没有参数，无法进行公共字段自动填充");
            return;
        }
        Object entity = args[0];

        // 准备公共字段的值
        LocalDateTime now = LocalDateTime.now();
        Long userId = BaseContext.getCurrentId();

        // 根据数据库操作类型通过反射进行公共字段的填充
        switch (operationType) {
            case INSERT:
                // 插入操作，填充创建时间和创建人、更新时间和更新人
                try {
                    // 通过反射获取方法对象
                    Method setCreateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class);
                    Method setCreateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_USER, Long.class);
                    Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                    Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);

                    // 通过反射调用方法
                    setCreateTime.invoke(entity, now);
                    setCreateUser.invoke(entity, userId);
                    setUpdateTime.invoke(entity, now);
                    setUpdateUser.invoke(entity, userId);
                } catch (Exception e) {
                    log.error("插入操作公共字段自动填充失败: {}", e.getMessage());
                }

                break;
            case UPDATE:
                // 更新操作，填充更新时间和更新人
                try {
                    // 通过反射获取方法对象
                    Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                    Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);

                    // 通过反射调用方法
                    setUpdateTime.invoke(entity, now);
                    setUpdateUser.invoke(entity, userId);
                } catch (Exception e) {
                    log.error("更新操作公共字段自动填充失败: {}", e.getMessage());
                }
                
                break;
            default:
                log.warn("不支持切入的数据库操作类型: {}", operationType);
        }
    }
}
