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
import javassist.CtNewMethod;
import javassist.NotFoundException;

public class Agent {


  private static final String CACHE_ASPECT_SUPPORT = "org.springframework.cache.interceptor.CacheAspectSupport";

  // Alternative implementation of generateKey() (Simplified version as in Spring 4.1)
  private static final String GENERATE_KEY_REPLACEMENT =
      "private Object generateKey41(org.springframework.cache.interceptor.CacheAspectSupport.CacheOperationContext context, Object result) {\n"
   +  "    Object key = context.generateKey(result);\n"
   +  "    if (key == null) {\n"
   +  "      throw new IllegalArgumentException(\"Null key returned for cache operation (maybe you are \" +\n"
   +  "          \"using named params on classes without debug info?) \");\n"
   +  "    }\n"
   +  "    return key;\n"
   +  "  }";

  private Agent() {}

  public static void agentmain(String agentArgs, Instrumentation inst) {
    premain(agentArgs, inst);

    Class<?>[] classes = Arrays.stream(inst.getAllLoadedClasses())
        .filter(c -> CACHE_ASPECT_SUPPORT.equals(c.getName()))
        .toArray(Class[]::new);

    if (classes.length == 0) {
      return;
    }

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
        CtMethod generateKeyReplacement = CtNewMethod.make(GENERATE_KEY_REPLACEMENT, cacheAspectSupport);
        CtMethod generateKeyMethod = cacheAspectSupport.getDeclaredMethod("generateKey");
        cacheAspectSupport.removeMethod(generateKeyMethod);
        cacheAspectSupport.addMethod(generateKeyReplacement);

        byte[] bytecode = cacheAspectSupport.toBytecode();
        cacheAspectSupport.detach();
        return bytecode;
      } catch (NotFoundException | CannotCompileException | IOException e) {
        e.printStackTrace();
        throw new RuntimeException(e);
      }
    }

    return null;
  }
}
