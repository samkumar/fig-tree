package figtree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class FigTree<V> {
	public FigTree(int order) {
		this.root = new FigTreeNode(0);
		this.ORDER = order;
	}
	
	public synchronized void put(Interval range, V value) {
	}
	
	public synchronized V lookup(Interval range) {
		return null;
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
			if (index < 0
					|| (index != 0 && newent.overlaps(this.entries.get(index - 1)))
					|| (index != entries.size() && newent.overlaps(this.entries.get(index)))) {
				throw new IllegalStateException("Insert() violates no-overlap invariant");
			}
			entries.add(index, newent);
			subtrees.set(index, leftChild);
			subtrees.add(index + 1, rightChild);
			if (entries.size() == 1 + (FigTree.this.ORDER << 1)) {
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
				for (i = i + 1; i < FigTree.this.ORDER; i++) {
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
		
		public List<FigTreeEntry> entries() {
			return Collections.<FigTreeEntry>unmodifiableList(entries);
		}
		
		public FigTreeNode subtree(int i) {
			return subtrees.get(i);
		}
		
		public boolean isLeaf() {
			return this.HEIGHT == 0;
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
		
		private Interval irange;
		private V value;
	}
	
	private FigTreeNode root;
	private final int ORDER;
}
