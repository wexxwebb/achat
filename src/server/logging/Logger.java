package server.logging;

import server.Server;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

public class Logger implements InvocationHandler {

    private Server server;
    private boolean logging = false;
    private AtomicInteger messageCount;

    public Logger(Server server, AtomicInteger messageCount) {
        this.server = server;
        this.messageCount = messageCount;
    }

    public void setLogging(boolean logging) {
        this.logging = logging;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (logging && method.getName().intern() == "print".intern()) {
            for (Annotation a : server.getClass().getAnnotations()) {
                if (a.annotationType().getCanonicalName().intern() == Logging.class.getCanonicalName().intern()) {
                    if (messageCount.incrementAndGet() % 10 == 0) {
                        System.out.println("Ten messages!!!!");
                    }
                }
            }
        }
        return method.invoke(server, args);
    }
}
