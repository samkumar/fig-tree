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
		return this.left <= other.left && this.right >= other.left;
	}
	
	public Interval restrict(Interval to) {
		return this.restrict(to.left, to.right);
	}
	
	public Interval restrict(int left, int right) {
		return new Interval(Math.max(this.left, left), Math.min(this.right, right));
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
	
	private int left;
	private int right;
}
