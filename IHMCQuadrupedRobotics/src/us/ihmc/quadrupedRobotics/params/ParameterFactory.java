package us.ihmc.quadrupedRobotics.params;

import us.ihmc.robotics.dataStructures.listener.VariableChangedListener;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.dataStructures.variable.YoVariable;

/**
 * A factory for creating and registering parameters and their default values.
 * <p/>
 * This class is the front-end for declaring parameters in user classes.
 * <p/>
 * Example usage:
 * <pre>
 * class MyClass {
 *    ParameterFactory parameterFactory = ParameterFactory.createWithRegistry(getClass(), registry);
 *    DoubleParameter jointDampingParameter = parameterFactory.createDouble("jointDamping", 2);
 * }
 * </pre>
 */
public class ParameterFactory
{
   private final String namespace;
   private final YoVariableRegistry registry;

   /**
    * Create a new parameter factory. Each namespace (class) should create its own
    *
    * @param namespace
    */
   private ParameterFactory(Class<?> namespace)
   {
      this.namespace = namespace.getName();
      this.registry = null;
   }

   private ParameterFactory(Class<?> namespace, YoVariableRegistry registry)
   {
      this.namespace = namespace.getName();
      this.registry = registry;
   }

   /**
    * Creates a new ParameterFactory without a {@link YoVariableRegistry}. Without a registry, {@link YoVariable}s will not be registered at all. If you would
    * like to access parameter values as {@link YoVariable}s, then use {@link #createWithRegistry(Class, YoVariableRegistry)} instead.
    *
    * @param namespace the namespace in which to register parameters
    * @return a new parameter factory
    */
   public static ParameterFactory createWithoutRegistry(Class<?> namespace)
   {
      return new ParameterFactory(namespace);
   }

   /**
    * Creates a new ParameterFactory with a {@link YoVariableRegistry}. For {@link Parameter} types that are supported, {@link YoVariable}s will be created and
    * values will be mirrored.
    *
    * @param namespace the namespace in which to register parameters
    * @return a new parameter factory
    */
   public static ParameterFactory createWithRegistry(Class<?> namespace, YoVariableRegistry registry)
   {
      return new ParameterFactory(namespace, registry);
   }

   public BooleanParameter createBoolean(String name, boolean defaultValue)
   {
      BooleanParameter parameter = new BooleanParameter(namespace + "." + name, defaultValue);
      register(parameter);
      return parameter;
   }

   public DoubleParameter createDouble(String name, double defaultValue)
   {
      final DoubleParameter parameter = new DoubleParameter(namespace + "." + name, defaultValue);

      if (registry != null)
      {
         DoubleYoVariable variable = new DoubleYoVariable("param__" + parameter.getShortPath(), registry);
         variable.set(parameter.get());
         variable.addVariableChangedListener(new VariableChangedListener()
         {
            @Override
            public void variableChanged(YoVariable<?> v)
            {
               parameter.set(((DoubleYoVariable) v).getDoubleValue());
            }
         });
      }

      register(parameter);
      return parameter;
   }

   public DoubleArrayParameter createDoubleArray(String name, double... defaultValue)
   {
      DoubleArrayParameter parameter = new DoubleArrayParameter(namespace + "." + name, defaultValue);
      register(parameter);
      return parameter;
   }

   public StringParameter createString(String name, String defaultValue)
   {
      StringParameter parameter = new StringParameter(namespace + "." + name, defaultValue);
      register(parameter);
      return parameter;
   }

   private static void register(Parameter parameter)
   {
      ParameterRegistry.getInstance().register(parameter);
   }
}
