package figtree;

import java.util.*;

public class FigTree<V> {
	public FigTree(int order) {
		this.root = new FigTreeNode(0);
		this.ORDER = order;
		this.SPLITLIMIT = 1 + (order << 1);
	}
	
	private class InsertArgs {
		public InsertArgs(Interval range, V value) {
			this(range, value, new ArrayList<FigTreeNode>(), new ArrayList<Integer>(),
					FigTree.this.root, new Interval(Integer.MIN_VALUE, Integer.MAX_VALUE));
		}
		public InsertArgs(Interval range, V value, ArrayList<FigTreeNode> path,
				ArrayList<Integer> pathIndices, FigTreeNode at, Interval valid) {
			this.range = range;
			this.value = value;
			this.path = path;
			this.pathIndices = pathIndices;
			this.at = at;
			this.valid = valid;
		}
		public Interval range;
		public V value;
		public ArrayList<FigTreeNode> path;
		public ArrayList<Integer> pathIndices;
		public FigTreeNode at;
		public Interval valid;
	}
	
	public void write(Interval range, V value) {
		// Insert the primary group [a, b]
		InsertArgs starinsert = this.insert(new InsertArgs(range, value));
		
		if (starinsert != null) {
			// Insert the residual group [b + 1, star], if any
			starinsert = this.insert(starinsert);
			
			if (starinsert != null) {
				// We should have at most one residual group!
				throw new IllegalStateException("Multiple star inserts");
			}
		}
	}
	
	private InsertArgs insert(InsertArgs args) {
		Interval range = args.range;
		V value = args.value;
		ArrayList<FigTreeNode> path = args.path;
		ArrayList<Integer> pathIndices = args.pathIndices;
		FigTreeNode currnode = args.at;
		Interval valid = args.valid;
		
		if (value == null) {
			throw new IllegalArgumentException("can't insert null value");
		}
		
		// System.out.printf("insert %s into %s, valid = %s\n", args.range, args.at, args.valid);
		
		/* Record the residual group [range.right() + 1, star]. */
		InsertArgs continuation = null;
		int numentries, i;
		
		outerloop:
			while (currnode != null) {
				currnode.pruneTo(valid);
				numentries = currnode.numEntries();
				FigTreeEntry current = null;
				Interval previval;
				Interval currival = null;
				for (i = 0; i < numentries; i++) {
					previval = currival;
					current = currnode.entry(i);
					currival = current.interval();
					if (currival.leftOverlaps(range)) {
						// We need to replace all entries like this with a single entry for "range"
						int j;
						FigTreeEntry previous = current;
						for (j = i + 1; j < numentries && (current = currnode.entry(j)).interval().leftOverlaps(range); j++) {
							previous = current;
						}
						currnode.replaceEntries(i, j, new FigTreeEntry(range, value));
						if (previous.interval().right() > range.right()) {
							// Get the value of star and store the next insert in a continuation
							if (continuation != null) {
								throw new IllegalStateException("Duplicate continuation set");
							}
							path.add(currnode);
							pathIndices.add(i + 1);
							continuation = new InsertArgs(new Interval(range.right() + 1, previous.interval().right()),
									previous.value(), path, pathIndices, currnode.subtree(i + 1),
									valid.restrict(previous.interval().right() + 1, j == numentries ? Integer.MAX_VALUE : current.interval().left() - 1, true));
						}
						break outerloop;
					} else if (range.leftOverlaps(currival)) {
						// Adjust this range and keep going (this can only possibly happen for one node)
						if (currival.right() > range.right()) {
							// In this case, we get star from here
							if (continuation != null) {
								throw new IllegalStateException("Duplicate continutation set");
							}
							continuation = new InsertArgs(new Interval(range.right() + 1, currival.right()), current.value());
						}
						// DON'T UPDATE currival with the new interval; we need the old interval to prune the next node correctly
						current.setInterval(new Interval(currival.left(), range.left() - 1));
					} else if (currival.rightOf(range)) {
						path.add(currnode);
						pathIndices.add(i);
						currnode = currnode.subtree(i);
						/* What if previval and currival are adjacent intervals? Then the entire subtree can be
						 * pruned. This is represented by the special empty interval.
						 */
						valid = valid.restrict(previval == null ? Integer.MIN_VALUE : previval.right() + 1, currival.left() - 1, true);
						continue outerloop;
					}
				}
				path.add(currnode);
				pathIndices.add(numentries);
				currnode = currnode.subtree(numentries);
				valid = valid.restrict(currival == null ? Integer.MIN_VALUE : currival.right() + 1, Integer.MAX_VALUE, true);
			}
		
		treeinsertion:
			if (currnode == null) {
				// In this case, we actually need to do an insertion...
				FigTreeNode rv = null;
				FigTreeEntry topush = new FigTreeEntry(range, value);
				FigTreeNode left = null;
				FigTreeNode right = null;
				FigTreeNode insertinto;
				int insertindex;
				for (int pathindex = path.size() - 1; pathindex >= 0; pathindex--) {
					insertinto = path.get(pathindex);
					insertindex = pathIndices.get(pathindex);
					rv = insertinto.insert(topush, insertindex, left, right);
					if (rv == null) {
						// Nothing to push up
						break treeinsertion;
					}
					topush = rv.entry(0);
					left = rv.subtree(0);
					right = rv.subtree(1);
				}
				
				// No parent to push to
				this.root = rv;
			}
		
		return continuation;
	}
	
