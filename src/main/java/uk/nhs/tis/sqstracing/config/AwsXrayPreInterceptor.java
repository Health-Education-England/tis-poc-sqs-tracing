package uk.nhs.tis.sqstracing.config;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.entities.TraceHeader;
import com.amazonaws.xray.entities.TraceID;
import java.util.Optional;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * This is interceptor comes before the standard XRay interceptor and is where the initial segment will be set up.
 */
@Slf4j
@Aspect
@Order(0)
@Component
public class AwsXrayPreInterceptor {

  @Pointcut(
      "@within(com.amazonaws.xray.spring.aop.XRayEnabled) && bean(*Listener)")
  public void xrayEnabledListeners() {

  }

  /**
   * TODO: complete re-write, needs to check sample rate etc. - can copy approach in {@link com.amazonaws.xray.javax.servlet.AWSXRayServletFilter#preFilter(ServletRequest, ServletResponse)}.
   * @param pjp the proceeding join point
   * @return
   * @throws Throwable
   */
  @Around("xrayEnabledListeners()")
  public Object traceAroundMethods(ProceedingJoinPoint pjp) throws Throwable {
    Object[] args = pjp.getArgs();

    for (Object arg : args) {
      if (arg instanceof String argString) {
        TraceHeader traceHeader = TraceHeader.fromString(argString);
        TraceID traceId = traceHeader.getRootTraceId();

        if (traceId != null) {
          log.info("Trace ID: {}", traceId);
          String parentId = traceHeader.getParentId();

          try (var segment = AWSXRay.beginSegment("tis-poc-sqs-tracing", traceId, parentId)) {
            return pjp.proceed();
          }
        }
      }
    }

    return pjp.proceed();
  }
}
