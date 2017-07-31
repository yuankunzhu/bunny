package org.rabix.engine.store.model.scatter;

import org.rabix.bindings.BindingException;
import org.rabix.bindings.model.dag.DAGNode;
import org.rabix.engine.store.model.scatter.impl.ScatterCartesian;
import org.rabix.engine.store.model.scatter.impl.ScatterDotproduct;

import com.google.common.base.Preconditions;

public class ScatterStrategyFactory {

  public ScatterStrategy create(DAGNode dagNode) throws BindingException {
    Preconditions.checkNotNull(dagNode);
    
    switch (dagNode.getScatterMethod()) {
    case dotproduct:
      return new ScatterDotproduct(dagNode);
    case crossproduct:
      return new ScatterCartesian(dagNode);
    default:
      throw new BindingException("Scatter method " + dagNode.getScatterMethod() + " is not supported.");
    }
  }
  
}
