package us.ihmc.robotEnvironmentAwareness.communication.packets;

import us.ihmc.communication.packets.Packet;
import us.ihmc.euclid.tuple3D.Point3D;

public class BoundingBoxParametersMessage extends Packet<BoundingBoxParametersMessage>
{
   public float minX, minY, minZ;
   public float maxX, maxY, maxZ;

   public BoundingBoxParametersMessage()
   {
   }

   public BoundingBoxParametersMessage(BoundingBoxParametersMessage other)
   {
      set(other);
   }

   @Override
   public void set(BoundingBoxParametersMessage other)
   {
      setPacketInformation(other);
      minX = other.minX;
      minY = other.minY;
      minZ = other.minZ;
      maxX = other.maxX;
      maxY = other.maxY;
      maxZ = other.maxZ;
   }

   public float getMinX()
   {
      return minX;
   }

   public float getMinY()
   {
      return minY;
   }

   public float getMinZ()
   {
      return minZ;
   }

   public float getMaxX()
   {
      return maxX;
   }

   public float getMaxY()
   {
      return maxY;
   }

   public float getMaxZ()
   {
      return maxZ;
   }

   public void setMinX(float minX)
   {
      this.minX = minX;
   }

   public void setMinY(float minY)
   {
      this.minY = minY;
   }

   public void setMinZ(float minZ)
   {
      this.minZ = minZ;
   }

   public void setMaxX(float maxX)
   {
      this.maxX = maxX;
   }

   public void setMaxY(float maxY)
   {
      this.maxY = maxY;
   }

   public void setMaxZ(float maxZ)
   {
      this.maxZ = maxZ;
   }

   public Point3D getMin()
   {
      return new Point3D(minX, minY, minZ);
   }

   public Point3D getMax()
   {
      return new Point3D(maxX, maxY, maxZ);
   }

   @Override
   public boolean epsilonEquals(BoundingBoxParametersMessage other, double epsilon)
   {
      if (Float.compare(minX, other.minX) != 0)
         return false;
      if (Float.compare(minY, other.minY) != 0)
         return false;
      if (Float.compare(minZ, other.minZ) != 0)
         return false;
      if (Float.compare(maxX, other.maxX) != 0)
         return false;
      if (Float.compare(maxY, other.maxY) != 0)
         return false;
      if (Float.compare(maxZ, other.maxZ) != 0)
         return false;
      return true;
   }

   @Override
   public String toString()
   {
      return "min: (" + minX + ", " + minY + ", " + minZ + "), max: (" + maxX + ", " + maxY + ", " + maxZ + ")";
   }
}
