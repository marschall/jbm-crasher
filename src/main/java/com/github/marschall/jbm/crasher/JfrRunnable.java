package com.github.marschall.jbm.crasher;


public final class JfrRunnable implements Runnable {
  
  public static void main(String[] args) {
    new JfrRunnable().run();
  }

  @Override
  public void run() {
    RunnableEvent event = new RunnableEvent();
    event.setRunnableClassName("JfrRunnable");
    event.begin();
    event.end();
    event.commit();
  }
}