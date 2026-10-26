//Experimental
package itsdc.dolphin.do2;

//You can give your constructors any number of parameters of any type and 
//supply additional methods on your enumerations to access those values.
public enum Do2Description {
	// Audit Cage Do2s
	DUMMY("DUMMY"),
	// Accounting Cage Do2s
	NRA("NRA"), CEA("CEA"), CILQ("CILQ");
	
	private Do2Description(String name) {
		this.name = name;
	}

	private final String name;

	public String toString() {
		return name;
	}
	
	final static public Do2Description getDo2Description(String description){
		Do2Description[] descriptions =  Do2Description.values();
		for(Do2Description d : descriptions){
			if(d.name.equals(description))
				return d;
		}
		return null;
	}
}