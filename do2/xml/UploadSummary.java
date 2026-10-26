package itsdc.dolphin.do2.xml;

import java.util.List;

public class UploadSummary {

    private String digitalSignatureStatus = "Invalid";

    private int totalXmlItemsUploaded;
    private int totalValidItems;
    private int totalDuplicateItems;
    private int totalPdfs;
    private String message;
    private String pdfFileName;
    private String topSheetFileName;
    private List<String> xmlFileNames;
    private String summaryZipFile;
    private String topSheetZipFile;
    
    private List<ZipWiseReport> zipWiseReports;


    
    
    public String getDigitalSignatureStatus() {
        return digitalSignatureStatus;
    }

    public void setDigitalSignatureStatus(String digitalSignatureStatus) {
        this.digitalSignatureStatus = digitalSignatureStatus;
    }

    public int getTotalXmlItemsUploaded() {
        return totalXmlItemsUploaded;
    }

    public void setTotalXmlItemsUploaded(int totalXmlItemsUploaded) {
        this.totalXmlItemsUploaded = totalXmlItemsUploaded;
    }

    public int getTotalValidItems() {
        return totalValidItems;
    }

    public void setTotalValidItems(int totalValidItems) {
        this.totalValidItems = totalValidItems;
    }

    public int getTotalDuplicateItems() {
        return totalDuplicateItems;
    }

    public void setTotalDuplicateItems(int totalDuplicateItems) {
        this.totalDuplicateItems = totalDuplicateItems;
    }

    public int getTotalPdfs() {
        return totalPdfs;
    }

    public void setTotalPdfs(int totalPdfs) {
        this.totalPdfs = totalPdfs;
    }
    
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

	public String getPdfFileName() {
		return pdfFileName;
	}

	public void setPdfFileName(String pdfFileName) {
		this.pdfFileName = pdfFileName;
	}

	public String getTopSheetFileName() {
		return topSheetFileName;
	}

	public void setTopSheetFileName(String topSheetFileName) {
		this.topSheetFileName = topSheetFileName;
	}

	public List<String> getXmlFileNames() {
		return xmlFileNames;
	}

	public void setXmlFileNames(List<String> xmlFileNames) {
		this.xmlFileNames = xmlFileNames;
	}

	public String getSummaryZipFile() {
		return summaryZipFile;
	}

	public void setSummaryZipFile(String summaryZipFile) {
		this.summaryZipFile = summaryZipFile;
	}

	public String getTopSheetZipFile() {
		return topSheetZipFile;
	}

	public void setTopSheetZipFile(String topSheetZipFile) {
		this.topSheetZipFile = topSheetZipFile;
	}

	public List<ZipWiseReport> getZipWiseReports() {
		return zipWiseReports;
	}

	public void setZipWiseReports(List<ZipWiseReport> zipWiseReports) {
		this.zipWiseReports = zipWiseReports;
	}
	
	
    
}
