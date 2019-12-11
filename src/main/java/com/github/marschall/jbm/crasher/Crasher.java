package com.github.marschall.jbm.crasher;

import java.io.File;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jboss.modules.JDKModuleFinder;
import org.jboss.modules.LocalModuleFinder;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleFinder;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;

public class Crasher {

  private volatile ClassLoader nextLoader;
  
  void crash() throws ReflectiveOperationException {
    
    int numberOfThreads = Runtime.getRuntime().availableProcessors();
    if (numberOfThreads <= 1) {
      throw new IllegalStateException("requies more than one thread");
    }
    ExecutorService threadPool = Executors.newFixedThreadPool(numberOfThreads);
    CyclicBarrier cyclicBarrier = new CyclicBarrier(numberOfThreads, () -> {
      this.nextLoader = newModuleClassLoader();
    });
    for (int i = 0; i < numberOfThreads; i++) {
      threadPool.submit(new LoadingRunnable(cyclicBarrier));
    }
    threadPool.shutdown();
  }

  public static void main(String[] args) throws ModuleLoadException, ReflectiveOperationException {
    new Crasher().crash();
  }

  private ClassLoader newModuleClassLoader() {
    Module module;
    try {
      module = newModule();
    } catch (ModuleLoadException e) {
      throw e.toError();
    }
    return module.getClassLoader();
  }

  private static Module newModule() throws ModuleLoadException {
//    ModuleFinder[] finders = new ModuleFinder[]{new MemoryModuleFinder(), JDKModuleFinder.getInstance()};
    File localModule = new File("/Users/marschall/git/jbm-crasher/src/main/resources");
    ModuleFinder[] finders = new ModuleFinder[]{new LocalModuleFinder(new File[] {localModule}), JDKModuleFinder.getInstance()};
    ModuleLoader loader = new ModuleLoader(finders);
    Module module = loader.loadModule("jbm-crasher");
    return module;
  }

  static Runnable loadJfrRunnable(ClassLoader classLoader) {
    try {
      Class<?> runnableClass = Class.forName(MemoryModuleFinder.JFR_RUNNABLE, false, classLoader);
      return runnableClass.asSubclass(Runnable.class).getConstructor().newInstance();
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException("could not load runnable", e);
    }
  }

  final class LoadingRunnable implements Runnable {

    private final CyclicBarrier barrier;

    LoadingRunnable(CyclicBarrier barrier) {
      this.barrier = barrier;
    }

    @Override
    public void run() {
      while (true) {
        try {
          this.barrier.await();
          Runnable runnable = loadJfrRunnable(Crasher.this.nextLoader);
          runnable.run();
        } catch (Throwable e) {
          e.printStackTrace(System.err);
          return;
        }
      }
    }

  }

}
