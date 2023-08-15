package exceptions;

public class GenericExampleException extends RuntimeException {

	private final String errorName;

	public GenericExampleException(String errorName, String errorMessage) {
		super(errorMessage);
		this.errorName = errorName;		
	}

	public String getErrorName() {
		return errorName;
	}
	
	@Override
	public String toString() {
	    return getErrorName() + ": " + super.getMessage();
	}
}
