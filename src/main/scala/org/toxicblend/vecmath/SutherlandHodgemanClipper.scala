package org.toxicblend.vecmath

import scala.collection.mutable.ArrayBuffer
import SutherlandHodgemanClipper.{intersection,conditionalAppend}

class SutherlandHodgemanClipper {
 
  def clipPolygon(input:IndexedSeq[Vec2D], a:Vec2D, b:Vec2D, ε:Double):IndexedSeq[Vec2D] = {
    
    @inline def isInside(p:Vec2D) = (a.x-p.x)*(b.y-p.y)>(a.y-p.y)*(b.x-p.x)
    
    var previousI = input.size-1
    val rv = (0 until input.size).foldLeft(new ArrayBuffer[Vec2D]) ((x,i) => {
      val current = input(i)
      val previous = input(previousI)
      (isInside(current),isInside(previous)) match {
        case (true,true)  => conditionalAppend(x,current,ε)
        case (true,false) => conditionalAppend(conditionalAppend(x,intersection(a,b,current,previous),ε),current,ε)
        case (false,true) => conditionalAppend(x,intersection(a,b,current,previous),ε)
        case _ => 
      }
      previousI = i
      x
    })
    if ( rv.size >1 && rv.last.=~=(rv.head,ε)) rv.remove(rv.size-1)
    rv
  }
  
  def clipPolygon(input:IndexedSeq[Vec2D], clipEdge:FiniteLine2D, ε:Double):IndexedSeq[Vec2D] = 
    clipPolygon(input, clipEdge.a, clipEdge.b, ε)
    
  def clipPolygon(input:IndexedSeq[Vec2D], clipEdges:IndexedSeq[Vec2D], ε:Double):IndexedSeq[Vec2D] = {
    val clipSize = clipEdges.size
    if (clipSize==2) clipPolygon(input, clipEdges.head, clipEdges.last, ε)
    else (0 until clipSize).foldLeft(input)((p,i) => clipPolygon(p, clipEdges((i+clipSize-1) % clipSize), clipEdges(i), ε))
  }
}

class SutherlandHodgemanRectangularClipper {
   
  def clipPolygon(input:IndexedSeq[Vec2D], a:Vec2D, b:Vec2D, aabb:AABB2D, ε:Double):IndexedSeq[Vec2D] = {
    
    @inline def isInside(p:Vec2D) = aabb.containsPoint(p, ε)
    
    var previousI = input.size-1
    val rv = (0 until input.size).foldLeft(new ArrayBuffer[Vec2D]) ((x,i) => {
      val current = input(i)
      val previous = input(previousI)
      (isInside(current),isInside(previous)) match {
        case (true,true)  => conditionalAppend(x,current,ε)
        case (true,false) => conditionalAppend(conditionalAppend(x,intersection(a,b,current,previous),ε),current,ε)
        case (false,true) => conditionalAppend(x,intersection(a,b,current,previous),ε)
        case _ => 
      }
      previousI = i
      x
    })
    if ( rv.size >1 && rv.last.=~=(rv.head,ε)) rv.remove(rv.size-1)
    rv
  }
    
  def clipPolygon(input:IndexedSeq[Vec2D], aabb:AABB2D, ε:Double):IndexedSeq[Vec2D] = {
    val clipEdges = aabb.toIndexedSeq(true)
    val clipSize = 4
    (0 until clipSize).foldLeft(input)((p,i) => clipPolygon(p, clipEdges((i+clipSize-1) % clipSize), clipEdges(i), aabb, ε))
  }
}

object SutherlandHodgemanClipper {
  
  def intersection(a:Vec2D, b:Vec2D, p:Vec2D, q:Vec2D) = {
    val a1 = b.y - a.y
    val b1 = a.x - b.x
    val c1 = a1 * a.x + b1 * a.y
    val a2 = q.y - p.y
    val b2 = p.x - q.x
    val c2 = a2 * p.x + b2 * p.y
    val det = a1 * b2 - a2 * b1
    Vec2D((b2 * c1 - b1 * c2) / det, (a1 * c2 - a2 * c1) / det)
  }
  
  @inline def conditionalAppend(buffer:ArrayBuffer[Vec2D], v:Vec2D, ε:Double) = {
    if (buffer.size==0 || !buffer.last.=~=(v,ε) ) 
      buffer.append(v)
    buffer
  }
  
  lazy val singleton = new SutherlandHodgemanClipper
  lazy val singletonRectangular = new SutherlandHodgemanRectangularClipper
  def clip(subject:Polygon2D, clipPolygon:Polygon2D, enforceDirection:Option[Boolean]=None, ε:Double=Polygon2D.ε)=Polygon2D(singleton.clipPolygon(subject.toIndexedSeq, clipPolygon.toIndexedSeq,ε), enforceDirection, ε)
  def clipRect(subject:Polygon2D, aabb:AABB2D, enforceDirection:Option[Boolean]=None, ε:Double=Polygon2D.ε)=Polygon2D(singletonRectangular.clipPolygon(subject.toIndexedSeq, aabb,ε), enforceDirection, ε)
 
  def clip(subject:IndexedSeq[Vec2D], clipEdges:IndexedSeq[Vec2D], ε:Double)=singleton.clipPolygon(subject, clipEdges, ε)
  def clip(subject:IndexedSeq[Vec2D], clipEdge:FiniteLine2D, ε:Double)=singleton.clipPolygon(subject, clipEdge, ε)
  def clip(subject:IndexedSeq[Vec2D], clipV1:Vec2D, clipV2:Vec2D, ε:Double)=singleton.clipPolygon(subject, clipV1, clipV2, ε)
}