	public void insertOld(Interval range, V value) {
		ArrayList<FigTreeNode> path = new ArrayList<FigTreeNode>();
		ArrayList<Integer> pathIndices = new ArrayList<Integer>();
		FigTreeNode currnode = this.root;
		int i;
		
		outerloop:
			do {
				int numentries = currnode.numEntries();
				for (i = 0; i < numentries; i++) {
					FigTreeEntry current = currnode.entry(i);
					Interval currival = current.interval();
					if (currival.left() == range.left()) {
						currnode.replaceEntries(i, i + 1, new FigTreeEntry(range, value));
						return;
					} else if (currival.left() > range.left()) {
						path.add(currnode);
						pathIndices.add(i);
						currnode = currnode.subtree(i);
						continue outerloop;
					}
				}
				path.add(currnode);
				pathIndices.add(numentries);
				currnode = currnode.subtree(numentries);
			} while (currnode != null);
				
		// Now we insert into a leaf and push up
		FigTreeNode rv = null;
		FigTreeEntry topush = new FigTreeEntry(range, value);
		FigTreeNode left = null;
		FigTreeNode right = null;
		FigTreeNode insertinto;
		int insertindex;
		for (int pathindex = path.size() - 1; pathindex >= 0; pathindex--) {
			insertinto = path.get(pathindex);
			insertindex = pathIndices.get(pathindex);
			rv = insertinto.insert(topush, insertindex, left, right);
			if (rv == null) {
				// Nothing to push up
				return;
			}
			topush = rv.entry(0);
			left = rv.subtree(0);
			right = rv.subtree(1);
		}
		
		// No parent to push to
		this.root = rv;
	}
	
	public V lookup(int location) {
		FigTreeNode currnode = this.root;
		
		outerloop:
			do {
				int numentries = currnode.numEntries();
				for (int i = 0; i < numentries; i++) {
					FigTreeEntry current = currnode.entry(i);
					Interval currival = current.interval();
					if (currival.contains(location)) {
						return current.value();
					} else if (currival.left() > location) {
						currnode = currnode.subtree(i);
						continue outerloop;
					}
				}
				currnode = currnode.subtree(numentries);
			} while (currnode != null);
		
		return null;
	}
	
	/* This is used just like a struct. */
	private class FigTreeIterState {
		/*
		 * Stores one node in a path of nodes to reach the current point in the iteration.
		 */
		public FigTreeIterState(FigTreeNode node, Interval valid, FigTreeIterState prev) {
			this.node = node;
			this.entry = null;
			if (node != null) {
				this.entryiter = node.entryIter();
				this.subtreeiter = node.subtreeIter();
			}
			this.valid = valid;
			this.pathprev = prev;
		}
		
		private FigTreeNode node;
		private FigTreeEntry entry;
		private Iterator<FigTreeEntry> entryiter;
		private Iterator<FigTreeNode> subtreeiter;
		private Interval valid;
		private FigTreeIterState pathprev;
	}
	
