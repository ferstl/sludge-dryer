package com.github.ferstl.sludgedrier;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;
import java.util.Arrays;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

public class Agent {


  private static final String CACHE_ASPECT_SUPPORT = "org.springframework.cache.interceptor.CacheAspectSupport";
  private static final String CACHE_OPERATION_CONTEXT = "org.springframework.cache.interceptor.CacheAspectSupport.CacheOperationContext";

  // Implementation of generateKey() in Spring 4.1
  private static final String GENERATE_KEY_41 =
      "Object key = context.generateKey(result);\n"
     + "    if (key == null) {\n"
     + "      throw new IllegalArgumentException(\"Null key returned for cache operation (maybe you are \" +\n"
     + "          \"using named params on classes without debug info?) \" + context.metadata.operation);\n"
     + "    }\n"
     + "    if (logger.isTraceEnabled()) {\n"
     + "      logger.trace(\"Computed cache key \" + key + \" for operation \" + context.metadata.operation);\n"
     + "    }\n"
     + "    return key;";

  private Agent() {}

  public static void agentmain(String agentArgs, Instrumentation inst) {
    premain(agentArgs, inst);

    Class<?>[] classes = Arrays.stream(inst.getAllLoadedClasses())
        .filter(c -> CACHE_ASPECT_SUPPORT.equals(c.getName()))
        .toArray(Class[]::new);

    try {
      inst.retransformClasses(classes);
    } catch (UnmodifiableClassException e) {
      System.err.println("Unmodifiable class");
      e.printStackTrace();
    }
  }

  public static void premain(String agentArgs, Instrumentation inst) {
    inst.addTransformer(Agent::transformGenerateKeyMethod, true);
  }

  static byte[] transformGenerateKeyMethod(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
    if ("org/springframework/cache/interceptor/CacheAspectSupport".equals(className)) {
      ClassPool classPool = ClassPool.getDefault();
      try {
        CtClass cacheAspectSupport = classPool.get(CACHE_ASPECT_SUPPORT);
        CtMethod generateKeyMethod = cacheAspectSupport.getDeclaredMethod(
            "generateKey",
            new CtClass[] {
                classPool.get(CACHE_OPERATION_CONTEXT),
                classPool.get("java.lang.Object")});

        generateKeyMethod.setBody(GENERATE_KEY_41)
        ;
        byte[] bytecode = cacheAspectSupport.toBytecode();
        cacheAspectSupport.detach();
        return bytecode;
      } catch (NotFoundException | CannotCompileException | IOException e) {
        throw new RuntimeException(e);
      }
    }

    return null;
  }
}
