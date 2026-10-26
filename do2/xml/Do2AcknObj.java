package itsdc.dolphin.do2.xml;

import java.util.List;

public class Do2AcknObj {

	String susNo;
	String ptoNo;
	String ptoDate;
	String obsn;
	String xmlFileName;
	List<Do2AcknItemObj> itemList;
	public String getSusNo() {
		return susNo;
	}
	public void setSusNo(String susNo) {
		this.susNo = susNo;
	}
	public String getPtoNo() {
		return ptoNo;
	}
	public void setPtoNo(String ptoNo) {
		this.ptoNo = ptoNo;
	}
	public String getPtoDate() {
		return ptoDate;
	}
	public void setPtoDate(String ptoDate) {
		this.ptoDate = ptoDate;
	}
	public String getObsn() {
		return obsn;
	}
	public void setObsn(String obsn) {
		this.obsn = obsn;
	}
	public String getXmlFileName() {
		return xmlFileName;
	}
	public void setXmlFileName(String xmlFileName) {
		this.xmlFileName = xmlFileName;
	}
	public List<Do2AcknItemObj> getItemList() {
		return itemList;
	}
	public void setItemList(List<Do2AcknItemObj> itemList) {
		this.itemList = itemList;
	}
	
	
	
}