	public Iterator<V> read(int start, int end) {
		// First, find the start
		Interval initvalid = new Interval(Integer.MIN_VALUE, Integer.MAX_VALUE);
		FigTreeIterState rs = new FigTreeIterState(this.root, initvalid, null);

		outerloop:
			do {
				Interval previval;
				Interval currival = new Interval(Integer.MIN_VALUE, Integer.MIN_VALUE);
				while (rs.entryiter.hasNext()) {
					rs.entry = rs.entryiter.next();
					previval = currival;
					currival = rs.entry.interval();
					if (currival.contains(start)) {
						break outerloop;
					} else if (currival.rightOf(start)) {
						// We won't run into a case where previval and currival are adjacent
						rs = new FigTreeIterState(rs.subtreeiter.next(), rs.valid.restrict(previval.right() + 1, currival.left() - 1), rs);
						continue outerloop;
					}
					rs.subtreeiter.next();
				}
				// So we know what to do when we traverse the subtree and come back here
				rs.entry = null;
				rs = new FigTreeIterState(rs.subtreeiter.next(), rs.valid.restrict(currival.right() + 1, Integer.MAX_VALUE), rs);
			} while (rs.node != null);
		
		if (rs.node == null) {
			/* Didn't find the exact starting byte.
			 * So, stop at the lowest node we got to.
			 */
			rs = rs.pathprev;
		}
		final FigTreeIterState rsfinal = rs;
		
		return new Iterator<V>() {
			public V next() {
				if (this.position >= end) {
					throw new NoSuchElementException();
				}
				V rv = null;
				
				notatend:
					if (rs != null) {
						/* rs === null  when we reach the end of the file; we backtrack past
						 * the root and the readstate becomes null.
						 */
						if (rs.entry.interval().leftOf(position) || rs.valid.leftOf(position)) {
							/* First, descend a subtree until we reach a leaf. */
							
							/* Skip remaining entries if we've moved past the right of the valid interval. */
							if (rs.valid.rightOverlaps(rs.entry.interval())) {
								rs.entry = null;
							} else {
								int leftlimit, rightlimit;
								FigTreeNode subtree;
								
								// Mark the entry to come back to after iterating over the subtree
								leftlimit = rs.entry.interval().right() + 1;
								if (rs.entryiter.hasNext() && !(rs.entry = rs.entryiter.next()).interval().rightOf(rs.valid)) {
									rightlimit = rs.entry.interval().left() - 1;
								} else {
									rs.entry = null;
									rightlimit = Integer.MAX_VALUE;
								}
								
								subtree = rs.subtreeiter.next();
								
								/* If rs.entry is adjacent to what it used to be, there's no point in traversing this subtree. */
								if (leftlimit <= rightlimit) {
									descendloop:
										while (subtree != null) {
											rs = new FigTreeIterState(subtree, rs.valid.restrict(leftlimit, rightlimit), rs);
											leftlimit = Integer.MIN_VALUE;
											if (rs.entryiter.hasNext()) {
												// So that we know to process it when we come back up here
												rs.entry = rs.entryiter.next();
												if (rs.entry.interval().rightOf(rs.valid)) {
													rs.entry = null;
												}
											}
											
											/* Skip entries to the left of the valid interval. */
											while (rs.entry != null && rs.entry.interval().leftOf(rs.valid)) {
												if (rs.entryiter.hasNext()) {
													rs.entry = rs.entryiter.next();
												} else {
													rs.entry = null;
												}
												rs.subtreeiter.next();
											}
											
											if (rs.entry == null) {
												rightlimit = Integer.MAX_VALUE;
											} else {
												/* If the entry overlaps partially with the interval, then
												 * we can skip the left subtree.
												 */
												if (rs.valid.leftOverlaps(rs.entry.interval())) {
													// Skip iterating over this subtree
													rs.subtreeiter.next();
													break descendloop;
												}
												rightlimit = rs.entry.interval().left() - 1;
											}
											subtree = rs.subtreeiter.next();
										}
								}
							}
							
							/* If there was really no subtree (meaning we ended up at the same
							 * node where we started), or the subtree is empty, backtrack up
							 * the tree.
							 */
							while (rs.entry == null) {
								rs = rs.pathprev;
								// If we go past the end of the tree, return null for the unmapped bytes
								if (rs == null) {
									break notatend;
								}
							}
						}
					if (rs.entry.interval().contains(this.position)) {
						rv = this.rs.entry.value();
					}
				}
				this.position++;
				return rv;
			}
			
			public boolean hasNext() {
				return this.position < end;
			}
			
			private int position = start;
			private FigTreeIterState rs = rsfinal;
		};
	}
	
