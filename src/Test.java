import figtree.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
public class Test {

	public static void main(String[] args) {
		Interval i = new Interval(1, 7);
		System.out.println(i.contains(7));
		
		FigTree<Integer> f = new FigTree<Integer>(2);
		
		for (int j = 1; j <= 19; j++) {
			int x = j << 1;
			f.write(new Interval(x, x), x);
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
		
		f.write(new Interval(4, 4), 5);
		System.out.println(f);
		System.out.println(f.lookup(4));
		
		FigTree<Integer> c = new FigTree<Integer>(3);
		Interval c1 = new Interval(592, 747);
		Interval c2 = new Interval(582, 782);
		Interval c3 = new Interval(352, 353);
		Interval c4 = new Interval(430, 771);
		Interval c5 = new Interval(484, 828);
		c.write(c1, 0x77832d2);
		c.write(c2, 0x8b1f4cf);
		c.write(c3, 0x6f46e7c3);
		c.write(c4, 0x76a36219);
		c.write(c5, 0x2f88bf80);
		System.out.println("C Test");
		System.out.println(c);
		
		FigTree<Integer> f4 = new FigTree<Integer>(3);
		f4.write(new Interval(1, 10), 0);
		f4.write(new Interval(101, 110), 0);
		f4.write(new Interval(201, 210), 100);
		f4.write(new Interval(301, 310), 200);
		f4.write(new Interval(401, 410), 300);
		f4.write(new Interval(501, 510), 400);
		f4.write(new Interval(601, 610), 500);
		f4.write(new Interval(701, 710), 700);
		f4.write(new Interval(801, 810), 800);
		f4.write(new Interval(901, 910), 900);
		f4.write(new Interval(1001, 1010), 1000);
		f4.write(new Interval(1101, 1110), 1100);
		f4.write(new Interval(1201, 1210), 1200);
		f4.write(new Interval(1301, 1310), 1300);
		
		f4.write(new Interval(350, 710), 1000000);
		
		
		
		System.out.println(f4);
		Iterator<FigTree<Integer>.Fig> range2 = f4.read(0, 1400);
		while (range2.hasNext()) {
			FigTree<Integer>.Fig f123 = range2.next();
			System.out.println(f123.range() + ": " + f123.value());
		}
				
		FigTree<Integer> f2 = new FigTree<Integer>(3);
		HashMap<Integer, Integer> rands = new HashMap<Integer, Integer>();
		final int NUM_INSERTS = 2000;
		Random rand = new Random(47);
		for (int k = 0; k < NUM_INSERTS; k++) {
			int rand1 = rand.nextInt(NUM_INSERTS);
			int rand2 = rand.nextInt(NUM_INSERTS);
			int rand3 = rand.nextInt(NUM_INSERTS);
			if (rand1 > rand2) {
				int temp = rand1;
				rand1 = rand2;
				rand2 = temp;
			}
			rand2 = rand1 + (rand2 & 0x000000FF);
			f2.write(new Interval(rand1, rand2), rand3);
			for (int r = rand1; r <= rand2; r++) {
				rands.put(r, rand3);
			}
			for (int m = 0; m < NUM_INSERTS; m++) {
				Integer r = f2.lookup(m);
				Integer ans = rands.get(m);
				if (((r == null) != (ans == null)) || (r != null && !r.equals(ans))) {
					System.out.println("Bad lookup");
					System.out.println(f2);
					System.out.println(m);
					System.out.println(ans);
					System.out.println(r);
					return;
				}
			}
			System.out.println(f2);
			System.out.printf("Running iterator tests %d\n", k);
			for (int start = 0; start < NUM_INSERTS; start++) {
				for (int end = 0; end < NUM_INSERTS; end++) {
					try {
						Iterator<FigTree<Integer>.Fig> range = f2.read(start, end);
						int y = start;
						while (range.hasNext()) {
							FigTree<Integer>.Fig iterval = range.next();
							Integer correct, intree;
							intree = iterval.value();
							for (; iterval.range().rightOf(y); y++) {
								correct = rands.get(y);
								if (correct != null) {
									System.out.printf("Bad iteration: %d: (nothing) != %s\n", y, correct);
									throw new IllegalStateException();
								}
							}
							for (; iterval.range().contains(y); y++) {
								correct = rands.get(y);
								if (!iterval.value().equals(correct)) {
									System.out.printf("Bad iteration: %d: %s != %s\n", y, intree, correct);
									throw new IllegalStateException();
								}
							}
						}
						for (; y < end; y++) {
							if (rands.get(y) != null) {
								System.out.printf("%d corresponds to %d\n", y, rands.get(y));
								System.out.println("Iteration ended early.");
								throw new IllegalStateException();
							}
						}
					} catch (Exception e) {
						System.out.printf("Was reading from %d to %d\n", start, end);
						e.printStackTrace();
						System.exit(1);
					}
				}
			}
		}
		
		System.out.println("Final tree");
		System.out.println(f2);
	}

}
