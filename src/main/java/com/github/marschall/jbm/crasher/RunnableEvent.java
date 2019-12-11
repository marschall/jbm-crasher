package com.github.marschall.jbm.crasher;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;

@Label("Runnable")
@Description("An executed Runnable")
@Category("Custom JFR Events")
class RunnableEvent extends Event {

  @Label("Class Name")
  @Description("The name of the Runnable class")
  private String runnableClassName;

  String getRunnableClassName() {
    return this.runnableClassName;
  }

  void setRunnableClassName(String operationName) {
    this.runnableClassName = operationName;
  }

}