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
	
	private int left;
	private int right;
}
