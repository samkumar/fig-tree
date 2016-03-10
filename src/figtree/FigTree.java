package figtree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class FigTree<V> {
	public FigTree(int order) {
		this.root = new FigTreeNode(0);
		this.ORDER = order;
		this.SPLITLIMIT = 1 + (order << 1);
	}
	
	public void insert(Interval range, V value) {
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
			assert height >= 0;
			
			this.entries = new ArrayList<FigTreeEntry>();
			this.subtrees = new ArrayList<FigTreeNode>();
			
			FigTreeNode firstchild;
			if (height == 0) {
				firstchild = null;
			} else {
				firstchild = new FigTreeNode(height - 1);
			}
			this.subtrees.add(firstchild);
			
			this.HEIGHT = height;
		}
		
		/**
		 * Inserts an entry into this node, and returns the entry to be pushed up to the parent.
		 * @param newent The entry to be inserted.
		 * @param leftChild The new left child of the inserted entry (or null if this is a leaf).
		 * @param rightChild The new right child of the inserted entry (or null if this is a leaf).
		 * @return The entry to be pushed up to the parent, as a node with two children, or null if no node is pushed up.
		 */
		public FigTreeNode insert(FigTreeEntry newent, FigTreeNode leftChild, FigTreeNode rightChild) {
			int index = -Collections.<FigTreeEntry>binarySearch(this.entries, newent);
			if (index < 0) {
				throw new IllegalArgumentException("Insert()ing an interval that already exists");
			}
			return this.insert(newent, index, leftChild, rightChild);
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
				throw new IllegalStateException("Insert() violates no-overlap invariant");
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
		 * Invalidates a left portion of this node, given a cached interval in an ancestor.
		 * @param invalid A cached interval in an ancestor of this node.
		 */
		public void invalidateLeft(Interval invalid) {
			Iterator<FigTreeEntry> entryiter = this.entries.iterator();
			FigTreeEntry e;
			Interval entryint = null;
			int i = 0;
			for (e = null; entryiter.hasNext(); e = entryiter.next()) {
				entryint = e.interval();
				if (invalid.contains(entryint)) {
					i++;
				} else {
					break;
				}
			}
			if (e == null) {
				// No entries in this node
				return;
			}
			boolean hasNext = entryiter.hasNext();
			this.entries.subList(0, i).clear();
			if (hasNext && invalid.overlaps(entryint)) {
				entryint = new Interval(invalid.right() + 1, entryint.right());
				e.setInterval(entryint);
				i++;
			}
			this.subtrees.subList(0, i).clear();
		}
		
		/**
		 * Invalidates a right portion of this node, given a cached interval in an ancestor.
		 * @param invalid A cached interval in an ancestor of this node.
		 */
		public void invalidateRight(Interval invalid) {
			int numentries = this.entries.size();
			ListIterator<FigTreeEntry> entryiter = this.entries.listIterator(numentries);
			FigTreeEntry e;
			Interval entryint = null;
			int i = numentries;
			for (e = null; entryiter.hasPrevious(); e = entryiter.previous()) {
				entryint = e.interval();
				if (invalid.contains(entryint)) {
					i--;
				} else {
					break;
				}
			}
			if (e == null) {
				// No entries in this node
				return;
			}
			boolean hasPrevious = entryiter.hasPrevious();
			this.entries.subList(i, numentries).clear();
			if (hasPrevious && invalid.overlaps(entryint)) {
				entryint = new Interval(entryint.left(), invalid.left() + 1);
				e.setInterval(entryint);
				i--;
			}
			this.subtrees.subList(i, numentries).clear();
		}
		
		public FigTreeEntry entry(int i) {
			return entries.get(i);
		}
		
		public FigTreeNode subtree(int i) {
			return subtrees.get(i);
		}
		
		public int numEntries() {
			return entries.size();
		}
		
		public boolean isLeaf() {
			return this.HEIGHT == 0;
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
