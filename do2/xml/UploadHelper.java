package itsdc.dolphin.do2.xml;

public class UploadHelper {

	
	static public Boolean isLastCharAlpha(String str) {
		String last = str.substring(str.length() - 1);
		if (last.matches("^[a-zA-Z]"))
			return true;
		else
			return false;
	}
}
