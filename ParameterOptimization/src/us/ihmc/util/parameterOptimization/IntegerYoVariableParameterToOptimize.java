package us.ihmc.util.parameterOptimization;

import com.yobotics.simulationconstructionset.IntegerYoVariable;

public class IntegerYoVariableParameterToOptimize extends IntegerParameterToOptimize
{
   private final IntegerYoVariable yoVariable;
   
   public IntegerYoVariableParameterToOptimize(int min, int max, IntegerYoVariable yoVariable, ListOfParametersToOptimize listOfParametersToOptimize)
   {
      super(yoVariable.getName(), min, max, listOfParametersToOptimize);
      this.yoVariable = yoVariable;
   }
   
   public void setCurrentValueGivenZeroToOne(double zeroToOne)
   {
      super.setCurrentValueGivenZeroToOne(zeroToOne);
      yoVariable.set(this.getCurrentValue());
   }
   
   public void setCurrentValue(int newValue)
   {
      super.setCurrentValue(newValue);
      yoVariable.set(newValue);
   }

}
