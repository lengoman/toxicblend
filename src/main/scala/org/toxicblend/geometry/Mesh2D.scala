package org.toxicblend.geometry

import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.collection.IndexedSeqLike
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.Buffer
import org.toxicblend.typeconverters.SeqShift
import org.toxicblend.typeconverters.SeqOperations
import toxi.geom.BooleanShapeBuilder
import toxi.geom.Polygon2D
import toxi.geom.ReadonlyVec2D
import toxi.geom.Vec2D
import collection.JavaConversions._
import org.toxicblend.util.EdgeToFaceMap
import java.awt.geom.GeneralPath
import java.awt.geom.Path2D
import java.awt.geom.Area

/**
 * A container for a 2D half-edge structure. 
 * It contains :
 *  2D Vertexes indexed by Int.
 *  Faces indexed by Int, containing lists of vertex indexes
 * The faces are implicitly closed, e.g. the face loop [0,1,2,0] is represented as [0,1,2]
 */
class Mesh2D protected ( val vertexes:ArrayBuffer[ReadonlyVec2D], val faces:ArrayBuffer[ArrayBuffer[Int]]){
  
  def this( f:(ArrayBuffer[ReadonlyVec2D],ArrayBuffer[ArrayBuffer[Int]]) ) = {
    this(f._1,f._2)
  }
  
  protected def uniqueConsecutivePoints(input:ArrayBuffer[Int]) = {
    val output = new ArrayBuffer[Int](input.size)
    var prev:Int = input(0)
    output += prev
    input.drop(1).foreach(i => {
      if (prev != i) 
        output += i
      prev = i
    }) 
    output
  }
  
  /** 
   * Removes duplicated vertexes and recalculates the faces
   */
  protected def removeDoubles:Mesh2D = {
    val uniquePoints = new HashMap[ReadonlyVec2D, Int]
    // translationTable(oldIndex) == newIndex (or -1 if unassigned)
    val translationTable = (0 until vertexes.size).map( _ => -1).toArray 
    var pNewIndex=0
    (0 until vertexes.size).foreach(pOldIndex => {
      val p = vertexes(pOldIndex)
      if (uniquePoints contains p) {
        // p is already known so it is not unique
        pNewIndex = uniquePoints(p)
      } else {
        pNewIndex = uniquePoints.size
        uniquePoints(p) = pNewIndex
      }
      translationTable(pOldIndex) = pNewIndex
    })
    val newVertexes = new Array[ReadonlyVec2D](uniquePoints.size).to[ArrayBuffer]
    (0 until vertexes.size).foreach(pOldIndex => {
      newVertexes(translationTable(pOldIndex)) = vertexes(pOldIndex)
    })
    val newFaces = faces.map( f => uniqueConsecutivePoints(f.map(p => translationTable(p)))).filter(x => x.size>1)
    /*
    println("removeDoubles:")
    println("  newVertexes:" + newVertexes.mkString("{",",","}"))
    println("  newFaces   :" + newFaces.map(x => x.mkString("(",", ",")")).mkString(", ")) 
    println()
    //(newVertexes,fuseFaces(newFaces))
    */ 
    setState(newVertexes,newFaces)
    this
  }
  
  /**
   * override the internal state with new vertexes and faces
   */
  protected def setState(vs:ArrayBuffer[ReadonlyVec2D],fs:ArrayBuffer[ArrayBuffer[Int]]) {
    vertexes.clear
    vs.foreach( v => vertexes += v)
    faces.clear
    fs.foreach( f => faces += f)
  }
    
  /** 
   * Recalculates the faces using BooleanShapeBuilder
   */
  protected def mergeAllFacesWithBooleanShapeBuilder:Mesh2D = {
    
    val builder = new BooleanShapeBuilder(BooleanShapeBuilder.Type.UNION)
    faces.foreach(facePoints => {
      val path=new Polygon2D() //Path2D.Float(PathIterator.WIND_NON_ZERO, facePoints.size)
      facePoints.foreach(pointIndex => {
        val point = vertexes(pointIndex)
        path.add(point.x, point.y)
      })
      builder.addShape(path)
    })
    buildFromPolygons(builder.computeShapes())
  }
  
  protected def buildFromPolygons(unionPolygons:java.util.List[Polygon2D]):Mesh2D = { 
    val rvVertexes = new ArrayBuffer[ReadonlyVec2D]()
    val rvFaces = new ArrayBuffer[ArrayBuffer[Int]]()
    unionPolygons.foreach(polygon => {
      if (polygon.size>1) {
        var index = rvVertexes.size
        val tmpFaces = new ArrayBuffer[Int]
        polygon.foreach(v => { 
          rvVertexes.append(v) 
          tmpFaces.append(index)
          index += 1
        })
        rvFaces.append(tmpFaces)
      }
    })
    
    setState(rvVertexes, rvFaces)
    this
  }
  
  /**
   * helper method that converts a face to an area object
   */
  protected def poly2Area(index:Int):Area = {
    val thisFace = faces(index)
    val firstVertex = vertexes(thisFace(0))
    val gp = new GeneralPath(Path2D.WIND_EVEN_ODD,thisFace.size)
    gp.moveTo(firstVertex.x, firstVertex.y)
    thisFace.foreach(vi => {
      val v = vertexes(vi)
      gp.lineTo(v.x, v.y)
    })
    gp.closePath
    new Area(gp)
  } 
  
  /** 
   * Recalculates the faces using BooleanShapeBuilder
   */
  protected def mergeAllFaces(multiThread:Boolean):Mesh2D = {
   
    val seqOp=(a:Area,b:Int) =>  {val rv = poly2Area(b); rv.add(a); rv }
    val combOp=(a:Area,b:Area) => { a.add(b); a }      
    
    val builder = new BooleanShapeBuilder(BooleanShapeBuilder.Type.UNION)
    builder.combineWithArea((0 until faces.size).par.aggregate(new Area)(seqOp,combOp))
    buildFromPolygons(builder.computeShapes())
  }
  
  def projectionOutline(multiThread:Boolean=false):Mesh2D = {
    if (faces.size < 10) {
      println("in faces  :" + faces.map(x => x.mkString("(",", ",")")).mkString(", ") + " faces:" + faces.size) 
      removeDoubles
      println("dedoubled :" + faces.map(x => x.mkString("(",", ",")")).mkString(", ") + " faces:" + faces.size) 
      if (multiThread) {
        println("using mergeAllFacesWithBooleanShapeBuilder") 
        mergeAllFacesWithBooleanShapeBuilder
      } else {
        println("using mergeAllFaces") 
        mergeAllFaces(true)
      }
      println("outFaces:" + faces.map(x => x.mkString("(",", ",")")).mkString(", ") + " faces:" + faces.size) 
    } else {
      println("in faces :" + faces.size) 
      removeDoubles
      println("dedoubled :" + faces.size) 
      if (multiThread) {
        println("using mergeAllFacesWithBooleanShapeBuilder") 
        mergeAllFacesWithBooleanShapeBuilder
      } else {
        println("using mergeAllFaces") 
        mergeAllFaces(true)
      }
      println("outFaces  :" +  faces.size)
    }
    this
  }
}

object Mesh2D {
  def apply( vertexes:ArrayBuffer[ReadonlyVec2D], faces:ArrayBuffer[ArrayBuffer[Int]]) = {
    new Mesh2D(vertexes, faces)
  } 
}