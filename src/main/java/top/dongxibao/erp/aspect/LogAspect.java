package top.dongxibao.erp.aspect;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import top.dongxibao.erp.annotation.Log;
import top.dongxibao.erp.entity.system.SysLog;
import top.dongxibao.erp.mapper.system.SysLogMapper;
import top.dongxibao.erp.util.IpUtils;
import top.dongxibao.erp.util.JsonUtils;
import top.dongxibao.erp.util.ServletUtils;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

/**
 * 操作日志记录处理
 *
 * @author Dongxibao
 * @date 2020-11-27
 */
@Slf4j
@Aspect
@Component
public class LogAspect {

    @Autowired
    private SysLogMapper interfaceLogMapper;

    /**
     * 配置织入点
     */
    @Pointcut("@annotation(top.dongxibao.erp.annotation.Log)")
    public void logPointCut() {
    }

    /**
     * 处理完请求后执行
     *
     * @param joinPoint 切点
     */
    @AfterReturning(pointcut = "logPointCut()", returning = "jsonResult")
    public void doAfterReturning(JoinPoint joinPoint, Object jsonResult) {
        handleLog(joinPoint, null, jsonResult);
    }

    /**
     * 拦截异常操作
     *
     * @param joinPoint 切点
     * @param e         异常
     */
    @AfterThrowing(value = "logPointCut()", throwing = "e")
    public void doAfterThrowing(JoinPoint joinPoint, Exception e) {
        handleLog(joinPoint, e, null);
    }

    protected void handleLog(final JoinPoint joinPoint, final Exception e, Object jsonResult) {
        try {
            // 获得注解
            Log controllerLog = getAnnotationLog(joinPoint);
            if (controllerLog == null) {
                return;
            }
            // *========数据库 接口日志=========*//
            SysLog sysLog = new SysLog();
            sysLog.setBusinessType(controllerLog.businessType().ordinal());
            sysLog.setStatus(1);
            // 请求的地址
            String ip = IpUtils.getIpAddr(ServletUtils.getRequest());
            sysLog.setRequestIp(ip);
            // 返回参数
            if (jsonResult != null) {
                sysLog.setResultParam(JsonUtils.obj2Json(jsonResult));
            }
            if (e != null) {
                sysLog.setStatus(0);
                sysLog.setMessage(StringUtils.substring(e.getMessage(), 0, 190));
            }
            // 设置方法名称
            String className = joinPoint.getTarget().getClass().getName();
            String methodName = joinPoint.getSignature().getName();
            sysLog.setMethod(className + "." + methodName + "()");
            // 设置请求方式
            if (ServletUtils.getRequest() != null) {
                sysLog.setRequestMethod(ServletUtils.getRequest().getMethod());
            }
            // 处理设置注解上的参数
            getControllerMethodDescription(joinPoint, controllerLog, sysLog);
            // 保存数据库
            interfaceLogMapper.insert(sysLog);
        } catch (Exception exp) {
            // 记录本地异常日志
            log.error("doAfterThrowing 异常信息:{};{}", exp, exp.getMessage());
        }
    }

    /**
     * 获取注解中对方法的描述信息 用于Controller层注解
     *
     * @param log    日志
     * @param sysLog 接口日志
     * @throws Exception
     */
    public void getControllerMethodDescription(JoinPoint joinPoint, Log log, SysLog sysLog) throws Exception {
        // 设置标题
        sysLog.setModuleCode(log.title());
        // 是否需要保存request，参数和值
        if (log.isSaveRequestData()) {
            // 获取参数的信息，传入到数据库中。
            setRequestValue(joinPoint, sysLog);
        }
    }

    /**
     * 获取请求的参数，放到log中
     *
     * @param sysLog 操作日志
     */
    private void setRequestValue(JoinPoint joinPoint, SysLog sysLog) {
        String requestMethod = sysLog.getRequestMethod();
        if (HttpMethod.PUT.name().equals(requestMethod) || HttpMethod.POST.name().equals(requestMethod) && joinPoint.getArgs() != null) {
            String params = argsArrayToString(joinPoint.getArgs());
            sysLog.setRequestParam(StringUtils.substring(params, 0, 3000));
        } else {
            if (ServletUtils.getRequest() != null && ServletUtils.getRequest().getQueryString() != null) {
                String paramsMap = ServletUtils.getRequest().getQueryString();
                sysLog.setRequestParam(StringUtils.substring(paramsMap, 0, 3000));
            }
        }
    }

    /**
     * 是否存在注解，如果存在就获取
     */
    private Log getAnnotationLog(JoinPoint joinPoint) throws Exception {
        Signature signature = joinPoint.getSignature();
        MethodSignature methodSignature = (MethodSignature) signature;
        Method method = methodSignature.getMethod();

        if (method != null) {
            return method.getAnnotation(Log.class);
        }
        return null;
    }

    /**
     * 参数拼装
     */
    private String argsArrayToString(Object[] paramsArray) {
        String params = "";
        if (paramsArray != null && paramsArray.length > 0) {
            for (int i = 0; i < paramsArray.length; i++) {
                if (!isFilterObject(paramsArray[i])) {
                    Object jsonObj = JsonUtils.obj2Json(paramsArray[i]);
                    params += jsonObj.toString() + " ";
                }
            }
        }
        return params.trim();
    }

    /**
     * 判断是否需要过滤的对象。
     *
     * @param o 对象信息。
     * @return 如果是需要过滤的对象，则返回true；否则返回false。
     */
    public boolean isFilterObject(final Object o) {
        return o instanceof MultipartFile || o instanceof HttpServletRequest || o instanceof HttpServletResponse;
    }
}
