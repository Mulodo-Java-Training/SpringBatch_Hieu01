package com.test.spring.batch.repeat.policy;

import java.util.List;
import org.springframework.batch.repeat.CompletionPolicy;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.RepeatStatus;
 
public class RepeatPolicy implements CompletionPolicy {
 
  private List<String> elements;
  private String currentElement;
 
  public boolean isComplete(RepeatContext repeatContext) {
    return elements.size() == 0;
  }
 
  public boolean isComplete(RepeatContext repeatContext,
      RepeatStatus repeatStatus) {
    return elements.size() == 0;
  }
 
  public RepeatContext start(RepeatContext repeatContext) {
    return repeatContext;
  }
 
  public void update(RepeatContext repeatContext) {
    currentElement = elements.remove(0);
  }
 
  public List<String> getElements() {
    return elements;
  }
 
  public void setElements(List<String> elements) {
    this.elements = elements;
  }
 
  public String getCurrentElement() {
    return currentElement;
  }
 
}