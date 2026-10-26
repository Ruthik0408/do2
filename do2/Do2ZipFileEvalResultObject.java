package itsdc.dolphin.do2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

 
 
public class Do2ZipFileEvalResultObject implements Serializable {

	private String zipEvalResult;
	private List<String> retEntryCrcFileNamesLst;
	private List<String> retEntryDsFileNamesLst;
	
	public String getZipEvalResult() {
		return zipEvalResult;
	}
	public void setZipEvalResult(String zipEvalResult) {
		this.zipEvalResult = zipEvalResult;
	}
	public List<String> getRetEntryCrcFileNamesLst() {
		return retEntryCrcFileNamesLst;
	}
	public void setRetEntryCrcFileNamesLst(List<String> retEntryCrcFileNamesLst) {
		this.retEntryCrcFileNamesLst = retEntryCrcFileNamesLst;
	}
	public List<String> getRetEntryDsFileNamesLst() {
		return retEntryDsFileNamesLst;
	}
	public void setRetEntryDsFileNamesLst(List<String> retEntryDsFileNamesLst) {
		this.retEntryDsFileNamesLst = retEntryDsFileNamesLst;
	}

}
