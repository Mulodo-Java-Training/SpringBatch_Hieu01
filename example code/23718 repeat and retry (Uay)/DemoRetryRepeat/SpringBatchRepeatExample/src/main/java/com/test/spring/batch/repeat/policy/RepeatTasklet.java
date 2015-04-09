package com.test.spring.batch.repeat.policy;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

 
public class RepeatTasklet implements Tasklet {
 
  private RepeatPolicy repeatPolicy;
 
  public RepeatStatus execute(StepContribution stepContribution,ChunkContext chunkContext)
      throws Exception {
 
    System.out.println("Current element: " + repeatPolicy.getCurrentElement());
    System.out.println("size element = "+ repeatPolicy.getElements().size());
    return RepeatStatus.FINISHED;
 
  }
 
  public RepeatPolicy getRepeatPolicy() {
    return repeatPolicy;
  }
 
  public void setRepeatPolicy(RepeatPolicy repeatPolicy) {
    this.repeatPolicy = repeatPolicy;
  }
 
}
