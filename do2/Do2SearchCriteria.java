package itsdc.dolphin.do2;

public class Do2SearchCriteria {
	private String key;
	private String operation;
	private Object value;
	
	
 
	public Do2SearchCriteria(String key2, String operation2, Object value2) {
		// TODO Auto-generated constructor stub
		this.key=key2;
		this.operation=operation2;
		this.value=value2;
	}
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public String getOperation() {
		return operation;
	}
	public void setOperation(String operation) {
		this.operation = operation;
	}
	public Object getValue() {
		return value;
	}
	public void setValue(Object value) {
		this.value = value;
	}
	
	

}
