/// A binary tree that maps a range [0, N) ("index") to another range [X, X + N) ("mapped to address")
sealed trait IndexNode:
  val range: Range
  def shiftRange(offset: Int): IndexNode

  // O(lgN)
  def remove(index: Int): (IndexNode, Int)

  // O(lgN)
  def insert(index: Int, indexMapToAddress: Int): IndexNode

  // O(lgN)
  def lookup(index: Int): Option[Int]

  // O(N)
  def lookupAddress(mappedAddress: Int): Option[Int]

  def traverse(visitor: (Int, Int) => Unit): Unit = traverseWithOffset(visitor, range.start)
  def traverseWithOffset(visitor: (Int, Int) => Unit, offset: Int): Unit

case class Leaf(range: Range, mapToAddress: Int) extends IndexNode {
  override def shiftRange(offset: Int): IndexNode = this.copy(range = Range(range.start + offset, range.end + offset))

  override def remove(index: Int): (IndexNode, Int) =
    require(range.nonEmpty)
    require(range.start <= index && index < range.end)

    val indexMappedToOffset = mapToAddress + (index - range.start)

    if (range.length == 1)
      (null, indexMappedToOffset)
    else
      val newRange = Range(range.start, range.end - 1)
      if (range.start == index)
        (Leaf(newRange, mapToAddress + 1), indexMappedToOffset)
      else if (range.end - 1 == index)
        (Leaf(newRange, mapToAddress), indexMappedToOffset)
      else
        // children ranges are relative the branch's range
        val newLeft = Leaf(Range(0, index - range.start), mapToAddress)
        val newRight = Leaf(Range(index - range.start, range.end - range.start - 1), indexMappedToOffset + 1)
        (Branch(newRange, newLeft, newRight), indexMappedToOffset)

  override def insert(index: Int, indexMapToAddress: Int): IndexNode =
    // note the "<= end part" - it's allowed to insert after the last element
    require(range.start <= index && index <= range.end)
    val newRange = Range(range.start, range.end + 1)
    // adjust to relative offset
    val newLeaf = Leaf(Range(index - range.start, index - range.start + 1), indexMapToAddress)
    val newLeftSize = index - range.start
    val newRightSize = range.end - index
    if (newLeftSize == 0)
      // adjust to relative offset
      Branch(newRange, newLeaf, this.shiftRange(-range.start + 1))
    else if (newRightSize == 0)
      // adjust to relative offset
      Branch(newRange, this.shiftRange(-range.start), newLeaf)
    else
      // adjust to relative offsets
      val newLeft = Leaf(Range(0, newLeftSize), this.mapToAddress)
      val newRight = Leaf(Range(newLeftSize + 1, newLeftSize + 1 + newRightSize), this.mapToAddress + newLeftSize)
      if (newLeftSize > newRightSize) {
        // for newLeaf and newRight, since they're going under a new right side branch, their ranges need to be
        // adjusted to relative offsets again
        val newLeafAdjusted = newLeaf.shiftRange(-newLeftSize)
        val newRightAdjusted = newRight.shiftRange(-newLeftSize)
        Branch(newRange, newLeft, Branch(Range(newLeftSize, newLeftSize + 1 + newRightSize), newLeafAdjusted, newRightAdjusted))
      } else {
        Branch(newRange, Branch(Range(0, newLeftSize + 1), newLeft, newLeaf), newRight)
      }

  override def traverseWithOffset(visitor: (Int, Int) => Unit, offset: Int): Unit =
    (range.start until range.end).foreach(index => visitor.apply(index + offset, mapToAddress + index - range.start))

  override def lookupAddress(address: Int): Option[Int] =
    val addressOffset = address - mapToAddress
    if (0 <= addressOffset && addressOffset < range.end - range.start)
      Some(range.start + addressOffset)
    else
      None

  override def lookup(index: Int): Option[Int] =
    if (range.start <= index && index < range.end)
      Some(mapToAddress + index - range.start)
    else
      None
}

