package org.chaos.office.controller;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Stack;
import lombok.Getter;
import org.chaos.office.utils.View;

@Singleton
public class BaseController {

  @Inject @Getter View view;

  private static @Getter Stack<BaseController> viewStack = new Stack<>();

  protected void initialize() {}
}
