package us.ihmc.commonWalkingControlModules.momentumBasedController.optimization;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import us.ihmc.convexOptimization.quadraticProgram.ActiveSetQPSolverWithInactiveVariablesInterface;
import us.ihmc.robotics.linearAlgebra.MatrixTools;
import us.ihmc.robotics.screwTheory.Wrench;
import us.ihmc.robotics.time.ExecutionTimer;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.yoVariables.variable.YoDouble;
import us.ihmc.yoVariables.variable.YoFrameVector3D;
import us.ihmc.yoVariables.variable.YoInteger;

public class InverseDynamicsQPSolver
{
   private static final boolean SETUP_WRENCHES_CONSTRAINT_AS_OBJECTIVE = true;

   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());

   private final ExecutionTimer qpSolverTimer = new ExecutionTimer("qpSolverTimer", 0.5, registry);

   private final YoFrameVector3D wrenchEquilibriumForceError;
   private final YoFrameVector3D wrenchEquilibriumTorqueError;

   private final YoBoolean addRateRegularization = new YoBoolean("AddRateRegularization", registry);
   private final ActiveSetQPSolverWithInactiveVariablesInterface qpSolver;

   private final DenseMatrix64F solverInput_H;
   private final DenseMatrix64F solverInput_f;

   private final DenseMatrix64F solverInput_H_previous;
   private final DenseMatrix64F solverInput_f_previous;

   private final DenseMatrix64F solverInput_Aeq;
   private final DenseMatrix64F solverInput_beq;
   private final DenseMatrix64F solverInput_Ain;
   private final DenseMatrix64F solverInput_bin;

   private final DenseMatrix64F solverInput_lb;
   private final DenseMatrix64F solverInput_ub;

   private final DenseMatrix64F solverInput_lb_previous;
   private final DenseMatrix64F solverInput_ub_previous;

   private final DenseMatrix64F solverInput_activeIndices;

   private final DenseMatrix64F solverOutput;
   private final DenseMatrix64F solverOutput_jointAccelerations;
   private final DenseMatrix64F solverOutput_rhos;

   private final YoInteger numberOfActiveVariables = new YoInteger("numberOfActiveVariables", registry);
   private final YoInteger numberOfIterations = new YoInteger("numberOfIterations", registry);
   private final YoInteger numberOfEqualityConstraints = new YoInteger("numberOfEqualityConstraints", registry);
   private final YoInteger numberOfInequalityConstraints = new YoInteger("numberOfInequalityConstraints", registry);
   private final YoInteger numberOfConstraints = new YoInteger("numberOfConstraints", registry);
   private final YoDouble jointAccelerationRegularization = new YoDouble("jointAccelerationRegularization", registry);
   private final YoDouble jointJerkRegularization = new YoDouble("jointJerkRegularization", registry);
   private final YoDouble jointTorqueWeight = new YoDouble("jointTorqueWeight", registry);
   private final DenseMatrix64F regularizationMatrix;

   private final DenseMatrix64F tempJtW;

   private final int numberOfDoFs;
   private final int rhoSize;
   private final int problemSize;
   private final boolean hasFloatingBase;
   private boolean hasWrenchesEquilibriumConstraintBeenSetup = false;

   private boolean resetActiveSet = false;
   private boolean useWarmStart = false;
   private int maxNumberOfIterations = 100;

   private final double dt;

   public InverseDynamicsQPSolver(ActiveSetQPSolverWithInactiveVariablesInterface qpSolver, int numberOfDoFs, int rhoSize, boolean hasFloatingBase,
                                  double dt, YoVariableRegistry parentRegistry)
   {
      this.qpSolver = qpSolver;
      this.numberOfDoFs = numberOfDoFs;
      this.rhoSize = rhoSize;
      this.hasFloatingBase = hasFloatingBase;
      this.problemSize = numberOfDoFs + rhoSize;
      this.dt = dt;

      addRateRegularization.set(false);

      solverInput_H = new DenseMatrix64F(problemSize, problemSize);
      solverInput_f = new DenseMatrix64F(problemSize, 1);

      solverInput_H_previous = new DenseMatrix64F(problemSize, problemSize);
      solverInput_f_previous = new DenseMatrix64F(problemSize, 1);

      solverInput_Aeq = new DenseMatrix64F(0, problemSize);
      solverInput_beq = new DenseMatrix64F(0, 1);
      solverInput_Ain = new DenseMatrix64F(0, problemSize);
      solverInput_bin = new DenseMatrix64F(0, 1);

      solverInput_lb = new DenseMatrix64F(problemSize, 1);
      solverInput_ub = new DenseMatrix64F(problemSize, 1);

      solverInput_lb_previous = new DenseMatrix64F(problemSize, 1);
      solverInput_ub_previous = new DenseMatrix64F(problemSize, 1);

      CommonOps.fill(solverInput_lb, Double.NEGATIVE_INFINITY);
      CommonOps.fill(solverInput_ub, Double.POSITIVE_INFINITY);

      solverInput_activeIndices = new DenseMatrix64F(problemSize, 1);
      CommonOps.fill(solverInput_activeIndices, 1.0);

      solverOutput = new DenseMatrix64F(problemSize, 1);
      solverOutput_jointAccelerations = new DenseMatrix64F(numberOfDoFs, 1);
      solverOutput_rhos = new DenseMatrix64F(rhoSize, 1);

      tempJtW = new DenseMatrix64F(problemSize, problemSize);

      jointAccelerationRegularization.set(0.005);
      jointJerkRegularization.set(0.1);
      jointTorqueWeight.set(0.001);
      regularizationMatrix = new DenseMatrix64F(problemSize, problemSize);

      for (int i = 0; i < numberOfDoFs; i++)
         regularizationMatrix.set(i, i, jointAccelerationRegularization.getDoubleValue());
      double defaultRhoRegularization = 0.00001;
      for (int i = numberOfDoFs; i < problemSize; i++)
         regularizationMatrix.set(i, i, defaultRhoRegularization);

      if (SETUP_WRENCHES_CONSTRAINT_AS_OBJECTIVE)
      {
         wrenchEquilibriumForceError = new YoFrameVector3D("wrenchEquilibriumForceError", null, registry);
         wrenchEquilibriumTorqueError = new YoFrameVector3D("wrenchEquilibriumTorqueError", null, registry);
      }
      else
      {
         wrenchEquilibriumForceError = null;
         wrenchEquilibriumTorqueError = null;
      }

      parentRegistry.addChild(registry);
   }

   public void setAccelerationRegularizationWeight(double weight)
   {
      jointAccelerationRegularization.set(weight);
   }

   public void setJerkRegularizationWeight(double weight)
   {
      jointJerkRegularization.set(weight);
   }

   public void setJointTorqueWeight(double weight)
   {
      jointTorqueWeight.set(weight);
   }

   public void setRhoRegularizationWeight(DenseMatrix64F weight)
   {
      CommonOps.insert(weight, regularizationMatrix, numberOfDoFs, numberOfDoFs);
   }

   public void setUseWarmStart(boolean useWarmStart)
   {
      this.useWarmStart = useWarmStart;
   }

   public void setMaxNumberOfIterations(int maxNumberOfIterations)
   {
      this.maxNumberOfIterations = maxNumberOfIterations;
   }

   public void notifyResetActiveSet()
   {
      this.resetActiveSet = true;
   }

   private boolean pollResetActiveSet()
   {
      boolean ret = resetActiveSet;
      resetActiveSet = false;
      return ret;
   }

   public void reset()
   {
      for (int i = 0; i < numberOfDoFs; i++)
         regularizationMatrix.set(i, i, jointAccelerationRegularization.getDoubleValue());

      solverInput_H.zero();

      solverInput_f.zero();

      solverInput_Aeq.reshape(0, problemSize);
      solverInput_beq.reshape(0, 1);

      solverInput_Ain.reshape(0, problemSize);
      solverInput_bin.reshape(0, 1);
   }

   private void addRegularization()
   {
      CommonOps.addEquals(solverInput_H, regularizationMatrix);

      if (addRateRegularization.getBooleanValue())
      {
         addJointJerkRegularization();
      }
   }

   public void resetRateRegularization()
   {
      addRateRegularization.set(false);
   }

   private void addJointJerkRegularization()
   {
      double factor = dt * dt / jointJerkRegularization.getDoubleValue();
      for (int i = 0; i < numberOfDoFs; i++)
      {
         solverInput_H.add(i, i, 1.0 / factor);
         solverInput_f.add(i, 0, -solverOutput_jointAccelerations.get(i, 0) / factor);
      }
   }

   public void addMotionInput(QPInput input)
   {
      switch (input.getConstraintType())
      {
      case OBJECTIVE:
         if (input.useWeightScalar())
            addMotionTask(input.taskJacobian, input.taskObjective, input.getWeightScalar());
         else
            addMotionTask(input.taskJacobian, input.taskObjective, input.taskWeightMatrix);
         break;
      case EQUALITY:
         addMotionEqualityConstraint(input.taskJacobian, input.taskObjective);
         break;
      case LEQ_INEQUALITY:
         addMotionLesserOrEqualInequalityConstraint(input.taskJacobian, input.taskObjective);
         break;
      case GEQ_INEQUALITY:
         addMotionGreaterOrEqualInequalityConstraint(input.taskJacobian, input.taskObjective);
         break;
      default:
         throw new RuntimeException("Unexpected constraint type: " + input.getConstraintType());
      }
   }

   public void addRhoInput(QPInput input)
   {
      switch (input.getConstraintType())
      {
      case OBJECTIVE:
         if (input.useWeightScalar())
         {
            addRhoTask(input.getTaskJacobian(), input.getTaskObjective(), input.getWeightScalar());
         }
         else
         {
            addRhoTask(input.getTaskJacobian(), input.getTaskObjective(), input.getTaskWeightMatrix());
         }
         break;
      case EQUALITY:
         addRhoEqualityConstraint(input.getTaskJacobian(), input.getTaskObjective());
         break;
      case LEQ_INEQUALITY:
         addRhoLesserOrEqualInequalityConstraint(input.getTaskJacobian(), input.getTaskObjective());
         break;
      case GEQ_INEQUALITY:
         addRhoGreaterOrEqualInequalityConstraint(input.getTaskJacobian(), input.getTaskObjective());
         break;
      default:
         throw new RuntimeException("Unexpected constraint type: " + input.getConstraintType());
      }
   }

   public void addMotionTask(DenseMatrix64F taskJacobian, DenseMatrix64F taskObjective, double taskWeight)
   {
      if (taskJacobian.getNumCols() != numberOfDoFs)
      {
         throw new RuntimeException("Motion task needs to have size macthing the DoFs of the robot.");
      }
      addTaskInternal(taskJacobian, taskObjective, taskWeight, 0);
   }

   public void addRhoTask(DenseMatrix64F taskJacobian, DenseMatrix64F taskObjective, double taskWeight)
   {
      if (taskJacobian.getNumCols() != rhoSize)
      {
         throw new RuntimeException("Rho task needs to have size macthing the number of rhos of the robot.");
      }
      addTaskInternal(taskJacobian, taskObjective, taskWeight, numberOfDoFs);
   }

   /**
    * Sets up a motion objective for the joint accelerations (qddot).
    * <p>
    *    min (J qddot - b)^T * W * (J qddot - b)
    * </p>
    * @param taskJacobian jacobian to map qddot to the objective space. J in the above equation.
    * @param taskObjective matrix of the desired objective for the rho task. b in the above equation.
    * @param taskWeight weight for the desired objective. W in the above equation. Assumed to be diagonal.
    */
   public void addMotionTask(DenseMatrix64F taskJacobian, DenseMatrix64F taskObjective, DenseMatrix64F taskWeight)
   {
      if (taskJacobian.getNumCols() != numberOfDoFs)
      {
         throw new RuntimeException("Motion task needs to have size macthing the DoFs of the robot.");
      }
      addTaskInternal(taskJacobian, taskObjective, taskWeight, 0);
   }

   /**
    * Sets up a motion objective for the generalized contact forces (rhos).
    * <p>
    *    min (J rho - b)^T * W * (J rho - b)
    * </p>
    * @param taskJacobian jacobian to map rho to the objective space. J in the above equation.
    * @param taskObjective matrix of the desired objective for the rho task. b in the above equation.
    * @param taskWeight weight for the desired objective. W in the above equation. Assumed to be diagonal.
    */
   public void addRhoTask(DenseMatrix64F taskJacobian, DenseMatrix64F taskObjective, DenseMatrix64F taskWeight)
   {
      if (taskJacobian.getNumCols() != rhoSize)
      {
         throw new RuntimeException("Rho task needs to have size macthing the number of rhos of the robot.");
      }
      addTaskInternal(taskJacobian, taskObjective, taskWeight, numberOfDoFs);
   }

   /**
    * Sets up a motion objective for the generalized contact forces (rhos).
    * <p>
    *    min (rho - b)^T * W * (rho - b)
    * </p>
    * @param taskObjective matrix of the desired objective for the rho task. b in the above equation.
    * @param taskWeight weight for the desired objective. W in the above equation. Assumed to be diagonal.
    */
   public void addRhoTask(DenseMatrix64F taskObjective, DenseMatrix64F taskWeight)
   {
      addTaskInternal(taskObjective, taskWeight, numberOfDoFs, rhoSize);
   }

   public void addMotionTask(DenseMatrix64F taskObjective, DenseMatrix64F taskWeight)
   {
      addTaskInternal(taskObjective, taskWeight, 0, numberOfDoFs);
   }

   public void addTaskInternal(DenseMatrix64F taskObjective, DenseMatrix64F taskWeight, int offset, int variables)
   {
      if (offset + variables > problemSize)
      {
         throw new RuntimeException("This task does not fit.");
      }

      MatrixTools.addMatrixBlock(solverInput_H, offset, offset, taskWeight, 0, 0, variables, variables, 1.0);
      MatrixTools.multAddBlock(-1.0, taskWeight, taskObjective, solverInput_f, offset, 0);
   }

   private void addTaskInternal(DenseMatrix64F taskJacobian, DenseMatrix64F taskObjective, DenseMatrix64F taskWeight, int offset)
   {
      int taskSize = taskJacobian.getNumRows();
      int variables = taskJacobian.getNumCols();
      if (offset + variables > problemSize)
      {
         throw new RuntimeException("This task does not fit.");
      }

      tempJtW.reshape(variables, taskSize);

      // J^T W
      CommonOps.multTransA(taskJacobian, taskWeight, tempJtW);

      // Compute: H += J^T W J
      MatrixTools.multAddBlock(tempJtW, taskJacobian, solverInput_H, offset, offset);

      // Compute: f += - J^T W Objective
      MatrixTools.multAddBlock(-1.0, tempJtW, taskObjective, solverInput_f, offset, 0);
   }

   private void addTaskInternal(DenseMatrix64F taskJacobian, DenseMatrix64F taskObjective, double taskWeight, int offset)
   {
      int variables = taskJacobian.getNumCols();
      if (offset + variables > problemSize)
      {
         throw new RuntimeException("This task does not fit.");
      }

      // Compute: H += J^T W J
      MatrixTools.multAddBlockInner(taskWeight, taskJacobian, solverInput_H, offset, offset);

      // Compute: f += - J^T W Objective
      MatrixTools.multAddBlockTransA(-taskWeight, taskJacobian, taskObjective, solverInput_f, offset, 0);
   }

   public void addMotionEqualityConstraint(DenseMatrix64F taskJacobian, DenseMatrix64F taskObjective)
   {
      if (taskJacobian.getNumCols() != numberOfDoFs)
      {
         throw new RuntimeException("Motion task needs to have size macthing the DoFs of the robot.");
      }
      addEqualityConstraintInternal(taskJacobian, taskObjective, 0);
   }

   public void addRhoEqualityConstraint(DenseMatrix64F taskJacobian, DenseMatrix64F taskObjective)
   {
      if (taskJacobian.getNumCols() != rhoSize)
      {
         throw new RuntimeException("Rho task needs to have size macthing the number of rhos of the robot.");
      }
      addEqualityConstraintInternal(taskJacobian, taskObjective, numberOfDoFs);
   }

   private void addEqualityConstraintInternal(DenseMatrix64F taskJacobian, DenseMatrix64F taskObjective, int offset)
   {
      int taskSize = taskJacobian.getNumRows();
      int variables = taskJacobian.getNumCols();
      if (offset + variables > problemSize)
      {
         throw new RuntimeException("This task does not fit.");
      }

      int previousSize = solverInput_beq.getNumRows();

      // Careful on that one, it works as long as matrices are row major and that the number of columns is not changed.
      solverInput_Aeq.reshape(previousSize + taskSize, problemSize, true);
      solverInput_beq.reshape(previousSize + taskSize, 1, true);

      CommonOps.insert(taskJacobian, solverInput_Aeq, previousSize, offset);
      CommonOps.insert(taskObjective, solverInput_beq, previousSize, offset);
   }

   public void addMotionLesserOrEqualInequalityConstraint(DenseMatrix64F taskJacobian, DenseMatrix64F taskObjective)
   {
      addMotionInequalityConstraintInternal(taskJacobian, taskObjective, 1.0);
   }

   public void addMotionGreaterOrEqualInequalityConstraint(DenseMatrix64F taskJacobian, DenseMatrix64F taskObjective)
   {
      addMotionInequalityConstraintInternal(taskJacobian, taskObjective, -1.0);
   }

   private void addRhoLesserOrEqualInequalityConstraint(DenseMatrix64F taskJacobian, DenseMatrix64F taskObjective)
   {
      addRhoInequalityConstraintInternal(taskJacobian, taskObjective, 1.0);
   }

   private void addRhoGreaterOrEqualInequalityConstraint(DenseMatrix64F taskJacobian, DenseMatrix64F taskObjective)
   {
      addRhoInequalityConstraintInternal(taskJacobian, taskObjective, -1.0);
   }

   private void addMotionInequalityConstraintInternal(DenseMatrix64F taskJacobian, DenseMatrix64F taskObjective, double sign)
   {
      addInequalityConstraintInternal(taskJacobian, taskObjective, sign, 0);
   }

   private void addRhoInequalityConstraintInternal(DenseMatrix64F taskJacobian, DenseMatrix64F taskObjective, double sign)
   {
      addInequalityConstraintInternal(taskJacobian, taskObjective, sign, numberOfDoFs);
   }

   private void addInequalityConstraintInternal(DenseMatrix64F taskJacobian, DenseMatrix64F taskObjective, double sign, int offset)
   {
      int taskSize = taskJacobian.getNumRows();
      int variables = taskJacobian.getNumCols();
      if (offset + variables > problemSize)
      {
         throw new RuntimeException("This task does not fit.");
      }

      int previousSize = solverInput_bin.getNumRows();

      // Careful on that one, it works as long as matrices are row major and that the number of columns is not changed.
      solverInput_Ain.reshape(previousSize + taskSize, problemSize, true);
      solverInput_bin.reshape(previousSize + taskSize, 1, true);

      MatrixTools.setMatrixBlock(solverInput_Ain, previousSize, offset, taskJacobian, 0, 0, taskSize, variables, sign);
      MatrixTools.setMatrixBlock(solverInput_bin, previousSize, 0, taskObjective, 0, 0, taskSize, 1, sign);
   }

   public void addTorqueMinimizationObjective(DenseMatrix64F torqueJacobian, DenseMatrix64F torqueObjective)
   {
      // Compute: H += J^T W J
      MatrixTools.multAddInner(jointTorqueWeight.getDoubleValue(), torqueJacobian, solverInput_H);

      // Compute: f += - J^T W Objective
      CommonOps.multTransA(-jointTorqueWeight.getDoubleValue(), torqueJacobian, torqueObjective, solverInput_f);
   }

   public void addTorqueMinimizationObjective(DenseMatrix64F torqueQddotJacobian, DenseMatrix64F torqueRhoJacobian, DenseMatrix64F torqueObjective)
   {
      int taskSize = torqueObjective.getNumRows();

      tempJtW.reshape(taskSize, problemSize);
      CommonOps.insert(torqueQddotJacobian, tempJtW, 0, 0);
      CommonOps.insert(torqueRhoJacobian, tempJtW, 0, numberOfDoFs);

      addTorqueMinimizationObjective(tempJtW, torqueObjective);
   }

   /**
    * Need to be called before {@link #solve()}. It sets up the constraint that ensures that the
    * solution is dynamically feasible:
    * <p>
    * <li>hDot = &sum;W<sub>ext</sub>
    * <li>A * qDDot + ADot * qDot = Q * &rho; + &sum;W<sub>user</sub> + W<sub>gravity</sub>
    * <li>-A * qDDot - ADot * qDot = - Q * &rho; - &sum;W<sub>user</sub> - W<sub>gravity</sub>
    * <li>-A * qDDot + Q * &rho; = ADot * qDot - &sum;W<sub>user</sub> - W<sub>gravity</sub>
    * <li>[-A Q] * [qDDot<sup>T</sup> &rho;<sup>T</sup>]<sup>T</sup> = ADot * qDot -
    * &sum;W<sub>user</sub> - W<sub>gravity</sub>
    * </p>
    *
    * @param centroidalMomentumMatrix refers to A in the equation.
    * @param rhoJacobian refers to Q in the equation. Q&rho; represents external wrench to be
    *           optimized for.
    * @param convectiveTerm refers to ADot * qDot in the equation.
    * @param additionalExternalWrench refers to &sum;W<sub>user</sub> in the equation. These are
    *           constant wrenches usually used for compensating for the weight of an object that the
    *           robot is holding.
    * @param gravityWrench refers to W<sub>gravity</sub> in the equation. It the wrench induced by
    *           the wieght of the robot.
    */
   public void setupWrenchesEquilibriumConstraint(DenseMatrix64F centroidalMomentumMatrix, DenseMatrix64F rhoJacobian, DenseMatrix64F convectiveTerm,
                                                  DenseMatrix64F additionalExternalWrench, DenseMatrix64F gravityWrench)
   {
      if (!hasFloatingBase)
      {
         hasWrenchesEquilibriumConstraintBeenSetup = true;
         return;
      }

      tempWrenchConstraint_RHS.set(convectiveTerm);
      CommonOps.subtractEquals(tempWrenchConstraint_RHS, additionalExternalWrench);
      CommonOps.subtractEquals(tempWrenchConstraint_RHS, gravityWrench);

      if (SETUP_WRENCHES_CONSTRAINT_AS_OBJECTIVE)
      {
         tempWrenchConstraint_J.reshape(Wrench.SIZE, problemSize);
         MatrixTools.setMatrixBlock(tempWrenchConstraint_J, 0, 0, centroidalMomentumMatrix, 0, 0, Wrench.SIZE, numberOfDoFs, -1.0);
         CommonOps.insert(rhoJacobian, tempWrenchConstraint_J, 0, numberOfDoFs);

         double weight = 150.0;
         MatrixTools.multAddInner(weight, tempWrenchConstraint_J, solverInput_H);

         CommonOps.multAddTransA(-weight, tempWrenchConstraint_J, tempWrenchConstraint_RHS, solverInput_f);
      }
      else
      {
         int constraintSize = Wrench.SIZE;
         int previousSize = solverInput_beq.getNumRows();

         // Careful on that one, it works as long as matrices are row major and that the number of columns is not changed.
         solverInput_Aeq.reshape(previousSize + constraintSize, problemSize, true);
         solverInput_beq.reshape(previousSize + constraintSize, 1, true);

         MatrixTools.setMatrixBlock(solverInput_Aeq, previousSize, 0, centroidalMomentumMatrix, 0, 0, constraintSize, numberOfDoFs, -1.0);
         CommonOps.insert(rhoJacobian, solverInput_Aeq, previousSize, numberOfDoFs);

         CommonOps.insert(tempWrenchConstraint_RHS, solverInput_beq, previousSize, 0);
      }

      hasWrenchesEquilibriumConstraintBeenSetup = true;
   }

   private final DenseMatrix64F tempWrenchConstraint_J = new DenseMatrix64F(Wrench.SIZE, 200);
   private final DenseMatrix64F tempWrenchConstraint_LHS = new DenseMatrix64F(Wrench.SIZE, 1);
   private final DenseMatrix64F tempWrenchConstraint_RHS = new DenseMatrix64F(Wrench.SIZE, 1);

   public boolean solve()
   {
      if (!hasWrenchesEquilibriumConstraintBeenSetup)
         throw new RuntimeException("The wrench equilibrium constraint has to be setup before calling solve().");

      addRegularization();

      numberOfEqualityConstraints.set(solverInput_Aeq.getNumRows());
      numberOfInequalityConstraints.set(solverInput_Ain.getNumRows());
      numberOfConstraints.set(solverInput_Aeq.getNumRows() + solverInput_Ain.getNumRows());

      qpSolverTimer.startMeasurement();

      qpSolver.clear();

      qpSolver.setUseWarmStart(useWarmStart);
      qpSolver.setMaxNumberOfIterations(maxNumberOfIterations);
      if (useWarmStart && pollResetActiveSet())
         qpSolver.resetActiveConstraints();

      numberOfActiveVariables.set((int) CommonOps.elementSum(solverInput_activeIndices));

      qpSolver.setQuadraticCostFunction(solverInput_H, solverInput_f, 0.0);
      qpSolver.setVariableBounds(solverInput_lb, solverInput_ub);
      qpSolver.setActiveVariables(solverInput_activeIndices);
      qpSolver.setLinearInequalityConstraints(solverInput_Ain, solverInput_bin);
      qpSolver.setLinearEqualityConstraints(solverInput_Aeq, solverInput_beq);

      numberOfIterations.set(qpSolver.solve(solverOutput));

      qpSolverTimer.stopMeasurement();

      hasWrenchesEquilibriumConstraintBeenSetup = false;

      if (MatrixTools.containsNaN(solverOutput))
      {
         return false;
      }

      CommonOps.extract(solverOutput, 0, numberOfDoFs, 0, 1, solverOutput_jointAccelerations, 0, 0);
      CommonOps.extract(solverOutput, numberOfDoFs, problemSize, 0, 1, solverOutput_rhos, 0, 0);

      addRateRegularization.set(true);

      if (SETUP_WRENCHES_CONSTRAINT_AS_OBJECTIVE)
      {
         if (hasFloatingBase)
         {
            CommonOps.mult(tempWrenchConstraint_J, solverOutput, tempWrenchConstraint_LHS);
            int index = 0;
            wrenchEquilibriumTorqueError.setX(tempWrenchConstraint_LHS.get(index, 0) - tempWrenchConstraint_RHS.get(index++, 0));
            wrenchEquilibriumTorqueError.setY(tempWrenchConstraint_LHS.get(index, 0) - tempWrenchConstraint_RHS.get(index++, 0));
            wrenchEquilibriumTorqueError.setZ(tempWrenchConstraint_LHS.get(index, 0) - tempWrenchConstraint_RHS.get(index++, 0));
            wrenchEquilibriumForceError.setX(tempWrenchConstraint_LHS.get(index, 0) - tempWrenchConstraint_RHS.get(index++, 0));
            wrenchEquilibriumForceError.setY(tempWrenchConstraint_LHS.get(index, 0) - tempWrenchConstraint_RHS.get(index++, 0));
            wrenchEquilibriumForceError.setZ(tempWrenchConstraint_LHS.get(index, 0) - tempWrenchConstraint_RHS.get(index++, 0));
         }
      }

      solverInput_H_previous.set(solverInput_H);
      solverInput_f_previous.set(solverInput_f);

      solverInput_lb_previous.set(solverInput_lb);
      solverInput_ub_previous.set(solverInput_ub);

      return true;
   }

   private void printForJerry()
   {
      MatrixTools.printJavaForConstruction("H", solverInput_H);
      MatrixTools.printJavaForConstruction("f", solverInput_f);
      MatrixTools.printJavaForConstruction("lowerBounds", solverInput_lb);
      MatrixTools.printJavaForConstruction("upperBounds", solverInput_ub);
      MatrixTools.printJavaForConstruction("solution", solverOutput);
   }

   public DenseMatrix64F getJointAccelerations()
   {
      return solverOutput_jointAccelerations;
   }

   public DenseMatrix64F getRhos()
   {
      return solverOutput_rhos;
   }

   public void setMinJointAccelerations(double qDDotMin)
   {
      for (int i = 4; i < numberOfDoFs; i++)
         solverInput_lb.set(i, 0, qDDotMin);
   }

   public void setMinJointAccelerations(DenseMatrix64F qDDotMin)
   {
      CommonOps.insert(qDDotMin, solverInput_lb, 0, 0);
   }

   public void setMaxJointAccelerations(double qDDotMax)
   {
      for (int i = 4; i < numberOfDoFs; i++)
         solverInput_ub.set(i, 0, qDDotMax);
   }

   public void setMaxJointAccelerations(DenseMatrix64F qDDotMax)
   {
      CommonOps.insert(qDDotMax, solverInput_ub, 0, 0);
   }

   public void setMinRho(double rhoMin)
   {
      for (int i = numberOfDoFs; i < problemSize; i++)
         solverInput_lb.set(i, 0, rhoMin);
   }

   public void setMinRho(DenseMatrix64F rhoMin)
   {
      CommonOps.insert(rhoMin, solverInput_lb, numberOfDoFs, 0);
   }

   public void setMaxRho(double rhoMax)
   {
      for (int i = numberOfDoFs; i < problemSize; i++)
         solverInput_ub.set(i, 0, rhoMax);
   }

   public void setMaxRho(DenseMatrix64F rhoMax)
   {
      CommonOps.insert(rhoMax, solverInput_ub, numberOfDoFs, 0);
   }

   public void setActiveRhos(DenseMatrix64F activeRhoMatrix)
   {
      CommonOps.insert(activeRhoMatrix, solverInput_activeIndices, numberOfDoFs, 0);
   }
}
