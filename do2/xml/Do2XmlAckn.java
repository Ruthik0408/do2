package itsdc.dolphin.do2.xml;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.IOUtils;
import org.apache.xml.security.Init;
import org.apache.xml.security.c14n.Canonicalizer;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.transforms.Transforms;
import org.apache.xml.security.utils.ElementProxy;
import org.springframework.core.env.Environment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import itsdc.dolphin.models.Pao;
import itsdc.dolphin.utils.Utils;

public class Do2XmlAckn {
	
	Environment env;
	
	String keyStorePath,keyStorePass,trustStorePath,trustStorePass,privateKeyPath,privateKeyAlias,privateKeyPass;
	
	
	
	public String getKeyStorePath() {
		return keyStorePath;
	}



	public void setKeyStorePath(String keyStorePath) {
		this.keyStorePath = keyStorePath;
	}



	public String getKeyStorePass() {
		return keyStorePass;
	}



	public void setKeyStorePass(String keyStorePass) {
		this.keyStorePass = keyStorePass;
	}



	public String getTrustStorePath() {
		return trustStorePath;
	}



	public void setTrustStorePath(String trustStorePath) {
		this.trustStorePath = trustStorePath;
	}



	public String getTrustStorePass() {
		return trustStorePass;
	}



	public void setTrustStorePass(String trustStorePass) {
		this.trustStorePass = trustStorePass;
	}



	public String getPrivateKeyPath() {
		return privateKeyPath;
	}



	public void setPrivateKeyPath(String privateKeyPath) {
		this.privateKeyPath = privateKeyPath;
	}



	public String getPrivateKeyAlias() {
		return privateKeyAlias;
	}



	public void setPrivateKeyAlias(String privateKeyAlias) {
		this.privateKeyAlias = privateKeyAlias;
	}



	public String getPrivateKeyPass() {
		return privateKeyPass;
	}



	public void setPrivateKeyPass(String privateKeyPass) {
		this.privateKeyPass = privateKeyPass;
	}



	public Environment getEnv() {
		return env;
	}



	public void setEnv(Environment env) {
		this.env = env;
	}



	public String generateXml(Do2AcknObj acknObj,String susNo,String do2No,Date do2Date) {
		
		String dir=env.getProperty("spring.application.filedir");
		SimpleDateFormat df1 = new SimpleDateFormat("dd/MM/yyyy");
		SimpleDateFormat df2 = new SimpleDateFormat("dd_MM_yyyy");
		String fname="";

		System.out.println("susno"+susNo+"--"+do2No+"--"+do2Date+"--"+acknObj+"--"+acknObj.getItemList());
		 
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document doc = docBuilder.newDocument();
			Element rootElement = null;
			rootElement = doc.createElement("HRMS");  
			doc.appendChild(rootElement);
			
			Element header = doc.createElement("Header");  
			rootElement.appendChild(header);
			
			Element sus = doc.createElement("SUS_No");  
			sus.appendChild(doc.createTextNode(acknObj.getSusNo()));
			header.appendChild(sus);
			
			Element ptoNo = doc.createElement("PTO_No");  
			ptoNo.appendChild(doc.createTextNode(acknObj.getPtoNo()));
			header.appendChild(ptoNo);
			
			Element ptoDate = doc.createElement("PTO_Date");  
			ptoDate.appendChild(doc.createTextNode(acknObj.getPtoDate()));
			header.appendChild(ptoDate);
			
			Element obsn = doc.createElement("Obsn_header");  
			obsn.appendChild(doc.createTextNode(acknObj.getObsn()));
			header.appendChild(obsn);
			
			Element detMain = doc.createElement("PTO_Details");  
 			rootElement.appendChild(detMain);
 			
 			
			
			if(acknObj.getItemList()!=null) {
				for(Do2AcknItemObj item:acknObj.getItemList()) {
					Element det = doc.createElement("row");  
		 			detMain.appendChild(det);
		 			
					Element itemNo = doc.createElement("Item_No");  
					itemNo.appendChild(doc.createTextNode(item.getItemNo()));
					det.appendChild(itemNo);
					
					Element armyNo = doc.createElement("Army_No");  
					armyNo.appendChild(doc.createTextNode(item.getArmyNo()));
					det.appendChild(armyNo);
					
					Element desc = doc.createElement("Description");  
					desc.appendChild(doc.createTextNode(item.getDescription()));
					det.appendChild(desc);
					
					Element status = doc.createElement("Status");  
					status.appendChild(doc.createTextNode(item.getStatus()));
					det.appendChild(status);
					
					Element reason = doc.createElement("Reason");  
					reason.appendChild(doc.createTextNode(item.getReason()));
					det.appendChild(reason);
					
					Element pass = doc.createElement("Pass_by");  
					pass.appendChild(doc.createTextNode(item.getPassBy()));
					det.appendChild(pass);
				}
			}
			
			TransformerFactory transformerFactory =  TransformerFactory.newDefaultInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			File f=new File(dir+"/Do2XmlAckn/Files_For_Ds");
			if(!f.exists())
				f.mkdirs();
			f=new File(dir+"/Do2XmlAckn/Files_For_Download");
			if(!f.exists())
				f.mkdirs();
			
			String xmlFileName = null;
			String mesg="";
			 
			fname = "Ackn_" + susNo + "_" + do2No + "_" + df2.format(do2Date) + ".xml";
			xmlFileName = dir + "/Do2XmlAckn/Files_For_Ds/" + fname;
			StreamResult result = new StreamResult(new File(xmlFileName));
			transformer.transform(source, result);
			f = new File(xmlFileName);

			if (f.exists()) {
			    signAcknXmlFiles(xmlFileName);
			}

			// return the SIGNED file path (Files_For_Download), not the deleted Files_For_Ds path
			fname = dir + "/Do2XmlAckn/Files_For_Download/" + fname;
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return fname;
	}

