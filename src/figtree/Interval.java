package figtree;

/**
 * An immutable closed interval [a, b].
 * 
 * @author samkumar99
 */
public class Interval {
	public Interval(int left, int right) {
		this.left = left;
		this.right = right;
	}
	
	public boolean contains(int x) {
		return x >= this.left && x <= this.right;
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
	
	public boolean equals(Object o) {
		if (o instanceof Interval) {
			Interval other = (Interval) o;
			return this.left == other.left && this.right == other.right;
		}
		return false;
	}
	
	private int left;
	private int right;
}
