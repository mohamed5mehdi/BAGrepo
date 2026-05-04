package com.pfe.gestionsachat.event;

import com.pfe.gestionsachat.model.DaHeader;
import com.pfe.gestionsachat.model.User;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class WorkflowNotificationEvent extends ApplicationEvent {
    private final DaHeader da;
    private final String message;
    private final User targetUser;

    public WorkflowNotificationEvent(Object source, DaHeader da, String message, User targetUser) {
        super(source);
        this.da = da;
        this.message = message;
        this.targetUser = targetUser;
    }
}