	public String toString() {
		StringBuilder rvbuilder = new StringBuilder();
		
		ArrayList<FigTreeNode> currlevel = new ArrayList<FigTreeNode>();
		ArrayList<FigTreeNode> nextlevel = new ArrayList<FigTreeNode>();
		
		currlevel.add(this.root);
		
		while (currlevel.size() != 0) {
			for (FigTreeNode node : currlevel) {
				rvbuilder.append(node);
				int numentries = node.numEntries();
				for (int i = 0; i <= numentries; i++) {
					/* Due to the B Tree invariants I could move this check outside the loop.
					 * But for debugging, it's useful to know if for some reason the B Tree
					 * isn't balanced.
					 */
					if (node.subtree(i) != null) {
						nextlevel.add(node.subtree(i));
					}
				}
			}
			ArrayList<FigTreeNode> temp = nextlevel;
			nextlevel = currlevel;
			currlevel = temp;
			
			nextlevel.clear();
			
			rvbuilder.append('\n');
		}
		
		return rvbuilder.toString();
	}
	
	private class FigTreeNode {
		public FigTreeNode(int height) {
			if (height < 0) {
				throw new IllegalStateException();
			}
			
			this.HEIGHT = height;
			
			this.clear();
		}
		
		public void clear() {
			this.entries = new ArrayList<FigTreeEntry>();
			this.subtrees = new ArrayList<FigTreeNode>();
			
			FigTreeNode firstchild;
			if (this.HEIGHT == 0) {
				firstchild = null;
			} else {
				firstchild = new FigTreeNode(this.HEIGHT - 1);
			}
			this.subtrees.add(firstchild);
		}
		
		/**
		 * Inserts an entry into this node, and returns the entry to be pushed up to the parent,
		 * given the index at which to insert the entry.
		 * @param index The index at which to insert the entry.
		 * @param leftChild The new left child of the inserted entry (or null if this is a leaf).
		 * @param rightChild The new right child of the inserted entry (or null if this is a leaf).
		 * @return The entry to be pushed up to the parent, as a node with two children, or null if no node is pushed up.
		 */
		public FigTreeNode insert(FigTreeEntry newent, int index, FigTreeNode leftChild, FigTreeNode rightChild) {
			if ((index != 0 && newent.overlaps(this.entries.get(index - 1)))
					|| (index != entries.size() && newent.overlaps(this.entries.get(index)))) {
				throw new IllegalStateException("Insert() violates no-overlap invariant: " + newent + " " + this);
			}
			entries.add(index, newent);
			subtrees.set(index, leftChild);
			subtrees.add(index + 1, rightChild);
			if (entries.size() == FigTree.this.SPLITLIMIT) {
				// Split the node and push middle entry to parent
				FigTreeNode left = new FigTreeNode(this.HEIGHT);
				FigTreeNode right = new FigTreeNode(this.HEIGHT);
				Iterator<FigTreeEntry> entryIter = this.entries.iterator();
				Iterator<FigTreeNode> subtreeIter = this.subtrees.iterator();
				int i;
				
				left.subtrees.set(0, subtreeIter.next());
				for (i = 0; i < FigTree.this.ORDER; i++) {
					left.entries.add(entryIter.next());
					left.subtrees.add(subtreeIter.next());
				}
				
				FigTreeEntry middleEntry = entryIter.next();
				right.subtrees.set(0, subtreeIter.next());
				for (i = i + 1; i < FigTree.this.SPLITLIMIT; i++) {
					right.entries.add(entryIter.next());
					right.subtrees.add(subtreeIter.next());
				}
				
				FigTreeNode pushup = new FigTreeNode(this.HEIGHT + 1);
				pushup.entries.add(middleEntry);
				pushup.subtrees.set(0, left);
				pushup.subtrees.add(right);
				return pushup;
			}
			return null;
		}
		
		public void replaceEntries(int start, int end, FigTreeEntry newent) {
			this.entries.set(start, newent);
			this.entries.subList(start + 1, end).clear();
			this.subtrees.subList(start + 1, end).clear();
		}
		
