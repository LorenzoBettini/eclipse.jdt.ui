package p;

public class UnmovableArg1_in {
	private int fN;
	public static UnmovableArg1_in createUnmovableArg1_in(int N) {
		return new UnmovableArg1_in(N);
	}
	public UnmovableArg1_in(int N) {
		fN= N;
	}
	public int get() {
		return fN;
	}
	public void foo(String[] args) {
		int				size;
		UnmovableArg1_in	ua= createUnmovableArg1_in(size=16);

		System.out.println("Value = " + ua.get());
	}
}
