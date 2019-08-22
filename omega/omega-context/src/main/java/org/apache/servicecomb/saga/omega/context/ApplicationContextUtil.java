package org.apache.servicecomb.saga.omega.context;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class ApplicationContextUtil implements ApplicationContextAware {
    private static ApplicationContext context;

    public void setApplicationContext(ApplicationContext context) throws BeansException {
        setContext(context);
    }

    private static void setContext(ApplicationContext context) {
        ApplicationContextUtil.context = context;
    }

    public static ApplicationContext getApplicationContext() {
        return context;
    }

}
