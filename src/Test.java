import figtree.*;
public class Test {

	public static void main(String[] args) {
		Interval i = new Interval(1, 7);
		System.out.println(i.contains(7));
		
		FigTree<Integer> f = new FigTree<Integer>(2);
		
		for (int j = 1; j <= 19; j++) {
			int x = j << 1;
			System.out.printf("Inserting %d\n", x);
			f.insert(new Interval(x, x), x);
			System.out.println(f);
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
	}

}
