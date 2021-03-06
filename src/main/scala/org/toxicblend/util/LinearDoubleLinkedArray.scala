package org.toxicblend.util

class LinearDoubleLinkedArray[T] private (val defaultValue:T, var indices:Array[DoubleLinkedArrayElement[T]] ) {
  
  def this(defaultValue:T, initialSize:Int, setupAsEmpty:Boolean=false) {
    this( defaultValue, new Array[DoubleLinkedArrayElement[T]](initialSize) )
    for (i<- 0 until initialSize) indices(i) = new DoubleLinkedArrayElement[T](defaultValue, i-1, i+1)    
    setup(initialSize, setupAsEmpty)
  }
  
  // an index to a vertex that has not been removed yet 
  private var theHead = 0
  
  @inline def next(i:Int):Int = indices(i).next
  @inline def prev(i:Int):Int = indices(i).prev
  
  def head:Int = theHead
  
  /**
   * Don't call this method unless you keep track of 'theHead'
   */
  @inline private def connect(i:Int, j:Int):Unit = {
    if ( i < 0 && j < 0 ) {
      return
    }
    if (i != -1) indices(i).next = j
    if (j != -1) indices(j).prev = i  
  }
  
  /**
   */
  def contains(element:Int): Boolean = {
    if (isEmpty) return false
    val e = indices(element)
    if (e.next < 0 && e.prev < 0 && theHead==element) return true
    e.next >= 0 || e.prev >= 0
  }
  
  @inline def isEmpty = theHead == -1
  
  /**
   * you must be sure that the element you drop is actually part of the list
   */
  @inline def drop(i:Int) {
    assert(i>=0)
    val ie = indices(i)
    if (theHead == i) {
      assert(ie.prev == -1)
      theHead = ie.next
    }
    connect(ie.prev, ie.next)
    
    ie.next = -1
    ie.prev = -1
  }
  
  @inline def safeDrop(i:Int) {
    if (contains(i)) {
      drop(i)
    }
  }
    
  /**
   * inserts a previous dropped element just ahead of 'theHead'
   */
  @inline def add(i:Int) {
    assert(i>=0)
    if (theHead<0){
      theHead = i
    } else {
      val ie = indices(i)
      val se = indices(theHead)
      if (se.prev >= 0) {
        val spe= indices(se.prev)
        spe.next = i
      } 
      ie.prev = se.prev
      se.prev = i
      ie.next = theHead
    }
    theHead = i
  }
  
  @inline def safeAdd(i:Int) {
    if (!contains(i)) {
      add(i)
    }
  }
  
  def toIndexedSeq:IndexedSeq[Int] = {
    if (theHead != -1){
      var rv = new collection.mutable.ArrayBuffer[Int]

      var i = theHead
      rv.append(theHead)
      i = next(theHead)
      while (i != -1){
        rv.append(i)
        i = next(i)
      }
      rv
    } else Array[Int]()
  }
  
  def setup(inputSize:Int, setupAsEmpty:Boolean=false) = {
    if (indices.size < inputSize) {
      val newIndices = new Array[DoubleLinkedArrayElement[T]](inputSize)
      val oldSize = indices.size
      for (i <- 0 until oldSize) {
        val e =  indices(i)
        if (setupAsEmpty){
          e.prev = -1
          e.next = -1
        } else {
          e.prev = i-1
          e.next = i+1
        }
        newIndices(i) = e
      }
      for (i <- oldSize until inputSize) 
        newIndices(i) = 
          if (setupAsEmpty) new DoubleLinkedArrayElement[T](defaultValue,-1, -1) 
          else new DoubleLinkedArrayElement[T](defaultValue, i-1, i+1) 
      indices = newIndices
    } else {
      for (i <- 0 until inputSize) {
        val e =  indices(i)
        if (setupAsEmpty){
          e.prev = -1
          e.next = -1
        } else {
          e.prev = i-1
          e.next = i+1
        }
      }
    }
    if (!setupAsEmpty){
      indices(0).prev = -1
      indices(inputSize-1).next = -1
      theHead = 0
    } else {
      theHead = -1
    }
  }
  @inline def apply(i:Int) = indices(i)
}