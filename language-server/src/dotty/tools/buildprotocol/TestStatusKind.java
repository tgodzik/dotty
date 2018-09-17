package dotty.tools.buildprotocol;

public enum TestStatusKind {

  Ignored(1),
  Running(2),
  Success(3),
  Failure(4);
	
	private final int value;
	
	TestStatusKind(int value) {
		this.value = value;
	}
	
	public int getValue() {
		return value;
	}
	
	public static TestStatusKind forValue(int value) {
		TestStatusKind[] allValues = TestStatusKind.values();
		if (value < 1 || value > allValues.length)
			throw new IllegalArgumentException("Illegal enum value: " + value);
		return allValues[value - 1];
	}

}
