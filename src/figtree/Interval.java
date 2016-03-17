package figtree;

/**
 * An immutable closed interval [a, b].
 * 
 * @author samkumar99
 */
public class Interval {
	public Interval(int left, int right) {
		if (left > right) {
			throw new IllegalStateException();
		}
		this.left = left;
		this.right = right;
	}
	
	public boolean contains(int x) {
		return x >= this.left && x <= this.right;
	}
	
	public boolean contains(Interval other) {
		return this.left <= other.left && this.right >= other.right;
	}
	
	public Interval restrict(Interval to, boolean allowempty) {
		return this.restrict(to.left, to.right, allowempty);
	}
	
	public Interval restrict(Interval to) {
		return this.restrict(to, false);
	}
	
	public Interval restrict(int left, int right, boolean allowempty) {
		int newleft = Math.max(this.left, left);
		int newright = Math.min(this.right, right);
		if (allowempty && newleft > newright) {
			return Interval.EMPTY;
		} else {
			return new Interval(newleft, newright);
		}
	}
	
	public Interval restrict(int left, int right) {
		return this.restrict(left, right, false);
	}
	
	public int left() {
		return this.left;
	}
	
	public int right() {
		return this.right;
	}
	
	public boolean overlaps(Interval other) {
		return this.right >= other.left && this.left <= other.right;
	}
	
	public boolean leftOverlaps(Interval other) {
		return other.contains(this.left);
	}
	
	public boolean rightOverlaps(Interval other) {
		return other.contains(this.right);
	}
	
	public boolean leftOf(Interval other) {
		return this.leftOf(other.left);
	}
	
	public boolean leftOf(int x) {
		return this.right < x;
	}
	
	public boolean rightOf(Interval other) {
		return this.rightOf(other.right);
	}
	
	public boolean rightOf(int x) {
		return this.left > x;
	}
	
	public boolean equals(Object o) {
		if (o instanceof Interval) {
			Interval other = (Interval) o;
			return this.left == other.left && this.right == other.right;
		}
		return false;
	}
	
	public String toString() {
		return String.format("[%d, %d]", this.left, this.right);
	}
	
	private static class EmptyInterval extends Interval {
		public EmptyInterval() {
			super(0, 0);
		}
		public boolean contains(int x) {
			return false;
		}
		public boolean contains(Interval other) {
			return false;
		}
		public Interval restrict(Interval to, boolean allowempty) {
			return this;
		}
		public Interval restrict(int left, int right, boolean allowempty) {
			return this;
		}
		public boolean overlaps(Interval other) {
			return false;
		}
		public boolean leftOverlaps(Interval other) {
			return false;
		}
		public boolean rightOverlaps(Interval other) {
			return false;
		}
		public int left() {
			return Integer.MAX_VALUE;
		}
		public int right() {
			return Integer.MIN_VALUE;
		}
		public boolean leftOf(Interval other) {
			return false;
		}
		public boolean leftOf(int x) {
			return false;
		}
		public boolean rightOf(Interval other) {
			return false;
		}
		public boolean rightOf(int x) {
			return false;
		}
		public boolean equals(Object o) {
			return o instanceof EmptyInterval;
		}
		public String toString() {
			return "[EMPTY]";
		}
	}
	
	private int left;
	private int right;
	
	// Singleton
	public static final Interval EMPTY = new EmptyInterval();
}
