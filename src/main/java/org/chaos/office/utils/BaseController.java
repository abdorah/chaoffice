package org.chaos.office.utils;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Stack;
import lombok.Getter;

@Singleton
public class BaseController {

  @Inject @Getter ViewManager viewManager;

  private static @Getter Stack<BaseController> viewStack = new Stack<>();

  protected void initialize() {}
}