		/**
		 * Prunes this node, given that an interval containing all possible valid entries
		 * (i.e., all entries that are not cached in an ancestor).
		 * @param valid An interval in which all valid entries in this subtree must exist.
		 */
		public void pruneTo(Interval valid) {
			Iterator<FigTreeEntry> entryiter = this.entries.iterator();
			Iterator<FigTreeNode> subtreeiter = this.subtrees.iterator();
			FigTreeEntry entry;
			FigTreeNode subtree;
			Interval entryint = null;
			
			if (valid == Interval.EMPTY) {
				this.clear();
				return;
			}
			
			// Drop all entries to the left of VALID, along with left subtrees
			if (!entryiter.hasNext()) {
				return;
			}
			
			entryint = (entry = entryiter.next()).interval();
			subtree = subtreeiter.next();
			while (entryint.leftOf(valid)) {
				entryiter.remove();
				subtreeiter.remove();
				
				if (!entryiter.hasNext()) {
					return;
				}
				entryint = (entry = entryiter.next()).interval();
				subtree = subtreeiter.next();
			}
			
			if (valid.leftOverlaps(entryint)) {
				if (subtree != null) {
					subtree.clear();
				}
				
				// In case the valid boundary is in the middle of this interval
				entry.setInterval(entryint.restrict(valid));
				
				if (!entryiter.hasNext()) {
					return;
				}
				entryint = (entry = entryiter.next()).interval();
				subtree = subtreeiter.next();
			}
			
			while (valid.contains(entryint)) {
				if (!entryiter.hasNext()) {
					return;
				}
				entryint = (entry = entryiter.next()).interval();
				subtree = subtreeiter.next();
			}
			
			/* At this point, entryint is either overlapping with the right edge of
			 * VALID, or it is beyond VALID. The left subtree, in either case, cannot
			 * be dropped, since part of it must be in the valid interval. So, just
			 * skip over it. After this, subtreeiter.next() is the right subtree of
			 * entryiter.next().
			 */
			subtree = subtreeiter.next();
			
			if (valid.rightOverlaps(entryint)) {
				if (subtree != null) {
					subtree.clear();
				}
				
				// In case the valid boundary is in the middle of this interval
				entry.setInterval(entryint.restrict(valid));
				
				if (!entryiter.hasNext()) {
					return;
				}
				entryint = (entry = entryiter.next()).interval();
				subtree = subtreeiter.next();
			}
			
			while (entryint.rightOf(valid)) {
				entryiter.remove();
				subtreeiter.remove();
				
				if (!entryiter.hasNext()) {
					return;
				}
				entryint = entryiter.next().interval(); // don't need to assign to entry
				subtreeiter.next(); // don't need to assign to subtree, since henceforth we only need to remove, not clear
			}
		}
		
		public Iterator<FigTreeEntry> entryIter() {
			return Collections.unmodifiableList(this.entries).iterator();
		}
		
		public Iterator<FigTreeNode> subtreeIter() {
			return Collections.unmodifiableList(this.subtrees).iterator();
		}
		
		public FigTreeEntry entry(int i) {
			return entries.get(i);
		}
		
		public FigTreeNode subtree(int i) {
			return subtrees.get(i);
		}
		
		public int numEntries() {
			if (entries.size() + 1 != subtrees.size()) {
				throw new IllegalStateException();
			}
			return entries.size();
		}
		
		public String toString() {
			StringBuilder rvbuilder = new StringBuilder();
			rvbuilder.append('{');
			for (FigTreeEntry entry : entries) {
				rvbuilder.append(entry);
				rvbuilder.append(',');
			}
			rvbuilder.append('}');
			
			return rvbuilder.toString();
		}
		
		private List<FigTreeEntry> entries;
		private List<FigTreeNode> subtrees;
		private final int HEIGHT;
	}
	
	private class FigTreeEntry implements Comparable<FigTreeEntry> {
		public FigTreeEntry(Interval range, V value) {
			this.irange = range;
			this.value = value;
		}
		
		public void setInterval(Interval newrange) {
			this.irange = newrange;
		}
		
		public Interval interval() {
			return this.irange;
		}
		
		public V value() {
			return value;
		}
		
		public boolean overlaps(FigTreeEntry other) {
			return this.irange.overlaps(other.irange);
		}
		
		public int compareTo(FigTreeEntry other) {
			return other.irange.left() - this.irange.left();
		}
		
		public String toString() {
			return String.format("(%s: %s)", this.irange.toString(), this.value.toString());
		}
		
		private Interval irange;
		private V value;
	}
	
	private FigTreeNode root;
	private final int ORDER;
	private final int SPLITLIMIT;
}
