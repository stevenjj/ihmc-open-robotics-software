package us.ihmc.robotics.geometry;

import org.junit.Test;
import us.ihmc.tools.agileTesting.BambooAnnotations.EstimatedDuration;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import java.util.ArrayList;

import static org.junit.Assert.assertTrue;

public class CapsuleTest
{
   
   private void transformAndCheck(RigidBodyTransform trans, Point3d expected, Point3d result )
   {
      double epsilon = 0.00001; 
      Point3d exp = new Point3d( expected );
      trans.transform(exp);
      assertTrue( result.epsilonEquals( exp, epsilon) );
   }

	@EstimatedDuration(duration = 0.0)
	@Test(timeout = 30000)
   public void test()
   {
      Point3d res1 = new Point3d();
      Point3d res2 = new Point3d();
      
      // first create some weird transformations to be applied to the capsules and the points
      RigidBodyTransform tr_base  = new RigidBodyTransform();
      RigidBodyTransform tr_other = new RigidBodyTransform();
      tr_base.setIdentity();
      
      ArrayList<RigidBodyTransform> transforms = new  ArrayList<RigidBodyTransform>();
      transforms.add( new RigidBodyTransform( tr_base ) );
      
     /// tr_other.rotX( 0.5);
      tr_other.setTranslation(new Vector3d( 1, 1, 1) );
      tr_base.multiply( tr_other );
      transforms.add( new RigidBodyTransform( tr_base ) );
      
      tr_other.rotY( 1.2);
      tr_base.multiply( tr_other );
      tr_other.rotZ( -0.6 );
      tr_other.setTranslation(new Vector3d( 0, -2, 3) );
      tr_base.multiply( tr_other );
      transforms.add( new RigidBodyTransform( tr_base ) );
      
      // check for each of the transformation if this the conditions are true.
      // it is easy to check this conditions in the case trans = identity
      for (int i=0; i< transforms.size(); i++)
      {    
         RigidBodyTransform trans = transforms.get(i);
         
         Capsule c1 = new Capsule( new Point3d( -1,  0,0), new Point3d( 1,0,0),        0.0 );
         Capsule c2 = new Capsule( new Point3d( 0.2, 0.3,0), new Point3d( 0.5,0.9,0),  0.0 );
         
         Capsule c3 = new Capsule( new Point3d(-1.5, 0, 0), new Point3d( -1.5, 1,0),   0.0 );
         Capsule c4 = new Capsule( new Point3d(0, -1,  0.5), new Point3d( 0, 1,  0.5), 0.0 );
         
         c1.transform(trans);
         c2.transform(trans);
         c3.transform(trans);
         c4.transform(trans);
      
         Capsule.distanceQuery(c1, c2, res1, res2); 
         transformAndCheck( trans, new Point3d( 0.2, 0,   0 ), res1 );
         transformAndCheck( trans, new Point3d( 0.2, 0.3, 0 ), res2 );
      
         Capsule.distanceQuery(c1, c3, res1, res2);    
         transformAndCheck( trans, new Point3d( -1.0, 0, 0 ), res1 );
         transformAndCheck( trans, new Point3d( -1.5, 0, 0 ), res2 );
   
         Capsule.distanceQuery(c1, c4, res1, res2);  
         transformAndCheck( trans, new Point3d( 0, 0, 0   ), res1 );
         transformAndCheck( trans, new Point3d( 0, 0, 0.5 ), res2 );
         
         c1.radius = 0.1;
         c2.radius = 0.05;
         c3.radius = 0.07;
         c4.radius = 0.15;
         
         Capsule.distanceQuery(c1, c2, res1, res2);    
         transformAndCheck( trans, new Point3d( 0.2, 0.1,  0 ), res1 );
         transformAndCheck( trans, new Point3d( 0.2, 0.25, 0 ), res2 );
         
         Capsule.distanceQuery(c1, c3, res1, res2);    
         transformAndCheck( trans, new Point3d( -1.1,  0, 0 ), res1 );
         transformAndCheck( trans, new Point3d( -1.43, 0, 0 ), res2 );
         
         Capsule.distanceQuery(c1, c4, res1, res2);  
         transformAndCheck( trans,new Point3d( 0, 0, 0.1   ), res1 );
         transformAndCheck( trans, new Point3d( 0, 0, 0.35 ), res2 );
      }
   }

}