	public String signAcknXmlFiles(String xmlFile) {
		String mesg="";
		 init();
		String fDir=env.getProperty("spring.application.filedir");
		File dir = new File(fDir + "/Do2XmlAckn/Files_For_Ds");
		InputStream fileInputStream = null;
		String dsXmlFileAbs = fDir + "/Do2XmlAckn/Files_For_Download";
		String opResult = "";
		List<String> opCompFilesLst = new ArrayList<String>();
		int fileKnt = 0;
		File f=new File(xmlFile);
		System.out.println("file exists==="+f.exists()+"--"+xmlFile);
		if (f.exists()) {
		//	FileFilter fileFilter = new WildcardFileFilter("*.*", IOCase.INSENSITIVE);
			//File[] files = dir.listFiles(fileFilter);
		//	if (files != null && files.length > 0) {
			//	for (File f : files) {
					try {
						fileInputStream = new FileInputStream(f.getAbsolutePath());
						opResult = output(signFile(fileInputStream),dsXmlFileAbs + "/" + f.getName());
						if (opResult.equals("OK")) {
							fileKnt++;
							if (opCompFilesLst == null || opCompFilesLst.size() <= 0
									|| !opCompFilesLst.contains(f.getAbsolutePath())) {
								opCompFilesLst.add(f.getAbsolutePath());
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
						//FacesMessages.instance().add("!!! Contact System Administrator !!!");
					} finally {
						IOUtils.closeQuietly(fileInputStream);
					}
				}
			//} else {
				//mesg="No Files Found for Digitally Signing";
		//	}
		//} else {
			//mesg="Folder Files_For_Ds is not Found";
		//}
		 f = null;
		if (opCompFilesLst != null && opCompFilesLst.size() > 0) {
			for (String s : opCompFilesLst) {
				f = new File(s);
				if (f.exists()) {
					f.delete();
				}
			}
		}
		opCompFilesLst.clear();
		if (fileKnt > 0) {
			mesg=fileKnt + " xml files digitally signed and are available for download.";
		}
		fileKnt = 0;
		//if (cppXmlFilesForDsList != null && cppXmlFilesForDsList.size() > 0) {
			//cppXmlFilesForDsList.clear();
			//populateCppXmlFilesForDs();
		//}
		return "OK";
	}
	
	public static String output(ByteArrayOutputStream signedOutputStream, String fileName) throws IOException {
		String retVal = "OK";
		final OutputStream fileOutputStream = new FileOutputStream(fileName);
		try {
			fileOutputStream.write(signedOutputStream.toByteArray());
			fileOutputStream.flush();
		} catch (Exception e) {
			Utils.handleException(e);
			retVal = "NOT OK";
		} finally {
			IOUtils.closeQuietly(fileOutputStream);
		}
		return retVal;
	}
	
	public ByteArrayOutputStream signFile(InputStream xmlFile) throws Exception {
		final Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile);
		Init.init();
		ElementProxy.setDefaultPrefix(org.apache.xml.security.utils.Constants.SignatureSpecNS, "");
		KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
//		System.out.println(">>>>>>>>>>>>>> PATH_TO_KEYSTORE : " + getPATH_TO_KEYSTORE());
		ks.load(new FileInputStream(getKeyStorePath()), getKeyStorePass().toCharArray());
		final XMLSignature sig = new XMLSignature(doc, null, XMLSignature.ALGO_ID_SIGNATURE_RSA);
		final Transforms transforms = new Transforms(doc);
		transforms.addTransform(Transforms.TRANSFORM_ENVELOPED_SIGNATURE);
		sig.addDocument("", transforms, org.apache.xml.security.utils.Constants.ALGO_ID_DIGEST_SHA1);
//        System.out.println("????????? PRIVATE_KEY_ALIAS : " + PRIVATE_KEY_ALIAS);
		final Key privateKey = ks.getKey(getPrivateKeyAlias(), getPrivateKeyPass().toCharArray());
		final X509Certificate cert = (X509Certificate) ks.getCertificate(getPrivateKeyAlias());
		sig.addKeyInfo(cert);
		sig.addKeyInfo(cert.getPublicKey());
		sig.sign(privateKey);
		doc.getDocumentElement().appendChild(sig.getElement());
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		outputStream
				.write(Canonicalizer.getInstance(Canonicalizer.ALGO_ID_C14N_WITH_COMMENTS).canonicalizeSubtree(doc));
		return outputStream;
	}
	
	public void init() {
		
		Pao pao = Utils.getPao();
		String reportsDir = env.getProperty("spring.application.filedir");
		String confDir=env.getProperty("spring.application.conf");
		Properties props = new Properties();
		String confFilePath=confDir + "/saamarthConfig.txt";
 
		try {
			FileInputStream in = new FileInputStream(confFilePath);
			if (in != null) {
				props.load(in);
 				setPrivateKeyAlias(props.getProperty("PAO_PKEY_ALIAS"));
 				setKeyStorePath(props.getProperty("PAO_KEYSTORE"));
				setKeyStorePass(props.getProperty("KEYSTORE_PASS"));
				setPrivateKeyPass(props.getProperty("PKEY_PASS"));
 				in.close();
				initKeyStore(getKeyStorePath(), getKeyStorePass());
				initTrustStore(getKeyStorePath(),getKeyStorePass().trim());
//				myPrivateKey = getRSAPrivateKey(props.getProperty("KEY_STORE").trim(), getDsPassword().trim(), props.getProperty("ALIAS").trim());
//				myPublicKey = getRSAPublicKey(props.getProperty("KEY_STORE").trim(), getDsPassword().trim(), props.getProperty("ALIAS").trim());
			}
			 
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void initKeyStore(String keyStorePath, String password) {
		System.setProperty("javax.net.ssl.keyStore", keyStorePath);
		System.setProperty("javax.net.ssl.keyStorePassword", password);
	}

	public static void initTrustStore(String trustStorePath, String password) {
		System.setProperty("javax.net.ssl.trustStore", trustStorePath);
		System.setProperty("javax.net.ssl.trustStorePassword", password);
	}

public String generateRejectionXml(Do2AcknObj acknObj, String susNo, String do2No, Date do2Date,
			String uploadFileName) {

		String dir = env.getProperty("spring.application.filedir");
		SimpleDateFormat df1 = new SimpleDateFormat("dd/MM/yyyy");
		SimpleDateFormat df2 = new SimpleDateFormat("dd_MM_yyyy");
		String fname = "";
		String originalFileName = uploadFileName;

		System.out.println(
				"susno" + susNo + "--" + do2No + "--" + do2Date + "--" + acknObj + "--" + acknObj.getItemList());

		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document doc = docBuilder.newDocument();
			Element rootElement = null;
			rootElement = doc.createElement("HRMS");
			doc.appendChild(rootElement);

			Element header = doc.createElement("Header");
			rootElement.appendChild(header);

			Element sus = doc.createElement("SUS_No");
			sus.appendChild(doc.createTextNode(acknObj.getSusNo()));
			header.appendChild(sus);

			Element ptoNo = doc.createElement("PTO_No");
			ptoNo.appendChild(doc.createTextNode(acknObj.getPtoNo()));
			header.appendChild(ptoNo);

			Element ptoDate = doc.createElement("PTO_Date");
			ptoDate.appendChild(doc.createTextNode(acknObj.getPtoDate()));
			header.appendChild(ptoDate);

			Element obsn = doc.createElement("Obsn_header");
			obsn.appendChild(doc.createTextNode(acknObj.getObsn()));
			header.appendChild(obsn);

			Element detMain = doc.createElement("PTO_Details");
			rootElement.appendChild(detMain);

			if (acknObj.getItemList() != null) {
				for (Do2AcknItemObj item : acknObj.getItemList()) {
					Element det = doc.createElement("row");
					detMain.appendChild(det);

					Element itemNo = doc.createElement("Item_No");
					itemNo.appendChild(doc.createTextNode(item.getItemNo()));
					det.appendChild(itemNo);

					Element armyNo = doc.createElement("Army_No");
					armyNo.appendChild(doc.createTextNode(item.getArmyNo()));
					det.appendChild(armyNo);

					Element desc = doc.createElement("Description");
					desc.appendChild(doc.createTextNode(item.getDescription()));
					det.appendChild(desc);

					Element status = doc.createElement("Status");
					status.appendChild(doc.createTextNode(item.getStatus()));
					det.appendChild(status);

					Element reason = doc.createElement("Reason");
					reason.appendChild(doc.createTextNode(item.getReason()));
					det.appendChild(reason);

					Element pass = doc.createElement("Pass_by");
					pass.appendChild(doc.createTextNode(item.getPassBy()));
					det.appendChild(pass);
				}
			}

			TransformerFactory transformerFactory = TransformerFactory.newDefaultInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			File f = new File(dir + "/Do2XmlAckn/Files_For_Ds");
			if (!f.exists())
				f.mkdirs();
			f = new File(dir + "/Do2XmlAckn/Files_For_Download");
			if (!f.exists())
				f.mkdirs();

			String xmlFileName = null;
			String mesg = "";
 
			if (originalFileName != null && originalFileName.toLowerCase().endsWith(".xml")) {
				originalFileName = originalFileName.substring(0, originalFileName.length() - 4);
			}
			// fname = "Ackn_" + susNo + "_" + do2No + "_" + df2.format(do2Date) + ".xml";
			fname = "Ackn_" + originalFileName + "_" + susNo + "_" + do2No + "_" + df2.format(do2Date) + ".xml";
//			xmlFileName = dir + "/Do2XmlAckn/Files_For_Ds/Ackn_" + susNo + "_" + do2No + "_" + df2.format(do2Date)
//					+ ".xml";
			xmlFileName = dir + "/Do2XmlAckn/Files_For_Ds/Ackn_" + originalFileName + "_" + susNo + "_" + do2No + "_"
					+ df2.format(do2Date) + ".xml";
			StreamResult result = new StreamResult(new File(xmlFileName));
			transformer.transform(source, result);
			f = new File(xmlFileName);

			if (f.exists()) {
				mesg = f.getName();
				signAcknXmlFiles(xmlFileName);
			}
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		fname = dir + "/Do2XmlAckn/Files_For_Download/" + "Ackn_" + susNo + "_" + do2No + "_" + df2.format(do2Date)
//				+ ".xml";
		fname = dir + "/Do2XmlAckn/Files_For_Download/Ackn_" + originalFileName + "_" + susNo + "_" + do2No + "_"
				+ df2.format(do2Date) + ".xml";

		return fname;
	}
	
}
