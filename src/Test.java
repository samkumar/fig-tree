import figtree.*;
import java.util.HashMap;
import java.util.Iterator;
public class Test {

	public static void main(String[] args) {
		Interval i = new Interval(1, 7);
		System.out.println(i.contains(7));
		
		FigTree<Integer> f = new FigTree<Integer>(2);
		
		for (int j = 1; j <= 19; j++) {
			int x = j << 1;
			f.insert(new Interval(x, x), x);
			for (int y = 1; y <= x; y++) {
				Integer v = f.lookup(y);
				if ((y & 0x1) == 0) {
					if (v != y) {
						System.out.println("Bad lookup");
					}
				} else {
					if (v != null) {
						System.out.println("Bad lookup");
					}
				}
			}
		}
		
		
		Integer z = f.lookup(1000);
		if (z != null) {
			System.out.println("Bad lookup");
		}
		
		f.insert(new Interval(4, 4), 5);
		System.out.println(f);
		System.out.println(f.lookup(4));
		
		FigTree<Integer> f2 = new FigTree<Integer>(3);
		HashMap<Integer, Integer> rands = new HashMap<Integer, Integer>();
		final int NUM_INSERTS = 1000;
		for (int k = 0; k < NUM_INSERTS; k++) {
			int rand1 = (int) (Math.random() * NUM_INSERTS);
			int rand2 = (int) (Math.random() * NUM_INSERTS);
			f2.insert(new Interval(rand1, rand1), rand2);
			rands.put(rand1, rand2);
			for (int m = 0; m < NUM_INSERTS; m++) {
				Integer r = f2.lookup(m);
				Integer ans = rands.get(m);
				if (((r == null) != (ans == null)) || (r != null && !r.equals(ans))) {
					System.out.println("Bad lookup");
				}
			}
		}
		
		System.out.println(f2);
		System.out.println("Finished running tests");
		
		System.out.println("Running iterator tests");
		Iterator<Integer> range = f2.read(0, NUM_INSERTS);
		for (int y = 0; y < NUM_INSERTS; y++) {
			Integer iterval = range.next();
			Integer correct = rands.get(y);
			if (((iterval == null) != (correct == null)) || (iterval != null && !iterval.equals(correct))) {
				System.out.printf("Bad iteration: %d\n", y);
			}
		}
		if (range.hasNext()) {
			System.out.println("Iteration did not end.");
		}
		System.out.println("Finished running iterator tests");
	}

}
