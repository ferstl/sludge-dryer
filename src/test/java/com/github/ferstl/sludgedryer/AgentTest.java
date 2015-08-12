package com.github.ferstl.sludgedryer;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.cache.interceptor.CacheAspectSupport;
import org.springframework.cache.interceptor.CacheInterceptor;
import org.springframework.util.ReflectionUtils;
import com.sun.tools.attach.VirtualMachine;


public class AgentTest {

  @Test
  @Ignore("Ugly hack to see if the modified method can be called at all")
  public void test() throws Throwable {
    String pid = getPid();
    VirtualMachine vm = VirtualMachine.attach(pid);

    vm.loadAgent("target/sludge-dryer.jar");
    vm.detach();


    CacheInterceptor interceptor = new CacheInterceptor();

    Class<?> opCtx = Class.forName("org.springframework.cache.interceptor.CacheAspectSupport$CacheOperationContext");
    Method generateKeyMethod = ReflectionUtils.findMethod(CacheAspectSupport.class, "generateKey", opCtx, Object.class);
    generateKeyMethod.setAccessible(true);
    ReflectionUtils.invokeMethod(generateKeyMethod, interceptor, null, new Object());
  }

  private static String getPid() {
    String procName = ManagementFactory.getRuntimeMXBean().getName();
    return procName.substring(0, procName.indexOf('@'));
  }

}
