package org.toxicblend.operations.zadjust

import com.bulletphysics.collision.dispatch.CollisionWorld.RayResultCallback
import com.bulletphysics.collision.dispatch.CollisionWorld.LocalRayResult
import com.bulletphysics.linearmath.VectorUtil
import javax.vecmath.Vector3d
import javax.vecmath.Point2d
import javax.vecmath.Point3d

class ClosestRayResultCallback(val minZ:Double,val maxZ:Double) extends RayResultCallback {
  val rayFromWorld = new Vector3d; rayFromWorld.z = maxZ
  val rayToWorld = new Vector3d; rayToWorld.z = minZ
  val hitPointWorld = new Point3d
  var triangleIndex:Int = -1
     
  /**
   * callback from jbullet on collision
   */
  override def addSingleResult(rayResult:LocalRayResult, normalInWorldSpace:Boolean):Double = {
    closestHitFraction = rayResult.hitFraction      
    triangleIndex = rayResult.localShapeInfo.triangleIndex
    VectorUtil.setInterpolate3(hitPointWorld, rayFromWorld, rayToWorld, closestHitFraction)
    //searchstate.currentC.setCollision(hitPointWorld, rayResult.localShapeInfo.triangleIndex)
    //val triangle = searchstate.collisionWrapper.models(0).getFaces(searchstate.currentC.triangleIndex).toIndexedSeq.map(i => searchstate.collisionWrapper.models(0).getVertices(i))
    //TrianglePlaneIntersection.trianglePlaneIntersection(triangle, searchstate.segmentPlane, searchstate.currentC.collisionPoint, searchstate.directionNormalized, searchstate.currentC)
    closestHitFraction
  }
  
  @inline def hasResult = closestHitFraction < 1d
  /**
   * returns the collision result as a reference to an internal reused variable
   */
  @inline def getResult = {
    if (!hasResult) {
      hitPointWorld.x = rayFromWorld.x
      hitPointWorld.y = rayFromWorld.y
      hitPointWorld.z = minZ
    }
    hitPointWorld
  }
  
  def resetForReuse(samplePoint:Point2d) = {
    rayFromWorld.x = samplePoint.x
    rayFromWorld.y = samplePoint.y
    rayToWorld.x = samplePoint.x
    rayToWorld.y = samplePoint.y
    
    triangleIndex = -1
    closestHitFraction = 1d
  }
}