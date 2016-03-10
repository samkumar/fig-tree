import figtree.*;
public class Test {

	public static void main(String[] args) {
		Interval i = new Interval(1, 7);
		System.out.println(i.contains(7));
		
		FigTree<Integer> f = new FigTree<Integer>(2);
		
		for (int x = 1; x <= 11; x++) {
			System.out.printf("Inserting %d\n", x);
			f.insert(new Interval(x, x), x);
			System.out.println(f);
		}
	}

}