case class Branch(range: Range, left: IndexNode, right: IndexNode) extends IndexNode {
  override def shiftRange(offset: Int): IndexNode = this.copy(range = Range(range.start + offset, range.end + offset))

  override def remove(index: Int): (IndexNode, Int) =
    require(range.length > 1)
    require(range.start <= index && index < range.end)

    val relativeIndex = index - range.start
    val newFrom = Range(range.start, range.end - 1)
    if (left.range.contains(relativeIndex))
      val (newLeft, mappedFromLeft) = left.remove(relativeIndex)
      val newRight = right.shiftRange(-1)
      if (newLeft != null) {
        (Branch(newFrom, newLeft, newRight), mappedFromLeft)
      } else {
        (newRight.shiftRange(this.range.start), mappedFromLeft)
      }
    else
      val (newRight, mappedFromRight) = right.remove(relativeIndex)
      if (newRight != null) {
        (Branch(newFrom, left, newRight), mappedFromRight)
      } else {
        (left.shiftRange(this.range.start), mappedFromRight)
      }

  override def insert(index: Int, indexMapToAddress: Int): IndexNode =
    // note the "<= end part" - it's allowed to insert after the last element
    require(range.start <= index && index <= range.end)

    val newRange = Range(range.start, range.end + 1)

    // adjust to relative offset
    val offset = index - range.start
    if (offset < right.range.start)
      Branch(newRange, left.insert(offset, indexMapToAddress), right.shiftRange(1))
    else
      Branch(newRange, left, right.insert(offset, indexMapToAddress))

  override def traverseWithOffset(visitor: (Int, Int) => Unit, offset: Int): Unit =
    left.traverseWithOffset(visitor, range.start + offset)
    right.traverseWithOffset(visitor, range.start + offset)

  override def lookupAddress(address: Int): Option[Int] =
    left.lookupAddress(address)
      .orElse(right.lookupAddress(address))
      .map(_ + range.start)

  override def lookup(index: Int): Option[Int] =
    val offset = index - range.start
    if (offset < right.range.start)
      left.lookup(offset)
    else if (offset < range.end)
      right.lookup(offset)
    else
      None
}

def mix(data: Array[Long], indexTree: IndexNode): IndexNode =
  data.indices.foldLeft[IndexNode](indexTree)((tree, origDataOffset) =>
    val indexToMove = tree.lookupAddress(origDataOffset).get
    val (updatedTree, elemAddr) = tree.remove(indexToMove)
    val elem = data(elemAddr)
    if (elem == 0)
      // no move for value 0
      tree
    else
      var moveToIndex = ((indexToMove + elem) % (data.length - 1)).toInt
      if (moveToIndex < 0)
        moveToIndex += data.length - 1
      if (moveToIndex == indexToMove)
        // no move if offset is the same
        tree
      else
        updatedTree.insert(moveToIndex, elemAddr)
  )

def printResult(data: Array[Long], finalTree: IndexNode): Unit = {
  val origZeroIndex = data.indexOf(0)
  val nowZeroIndex = finalTree.lookupAddress(origZeroIndex).get
  val at1000th = data(finalTree.lookup((nowZeroIndex + 1000) % data.length).get)
  val at2000th = data(finalTree.lookup((nowZeroIndex + 2000) % data.length).get)
  val at3000th = data(finalTree.lookup((nowZeroIndex + 3000) % data.length).get)
  println(f"$at1000th + $at2000th + $at3000th = ${at1000th + at2000th + at3000th}")
}

@main def hello(): Unit =
  val data = io.Source.stdin.getLines().map { _.toLong }.toArray
  val blankTree = Leaf(Range(0, data.length), 0)
  val indexTree = mix(data, blankTree)
  printResult(data, indexTree)

  // part 2
  val data2 = data.map(_ * 811589153)
  // Index trees are immutable so it's totally fine to reuse the same blankTree here
  val finalTree2 = (0 until 10).foldLeft[IndexNode](blankTree)((tree, _) => mix(data2, tree))
  printResult(data2, finalTree2)
