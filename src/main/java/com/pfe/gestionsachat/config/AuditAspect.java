package com.pfe.gestionsachat.config;

import com.pfe.gestionsachat.model.AuditLog;
import com.pfe.gestionsachat.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Aspect
@Component
public class AuditAspect {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Pointcut("execution(* com.pfe.gestionsachat.service.*.process*(..)) || execution(* com.pfe.gestionsachat.service.*.validate*(..))")
    public void businessMethods() {}

    @AfterReturning(pointcut = "businessMethods()", returning = "result")
    public void logAudit(JoinPoint joinPoint, Object result) {
        HttpServletRequest request = null;
        String ipAddress = "0.0.0.0";
        try {
            request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            ipAddress = request.getRemoteAddr();
        } catch (Exception e) {
            // Fallback for non-web contexts (e.g. Scheduled tasks)
        }
        
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        Object[] args = joinPoint.getArgs();

        AuditLog log = new AuditLog();
        log.setAction(methodName);
        log.setEntite(className);
        log.setDateAction(LocalDateTime.now());
        log.setIpAddress(ipAddress);
        
        // Capture des valeurs (Old/New) simplifiée via les arguments et le résultat
        log.setValeurAvant("Args: " + java.util.Arrays.toString(args));
        log.setValeurApres(result != null ? "Result: " + result.toString() : "Success");
        
        // Tentative de récupération de l'utilisateur dans les arguments (convention: userId ou acheteurId)
        for (Object arg : args) {
            if (arg instanceof Integer) {
                // On pourrait chercher l'utilisateur par ID ici si on voulait être précis
                // Mais l'important est d'avoir la trace technique.
            }
        }
        
        auditLogRepository.save(log);
    }
}
